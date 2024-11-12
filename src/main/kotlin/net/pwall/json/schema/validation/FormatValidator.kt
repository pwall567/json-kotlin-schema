/*
 * @(#) FormatValidator.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2021, 2024 Peter Wall
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

package net.pwall.json.schema.validation

import java.net.URI
import java.util.function.Predicate

import io.kjson.JSONNumber
import io.kjson.JSONString
import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer
import io.kjson.pointer.get

import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.validation.JSONValidation

class FormatValidator(
    uri: URI?,
    location: JSONPointer,
    val checker: FormatChecker
) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("format")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        return checker.check(json[instanceLocation])
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = json[instanceLocation]
        return if (checker.check(instance)) null else
                checker.getBasicErrorEntry(this, relativeLocation, instanceLocation, json)
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is FormatValidator && super.equals(other) && checker == other.checker

    override fun hashCode(): Int = super.hashCode() xor checker.hashCode()

    interface FormatChecker {

        val name: String

        fun check(value: JSONValue?): Boolean

        fun getBasicErrorEntry(
            schema: JSONSchema,
            relativeLocation: JSONPointer,
            instanceLocation: JSONPointer,
            json: JSONValue?,
        ):  BasicErrorEntry {
            return schema.createBasicErrorEntry(
                relativeLocation = relativeLocation,
                instanceLocation = instanceLocation,
                error = "Value fails format check \"$name\", was ${json[instanceLocation]?.toJSON()}",
            )
        }

    }

    // Additional formats for OpenAPI: int32 and int64

    object Int64FormatChecker : FormatChecker {

        override val name: String = "int64"

        override fun check(value: JSONValue?): Boolean = value !is JSONNumber || value.isLong()

    }

    object Int32FormatChecker : FormatChecker {

        override val name: String = "int32"

        override fun check(value: JSONValue?): Boolean = value !is JSONNumber || value.isInt()

    }

    class NullFormatChecker(override val name: String) : FormatChecker {

        override fun check(value: JSONValue?): Boolean = true

        override fun equals(other: Any?): Boolean = this === other ||
                other is NullFormatChecker && name == other.name

        override fun hashCode(): Int = name.hashCode()

    }

    class DelegatingFormatChecker(override val name: String, vararg val validators: Validator) : FormatChecker {

        override fun check(value: JSONValue?): Boolean {
            for (validator in validators)
                if (!validator.validate(value))
                    return false
            return true
        }

        override fun getBasicErrorEntry(
            schema: JSONSchema,
            relativeLocation: JSONPointer,
            instanceLocation: JSONPointer,
            json: JSONValue?,
        ): BasicErrorEntry {
            for (validator in validators)
                validator.getErrorEntry(relativeLocation.child(name), json, instanceLocation)?.let { return it }
            return super.getBasicErrorEntry(schema, relativeLocation, instanceLocation, json)
        }

        override fun equals(other: Any?): Boolean = this === other ||
                other is DelegatingFormatChecker && name == other.name && validators.contentEquals(other.validators)

        override fun hashCode(): Int = name.hashCode() xor validators.hashCode()

    }

    class StringFormatChecker(override val name: String, private val predicate: Predicate<String>) : FormatChecker {

        override fun check(value: JSONValue?): Boolean = value !is JSONString || predicate.test(value.value)

    }

    companion object {

        private val checkers = listOf(
            Int32FormatChecker,
            Int64FormatChecker
        )

        fun findChecker(keyword: String): FormatChecker? {
            JSONValidation.validations[keyword]?.let { return StringFormatChecker(keyword, it) }
            return checkers.find { it.name == keyword }
        }

    }

}
