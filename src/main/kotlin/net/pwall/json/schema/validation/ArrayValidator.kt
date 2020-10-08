package net.pwall.json.schema.validation

import java.net.URI

import net.pwall.json.JSONSequence
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry

class ArrayValidator(uri: URI?, location: JSONPointer, val condition: ValidationType, val value: Int) :
        JSONSchema.Validator(uri, location) {

    enum class ValidationType(val keyword: String) {
        MAX_ITEMS("maxItems"),
        MIN_ITEMS("minItems")
    }

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child(condition.keyword)

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        return instance !is JSONSequence<*> || validNumberOfItems(instance)
    }

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? {
        val instance = instanceLocation.eval(json)
        return if (instance !is JSONSequence<*> || validNumberOfItems(instance)) null else
                createBasicErrorEntry(relativeLocation, instanceLocation,
                        "Array fails number of items check: ${condition.keyword} $value, was ${instance.size}")
    }

    private fun validNumberOfItems(instance: JSONSequence<*>): Boolean = when (condition) {
        ValidationType.MAX_ITEMS -> instance.size <= value
        ValidationType.MIN_ITEMS -> instance.size >= value
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is ArrayValidator && super.equals(other) && condition == other.condition && value == other.value

    override fun hashCode(): Int = super.hashCode() xor condition.hashCode() xor value.hashCode()

}
