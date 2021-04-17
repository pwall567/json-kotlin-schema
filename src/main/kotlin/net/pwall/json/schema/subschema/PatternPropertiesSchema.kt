/*
 * @(#) PatternPropertiesSchema.kt
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

import net.pwall.json.JSONMapping
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.DetailedOutput

class PatternPropertiesSchema(uri: URI?, location: JSONPointer, val properties: List<Pair<Regex, JSONSchema>>) :
        JSONSchema.SubSchema(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("patternProperties")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONMapping<*>)
            return true
        for ((propertyPattern, propertySchema) in properties) {
            for (name in instance.keys) {
                if (propertyPattern.containsMatchIn(name)) {
                    if (!propertySchema.validate(json, instanceLocation.child(name)))
                        return false
                }
            }
        }
        return true
    }

    override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicOutput {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONMapping<*>)
            return BasicOutput.trueOutput
        val errors = mutableListOf<BasicErrorEntry>()
        for ((propertyPattern, propertySchema) in properties) {
            for (name in instance.keys) {
                if (propertyPattern.containsMatchIn(name)) {
                    propertySchema.validateBasic(relativeLocation.child(propertyPattern.toString()), json,
                            instanceLocation.child(name)).let { propertyResult ->
                        if (!propertyResult.valid)
                            errors.addAllFromNullable(propertyResult.errors)
                    }
                }
            }
        }
        if (errors.isEmpty())
            return BasicOutput.trueOutput
        return BasicOutput(false, errors)
    }

    override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            DetailedOutput {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONMapping<*>)
            return createAnnotation(relativeLocation, instanceLocation, "Value is not an object")
        val errors = mutableListOf<DetailedOutput>()
        for ((propertyPattern, propertySchema) in properties) {
            for (name in instance.keys) {
                if (propertyPattern.containsMatchIn(name)) {
                    val propertyResult = propertySchema.validateDetailed(
                            relativeLocation.child(propertyPattern.toString()), json, instanceLocation.child(name))
                    if (!propertyResult.valid)
                        errors.add(propertyResult)
                }
            }
        }
        return when (errors.size) {
            0 -> createAnnotation(relativeLocation, instanceLocation, "patternProperties are valid")
            1 -> errors[0]
            else -> createError(relativeLocation, instanceLocation, "Errors in patternProperties", errors = errors)
        }
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is PatternPropertiesSchema && super.equals(other) && properties == other.properties

    override fun hashCode(): Int = super.hashCode() xor properties.hashCode()

}
