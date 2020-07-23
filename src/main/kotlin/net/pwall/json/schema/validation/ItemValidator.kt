package net.pwall.json.schema.validation

import net.pwall.json.JSONArray
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI

class ItemValidator(uri: URI?, location: JSONPointer, val itemSchema: JSONSchema) :
        JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("items")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONArray)
            return trueOutput
        val errors = mutableListOf<Output>()
        for (i in instance.indices) {
            val itemResult = itemSchema.validate(relativeLocation, json, instanceLocation.child(i))
            if (!itemResult.valid)
                errors.add(itemResult)
        }
        return validationResult(relativeLocation, instanceLocation, errors)
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is ItemValidator && super.equals(other) && itemSchema == other.itemSchema

    override fun hashCode(): Int = super.hashCode() xor itemSchema.hashCode()

}
