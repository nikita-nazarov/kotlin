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

import java.util.HashMap
import java.util.HashSet

class InlineResult private constructor() {

    private val notChangedTypes = hashSetOf<String>()
    private val classesToRemove = HashSet<String>()
    private val changedTypes = HashMap<String, String>()
    val reifiedTypeParametersUsages = ReifiedTypeParametersUsages()
    val lineNumbersBeforeRemapping = mutableListOf<Int>()
    val lineNumbersAfterRemapping = mutableListOf<Int>()
    val restoredScopes = mutableListOf<InlineScopeInfo>()
    val restoredMappings = mutableListOf<ScopeMapping>()

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

    fun addInlineScopeInfo(smap: SMAP) {
        if (lineNumbersAfterRemapping.isEmpty()) {
            return
        }

        val old2NewLineNumbers = hashMapOf<Int, Int>()
        for ((i, j) in lineNumbersBeforeRemapping.zip(lineNumbersAfterRemapping)) {
            old2NewLineNumbers[i] = j
        }

        val lineNumbersBeforeRemappingSet = lineNumbersBeforeRemapping.toSet()
        for (scopeMapping in smap.scopeMappings) {
            val inlineScope = smap.inlineScopes.getOrNull(scopeMapping.scopeNumber) ?: continue
            val matchingLineNumbers = scopeMapping.lineNumbers.filter { it in lineNumbersBeforeRemappingSet }
            if (matchingLineNumbers.isNotEmpty()) {
                val newCallSiteLineNumber = old2NewLineNumbers[inlineScope.callSiteLineNumber] ?: inlineScope.callSiteLineNumber
                if (inlineScope is InlineLambdaScopeInfo) {
                    restoredScopes.add(InlineLambdaScopeInfo(inlineScope.name, inlineScope.callerScopeId, newCallSiteLineNumber, inlineScope.surroundingScopeId))
                } else {
                    restoredScopes.add(InlineScopeInfo(inlineScope.name, inlineScope.callerScopeId, newCallSiteLineNumber))
                }
                restoredMappings.add(
                    ScopeMapping(
                        restoredMappings.size,
                        matchingLineNumbers.mapNotNull { old2NewLineNumbers[it] }.toMutableList()
                    )
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun create(): InlineResult {
            return InlineResult()
        }
    }
}
