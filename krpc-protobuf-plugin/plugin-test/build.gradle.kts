/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.conventions.jvm)
    id("com.google.protobuf")
}

dependencies {
    implementation(libs.coroutines.core)

    implementation("io.grpc:grpc-stub:1.57.2")
    implementation("io.grpc:grpc-protobuf:1.57.2")
    implementation("com.google.protobuf:protobuf-java-util:3.24.1")
    implementation("io.grpc:grpc-kotlin-stub:1.3.1")
}

val pluginProject = project(":krpc-protobuf-plugin")

val krpcProtobufPluginJarTask = pluginProject.tasks.jar

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.1"
    }

    plugins {
        create("krpc") {
            val pluginBuildDir = pluginProject.layout.buildDirectory.get().asFile.absolutePath
            path = "$pluginBuildDir/libs/krpc-protobuf-plugin-$version.jar"
        }

        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.57.2"
        }

        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.1:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("krpc") {
                    option("debugOutput=build/krpc-protobuf-plugin.log")
                }
                create("grpc")
                create("grpckt")
            }

            task.dependsOn(krpcProtobufPluginJarTask)
        }
    }
}

kotlin {
    explicitApi = ExplicitApiMode.Disabled
}
