/*
 * @(#) FormatValidator.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2021 Peter Wall
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

import kotlin.math.floor

import java.math.BigDecimal
import java.net.URI

import net.pwall.json.JSONDecimal
import net.pwall.json.JSONDouble
import net.pwall.json.JSONFloat
import net.pwall.json.JSONLong
import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
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
        return checker.check(instanceLocation.eval(json))
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = instanceLocation.eval(json)
        return if (checker.check(instance)) null else createBasicErrorEntry(relativeLocation, instanceLocation,
                "Value fails format check \"${checker.name}\", was ${instance.toJSON()}")
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is FormatValidator && super.equals(other) && checker == other.checker

    override fun hashCode(): Int = super.hashCode() xor checker.hashCode()

    interface FormatChecker {
        val name: String
        fun check(value: JSONValue?): Boolean
    }

    object DateTimeFormatChecker : FormatChecker {

        override val name: String = "date-time"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isDateTime(value.value)

    }

    object DateFormatChecker : FormatChecker {

        override val name: String = "date"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isDate(value.value)

    }

    object TimeFormatChecker : FormatChecker {

        override val name: String = "time"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isTime(value.value)

    }

    object DurationFormatChecker : FormatChecker {

        override val name: String = "duration"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isDuration(value.value)

    }

    object EmailFormatChecker : FormatChecker {

        override val name: String = "email"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isEmail(value.value)

    }

    object HostnameFormatChecker : FormatChecker {

        override val name: String = "hostname"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isHostname(value.value)

    }

    object IPV4FormatChecker : FormatChecker {

        override val name: String = "ipv4"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isIPV4(value.value)

    }

    object IPV6FormatChecker : FormatChecker {

        override val name: String = "ipv6"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isIPV6(value.value)

    }

    object URIFormatChecker : FormatChecker {

        override val name: String = "uri"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isURI(value.value)

    }

    object URIReferenceFormatChecker : FormatChecker {

        override val name: String = "uri-reference"

        override fun check(value: JSONValue?): Boolean =
                value !is JSONString || JSONValidation.isURIReference(value.value)

    }

    object UUIDFormatChecker : FormatChecker {

        override val name: String = "uuid"

        override fun check(value: JSONValue?): Boolean = value !is JSONString || JSONValidation.isUUID(value.value)

    }

    object JSONPointerFormatChecker : FormatChecker {

        override val name: String = "json-pointer"

        override fun check(value: JSONValue?): Boolean =
                value !is JSONString || JSONValidation.isJSONPointer(value.value)

    }

    object RelativeJSONPointerFormatChecker : FormatChecker {

        override val name: String = "relative-json-pointer"

        override fun check(value: JSONValue?): Boolean =
                value !is JSONString || JSONValidation.isRelativeJSONPointer(value.value)

    }

    // Not yet implemented:
    //   idn-email
    //   idn-hostname
    //   iri
    //   iri-reference
    //   uri-template
    //   regex

    // Additional formats for OpenAPI: int32 and int64

    object Int64FormatChecker : FormatChecker {

        override val name: String = "int64"

        override fun check(value: JSONValue?): Boolean = when (value) {
            is JSONDecimal -> {
                try {
                    value.bigDecimalValue().setScale(0) in BigDecimal(Long.MIN_VALUE)..BigDecimal(Long.MAX_VALUE)
                }
                catch (e: ArithmeticException) {
                    false
                }
            }
            is JSONDouble -> {
                val doubleValue = value.value
                doubleValue == floor(doubleValue) && doubleValue in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()
            }
            is JSONFloat -> {
                val floatValue = value.value
                floatValue == floor(floatValue) && floatValue in Long.MIN_VALUE.toFloat()..Long.MAX_VALUE.toFloat()
            }
            else -> true // includes JSONInteger, JSONLong, JSONZero
        }

    }

    object Int32FormatChecker : FormatChecker {

        override val name: String = "int32"

        override fun check(value: JSONValue?): Boolean = when (value) {
            is JSONLong -> value.value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
            is JSONDecimal -> {
                try {
                    value.bigDecimalValue().setScale(0) in BigDecimal(Int.MIN_VALUE)..BigDecimal(Int.MAX_VALUE)
                }
                catch (e: ArithmeticException) {
                    false
                }
            }
            is JSONDouble -> {
                val doubleValue = value.value
                doubleValue == floor(doubleValue) && doubleValue in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()
            }
            is JSONFloat -> {
                val floatValue = value.value
                floatValue == floor(floatValue) && floatValue in Int.MIN_VALUE.toFloat()..Int.MAX_VALUE.toFloat()
            }
            else -> true // includes JSONInteger, JSONZero
        }

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

        override fun equals(other: Any?): Boolean = this === other ||
                other is DelegatingFormatChecker && name == other.name && validators.contentEquals(other.validators)

        override fun hashCode(): Int = name.hashCode() xor validators.hashCode()

    }

    companion object {

        private val checkers = listOf(
            DateTimeFormatChecker,
            DateFormatChecker,
            TimeFormatChecker,
            DurationFormatChecker,
            EmailFormatChecker,
            HostnameFormatChecker,
            IPV4FormatChecker,
            IPV6FormatChecker,
            URIFormatChecker,
            URIReferenceFormatChecker,
            UUIDFormatChecker,
            JSONPointerFormatChecker,
            RelativeJSONPointerFormatChecker,
            Int32FormatChecker,
            Int64FormatChecker
        )

        fun findChecker(keyword: String): FormatChecker? = checkers.find { it.name == keyword }

    }

}
