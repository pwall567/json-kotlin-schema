/*
 * @(#) IfThenElseSchema.kt
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

class IfThenElseSchema(uri: URI?, location: JSONPointer, private val ifSchema: JSONSchema,
        private val thenSchema: JSONSchema?, private val elseSchema: JSONSchema?) :
                JSONSchema.SubSchema(uri, location) {

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        return if (ifSchema.validate(json, instanceLocation)) thenSchema.validate( json, instanceLocation)
                else elseSchema.validate(json, instanceLocation)
    }

    override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicOutput {
        return if (ifSchema.validate(json, instanceLocation))
                thenSchema.validateBasic(relativeLocation.child("then"), json, instanceLocation)
                        else elseSchema.validateBasic(relativeLocation.child("else"), json, instanceLocation)
    }

    override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            DetailedOutput {
        val ifResult = ifSchema.validateDetailed(relativeLocation.child("if"), json, instanceLocation)
        if (ifResult.valid) {
            if (thenSchema == null)
                return createAnnotation(relativeLocation, instanceLocation, "\"if\" schema is true",
                        annotations = listOf(ifResult))
            val thenResult = thenSchema.validateDetailed(relativeLocation.child("then"), json, instanceLocation)
            return if (thenResult.valid)
                createAnnotation(relativeLocation, instanceLocation, "\"if\" schema is true",
                        annotations = listOf(ifResult, thenResult))
            else
                createError(relativeLocation, instanceLocation, "A subschema had errors.",
                        errors = listOf(thenResult), annotations = listOf(ifResult))
        }
        else {
            if (elseSchema == null)
                return createAnnotation(relativeLocation, instanceLocation, "\"if\" schema is false",
                        annotations = listOf(ifResult))
            val elseResult = elseSchema.validateDetailed(relativeLocation.child("else"), json, instanceLocation)
            return if (elseResult.valid)
                createAnnotation(relativeLocation, instanceLocation, "\"if\" schema is false",
                        annotations = listOf(ifResult, elseResult))
            else
                createError(relativeLocation, instanceLocation, "A subschema had errors.",
                        errors = listOf(elseResult), annotations = listOf(ifResult))
        }
    }

    companion object {

        fun JSONSchema?.validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
            if (this == null)
                return true
            return validate(json, instanceLocation)
        }

        fun JSONSchema?.validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
                BasicOutput {
            if (this == null)
                return BasicOutput.trueOutput
            return validateBasic(relativeLocation, json, instanceLocation)
        }

    }

}