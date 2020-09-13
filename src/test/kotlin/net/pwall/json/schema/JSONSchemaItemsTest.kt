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
import kotlin.test.expect
import kotlin.test.fail

import net.pwall.json.JSON

class JSONSchemaItemsTest {

    @Test fun `should validate items of array`() {
        val filename = "src/test/resources/test-item.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":[1]}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":[-1]}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(4) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-item#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect("A subschema had errors") { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-item#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("A subschema had errors") { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/items") { it.keywordLocation }
            expect("http://pwall.net/test-item#/properties/aaa/items") { it.absoluteKeywordLocation }
            expect("#/aaa/0") { it.instanceLocation }
            expect("A subschema had errors") { it.error }
        }
        errors[3].let {
            expect("#/properties/aaa/items/minimum") { it.keywordLocation }
            expect("http://pwall.net/test-item#/properties/aaa/items/minimum") { it.absoluteKeywordLocation }
            expect("#/aaa/0") { it.instanceLocation }
            expect("Number fails check: minimum 0, was -1") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate items of array with multiple validations`() {
        val filename = "src/test/resources/test-item-array.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":[1,11]}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":[1,11,1]}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(5) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-item-array#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect("A subschema had errors") { it.error }
        }
        errors[1].let {
            expect("#/properties/aaa") { it.keywordLocation }
            expect("http://pwall.net/test-item-array#/properties/aaa") { it.absoluteKeywordLocation }
            expect("#/aaa") { it.instanceLocation }
            expect("A subschema had errors") { it.error }
        }
        errors[2].let {
            expect("#/properties/aaa/additionalItems") { it.keywordLocation }
            expect("http://pwall.net/test-item-array#/properties/aaa/additionalItems") { it.absoluteKeywordLocation }
            expect("#/aaa/2") { it.instanceLocation }
            expect("Additional item 2 found but was invalid") { it.error }
        }
        errors[3].let {
            expect("#/properties/aaa/additionalItems") { it.keywordLocation }
            expect("http://pwall.net/test-item-array#/properties/aaa/additionalItems") { it.absoluteKeywordLocation }
            expect("#/aaa/2") { it.instanceLocation }
            expect("A subschema had errors") { it.error }
        }
        errors[4].let {
            expect("#/properties/aaa/additionalItems/exclusiveMaximum") { it.keywordLocation }
            expect("http://pwall.net/test-item-array#/properties/aaa/additionalItems/exclusiveMaximum") {
                    it.absoluteKeywordLocation }
            expect("#/aaa/2") { it.instanceLocation }
            expect("Number fails check: exclusiveMaximum 0, was 1") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

}
