/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    alias(libs.plugins.conventions.jvm)
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.24.1")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.jetbrains.krpc.protobuf.MainKt"

        // for plugin-test module to locate dependencies when running protoc plugin
        attributes["Class-Path"] = configurations.runtimeClasspath.get()
            .joinToString(" ") { it.absolutePath }
    }
}
