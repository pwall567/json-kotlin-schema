/*
 * @(#) JSONSchemaTest.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2021, 2022, 2024 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.json.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.expect
import kotlin.test.fail

import java.io.File
import java.net.URI

import net.pwall.json.JSONObject
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.JSON

class JSONSchemaTest {

    @Test fun `should validate example schema`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example.json"))
        expect(true) { schema.validate(json) }
        expect(true) { schema.validateBasic(json).valid }
        expect(true) { schema.validateDetailed(json).valid }
    }

    @Test fun `should validate example schema from File`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(File(filename))
        val jsonString = File("src/test/resources/example.json").readText()
        expect(true) { schema.validate(jsonString) }
        expect(true) { schema.validateBasic(jsonString).valid }
        expect(true) { schema.validateDetailed(jsonString).valid }
    }

    @Test fun `should validate example schema from string`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val jsonString = File("src/test/resources/example.json").readText()
        expect(true) { schema.validate(jsonString) }
        expect(true) { schema.validateBasic(jsonString).valid }
        expect(true) { schema.validateDetailed(jsonString).valid }
    }

    @Test fun `should validate example schema in YAML form`() {
        val filename = "src/test/resources/example.schema.yaml"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example.json"))
        expect(true) { schema.validate(json) }
        expect(true) { schema.validateBasic(json).valid }
        expect(true) { schema.validateDetailed(json).valid }
    }

    @Test fun `should validate example schema with missing property`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example-error1.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(2) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/required/1") { it.keywordLocation }
            expect("http://pwall.net/test#/required") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect("Required property \"name\" not found") { it.error }
        }
        expect(false) { schema.validateDetailed(json).valid }
    }

    @Test fun `should validate example schema with wrong property type`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example-error2.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/price") { it.keywordLocation }
            expect("http://pwall.net/test#/properties/price") { it.absoluteKeywordLocation }
            expect("#/price") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/price/type") { it.keywordLocation }
            expect("http://pwall.net/test#/properties/price/type") { it.absoluteKeywordLocation }
            expect("#/price") { it.instanceLocation }
            expect("Incorrect type, expected number") { it.error }
        }
        expect(false) { schema.validateDetailed(json).valid }
    }

    @Test fun `should validate example schema with value out of range`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example-error3.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/price") { it.keywordLocation }
            expect("http://pwall.net/test#/properties/price") { it.absoluteKeywordLocation }
            expect("#/price") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/price/minimum") { it.keywordLocation }
            expect("http://pwall.net/test#/properties/price/minimum") { it.absoluteKeywordLocation }
            expect("#/price") { it.instanceLocation }
            expect("Number fails check: minimum 0, was -1") { it.error }
        }
        expect(false) { schema.validateDetailed(json).valid }
    }

    @Test fun `should validate example schema with array item of wrong type`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example-error4.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(4) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/tags") { it.keywordLocation }
            expect("http://pwall.net/test#/properties/tags") { it.absoluteKeywordLocation }
            expect("#/tags") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/tags/items") { it.keywordLocation }
            expect("http://pwall.net/test#/properties/tags/items") { it.absoluteKeywordLocation }
            expect("#/tags/2") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[3].let {
            expect("#/properties/tags/items/type") { it.keywordLocation }
            expect("http://pwall.net/test#/properties/tags/items/type") { it.absoluteKeywordLocation }
            expect("#/tags/2") { it.instanceLocation }
            expect("Incorrect type, expected string") { it.error }
        }
        expect(false) { schema.validateDetailed(json).valid }
    }

    @Test fun `should validate example schema with reference`() {
        val filename = "src/test/resources/test-ref.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/test-ref.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(4) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-ref#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/bbb") { it.keywordLocation }
            expect("http://pwall.net/test-ref#/properties/bbb") { it.absoluteKeywordLocation }
            expect("#/bbb") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/bbb/\$ref") { it.keywordLocation }
            expect("http://pwall.net/test-ref#/\$defs/amount") { it.absoluteKeywordLocation }
            expect("#/bbb") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[3].let {
            expect("#/properties/bbb/\$ref/type") { it.keywordLocation }
            expect("http://pwall.net/test-ref#/\$defs/amount/type") { it.absoluteKeywordLocation }
            expect("#/bbb") { it.instanceLocation }
            expect("Incorrect type, expected number") { it.error }
        }
        expect(false) { schema.validateDetailed(json).valid }
    }

    @Test fun `should validate additional properties`() {
        val filename = "src/test/resources/test-additional.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA","bbb":1,"ccc":99}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"AAA","bbb":1,"ccc":"99"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(4) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-additional#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/additionalProperties") { it.keywordLocation }
            expect("http://pwall.net/test-additional#/additionalProperties") { it.absoluteKeywordLocation }
            expect("#/ccc") { it.instanceLocation }
            expect("Additional property 'ccc' found but was invalid") { it.error }
        }
        errors[2].let {
            expect("#/additionalProperties") { it.keywordLocation }
            expect("http://pwall.net/test-additional#/additionalProperties") { it.absoluteKeywordLocation }
            expect("#/ccc") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[3].let {
            expect("#/additionalProperties/type") { it.keywordLocation }
            expect("http://pwall.net/test-additional#/additionalProperties/type") { it.absoluteKeywordLocation }
            expect("#/ccc") { it.instanceLocation }
            expect("Incorrect type, expected integer") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate additional properties not allowed`() {
        val filename = "src/test/resources/test-additional-false.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"AAA","bbb":1}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-additional-false#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/additionalProperties") { it.keywordLocation }
            expect("http://pwall.net/test-additional-false#/additionalProperties") { it.absoluteKeywordLocation }
            expect("#/bbb") { it.instanceLocation }
            expect("Additional property 'bbb' found but was invalid") { it.error }
        }
        errors[2].let {
            expect("#/additionalProperties") { it.keywordLocation }
            expect("http://pwall.net/test-additional-false#/additionalProperties") { it.absoluteKeywordLocation }
            expect("#/bbb") { it.instanceLocation }
            expect("Constant schema \"false\"") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate enum`() {
        val filename = "src/test/resources/test-enum.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-enum#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-enum#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/enum") { it.keywordLocation }
            expect("http://pwall.net/test-enum#/properties/aaa/enum") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("Not in enumerated values: \"ZZZZZZZZZZZZZZZ ... ZZZZZZZZZZZZZZZ\"") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate const`() {
        val filename = "src/test/resources/test-const.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-const#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-const#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/const") { it.keywordLocation }
            expect("http://pwall.net/test-const#/properties/aaa/const") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("Does not match constant: \"ZZZZZZZZZZZZZZZ ... ZZZZZZZZZZZZZZZ\"") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate string length`() {
        val filename = "src/test/resources/test-string-length.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-string-length#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-string-length#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/maxLength") { it.keywordLocation }
            expect("http://pwall.net/test-string-length#/properties/aaa/maxLength") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("String fails length check: maxLength 10, was 66") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate string pattern`() {
        val filename = "src/test/resources/test-string-pattern.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"A001"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"9999"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-string-pattern#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-string-pattern#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/pattern") { it.keywordLocation }
            expect("http://pwall.net/test-string-pattern#/properties/aaa/pattern") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("String doesn't match pattern ^[A-Z][0-9]{3}\$ - \"9999\"") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate string format`() {
        val filename = "src/test/resources/test-string-format.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"dateTimeTest":"2020-07-22T19:29:33.456+10:00"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"dateTimeTest":"wrong"}""")
        expect(false) { schema.validate(json2) }
        val validateResult2 = schema.validateBasic(json2)
        expect(false) { validateResult2.valid }
        val errors2 = validateResult2.errors ?: fail()
        expect(3) { errors2.size }
        errors2[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-string-format#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors2[1].let {
            expect("#/properties/dateTimeTest") { it.keywordLocation }
            expect("http://pwall.net/test-string-format#/properties/dateTimeTest") { it.absoluteKeywordLocation }
            expect("#/dateTimeTest") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors2[2].let {
            expect("#/properties/dateTimeTest/format") { it.keywordLocation }
            expect("http://pwall.net/test-string-format#/properties/dateTimeTest/format") { it.absoluteKeywordLocation }
            expect("#/dateTimeTest") { it.instanceLocation }
            expect("Value fails format check \"date-time\", was \"wrong\"") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
        val json3 = JSON.parse("""{"dateTest":"2020-07-22"}""")
        expect(true) { schema.validate(json3) }
        expect(true) { schema.validateBasic(json3).valid }
        expect(true) { schema.validateDetailed(json3).valid }
        val json4 = JSON.parse("""{"dateTest":"wrong"}""")
        expect(false) { schema.validate(json4) }
        val validateResult4 = schema.validateBasic(json4)
        expect(false) { validateResult4.valid }
        val errors4 = validateResult4.errors ?: fail()
        expect(3) { errors4.size }
        errors4[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-string-format#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors4[1].let {
            expect("#/properties/dateTest") { it.keywordLocation }
            expect("http://pwall.net/test-string-format#/properties/dateTest") { it.absoluteKeywordLocation }
            expect("#/dateTest") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors4[2].let {
            expect("#/properties/dateTest/format") { it.keywordLocation }
            expect("http://pwall.net/test-string-format#/properties/dateTest/format") { it.absoluteKeywordLocation }
            expect("#/dateTest") { it.instanceLocation }
            expect("Value fails format check \"date\", was \"wrong\"") { it.error }
        }
        expect(false) { schema.validateDetailed(json4).valid }
        val json5 = JSON.parse("""{"durationTest":"P1M"}""")
        expect(true) { schema.validate(json5) }
        expect(true) { schema.validateBasic(json5).valid }
        expect(true) { schema.validateDetailed(json5).valid }
        val json6 = JSON.parse("""{"durationTest":"wrong"}""")
        expect(false) { schema.validate(json6) }
        val validateResult6 = schema.validateBasic(json6)
        expect(false) { validateResult6.valid }
        val errors6 = validateResult6.errors ?: fail()
        expect(3) { errors6.size }
        errors6[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-string-format#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors6[1].let {
            expect("#/properties/durationTest") { it.keywordLocation }
            expect("http://pwall.net/test-string-format#/properties/durationTest") { it.absoluteKeywordLocation }
            expect("#/durationTest") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors6[2].let {
            expect("#/properties/durationTest/format") { it.keywordLocation }
            expect("http://pwall.net/test-string-format#/properties/durationTest/format") { it.absoluteKeywordLocation }
            expect("#/durationTest") { it.instanceLocation }
            expect("Value fails format check \"duration\", was \"wrong\"") { it.error }
        }
        expect(false) { schema.validateDetailed(json6).valid }
    }

    @Test fun `should validate schema with anyOf`() {
        val filename = "src/test/resources/test-anyof.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"2020-07-22"}""")
        expect(true) { schema.validate(json2) }
        expect(true) { schema.validateBasic(json2).valid }
        expect(true) { schema.validateDetailed(json2).valid }
        val json3 = JSON.parse("""{"aaa":"wrong"}""")
        expect(false) { schema.validate(json3) }
        val validateResult = schema.validateBasic(json3)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(7) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-anyof#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-anyof#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/anyOf") { it.keywordLocation }
            expect("http://pwall.net/test-anyof#/properties/aaa/anyOf") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("Combination schema \"anyOf\" fails - 0 of 2 valid") { it.error }
        }
        errors[3].let {
            expect("#/properties/aaa/anyOf/0") { it.keywordLocation }
            expect("http://pwall.net/test-anyof#/properties/aaa/anyOf/0") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[4].let {
            expect("#/properties/aaa/anyOf/0/const") { it.keywordLocation }
            expect("http://pwall.net/test-anyof#/properties/aaa/anyOf/0/const") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("Does not match constant: \"wrong\"") { it.error }
        }
        errors[5].let {
            expect("#/properties/aaa/anyOf/1") { it.keywordLocation }
            expect("http://pwall.net/test-anyof#/properties/aaa/anyOf/1") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[6].let {
            expect("#/properties/aaa/anyOf/1/format") { it.keywordLocation }
            expect("http://pwall.net/test-anyof#/properties/aaa/anyOf/1/format") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("Value fails format check \"date\", was \"wrong\"") { it.error }
        }
        expect(false) { schema.validateDetailed(json3).valid }
    }

    @Test fun `should validate schema with not`() {
        val filename = "src/test/resources/test-not.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"BBB"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"AAA"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-not#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-not#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/not") { it.keywordLocation }
            expect("http://pwall.net/test-not#/properties/aaa/not") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("Schema \"not\" - target was valid") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate schema with if-then-else`() {
        val filename = "src/test/resources/test-if-then-else.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"U","bbb":"ae102612-e023-11ea-a115-6b5393cb81fd"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        val json2 = JSON.parse("""{"aaa":"U","bbb":"2020-08-17T19:30:00+10:00"}""")
        expect(false) { schema.validate(json2) }
        val validateResult2 = schema.validateBasic(json2)
        expect(false) { validateResult2.valid }
        val errors2 = validateResult2.errors ?: fail()
        expect(4) { errors2.size }
        errors2[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-if-then-else#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors2[1].let {
            expect("#/then") { it.keywordLocation }
            expect("http://pwall.net/test-if-then-else#/then") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors2[2].let {
            expect("#/then/properties/bbb") { it.keywordLocation }
            expect("http://pwall.net/test-if-then-else#/then/properties/bbb") { it.absoluteKeywordLocation }
            expect("#/bbb") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors2[3].let {
            expect("#/then/properties/bbb/format") { it.keywordLocation }
            expect("http://pwall.net/test-if-then-else#/then/properties/bbb/format") { it.absoluteKeywordLocation }
            expect("#/bbb") { it.instanceLocation }
            expect("Value fails format check \"uuid\", was \"2020-08-17T19:30:00+10:00\"") { it.error }
        }
        val json3 = JSON.parse("""{"aaa":"D","bbb":"2020-08-17T19:30:00+10:00"}""")
        expect(true) { schema.validate(json3) }
        expect(true) { schema.validateBasic(json3).valid }
        val json4 = JSON.parse("""{"aaa":"D","bbb":"ae102612-e023-11ea-a115-6b5393cb81fd"}""")
        expect(false) { schema.validate(json4) }
        val validateResult4 = schema.validateBasic(json4)
        expect(false) { validateResult4.valid }
        val errors4 = validateResult4.errors ?: fail()
        expect(4) { errors4.size }
        errors4[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-if-then-else#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors4[1].let {
            expect("#/else") { it.keywordLocation }
            expect("http://pwall.net/test-if-then-else#/else") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors4[2].let {
            expect("#/else/properties/bbb") { it.keywordLocation }
            expect("http://pwall.net/test-if-then-else#/else/properties/bbb") { it.absoluteKeywordLocation }
            expect("#/bbb") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors4[3].let {
            expect("#/else/properties/bbb/format") { it.keywordLocation }
            expect("http://pwall.net/test-if-then-else#/else/properties/bbb/format") { it.absoluteKeywordLocation }
            expect("#/bbb") { it.instanceLocation }
            expect("Value fails format check \"date-time\", was \"ae102612-e023-11ea-a115-6b5393cb81fd\"") { it.error }
        }
    }

    @Test fun `should validate string of JSON`() {
        expect(true) { trueSchema.validate("{}") }
        expect(true) { trueSchema.validateBasic("{}").valid }
        expect(true) { trueSchema.validateDetailed("{}").valid }
    }

    @Test fun `should return true from true schema`() {
        expect(true) { trueSchema.validate(emptyObject) }
        expect(true) { trueSchema.validateBasic(emptyObject).valid }
        expect(true) { trueSchema.validateDetailed(emptyObject).valid }
    }

    @Test fun `should return false from false schema`() {
        expect(false) { falseSchema.validate(emptyObject) }
        expect(false) { falseSchema.validateBasic(emptyObject).valid }
        expect(false) { falseSchema.validateDetailed(emptyObject).valid }
    }

    @Test fun `should return false from not true schema`() {
        JSONSchema.Not(null, JSONPointer.root, trueSchema).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
    }

    @Test fun `should return true from not false schema`() {
        JSONSchema.Not(null, JSONPointer.root, falseSchema).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
    }

    @Test fun `should give correct result from allOf`() {
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, trueSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
    }

    @Test fun `should give correct result from anyOf`() {
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, trueSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, falseSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
    }

    @Test fun `should give correct result from oneOf`() {
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, trueSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, falseSchema)).let {
            expect(true) { it.validate(emptyObject) }
            expect(true) { it.validateBasic(emptyObject).valid }
            expect(true) { it.validateDetailed(emptyObject).valid }
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema)).let {
            expect(false) { it.validate(emptyObject) }
            expect(false) { it.validateBasic(emptyObject).valid }
            expect(false) { it.validateDetailed(emptyObject).valid }
        }
    }

    @Test fun `should give correct absolute location`() {
        JSONSchema.True(URI("http://pwall.net/schema/true1"), JSONPointer("/abc/0")).let {
            expect("http://pwall.net/schema/true1#/abc/0") { it.absoluteLocation }
        }
    }

    @Test fun `should validate null`() {
        val filename = "src/test/resources/test-type-null.schema.json"
        JSONSchema.parseFile(filename).let {
            expect(true) { it.validate("null") }
            expect(true) { it.validateBasic("null").valid }
            expect(true) { it.validateDetailed("null").valid }
            expect(false) { it.validate("{}") }
            expect(false) { it.validateBasic("{}").valid }
            expect(false) { it.validateDetailed("{}").valid }
            expect(false) { it.validate("123") }
            expect(false) { it.validateBasic("123").valid }
            expect(false) { it.validateDetailed("123").valid }
            expect(false) { it.validate("[]") }
            expect(false) { it.validateBasic("[]").valid }
            expect(false) { it.validateDetailed("[]").valid }
        }
    }

    @Test fun `should perform equals and hashCode correctly`() {
        val filename = "src/test/resources/test-additional.schema.json"
        val schema1 = JSONSchema.parseFile(filename)
        val schema2 = JSONSchema.parseFile(filename)
        assertEquals(schema1, schema2)
        assertEquals(schema1.hashCode(), schema2.hashCode())
    }

    @Test fun `check assumptions about URIs`() {
        val file = File("src/test/resources/example.json")
        val uri1 = URI("file://${file.absolutePath}")
        expect("file://${file.absolutePath}") { uri1.toString() }
        expect("file") { uri1.scheme }
        expect(file.absolutePath) { uri1.path }
        expect(null) { uri1.fragment }
        expect(null) { uri1.host }
        expect(-1) { uri1.port }
        expect(null) { uri1.userInfo }
        expect(null) { uri1.query }
        expect("//${file.absolutePath}") { uri1.schemeSpecificPart }
        val uri2 = uri1.resolve("http://pwall.net/schema/true1")
        expect("http://pwall.net/schema/true1") { uri2.toString() }
        expect("http") { uri2.scheme }
        expect("/schema/true1") { uri2.path }
        expect(null) { uri2.fragment }
        expect("pwall.net") { uri2.host }
        expect(-1) { uri2.port }
        expect(null) { uri2.userInfo }
        expect(null) { uri2.query }
        expect("//pwall.net/schema/true1") { uri2.schemeSpecificPart }
        val uri3 = uri2.resolve("true2")
        expect("http://pwall.net/schema/true2") { uri3.toString() }
        expect("http") { uri3.scheme }
        expect("/schema/true2") { uri3.path }
        expect(null) { uri3.fragment }
        expect("pwall.net") { uri3.host }
        expect(-1) { uri3.port }
        expect(null) { uri3.userInfo }
        expect(null) { uri3.query }
        val uri3a = uri2.resolve("true2?a=1&b=2")
        expect("http://pwall.net/schema/true2?a=1&b=2") { uri3a.toString() }
        expect("http") { uri3a.scheme }
        expect("/schema/true2") { uri3a.path }
        expect(null) { uri3a.fragment }
        expect("pwall.net") { uri3a.host }
        expect(-1) { uri3a.port }
        expect(null) { uri3a.userInfo }
        expect("a=1&b=2") { uri3a.query }
        expect("//pwall.net/schema/true2?a=1&b=2") { uri3a.schemeSpecificPart }
        val uri4 = uri3.resolve("http://pwall.net/schema/true3#")
        expect("http://pwall.net/schema/true3#") { uri4.toString() }
        expect("http") { uri4.scheme }
        expect("/schema/true3") { uri4.path }
        expect("") { uri4.fragment }
        expect("pwall.net") { uri4.host }
        expect("//pwall.net/schema/true3") { uri4.schemeSpecificPart }
        val uri5 = uri3.resolve("#frag1")
        expect("http://pwall.net/schema/true2#frag1") { uri5.toString() }
        expect("http") { uri5.scheme }
        expect("/schema/true2") { uri5.path }
        expect("frag1") { uri5.fragment }
        expect("pwall.net") { uri5.host }
        expect("//pwall.net/schema/true2") { uri5.schemeSpecificPart }
        val uri6 = URI("http://pwall.net/schema/true2?a=1&b=2#frag1")
        expect("http://pwall.net/schema/true2?a=1&b=2#frag1") { uri6.toString() }
        expect("http") { uri6.scheme }
        expect("/schema/true2") { uri6.path }
        expect("frag1") { uri6.fragment }
        expect("pwall.net") { uri6.host }
        expect("a=1&b=2") { uri6.query }
        expect("//pwall.net/schema/true2?a=1&b=2") { uri6.schemeSpecificPart }
        val uri9 = URI("classpath:/example.json")
        expect("classpath:/example.json") { uri9.toString() }
        expect("classpath") { uri9.scheme }
        expect("/example.json") { uri9.path }
        expect(false) { uri9.isOpaque }
        val uri10 = URI("scm:git:git://github.com/pwall567/json-kotlin-schema.git")
        expect(true) { uri10.isOpaque }
        expect("scm") { uri10.scheme }
        expect("git:git://github.com/pwall567/json-kotlin-schema.git") { uri10.schemeSpecificPart }
        expect(null) { uri10.path }
        expect(null) { uri10.fragment }
        expect(null) { uri10.host }
        expect(-1) { uri10.port }
        expect(null) { uri10.userInfo }
        expect(null) { uri10.query }
        val uri10a = uri10.resolve("#frag99")
        expect("#frag99") { uri10a.toString() } // can't add fragment to opaque URI
        val uri11 = URI("test")
        expect(false) { uri11.isOpaque }
        expect(null) { uri11.scheme }
        expect("test") { uri11.path }
        expect(null) { uri11.fragment }
        expect(null) { uri11.host }
        expect(-1) { uri11.port }
        expect(null) { uri11.userInfo }
        expect(null) { uri11.query }
        val uri12 = uri1.resolve("http://pwall.net:8080/schema/true1")
        expect("http://pwall.net:8080/schema/true1") { uri12.toString() }
        expect("http") { uri12.scheme }
        expect("/schema/true1") { uri12.path }
        expect(null) { uri12.fragment }
        expect("pwall.net") { uri12.host }
        expect(8080) { uri12.port }
        expect(null) { uri12.userInfo }
        expect("pwall.net:8080") { uri12.authority }
        expect(null) { uri12.query }
        expect("//pwall.net:8080/schema/true1") { uri12.schemeSpecificPart }
        val uri13 = URI("http", "pwall.net:8080", "/schema/test99", "a=0", "frag88")
        expect("http://pwall.net:8080/schema/test99?a=0#frag88") { uri13.toString() }
        val uri14 = URI("http://pwall.net/dir1/dir2/../dir3/file").normalize()
        expect("/dir1/dir3/file") { uri14.path }
        val uri15 = URI("http://pwall.net")
        expect("") { uri15.path }
    }

    companion object {

        val emptyObject = JSONObject()
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val falseSchema = JSONSchema.False(null, JSONPointer.root)

    }

}
