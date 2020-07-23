package net.pwall.json.schema.validation

import net.pwall.json.JSONArray
import net.pwall.json.JSONBoolean
import net.pwall.json.JSONInteger
import net.pwall.json.JSONLong
import net.pwall.json.JSONNumberValue
import net.pwall.json.JSONObject
import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.JSONZero
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI

class TypeValidator(uri: URI?, location: JSONPointer, val types: List<Type>) : JSONSchema.Validator(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("type")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
        val instance = instanceLocation.eval(json)
        for (type in types) {
            when (type) {
                Type.NULL -> if (instance == null) return trueOutput
                Type.BOOLEAN -> if (instance is JSONBoolean) return trueOutput
                Type.OBJECT -> if (instance is JSONObject) return trueOutput
                Type.ARRAY -> if (instance is JSONArray) return trueOutput
                Type.NUMBER -> if (instance is JSONNumberValue) return trueOutput
                Type.STRING -> if (instance is JSONString) return trueOutput
                Type.INTEGER -> if (instance is JSONInteger || instance is JSONLong || instance is JSONZero)
                        return trueOutput
            }
        }
        return createError(relativeLocation, instanceLocation,
                "Incorrect type, expected ${types.joinToString(separator = " or ") { it.value }}")
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is TypeValidator && super.equals(other) && types == other.types

    override fun hashCode(): Int = super.hashCode() xor types.hashCode()

}
