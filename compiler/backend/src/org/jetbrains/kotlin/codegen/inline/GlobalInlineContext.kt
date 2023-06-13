/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.utils.threadLocal
import java.util.*

data class InlineScope(val functionId: String, val lineNumbers: List<Int>, val callSiteLineNumber: Int, val parentScopeId: String?) {
    fun isInlineLambdaScope(): Boolean = functionId.contains("lambda")
}

class GlobalInlineContext(private val diagnostics: DiagnosticSink) {
    // Ordered set of declarations and inline calls being generated right now.
    // No call in it should point to a declaration that's before it in the stack.
    private val inlineCallsAndDeclarations by threadLocal { LinkedList<Any? /* CallableDescriptor | PsiElement? */>() }
    private val inlineDeclarationSet by threadLocal { mutableSetOf<CallableDescriptor>() }

    private val typesUsedInInlineFunctions by threadLocal { LinkedList<MutableSet<String>>() }

    val inlineFunctionToScopes: HashMap<String, MutableList<InlineScope>> = hashMapOf()

    fun enterDeclaration(descriptor: CallableDescriptor) {
        assert(descriptor.original !in inlineDeclarationSet) { "entered inlining cycle on $descriptor" }
        inlineDeclarationSet.add(descriptor.original)
        inlineCallsAndDeclarations.add(descriptor.original)
    }

    fun exitDeclaration() {
        inlineDeclarationSet.remove(inlineCallsAndDeclarations.removeLast())
    }

    fun enterIntoInlining(callee: CallableDescriptor?, element: PsiElement?): Boolean {
        if (callee != null && callee.original in inlineDeclarationSet) {
            element?.let { diagnostics.report(Errors.INLINE_CALL_CYCLE.on(it, callee.original)) }
            for ((call, callTarget) in inlineCallsAndDeclarations.dropWhile { it != callee.original }.zipWithNext()) {
                // Every call element should be followed by the callee's descriptor.
                if (call is PsiElement && callTarget is CallableDescriptor) {
                    diagnostics.report(Errors.INLINE_CALL_CYCLE.on(call, callTarget))
                }
            }
            return false
        }
        inlineCallsAndDeclarations.add(element)
        typesUsedInInlineFunctions.push(hashSetOf())
        return true
    }

    fun exitFromInlining() {
        inlineCallsAndDeclarations.removeLast()
        val pop = typesUsedInInlineFunctions.pop()
        typesUsedInInlineFunctions.peek()?.addAll(pop)
    }

    fun recordTypeFromInlineFunction(type: String) = typesUsedInInlineFunctions.peek().add(type)

    fun isTypeFromInlineFunction(type: String) = typesUsedInInlineFunctions.peek().contains(type)
}

fun List<InlineScope>.arranged(): List<InlineScope> {
    val nameToIndex = hashMapOf<String, Int>()
    for ((i, scope) in withIndex()) {
        nameToIndex[scope.functionId] = i
    }

    val children = Array<MutableList<Int>>(size) { mutableListOf() }
    val roots = mutableListOf<Int>()
    for ((i, scope) in withIndex()) {
        val parent = scope.parentScopeId
        if (parent != null) {
            children[nameToIndex[parent]!!].add(i)
        } else {
            roots.add(i)
        }
    }

    val ids = Array<Int>(size) { 0 }
    val visited = Array<Boolean>(size) { false }
    var currentId = 0
    fun dfs(v: Int) {
        ids[v] = currentId
        visited[v] = true
        currentId += 1

        for (child in children[v]) {
            if (!visited[child]) {
                dfs(child)
            }
        }
    }

    for (i in roots) {
        dfs(i)
    }

    val sortedScopes = withIndex().sortedBy { (i, _) -> ids[i] }.map { it.value }
    return sortedScopes
}
