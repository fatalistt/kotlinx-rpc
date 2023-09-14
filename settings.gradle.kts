pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "kRPC"

include(":codegen:codegen-test")
include(":codegen:ir-extension")
include(":codegen:sources-generation")
include(":runtime")
include(":testJvmModule")
