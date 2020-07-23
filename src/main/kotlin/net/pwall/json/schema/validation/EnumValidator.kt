package net.pwall.json.schema.validation

import net.pwall.json.JSONArray
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI

class EnumValidator(uri: URI?, location: JSONPointer, val array: JSONArray) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("enum")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
        val instance = instanceLocation.eval(json)
        array.forEach { if (instance == it) return trueOutput }
        return createError(relativeLocation, instanceLocation, "Not in enumerated values: ${instance.toErrorDisplay()}")
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is EnumValidator && super.equals(other) && array == other.array

    override fun hashCode(): Int = super.hashCode() xor array.hashCode()

}
