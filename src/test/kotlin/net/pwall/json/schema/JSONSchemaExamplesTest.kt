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

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeNonNull

import net.pwall.json.schema.parser.Parser

class JSONSchemaExamplesTest {

    @Test fun `should validate example`() {
        val filename = "src/test/resources/test-example-valid.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        parser.examplesValidationErrors.isEmpty() shouldBe true
    }

    @Test fun `should validate invalid example`() {
        val filename = "src/test/resources/test-example-invalid.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        parser.examplesValidationErrors.size shouldBe 1
        with(parser.examplesValidationErrors[0]) {
            valid shouldBe false
            with(errors) {
                shouldBeNonNull()
                size shouldBe 3
                with(this[0]) {
                    keywordLocation shouldBe "#"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-example-invalid#"
                    instanceLocation shouldBe "#/example"
                    error shouldBe JSONSchema.subSchemaErrorMessage
                }
                with(this[1]) {
                    keywordLocation shouldBe "#/properties/aaa"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-example-invalid#/properties/aaa"
                    instanceLocation shouldBe "#/example/aaa"
                    error shouldBe JSONSchema.subSchemaErrorMessage
                }
                with(this[2]) {
                    keywordLocation shouldBe "#/properties/aaa/minLength"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-example-invalid#/properties/aaa/minLength"
                    instanceLocation shouldBe "#/example/aaa"
                    error shouldBe "String fails length check: minLength 3, was 2"
                }
            }
        }
    }

    @Test fun `should validate invalid example 2`() {
        val filename = "src/test/resources/test-example-invalid-2.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        parser.examplesValidationErrors.size shouldBe 1
        with(parser.examplesValidationErrors[0]) {
            valid shouldBe false
            with(errors) {
                shouldBeNonNull()
                size shouldBe 2
                with(this[0]) {
                    keywordLocation shouldBe "#"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-example-invalid-2#"
                    instanceLocation shouldBe "#/example"
                    error shouldBe JSONSchema.subSchemaErrorMessage
                }
                with(this[1]) {
                    keywordLocation shouldBe "#/minimum"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-example-invalid-2#/minimum"
                    instanceLocation shouldBe "#/example"
                    error shouldBe "Number fails check: minimum 20, was 19"
                }
            }
        }
    }

    @Test fun `should validate examples`() {
        val filename = "src/test/resources/test-examples-valid.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        parser.examplesValidationErrors.isEmpty() shouldBe true
    }

    @Test fun `should validate invalid examples`() {
        val filename = "src/test/resources/test-examples-invalid.schema.json"
        val parser = Parser()
        parser.options.validateExamples = true
        parser.parseFile(filename)
        parser.examplesValidationErrors.size shouldBe 3
        with(parser.examplesValidationErrors[0]) {
            valid shouldBe false
            with(errors) {
                shouldBeNonNull()
                size shouldBe 2
                with(this[0]) {
                    keywordLocation shouldBe "#/properties/bbb"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-examples-invalid#/properties/bbb"
                    instanceLocation shouldBe "#/properties/bbb/examples/0"
                    error shouldBe JSONSchema.subSchemaErrorMessage
                }
                with(this[1]) {
                    keywordLocation shouldBe "#/properties/bbb/minimum"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-examples-invalid#/properties/bbb/minimum"
                    instanceLocation shouldBe "#/properties/bbb/examples/0"
                    error shouldBe "Number fails check: minimum 0, was -1"
                }
            }
        }
        with(parser.examplesValidationErrors[1]) {
            with(errors) {
                shouldBeNonNull()
                size shouldBe 3
                with(this[0]) {
                    keywordLocation shouldBe "#"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-examples-invalid#"
                    instanceLocation shouldBe "#/examples/2"
                    error shouldBe JSONSchema.subSchemaErrorMessage
                }
                with(this[1]) {
                    keywordLocation shouldBe "#/properties/aaa"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-examples-invalid#/properties/aaa"
                    instanceLocation shouldBe "#/examples/2/aaa"
                    error shouldBe JSONSchema.subSchemaErrorMessage
                }
                with(this[2]) {
                    keywordLocation shouldBe "#/properties/aaa/minLength"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-examples-invalid#/properties/aaa/minLength"
                    instanceLocation shouldBe "#/examples/2/aaa"
                    error shouldBe "String fails length check: minLength 3, was 2"
                }
            }
        }
        with(parser.examplesValidationErrors[2]) {
            with(errors) {
                shouldBeNonNull()
                size shouldBe 4
                with(this[0]) {
                    keywordLocation shouldBe "#"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-examples-invalid#"
                    instanceLocation shouldBe "#/examples/3"
                    error shouldBe JSONSchema.subSchemaErrorMessage
                }
                with(this[1]) {
                    keywordLocation shouldBe "#/properties/bbb"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-examples-invalid#/properties/bbb"
                    instanceLocation shouldBe "#/examples/3/bbb"
                    error shouldBe JSONSchema.subSchemaErrorMessage
                }
                with(this[2]) {
                    keywordLocation shouldBe "#/properties/bbb/minimum"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-examples-invalid#/properties/bbb/minimum"
                    instanceLocation shouldBe "#/examples/3/bbb"
                    error shouldBe "Number fails check: minimum 0, was -2"
                }
                with(this[3]) {
                    keywordLocation shouldBe "#/required/0"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-examples-invalid#/required"
                    instanceLocation shouldBe "#/examples/3"
                    error shouldBe "Required property \"aaa\" not found"
                }
            }
        }
    }

    @Test fun `should validate default`() {
        val filename = "src/test/resources/test-default-valid.schema.json"
        Parser().apply {
            options.validateDefault = true
            parseFile(filename)
            defaultValidationErrors.isEmpty() shouldBe true
        }
    }

    @Test fun `should validate invalid default`() {
        val filename = "src/test/resources/test-default-invalid.schema.json"
        val parser = Parser()
        parser.options.validateDefault = true
        parser.parseFile(filename)
        parser.defaultValidationErrors.size shouldBe 1
        with(parser.defaultValidationErrors[0]) {
            valid shouldBe false
            with(errors) {
                shouldBeNonNull()
                size shouldBe 2
                with(this[0]) {
                    keywordLocation shouldBe "#/properties/aaa"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-default-invalid#/properties/aaa"
                    instanceLocation shouldBe "#/properties/aaa/default"
                    error shouldBe JSONSchema.subSchemaErrorMessage
                }
                with(this[1]) {
                    keywordLocation shouldBe "#/properties/aaa/minimum"
                    absoluteKeywordLocation shouldBe "http://pwall.net/test-default-invalid#/properties/aaa/minimum"
                    instanceLocation shouldBe "#/properties/aaa/default"
                    error shouldBe "Number fails check: minimum 20, was 15"
                }
            }
        }
    }

}
