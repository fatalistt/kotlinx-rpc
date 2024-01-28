/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.krpc.protobuf

import com.google.protobuf.DescriptorProtos.*
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.Feature
import org.jetbrains.krpc.internal.codegen.CodeGenerator
import org.jetbrains.krpc.internal.codegen.FileGenerator
import org.jetbrains.krpc.internal.codegen.file
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import org.slf4j.simple.SimpleLogger
import org.slf4j.simple.SimpleLoggerFactory

// todo
//  type resolution (avoid over qualified types)
//  comments
//  extensions
//  enum aliases
//  maps
//  kmp sources sets
//  platform specific bindings
//  common API
//  DSL builders
//  kotlin_multiple_files, kotlin_package options
//  library proto files
//  services
//    unfolded types overloads
//    nested streams
//

private data class ProtoDependency(
    val protoFilename: String,
    val generatedPackageName: String,
)

class KRPCProtobufPlugin {
    companion object {
        private const val DEBUG_OUTPUT_OPTION = "debugOutput"
    }

    private var debugOutput: Boolean = false
    private fun setupDebugOutput(path: String) {
        System.setProperty(SimpleLogger.LOG_FILE_KEY, path)
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")
        debugOutput = true
    }

    private val logger: Logger by lazy {
        if (debugOutput) {
            SimpleLoggerFactory().getLogger(KRPCProtobufPlugin::class.java.simpleName)
        } else {
            NOPLogger.NOP_LOGGER
        }
    }

    private val dependencies = mutableMapOf<String, ProtoDependency>()

    fun generate(input: CodeGeneratorRequest): CodeGeneratorResponse {
        val parameters = input.parameter.split(",")
        parameters.singleOrNull { it.startsWith(DEBUG_OUTPUT_OPTION) }?.let {
            val path = it.removePrefix("$DEBUG_OUTPUT_OPTION=")
            setupDebugOutput(path)
        }

        val files = input.generateKotlinFiles()
            .map { file ->
                CodeGeneratorResponse.File.newBuilder()
                    .apply {
                        val dir = file.packageName?.replace('.', '/')?.plus("/") ?: ""

                        // some filename already contain package (true for Google's default .proto files)
                        val filename = file.filename?.removePrefix(dir) ?: error("File name can not be null")
                        name = "$dir$filename"
                        content = file.build()
                    }
                    .build()
            }

        return CodeGeneratorResponse.newBuilder()
            .apply {
                files.forEach(::addFile)

                supportedFeatures = Feature.FEATURE_PROTO3_OPTIONAL_VALUE.toLong()
            }
            .build()
    }

    private fun CodeGeneratorRequest.generateKotlinFiles(): List<FileGenerator> {
        return protoFileList.map { descriptor: FileDescriptorProto ->
            additionalImports.clear()
            oneOfFieldMembers.clear()

            descriptor.generateKotlinFile()
        }
    }

    private fun FileDescriptorProto.generateKotlinFile(): FileGenerator {
        return file(logger = logger) {
            filename = kotlinFileName(name)
            packageName = kotlinPackageName(`package`, options)

            dependencyList.forEach { depFilename ->
                val dependency = dependencies[depFilename]
                    ?: error("Unknown dependency $depFilename for $name proto file, wrong topological order")

                importPackage(dependency.generatedPackageName)
            }

            dependencies[name] = ProtoDependency(name, packageName!!)

            generateDeclaredEntities(this@generateKotlinFile)

            additionalImports.forEach {
                import(it)
            }
        }
    }

    private val additionalImports = mutableSetOf<String>()

    private fun kotlinFileName(originalName: String): String {
        return "${originalName.removeSuffix(".proto").fullProtoNameToKotlin(firstLetterUpper = true)}.kt"
    }

    private fun kotlinPackageName(originalPackage: String, options: FileOptions): String {
        return originalPackage
    }

    private fun CodeGenerator.generateDeclaredEntities(fileDescriptor: FileDescriptorProto) {
        fileDescriptor.messageTypeList.forEach { generateMessageDeclaration(it) }
        fileDescriptor.enumTypeList.forEach { generateEnumDeclaration(it) }
        fileDescriptor.serviceList.forEach { generateServiceDeclaration(it) }
    }

    private fun CodeGenerator.generateMessageDeclaration(descriptor: DescriptorProto) {
        val fields = descriptor.fieldList
            .mapNotNull { it.generateFieldDeclaration(descriptor.oneofDeclList) }
            .map { "val $it" }

        val (declaration, modifiers) = if (fields.isEmpty()) {
            CodeGenerator.Declaration.Object to ""
        } else {
            CodeGenerator.Declaration.Class to "data"
        }

        clazz(
            name = descriptor.name.fullProtoNameToKotlin(firstLetterUpper = true),
            modifiers = modifiers,
            constructorArgs = fields,
            declaration = declaration
        ) {
            generateOneOfDeclarations(descriptor)

            descriptor.nestedTypeList.forEach { nested ->
                generateMessageDeclaration(nested)
            }

            descriptor.enumTypeList.forEach { enum ->
                generateEnumDeclaration(enum)
            }
        }
    }

    private val oneOfFieldMembers = mutableMapOf<Int, MutableList<FieldDescriptorProto>>()

