package net.pwall.json.schema.validation

import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI

class ConstValidator(uri: URI?, location: JSONPointer, val value: JSONValue?) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("const")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output =
            instanceLocation.eval(json).let { if (it == value) trueOutput else
                    createError(relativeLocation, instanceLocation, "Does not match constant: ${it.toErrorDisplay()}") }

    override fun equals(other: Any?): Boolean =
            this === other || other is ConstValidator && super.equals(other) && value == other.value

    override fun hashCode(): Int = super.hashCode() xor value.hashCode()

}
