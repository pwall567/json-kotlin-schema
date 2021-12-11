/*
 * @(#) AdditionalItemsSchema.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2021 Peter Wall
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

import net.pwall.json.JSONSequence
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.DetailedOutput

class AdditionalItemsSchema(private val parent: General, uri: URI?, location: JSONPointer, val schema: JSONSchema) :
        JSONSchema.SubSchema(uri, location) {

    private val itemsSchema: ItemsSchema? by lazy {
        parent.children.filterIsInstance<ItemsSchema>().firstOrNull()
    }

    private val itemsArraySchema: ItemsArraySchema? by lazy {
        parent.children.filterIsInstance<ItemsArraySchema>().firstOrNull()
    }

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("additionalItems")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONSequence<*>)
            return true
        if (itemsSchema == null) {
            itemsArraySchema?.let {
                if (instance.size > it.itemSchemaList.size) {
                    for (i in it.itemSchemaList.size until instance.size)
                        if (!schema.validate(json, instanceLocation.child(i)))
                            return false
                }
            }
        }
        return true
    }

    override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicOutput {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONSequence<*>)
            return BasicOutput.trueOutput
        val errors = mutableListOf<BasicErrorEntry>()
        if (itemsSchema == null) {
            itemsArraySchema?.let {
                if (instance.size > it.itemSchemaList.size) {
                    for (i in it.itemSchemaList.size until instance.size) {
                        schema.validateBasic(relativeLocation, json, instanceLocation.child(i)).let { output ->
                            if (!output.valid) {
                                errors.add(createBasicErrorEntry(relativeLocation, instanceLocation.child(i),
                                        "Additional item $i found but was invalid"))
                                errors.addAllFromNullable(output.errors)
                            }
                        }
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
        if (instance !is JSONSequence<*>)
            return createAnnotation(relativeLocation, instanceLocation, "Value is not an array")
        val errors = mutableListOf<DetailedOutput>()
        if (itemsSchema == null) {
            itemsArraySchema?.let {
                if (instance.size > it.itemSchemaList.size) {
                    for (i in it.itemSchemaList.size until instance.size) {
                        schema.validateDetailed(relativeLocation, json, instanceLocation.child(i)).let { output ->
                            if (!output.valid) {
                                errors.add(createError(relativeLocation, instanceLocation.child(i),
                                        "Additional item $i found but was invalid", errors = listOf(output)))
                            }
                        }
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

    /**
     * Compare two `AdditionalItemsSchema` objects for equality.
     *
     * This will report two objects as equal even if the `parent` is different, because including the parent in the
     * comparison leads to infinite recursion.  This should be a reasonable approach, since the function will mostly be
     * called as a nested equality comparison on the parent object.
     *
     * @param   other       the other object
     * @return              `true` if the two objects are equal, subject to the above note.
     */
    override fun equals(other: Any?): Boolean = this === other || other is AdditionalItemsSchema &&
            super.equals(other) && schema == other.schema

    override fun hashCode(): Int = super.hashCode() xor schema.hashCode()

}
