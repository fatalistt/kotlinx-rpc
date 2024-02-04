/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.krpc.protobuf.model

data class MessageDeclaration(
    val name: FqName,
    val actualFields: List<FieldDeclaration>, // excludes oneOf fields, but includes oneOf itself
    val oneOfDeclarations: List<OneOfDeclaration>,
    val enumDeclarations: List<EnumDeclaration>,
    val nestedDeclarations: List<MessageDeclaration>,
    val deprecated: Boolean,
    val doc: String?,
)
