package net.pwall.json.schema.validation

import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI

class PatternValidator(uri: URI?, location: JSONPointer, val regex: Regex) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("pattern")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONString)
            return trueOutput
        return if (instance.get() matches regex) trueOutput else createError(relativeLocation, instanceLocation,
                "String doesn't match pattern $regex - ${instance.toErrorDisplay()}")
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is PatternValidator && super.equals(other) && regex == other.regex

    override fun hashCode(): Int = super.hashCode() xor regex.hashCode()

}
