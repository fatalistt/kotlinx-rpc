/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.krpc.protobuf

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.Feature

class KRPCProtobufPlugin(
    private val input: CodeGeneratorRequest,
) {
    fun generate(): CodeGeneratorResponse {
        return CodeGeneratorResponse.newBuilder()
            .apply {
                addFile(
                    CodeGeneratorResponse.File.newBuilder()
                        .apply {
                            name = "hello_from_krpc.txt"
                            content = "Hello, world!"
                        }
                        .build()
                )

                supportedFeatures = Feature.FEATURE_PROTO3_OPTIONAL_VALUE.toLong()
            }
            .build()
    }
}
