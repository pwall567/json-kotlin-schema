/*
 * @(#) JSONSchemaNonstandardFormatTest.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2021, 2024 Peter Wall
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

import java.net.URI

import io.kstuff.test.shouldBe

import io.kjson.JSON
import io.kjson.pointer.JSONPointer

import net.pwall.json.schema.parser.Parser
import net.pwall.json.schema.validation.FormatValidator
import net.pwall.json.schema.validation.StringValidator

class JSONSchemaNonstandardFormatTest {

    @Test fun `should make use of nonstandard format replacement validation`() {
        val parser = Parser()
        val configURI = URI("https://example.com/config.json")
        parser.nonstandardFormatHandler = { keyword ->
            if (keyword == "non-standard")
                FormatValidator.DelegatingFormatChecker(keyword,
                    StringValidator(configURI, JSONPointer("/x-f/0"), StringValidator.ValidationType.MIN_LENGTH, 1))
            else
                null
        }
        val filename = "src/test/resources/test-nonstandard-format.schema.json"
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
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-nonstandard-format#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-nonstandard-format#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/format/non-standard"
            it.absoluteKeywordLocation shouldBe "https://example.com/config.json#/x-f/0"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "String fails length check: minLength 1, was 0"
        }
    }

    @Test fun `should make use of nonstandard format replacement validation - multiple`() {
        val parser = Parser()
        val configURI = URI("https://example.com/config.json")
        parser.nonstandardFormatHandler = { keyword ->
            when (keyword) {
                "non-standard" -> FormatValidator.DelegatingFormatChecker(keyword,
                    StringValidator(configURI, JSONPointer("/x-f/0"), StringValidator.ValidationType.MIN_LENGTH, 1),
                    StringValidator(configURI, JSONPointer("/x-f/1"), StringValidator.ValidationType.MAX_LENGTH, 3))
                else -> null
            }
        }
        val filename = "src/test/resources/test-nonstandard-format.schema.json"
        val schema = parser.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"Q"}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":""}""")
        schema.validate(json2) shouldBe false
        val validateResult2 = schema.validateBasic(json2)
        validateResult2.valid shouldBe false
        val errors2 = validateResult2.errors ?: fail()
        errors2.size shouldBe 3
        errors2[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-nonstandard-format#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors2[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-nonstandard-format#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors2[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/format/non-standard"
            it.absoluteKeywordLocation shouldBe "https://example.com/config.json#/x-f/0"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "String fails length check: minLength 1, was 0"
        }
        val json3 = JSON.parse("""{"aaa":"XXXX"}""")
        schema.validate(json3) shouldBe false
        val validateResult3 = schema.validateBasic(json3)
        validateResult3.valid shouldBe false
        val errors3 = validateResult3.errors ?: fail()
        errors3.size shouldBe 3
        errors3[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-nonstandard-format#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors3[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-nonstandard-format#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors3[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/format/non-standard"
            it.absoluteKeywordLocation shouldBe "https://example.com/config.json#/x-f/1"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "String fails length check: maxLength 3, was 4"
        }
    }

}
