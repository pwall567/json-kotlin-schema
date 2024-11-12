/*
 * @(#) EnumValidator.kt
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

import io.kjson.JSON.displayValue
import io.kjson.JSONArray
import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer
import io.kjson.pointer.get

import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry

class EnumValidator(uri: URI?, location: JSONPointer, val array: JSONArray) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("enum")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = json[instanceLocation]
        array.forEach { if (instance == it) return true }
        return false
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = json[instanceLocation]
        array.forEach { if (instance == it) return null }
        return createBasicErrorEntry(relativeLocation, instanceLocation,
                "Not in enumerated values: ${instance.displayValue()}")
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is EnumValidator && super.equals(other) && array == other.array

    override fun hashCode(): Int = super.hashCode() xor array.hashCode()

}
