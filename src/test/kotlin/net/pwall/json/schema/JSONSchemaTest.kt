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
import kotlin.test.fail

import java.io.File
import java.net.URI

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeNonNull

import io.kjson.JSON
import io.kjson.JSONObject
import io.kjson.pointer.JSONPointer

class JSONSchemaTest {

    @Test fun `should validate example schema`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example.json").readText())
        schema.validate(json) shouldBe true
        schema.validateBasic(json).valid shouldBe true
        schema.validateDetailed(json).valid shouldBe true
    }

    @Test fun `should validate example schema from File`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(File(filename))
        val jsonString = File("src/test/resources/example.json").readText()
        schema.validate(jsonString) shouldBe true
        schema.validateBasic(jsonString).valid shouldBe true
        schema.validateDetailed(jsonString).valid shouldBe true
    }

    @Test fun `should validate example schema from string`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val jsonString = File("src/test/resources/example.json").readText()
        schema.validate(jsonString) shouldBe true
        schema.validateBasic(jsonString).valid shouldBe true
        schema.validateDetailed(jsonString).valid shouldBe true
    }

    @Test fun `should validate example schema in YAML form`() {
        val filename = "src/test/resources/example.schema.yaml"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example.json").readText())
        schema.validate(json) shouldBe true
        schema.validateBasic(json).valid shouldBe true
        schema.validateDetailed(json).valid shouldBe true
    }

    @Test fun `should validate example schema with missing property`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example-error1.json").readText())
        schema.validate(json) shouldBe false
        val validateResult = schema.validateBasic(json)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 2
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/required/1"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#/required"
            it.instanceLocation shouldBe "#"
            it.error shouldBe "Required property \"name\" not found"
        }
        schema.validateDetailed(json).valid shouldBe false
    }

    @Test fun `should validate example schema with wrong property type`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example-error2.json").readText())
        schema.validate(json) shouldBe false
        val validateResult = schema.validateBasic(json)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/price"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#/properties/price"
            it.instanceLocation shouldBe "#/price"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/price/type"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#/properties/price/type"
            it.instanceLocation shouldBe "#/price"
            it.error shouldBe "Incorrect type, expected number"
        }
        schema.validateDetailed(json).valid shouldBe false
    }

    @Test fun `should validate example schema with value out of range`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example-error3.json").readText())
        schema.validate(json) shouldBe false
        val validateResult = schema.validateBasic(json)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/price"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#/properties/price"
            it.instanceLocation shouldBe "#/price"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/price/minimum"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#/properties/price/minimum"
            it.instanceLocation shouldBe "#/price"
            it.error shouldBe "Number fails check: minimum 0, was -1"
        }
        schema.validateDetailed(json).valid shouldBe false
    }

    @Test fun `should validate example schema with array item of wrong type`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/example-error4.json").readText())
        schema.validate(json) shouldBe false
        val validateResult = schema.validateBasic(json)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 4
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/tags"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#/properties/tags"
            it.instanceLocation shouldBe "#/tags"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/tags/items"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#/properties/tags/items"
            it.instanceLocation shouldBe "#/tags/2"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[3].let {
            it.keywordLocation shouldBe "#/properties/tags/items/type"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test#/properties/tags/items/type"
            it.instanceLocation shouldBe "#/tags/2"
            it.error shouldBe "Incorrect type, expected string"
        }
        schema.validateDetailed(json).valid shouldBe false
    }

    @Test fun `should validate example schema with reference`() {
        val filename = "src/test/resources/test-ref.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json = JSON.parse(File("src/test/resources/test-ref.json").readText())
        schema.validate(json) shouldBe false
        val validateResult = schema.validateBasic(json)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 4
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-ref#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/bbb"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-ref#/properties/bbb"
            it.instanceLocation shouldBe "#/bbb"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/bbb/\$ref"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-ref#/\$defs/amount"
            it.instanceLocation shouldBe "#/bbb"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[3].let {
            it.keywordLocation shouldBe "#/properties/bbb/\$ref/type"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-ref#/\$defs/amount/type"
            it.instanceLocation shouldBe "#/bbb"
            it.error shouldBe "Incorrect type, expected number"
        }
        schema.validateDetailed(json).valid shouldBe false
    }

    @Test fun `should validate additional properties`() {
        val filename = "src/test/resources/test-additional.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA","bbb":1,"ccc":99}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"AAA","bbb":1,"ccc":"99"}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 4
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-additional#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/additionalProperties"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-additional#/additionalProperties"
            it.instanceLocation shouldBe "#/ccc"
            it.error shouldBe "Additional property 'ccc' found but was invalid"
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/additionalProperties"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-additional#/additionalProperties"
            it.instanceLocation shouldBe "#/ccc"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[3].let {
            it.keywordLocation shouldBe "#/additionalProperties/type"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-additional#/additionalProperties/type"
            it.instanceLocation shouldBe "#/ccc"
            it.error shouldBe "Incorrect type, expected integer"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate additional properties not allowed`() {
        val filename = "src/test/resources/test-additional-false.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"AAA","bbb":1}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-additional-false#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/additionalProperties"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-additional-false#/additionalProperties"
            it.instanceLocation shouldBe "#/bbb"
            it.error shouldBe "Additional property 'bbb' found but was invalid"
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/additionalProperties"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-additional-false#/additionalProperties"
            it.instanceLocation shouldBe "#/bbb"
            it.error shouldBe "Constant schema \"false\""
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate enum`() {
        val filename = "src/test/resources/test-enum.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-enum#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-enum#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/enum"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-enum#/properties/aaa/enum"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "Not in enumerated values: \"ZZZZZZZZ ... ZZZZZZZZ\""
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate const`() {
        val filename = "src/test/resources/test-const.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-const#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-const#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/const"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-const#/properties/aaa/const"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "Does not match constant: \"ZZZZZZZZ ... ZZZZZZZZ\""
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate string length`() {
        val filename = "src/test/resources/test-string-length.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-string-length#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-string-length#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/maxLength"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-string-length#/properties/aaa/maxLength"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "String fails length check: maxLength 10, was 66"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate string pattern`() {
        val filename = "src/test/resources/test-string-pattern.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"A001"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"9999"}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-string-pattern#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-string-pattern#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/pattern"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-string-pattern#/properties/aaa/pattern"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "String doesn't match pattern ^[A-Z][0-9]{3}\$ - \"9999\""
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate string format`() {
        val filename = "src/test/resources/test-string-format.schema.json"
        val schema = JSONSchema.parseFile(filename)
        with(JSON.parse("""{"dateTimeTest":"2020-07-22T19:29:33.456+10:00"}""")) {
            schema.validate(this) shouldBe true
            schema.validateBasic(this).valid shouldBe true
            schema.validateDetailed(this).valid shouldBe true
        }
        with(JSON.parse("""{"dateTimeTest":"wrong"}""")) {
            schema.validate(this) shouldBe false
            with(schema.validateBasic(this)) {
                valid shouldBe false
                with(errors) {
                    shouldBeNonNull()
                    size shouldBe 3
                    with(this[0]) {
                        keywordLocation shouldBe "#"
                        absoluteKeywordLocation shouldBe "http://pwall.net/test-string-format#"
                        instanceLocation shouldBe "#"
                        error shouldBe JSONSchema.subSchemaErrorMessage
                    }
                    with(this[1]) {
                        keywordLocation shouldBe "#/properties/dateTimeTest"

                        absoluteKeywordLocation shouldBe "http://pwall.net/test-string-format#/properties/dateTimeTest"
                        instanceLocation shouldBe "#/dateTimeTest"
                        error shouldBe JSONSchema.subSchemaErrorMessage
                    }
                    with(this[2]) {
                        keywordLocation shouldBe "#/properties/dateTimeTest/format"
                        absoluteKeywordLocation shouldBe
                                "http://pwall.net/test-string-format#/properties/dateTimeTest/format"
                        instanceLocation shouldBe "#/dateTimeTest"
                        error shouldBe "Value fails format check \"date-time\", was \"wrong\""
                    }
                }
            }
            with(schema.validateDetailed(this)) {
                valid shouldBe false
            }
        }
        with(JSON.parse("""{"dateTest":"2020-07-22"}""")) {
            schema.validate(this) shouldBe true
            with(schema.validateBasic(this)) {
                valid shouldBe true
            }
            with(schema.validateDetailed(this)) {
                valid shouldBe true
            }
        }
        with(JSON.parse("""{"dateTest":"wrong"}""")) {
            schema.validate(this) shouldBe false
            with(schema.validateBasic(this)) {
                valid shouldBe false
                with(errors) {
                    shouldBeNonNull()
                    size shouldBe 3
                    with(this[0]) {
                        keywordLocation shouldBe "#"
                        absoluteKeywordLocation shouldBe "http://pwall.net/test-string-format#"
                        instanceLocation shouldBe "#"
                        error shouldBe JSONSchema.subSchemaErrorMessage
                    }
                    with(this[1]) {
                        keywordLocation shouldBe "#/properties/dateTest"
                        absoluteKeywordLocation shouldBe "http://pwall.net/test-string-format#/properties/dateTest"
                        instanceLocation shouldBe "#/dateTest"
                        error shouldBe JSONSchema.subSchemaErrorMessage
                    }
                    with(this[2]) {
                        keywordLocation shouldBe "#/properties/dateTest/format"
                        absoluteKeywordLocation shouldBe
                                "http://pwall.net/test-string-format#/properties/dateTest/format"
                        instanceLocation shouldBe "#/dateTest"
                        error shouldBe "Value fails format check \"date\", was \"wrong\""
                    }
                }
            }
            with(schema.validateDetailed(this)) {
                valid shouldBe false
            }
        }
        with(JSON.parse("""{"durationTest":"P1M"}""")) {
            schema.validate(this) shouldBe true
            with(schema.validateBasic(this)) {
                valid shouldBe true
            }
            with(schema.validateDetailed(this)) {
                valid shouldBe true
            }
        }
        with(JSON.parse("""{"durationTest":"wrong"}""")) {
            schema.validate(this) shouldBe false
            with(schema.validateBasic(this)) {
                valid shouldBe false
                with(errors) {
                    shouldBeNonNull()
                    size shouldBe 3
                    with(this[0]) {
                        keywordLocation shouldBe "#"
                        absoluteKeywordLocation shouldBe "http://pwall.net/test-string-format#"
                        instanceLocation shouldBe "#"
                        error shouldBe JSONSchema.subSchemaErrorMessage
                    }
                    with(this[1]) {
                        keywordLocation shouldBe "#/properties/durationTest"
                        absoluteKeywordLocation shouldBe "http://pwall.net/test-string-format#/properties/durationTest"
                        instanceLocation shouldBe "#/durationTest"
                        error shouldBe JSONSchema.subSchemaErrorMessage
                    }
                    with(this[2]) {
                        keywordLocation shouldBe "#/properties/durationTest/format"
                        absoluteKeywordLocation shouldBe
                                "http://pwall.net/test-string-format#/properties/durationTest/format"
                        instanceLocation shouldBe "#/durationTest"
                        error shouldBe "Value fails format check \"duration\", was \"wrong\""
                    }
                }
            }
            with(schema.validateDetailed(this)) {
                valid shouldBe false
            }
        }
        with(JSON.parse("""{"uriTemplateTest":"http://example.com/customer/{customerId}"}""")) {
            schema.validate(this) shouldBe true
            with(schema.validateBasic(this)) {
                valid shouldBe true
            }
            with(schema.validateDetailed(this)) {
                valid shouldBe true
            }
        }
        with(JSON.parse("""{"uriTemplateTest":"incorrect template"}""")) {
            schema.validate(this) shouldBe false
            with(schema.validateBasic(this)) {
                valid shouldBe false
                with(errors) {
                    shouldBeNonNull()
                    size shouldBe 3
                    with(this[0]) {
                        keywordLocation shouldBe "#"
                        absoluteKeywordLocation shouldBe "http://pwall.net/test-string-format#"
                        instanceLocation shouldBe "#"
                        error shouldBe JSONSchema.subSchemaErrorMessage
                    }
                    with(this[1]) {
                        keywordLocation shouldBe "#/properties/uriTemplateTest"
                        absoluteKeywordLocation shouldBe
                                "http://pwall.net/test-string-format#/properties/uriTemplateTest"
                        instanceLocation shouldBe "#/uriTemplateTest"
                        error shouldBe JSONSchema.subSchemaErrorMessage
                    }
                    with(this[2]) {
                        keywordLocation shouldBe "#/properties/uriTemplateTest/format"
                        absoluteKeywordLocation shouldBe
                                "http://pwall.net/test-string-format#/properties/uriTemplateTest/format"
                        instanceLocation shouldBe "#/uriTemplateTest"
                        error shouldBe "Value fails format check \"uri-template\", was \"incorrect template\""
                    }
                }
            }
            with(schema.validateDetailed(this)) {
                valid shouldBe false
            }
        }
    }

    @Test fun `should validate schema with anyOf`() {
        val filename = "src/test/resources/test-anyof.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"2020-07-22"}""")
        schema.validate(json2) shouldBe true
        schema.validateBasic(json2).valid shouldBe true
        schema.validateDetailed(json2).valid shouldBe true
        val json3 = JSON.parse("""{"aaa":"wrong"}""")
        schema.validate(json3) shouldBe false
        val validateResult = schema.validateBasic(json3)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 7
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-anyof#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-anyof#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/anyOf"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-anyof#/properties/aaa/anyOf"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "Combination schema \"anyOf\" fails - 0 of 2 valid"
        }
        errors[3].let {
            it.keywordLocation shouldBe "#/properties/aaa/anyOf/0"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-anyof#/properties/aaa/anyOf/0"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[4].let {
            it.keywordLocation shouldBe "#/properties/aaa/anyOf/0/const"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-anyof#/properties/aaa/anyOf/0/const"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "Does not match constant: \"wrong\""
        }
        errors[5].let {
            it.keywordLocation shouldBe "#/properties/aaa/anyOf/1"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-anyof#/properties/aaa/anyOf/1"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[6].let {
            it.keywordLocation shouldBe "#/properties/aaa/anyOf/1/format"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-anyof#/properties/aaa/anyOf/1/format"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "Value fails format check \"date\", was \"wrong\""
        }
        schema.validateDetailed(json3).valid shouldBe false
    }

    @Test fun `should validate schema with not`() {
        val filename = "src/test/resources/test-not.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"BBB"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"AAA"}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-not#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-not#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/not"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-not#/properties/aaa/not"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "Schema \"not\" - target was valid"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate schema with if-then-else`() {
        val filename = "src/test/resources/test-if-then-else.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"U","bbb":"ae102612-e023-11ea-a115-6b5393cb81fd"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"U","bbb":"2020-08-17T19:30:00+10:00"}""")
        schema.validate(json2) shouldBe false
        val validateResult2 = schema.validateBasic(json2)
        validateResult2.valid shouldBe false
        val errors2 = validateResult2.errors ?: fail()
        errors2.size shouldBe 4
        errors2[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-if-then-else#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors2[1].let {
            it.keywordLocation shouldBe "#/then"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-if-then-else#/then"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors2[2].let {
            it.keywordLocation shouldBe "#/then/properties/bbb"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-if-then-else#/then/properties/bbb"
            it.instanceLocation shouldBe "#/bbb"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors2[3].let {
            it.keywordLocation shouldBe "#/then/properties/bbb/format"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-if-then-else#/then/properties/bbb/format"
            it.instanceLocation shouldBe "#/bbb"
            it.error shouldBe "Value fails format check \"uuid\", was \"2020-08-17T19:30:00+10:00\""
        }
        val json3 = JSON.parse("""{"aaa":"D","bbb":"2020-08-17T19:30:00+10:00"}""")
        schema.validate(json3) shouldBe true
        schema.validateBasic(json3).valid shouldBe true
        val json4 = JSON.parse("""{"aaa":"D","bbb":"ae102612-e023-11ea-a115-6b5393cb81fd"}""")
        schema.validate(json4) shouldBe false
        val validateResult4 = schema.validateBasic(json4)
        validateResult4.valid shouldBe false
        val errors4 = validateResult4.errors ?: fail()
        errors4.size shouldBe 4
        errors4[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-if-then-else#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors4[1].let {
            it.keywordLocation shouldBe "#/else"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-if-then-else#/else"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors4[2].let {
            it.keywordLocation shouldBe "#/else/properties/bbb"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-if-then-else#/else/properties/bbb"
            it.instanceLocation shouldBe "#/bbb"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors4[3].let {
            it.keywordLocation shouldBe "#/else/properties/bbb/format"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-if-then-else#/else/properties/bbb/format"
            it.instanceLocation shouldBe "#/bbb"
            it.error shouldBe "Value fails format check \"date-time\", was \"ae102612-e023-11ea-a115-6b5393cb81fd\""
        }
    }

    @Test fun `should validate string of JSON`() {
        trueSchema.validate("{}") shouldBe true
        trueSchema.validateBasic("{}").valid shouldBe true
        trueSchema.validateDetailed("{}").valid shouldBe true
    }

    @Test fun `should return true from true schema`() {
        trueSchema.validate(emptyObject) shouldBe true
        trueSchema.validateBasic(emptyObject).valid shouldBe true
        trueSchema.validateDetailed(emptyObject).valid shouldBe true
    }

    @Test fun `should return false from false schema`() {
        falseSchema.validate(emptyObject) shouldBe false
        falseSchema.validateBasic(emptyObject).valid shouldBe false
        falseSchema.validateDetailed(emptyObject).valid shouldBe false
    }

    @Test fun `should return false from not true schema`() {
        JSONSchema.Not(null, JSONPointer.root, trueSchema).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
    }

    @Test fun `should return true from not false schema`() {
        JSONSchema.Not(null, JSONPointer.root, falseSchema).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
    }

    @Test fun `should give correct result from allOf`() {
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, trueSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
    }

    @Test fun `should give correct result from anyOf`() {
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, trueSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
    }

    @Test fun `should give correct result from oneOf`() {
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, trueSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe true
            it.validateBasic(emptyObject).valid shouldBe true
            it.validateDetailed(emptyObject).valid shouldBe true
        }
        JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema)).let {
            it.validate(emptyObject) shouldBe false
            it.validateBasic(emptyObject).valid shouldBe false
            it.validateDetailed(emptyObject).valid shouldBe false
        }
    }

    @Test fun `should give correct absolute location`() {
        JSONSchema.True(URI("http://pwall.net/schema/true1"), JSONPointer("/abc/0")).let {
            it.absoluteLocation shouldBe "http://pwall.net/schema/true1#/abc/0"
        }
    }

    @Test fun `should validate null`() {
        val filename = "src/test/resources/test-type-null.schema.json"
        JSONSchema.parseFile(filename).let {
            it.validate("null") shouldBe true
            it.validateBasic("null").valid shouldBe true
            it.validateDetailed("null").valid shouldBe true
            it.validate("{}") shouldBe false
            it.validateBasic("{}").valid shouldBe false
            it.validateDetailed("{}").valid shouldBe false
            it.validate("123") shouldBe false
            it.validateBasic("123").valid shouldBe false
            it.validateDetailed("123").valid shouldBe false
            it.validate("[]") shouldBe false
            it.validateBasic("[]").valid shouldBe false
            it.validateDetailed("[]").valid shouldBe false
        }
    }

    @Test fun `should perform equals and hashCode correctly`() {
        val filename = "src/test/resources/test-additional.schema.json"
        val schema1 = JSONSchema.parseFile(filename)
        val schema2 = JSONSchema.parseFile(filename)
        schema2 shouldBe schema1
        schema2.hashCode() shouldBe schema1.hashCode()
    }

    @Test fun `check assumptions about URIs`() {
        val file = File("src/test/resources/example.json")
        val uri1 = file.absoluteFile.toURI()
        if (File.separatorChar == '\\')
            uri1.toString() shouldBe "file:/${file.absolutePath.replace('\\', '/')}"
        else
            uri1.toString() shouldBe "file:${file.absolutePath}"
        uri1.scheme shouldBe "file"
        if (File.separatorChar == '\\')
            uri1.path shouldBe "/${file.absolutePath.replace('\\', '/')}"
        else
            uri1.path shouldBe file.absolutePath
        uri1.fragment shouldBe null
        uri1.host shouldBe null
        uri1.port shouldBe -1
        uri1.userInfo shouldBe null
        uri1.query shouldBe null
        if (File.separatorChar == '\\')
            uri1.schemeSpecificPart shouldBe "/${file.absolutePath.replace('\\', '/')}"
        else
            uri1.schemeSpecificPart shouldBe file.absolutePath
        val uri2 = uri1.resolve("http://pwall.net/schema/true1")
        uri2.toString() shouldBe "http://pwall.net/schema/true1"
        uri2.scheme shouldBe "http"
        uri2.path shouldBe "/schema/true1"
        uri2.fragment shouldBe null
        uri2.host shouldBe "pwall.net"
        uri2.port shouldBe -1
        uri2.userInfo shouldBe null
        uri2.query shouldBe null
        uri2.schemeSpecificPart shouldBe "//pwall.net/schema/true1"
        val uri3 = uri2.resolve("true2")
        uri3.toString() shouldBe "http://pwall.net/schema/true2"
        uri3.scheme shouldBe "http"
        uri3.path shouldBe "/schema/true2"
        uri3.fragment shouldBe null
        uri3.host shouldBe "pwall.net"
        uri3.port shouldBe -1
        uri3.userInfo shouldBe null
        uri3.query shouldBe null
        val uri3a = uri2.resolve("true2?a=1&b=2")
        uri3a.toString() shouldBe "http://pwall.net/schema/true2?a=1&b=2"
        uri3a.scheme shouldBe "http"
        uri3a.path shouldBe "/schema/true2"
        uri3a.fragment shouldBe null
        uri3a.host shouldBe "pwall.net"
        uri3a.port shouldBe -1
        uri3a.userInfo shouldBe null
        uri3a.query shouldBe "a=1&b=2"
        uri3a.schemeSpecificPart shouldBe "//pwall.net/schema/true2?a=1&b=2"
        val uri4 = uri3.resolve("http://pwall.net/schema/true3#")
        uri4.toString() shouldBe "http://pwall.net/schema/true3#"
        uri4.scheme shouldBe "http"
        uri4.path shouldBe "/schema/true3"
        uri4.fragment shouldBe ""
        uri4.host shouldBe "pwall.net"
        uri4.schemeSpecificPart shouldBe "//pwall.net/schema/true3"
        val uri5 = uri3.resolve("#frag1")
        uri5.toString() shouldBe "http://pwall.net/schema/true2#frag1"
        uri5.scheme shouldBe "http"
        uri5.path shouldBe "/schema/true2"
        uri5.fragment shouldBe "frag1"
        uri5.host shouldBe "pwall.net"
        uri5.schemeSpecificPart shouldBe "//pwall.net/schema/true2"
        val uri6 = URI("http://pwall.net/schema/true2?a=1&b=2#frag1")
        uri6.toString() shouldBe "http://pwall.net/schema/true2?a=1&b=2#frag1"
        uri6.scheme shouldBe "http"
        uri6.path shouldBe "/schema/true2"
        uri6.fragment shouldBe "frag1"
        uri6.host shouldBe "pwall.net"
        uri6.query shouldBe "a=1&b=2"
        uri6.schemeSpecificPart shouldBe "//pwall.net/schema/true2?a=1&b=2"
        val uri9 = URI("classpath:/example.json")
        uri9.toString() shouldBe "classpath:/example.json"
        uri9.scheme shouldBe "classpath"
        uri9.path shouldBe "/example.json"
        uri9.isOpaque shouldBe false
        val uri10 = URI("scm:git:git://github.com/pwall567/json-kotlin-schema.git")
        uri10.isOpaque shouldBe true
        uri10.scheme shouldBe "scm"
        uri10.schemeSpecificPart shouldBe "git:git://github.com/pwall567/json-kotlin-schema.git"
        uri10.path shouldBe null
        uri10.fragment shouldBe null
        uri10.host shouldBe null
        uri10.port shouldBe -1
        uri10.userInfo shouldBe null
        uri10.query shouldBe null
        val uri10a = uri10.resolve("#frag99")
        uri10a.toString() shouldBe "#frag99" // can't add fragment to opaque URI
        val uri11 = URI("test")
        uri11.isOpaque shouldBe false
        uri11.scheme shouldBe null
        uri11.path shouldBe "test"
        uri11.fragment shouldBe null
        uri11.host shouldBe null
        uri11.port shouldBe -1
        uri11.userInfo shouldBe null
        uri11.query shouldBe null
        val uri12 = uri1.resolve("http://pwall.net:8080/schema/true1")
        uri12.toString() shouldBe "http://pwall.net:8080/schema/true1"
        uri12.scheme shouldBe "http"
        uri12.path shouldBe "/schema/true1"
        uri12.fragment shouldBe null
        uri12.host shouldBe "pwall.net"
        uri12.port shouldBe 8080
        uri12.userInfo shouldBe null
        uri12.authority shouldBe "pwall.net:8080"
        uri12.query shouldBe null
        uri12.schemeSpecificPart shouldBe "//pwall.net:8080/schema/true1"
        val uri13 = URI("http", "pwall.net:8080", "/schema/test99", "a=0", "frag88")
        uri13.toString() shouldBe "http://pwall.net:8080/schema/test99?a=0#frag88"
        val uri14 = URI("http://pwall.net/dir1/dir2/../dir3/file").normalize()
        uri14.path shouldBe "/dir1/dir3/file"
        val uri15 = URI("http://pwall.net")
        uri15.path shouldBe ""
    }

    companion object {

        val emptyObject = JSONObject.EMPTY
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val falseSchema = JSONSchema.False(null, JSONPointer.root)

    }

}
