package net.pwall.json.schema

import net.pwall.json.JSONException

class JSONSchemaException(message: String) : JSONException(message) {

    constructor(message: String, throwable: Throwable) : this(message) {
        initCause(throwable)
    }

}
