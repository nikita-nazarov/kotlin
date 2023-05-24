/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import gnu.trove.TIntIntHashMap
import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.*
import kotlin.math.max

const val KOTLIN_STRATA_NAME = "Kotlin"
const val KOTLIN_DEBUG_STRATA_NAME = "KotlinDebug"
const val KOTLIN_INLINE_DEBUG_STRATA_NAME = "KotlinInlineDebug"

object SMAPBuilder {
    fun build(fileMappings: List<FileMapping>, inlineScopes: List<InlineScopeInfo>, scopeMappings: List<ScopeMapping>, backwardsCompatibleSyntax: Boolean): String? {
        if (fileMappings.isEmpty()) {
            return null
        }

        val debugMappings = linkedMapOf<Pair<String, String>, FileMapping>()
        for (fileMapping in fileMappings) {
            for ((_, dest, range, callSite) in fileMapping.lineMappings) {
                callSite?.let { (line, file, path) ->
                    debugMappings.getOrPut(file to path) { FileMapping(file, path) }.mapNewInterval(line, dest, range)
                }
            }
        }

        // Old versions of kotlinc and the IDEA plugin have incorrect implementations of SMAPParser:
        //   1. they require *E between strata, which is not correct syntax according to JSR-045;
        //   2. in KotlinDebug, they use `1#2,3:4` to mean "map lines 4..6 to line 1 of #2", when in reality (and in
        //      the non-debug stratum) this maps lines 4..6 to lines 1..3. The correct syntax is `1#2:4,3`.
        val defaultStrata = fileMappings.toSMAP(KOTLIN_STRATA_NAME, mapToFirstLine = false)
        val debugStrata = debugMappings.values.toSMAP(KOTLIN_DEBUG_STRATA_NAME, mapToFirstLine = !backwardsCompatibleSyntax)
        val inlineDebugStrata =
            "${SMAP.STRATA_SECTION} $KOTLIN_INLINE_DEBUG_STRATA_NAME\n" +
                    "${SMAP.FILE_SECTION}\n${inlineScopes.toSMAP()}" +
                    "${SMAP.LINE_SECTION}\n${scopeMappings.map { it.toSMAP() }.joinToString("")}"

        if (backwardsCompatibleSyntax && defaultStrata.isNotEmpty() && debugStrata.isNotEmpty()) {
            return "SMAP\n${fileMappings[0].name}\n$KOTLIN_STRATA_NAME\n$defaultStrata${SMAP.END}\n$debugStrata${SMAP.END}$inlineDebugStrata${SMAP.END}\n"
        }
        return "SMAP\n${fileMappings[0].name}\n$KOTLIN_STRATA_NAME\n$defaultStrata$debugStrata$inlineDebugStrata${SMAP.END}\n"
    }

    private fun Collection<FileMapping>.toSMAP(stratumName: String, mapToFirstLine: Boolean): String = if (isEmpty()) "" else
        "${SMAP.STRATA_SECTION} $stratumName\n" +
                "${SMAP.FILE_SECTION}\n${mapIndexed { id, file -> file.toSMAPFile(id + 1) }.joinToString("")}" +
                "${SMAP.LINE_SECTION}\n${mapIndexed { id, file -> file.toSMAPMapping(id + 1, mapToFirstLine) }.joinToString("")}"

    private fun List<InlineScopeInfo>.toSMAP(): String =
        buildString {
            for ((i, scope) in this@toSMAP.withIndex()) {
                append("$i ")
                val callerId = scope.callerScopeId
                val callerIdStr = if (callerId == -1) "" else callerId.toString()
                append("${scope.name}/$callerIdStr/${scope.callSiteLineNumber}\n")
            }
        }

    private fun ScopeMapping.toSMAP(): String =
        buildString {
            val ranges = lineNumbers.toRanges()
            for (range in ranges) {
                val size = range.endInclusive - range.start + 1
                append("${range.start}#${scopeNumber},$size:0\n")
            }
        }

    private fun List<Int>.toRanges(): List<ClosedRange<Int>> {
        if (isEmpty()) return emptyList()
        val numToRange = hashMapOf<Int, IntRange?>()
        for (num in this) {
            val range = numToRange[num - 1]
            if (range != null) {
                numToRange[num] = IntRange(range.start, num)
                numToRange[num - 1] = null
            } else {
                numToRange[num] = IntRange(num, num)
            }
        }
        return numToRange.values.filterNotNull()
    }

    private fun RangeMapping.toSMAP(fileId: Int, oneLine: Boolean): String =
        if (range == 1) "$source#$fileId:$dest\n" else if (oneLine) "$source#$fileId:$dest,$range\n" else "$source#$fileId,$range:$dest\n"

    private fun FileMapping.toSMAPFile(id: Int): String =
        "+ $id $name\n$path\n"

    private fun FileMapping.toSMAPMapping(id: Int, mapToFirstLine: Boolean): String =
        lineMappings.joinToString("") { it.toSMAP(id, mapToFirstLine) }
}

class SourceMapCopier(val parent: SourceMapper, private val smap: SMAP, val callSite: SourcePosition? = null) {
    private val visitedLines = TIntIntHashMap()
    private var lastVisitedRange: RangeMapping? = null

    fun mapLineNumber(lineNumber: Int): Int {
        val mappedLineNumber = visitedLines.get(lineNumber)
        if (mappedLineNumber > 0) {
            return mappedLineNumber
        }

        val range = lastVisitedRange?.takeIf { lineNumber in it } ?: smap.findRange(lineNumber) ?: return -1
        lastVisitedRange = range
        val newLineNumber = parent.mapLineNumber(range.mapDestToSource(lineNumber), callSite ?: range.callSite)

        visitedLines.put(lineNumber, newLineNumber)
        return newLineNumber
    }
}

