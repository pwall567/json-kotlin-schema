/*
 * @(#) JSONSchemaPropertiesTest.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2021 Peter Wall
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

import io.kstuff.test.shouldBe

import io.kjson.JSON

class JSONSchemaPropertiesTest {

    @Test fun `should validate patternProperties`() {
        val filename = "src/test/resources/test-pattern-properties.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"X","field1":11}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"X","field1":1}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-pattern#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/patternProperties/%5Efield%5B0-9%5D%7B1%2C3%7D\$"
            it.absoluteKeywordLocation shouldBe
                    "http://pwall.net/test-pattern#/patternProperties/%5Efield%5B0-9%5D%7B1%2C3%7D\$"
            it.instanceLocation shouldBe "#/field1"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/patternProperties/%5Efield%5B0-9%5D%7B1%2C3%7D\$/minimum"
            it.absoluteKeywordLocation shouldBe
                    "http://pwall.net/test-pattern#/patternProperties/%5Efield%5B0-9%5D%7B1%2C3%7D\$/minimum"
            it.instanceLocation shouldBe "#/field1"
            it.error shouldBe "Number fails check: minimum 10, was 1"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate patternProperties with additionalProperties`() {
        val filename = "src/test/resources/test-pattern-properties.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"X","field1":11,"extra":25}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"X","field1":11,"extra":2}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 4
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-pattern#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/additionalProperties"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-pattern#/additionalProperties"
            it.instanceLocation shouldBe "#/extra"
            it.error shouldBe "Additional property 'extra' found but was invalid"
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/additionalProperties"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-pattern#/additionalProperties"
            it.instanceLocation shouldBe "#/extra"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[3].let {
            it.keywordLocation shouldBe "#/additionalProperties/minimum"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-pattern#/additionalProperties/minimum"
            it.instanceLocation shouldBe "#/extra"
            it.error shouldBe "Number fails check: minimum 20, was 2"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate propertyNames`() {
        val filename = "src/test/resources/test-property-names.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"X","bbb":11}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":"X","bbbb":11}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-property-names#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/propertyNames"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-property-names#/propertyNames"
            it.instanceLocation shouldBe "#/bbbb"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/propertyNames/maxLength"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-property-names#/propertyNames/maxLength"
            it.instanceLocation shouldBe "#/bbbb"
            it.error shouldBe "String fails length check: maxLength 3, was 4"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

}
