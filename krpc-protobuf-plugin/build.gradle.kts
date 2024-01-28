/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.conventions.jvm)
}

dependencies {
    implementation(project(":krpc-utils"))
    implementation(project(":krpc-utils"))

    implementation("com.google.protobuf:protobuf-java:3.24.1")

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.jetbrains.krpc.protobuf.MainKt"

        // for plugin-test module to locate dependencies when running protoc plugin
        attributes["Class-Path"] = configurations.runtimeClasspath.get()
            .joinToString(" ") { it.absolutePath }
    }
}

kotlin {
    explicitApi = ExplicitApiMode.Disabled
}
