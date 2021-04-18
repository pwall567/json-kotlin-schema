/*
 * @(#) UniqueItemsValidator.kt
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

import java.math.BigDecimal
import java.net.URI

import net.pwall.json.JSONBoolean
import net.pwall.json.JSONDecimal
import net.pwall.json.JSONDouble
import net.pwall.json.JSONFloat
import net.pwall.json.JSONInteger
import net.pwall.json.JSONLong
import net.pwall.json.JSONMapping
import net.pwall.json.JSONSequence
import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.JSONZero
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry

@Suppress("EqualsOrHashCode")
class UniqueItemsValidator(uri: URI?, location: JSONPointer): JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("uniqueItems")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        return instance !is JSONSequence<*> || uniqueItems(instance)
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = instanceLocation.eval(json)
        return if (instance !is JSONSequence<*> || uniqueItems(instance)) null else
                createBasicErrorEntry(relativeLocation, instanceLocation, "Array items not unique")
    }

    override fun equals(other: Any?): Boolean = this === other || other is UniqueItemsValidator && super.equals(other)

    companion object {

        fun uniqueItems(array: JSONSequence<*>): Boolean {
            for (i in 1 until array.size) {
                val current = array[i]
                for (j in 0 until i)
                    if (itemsEqual(array[j], current))
                        return false
            }
            return true
        }

        private fun itemsEqual(a: JSONValue?, b: JSONValue?): Boolean {
            when (a) {
                is JSONMapping<*> -> {
                    if (b !is JSONMapping<*> || a.size != b.size)
                        return false
                    for (e in a.entries)
                        if (!itemsEqual(e.value, b[e.key]))
                            return false
                    return true
                }
                is JSONSequence<*> -> {
                    if (b !is JSONSequence<*> || a.size != b.size)
                        return false
                    for (i in a.indices)
                        if (!itemsEqual(a[i], b[i]))
                            return false
                    return true
                }
                is JSONString -> return b is JSONString && a.get() == b.get()
                is JSONBoolean -> return b is JSONBoolean && a.get() == b.get()
                is JSONDecimal -> {
                    return when (b) {
                        is JSONDecimal -> a.get().compareTo(b.get()) == 0
                        is JSONDouble -> a.get().compareTo(BigDecimal(b.get())) == 0
                        is JSONFloat -> a.get().compareTo(BigDecimal(b.get().toDouble())) == 0
                        is JSONLong -> a.get().compareTo(BigDecimal(b.get())) == 0
                        is JSONInteger -> a.get().compareTo(BigDecimal(b.get())) == 0
                        is JSONZero -> a.get().compareTo(BigDecimal.ZERO) == 0
                        else -> false
                    }
                }
                is JSONDouble -> {
                    return when (b) {
                        is JSONDecimal -> BigDecimal(a.get()).compareTo(b.get()) == 0
                        is JSONDouble -> a.get() == b.get()
                        is JSONFloat -> a.get() == b.get().toDouble()
                        is JSONLong -> a.get() == b.get().toDouble()
                        is JSONInteger -> a.get() == b.get().toDouble()
                        is JSONZero -> a.get() == 0.0
                        else -> false
                    }
                }
                is JSONFloat -> {
                    return when (b) {
                        is JSONDecimal -> BigDecimal(a.get().toDouble()).compareTo(b.get()) == 0
                        is JSONDouble -> a.get().toDouble() == b.get()
                        is JSONFloat -> a.get() == b.get()
                        is JSONLong -> a.get().toDouble() == b.get().toDouble()
                        is JSONInteger -> a.get().toDouble() == b.get().toDouble()
                        is JSONZero -> a.get() == 0.0F
                        else -> false
                    }
                }
                is JSONLong -> {
                    return when (b) {
                        is JSONDecimal -> BigDecimal(a.get()).compareTo(b.get()) == 0
                        is JSONDouble -> a.get().toDouble() == b.get()
                        is JSONFloat -> a.get().toDouble() == b.get().toDouble()
                        is JSONLong -> a.get() == b.get()
                        is JSONInteger -> a.get() == b.get().toLong()
                        is JSONZero -> a.get() == 0L
                        else -> false
                    }
                }
                is JSONInteger -> {
                    return when (b) {
                        is JSONDecimal -> BigDecimal(a.get()).compareTo(b.get()) == 0
                        is JSONDouble -> a.get().toDouble() == b.get()
                        is JSONFloat -> a.get().toDouble() == b.get().toDouble()
                        is JSONLong -> a.get().toLong() == b.get()
                        is JSONInteger -> a.get() == b.get()
                        is JSONZero -> a.get() == 0
                        else -> false
                    }
                }
                is JSONZero -> {
                    return when (b) {
                        is JSONDecimal -> BigDecimal.ZERO.compareTo(b.get()) == 0
                        is JSONDouble -> b.get() == 0.0
                        is JSONFloat -> b.get() == 0.0F
                        is JSONLong -> b.get() == 0L
                        is JSONInteger -> b.get() == 0
                        is JSONZero -> true
                        else -> false
                    }
                }
                else -> return false
            }
        }

    }

}
