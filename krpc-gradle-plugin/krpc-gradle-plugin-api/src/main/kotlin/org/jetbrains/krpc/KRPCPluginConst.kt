package org.jetbrains.krpc

import org.jetbrains.kotlin.gradle.utils.loadPropertyFromResources
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

object KRPCPluginConst {
    const val KSP_PLUGIN_ID = "com.google.devtools.ksp"

    const val KRPC_PLUGIN_ID = "org.jetbrains.krpc.plugin"
    const val KRPC_GROUP_ID = "org.jetbrains.krpc"
    const val KRPC_COMPILER_PLUGIN_ARTIFACT_ID = "krpc-compiler-plugin"
    const val KRPC_KSP_PLUGIN_ARTIFACT_ID = "krpc-ksp-plugin"
    const val KRPC_BOM_ARTIFACT_ID = "krpc-bom"

    const val KRPC_CONFIG_PROPERTY = "krpc-plugin-config"

    const val KRPC_INTERNAL_DEVELOPMENT_PROPERTY = "krpc.plugin.internalDevelopment"

    val kotlinVersion by lazy { loadKotlinVersion() }

    val krpcFullVersion by lazy {
        "${kotlinVersion}-$PLUGIN_VERSION"
    }

    /**
     * Same as [getKotlinPluginVersion] but without logger
     *
     * Cannot use with [Project] because [krpcFullVersion] is called in [KRPCKotlinCompilerPlugin]
     */
    private fun loadKotlinVersion(): String {
        return object {}
            .loadPropertyFromResources("project.properties", "project.version")
    }
}
