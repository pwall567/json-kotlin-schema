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
import kotlin.test.expect
import kotlin.test.fail

import java.net.URI

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
            expect("http://pwall.net/test-nonstandard-format#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-nonstandard-format#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/format/non-standard") { it.keywordLocation }
            expect("https://example.com/config.json#/x-f/0") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("String fails length check: minLength 1, was 0") { it.error }
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
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        val json2 = JSON.parse("""{"aaa":""}""")
        expect(false) { schema.validate(json2) }
        val validateResult2 = schema.validateBasic(json2)
        expect(false) { validateResult2.valid }
        val errors2 = validateResult2.errors ?: fail()
        expect(3) { errors2.size }
        errors2[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-nonstandard-format#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors2[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-nonstandard-format#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors2[2].let {
            expect("#/properties/aaa/format/non-standard") { it.keywordLocation }
            expect("https://example.com/config.json#/x-f/0") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("String fails length check: minLength 1, was 0") { it.error }
        }
        val json3 = JSON.parse("""{"aaa":"XXXX"}""")
        expect(false) { schema.validate(json3) }
        val validateResult3 = schema.validateBasic(json3)
        expect(false) { validateResult3.valid }
        val errors3 = validateResult3.errors ?: fail()
        expect(3) { errors3.size }
        errors3[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-nonstandard-format#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors3[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-nonstandard-format#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors3[2].let {
            expect("#/properties/aaa/format/non-standard") { it.keywordLocation }
            expect("https://example.com/config.json#/x-f/1") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("String fails length check: maxLength 3, was 4") { it.error }
        }
    }

}
