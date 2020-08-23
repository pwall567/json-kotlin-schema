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

import net.pwall.json.JSONArray
import net.pwall.json.JSONBoolean
import net.pwall.json.JSONDecimal
import net.pwall.json.JSONDouble
import net.pwall.json.JSONFloat
import net.pwall.json.JSONInteger
import net.pwall.json.JSONLong
import net.pwall.json.JSONNumberValue
import net.pwall.json.JSONObject
import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.pointer.JSONPointerException
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchema.Companion.toErrorDisplay
import net.pwall.json.schema.JSONSchemaException
import net.pwall.json.schema.subschema.IfThenElseSchema
import net.pwall.json.schema.subschema.ItemSchema
import net.pwall.json.schema.subschema.PropertySchema
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
        val schemaVersion = (json as? JSONObject)?.getStringOrNull(JSONPointer.root.child("\$schema"))
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
        if (schemaJSON !is JSONObject)
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
        for (entry in schemaJSON.entries) {
            when (entry.key) {
                "\$ref" -> children.add(parseRef(json, pointer.child("type"), uri, entry.value))
                "\$defs", "\$schema", "\$id", "\$comment", "title", "description", "then", "else" -> {}
                "default" -> children.add(DefaultValidator(uri, pointer.child("default"), entry.value))
                "allOf" -> children.add(parseCombinationSchema(json, pointer.child("allOf"), uri, entry.value,
                        JSONSchema.Companion::allOf))
                "anyOf" -> children.add(parseCombinationSchema(json, pointer.child("anyOf"), uri, entry.value,
                        JSONSchema.Companion::anyOf))
                "oneOf" -> children.add(parseCombinationSchema(json, pointer.child("oneOf"), uri, entry.value,
                        JSONSchema.Companion::oneOf))
                "not" -> children.add(JSONSchema.Not(uri, pointer.child("not"),
                        parseSchema(json, pointer.child("not"), uri)))
                "if" -> children.add(parseIf(json, pointer, uri))
                "type" -> children.add(parseType(pointer.child("type"), uri, entry.value))
                "enum" -> children.add(parseEnum(pointer.child("enum"), uri, entry.value))
                "const" -> children.add(parseConst(pointer.child("const"), uri, entry.value))
                "properties" -> children.add(parseProperties(json, pointer.child("properties"), uri, entry.value))
                "required" -> children.add(parseRequired(pointer.child("required"), uri, entry.value))
                "items" -> children.add(parseItems(json, pointer.child("items"), uri))
                in NumberValidator.typeKeywords -> children.add(parseNumberLimit(pointer.child(entry.key), uri,
                        NumberValidator.findType(entry.key), entry.value))
                "maxItems" -> children.add(parseArrayNumberOfItems(pointer.child("maxItems"), uri,
                        ArrayValidator.ValidationType.MAX_ITEMS, entry.value))
                "minItems" -> children.add(parseArrayNumberOfItems(pointer.child("minItems"), uri,
                        ArrayValidator.ValidationType.MIN_ITEMS, entry.value))
                "maxLength" -> children.add(parseStringLength(pointer.child("maxLength"), uri,
                        StringValidator.ValidationType.MAX_LENGTH, entry.value))
                "minLength" -> children.add(parseStringLength(pointer.child("minLength"), uri,
                        StringValidator.ValidationType.MIN_LENGTH, entry.value))
                "pattern" -> children.add(parsePattern(pointer.child("pattern"), uri, entry.value))
                "format" -> {
                    entry.value.let { value ->
                        if (value !is JSONString)
                            throw JSONSchemaException("Must be string - ${pointer.child("format")}")
                        value.get().let {
                            if (it in FormatValidator.typeKeywords)
                                children.add(FormatValidator(uri, pointer.child("format"),
                                        FormatValidator.findType(it)))
                        }
                    }
                }
            }
        }
        val result = JSONSchema.General(schemaVersion201909[0], title, description, uri, pointer, children)
        uri?.let { schemaCache[uri.resolve(pointer.toURIFragment())] = result }
        return result
    }

    private fun parseIf(json: JSONValue, pointer: JSONPointer, uri: URI?): JSONSchema {
        val ifSchema = parseSchema(json, pointer.child("if"), uri)
        val thenSchema = pointer.child("then").let { if (it.exists(json)) parseSchema(json, it, uri) else null }
        val elseSchema = pointer.child("else").let { if (it.exists(json)) parseSchema(json, it, uri) else null }
        return IfThenElseSchema(uri, pointer, ifSchema, thenSchema, elseSchema)
    }

    private fun parseCombinationSchema(json: JSONValue, pointer: JSONPointer, uri: URI?, array: JSONValue?,
            creator: (URI?, JSONPointer, List<JSONSchema>) -> JSONSchema): JSONSchema {
        if (array !is JSONArray)
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

    private fun parseItems(json: JSONValue, pointer: JSONPointer, uri: URI?): ItemSchema {
        return ItemSchema(uri, pointer, parseSchema(json, pointer, uri))
    }

    private fun parseProperties(json: JSONValue, pointer: JSONPointer, uri: URI?, value: JSONValue?):
            PropertySchema {
        if (value !is JSONObject)
            throw JSONSchemaException("properties must be object - $pointer")
        val properties = mutableListOf<Pair<String, JSONSchema>>()
        for (key in value.keys)
            properties.add(key to parseSchema(json, pointer.child(key), uri))
        return PropertySchema(uri, pointer, properties)
    }

    private fun parseRequired(pointer: JSONPointer, uri: URI?, value: JSONValue?): RequiredSchema {
        if (value !is JSONArray)
            throw JSONSchemaException("required must be array - ${pointer.pointerOrRoot()}")
        val properties = mutableListOf<String>()
        value.forEachIndexed { i, entry ->
            if (entry !is JSONString)
                throw JSONSchemaException("required items must be string - ${pointer.child(i)}")
            properties.add(entry.get())
        }
        return RequiredSchema(uri, pointer, properties)
    }

    private fun parseType(pointer: JSONPointer, uri: URI?, value: JSONValue?): TypeValidator {
        val types: List<JSONSchema.Type> = when (value) {
            is JSONString -> listOf(checkType(value.get()))
            is JSONArray -> value.mapIndexed { index, item ->
                if (item is JSONString)
                    checkType(item.get())
                else
                    throw JSONSchemaException("Invalid type ${pointer.child(index)}")
            }
            else -> throw JSONSchemaException("Invalid type $pointer")
        }
        return TypeValidator(uri, pointer, types)
    }

    private fun checkType(str: String): JSONSchema.Type {
        for (type in JSONSchema.Type.values()) {
            if (str == type.value)
                return type
        }
        throw JSONSchemaException("TODO")
    }

    private fun parseEnum(pointer: JSONPointer, uri: URI?, value: JSONValue?): EnumValidator {
        if (value !is JSONArray)
            throw JSONSchemaException("enum must be array - ${pointer.pointerOrRoot()}")
        return EnumValidator(uri, pointer, value)
    }

    private fun parseConst(pointer: JSONPointer, uri: URI?, value: JSONValue?) = ConstValidator(uri, pointer, value)

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
            throw JSONSchemaException("pattern invalid (${value.toErrorDisplay()}) - ${pointer.pointerOrRoot()}")
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

        val defaultURIResolver: (URI) -> InputStream? = { uri ->
                if (uri.scheme == "file") uri.toURL().openStream() else null }

        fun URI.dropFragment(): URI =
                toString().let { if (it.contains('#')) URI(it.substringBefore('#')) else this }

        fun JSONObject.getStringOrNull(key: String): String? =
                get(key)?.let { if (it is JSONString) it.get() else throw JSONSchemaException("Incorrect $key") }

        fun JSONObject.getStringOrNull(pointer: JSONPointer): String? {
            if (!pointer.exists(this))
                return null
            val value = pointer.eval(this)
            if (value !is JSONString)
                throw JSONSchemaException("Incorrect $pointer")
            return value.get()
        }

        @Suppress("unused")
        fun JSONObject.getStringOrDefault(pointer: JSONPointer, default: String?): String? {
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
