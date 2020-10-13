package net.pwall.json.schema

import kotlin.test.Test
import kotlin.test.expect
import kotlin.test.fail
import net.pwall.json.JSON
import net.pwall.json.JSONString
import net.pwall.json.schema.parser.Parser
import net.pwall.json.schema.validation.StringValidator

class JSONSchemaCustomValidatorTest {

    @Test fun `should make use of custom validator`() {
        val parser = Parser()
        parser.customValidationHandler = { key, uri, location, value ->
            when (key) {
                "x-test" -> {
                    if (value is JSONString && value.get() == "not-empty")
                        StringValidator(uri, location, StringValidator.ValidationType.MIN_LENGTH, 1)
                    else
                        throw RuntimeException("Unknown type")
                }
                else -> null
            }
        }
        val filename = "src/test/resources/test-custom-validator.schema.json"
        val schema = parser.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"Q"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        val json2 = JSON.parse("""{"aaa":""}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-custom#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect("A subschema had errors") { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-custom#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("A subschema had errors") { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/x-test") { it.keywordLocation }
            expect("http://pwall.net/test-custom#/properties/aaa/x-test") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("String fails length check: minLength 1, was 0") { it.error }
        }
    }

}
