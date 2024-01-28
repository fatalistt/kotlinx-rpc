/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.krpc.protobuf

import com.google.protobuf.compiler.PluginProtos

fun main() {
    val inputBytes = System.`in`.readBytes()
    val request = PluginProtos.CodeGeneratorRequest.parseFrom(inputBytes)
    val plugin = KRPCProtobufPlugin()
    val output: PluginProtos.CodeGeneratorResponse = plugin.generate(request)
    output.writeTo(System.out)
}
