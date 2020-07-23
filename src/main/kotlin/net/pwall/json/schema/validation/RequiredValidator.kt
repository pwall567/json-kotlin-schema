package net.pwall.json.schema.validation

import net.pwall.json.JSONObject
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI

class RequiredValidator(uri: URI?, location: JSONPointer, private val properties: List<String>) :
        JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("required")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONObject)
            return trueOutput
        val errors = mutableListOf<Output>()
        properties.forEachIndexed { i, propertyName ->
            if (!instance.containsKey(propertyName))
                errors.add(createErrorForChild(i, relativeLocation.child(i), instanceLocation,
                        "Required property \"$propertyName\" was missing"))
        }
        return validationResult(relativeLocation, instanceLocation, errors)
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is RequiredValidator && super.equals(other) && properties == other.properties

    override fun hashCode(): Int = super.hashCode() xor properties.hashCode()

}
