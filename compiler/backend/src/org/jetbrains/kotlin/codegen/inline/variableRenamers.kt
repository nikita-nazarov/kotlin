/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.LocalVariableNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.*

const val INLINE_SCOPE_NUMBER_SEPARATOR = "\\"

interface InlineVariableRenamer {
    val inlineScopesNum: Int
        get() = 0

    fun renameVariables(node: MethodNode, scopeOffset: Int = 0)
}

class InlineMarkerVariableRenamer : InlineVariableRenamer {
    override var inlineScopesNum = 0

    override fun renameVariables(node: MethodNode, scopeOffset: Int) {
        val labelNodes = node.instructions.filterIsInstance<LabelNode>()
        val labelToIndex = mutableMapOf<Label, Int>()
        for ((i, label) in labelNodes.withIndex()) {
            labelToIndex[label.label] = i
        }

        fun LocalVariableNode.contains(other: LocalVariableNode): Boolean {
            val startIndex = labelToIndex[start.label] ?: return false
            val endIndex = labelToIndex[end.label] ?: return false
            val otherStartIndex = labelToIndex[other.start.label] ?: return false
            val otherEndIndex = labelToIndex[other.end.label] ?: return false
            return startIndex <= otherStartIndex && endIndex >= otherEndIndex
        }

        val sortedVariables = node.localVariables.sortedBy { labelToIndex[it.start.label] }

        val variablesWithNotMatchingDepth = mutableListOf<LocalVariableNode>()
        val inlineScopes = Stack<LocalVariableNode>()
        var inlineScopeIndex = -1

        fun newName(variable: LocalVariableNode) =
            variable.name
                .withoutInlineVarSuffix()
                .withoutScopeNumber()
                .addScopeNumber(inlineScopeIndex + scopeOffset)

        for (variable in sortedVariables) {
            val name = variable.name
            if (isFakeLocalVariableForInline(name)) {
                inlineScopeIndex += 1
                while (inlineScopes.isNotEmpty() && !inlineScopes.peek().contains(variable)) {
                    inlineScopes.pop()
                }
                inlineScopes.push(variable)
                variable.name = newName(variable)
                variablesWithNotMatchingDepth.forEach { it.name = newName(it) }
                variablesWithNotMatchingDepth.clear()
                continue
            }

            // Since the node is an inline function, the depth of all variables inside it is at least one
            val depth = getInlineDepth(name) + 1
            if (depth != inlineScopes.size) {
                variablesWithNotMatchingDepth.add(variable)
            } else {
                variable.name = newName(variable)
            }
        }

        inlineScopesNum = inlineScopeIndex + 1
    }
}

class InlineScopeNumbersRenamer : InlineVariableRenamer {
    override var inlineScopesNum = 0

    override fun renameVariables(node: MethodNode, scopeOffset: Int) {
        val inlineScopesSet = mutableSetOf<Int>()
        for (variable in node.localVariables) {
            val name = variable.name
            variable.start
            val strippedName = name.replace(INLINE_FUN_VAR_SUFFIX, "")
            val scopeNumber = strippedName.substringAfter('\\').toIntOrNull()
            if (scopeNumber != null) {
                inlineScopesSet.add(scopeNumber)
            }
        }

        for (variable in node.localVariables) {
            val name = variable.name
            val scopeNumber = name.substringAfter('\\').toIntOrNull()
            val newScopeNumber =
                if (scopeNumber == null) {
                    0
                } else {
                    1 + if (inlineScopesSet.size != 0) scopeNumber % inlineScopesSet.size else scopeNumber
                }

            variable.name = name.withoutScopeNumber().addScopeNumber(scopeOffset + newScopeNumber)
        }

        inlineScopesNum = inlineScopesSet.size + 1
    }
}

internal fun String.withoutScopeNumber(): String =
    substringBefore(INLINE_SCOPE_NUMBER_SEPARATOR)

private fun String.withoutInlineVarSuffix(): String =
    replace(INLINE_FUN_VAR_SUFFIX, "")

private fun String.addScopeNumber(num: Int): String =
    "$this$INLINE_SCOPE_NUMBER_SEPARATOR$num"

private fun getInlineDepth(variableName: String): Int {
    var endIndex = variableName.length
    var depth = 0

    val suffixLen = INLINE_FUN_VAR_SUFFIX.length
    while (endIndex >= suffixLen) {
        if (variableName.substring(endIndex - suffixLen, endIndex) != INLINE_FUN_VAR_SUFFIX) {
            break
        }

        depth++
        endIndex -= suffixLen
    }

    return depth
}
