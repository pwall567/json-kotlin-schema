package net.pwall.json.schema.codegen

import net.pwall.json.schema.JSONSchema
import net.pwall.util.Strings

class NamedConstraints(schema: JSONSchema, val name: String) : Constraints(schema) {

    var overridingName: String? = null

    @Suppress("unused")
    val propertyName: String
        get() = name

    @Suppress("unused")
    val capitalisedName: String
        get() = Strings.capitalise(name)

    @Suppress("unused")
    val className: String
        get() = overridingName ?: capitalisedName

    @Suppress("unused")
    val nameFromURIOrName: String
        get() = nameFromURI ?: capitalisedName

}
