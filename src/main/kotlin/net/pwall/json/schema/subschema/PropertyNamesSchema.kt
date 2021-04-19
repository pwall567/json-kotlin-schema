/*
 * @(#) PropertyNamesSchema.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2021 Peter Wall
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
import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.DetailedOutput
import net.pwall.json.schema.output.Output

class PropertyNamesSchema(uri: URI?, location: JSONPointer, private val nameSchema: JSONSchema) :
        JSONSchema.SubSchema(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("propertyNames")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONMapping<*>)
            return true
        for (propertyName in instance.keys)
            if (!nameSchema.validate(JSONString(propertyName), JSONPointer.root))
                return false
        return true
    }

    override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicOutput {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONMapping<*>)
            return BasicOutput.trueOutput
        val errors = mutableListOf<BasicErrorEntry>()
        for (propertyName in instance.keys) {
            nameSchema.validateBasic(relativeLocation, JSONString(propertyName), JSONPointer.root).let { output ->
                if (!output.valid)
                    output.errors?.forEach {
                        errors.add(BasicErrorEntry(it.keywordLocation, it.absoluteKeywordLocation,
                                instanceLocation.child(propertyName).schemaURIFragment(), it.error))
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
        val errors = mutableListOf<Output>()
        val annotations = mutableListOf<Output>()
        for (propertyName in instance.keys) {
            val instanceString = instanceLocation.child(propertyName).schemaURIFragment()
            nameSchema.validateDetailed(JSONString(propertyName), JSONPointer.root).let { output ->
                output.mapInstance(instanceString).let {
                    if (it.valid)
                        annotations.add(it)
                    else
                        errors.add(it)
                }
            }
        }
        if (errors.isEmpty())
            return createAnnotation(relativeLocation, instanceLocation, "Property names are valid",
                    annotations = annotations)
        (errors.first() as? DetailedOutput)?.let { return it }
        return createError(relativeLocation, instanceLocation, "Errors in property names", errors = errors,
                annotations = annotations.takeIf { it.isNotEmpty() })
    }

    private fun Output.mapInstance(instanceString: String): Output = when (this) {
        is DetailedOutput -> {
            DetailedOutput(
                    valid = valid,
                    keywordLocation = keywordLocation,
                    absoluteKeywordLocation = absoluteKeywordLocation,
                    instanceLocation = instanceString,
                    error = error,
                    annotation = annotation,
                    errors = errors?.map { it.mapInstance(instanceString) },
                    annotations = annotations?.map { it.mapInstance(instanceString) }
            )
        }
        else -> this
    }

}
