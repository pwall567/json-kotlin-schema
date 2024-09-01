/*
 * @(#) CombinationSchema.kt
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

import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer

import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.DetailedOutput

abstract class CombinationSchema(uri: URI?, location: JSONPointer, val name: String, val array: List<JSONSchema>) :
        JSONSchema.SubSchema(uri, location) {

    abstract fun resultValid(trueCount: Int): Boolean

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child(name)

    override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicOutput {
        val errors = mutableListOf<BasicErrorEntry>()
        var trueCount = 0
        array.forEachIndexed { i, schema ->
            schema.validateBasic(relativeLocation.child(i), json, instanceLocation).also { basicOutput ->
                if (basicOutput.valid)
                    trueCount++
                else
                    errors.addAllFromNullable(basicOutput.errors)
            }
        }
        if (resultValid(trueCount))
            return BasicOutput.trueOutput
        errors.add(0, createBasicErrorEntry(relativeLocation, instanceLocation, failureMessage(trueCount)))
        return BasicOutput(false, errors)
    }

    override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            DetailedOutput {
        val errors = mutableListOf<DetailedOutput>()
        val annotations = mutableListOf<DetailedOutput>()
        var trueCount = 0
        array.forEachIndexed { i, schema ->
            schema.validateDetailed(relativeLocation.child(i), json, instanceLocation).let {
                if (it.valid) {
                    trueCount++
                    annotations.add(it)
                }
                else
                    errors.add(it)
            }
        }
        return if (resultValid(trueCount))
            createAnnotation(relativeLocation, instanceLocation,
                    "Combination schema \"$name\" succeeds - $trueCount of ${array.size} valid", errors,
                    annotations)
        else
            createError(relativeLocation, instanceLocation, failureMessage(trueCount), errors, annotations)
    }

    private fun failureMessage(trueCount: Int) =
            "Combination schema \"$name\" fails - $trueCount of ${array.size} valid"

    override fun equals(other: Any?): Boolean =
            this === other || other is CombinationSchema && super.equals(other) && name == other.name &&
                    array == other.array

    override fun hashCode(): Int = super.hashCode() xor name.hashCode() xor array.hashCode()

}
