package net.pwall.json.schema.codegen

import java.io.File

import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchemaException
import net.pwall.json.schema.parser.Parser
import net.pwall.json.schema.validation.ConstValidator
import net.pwall.json.schema.validation.EnumValidator
import net.pwall.json.schema.validation.FormatValidator
import net.pwall.json.schema.validation.ItemValidator
import net.pwall.json.schema.validation.NumberValidator
import net.pwall.json.schema.validation.PatternValidator
import net.pwall.json.schema.validation.PropertyValidator
import net.pwall.json.schema.validation.RefValidator
import net.pwall.json.schema.validation.RequiredValidator
import net.pwall.json.schema.validation.StringValidator
import net.pwall.json.schema.validation.TypeValidator
import net.pwall.mustache.Template

class CodeGenerator(
        var templates: String = "kotlin",
        var suffix: String = "kt",
        var basePackageName: String? = null,
        var baseDirectoryName: String = ".",
        var derivePackageFromStructure: Boolean = true
) {

    var schemaParser: Parser? = null

    private val defaultSchemaParser: Parser by lazy {
        Parser()
    }

    private val actualSchemaParser: Parser
        get() = schemaParser ?: defaultSchemaParser

    var templateParser: net.pwall.mustache.parser.Parser? = null

    private val defaultTemplateParser: net.pwall.mustache.parser.Parser by lazy {
        net.pwall.mustache.parser.Parser().also {
            it.resolvePartial = { name ->
                CodeGenerator::class.java.getResourceAsStream("/$templates/$name.mustache").reader()
            }
        }
    }

    private val actualTemplateParser: net.pwall.mustache.parser.Parser
        get() = templateParser ?: defaultTemplateParser

    var template: Template? = null

    private val defaultTemplate: Template by lazy {
        actualTemplateParser.parse(actualTemplateParser.resolvePartial("class"))
    }

    private val actualTemplate: Template
        get() = template ?: defaultTemplate

    var outputResolver: OutputResolver? = null

    private val defaultOutputResolver: OutputResolver = { baseDirectory, subDirectories, className, suffix ->
        var dir = checkDirectory(File(baseDirectory), "output directory")
        subDirectories.forEach { dir = checkDirectory(File(dir, it), "output directory") }
        File(dir, "$className.$suffix").writer()
    }

    private val actualOutputResolver: OutputResolver
        get() = outputResolver ?: defaultOutputResolver

    fun setTemplateDirectory(directory: File, suffix: String = "mustache") {
        when {
            directory.isFile -> throw JSONSchemaException("Template directory must be a directory")
            directory.isDirectory -> {}
            else -> throw JSONSchemaException("Error accessing template directory")
        }
        templateParser = net.pwall.mustache.parser.Parser().also {
            it.resolvePartial = { name ->
                File(directory, "$name.$suffix").reader()
            }
        }
    }

    fun generate(inputDir: File, subDirectories: List<String> = emptyList()) {
        val parser = actualSchemaParser
        parser.preLoad(inputDir)
        when {
            inputDir.isFile -> generate(parser.parse(inputDir), subDirectories)
            inputDir.isDirectory -> generate(parser, inputDir, subDirectories)
        }
    }

    private fun generate(parser: Parser, inputDir: File, subDirectories: List<String>) {
        inputDir.listFiles()?.forEach {
            when {
                it.isDirectory -> { generate(parser, it, subDirectories + it.name.mapDirectoryName()) }
                it.isFile -> { generate(parser.parse(it), subDirectories) }
            }
        }
    }

    private fun String.mapDirectoryName(): String = StringBuilder().also {
        for (ch in this)
            if (ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9')
                it.append(ch)
    }.toString()

    fun generate(schema: JSONSchema, subDirectories: List<String>) {
        process(schema)?.let { constraints ->
            var packageName = basePackageName
            if (derivePackageFromStructure)
                subDirectories.forEach { packageName = if (packageName.isNullOrEmpty()) it else "$packageName.$it" }
            constraints.packageName = packageName
            constraints.description = schema.description
            val className = constraints.nameFromURI ?: "GeneratedClass"
            actualOutputResolver(baseDirectoryName, subDirectories, className, suffix).use {
                actualTemplate.processTo(it, constraints)
            }
        }
    }

    private fun process(schema: JSONSchema): Constraints? {
        val constraints = Constraints(schema)
        processSchema(schema, constraints)
        if (!constraints.isObject)
            return null
        analyseConstraints(constraints, constraints)
        return constraints
    }

    private fun analyseConstraints(parentConstraints: Constraints, constraints: Constraints) {
        constraints.properties.forEach { property ->
            if (property.isObject) {
                // TODO - how do we handle nested classes?
                // we always generate as nested classes for now, look at alternatives later
                parentConstraints.nestedClasses.add(property)
                analyseConstraints(parentConstraints, property)
                property.localTypeName = property.capitalisedName
                if (property.name !in constraints.required)
                    property.nullable = true
            }
            if (property.isArray) {
                parentConstraints.systemClasses.addOnce(Constraints.SystemClass.LIST)
                property.arrayItems?.let { analyseConstraints(parentConstraints, it) }
            }
            if (property.isInt) {
                if (property.maximum != null || property.exclusiveMaximum != null || property.minimum != null ||
                        property.exclusiveMaximum != null || property.multipleOf != null)
                    parentConstraints.validationsPresent = true
            }
            if (property.isString) {
                when (property.format) {
                    FormatValidator.FormatType.EMAIL,
                    FormatValidator.FormatType.HOSTNAME -> {
                        parentConstraints.systemClasses.addOnce(Constraints.SystemClass.VALIDATION)
                        parentConstraints.validationsPresent = true
                    }
                    FormatValidator.FormatType.DATE_TIME -> {
                        parentConstraints.systemClasses.addOnce(Constraints.SystemClass.DATE_TIME)
                        property.systemClass = Constraints.SystemClass.DATE_TIME
                    }
                    FormatValidator.FormatType.DATE -> {
                        parentConstraints.systemClasses.addOnce(Constraints.SystemClass.DATE)
                        property.systemClass = Constraints.SystemClass.DATE
                    }
                    FormatValidator.FormatType.TIME -> {
                        parentConstraints.systemClasses.addOnce(Constraints.SystemClass.TIME)
                        property.systemClass = Constraints.SystemClass.TIME
                    }
                    FormatValidator.FormatType.DURATION -> {
                        parentConstraints.systemClasses.addOnce(Constraints.SystemClass.DURATION)
                        property.systemClass = Constraints.SystemClass.DURATION
                    }
                    FormatValidator.FormatType.UUID -> {
                        parentConstraints.systemClasses.addOnce(Constraints.SystemClass.UUID)
                        property.systemClass = Constraints.SystemClass.UUID
                    }
                    else -> {}
                }
            }
            if (property.isDecimal) {
                parentConstraints.systemClasses.addOnce(Constraints.SystemClass.DECIMAL)
                property.systemClass = Constraints.SystemClass.DECIMAL
            }
            // TODO - more validation types (enum, const, pattern); nested structures
        }
    }

    private fun <T: Any> MutableList<T>.addOnce(entry: T) {
        if (entry !in this)
            add(entry)
    }

    private fun processSchema(schema: JSONSchema, constraints: Constraints) {
        when (schema) {
            is JSONSchema.True -> throw JSONSchemaException("Can't generate code for \"true\" schema")
            is JSONSchema.False -> throw JSONSchemaException("Can't generate code for \"false\" schema")
            is JSONSchema.Not -> throw JSONSchemaException("Can't generate code for \"not\" schema")
            is JSONSchema.ArrayValidator -> processArrayValidator(schema, constraints)
            is JSONSchema.Validator -> processValidator(schema, constraints)
            is JSONSchema.General -> processGeneral(schema, constraints)
        }
    }

    private fun processArrayValidator(arrayValidator: JSONSchema.ArrayValidator, constraints: Constraints) {
        if (arrayValidator.name != "allOf") // can only handle allOf currently
            throw JSONSchemaException("Can't generate code for \"${arrayValidator.name}\" schema")
        arrayValidator.array.forEach { processSchema(it, constraints) }
    }

    private fun processValidator(validator: JSONSchema.Validator, constraints: Constraints) {
        when (validator) {
            is ConstValidator -> processConstValidator(validator, constraints)
            is EnumValidator -> processEnumValidator(validator, constraints)
            is FormatValidator -> processFormatValidator(validator, constraints)
            is ItemValidator -> processSchema (validator.itemSchema,
                    constraints.arrayItems ?: Constraints(validator.itemSchema).also { constraints.arrayItems = it })
            is NumberValidator -> processNumberValidator(validator, constraints)
            is PatternValidator -> processPatternValidator(validator, constraints)
            is PropertyValidator -> processPropertyValidator(validator, constraints)
            is RefValidator -> processSchema(validator.target, constraints)
            is RequiredValidator -> validator.properties.forEach {
                    if (it !in constraints.required) constraints.required.add(it) }
            is StringValidator -> processStringValidator(validator, constraints)
            is TypeValidator -> processTypeValidator(validator, constraints)
        }
    }

    private fun processConstValidator(constValidator: ConstValidator, constraints: Constraints) {
        if (constraints.constValue != null)
            throw JSONSchemaException("Duplicate const")
        constraints.constValue = constValidator.value
    }

    private fun processEnumValidator(enumValidator: EnumValidator, constraints: Constraints) {
        if (constraints.enumValues != null)
            throw JSONSchemaException("Duplicate enum")
        constraints.enumValues = enumValidator.array
    }

    private fun processFormatValidator(formatValidator: FormatValidator, constraints: Constraints) {
        if (constraints.format != null)
            throw JSONSchemaException("Duplicate format")
        constraints.format = formatValidator.type
    }

    private fun processNumberValidator(numberValidator: NumberValidator, constraints: Constraints) {
        when (numberValidator.condition) {
            NumberValidator.ValidationType.MULTIPLE_OF -> constraints.multipleOf =
                    Constraints.lcm(constraints.multipleOf, numberValidator.value)
            NumberValidator.ValidationType.MINIMUM -> constraints.minimum =
                    Constraints.maximumOf(constraints.minimum, numberValidator.value)
            NumberValidator.ValidationType.EXCLUSIVE_MINIMUM -> constraints.exclusiveMinimum =
                    Constraints.maximumOf(constraints.exclusiveMinimum, numberValidator.value)
            NumberValidator.ValidationType.MAXIMUM -> constraints.maximum =
                    Constraints.minimumOf(constraints.maximum, numberValidator.value)
            NumberValidator.ValidationType.EXCLUSIVE_MAXIMUM -> constraints.exclusiveMaximum =
                    Constraints.minimumOf(constraints.exclusiveMaximum, numberValidator.value)
        }
    }

    private fun processPatternValidator(patternValidator: PatternValidator, constraints: Constraints) {
        if (constraints.regex != null)
            throw JSONSchemaException("Duplicate pattern")
        constraints.regex = patternValidator.regex
    }

    private fun processStringValidator(stringValidator: StringValidator, constraints: Constraints) {
        when (stringValidator.condition) {
            StringValidator.ValidationType.MAX_LENGTH -> constraints.maxLength =
                    Constraints.minimumOf(constraints.maxLength, stringValidator.value)?.toInt()
            StringValidator.ValidationType.MIN_LENGTH -> constraints.minLength =
                    Constraints.maximumOf(constraints.minLength, stringValidator.value)?.toInt()
        }
    }

    private fun processPropertyValidator(propertyValidator: PropertyValidator, constraints: Constraints) {
        propertyValidator.properties.forEach { (name, schema) ->
            val propertyConstraints = constraints.properties.find { it.name == name } ?:
                    NamedConstraints(schema, name).also { constraints.properties.add(it) }
            processSchema(schema, propertyConstraints)
        }
    }

    private fun processTypeValidator(typeValidator: TypeValidator, constraints: Constraints) {
        typeValidator.types.forEach {
            when (it) {
                JSONSchema.Type.NULL -> constraints.nullable = true
                in constraints.types -> {}
                else -> constraints.types.add(it)
            }
        }
    }

    private fun processGeneral(general: JSONSchema.General, constraints: Constraints) {
        general.children.forEach { processSchema(it, constraints) }
    }

    companion object {

        private fun checkDirectory(directory: File, name: String): File {
            when {
                !directory.exists() -> {
                    if (!directory.mkdirs())
                        throw JSONSchemaException("Error creating $name - $directory")
                }
                directory.isDirectory -> {}
                directory.isFile -> throw JSONSchemaException("File given for $name - $directory")
                else -> throw JSONSchemaException("Error accessing $name - $directory")
            }
            return directory
        }

    }

}
