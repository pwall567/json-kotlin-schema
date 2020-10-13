package net.pwall.json.schema.validation

import java.net.URI
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry

class DelegatingValidator(uri: URI?, location: JSONPointer, private val childName: String,
        val validator: Validator) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child(childName)

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean =
            validator.validate(json, instanceLocation)

    override fun getErrorEntry(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicErrorEntry? = validator.getErrorEntry(relativeLocation, json, instanceLocation)

}
