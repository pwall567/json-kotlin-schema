/*
 * @(#) JSONSchemaCustomValidatorTest.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020 Peter Wall
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

import io.kjson.JSON
import io.kjson.JSONString

import io.kstuff.test.shouldBe

import net.pwall.json.schema.parser.Parser
import net.pwall.json.schema.validation.StringValidator

class JSONSchemaCustomValidatorTest {

    @Test fun `should make use of custom validator`() {
        val parser = Parser()
        parser.customValidationHandler = { key, uri, location, value ->
            when (key) {
                "x-test" -> {
                    if (value is JSONString && value.value == "not-empty")
                        StringValidator(uri, location, StringValidator.ValidationType.MIN_LENGTH, 1)
                    else
                        fail("Unknown type")
                }
                else -> null
            }
        }
        val filename = "src/test/resources/test-custom-validator.schema.json"
        val schema = parser.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"Q"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":""}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-custom#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-custom#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/x-test"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-custom#/properties/aaa/x-test"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "String fails length check: minLength 1, was 0"
        }
    }

}
