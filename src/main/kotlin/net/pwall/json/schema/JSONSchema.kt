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
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.DetailedOutput
import net.pwall.json.schema.output.Output
import net.pwall.json.schema.parser.Parser
import net.pwall.json.schema.subschema.AllOfSchema
import net.pwall.json.schema.subschema.AnyOfSchema
import net.pwall.json.schema.subschema.OneOfSchema

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

    abstract fun validate(json: JSONValue?, instanceLocation: JSONPointer = JSONPointer.root): Boolean

    abstract fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicOutput

    abstract fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            DetailedOutput

    fun validateBasic(json: JSONValue?, instanceLocation: JSONPointer = JSONPointer.root) =
            validateBasic(JSONPointer.root, json, instanceLocation)

    fun validateDetailed(json: JSONValue?, instanceLocation: JSONPointer = JSONPointer.root) =
            validateDetailed(JSONPointer.root, json, instanceLocation)

    fun validate(json: String, instanceLocation: JSONPointer = JSONPointer.root) =
            validate(JSON.parse(json), instanceLocation)

    fun validateBasic(json: String, instanceLocation: JSONPointer = JSONPointer.root) =
            validateBasic(JSONPointer.root, JSON.parse(json), instanceLocation)

    fun validateDetailed(json: String, instanceLocation: JSONPointer = JSONPointer.root) =
            validateDetailed(JSONPointer.root, JSON.parse(json), instanceLocation)

    fun createAnnotation(relativeLocation: JSONPointer, instanceLocation: JSONPointer, annotation: String,
                         errors: List<Output>? = null, annotations: List<Output>? = null): DetailedOutput {
        return DetailedOutput.createAnnotation(relativeLocation.schemaURIFragment(), absoluteLocation,
                instanceLocation.schemaURIFragment(), annotation, errors, annotations)
    }

    fun createError(relativeLocation: JSONPointer, instanceLocation: JSONPointer, error: String,
                    errors: List<Output>? = null, annotations: List<Output>? = null): DetailedOutput {
        return DetailedOutput.createError(relativeLocation.schemaURIFragment(), absoluteLocation,
                instanceLocation.schemaURIFragment(), error, errors, annotations)
    }

    fun createBasicErrorEntry(relativeLocation: JSONPointer, instanceLocation: JSONPointer, error: String) =
            BasicErrorEntry(relativeLocation.schemaURIFragment(), absoluteLocation,
                    instanceLocation.schemaURIFragment(), error)

    fun createBasicError(relativeLocation: JSONPointer, instanceLocation: JSONPointer, error: String): BasicOutput {
        return BasicOutput(false, listOf(createBasicErrorEntry(relativeLocation, instanceLocation, error)))
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is JSONSchema && uri == other.uri && location == other.location

    override fun hashCode(): Int = uri.hashCode() xor location.hashCode()

    @Suppress("EqualsOrHashCode")
    class True(uri: URI?, location: JSONPointer) : JSONSchema(uri, location) {

        override fun validate(json: JSONValue?, instanceLocation: JSONPointer) = true

        override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer) =
                BasicOutput.trueOutput

        override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer) =
                createAnnotation(relativeLocation, instanceLocation, "Constant schema \"true\"")

        override fun equals(other: Any?): Boolean = this === other || other is True && super.equals(other)

    }

    @Suppress("EqualsOrHashCode")
    class False(uri: URI?, location: JSONPointer) : JSONSchema(uri, location) {

        override fun validate(json: JSONValue?, instanceLocation: JSONPointer) = false

        override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer) =
                createBasicError(relativeLocation, instanceLocation, "Constant schema \"false\"")

        override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer) =
                createError(relativeLocation, instanceLocation, "Constant schema \"false\"")

        override fun equals(other: Any?): Boolean = this === other || other is False && super.equals(other)

    }

    class Not(uri: URI?, location: JSONPointer, private val nested: JSONSchema) : JSONSchema(uri, location) {

        override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("not")

        override fun validate(json: JSONValue?, instanceLocation: JSONPointer) =
                !nested.validate(json, instanceLocation)

        override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
                BasicOutput {
            val nestedOutput = nested.validateBasic(relativeLocation, json, instanceLocation)
            return if (nestedOutput.valid)
                createBasicError(relativeLocation, instanceLocation, "Schema \"not\" - target was valid")
            else
                BasicOutput.trueOutput
        }

        override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
                DetailedOutput {
            val nestedOutput = nested.validateDetailed(relativeLocation, json, instanceLocation)
            return if (nestedOutput.valid)
                createError(relativeLocation, instanceLocation, "Schema \"not\" - target was valid",
                        annotations = listOf(nestedOutput))
            else
                createAnnotation(relativeLocation, instanceLocation, "Schema \"not\" - target was invalid",
                        errors = listOf(nestedOutput))
        }

        override fun equals(other: Any?): Boolean =
                this === other || other is Not && super.equals(other) && nested == other.nested

        override fun hashCode(): Int = super.hashCode() xor nested.hashCode()

    }

    abstract class SubSchema(uri: URI?, location: JSONPointer) : JSONSchema(uri, location)

    abstract class Validator(uri: URI?, location: JSONPointer) : JSONSchema(uri, location) {

        abstract fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
                BasicErrorEntry?

        override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
                BasicOutput {
            val result = getErrorEntry(relativeLocation, json, instanceLocation)
            return if (result == null)
                BasicOutput.trueOutput
            else
                BasicOutput(false, listOf(result))
        }

        override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
                DetailedOutput {
            val result = getErrorEntry(relativeLocation, json, instanceLocation)
            return if (result == null)
                createAnnotation(relativeLocation, instanceLocation, "Validation successful")
            else
                createError(relativeLocation, instanceLocation, result.error)
        }

    }

    class General(val schemaVersion: String, override val title: String?, override val description: String?, uri: URI?,
            location: JSONPointer, val children: List<JSONSchema>) : JSONSchema(uri, location) {

        override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
            for (child in children)
                if (!child.validate(json, instanceLocation))
                    return false
            return true
        }

        override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
                BasicOutput {
            val errors = children.fold(mutableListOf<BasicErrorEntry>()) { list, child ->
                child.validateBasic(child.childLocation(relativeLocation), json, instanceLocation).let { basicOutput ->
                    list.addAllFromNullable(basicOutput.errors)
                }
            }
            if (errors.isEmpty())
                return BasicOutput.trueOutput
            errors.add(0, createBasicErrorEntry(relativeLocation, instanceLocation, "A subschema had errors"))
            return BasicOutput(false, errors)
        }

        override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
                DetailedOutput {
            val errors = mutableListOf<Output>()
            for (child in children) {
                child.validateDetailed(child.childLocation(relativeLocation), json, instanceLocation).let {
                    if (!it.valid)
                        errors.add(it)
                }
            }
            if (errors.isEmpty())
                return createAnnotation(relativeLocation, instanceLocation, "Validation successful")
            return createError(relativeLocation, instanceLocation, "A subschema had errors", errors)
        }

        override fun equals(other: Any?): Boolean =
                this === other || other is General && super.equals(other) && schemaVersion == other.schemaVersion &&
                        title == other.title && description == other.description && children == other.children

        override fun hashCode(): Int = super.hashCode() xor schemaVersion.hashCode() xor title.hashCode() xor
                description.hashCode() xor children.hashCode()

    }

    companion object {

        val parser by lazy { Parser() }

        fun parseFile(filename: String): JSONSchema = parser.parseFile(filename)

        fun parse(file: File): JSONSchema = parser.parse(file)

        fun allOf(uri: URI?, location: JSONPointer, array: List<JSONSchema>) = AllOfSchema(uri, location, array)

        fun anyOf(uri: URI?, location: JSONPointer, array: List<JSONSchema>) = AnyOfSchema(uri, location, array)

        fun oneOf(uri: URI?, location: JSONPointer, array: List<JSONSchema>) = OneOfSchema(uri, location, array)

        fun <T: Any> MutableList<T>.addAllFromNullable(collection: Collection<T>?): MutableList<T> {
            if (collection != null)
                addAll(collection)
            return this
        }

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
