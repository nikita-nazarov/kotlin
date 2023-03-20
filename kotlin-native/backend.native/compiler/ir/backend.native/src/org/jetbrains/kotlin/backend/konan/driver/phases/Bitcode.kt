/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.*
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.collectLLVMModules
import org.jetbrains.kotlin.backend.konan.insertAliasToEntryPoint
import org.jetbrains.kotlin.backend.konan.llvm.getName
import org.jetbrains.kotlin.backend.konan.objcexport.objCExportTopLevelNamePrefix
import kotlin.random.Random

/**
 * Write in-memory LLVM module to filesystem as a bitcode.
 *
 * TODO: Use explicit input (LLVMModule) and output (File)
 *  after static driver removal.
 */

internal val WriteBitcodeFilePhase = createSimpleNamedCompilerPhase(
        "WriteBitcodeFile",
        "Write bitcode file",
        outputIfNotEnabled = { _, _, _, _ -> }
) { context: NativeGenerationState, _: Unit ->
//    val output = context.tempFiles.nativeBinaryFileName
    context.bitcodeFileName = context.tempFiles.bitcodeDump.path
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    if (context.config.readFrameworkBitcode) {
        insertAliasToEntryPoint(context)
    }

    LLVMWriteBitcodeToFile(context.llvm.module, context.bitcodeFileName)
    val entryPointName = context.config.entryPointName
    var entryPoint: LLVMValueRef? = null
    if (!context.config.readFrameworkBitcode) {
        for (module in collectLLVMModules(context)) {
            if (entryPoint == null) {
                LLVMGetNamedFunction(module, entryPointName)?.let {
                    entryPoint = it
                    LLVMAddAlias(module, LLVMTypeOf(entryPoint)!!, entryPoint, "main")
                }
            }

            val name = module.getName().substringAfterLast("/").substringBefore(".")
            LLVMWriteBitcodeToFile(module, "tmp/$name${context.random.nextInt()}.bc")
        }
    }
}
