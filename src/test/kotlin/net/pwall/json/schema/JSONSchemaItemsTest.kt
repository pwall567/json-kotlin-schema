/*
 * @(#) JSONSchemaItemsTest.kt
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
import io.kstuff.test.shouldBe

class JSONSchemaItemsTest {

    @Test fun `should validate items of array`() {
        val filename = "src/test/resources/test-item.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":[1]}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":[-1]}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 4
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-item#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-item#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/items"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-item#/properties/aaa/items"
            it.instanceLocation shouldBe "#/aaa/0"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[3].let {
            it.keywordLocation shouldBe "#/properties/aaa/items/minimum"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-item#/properties/aaa/items/minimum"
            it.instanceLocation shouldBe "#/aaa/0"
            it.error shouldBe "Number fails check: minimum 0, was -1"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate items of array with multiple validations`() {
        val filename = "src/test/resources/test-item-array.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":[1,11]}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":[1,11,1]}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 5
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-item-array#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-item-array#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/additionalItems"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-item-array#/properties/aaa/additionalItems"
            it.instanceLocation shouldBe "#/aaa/2"
            it.error shouldBe "Additional item 2 found but was invalid"
        }
        errors[3].let {
            it.keywordLocation shouldBe "#/properties/aaa/additionalItems"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-item-array#/properties/aaa/additionalItems"
            it.instanceLocation shouldBe "#/aaa/2"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[4].let {
            it.keywordLocation shouldBe "#/properties/aaa/additionalItems/exclusiveMaximum"
            it.absoluteKeywordLocation shouldBe
                    "http://pwall.net/test-item-array#/properties/aaa/additionalItems/exclusiveMaximum"
            it.instanceLocation shouldBe "#/aaa/2"
            it.error shouldBe "Number fails check: exclusiveMaximum 0, was 1"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate items of array with uniqueItems`() {
        val filename = "src/test/resources/test-unique-item.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":[1,2,3]}""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""{"aaa":[1,2,1]}""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 3
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-unique-item#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/properties/aaa"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-unique-item#/properties/aaa"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[2].let {
            it.keywordLocation shouldBe "#/properties/aaa/uniqueItems"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-unique-item#/properties/aaa/uniqueItems"
            it.instanceLocation shouldBe "#/aaa"
            it.error shouldBe "Array items not unique"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate array with contains`() {
        val filename = "src/test/resources/test-contains.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""[1,2,5]""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""[1,2,3]""")
        schema.validate(json2) shouldBe false
        val validateResult = schema.validateBasic(json2)
        validateResult.valid shouldBe false
        val errors = validateResult.errors ?: fail()
        errors.size shouldBe 2
        errors[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-contains#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors[1].let {
            it.keywordLocation shouldBe "#/contains"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-contains#/contains"
            it.instanceLocation shouldBe "#"
            it.error shouldBe "No matching entry"
        }
        schema.validateDetailed(json2).valid shouldBe false
    }

    @Test fun `should validate array with contains and min and max`() {
        val filename = "src/test/resources/test-contains-minmax.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""[1,2,5,5]""")
        schema.validate(json1) shouldBe true
        schema.validateBasic(json1).valid shouldBe true
        schema.validateDetailed(json1).valid shouldBe true
        val json2 = JSON.parse("""[1,2,5]""")
        schema.validate(json2) shouldBe false
        val validateResult1 = schema.validateBasic(json2)
        validateResult1.valid shouldBe false
        val errors1 = validateResult1.errors ?: fail()
        errors1.size shouldBe 2
        errors1[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-contains-minmax#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors1[1].let {
            it.keywordLocation shouldBe "#/minContains"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-contains-minmax#/minContains"
            it.instanceLocation shouldBe "#"
            it.error shouldBe "Matching entry minimum 2, was 1"
        }
        schema.validateDetailed(json2).valid shouldBe false
        val json3 = JSON.parse("""[1,2,5,5,5,5]""")
        schema.validate(json3) shouldBe false
        val validateResult2 = schema.validateBasic(json3)
        validateResult2.valid shouldBe false
        val errors2 = validateResult2.errors ?: fail()
        errors2.size shouldBe 2
        errors2[0].let {
            it.keywordLocation shouldBe "#"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-contains-minmax#"
            it.instanceLocation shouldBe "#"
            it.error shouldBe JSONSchema.subSchemaErrorMessage
        }
        errors2[1].let {
            it.keywordLocation shouldBe "#/maxContains"
            it.absoluteKeywordLocation shouldBe "http://pwall.net/test-contains-minmax#/maxContains"
            it.instanceLocation shouldBe "#"
            it.error shouldBe "Matching entry maximum 3, was 4"
        }
        schema.validateDetailed(json3).valid shouldBe false
    }

}
