/*
 * @(#) JSONSchemaExamplesTest.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2024 Peter Wall
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.expect

import net.pwall.json.schema.parser.Parser

class JSONSchemaExamplesTest {

    @Test fun `should validate example`() {
        val filename = "src/test/resources/test-example-valid.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        assertTrue(parser.examplesValidationErrors.isEmpty())
    }

    @Test fun `should validate invalid example`() {
        val filename = "src/test/resources/test-example-invalid.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        expect(1) { parser.examplesValidationErrors.size }
        with(parser.examplesValidationErrors[0]) {
            assertFalse(valid)
            with(errors) {
                assertNotNull(this)
                expect(3) { size }
                with(this[0]) {
                    expect("#") { keywordLocation }
                    expect("http://pwall.net/test-example-invalid#") { absoluteKeywordLocation }
                    expect("#/example") { instanceLocation }
                    expect(JSONSchema.subSchemaErrorMessage) { error }
                }
                with(this[1]) {
                    expect("#/properties/aaa") { keywordLocation }
                    expect("http://pwall.net/test-example-invalid#/properties/aaa") { absoluteKeywordLocation }
                    expect("#/example/aaa") { instanceLocation }
                    expect(JSONSchema.subSchemaErrorMessage) { error }
                }
                with(this[2]) {
                    expect("#/properties/aaa/minLength") { keywordLocation }
                    expect("http://pwall.net/test-example-invalid#/properties/aaa/minLength") {
                        absoluteKeywordLocation
                    }
                    expect("#/example/aaa") { instanceLocation }
                    expect("String fails length check: minLength 3, was 2") { error }
                }
            }
        }
    }

    @Test fun `should validate invalid example 2`() {
        val filename = "src/test/resources/test-example-invalid-2.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        expect(1) { parser.examplesValidationErrors.size }
        with(parser.examplesValidationErrors[0]) {
            assertFalse(valid)
            with(errors) {
                assertNotNull(this)
                expect(2) { size }
                with(this[0]) {
                    expect("#") { keywordLocation }
                    expect("http://pwall.net/test-example-invalid-2#") { absoluteKeywordLocation }
                    expect("#/example") { instanceLocation }
                    expect(JSONSchema.subSchemaErrorMessage) { error }
                }
                with(this[1]) {
                    expect("#/minimum") { keywordLocation }
                    expect("http://pwall.net/test-example-invalid-2#/minimum") { absoluteKeywordLocation }
                    expect("#/example") { instanceLocation }
                    expect("Number fails check: minimum 20, was 19") { error }
                }
            }
        }
    }

    @Test fun `should validate examples`() {
        val filename = "src/test/resources/test-examples-valid.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        assertTrue(parser.examplesValidationErrors.isEmpty())
    }

    @Test fun `should validate invalid examples`() {
        val filename = "src/test/resources/test-examples-invalid.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        expect(3) { parser.examplesValidationErrors.size }
        with(parser.examplesValidationErrors[0]) {
            assertFalse(valid)
            with(errors) {
                assertNotNull(this)
                expect(2) { size }
                with(this[0]) {
                    expect("#/properties/bbb") { keywordLocation }
                    expect("http://pwall.net/test-examples-invalid#/properties/bbb") { absoluteKeywordLocation }
                    expect("#/properties/bbb/examples/0") { instanceLocation }
                    expect(JSONSchema.subSchemaErrorMessage) { error }
                }
                with(this[1]) {
                    expect("#/properties/bbb/minimum") { keywordLocation }
                    expect("http://pwall.net/test-examples-invalid#/properties/bbb/minimum") { absoluteKeywordLocation }
                    expect("#/properties/bbb/examples/0") { instanceLocation }
                    expect("Number fails check: minimum 0, was -1") { error }
                }
            }
        }
        with(parser.examplesValidationErrors[1]) {
            with(errors) {
                assertNotNull(this)
                expect(3) { size }
                with(this[0]) {
                    expect("#") { keywordLocation }
                    expect("http://pwall.net/test-examples-invalid#") { absoluteKeywordLocation }
                    expect("#/examples/2") { instanceLocation }
                    expect(JSONSchema.subSchemaErrorMessage) { error }
                }
                with(this[1]) {
                    expect("#/properties/aaa") { keywordLocation }
                    expect("http://pwall.net/test-examples-invalid#/properties/aaa") { absoluteKeywordLocation }
                    expect("#/examples/2/aaa") { instanceLocation }
                    expect(JSONSchema.subSchemaErrorMessage) { error }
                }
                with(this[2]) {
                    expect("#/properties/aaa/minLength") { keywordLocation }
                    expect("http://pwall.net/test-examples-invalid#/properties/aaa/minLength") {
                        absoluteKeywordLocation
                    }
                    expect("#/examples/2/aaa") { instanceLocation }
                    expect("String fails length check: minLength 3, was 2") { error }
                }
            }
        }
        with(parser.examplesValidationErrors[2]) {
            with(errors) {
                assertNotNull(this)
                expect(4) { size }
                with(this[0]) {
                    expect("#") { keywordLocation }
                    expect("http://pwall.net/test-examples-invalid#") { absoluteKeywordLocation }
                    expect("#/examples/3") { instanceLocation }
                    expect(JSONSchema.subSchemaErrorMessage) { error }
                }
                with(this[1]) {
                    expect("#/properties/bbb") { keywordLocation }
                    expect("http://pwall.net/test-examples-invalid#/properties/bbb") { absoluteKeywordLocation }
                    expect("#/examples/3/bbb") { instanceLocation }
                    expect(JSONSchema.subSchemaErrorMessage) { error }
                }
                with(this[2]) {
                    expect("#/properties/bbb/minimum") { keywordLocation }
                    expect("http://pwall.net/test-examples-invalid#/properties/bbb/minimum") {
                        absoluteKeywordLocation
                    }
                    expect("#/examples/3/bbb") { instanceLocation }
                    expect("Number fails check: minimum 0, was -2") { error }
                }
                with(this[3]) {
                    expect("#/required/0") { keywordLocation }
                    expect("http://pwall.net/test-examples-invalid#/required") { absoluteKeywordLocation }
                    expect("#/examples/3") { instanceLocation }
                    expect("Required property \"aaa\" not found") { error }
                }
            }
        }
    }

    @Test fun `should validate default`() {
        val filename = "src/test/resources/test-default-valid.schema.json"
        Parser().apply {
            options.validateDefault = true
            parseFile(filename)
            assertTrue(defaultValidationErrors.isEmpty())
        }
    }

    @Test fun `should validate invalid default`() {
        val filename = "src/test/resources/test-default-invalid.schema.json"
        val parser = Parser()
        parser.options.validateDefault = true
        parser.parseFile(filename)
        expect(1) { parser.defaultValidationErrors.size }
        with(parser.defaultValidationErrors[0]) {
            assertFalse(valid)
            with(errors) {
                assertNotNull(this)
                expect(2) { size }
                with(this[0]) {
                    expect("#/properties/aaa") { keywordLocation }
                    expect("http://pwall.net/test-default-invalid#/properties/aaa") { absoluteKeywordLocation }
                    expect("#/properties/aaa/default") { instanceLocation }
                    expect(JSONSchema.subSchemaErrorMessage) { error }
                }
                with(this[1]) {
                    expect("#/properties/aaa/minimum") { keywordLocation }
                    expect("http://pwall.net/test-default-invalid#/properties/aaa/minimum") { absoluteKeywordLocation }
                    expect("#/properties/aaa/default") { instanceLocation }
                    expect("Number fails check: minimum 20, was 15") { error }
                }
            }
        }
    }

}
