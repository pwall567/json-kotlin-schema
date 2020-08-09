/*
 * @(#) JSONSchema.kt
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

package net.pwall.json.schema

import java.io.File
import java.net.URI

import net.pwall.json.JSON
import net.pwall.json.JSONArray
import net.pwall.json.JSONBoolean
import net.pwall.json.JSONNumberValue
import net.pwall.json.JSONObject
import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.Output
import net.pwall.json.schema.parser.Parser

/**
 * A JSON Schema.
 *
 * @author  Peter Wall
 */
sealed class JSONSchema(
        /** The URI for the schema */
        val uri: URI?,
        /** The JSON Pointer for the location of the schema */
        val location: JSONPointer
) {

    enum class Type(val value: String) {
        NULL("null"),
        BOOLEAN("boolean"),
        OBJECT("object"),
        ARRAY("array"),
        NUMBER("number"),
        STRING("string"),
        INTEGER("integer")
    }

    val absoluteLocation: String?
            get() = uri?.let { "$it${location.schemaURIFragment()}" }

    open val description: String? = null

    open val title: String? = null

    open fun childLocation(pointer: JSONPointer): JSONPointer = pointer

    abstract fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output

    fun validate(json: JSONValue?, instanceLocation: JSONPointer = JSONPointer.root) =
            validate(JSONPointer.root, json, instanceLocation)

    fun validate(json: String, instanceLocation: JSONPointer = JSONPointer.root) =
            validate(JSONPointer.root, JSON.parse(json), instanceLocation)

    fun createAnnotation(relativeLocation: JSONPointer, instanceLocation: JSONPointer, annotation: String,
                         errors: List<Output>? = null, annotations: List<Output>? = null): BasicOutput {
        return BasicOutput.createAnnotation(relativeLocation.schemaURIFragment(), absoluteLocation,
                instanceLocation.schemaURIFragment(), annotation, errors, annotations)
    }

    fun createError(relativeLocation: JSONPointer, instanceLocation: JSONPointer, error: String,
                    errors: List<Output>? = null, annotations: List<Output>? = null): BasicOutput {
        return BasicOutput.createError(relativeLocation.schemaURIFragment(), absoluteLocation,
                instanceLocation.schemaURIFragment(), error, errors, annotations)
    }

    fun createErrorForChild(index: Int, relativeLocation: JSONPointer, instanceLocation: JSONPointer, error: String,
                            errors: List<Output>? = null, annotations: List<Output>? = null): BasicOutput {
        return BasicOutput.createError(relativeLocation.toURIFragment(),
                uri?.let { "$it${location.child(index).toURIFragment()}" }, instanceLocation.toURIFragment(), error,
                errors, annotations)
    }

    fun createErrorForChild(property: String, relativeLocation: JSONPointer, instanceLocation: JSONPointer,
                            error: String, errors: List<Output>? = null, annotations: List<Output>? = null): BasicOutput {
        return BasicOutput.createError(relativeLocation.toURIFragment(),
                uri?.let { "$it${location.child(property).toURIFragment()}" }, instanceLocation.toURIFragment(), error,
                errors, annotations)
    }

    protected fun validationResult(relativeLocation: JSONPointer, instanceLocation: JSONPointer,
            errors: List<Output>): Output = when (errors.size) {
        0 -> trueOutput
        1 -> errors[0]
        else -> createError(relativeLocation, instanceLocation, "Multiple errors", errors = errors)
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is JSONSchema && uri == other.uri && location == other.location

    override fun hashCode(): Int = uri.hashCode() xor location.hashCode()

    @Suppress("EqualsOrHashCode")
    class True(uri: URI?, location: JSONPointer) : JSONSchema(uri, location) {

        override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer) =
                createAnnotation(relativeLocation, instanceLocation, "true")

        override fun equals(other: Any?): Boolean = this === other || other is True && super.equals(other)

    }

    @Suppress("EqualsOrHashCode")
    class False(uri: URI?, location: JSONPointer) : JSONSchema(uri, location) {

        override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer) =
                createError(relativeLocation, instanceLocation, "false")

        override fun equals(other: Any?): Boolean = this === other || other is False && super.equals(other)

    }

    class Not(uri: URI?, location: JSONPointer, private val nested: JSONSchema) : JSONSchema(uri, location) {

        override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output =
            nested.validate(relativeLocation, json, instanceLocation).let {
                if (it.valid)
                    createError(relativeLocation, instanceLocation, "not", annotations = listOf(it))
                else
                    createAnnotation(relativeLocation, instanceLocation, "not", errors = listOf(it))
            }

        override fun equals(other: Any?): Boolean =
                this === other || other is Not && super.equals(other) && nested == other.nested

        override fun hashCode(): Int = super.hashCode() xor nested.hashCode()

    }

    class ArrayValidator(uri: URI?, location: JSONPointer, val name: String, val array: List<JSONSchema>,
                         val resultValid: (Int) -> Boolean) : JSONSchema(uri, location) {

        override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child(name)

        override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
            val errors = mutableListOf<Output>()
            val annotations = mutableListOf<Output>()
            var trueCount = 0
            array.forEachIndexed { i, schema ->
                schema.validate(relativeLocation.child(i), json, instanceLocation).let {
                    if (it.valid) {
                        trueCount++
                        annotations.add(it)
                    }
                    else
                        errors.add(it)
                }
            }
            return if (resultValid(trueCount))
                createAnnotation(relativeLocation, instanceLocation, "$name succeeds", errors, annotations)
            else
                createError(relativeLocation, instanceLocation, "$name fails", errors, annotations)
        }

        override fun equals(other: Any?): Boolean =
                this === other || other is ArrayValidator && super.equals(other) && name == other.name &&
                        array == other.array && resultValid == other.resultValid

        override fun hashCode(): Int = super.hashCode() xor name.hashCode() xor array.hashCode() xor
                resultValid.hashCode()

    }

    abstract class Validator(uri: URI?, location: JSONPointer) : JSONSchema(uri, location)

    class General(val schemaVersion: String, override val title: String?, override val description: String?, uri: URI?,
            location: JSONPointer, val children: List<JSONSchema>) : JSONSchema(uri, location) {

        override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
            val errors = mutableListOf<Output>()
            for (child in children) {
                child.validate(child.childLocation(relativeLocation), json, instanceLocation).let {
                    if (!it.valid)
                        errors.add(it)
                }
            }
            return validationResult(relativeLocation, instanceLocation, errors)
        }

        override fun equals(other: Any?): Boolean =
                this === other || other is General && super.equals(other) && schemaVersion == other.schemaVersion &&
                        title == other.title && description == other.description && children == other.children

        override fun hashCode(): Int = super.hashCode() xor schemaVersion.hashCode() xor title.hashCode() xor
                description.hashCode() xor children.hashCode()

    }

    companion object {

        val trueOutput = Output(true)
        val falseOutput = Output(false)

        val parser by lazy { Parser() }

        fun parse(filename: String): JSONSchema = parse(File(filename))

        fun parse(file: File): JSONSchema = parser.parse(file)

        fun allOf(uri: URI?, location: JSONPointer, array: List<JSONSchema>): ArrayValidator =
                ArrayValidator(uri, location, "allOf", array) { it == array.size }

        fun anyOf(uri: URI?, location: JSONPointer, array: List<JSONSchema>): ArrayValidator =
                ArrayValidator(uri, location, "anyOf", array) { it > 0 }

        fun oneOf(uri: URI?, location: JSONPointer, array: List<JSONSchema>): ArrayValidator =
                ArrayValidator(uri, location, "oneOf", array) { it == 1 }

        fun JSONPointer.schemaURIFragment() = toURIFragment().replace("%24", "\$")

        fun JSONValue?.toErrorDisplay(): String = when (this) {
            null -> "null"
            is JSONObject -> "object"
            is JSONArray -> "array"
            is JSONBoolean,
            is JSONNumberValue -> toString()
            is JSONString -> {
                val s = toJSON()
                if (s.length > 40) "${s.take(16)} ... ${s.takeLast(16)}" else s
            }
            else -> "unknown"
        }
    }

}
