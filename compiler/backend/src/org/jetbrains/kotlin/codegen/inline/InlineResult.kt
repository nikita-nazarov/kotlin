/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.HashMap
import java.util.HashSet

class InlineResult private constructor() {

    private val notChangedTypes = hashSetOf<String>()
    private val classesToRemove = HashSet<String>()
    private val changedTypes = HashMap<String, String>()
    val reifiedTypeParametersUsages = ReifiedTypeParametersUsages()
    val lineNumbersBeforeRemapping = mutableListOf<Int>()
    val lineNumbersAfterRemapping = mutableListOf<Int>()
    val inlineScopes = mutableListOf<InlineScope>()

    fun merge(child: InlineResult) {
        classesToRemove.addAll(child.calcClassesToRemove())
    }

    fun mergeWithNotChangeInfo(child: InlineResult) {
        notChangedTypes.addAll(child.notChangedTypes)
        merge(child)
    }

    fun addClassToRemove(classInternalName: String) {
        classesToRemove.add(classInternalName)
    }

    fun addNotChangedClass(classInternalName: String) {
        notChangedTypes.add(classInternalName)
    }

    fun addChangedType(oldClassInternalName: String, newClassInternalName: String) {
        changedTypes.put(oldClassInternalName, newClassInternalName)
    }


    fun calcClassesToRemove(): Set<String> {
        return classesToRemove - notChangedTypes
    }

    fun getChangedTypes(): Map<String, String> {
        return changedTypes
    }

    fun addInlineScopeInfo(nodeAndSMAP: SMAPAndMethodNode) {
        val inlineScopesNum = inlineScopes.size
        addInlineScopeInfoFromSMAP(nodeAndSMAP.classSMAP)
        if (inlineScopesNum == inlineScopes.size) {
            addInlineScopeInfoFromMarkerVariables(nodeAndSMAP.node)
        }
    }

    private fun addInlineScopeInfoFromMarkerVariables(node: MethodNode) {
        val oldToNewLineNumbers = getOldToNewLineNumbersMapping()
        val labels = mutableListOf<Label>()
        val lineNumbers = mutableListOf<Int>()
        val labelToIndex = mutableMapOf<Label, Int>()
        val markerVariableInfos = mutableListOf<Triple<String, Int, Int>>()

        // When visiting the LVT, the ordering of inline scope marker variables is unknown,
        // so it's more convenient to collect them first, and then sort by their occurrence in
        // bytecode.
        node.accept(object : MethodVisitor(Opcodes.API_VERSION) {
            var currentLineNumber = 0
            override fun visitLabel(label: Label?) {
                label?.let {
                    labelToIndex[it] = labels.size
                    lineNumbers.add(currentLineNumber)
                    labels.add(it)
                }
                super.visitLabel(label)
            }

            override fun visitLineNumber(line: Int, start: Label?) {
                currentLineNumber = line
                super.visitLineNumber(line, start)
            }

            override fun visitLocalVariable(
                name: String?,
                descriptor: String?,
                signature: String?,
                start: Label?,
                end: Label?,
                index: Int,
            ) {
                super.visitLocalVariable(name, descriptor, signature, start, end, index)
                if (name == null || start == null || end == null) {
                    return
                }

                if (!name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) &&
                    !name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)
                ) {
                    return
                }

                // Do not add information about the inline scope of a method itself as it
                // will be added later
                if (labels.isEmpty() || (((index == 0 && start == labels.first()) || start == labels[1]) && end == labels.last())) {
                    return
                }

                val startIndex = labelToIndex[start]?.takeIf { it >= 0 && it < labels.size } ?: return
                val endIndex = labelToIndex[end]?.takeIf { it > startIndex && it < labels.size } ?: return
                markerVariableInfos.add(Triple(name, startIndex, endIndex))
            }
        })

        markerVariableInfos.sortBy { it.second }

        val scopeNumbers = Array<Int?>(labels.size) { null }
        for ((i, info) in markerVariableInfos.withIndex()) {
            val (_, start, end) = info
            for (j in start + 1..end) {
                scopeNumbers[j] = i
            }
        }

        val inlineScopesSize = inlineScopes.size
        val indexToLambdaScopeIndex = Array<Int?>(labels.size) { null }
        for ((i, info) in markerVariableInfos.withIndex()) {
            val (name, start, end) = info
            val introductionLine = oldToNewLineNumbers[lineNumbers[start]] ?: continue
            val scopeLineNumbers = LinkedHashSet<Int>()
            val isInlineArgumentMarker = name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)
            for (j in start + 1..end) {
                if (isInlineArgumentMarker) {
                    indexToLambdaScopeIndex[j] = inlineScopes.size
                }

                if (scopeNumbers[j] == i) {
                    val lineNumber = lineNumbers[j]
                    scopeLineNumbers.addIfNotNull(oldToNewLineNumbers[lineNumber])
                }
            }

            val parentIndex = scopeNumbers[start]?.plus(inlineScopesSize)
            val surroundingScopeIndex = indexToLambdaScopeIndex[start]
            if (isInlineArgumentMarker) {
                inlineScopes.add(InlineLambdaScope(name.withoutScopeNumber(), parentIndex, introductionLine, surroundingScopeIndex, scopeLineNumbers.toMutableList()))
            } else {
                inlineScopes.add(InlineScope(name.withoutScopeNumber(), parentIndex, introductionLine, scopeLineNumbers.toMutableList()))
            }
        }
    }


    private fun addInlineScopeInfoFromSMAP(smap: SMAP) {
        if (lineNumbersAfterRemapping.isEmpty()) {
            return
        }

        val oldToNewLineNumbers = getOldToNewLineNumbersMapping()
        val lineNumbersBeforeRemappingSet = lineNumbersBeforeRemapping.toSet()
        for (scope in smap.inlineScopes) {
            val matchingLineNumbers = scope.lineNumbers.filter { it in lineNumbersBeforeRemappingSet }
            if (matchingLineNumbers.isNotEmpty()) {
                val newCallSiteLineNumber = oldToNewLineNumbers[scope.callSiteLineNumber] ?: scope.callSiteLineNumber
                val lineNumbers = matchingLineNumbers.mapNotNull { oldToNewLineNumbers[it] }.toMutableList()
                if (scope is InlineLambdaScope) {
                    inlineScopes.add(
                        InlineLambdaScope(
                            scope.name,
                            scope.callerScopeId,
                            newCallSiteLineNumber,
                            scope.surroundingScopeId,
                            lineNumbers
                        )
                    )
                } else {
                    inlineScopes.add(InlineScope(scope.name, scope.callerScopeId, newCallSiteLineNumber, lineNumbers))
                }
            }
        }
    }

    private fun getOldToNewLineNumbersMapping(): Map<Int, Int> =
        lineNumbersBeforeRemapping.zip(lineNumbersAfterRemapping).toMap()

    companion object {
        @JvmStatic
        fun create(): InlineResult {
            return InlineResult()
        }
    }
}