data class SourcePosition(val line: Int, val file: String, val path: String)
data class InlineScopeInfo(val name: String, var callerScopeId: Int, val callSiteLineNumber: Int)
data class ScopeMapping(var scopeNumber: Int, val lineNumbers: List<Int>)

class SourceMapper(val sourceInfo: SourceInfo?) {
    private var maxUsedValue: Int = sourceInfo?.linesInFile ?: 0
    private var fileMappings: LinkedHashMap<Pair<String, String>, FileMapping> = linkedMapOf()

    val inlineScopes = mutableListOf<InlineScopeInfo>()
    val scopeMappings = mutableListOf<ScopeMapping>()

    val resultMappings: List<FileMapping>
        get() = fileMappings.values.toList()

    companion object {
        const val FAKE_FILE_NAME = "fake.kt"
        const val FAKE_PATH = "kotlin/jvm/internal/FakeKt"
        const val LOCAL_VARIABLE_INLINE_ARGUMENT_SYNTHETIC_LINE_NUMBER = 1
    }

    init {
        sourceInfo?.let { sourceInfo ->
            // If 'sourceFileName' is null we are dealing with a synthesized class
            // (e.g., multi-file class facade with multiple parts). Such classes
            // only have synthetic debug information and we use a fake file name.
            val sourceFileName = sourceInfo.sourceFileName ?: FAKE_FILE_NAME
            // Explicitly map the file to itself -- we'll probably need a lot of lines from it, so this will produce fewer ranges.
            getOrRegisterNewSource(sourceFileName, sourceInfo.pathOrCleanFQN)
                .mapNewInterval(1, 1, sourceInfo.linesInFile)
        }
    }

    val isTrivial: Boolean
        get() = maxUsedValue == 0 || maxUsedValue == sourceInfo?.linesInFile

    private fun getOrRegisterNewSource(name: String, path: String): FileMapping =
        fileMappings.getOrPut(name to path) { FileMapping(name, path) }

    fun mapLineNumber(inlineSource: SourcePosition, inlineCallSite: SourcePosition?): Int {
        val fileMapping = getOrRegisterNewSource(inlineSource.file, inlineSource.path)
        val mappedLineIndex = fileMapping.mapNewLineNumber(inlineSource.line, maxUsedValue, inlineCallSite)
        maxUsedValue = max(maxUsedValue, mappedLineIndex)
        return mappedLineIndex
    }

    fun mapSyntheticLineNumber(id: Int): Int {
        return mapLineNumber(SourcePosition(id, FAKE_FILE_NAME, FAKE_PATH), null)
    }
}

class SMAP(val fileMappings: List<FileMapping>) {
    // assuming disjoint line mappings (otherwise binary search can't be used anyway)
    private val intervals = fileMappings.flatMap { it.lineMappings }.sortedBy { it.dest }

    val inlineScopes = mutableListOf<InlineScopeInfo>()
    val scopeMappings = mutableListOf<ScopeMapping>()

    fun findRange(lineNumber: Int): RangeMapping? {
        val index = intervals.binarySearch { if (lineNumber in it) 0 else it.dest - lineNumber }
        return if (index < 0) null else intervals[index]
    }

    companion object {
        const val FILE_SECTION = "*F"
        const val LINE_SECTION = "*L"
        const val STRATA_SECTION = "*S"
        const val END = "*E"
    }
}

data class SMAPAndMethodNode(val node: MethodNode, val classSMAP: SMAP)

class FileMapping(val name: String, val path: String) {
    val lineMappings = arrayListOf<RangeMapping>()

    fun toSourceInfo(): SourceInfo =
        SourceInfo(name, path, lineMappings.fold(0) { result, mapping -> max(result, mapping.source + mapping.range - 1) })

    fun mapNewLineNumber(source: Int, currentIndex: Int, callSite: SourcePosition?): Int {
        // Save some space in the SMAP by reusing (or extending if it's the last one) the existing range.
        // TODO some *other* range may already cover `source`; probably too slow to check them all though.
        //   Maybe keep the list ordered by `source` and use binary search to locate the closest range on the left?
        val mapping = lineMappings.lastOrNull()?.takeIf { it.canReuseFor(source, currentIndex, callSite) }
            ?: lineMappings.firstOrNull()?.takeIf { it.canReuseFor(source, currentIndex, callSite) }
            ?: mapNewInterval(source, currentIndex + 1, 1, callSite)
        mapping.range = max(mapping.range, source - mapping.source + 1)
        return mapping.mapSourceToDest(source)
    }

    private fun RangeMapping.canReuseFor(newSource: Int, globalMaxDest: Int, newCallSite: SourcePosition?): Boolean =
        callSite == newCallSite && (newSource - source) in 0 until range + (if (globalMaxDest in this) 10 else 0)

    fun mapNewInterval(source: Int, dest: Int, range: Int, callSite: SourcePosition? = null): RangeMapping =
        RangeMapping(source, dest, range, callSite, parent = this).also { lineMappings.add(it) }
}

data class RangeMapping(val source: Int, val dest: Int, var range: Int, val callSite: SourcePosition?, val parent: FileMapping) {
    operator fun contains(destLine: Int): Boolean =
        dest <= destLine && destLine < dest + range

    fun hasMappingForSource(sourceLine: Int): Boolean =
        source <= sourceLine && sourceLine < source + range

    fun mapDestToSource(destLine: Int): SourcePosition =
        SourcePosition(source + (destLine - dest), parent.name, parent.path)

    fun mapSourceToDest(sourceLine: Int): Int =
        dest + (sourceLine - source)
}

val RangeMapping.toRange: IntRange
    get() = dest until dest + range
