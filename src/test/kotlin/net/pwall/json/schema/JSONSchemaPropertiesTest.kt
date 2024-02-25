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
import kotlin.test.expect
import kotlin.test.fail

import net.pwall.json.JSON

class JSONSchemaPropertiesTest {

    @Test fun `should validate patternProperties`() {
        val filename = "src/test/resources/test-pattern-properties.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"X","field1":11}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"X","field1":1}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-pattern#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/patternProperties/%5Efield%5B0-9%5D%7B1%2C3%7D\$") { it.keywordLocation }
            expect("http://pwall.net/test-pattern#/patternProperties/%5Efield%5B0-9%5D%7B1%2C3%7D\$") {
                    it.absoluteKeywordLocation }
            expect("#/field1") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/patternProperties/%5Efield%5B0-9%5D%7B1%2C3%7D\$/minimum") { it.keywordLocation }
            expect("http://pwall.net/test-pattern#/patternProperties/%5Efield%5B0-9%5D%7B1%2C3%7D\$/minimum") {
                    it.absoluteKeywordLocation }
            expect("#/field1") { it.instanceLocation }
            expect("Number fails check: minimum 10, was 1") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate patternProperties with additionalProperties`() {
        val filename = "src/test/resources/test-pattern-properties.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"X","field1":11,"extra":25}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"X","field1":11,"extra":2}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(4) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-pattern#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/additionalProperties") { it.keywordLocation }
            expect("http://pwall.net/test-pattern#/additionalProperties") {
                    it.absoluteKeywordLocation }
            expect("#/extra") { it.instanceLocation }
            expect("Additional property 'extra' found but was invalid") { it.error }
        }
        errors[2].let {
            expect("#/additionalProperties") { it.keywordLocation }
            expect("http://pwall.net/test-pattern#/additionalProperties") { it.absoluteKeywordLocation }
            expect("#/extra") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[3].let {
            expect("#/additionalProperties/minimum") { it.keywordLocation }
            expect("http://pwall.net/test-pattern#/additionalProperties/minimum") { it.absoluteKeywordLocation }
            expect("#/extra") { it.instanceLocation }
            expect("Number fails check: minimum 20, was 2") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

    @Test fun `should validate propertyNames`() {
        val filename = "src/test/resources/test-property-names.schema.json"
        val schema = JSONSchema.parseFile(filename)
        val json1 = JSON.parse("""{"aaa":"X","bbb":11}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        expect(true) { schema.validateDetailed(json1).valid }
        val json2 = JSON.parse("""{"aaa":"X","bbbb":11}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        errors[0].let {
            expect("#") { it.keywordLocation }
            expect("http://pwall.net/test-property-names#") { it.absoluteKeywordLocation }
            expect("#") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[1].let {
            expect("#/propertyNames") { it.keywordLocation }
            expect("http://pwall.net/test-property-names#/propertyNames") { it.absoluteKeywordLocation }
            expect("#/bbbb") { it.instanceLocation }
            expect(JSONSchema.subSchemaErrorMessage) { it.error }
        }
        errors[2].let {
            expect("#/propertyNames/maxLength") { it.keywordLocation }
            expect("http://pwall.net/test-property-names#/propertyNames/maxLength") { it.absoluteKeywordLocation }
            expect("#/bbbb") { it.instanceLocation }
            expect("String fails length check: maxLength 3, was 4") { it.error }
        }
        expect(false) { schema.validateDetailed(json2).valid }
    }

}
