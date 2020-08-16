/*
 * @(#) NumberValidator.kt
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

package net.pwall.json.schema.validation

import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI

import net.pwall.json.JSONDecimal
import net.pwall.json.JSONDouble
import net.pwall.json.JSONFloat
import net.pwall.json.JSONNumberValue
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry

class NumberValidator(uri: URI?, location: JSONPointer, val value: Number, val condition: ValidationType) :
        JSONSchema.Validator(uri, location) {

    enum class ValidationType(val keyword: String) {
        MULTIPLE_OF("multipleOf"),
        MAXIMUM("maximum"),
        EXCLUSIVE_MAXIMUM("exclusiveMaximum"),
        MINIMUM("minimum"),
        EXCLUSIVE_MINIMUM("exclusiveMinimum")
    }

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child(condition.keyword)

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        return instance !is JSONNumberValue || validNumber(instance)
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = instanceLocation.eval(json)
        return if (instance !is JSONNumberValue || validNumber(instance)) null else
                createBasicErrorEntry(relativeLocation, instanceLocation,
                        "Number fails check: ${condition.keyword} $value, was $instance")
    }

    private fun validNumber(instance: JSONNumberValue) = when (condition) {
        ValidationType.MULTIPLE_OF -> multipleOf(instance)
        ValidationType.MAXIMUM -> maximum(instance)
        ValidationType.EXCLUSIVE_MAXIMUM -> exclusiveMaximum(instance)
        ValidationType.MINIMUM -> minimum(instance)
        ValidationType.EXCLUSIVE_MINIMUM -> exclusiveMinimum(instance)
    }

    private fun multipleOf(instance: JSONNumberValue): Boolean = when (instance) {
        is JSONDecimal -> instance.toBigDecimal().rem(value.toBigDecimal()).compareTo(BigDecimal.ZERO) == 0
        is JSONDouble -> instance.toDouble().rem(value.toDouble()) == 0.0
        is JSONFloat -> instance.toFloat().rem(value.toFloat()) == 0.0F
        else -> instance.toLong().rem(value.toLong()) == 0L
    }

    private fun maximum(instance: JSONNumberValue): Boolean = when (instance) {
        is JSONDecimal -> instance.toBigDecimal() <= value.toBigDecimal()
        is JSONDouble -> instance.toDouble() <= value.toDouble()
        is JSONFloat -> instance.toFloat() <= value.toFloat()
        else -> instance.toLong() <= value.toLong()
    }

    private fun exclusiveMaximum(instance: JSONNumberValue): Boolean = when (instance) {
        is JSONDecimal -> instance.toBigDecimal() < value.toBigDecimal()
        is JSONDouble -> instance.toDouble() < value.toDouble()
        is JSONFloat -> instance.toFloat() < value.toFloat()
        else -> instance.toLong() < value.toLong()
    }

    private fun minimum(instance: JSONNumberValue): Boolean = when (instance) {
        is JSONDecimal -> instance.toBigDecimal() >= value.toBigDecimal()
        is JSONDouble -> instance.toDouble() >= value.toDouble()
        is JSONFloat -> instance.toFloat() >= value.toFloat()
        else -> instance.toLong() >= value.toLong()
    }

    private fun exclusiveMinimum(instance: JSONNumberValue): Boolean = when (instance) {
        is JSONDecimal -> instance.toBigDecimal() > value.toBigDecimal()
        is JSONDouble -> instance.toDouble() > value.toDouble()
        is JSONFloat -> instance.toFloat() > value.toFloat()
        else -> instance.toLong() > value.toLong()
    }

    private fun Number.toBigDecimal() = when (this) {
        is BigDecimal -> this
        is BigInteger -> BigDecimal(this)
        is Double -> BigDecimal(this)
        is Float -> BigDecimal(this.toDouble())
        else -> BigDecimal(this.toLong())
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is NumberValidator && super.equals(other) && value == other.value && condition == other.condition

    override fun hashCode(): Int = super.hashCode() xor value.hashCode() xor condition.hashCode()

    companion object {

        val typeKeywords: List<String> = ValidationType.values().map { it.keyword }

        fun findType(keyword: String): ValidationType {
            ValidationType.values().forEach { if (it.keyword == keyword) return it }
            throw RuntimeException("Can't find validation type - should not happen")
        }

    }

}
