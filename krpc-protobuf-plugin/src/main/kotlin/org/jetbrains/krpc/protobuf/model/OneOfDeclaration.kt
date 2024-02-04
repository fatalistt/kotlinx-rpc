/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.krpc.protobuf.model

data class OneOfDeclaration(
    val name: FqName,
    val variants: List<FieldDeclaration>,
)
