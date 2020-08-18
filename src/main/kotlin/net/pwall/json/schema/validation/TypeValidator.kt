/*
 * @(#) TypeValidator.kt
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

import net.pwall.json.JSONArray
import net.pwall.json.JSONBoolean
import net.pwall.json.JSONInteger
import net.pwall.json.JSONLong
import net.pwall.json.JSONNumberValue
import net.pwall.json.JSONObject
import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.JSONZero
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry

class TypeValidator(uri: URI?, location: JSONPointer, val types: List<Type>) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("type")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        for (type in types) {
            when (type) {
                Type.NULL -> if (instance == null) return true
                Type.BOOLEAN -> if (instance is JSONBoolean) return true
                Type.OBJECT -> if (instance is JSONObject) return true
                Type.ARRAY -> if (instance is JSONArray) return true
                Type.NUMBER -> if (instance is JSONNumberValue) return true
                Type.STRING -> if (instance is JSONString) return true
                Type.INTEGER -> if (instance is JSONInteger || instance is JSONLong || instance is JSONZero)
                    return true
            }
        }
        return false
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? = if (validate(json, instanceLocation)) null else
                    createBasicErrorEntry(relativeLocation, instanceLocation,
                            "Incorrect type, expected ${types.joinToString(separator = " or ") { it.value }}")

    override fun equals(other: Any?): Boolean =
            this === other || other is TypeValidator && super.equals(other) && types == other.types

    override fun hashCode(): Int = super.hashCode() xor types.hashCode()

}
