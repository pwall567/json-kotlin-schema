/*
 * @(#) StringValidator.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2023 Peter Wall
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

import io.kjson.JSONString
import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer
import io.kjson.pointer.get

import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.pipeline.IntCounter
import net.pwall.pipeline.codec.UTF16_CodePoint

class StringValidator(uri: URI?, location: JSONPointer, val condition: ValidationType, val value: Int) :
        JSONSchema.Validator(uri, location) {

    enum class ValidationType(val keyword: String) {
        MAX_LENGTH("maxLength"),
        MIN_LENGTH("minLength")
    }

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child(condition.keyword)

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = json[instanceLocation]
        return instance !is JSONString || validLength(instance)
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = json[instanceLocation]
        return if (instance !is JSONString || validLength(instance)) null else
                createBasicErrorEntry(relativeLocation, instanceLocation,
                        "String fails length check: ${condition.keyword} $value, was ${instance.unicodeLength()}")
    }

    private fun validLength(instance: JSONString): Boolean = when (condition) {
        ValidationType.MAX_LENGTH -> instance.unicodeLength() <= value
        ValidationType.MIN_LENGTH -> instance.unicodeLength() >= value
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is StringValidator && super.equals(other) && condition == other.condition &&
                    value == other.value

    override fun hashCode(): Int = super.hashCode() xor condition.hashCode() xor value

    companion object {

        // use a UTF-16 to code point converter and simply count the output
        fun JSONString.unicodeLength(): Int = UTF16_CodePoint(IntCounter()).apply { accept(value) }.result

    }

}