    private fun FieldDescriptorProto.generateFieldDeclaration(oneOfList: MutableList<OneofDescriptorProto>): String? {
        if (hasOneofIndex()) {
            val oneOfName = oneOfList[oneofIndex].name

            return when {
                // effectively optional
                // https://github.com/protocolbuffers/protobuf/blob/main/docs/implementing_proto3_presence.md#updating-a-code-generator
                oneOfName == "_$name" -> {
                    "${typeFqName()}?"
                }

                oneOfFieldMembers[oneofIndex] == null -> {
                    oneOfFieldMembers[oneofIndex] = mutableListOf<FieldDescriptorProto>().also {
                        it.add(this)
                    }

                    oneOfName.fullProtoNameToKotlin(firstLetterUpper = true)
                }

                else -> {
                    oneOfFieldMembers[oneofIndex]!!.add(this)
                    null
                }
            }?.let { "${oneOfName.removePrefix("_").fullProtoNameToKotlin()}: $it" }
        }

        return "${name.fullProtoNameToKotlin()}: ${typeFqName()}"
    }

    private fun FieldDescriptorProto.typeFqName(): String {
        return when {
            hasTypeName() -> {
                typeName
                    // from https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/descriptor.proto
                    // if the name starts with a '.', it is fully-qualified.
                    .substringAfter('.')
                    .fullProtoNameToKotlin(firstLetterUpper = true)
            }

            else -> {
                primitiveType()
            }
        }.let { wrapWithLabel(it) }
    }

    @Suppress("detekt.CyclomaticComplexMethod")
    private fun FieldDescriptorProto.primitiveType(): String {
        return when (type) {
            Type.TYPE_STRING -> "String"
            Type.TYPE_BYTES -> "ByteArray"
            Type.TYPE_BOOL -> "Boolean"
            Type.TYPE_FLOAT -> "Float"
            Type.TYPE_DOUBLE -> "Double"
            Type.TYPE_INT32 -> "Int"
            Type.TYPE_INT64 -> "Long"
            Type.TYPE_UINT32 -> "UInt"
            Type.TYPE_UINT64 -> "ULong"
            Type.TYPE_FIXED32 -> "UInt"
            Type.TYPE_FIXED64 -> "ULong"
            Type.TYPE_SINT32 -> "Int"
            Type.TYPE_SINT64 -> "Long"
            Type.TYPE_SFIXED32 -> "Int"
            Type.TYPE_SFIXED64 -> "Long"

            Type.TYPE_ENUM, Type.TYPE_MESSAGE, Type.TYPE_GROUP, null ->
                error("Expected to find primitive type, instead got $type with name '$typeName'")
        }
    }

    private fun FieldDescriptorProto.wrapWithLabel(rawType: String): String {
        return when (label) {
            Label.LABEL_REPEATED -> "List<$rawType>"
            // LABEL_OPTIONAL is not actually optional in proto3.
            // Actual optional is oneOf with one option and same name
            else -> rawType
        }
    }

    private fun CodeGenerator.generateOneOfDeclarations(messageDescriptor: DescriptorProto) {
        messageDescriptor.oneofDeclList.forEachIndexed { index, descriptor ->
            val interfaceName = descriptor.name.fullProtoNameToKotlin(firstLetterUpper = true)

            val fields = oneOfFieldMembers[index] ?: return@forEachIndexed
            clazz(interfaceName, "sealed", declaration = CodeGenerator.Declaration.Interface) {
                fields.forEach { field ->
                    clazz(
                        name = field.name.fullProtoNameToKotlin(firstLetterUpper = true),
                        modifiers = "value",
                        constructorArgs = listOf("val value: ${field.typeFqName()}"),
                        annotations = listOf("@JvmInline"),
                        superTypes = listOf(interfaceName),
                    )

                    additionalImports.add("kotlin.jvm.JvmInline")
                }
            }
        }
    }

    private fun CodeGenerator.generateEnumDeclaration(descriptor: EnumDescriptorProto) {
        clazz(descriptor.name.fullProtoNameToKotlin(firstLetterUpper = true), "enum") {
            // todo aliases
            code(descriptor.valueList.joinToString(", ") { enumEntry ->
                enumEntry.name
            })
        }
    }

    private fun CodeGenerator.generateServiceDeclaration(descriptor: ServiceDescriptorProto) {

    }

    private fun String.fullProtoNameToKotlin(firstLetterUpper: Boolean = false): String {
        val lastDelimiterIndex = indexOfLast { it == '.' || it == '/' }
        return if (lastDelimiterIndex != -1) {
            val packageName = substring(0, lastDelimiterIndex)
            val name = substring(lastDelimiterIndex + 1)
            val delimiter = this[lastDelimiterIndex]
            return "$packageName$delimiter${name.simpleProtoNameToKotlin(true)}"
        } else {
            simpleProtoNameToKotlin(firstLetterUpper)
        }
    }

    private val snakeRegExp = "_[a-z]".toRegex()

    private fun String.snakeToCamelCase(): String {
        return replace(snakeRegExp) { it.value.last().uppercase() }
    }

    private fun String.simpleProtoNameToKotlin(firstLetterUpper: Boolean = false): String {
        return snakeToCamelCase().run {
            if (firstLetterUpper) {
                replaceFirstChar { it.uppercase() }
            } else {
                this
            }
        }
    }
}
