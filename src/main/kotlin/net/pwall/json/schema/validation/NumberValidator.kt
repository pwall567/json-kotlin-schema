package net.pwall.json.schema.validation

import net.pwall.json.JSONDecimal
import net.pwall.json.JSONDouble
import net.pwall.json.JSONFloat
import net.pwall.json.JSONNumberValue
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI

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

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONNumberValue)
            return trueOutput
        val result: Boolean = when (condition) {
            ValidationType.MULTIPLE_OF -> multipleOf(instance)
            ValidationType.MAXIMUM -> maximum(instance)
            ValidationType.EXCLUSIVE_MAXIMUM -> exclusiveMaximum(instance)
            ValidationType.MINIMUM -> minimum(instance)
            ValidationType.EXCLUSIVE_MINIMUM -> exclusiveMinimum(instance)
        }
        return if (result) trueOutput else createError(relativeLocation, instanceLocation,
                "Number fails check: ${condition.keyword} $value, was $instance")
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

    override fun equals(other: Any?): Boolean =
            this === other || other is NumberValidator && super.equals(other) && value == other.value &&
                    condition == other.condition

    override fun hashCode(): Int = super.hashCode() xor value.hashCode() xor condition.hashCode()

    companion object {

        val typeKeywords: List<String> = ValidationType.values().map { it.keyword }

        fun findType(keyword: String): ValidationType {
            ValidationType.values().forEach { if (it.keyword == keyword) return it }
            throw RuntimeException("Can't find validation type - should not happen")
        }

    }

}
