/*
 * @(#) Parser.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2021, 2022, 2023, 2024 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.json.schema.parser

import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

import net.pwall.json.JSONBoolean
import net.pwall.json.JSONDecimal
import net.pwall.json.JSONDouble
import net.pwall.json.JSONFloat
import net.pwall.json.JSONInteger
import net.pwall.json.JSONLong
import net.pwall.json.JSONMapping
import net.pwall.json.JSONNumberValue
import net.pwall.json.JSONSequence
import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.JSONZero
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchema.Companion.booleanSchema
import net.pwall.json.schema.JSONSchema.Companion.toErrorDisplay
import net.pwall.json.schema.JSONSchemaException
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.subschema.AdditionalItemsSchema
import net.pwall.json.schema.subschema.AdditionalPropertiesSchema
import net.pwall.json.schema.subschema.ExtensionSchema
import net.pwall.json.schema.subschema.IfThenElseSchema
import net.pwall.json.schema.subschema.ItemsArraySchema
import net.pwall.json.schema.subschema.ItemsSchema
import net.pwall.json.schema.subschema.PatternPropertiesSchema
import net.pwall.json.schema.subschema.PropertiesSchema
import net.pwall.json.schema.subschema.PropertyNamesSchema
import net.pwall.json.schema.subschema.RefSchema
import net.pwall.json.schema.subschema.RequiredSchema
import net.pwall.json.schema.validation.ArrayValidator
import net.pwall.json.schema.validation.ConstValidator
import net.pwall.json.schema.validation.ContainsValidator
import net.pwall.json.schema.validation.DefaultValidator
import net.pwall.json.schema.validation.DelegatingValidator
import net.pwall.json.schema.validation.EnumValidator
import net.pwall.json.schema.validation.FormatValidator
import net.pwall.json.schema.validation.NumberValidator
import net.pwall.json.schema.validation.PatternValidator
import net.pwall.json.schema.validation.PropertiesValidator
import net.pwall.json.schema.validation.StringValidator
import net.pwall.json.schema.validation.TypeValidator
import net.pwall.json.schema.validation.UniqueItemsValidator

class Parser(var options: Options = Options(), uriResolver: (URI) -> InputStream? = defaultURIResolver) {

    data class Options(
        var allowDescriptionRef: Boolean = false,
        var validateExamples: Boolean = false,
        var validateDefault: Boolean = false,
    )

    val examplesValidationErrors = mutableListOf<BasicOutput>()
    val defaultValidationErrors = mutableListOf<BasicOutput>()

    var customValidationHandler: (String, URI?, JSONPointer, JSONValue?) -> JSONSchema.Validator? =
            { _, _, _, _ -> null }

    var nonstandardFormatHandler: (String) -> FormatValidator.FormatChecker? = { _ -> null }

    val jsonReader = JSONReader(uriResolver)

    fun setExtendedResolver(extendedResolver: (URI) -> InputDetails?) {
        jsonReader.extendedResolver = extendedResolver
    }

    private val schemaCache = mutableMapOf<URI, JSONSchema>()

    fun preLoad(filename: String) {
        jsonReader.preLoad(filename)
    }

    fun preLoad(file: File) {
        jsonReader.preLoad(file)
    }

    fun preLoad(path: Path) {
        jsonReader.preLoad(path)
    }

    fun parseFile(filename: String): JSONSchema = parse(File(filename))

    fun parseURI(uriString: String): JSONSchema = parse(URI(uriString))

    fun parse(file: File): JSONSchema {
        if (!file.isFile)
            throw JSONSchemaException("Invalid file - $file")
        val uri = file.toURI()
        schemaCache[uri]?.let { return it }
        val json = jsonReader.readJSON(file)
        return parse(json, uri)
    }

    fun parse(uri: URI): JSONSchema {
        schemaCache[uri]?.let { return it }
        val json = jsonReader.readJSON(uri)
        return parse(json, uri)
    }

    fun parse(path: Path): JSONSchema {
        if (!Files.isRegularFile(path))
            throw JSONSchemaException("Invalid file - $path")
        val uri = path.toUri()
        schemaCache[uri]?.let { return it }
        val json = jsonReader.readJSON(path)
        return parse(json, uri)
    }

    fun parse(string: String, uri: URI? = null): JSONSchema {
        val json = jsonReader.readJSON(string, uri)
        return parse(json, uri)
    }

    private fun parse(json: JSONValue, uri: URI?): JSONSchema {
        val schemaVersion = (json as? JSONMapping<*>)?.getStringOrNull(uri, JSONPointer.root.child("\$schema"))
        val pointer = JSONPointer.root
        return when (schemaVersion) {
            in schemaVersion201909 -> parseSchema(json, pointer, uri)
            in schemaVersionDraft07 -> parseDraft07(json, pointer, uri)
            else -> parseSchema(json, pointer, uri)
        }
    }

    /**
     * Parse schema.
     *
     * @param   json        the schema JSON
     * @param   pointer     the JSON Pointer to the current location in the schema JSON
     * @param   parentUri   the parent URI for the schema
     */
    fun parseSchema(json: JSONValue, pointer: JSONPointer, parentUri: URI?): JSONSchema {
        val schemaJSON = pointer.eval(json)
        if (schemaJSON is JSONBoolean)
            return booleanSchema(schemaJSON.booleanValue(), parentUri, pointer)
        if (schemaJSON !is JSONMapping<*>)
            throw JSONSchemaException("Schema is not boolean or object - ${errorPointer(parentUri, pointer)}")
        val id = getIdOrNull(schemaJSON)
        val uri = when {
            id == null -> parentUri
            parentUri == null -> URI(id).dropFragment()
            else -> parentUri.resolve(id).dropFragment()
        }
        uri?.let {
            val fragmentURI = uri.withFragment(pointer)
            schemaCache[fragmentURI]?.let {
                return if (it !is JSONSchema.False) it else fatal("Recursive \$ref", uri, pointer)
            }
            schemaCache[fragmentURI] = JSONSchema.False(uri, pointer)
        }
        val title = schemaJSON.getStringOrNull(uri, "title")
        val description = getDescription(schemaJSON, uri, pointer)

        val children = mutableListOf<JSONSchema>()
        val result = JSONSchema.General(schemaVersion201909[0], title, description, uri, pointer, children)
        for ((key, value) in schemaJSON.entries) {
            val childPointer = pointer.child(key)
            when (key) {
                "\$schema" -> {
                    if (pointer != JSONPointer.root)
                        fatal("May only appear in the root of the document", uri, childPointer)
                    if (value !is JSONString)
                        fatal("String expected", uri, childPointer)
                }
                "\$id", "\$defs", "title", "description", "then", "else", "minContains", "maxContains", "example" -> {}
                "\$comment" -> {
                    if (value !is JSONString)
                        fatal("String expected", uri, childPointer)
                }
                "examples" -> {
                    if (value !is JSONSequence<*>)
                        fatal("Must be array", uri, childPointer)
                }
                "\$ref" -> children.add(parseRef(json, childPointer, uri, value))
                "default" -> children.add(DefaultValidator(uri, childPointer, value))
                "allOf" -> children.add(parseCombinationSchema(json, childPointer, uri, value,
                        JSONSchema.Companion::allOf))
                "anyOf" -> children.add(parseCombinationSchema(json, childPointer, uri, value,
                        JSONSchema.Companion::anyOf))
                "oneOf" -> children.add(parseCombinationSchema(json, childPointer, uri, value,
                        JSONSchema.Companion::oneOf))
                "not" -> children.add(JSONSchema.Not(uri, childPointer, parseSchema(json, pointer.child("not"), uri)))
                "if" -> children.add(parseIf(json, pointer, uri))
                "type" -> children.add(parseType(childPointer, uri, value))
                "enum" -> children.add(parseEnum(childPointer, uri, value))
                "const" -> children.add(ConstValidator(uri, childPointer, value))
                "properties" -> children.add(parseProperties(json, childPointer, uri, value))
                "patternProperties" -> children.add(parsePatternProperties(json, childPointer, uri, value))
                "propertyNames" -> children.add(parsePropertyNames(json, childPointer, uri))
                "minProperties" -> children.add(parsePropertiesSize(childPointer, uri,
                        PropertiesValidator.ValidationType.MIN_PROPERTIES, value))
                "maxProperties" -> children.add(parsePropertiesSize(childPointer, uri,
                        PropertiesValidator.ValidationType.MAX_PROPERTIES, value))
                "required" -> children.add(parseRequired(childPointer, uri, value))
                "items" -> children.add(parseItems(json, childPointer, uri, value))
                in NumberValidator.typeKeywords -> children.add(parseNumberLimit(childPointer, uri,
                        NumberValidator.findType(key), value))
                "maxItems" -> children.add(parseArrayNumberOfItems(childPointer, uri,
                        ArrayValidator.ValidationType.MAX_ITEMS, value))
                "minItems" -> children.add(parseArrayNumberOfItems(childPointer, uri,
                        ArrayValidator.ValidationType.MIN_ITEMS, value))
                "uniqueItems" -> parseArrayUniqueItems(childPointer, uri, value)?.let { children.add(it) }
                "maxLength" -> children.add(parseStringLength(childPointer, uri,
                        StringValidator.ValidationType.MAX_LENGTH, value))
                "minLength" -> children.add(parseStringLength(childPointer, uri,
                        StringValidator.ValidationType.MIN_LENGTH, value))
                "pattern" -> children.add(parsePattern(childPointer, uri, value))
                "format" -> children.add(parseFormat(childPointer, uri, value))
                "additionalProperties" -> children.add(AdditionalPropertiesSchema(result, uri, childPointer,
                        parseSchema(json, childPointer, uri)))
                "additionalItems" -> children.add(AdditionalItemsSchema(result, uri, childPointer,
                        parseSchema(json, childPointer, uri)))
                "contains" -> children.add(parseContains(json, pointer, uri))
                else -> customValidationHandler(key, uri, childPointer, value)?.let {
                    children.add(DelegatingValidator(it.uri, it.location, key, it))
                }
            }
            if (key.startsWith("x-"))
                children.add(ExtensionSchema(uri, childPointer, key, value?.toSimpleValue()))
        }
        if (options.validateExamples) {
            if (schemaJSON.containsKey("example"))
                validateExample(result, pointer, json, pointer.child("example"), examplesValidationErrors)
            if (schemaJSON.containsKey("examples")) {
                val examplesArray = schemaJSON["examples"] as JSONSequence<*> // checked earlier
                val examplesPointer = pointer.child("examples")
                for (i in examplesArray.indices)
                    validateExample(result, pointer, json, examplesPointer.child(i), examplesValidationErrors)
            }
        }
        if (options.validateDefault && schemaJSON.containsKey("default"))
            validateExample(result, pointer,  json, pointer.child("default"), defaultValidationErrors)
        uri?.let { schemaCache[uri.withFragment(pointer)] = result }
        return result
    }

    private fun validateExample(schema: JSONSchema, relativeLocation: JSONPointer, root: JSONValue?,
            location: JSONPointer, errorList: MutableList<BasicOutput>) {
        val result = schema.validateBasic(relativeLocation, root, location)
        if (!result.valid)
            errorList.add(result)
    }

    private fun getDescription(schemaJSON: JSONMapping<*>, uri: URI?, pointer: JSONPointer): String? {
        val value = schemaJSON["description"] ?: return null
        if (value is JSONString)
            return value.value
        // add controlling flag?
        if (options.allowDescriptionRef && value is JSONMapping<*> && value.size == 1) {
            val ref = value["\$ref"]
            if (ref is JSONString) {
                try {
                    return (if (uri == null) URI(ref.value) else uri.resolve(ref.value)).toURL().readText()
                }
                catch (e: Exception) {
                    fatal("Error reading external description $ref", uri, pointer)
                }
            }
        }
        fatal("Invalid description", uri, pointer)
    }

    private fun parseIf(json: JSONValue, pointer: JSONPointer, uri: URI?): JSONSchema {
        val ifSchema = parseSchema(json, pointer.child("if"), uri)
        val thenSchema = pointer.child("then").let { if (it.exists(json)) parseSchema(json, it, uri) else null }
        val elseSchema = pointer.child("else").let { if (it.exists(json)) parseSchema(json, it, uri) else null }
        return IfThenElseSchema(uri, pointer, ifSchema, thenSchema, elseSchema)
    }

    private fun parseContains(json: JSONValue, pointer: JSONPointer, uri: URI?): ContainsValidator {
        val containsSchema = parseSchema(json, pointer.child("contains"), uri)
        val minContains = pointer.child("minContains").let {
            if (it.exists(json)) getNonNegativeInteger(json, uri, it) else null
        }
        val maxContains = pointer.child("maxContains").let {
            if (it.exists(json)) getNonNegativeInteger(json, uri, it) else null
        }
        return ContainsValidator(uri, pointer.child("contains"), containsSchema, minContains, maxContains)
    }

    private fun parseFormat(pointer: JSONPointer, uri: URI?, value: JSONValue?): JSONSchema.Validator {
        if (value !is JSONString)
            fatal("String expected", uri, pointer)
        value.value.let { keyword ->
            val checker = nonstandardFormatHandler(keyword) ?: FormatValidator.findChecker(keyword) ?:
                    FormatValidator.NullFormatChecker(keyword)
            return FormatValidator(uri, pointer, checker)
        }
    }

    private fun parseCombinationSchema(json: JSONValue, pointer: JSONPointer, uri: URI?, array: JSONValue?,
            creator: (URI?, JSONPointer, List<JSONSchema>) -> JSONSchema): JSONSchema {
        if (array !is JSONSequence<*>)
            fatal("Compound must take array", uri, pointer)
        return creator(uri, pointer, array.mapIndexed { i, _ -> parseSchema(json, pointer.child(i), uri) })
    }

    private fun parseRef(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?): RefSchema {
        if (value !is JSONString)
            fatal("\$ref must be string", uri, pointer)
        val refString = value.value
        val refURI: URI?
        val refJSON: JSONValue
        val refURIFragment: String?
        val refPointer: JSONPointer
        val hashIndex = refString.indexOf('#')
        when {
            hashIndex < 0 -> {
                // no fragment
                refURI = uri?.resolve(refString) ?: URI(refString)
                refJSON = jsonReader.readJSON(refURI)
                refURIFragment = null
                refPointer = JSONPointer.root
            }
            hashIndex == 0 -> {
                // fragment only
                refURI = uri
                refJSON = json
                refURIFragment = refString.substring(1)
                refPointer = JSONPointer.fromURIFragment(refString)
            }
            else -> {
                // uri with fragment
                val refStringWithoutFragment = refString.substring(0, hashIndex)
                if (uri != null && uri.toString() == refStringWithoutFragment) { // same base URI?
                    refURI = uri
                    refJSON = json
                }
                else {
                    refURI = uri?.resolve(refStringWithoutFragment) ?: URI(refStringWithoutFragment)
                    refJSON = jsonReader.readJSON(refURI)
                }
                refURIFragment = refString.substring(hashIndex + 1)
                refPointer = JSONPointer.fromURIFragment(refString.substring(hashIndex))
            }
        }
        if (!refPointer.exists(refJSON))
            fatal("\$ref not found $refString", uri, pointer)
        return RefSchema(
            uri = uri,
            location = pointer,
            target = parseSchema(refJSON, refPointer, refURI),
            fragment = refURIFragment,
        )
    }

    private fun parseItems(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?): JSONSchema.SubSchema {
        return if (value is JSONSequence<*>)
            ItemsArraySchema(uri, pointer, value.mapIndexed { i, _ -> parseSchema(json, pointer.child(i), uri) })
        else
            ItemsSchema(uri, pointer, parseSchema(json, pointer, uri))
    }

    private fun parseProperties(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?): PropertiesSchema {
        if (value !is JSONMapping<*>)
            fatal("properties must be object", uri, pointer)
        return PropertiesSchema(uri, pointer, value.keys.map { it to parseSchema(json, pointer.child(it), uri) })
    }

    private fun parsePatternProperties(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?):
            PatternPropertiesSchema {
        if (value !is JSONMapping<*>)
            fatal("patternProperties must be object", uri, pointer)
        return PatternPropertiesSchema(uri, pointer, value.keys.map { key ->
            val childPointer = pointer.child(key)
            val regex = try { Regex(key) } catch (e: Exception) {
                    fatal("Invalid regex in patternProperties", uri, childPointer) }
            regex to parseSchema(json, childPointer, uri)
        })
    }

    private fun parsePropertyNames(json: JSONValue, pointer: JSONPointer, uri: URI?): PropertyNamesSchema {
        return PropertyNamesSchema(uri, pointer, parseSchema(json, pointer, uri))
    }

    private fun parsePropertiesSize(pointer: JSONPointer, uri: URI?, condition: PropertiesValidator.ValidationType,
            value: JSONValue?): PropertiesValidator {
        return PropertiesValidator(uri, pointer, condition, getInteger(value, uri, pointer))
    }

    private fun parseRequired(pointer: JSONPointer, uri: URI?, value: JSONValue?): RequiredSchema {
        if (value !is JSONSequence<*>)
            fatal("required must be array", uri, pointer)
        return RequiredSchema(uri, pointer, value.mapIndexed { i, entry ->
            if (entry !is JSONString)
                fatal("required item must be string", uri, pointer.child(i))
            entry.value
        })
    }

    private fun parseType(pointer: JSONPointer, uri: URI?, value: JSONValue?): TypeValidator {
        val types: List<JSONSchema.Type> = when (value) {
            is JSONString -> listOf(checkType(value, pointer, uri))
            is JSONSequence<*> -> value.mapIndexed { index, item -> checkType(item, pointer.child(index), uri) }
            else -> fatal("Invalid type", uri, pointer)
        }
        return TypeValidator(uri, pointer, types)
    }

    private fun checkType(item: JSONValue?, pointer: JSONPointer, uri: URI?): JSONSchema.Type {
        if (item is JSONString) {
            for (type in JSONSchema.Type.values()) {
                if (item.value == type.value)
                    return type
            }
        }
        fatal("Invalid type", uri, pointer)
    }

    private fun parseEnum(pointer: JSONPointer, uri: URI?, value: JSONValue?): EnumValidator {
        if (value !is JSONSequence<*>)
            fatal("enum must be array", uri, pointer)
        return EnumValidator(uri, pointer, value)
    }

    private fun parseNumberLimit(pointer: JSONPointer, uri: URI?, condition: NumberValidator.ValidationType,
            value: JSONValue?): NumberValidator {
        if (value !is JSONNumberValue)
            fatal("Must be number (was ${value.toErrorDisplay()})", uri, pointer)
        val number: Number = when (value) {
            is JSONDouble, // should not happen
            is JSONFloat,
            is JSONDecimal -> value.bigDecimalValue()
            is JSONLong -> value.toLong()
            else -> value.toInt() // includes JSONInteger, JSONZero
        }
        if (condition == NumberValidator.ValidationType.MULTIPLE_OF && !number.isPositive())
            fatal("multipleOf must be greater than 0", uri, pointer)
        return NumberValidator(uri, pointer, number, condition)
    }

    private fun parseStringLength(pointer: JSONPointer, uri: URI?, condition: StringValidator.ValidationType,
            value: JSONValue?): StringValidator {
        return StringValidator(uri, pointer, condition, getInteger(value, uri, pointer))
    }

    private fun parseArrayNumberOfItems(pointer: JSONPointer, uri: URI?, condition: ArrayValidator.ValidationType,
            value: JSONValue?): ArrayValidator {
        return ArrayValidator(uri, pointer, condition, getInteger(value, uri, pointer))
    }

    private fun parseArrayUniqueItems(pointer: JSONPointer, uri: URI?, value: JSONValue?): UniqueItemsValidator? {
        if (value !is JSONBoolean)
            fatal("Must be boolean", uri, pointer)
        return if (value.booleanValue()) UniqueItemsValidator(uri, pointer) else null
    }

    private fun parsePattern(pointer: JSONPointer, uri: URI?, value: JSONValue?): PatternValidator {
        if (value !is JSONString)
            fatal("Must be string", uri, pointer)
        val regex = try {
            Regex(value.value)
        } catch (e: Exception) {
            val msg = e.message?.let { "- ${it.substringBefore('\n').trim()}" } ?: ""
            fatal("Pattern invalid $msg (${value.toErrorDisplay()})", uri, pointer)
        }
        return PatternValidator(uri, pointer, regex)
    }

    private fun parseDraft07(json: JSONValue, pointer: JSONPointer, parentUri: URI?): JSONSchema {
        return parseSchema(json, pointer, parentUri) // temporary - treat as 201909
    }

    companion object {

        @Suppress("unused")
        val schemaVersion202012 = listOf("http://json-schema.org/draft/2020-12/schema",
                "https://json-schema.org/draft/2020-12/schema")
        val schemaVersion201909 = listOf("http://json-schema.org/draft/2019-09/schema",
                "https://json-schema.org/draft/2019-09/schema")
        val schemaVersionDraft07 = listOf("http://json-schema.org/draft-07/schema",
                "https://json-schema.org/draft-07/schema")

        val defaultURIResolver: (URI) -> InputStream? = { uri -> uri.toURL().openStream() }

        val defaultExtendedResolver: (URI) -> InputDetails? = { uri ->
            when (val conn = uri.toURL().openConnection()) {
                is HttpURLConnection -> {
                    if (conn.responseCode == HttpURLConnection.HTTP_NOT_FOUND)
                        null
                    else {
                        val contentType = conn.contentType?.split(';')?.map { it.trim() }
                        val charset = contentType?.findStartingFrom(1) { it.startsWith("charset=") }?.drop(8)?.trim()
                        val reader = charset?.let { conn.inputStream.reader(Charset.forName(it)) } ?:
                                conn.inputStream.reader()
                        InputDetails(reader, contentType?.get(0))
                    }
                }
                else -> InputDetails(conn.inputStream.reader())
            }
        }

        private inline fun <T> List<T>.findStartingFrom(index: Int = 0, predicate: (T) -> Boolean): T? {
            for (i in index until this.size)
                this[i].let { if (predicate(it)) return it }
            return null
        }

        fun URI.dropFragment(): URI = when {
            fragment == null -> this
            isOpaque -> URI(scheme, schemeSpecificPart, null)
            else -> URI(scheme, authority, path, query, null)
        }

        private fun URI.withFragment(newFragment: String): URI = when {
            isOpaque -> URI(scheme, schemeSpecificPart, newFragment)
            else -> URI(scheme, authority, path, query, newFragment)
        }

        fun URI.withFragment(pointer: JSONPointer): URI = when (pointer) {
            JSONPointer.root -> dropFragment()
            else -> withFragment(pointer.toURIFragment().substring(1))
        }

        private fun fatal(message: String, uri: URI?, pointer: JSONPointer): Nothing {
            throw JSONSchemaException("$message - ${errorPointer(uri, pointer)}")
        }

        private fun errorPointer(uri: URI?, pointer: JSONPointer): String =
            uri?.withFragment(pointer)?.toString() ?: pointer.pointerOrRoot()

        private fun JSONPointer.pointerOrRoot() = if (this == JSONPointer.root) "root" else toString()

        fun getInteger(value: JSONValue?, uri: URI?, pointer: JSONPointer): Int {
            if (value is JSONZero)
                return 0
            if (value is JSONInteger)
                return value.value
            fatal("Must be integer", uri, pointer)
        }

        fun getNonNegativeInteger(json: JSONValue, uri: URI?, pointer: JSONPointer): Int {
            val value = pointer.find(json)
            if (value is JSONZero)
                return 0
            if (value is JSONInteger && value.value >= 0)
                return value.value
            fatal("Must be non-negative integer", uri, pointer)
        }

        fun JSONMapping<*>.getStringOrNull(uri: URI?, key: String): String? = get(key)?.let {
            if (it is JSONString) it.value else fatal("Must be string", uri, JSONPointer.root.child(key))
        }

        fun JSONMapping<*>.getStringOrNull(uri: URI?, pointer: JSONPointer): String? {
            if (!pointer.exists(this))
                return null
            val value = pointer.eval(this)
            if (value !is JSONString)
                fatal("Must be string", uri, pointer)
            return value.value
        }

        @Suppress("unused")
        fun JSONMapping<*>.getStringOrDefault(pointer: JSONPointer, default: String?): String? {
            if (!pointer.exists(this))
                return default
            val value = pointer.eval(this)
            if (value !is JSONString)
                throw JSONSchemaException("Incorrect $pointer")
            return value.value
        }

        @Suppress("unused")
        fun Number.isZero(): Boolean = when (this) {
            is BigDecimal -> this.compareTo(BigDecimal.ZERO) == 0
            is BigInteger -> this == BigInteger.ZERO
            is Double -> this == 0.0
            is Float -> this == 0.0F
            is Long -> this == 0L
            else -> this.toInt() == 0
        }

        fun Number.isPositive(): Boolean = when (this) {
            is BigDecimal -> this > BigDecimal.ZERO
            is BigInteger -> this > BigInteger.ZERO
            is Double -> this > 0.0
            is Float -> this > 0.0F
            is Long -> this > 0L
            else -> this.toInt() > 0
        }

        fun getIdOrNull(jsonValue: JSONValue): String? =
                ((jsonValue as? JSONMapping<*>)?.get("\$id") as? JSONString)?.value

    }

}
