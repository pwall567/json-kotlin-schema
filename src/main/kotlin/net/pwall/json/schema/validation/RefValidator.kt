package net.pwall.json.schema.validation

import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI

class RefValidator(uri: URI?, location: JSONPointer, val target: JSONSchema) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("\$ref")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output =
            target.validate(relativeLocation, json, instanceLocation)

    override fun equals(other: Any?): Boolean =
            this === other || other is RefValidator && super.equals(other) && target == other.target

    override fun hashCode(): Int = super.hashCode() xor target.hashCode()

}
