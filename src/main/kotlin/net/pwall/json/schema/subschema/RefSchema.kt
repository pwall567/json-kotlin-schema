/*
 * @(#) RefSchema.kt
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

package net.pwall.json.schema.subschema

import java.net.URI

import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.DetailedOutput

class RefSchema(uri: URI?, location: JSONPointer, val target: JSONSchema) : JSONSchema.SubSchema(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("\$ref")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean =
            target.validate(json, instanceLocation)

    override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicOutput = target.validateBasic(relativeLocation, json, instanceLocation)

    override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            DetailedOutput {
        val refResult = target.validateDetailed(relativeLocation, json, instanceLocation)
        return if (refResult.valid)
            createAnnotation(relativeLocation, instanceLocation, "\$ref schema valid")
        else
            createError(relativeLocation, instanceLocation, "\$ref schema invalid", listOf(refResult))
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is RefSchema && super.equals(other) && target == other.target

    override fun hashCode(): Int = super.hashCode() xor target.hashCode()

}
