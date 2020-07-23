package net.pwall.json.schema.validation

import net.pwall.json.JSONString
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.Output
import java.net.URI
import java.net.URISyntaxException
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

class FormatValidator(uri: URI?, location: JSONPointer, private val type: FormatType) :
        JSONSchema.Validator(uri, location) {

    enum class FormatType(val keyword: String) {
        DATE_TIME("date-time"),
        DATE("date"),
        TIME("time"),
        DURATION("duration"),
        EMAIL("email"),
        IDN_EMAIL("idn-email"),
        HOSTNAME("hostname"),
        IDN_HOSTNAME("idn-hostname"),
        IPV4("ipv4"),
        IPV6("ipv6"),
        URI("uri"),
        URI_REFERENCE("uri-reference"),
        IRI("iri"),
        IRI_REFERENCE("iri-reference"),
        UUID("uuid"),
        URI_TEMPLATE("uri-template"),
        JSON_POINTER("json-pointer"),
        RELATIVE_JSON_POINTER("relative-json-pointer"),
        REGEX("regex")
    }

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("format")

    override fun validate(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer): Output {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONString)
            return trueOutput
        val str = instance.get()
        val result: Boolean = when (type) {
            FormatType.DATE_TIME -> str.isValidDateTime()
            FormatType.DATE -> str.isValidDate()
            FormatType.TIME -> str.isValidTime()
            FormatType.DURATION -> str.isValidDuration()
            FormatType.EMAIL -> true
            FormatType.IDN_EMAIL -> true
            FormatType.HOSTNAME -> true
            FormatType.IDN_HOSTNAME -> true
            FormatType.IPV4 -> true
            FormatType.IPV6 -> true
            FormatType.URI -> str.isValidURI()
            FormatType.URI_REFERENCE -> true
            FormatType.IRI -> true
            FormatType.IRI_REFERENCE -> true
            FormatType.UUID -> str.isValidUUID()
            FormatType.URI_TEMPLATE -> true
            FormatType.JSON_POINTER -> str.isValidJSONPointer()
            FormatType.RELATIVE_JSON_POINTER -> true
            FormatType.REGEX -> str.isValidRegex()
        }
        return if (result) trueOutput else createError(relativeLocation, instanceLocation,
                "String fails format check: ${type.keyword}, was $str")
    }

    companion object {

        fun String.isValidDateTime(): Boolean = try {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(this)
                true
            }
            catch (ignored: DateTimeParseException) {
                false
            }

        fun String.isValidDate(): Boolean = try {
                DateTimeFormatter.ISO_LOCAL_DATE.parse(this)
                true
            }
            catch (ignored: DateTimeParseException) {
                false
            }

        fun String.isValidTime(): Boolean = try {
                DateTimeFormatter.ISO_OFFSET_TIME.parse(this)
                true
            }
            catch (ignored: DateTimeParseException) {
                false
            }

        fun String.isValidDuration(): Boolean = try {
                Duration.parse(this)
                true
            }
            catch (ignored: DateTimeParseException) {
                false
            }

        fun String.isValidURI(): Boolean = try {
                URI(this) // is this sufficient to validate string as a URI?
                true
            }
            catch (ignored: URISyntaxException) {
                false
            }

        fun String.isValidUUID(): Boolean = try {
                UUID.fromString(this)
                true
            }
            catch (ignored: Exception) {
                false
            }

        fun String.isValidJSONPointer(): Boolean = try {
                JSONPointer(this)
                true
            }
            catch (ignored: Exception) {
                false
            }

        fun String.isValidRegex(): Boolean = try {
                Regex(this)
                true
            }
            catch (ignored: Exception) {
                false
            }

        val typeKeywords: List<String> = FormatType.values().map { it.keyword }

        fun findType(keyword: String): FormatType {
            FormatType.values().forEach { if (it.keyword == keyword) return it }
            throw RuntimeException("Can't find format type - should not happen")
        }

    }

}
