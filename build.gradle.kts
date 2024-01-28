/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.krpc) apply false
    alias(libs.plugins.atomicfu) apply false
    alias(libs.plugins.detekt) apply false
    id("com.google.protobuf") version "0.9.4" apply false
    alias(libs.plugins.binary.compatibility.validator)
}

object Const {
    const val KRPC_COMPILER_PLUGIN_MODULE_NAME = "krpc-compiler-plugin"
    const val INTERNAL_KRPC_API_ANNOTATION = "org.jetbrains.krpc.internal.InternalKRPCApi"
}

apiValidation {
    val compilerPluginModules = subprojects.single { it.name == Const.KRPC_COMPILER_PLUGIN_MODULE_NAME }.let {
        it.subprojects.map { sub -> sub.name }
    } + Const.KRPC_COMPILER_PLUGIN_MODULE_NAME

    ignoredProjects.addAll(
        listOf(
            "codegen-tests-jvm",
            "codegen-tests-mpp",
            "krpc-runtime-test",
            "krpc-utils",
            "krpc-utils-service-loader",
            "krpc-ksp-plugin",
            "krpc-protobuf-plugin",
            "plugin-test", // protobuf
        ) + compilerPluginModules
    )

    nonPublicMarkers.add(Const.INTERNAL_KRPC_API_ANNOTATION)
}

allprojects {
    group = "org.jetbrains.krpc"
    version = rootProject.libs.versions.krpc.full.get()
}

println("kRPC project version: $version")

// If the prefix of the kPRC version is not Kotlin gradle plugin version - you have a problem :)
// Probably some dependency brings kotlin with higher version.
// To mitigate so, please refer to `gradle/kotlin-version-lookup.json`
// and it's usage in `gradle-settings-conventions/src/main/kotlin/settings-conventions.settings.gradle.kts`
val kotlinVersion = getKotlinPluginVersion()
if (!version.toString().startsWith(kotlinVersion)) {
    error("Kotlin gradle plugin version mismatch: kRPC version: $version, Kotlin gradle plugin version: $kotlinVersion")
}

// necessary for CI js tests
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().ignoreScripts = false
}
