package net.pwall.json.schema.output

open class Output(
        val valid: Boolean
) {

    override fun equals(other: Any?): Boolean {
        return this === other || other is Output && valid == other.valid
    }

    override fun hashCode() = valid.hashCode()

}

class BasicOutput(
        valid: Boolean,
        val keywordLocation: String,
        val absoluteKeywordLocation: String? = null,
        val instanceLocation: String,
        val error: String? = null,
        val annotation: String? = null,
        val errors: List<Output>? = null,
        val annotations: List<Output>? = null
) : Output(valid) {

    override fun equals(other: Any?): Boolean {
        return this === other || other is BasicOutput && super.equals(other) &&
                keywordLocation == other.keywordLocation && absoluteKeywordLocation == other.absoluteKeywordLocation &&
                instanceLocation == other.instanceLocation && error == other.error && annotation == other.annotation &&
                errors == other.errors && annotations == other.annotations
    }

    override fun hashCode(): Int {
        return super.hashCode() xor keywordLocation.hashCode()
    }

    companion object {

        fun createError(keywordLocation: String, absoluteKeywordLocation: String? = null, instanceLocation: String,
                        error: String, errors: List<Output>? = null, annotations: List<Output>? = null): BasicOutput {
            return BasicOutput(false, keywordLocation, absoluteKeywordLocation, instanceLocation, error, null,
                    errors?.let { if (it.isEmpty()) null else it }, annotations?.let { if (it.isEmpty()) null else it })
        }

        fun createAnnotation(keywordLocation: String, absoluteKeywordLocation: String? = null, instanceLocation: String,
                             annotation: String, errors: List<Output>? = null, annotations: List<Output>? = null): BasicOutput {
            return BasicOutput(true, keywordLocation, absoluteKeywordLocation, instanceLocation, null, annotation,
                    errors?.let { if (it.isEmpty()) null else it }, annotations?.let { if (it.isEmpty()) null else it })
        }

    }

}
