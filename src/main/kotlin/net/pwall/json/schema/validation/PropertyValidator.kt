package net.pwall.json.schema.validation

import net.pwall.json.JSONObject
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI

class PropertyValidator(uri: URI?, location: JSONPointer, private val properties: List<Pair<String, JSONSchema>>) :
        JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("properties")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONObject)
            return trueOutput
        val errors = mutableListOf<Output>()
        for (property in properties) {
            val propertyName = property.first
            if (instance.containsKey(propertyName)) {
                val propertyResult = property.second.validate(relativeLocation.child(propertyName), json,
                        instanceLocation.child(propertyName))
                if (!propertyResult.valid)
                    errors.add(propertyResult)
            }
        }
        return validationResult(relativeLocation, instanceLocation, errors)
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is PropertyValidator && super.equals(other) && properties == other.properties

    override fun hashCode(): Int = super.hashCode() xor properties.hashCode()

}
