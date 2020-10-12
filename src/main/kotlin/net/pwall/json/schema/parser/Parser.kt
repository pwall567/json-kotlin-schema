/*
 * @(#) Parser.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020 Peter Wall
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
import java.net.URI

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
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.pointer.JSONPointerException
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchema.Companion.toErrorDisplay
import net.pwall.json.schema.JSONSchemaException
import net.pwall.json.schema.subschema.AdditionalItemsSchema
import net.pwall.json.schema.subschema.AdditionalPropertiesSchema
import net.pwall.json.schema.subschema.IfThenElseSchema
import net.pwall.json.schema.subschema.ItemsArraySchema
import net.pwall.json.schema.subschema.ItemsSchema
import net.pwall.json.schema.subschema.PatternPropertiesSchema
import net.pwall.json.schema.subschema.PropertiesSchema
import net.pwall.json.schema.subschema.RefSchema
import net.pwall.json.schema.subschema.RequiredSchema
import net.pwall.json.schema.validation.ArrayValidator
import net.pwall.json.schema.validation.ConstValidator
import net.pwall.json.schema.validation.DefaultValidator
import net.pwall.json.schema.validation.EnumValidator
import net.pwall.json.schema.validation.FormatValidator
import net.pwall.json.schema.validation.NumberValidator
import net.pwall.json.schema.validation.PatternValidator
import net.pwall.json.schema.validation.StringValidator
import net.pwall.json.schema.validation.TypeValidator

class Parser(uriResolver: (URI) -> InputStream? = defaultURIResolver) {

    private val jsonReader = JSONReader(uriResolver)

    private val schemaCache = mutableMapOf<URI, JSONSchema>()

    fun preLoad(filename: String) {
        jsonReader.preLoad(filename)
    }

    fun preLoad(file: File) {
        jsonReader.preLoad(file)
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

    private fun parse(json: JSONValue, uri: URI): JSONSchema {
        val schemaVersion = (json as? JSONMapping<*>)?.getStringOrNull(JSONPointer.root.child("\$schema"))
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
            return if (schemaJSON.booleanValue()) JSONSchema.True(parentUri, pointer) else
                    JSONSchema.False(parentUri, pointer)
        if (schemaJSON !is JSONMapping<*>)
            throw JSONSchemaException("Schema is not boolean or object - ${pointer.pointerOrRoot()}")
        val id = schemaJSON.getStringOrNull("\$id")
        val uri = when {
            id == null -> parentUri
            parentUri == null -> URI(id).dropFragment()
            else -> parentUri.resolve(id).dropFragment()
        }
        uri?.let {
            val fragmentURI = uri.resolve(pointer.toURIFragment())
            schemaCache[fragmentURI]?.let {
                return if (it !is JSONSchema.False) it else throw JSONPointerException("Recursive \$ref - $pointer")
            }
            schemaCache[fragmentURI] = JSONSchema.False(uri, pointer)
        }
        val title = schemaJSON.getStringOrNull("title")
        val description = schemaJSON.getStringOrNull("description")

        val children = mutableListOf<JSONSchema>()
        val result = JSONSchema.General(schemaVersion201909[0], title, description, uri, pointer, children)
        for ((key, value) in schemaJSON.entries) {
            val childPointer = pointer.child(key)
            when (key) {
                "\$defs", "then", "else" -> {}
                "\$schema", "\$id", "\$comment", "title", "description" -> {
                    if (value !is JSONString)
                        throw JSONSchemaException("String expected - $childPointer")
                }
                "examples" -> {
                    if (value !is JSONSequence<*>)
                        throw JSONSchemaException("Must be array - $childPointer")
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
                "required" -> children.add(parseRequired(childPointer, uri, value))
                "items" -> children.add(parseItems(json, childPointer, uri, value))
                in NumberValidator.typeKeywords -> children.add(parseNumberLimit(childPointer, uri,
                        NumberValidator.findType(key), value))
                "maxItems" -> children.add(parseArrayNumberOfItems(childPointer, uri,
                        ArrayValidator.ValidationType.MAX_ITEMS, value))
                "minItems" -> children.add(parseArrayNumberOfItems(childPointer, uri,
                        ArrayValidator.ValidationType.MIN_ITEMS, value))
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
            }
        }
        uri?.let { schemaCache[uri.resolve(pointer.toURIFragment())] = result }
        return result
    }

    private fun parseIf(json: JSONValue, pointer: JSONPointer, uri: URI?): JSONSchema {
        val ifSchema = parseSchema(json, pointer.child("if"), uri)
        val thenSchema = pointer.child("then").let { if (it.exists(json)) parseSchema(json, it, uri) else null }
        val elseSchema = pointer.child("else").let { if (it.exists(json)) parseSchema(json, it, uri) else null }
        return IfThenElseSchema(uri, pointer, ifSchema, thenSchema, elseSchema)
    }

    private fun parseFormat(pointer: JSONPointer, uri: URI?, value: JSONValue?): FormatValidator {
        if (value !is JSONString)
            throw JSONSchemaException("String expected - $pointer")
        value.get().let {
            if (it !in FormatValidator.typeKeywords)
                throw JSONSchemaException("Format not recognised - $it - $pointer")
            return FormatValidator(uri, pointer, FormatValidator.findType(it))
        }
    }

    private fun parseCombinationSchema(json: JSONValue, pointer: JSONPointer, uri: URI?, array: JSONValue?,
            creator: (URI?, JSONPointer, List<JSONSchema>) -> JSONSchema): JSONSchema {
        if (array !is JSONSequence<*>)
            throw JSONPointerException("Compound must take array - $pointer")
        return creator(uri, pointer, array.mapIndexed { i, _ -> parseSchema(json, pointer.child(i), uri) })
    }

    private fun parseRef(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?): RefSchema {
        if (value !is JSONString)
            throw JSONPointerException("\$ref must be string - $pointer")
        val refURIString = (if (uri == null) URI(value.get()) else uri.resolve(value.get())).toString()
        val (refURIPath, refURIFragment) = if (refURIString.contains('#'))
            refURIString.substringBefore('#') to refURIString.substringAfter('#')
        else
            refURIString to null
        val refJSON = if (refURIPath == uri.toString()) json else
                jsonReader.readJSON(URI(if (refURIPath.endsWith('/')) refURIPath.dropLast(1) else refURIPath))
        val refPointer = refURIFragment?.let { JSONPointer.fromURIFragment("#$it") }  ?: JSONPointer.root
        if (!refPointer.exists(refJSON))
            throw JSONSchemaException("\$ref not found $value - $pointer")
        val target = parseSchema(refJSON, refPointer, URI(refURIPath))
        return RefSchema(uri, pointer, target, refURIFragment)
    }

    private fun parseItems(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?): JSONSchema.SubSchema {
        return if (value is JSONSequence<*>)
            ItemsArraySchema(uri, pointer, value.mapIndexed { i, _ -> parseSchema(json, pointer.child(i), uri) })
        else
            ItemsSchema(uri, pointer, parseSchema(json, pointer, uri))
    }

    private fun parseProperties(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?): PropertiesSchema {
        if (value !is JSONMapping<*>)
            throw JSONSchemaException("properties must be object - $pointer")
        return PropertiesSchema(uri, pointer, value.keys.map { it to parseSchema(json, pointer.child(it), uri) })
    }

    private fun parsePatternProperties(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?):
            PatternPropertiesSchema {
        if (value !is JSONMapping<*>)
            throw JSONSchemaException("patternProperties must be object - $pointer")
        return PatternPropertiesSchema(uri, pointer, value.keys.map { key ->
            val childPointer = pointer.child(key)
            val regex = try { Regex(key) } catch (e: Exception) {
                    throw JSONSchemaException("Invalid regex in patternProperties - $childPointer") }
            regex to parseSchema(json, childPointer, uri)
        })
    }

    private fun parseRequired(pointer: JSONPointer, uri: URI?, value: JSONValue?): RequiredSchema {
        if (value !is JSONSequence<*>)
            throw JSONSchemaException("required must be array - ${pointer.pointerOrRoot()}")
        return RequiredSchema(uri, pointer, value.mapIndexed { i, entry ->
            if (entry !is JSONString)
                throw JSONSchemaException("required items must be string - ${pointer.child(i)}")
            entry.get()
        })
    }

    private fun parseType(pointer: JSONPointer, uri: URI?, value: JSONValue?): TypeValidator {
        val types: List<JSONSchema.Type> = when (value) {
            is JSONString -> listOf(checkType(value, pointer))
            is JSONSequence<*> -> value.mapIndexed { index, item -> checkType(item, pointer.child(index)) }
            else -> throw JSONSchemaException("Invalid type $pointer")
        }
        return TypeValidator(uri, pointer, types)
    }

    private fun checkType(item: JSONValue?, pointer: JSONPointer): JSONSchema.Type {
        if (item is JSONString) {
            for (type in JSONSchema.Type.values()) {
                if (item.get() == type.value)
                    return type
            }
        }
        throw JSONSchemaException("Invalid type $pointer")
    }

    private fun parseEnum(pointer: JSONPointer, uri: URI?, value: JSONValue?): EnumValidator {
        if (value !is JSONSequence<*>)
            throw JSONSchemaException("enum must be array - ${pointer.pointerOrRoot()}")
        return EnumValidator(uri, pointer, value)
    }

    private fun parseNumberLimit(pointer: JSONPointer, uri: URI?, condition: NumberValidator.ValidationType,
            value: JSONValue?): NumberValidator {
        if (value !is JSONNumberValue)
            throw JSONSchemaException("Must be number (was ${value.toErrorDisplay()}) - ${pointer.pointerOrRoot()}")
        val number: Number = when (value) {
            is JSONDouble, // should not happen
            is JSONFloat,
            is JSONDecimal -> value.bigDecimalValue()
            is JSONLong -> value.toLong()
            else -> value.toInt() // includes JSONInteger, JSONZero
        }
        if (condition == NumberValidator.ValidationType.MULTIPLE_OF && !number.isPositive())
            throw JSONSchemaException("multipleOf must be greater than 0 - ${pointer.pointerOrRoot()}")
        return NumberValidator(uri, pointer, number, condition)
    }

    private fun parseStringLength(pointer: JSONPointer, uri: URI?, condition: StringValidator.ValidationType,
            value: JSONValue?): StringValidator {
        if (value !is JSONInteger)
            throw JSONSchemaException("Must be integer - ${pointer.pointerOrRoot()}")
        return StringValidator(uri, pointer, condition, value.get())
    }

    private fun parseArrayNumberOfItems(pointer: JSONPointer, uri: URI?, condition: ArrayValidator.ValidationType,
            value: JSONValue?): ArrayValidator {
        if (value !is JSONInteger)
            throw JSONSchemaException("Must be integer - ${pointer.pointerOrRoot()}")
        return ArrayValidator(uri, pointer, condition, value.get())
    }

    private fun parsePattern(pointer: JSONPointer, uri: URI?, value: JSONValue?): PatternValidator {
        if (value !is JSONString)
            throw JSONSchemaException("Must be string - ${pointer.pointerOrRoot()}")
        val regex = try {
            Regex(value.get())
        }
        catch (e: Exception) {
            throw JSONSchemaException("Pattern invalid (${value.toErrorDisplay()}) - ${pointer.pointerOrRoot()}")
        }
        return PatternValidator(uri, pointer, regex)
    }

    private fun JSONPointer.pointerOrRoot() = if (this == JSONPointer.root) "root" else toString()

    private fun parseDraft07(json: JSONValue, pointer: JSONPointer, parentUri: URI?): JSONSchema {
        return parseSchema(json, pointer, parentUri) // temporary - treat as 201909
    }

    companion object {

        @Suppress("unused")
        val schemaVersion201909 = listOf("http://json-schema.org/draft/2019-09/schema",
                "https://json-schema.org/draft/2019-09/schema")
        @Suppress("unused")
        val schemaVersionDraft07 = listOf("http://json-schema.org/draft-07/schema",
                "https://json-schema.org/draft-07/schema")

        val defaultURIResolver: (URI) -> InputStream? = { uri -> uri.toURL().openStream() }

        fun URI.dropFragment(): URI =
                toString().let { if (it.contains('#')) URI(it.substringBefore('#')) else this }

        fun JSONMapping<*>.getStringOrNull(key: String): String? =
                get(key)?.let { if (it is JSONString) it.get() else throw JSONSchemaException("Incorrect $key") }

        fun JSONMapping<*>.getStringOrNull(pointer: JSONPointer): String? {
            if (!pointer.exists(this))
                return null
            val value = pointer.eval(this)
            if (value !is JSONString)
                throw JSONSchemaException("Incorrect $pointer")
            return value.get()
        }

        @Suppress("unused")
        fun JSONMapping<*>.getStringOrDefault(pointer: JSONPointer, default: String?): String? {
            if (!pointer.exists(this))
                return default
            val value = pointer.eval(this)
            if (value !is JSONString)
                throw JSONSchemaException("Incorrect $pointer")
            return value.get()
        }

        @Suppress("unused")
        fun Number.isZero(): Boolean = when (this) {
            is BigDecimal -> this == BigDecimal.ZERO
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

    }

}
