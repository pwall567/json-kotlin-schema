/*
 * @(#) FormatValidator.kt
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

package net.pwall.json.schema.validation

import java.net.URI

import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.validation.JSONValidation

class FormatValidator(uri: URI?, location: JSONPointer, val type: FormatType) : JSONSchema.Validator(uri, location) {

    enum class FormatType(val keyword: String) {
        DATE_TIME("date-time"),
        DATE("date"),
        TIME("time"),
        DURATION("duration"),
        EMAIL("email"),
        IDN_EMAIL("idn-email"),
        HOSTNAME("hostname"),
        IDN_HOSTNAME("idn-hostname"),
        IPV4("ipv4"),
        IPV6("ipv6"),
        URI("uri"),
        URI_REFERENCE("uri-reference"),
        IRI("iri"),
        IRI_REFERENCE("iri-reference"),
        UUID("uuid"),
        URI_TEMPLATE("uri-template"),
        JSON_POINTER("json-pointer"),
        RELATIVE_JSON_POINTER("relative-json-pointer"),
        REGEX("regex")
    }

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("format")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONString)
            return true
        return validFormat(instance.get())
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONString)
            return null
        val str = instance.get()
        return if (validFormat(str)) null else createBasicErrorEntry(relativeLocation, instanceLocation,
                "String fails format check: ${type.keyword}, was $str")
    }

    private fun validFormat(str: String): Boolean {
        return when (type) {
            FormatType.DATE_TIME -> JSONValidation.isDateTime(str)
            FormatType.DATE -> JSONValidation.isDate(str)
            FormatType.TIME -> JSONValidation.isTime(str)
            FormatType.DURATION -> JSONValidation.isDuration(str)
            FormatType.EMAIL -> JSONValidation.isEmail(str)
            FormatType.IDN_EMAIL -> true
            FormatType.HOSTNAME -> JSONValidation.isHostname(str)
            FormatType.IDN_HOSTNAME -> true
            FormatType.IPV4 -> true
            FormatType.IPV6 -> true
            FormatType.URI -> true
            FormatType.URI_REFERENCE -> true
            FormatType.IRI -> true
            FormatType.IRI_REFERENCE -> true
            FormatType.UUID -> JSONValidation.isUUID(str)
            FormatType.URI_TEMPLATE -> true
            FormatType.JSON_POINTER -> true
            FormatType.RELATIVE_JSON_POINTER -> true
            FormatType.REGEX -> true
        }
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is FormatValidator && super.equals(other) && type == other.type

    override fun hashCode(): Int = super.hashCode() xor type.hashCode()

    companion object {

        val typeKeywords: List<String> = FormatType.values().map { it.keyword }

        fun findType(keyword: String): FormatType {
            FormatType.values().forEach { if (it.keyword == keyword) return it }
            throw RuntimeException("Can't find format type - should not happen")
        }

    }

}
