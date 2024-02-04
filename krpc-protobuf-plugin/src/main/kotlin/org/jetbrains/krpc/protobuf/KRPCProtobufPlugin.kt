/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.krpc.protobuf

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.Feature
import org.jetbrains.krpc.internal.codegen.FileGenerator
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import org.slf4j.simple.SimpleLogger
import org.slf4j.simple.SimpleLoggerFactory

class KRPCProtobufPlugin {
    companion object {
        private const val DEBUG_OUTPUT_OPTION = "debugOutput"
    }

    private var debugOutput: Boolean = false
    private fun setupDebugOutput(path: String) {
        System.setProperty(SimpleLogger.LOG_FILE_KEY, path)
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")
        debugOutput = true
    }

    private val logger: Logger by lazy {
        if (debugOutput) {
            SimpleLoggerFactory().getLogger(KRPCProtobufPlugin::class.java.simpleName)
        } else {
            NOPLogger.NOP_LOGGER
        }
    }

    fun run(input: CodeGeneratorRequest): CodeGeneratorResponse {
        val parameters = input.parameter.split(",")
        parameters.singleOrNull { it.startsWith(DEBUG_OUTPUT_OPTION) }?.let {
            val path = it.removePrefix("$DEBUG_OUTPUT_OPTION=")
            setupDebugOutput(path)
        }

        val files = input.generateKotlinFiles()
            .map { file ->
                CodeGeneratorResponse.File.newBuilder()
                    .apply {
                        val dir = file.packageName?.replace('.', '/')?.plus("/") ?: ""

                        // some filename already contain package (true for Google's default .proto files)
                        val filename = file.filename?.removePrefix(dir) ?: error("File name can not be null")
                        name = "$dir$filename"
                        content = file.build()
                    }
                    .build()
            }

        return CodeGeneratorResponse.newBuilder()
            .apply {
                files.forEach(::addFile)

                supportedFeatures = Feature.FEATURE_PROTO3_OPTIONAL_VALUE.toLong()
            }
            .build()
    }

    private fun CodeGeneratorRequest.generateKotlinFiles(): List<FileGenerator> {
        val interpreter = ProtoToModelInterpreter(logger)
        val model = interpreter.interpretProtocRequest(this)
        val fileGenerator = ModelToKotlinGenerator(model, logger)
        return fileGenerator.generateKotlinFiles()
    }
}
