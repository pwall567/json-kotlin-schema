package net.pwall.json.schema.validation

import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI

class StringValidator(uri: URI?, location: JSONPointer, private val condition: ValidationType, private val value: Int) :
        JSONSchema.Validator(uri, location) {

    enum class ValidationType(val keyword: String) {
        MAX_LENGTH("maxLength"),
        MIN_LENGTH("minLength")
    }

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child(condition.keyword)

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONString)
            return trueOutput
        val result: Boolean = when (condition) {
            ValidationType.MAX_LENGTH -> instance.length <= value
            ValidationType.MIN_LENGTH -> instance.length >= value
        }
        return if (result) trueOutput else createError(relativeLocation, instanceLocation,
                "String fails length check: ${condition.keyword} $value, was ${instance.length}")
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is StringValidator && super.equals(other) && condition == other.condition &&
                    value == other.value

    override fun hashCode(): Int = super.hashCode() xor condition.hashCode() xor value.hashCode()

}
