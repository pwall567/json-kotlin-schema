/*
 * @(#) NumberValidator.kt
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

package net.pwall.json.schema.validation

import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI

import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.JSONNumber
import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer
import io.kjson.pointer.get

import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchemaException
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

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = json[instanceLocation]
        return instance !is JSONNumber || validNumber(instance)
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = json[instanceLocation]
        return if (instance !is JSONNumber || validNumber(instance)) null else
                createBasicErrorEntry(relativeLocation, instanceLocation,
                        "Number fails check: ${condition.keyword} $value, was $instance")
    }

    private fun validNumber(instance: JSONNumber) = when (condition) {
        ValidationType.MULTIPLE_OF -> multipleOf(instance)
        ValidationType.MAXIMUM -> maximum(instance)
        ValidationType.EXCLUSIVE_MAXIMUM -> exclusiveMaximum(instance)
        ValidationType.MINIMUM -> minimum(instance)
        ValidationType.EXCLUSIVE_MINIMUM -> exclusiveMinimum(instance)
    }

    private fun multipleOf(instance: JSONNumber): Boolean = when (value) {
        is BigDecimal -> instance.toDecimal().rem(value).compareTo(BigDecimal.ZERO) == 0
        is Long -> when (instance) {
            is JSONDecimal -> instance.value.rem(BigDecimal(value)).compareTo(BigDecimal.ZERO) == 0
            is JSONLong -> instance.value.rem(value) == 0L
            is JSONInt -> instance.toLong().rem(value) == 0L
            else -> throw JSONSchemaException("Impossible type")
        }
        is Int -> when (instance) {
            is JSONDecimal -> instance.value.rem(BigDecimal(value)).compareTo(BigDecimal.ZERO) == 0
            is JSONLong -> instance.value.rem(value) == 0L
            is JSONInt -> instance.toLong().rem(value) == 0L
            else -> throw JSONSchemaException("Impossible type")
        }
        else -> throw JSONSchemaException("Impossible type")
    }

    private fun maximum(instance: JSONNumber): Boolean = when (instance) {
        is JSONDecimal -> instance.value <= value.toBigDecimal()
        else -> instance.toLong() <= value.toLong()
    }

    private fun exclusiveMaximum(instance: JSONNumber): Boolean = when (instance) {
        is JSONDecimal -> instance.value < value.toBigDecimal()
        else -> instance.toLong() < value.toLong()
    }

    private fun minimum(instance: JSONNumber): Boolean = when (instance) {
        is JSONDecimal -> instance.value >= value.toBigDecimal()
        else -> instance.toLong() >= value.toLong()
    }

    private fun exclusiveMinimum(instance: JSONNumber): Boolean = when (instance) {
        is JSONDecimal -> instance.value > value.toBigDecimal()
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

        val typeKeywords: List<String> = ValidationType.entries.map { it.keyword }

        fun findType(keyword: String): ValidationType {
            ValidationType.entries.forEach { if (it.keyword == keyword) return it }
            throw RuntimeException("Can't find validation type - should not happen")
        }

    }

}
