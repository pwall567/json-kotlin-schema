/*
 * @(#) AdditionalItemsSchema.kt
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

import net.pwall.json.JSONArray
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.DetailedOutput

class AdditionalItemsSchema(private val parent: General, uri: URI?, location: JSONPointer, val schema: JSONSchema) :
        JSONSchema.SubSchema(uri, location) {

    private val itemsSchemaPresent: Boolean by lazy {
        parent.children.filterIsInstance<ItemsSchema>().isNotEmpty()
    }

    private val itemsArraySchemaSize: Int by lazy {
        parent.children.filterIsInstance<ItemsArraySchema>().firstOrNull()?.itemSchemaList?.size ?: 0
    }

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("additionalItems")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONArray)
            return true
        if (!itemsSchemaPresent && instance.size > itemsArraySchemaSize) {
            for (i in itemsArraySchemaSize until instance.size) {
                if (!schema.validate(json, instanceLocation.child(i)))
                    return false
            }
        }
        return true
    }

    override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicOutput {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONArray)
            return BasicOutput.trueOutput
        val errors = mutableListOf<BasicErrorEntry>()
        if (!itemsSchemaPresent && instance.size > itemsArraySchemaSize) {
            for (i in itemsArraySchemaSize until instance.size) {
                schema.validateBasic(relativeLocation, json, instanceLocation.child(i)).let {
                    if (!it.valid) {
                        errors.add(createBasicErrorEntry(relativeLocation, instanceLocation.child(i),
                                "Additional item $i found but was invalid"))
                        errors.addAllFromNullable(it.errors)
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
        if (instance !is JSONArray)
            return createAnnotation(relativeLocation, instanceLocation, "Value is not an array")
        val errors = mutableListOf<DetailedOutput>()
        if (!itemsSchemaPresent && instance.size > itemsArraySchemaSize) {
            for (i in itemsArraySchemaSize until instance.size) {
                schema.validateDetailed(relativeLocation, json, instanceLocation.child(i)).let {
                    if (!it.valid) {
                        errors.add(createError(relativeLocation, instanceLocation.child(i),
                                "Additional item $i found but was invalid", errors = listOf(it)))
                    }
                }
            }
        }
        return when (errors.size) {
            0 -> createAnnotation(relativeLocation, instanceLocation, "items are valid")
            1 -> errors[0]
            else -> createError(relativeLocation, instanceLocation, "Errors in items", errors = errors)
        }
    }

    override fun equals(other: Any?): Boolean = this === other || other is AdditionalItemsSchema &&
            super.equals(other) && parent == other.parent && schema == other.schema

    override fun hashCode(): Int = super.hashCode() xor parent.hashCode() xor schema.hashCode()

}
