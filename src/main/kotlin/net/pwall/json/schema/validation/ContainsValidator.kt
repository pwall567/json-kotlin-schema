/*
 * @(#) ContainsValidator.kt
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

package net.pwall.json.schema.validation

import java.net.URI
import net.pwall.json.JSONSequence
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry

class ContainsValidator(uri: URI?, location: JSONPointer, private val containsSchema: JSONSchema,
            private val minContains: Int?, private val maxContains: Int?) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("contains")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONSequence<*>)
            return true
        var count = 0
        for (i in instance.indices) {
            if (containsSchema.validate(json, instanceLocation.child(i)))
                count++
        }
        if (count == 0)
            return false
        minContains?.let {
            if (count < it)
                return false
        }
        maxContains?.let {
            if (count > it)
                return false
        }
        return true
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONSequence<*>)
            return null
        var count = 0
        for (i in instance.indices) {
            if (containsSchema.validate(json, instanceLocation.child(i)))
                count++
        }
        if (count == 0)
            return createBasicErrorEntry(relativeLocation, instanceLocation, "No matching entry")
        minContains?.let {
            if (count < it)
                return BasicErrorEntry(
                        relativeLocation.parent().child("minContains").schemaURIFragment(),
                        uri?.let { x -> "$x${location.parent().child("minContains").schemaURIFragment()}" },
                        instanceLocation.schemaURIFragment(),
                        "Matching entry minimum $it, was $count"
                )
        }
        maxContains?.let {
            if (count > it)
                return BasicErrorEntry(
                        relativeLocation.parent().child("maxContains").schemaURIFragment(),
                        uri?.let { x -> "$x${location.parent().child("maxContains").schemaURIFragment()}" },
                        instanceLocation.schemaURIFragment(),
                        "Matching entry maximum $it, was $count"
                )
        }
        return null
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is ContainsValidator && super.equals(other) && containsSchema == other.containsSchema &&
            minContains == other.minContains && maxContains == other.maxContains

    override fun hashCode(): Int = super.hashCode() xor containsSchema.hashCode() xor minContains.hashCode() xor
            maxContains.hashCode()

}
