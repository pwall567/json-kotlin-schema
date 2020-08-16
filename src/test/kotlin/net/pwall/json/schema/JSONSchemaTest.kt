/*
 * @(#) JSONSchemaTest.kt
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

import java.io.File
import java.net.URI

import net.pwall.json.JSONObject
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.JSON

class JSONSchemaTest {

    @Test fun `should validate example schema`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example.json"))
        expect(true) { schema.validate(json) }
        expect(true) { schema.validateBasic(json).valid }
        expect(true) { schema.validateDetailed(json).valid }
    }

    @Test fun `should validate example schema from string`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val jsonString = File("src/test/resources/example.json").readText()
        expect(true) { schema.validate(jsonString) }
        expect(true) { schema.validateBasic(jsonString).valid }
        expect(true) { schema.validateDetailed(jsonString).valid }
    }

    @Test fun `should validate example schema with missing property`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example-error1.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(2) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/required/1") { error1.keywordLocation }
        expect("http://pwall.net/test#/required") { error1.absoluteKeywordLocation }
        expect("#") { error1.instanceLocation }
        expect("Required property \"name\" not found") { error1.error }
    }

    @Test fun `should validate example schema with wrong property type`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example-error2.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/price") { error1.keywordLocation }
        expect("http://pwall.net/test#/properties/price") { error1.absoluteKeywordLocation }
        expect("#/price") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/price/type") { error2.keywordLocation }
        expect("http://pwall.net/test#/properties/price/type") { error2.absoluteKeywordLocation }
        expect("#/price") { error2.instanceLocation }
        expect("Incorrect type, expected number") { error2.error }
    }

    @Test fun `should validate example schema with value out of range`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example-error3.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/price") { error1.keywordLocation }
        expect("http://pwall.net/test#/properties/price") { error1.absoluteKeywordLocation }
        expect("#/price") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/price/minimum") { error2.keywordLocation }
        expect("http://pwall.net/test#/properties/price/minimum") { error2.absoluteKeywordLocation }
        expect("#/price") { error2.instanceLocation }
        expect("Number fails check: minimum 0, was -1") { error2.error }
    }

    @Test fun `should validate example schema with array item of wrong type`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example-error4.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(4) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/tags") { error1.keywordLocation }
        expect("http://pwall.net/test#/properties/tags") { error1.absoluteKeywordLocation }
        expect("#/tags") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/tags/items") { error2.keywordLocation }
        expect("http://pwall.net/test#/properties/tags/items") { error2.absoluteKeywordLocation }
        expect("#/tags/2") { error2.instanceLocation }
        expect("A subschema had errors") { error2.error }
        val error3 = errors[3]
        expect("#/properties/tags/items/type") { error3.keywordLocation }
        expect("http://pwall.net/test#/properties/tags/items/type") { error3.absoluteKeywordLocation }
        expect("#/tags/2") { error3.instanceLocation }
        expect("Incorrect type, expected string") { error3.error }
    }

    @Test fun `should validate example schema with reference`() {
        val filename = "src/test/resources/test-ref.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/test-ref.json"))
        expect(false) { schema.validate(json) }
        val validateResult = schema.validateBasic(json)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(4) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test-ref#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/bbb") { error1.keywordLocation }
        expect("http://pwall.net/test-ref#/properties/bbb") { error1.absoluteKeywordLocation }
        expect("#/bbb") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/bbb/\$ref") { error2.keywordLocation }
        expect("http://pwall.net/test-ref#/\$defs/amount") { error2.absoluteKeywordLocation }
        expect("#/bbb") { error2.instanceLocation }
        expect("A subschema had errors") { error2.error }
        val error3 = errors[3]
        expect("#/properties/bbb/\$ref/type") { error3.keywordLocation }
        expect("http://pwall.net/test-ref#/\$defs/amount/type") { error3.absoluteKeywordLocation }
        expect("#/bbb") { error3.instanceLocation }
        expect("Incorrect type, expected number") { error3.error }
    }

    @Test fun `should validate enum`() {
        val filename = "src/test/resources/test-enum.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1) }
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test-enum#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/aaa") { error1.keywordLocation }
        expect("http://pwall.net/test-enum#/properties/aaa") { error1.absoluteKeywordLocation }
        expect("#/aaa") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/aaa/enum") { error2.keywordLocation }
        expect("http://pwall.net/test-enum#/properties/aaa/enum") { error2.absoluteKeywordLocation }
        expect("#/aaa") { error2.instanceLocation }
        expect("Not in enumerated values: \"ZZZZZZZZZZZZZZZ ... ZZZZZZZZZZZZZZZ\"") { error2.error }
    }

    @Test fun `should validate const`() {
        val filename = "src/test/resources/test-const.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1) }
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test-const#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/aaa") { error1.keywordLocation }
        expect("http://pwall.net/test-const#/properties/aaa") { error1.absoluteKeywordLocation }
        expect("#/aaa") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/aaa/const") { error2.keywordLocation }
        expect("http://pwall.net/test-const#/properties/aaa/const") { error2.absoluteKeywordLocation }
        expect("#/aaa") { error2.instanceLocation }
        expect("Does not match constant: \"ZZZZZZZZZZZZZZZ ... ZZZZZZZZZZZZZZZ\"") { error2.error }
    }

    @Test fun `should validate string length`() {
        val filename = "src/test/resources/test-string-length.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1) }
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test-string-length#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/aaa") { error1.keywordLocation }
        expect("http://pwall.net/test-string-length#/properties/aaa") { error1.absoluteKeywordLocation }
        expect("#/aaa") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/aaa/maxLength") { error2.keywordLocation }
        expect("http://pwall.net/test-string-length#/properties/aaa/maxLength") { error2.absoluteKeywordLocation }
        expect("#/aaa") { error2.instanceLocation }
        expect("String fails length check: maxLength 10, was 66") { error2.error }
    }

    @Test fun `should validate string pattern`() {
        val filename = "src/test/resources/test-string-pattern.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"A001"}""")
        expect(true) { schema.validate(json1) }
        val json2 = JSON.parse("""{"aaa":"9999"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test-string-pattern#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/aaa") { error1.keywordLocation }
        expect("http://pwall.net/test-string-pattern#/properties/aaa") { error1.absoluteKeywordLocation }
        expect("#/aaa") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/aaa/pattern") { error2.keywordLocation }
        expect("http://pwall.net/test-string-pattern#/properties/aaa/pattern") { error2.absoluteKeywordLocation }
        expect("#/aaa") { error2.instanceLocation }
        expect("String doesn't match pattern ^[A-Z][0-9]{3}\$ - \"9999\"") { error2.error }
    }

    @Test fun `should validate string format`() {
        val filename = "src/test/resources/test-string-format.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"dateTimeTest":"2020-07-22T19:29:33.456+10:00"}""")
        expect(true) { schema.validate(json1) }
        expect(true) { schema.validateBasic(json1).valid }
        val json2 = JSON.parse("""{"dateTimeTest":"wrong"}""")
        expect(false) { schema.validate(json2) }
        val validateResult2 = schema.validateBasic(json2)
        expect(false) { validateResult2.valid }
        val errors2 = validateResult2.errors ?: fail()
        expect(3) { errors2.size }
        val error20 = errors2[0]
        expect("#") { error20.keywordLocation }
        expect("http://pwall.net/test-string-format#") { error20.absoluteKeywordLocation }
        expect("#") { error20.instanceLocation }
        expect("A subschema had errors") { error20.error }
        val error21 = errors2[1]
        expect("#/properties/dateTimeTest") { error21.keywordLocation }
        expect("http://pwall.net/test-string-format#/properties/dateTimeTest") { error21.absoluteKeywordLocation }
        expect("#/dateTimeTest") { error21.instanceLocation }
        expect("A subschema had errors") { error21.error }
        val error22 = errors2[2]
        expect("#/properties/dateTimeTest/format") { error22.keywordLocation }
        expect("http://pwall.net/test-string-format#/properties/dateTimeTest/format") { error22.absoluteKeywordLocation }
        expect("#/dateTimeTest") { error22.instanceLocation }
        expect("String fails format check: date-time, was wrong") { error22.error }
        val json3 = JSON.parse("""{"dateTest":"2020-07-22"}""")
        expect(true) { schema.validate(json3) }
        expect(true) { schema.validateBasic(json3).valid }
        val json4 = JSON.parse("""{"dateTest":"wrong"}""")
        expect(false) { schema.validate(json4) }
        val validateResult4 = schema.validateBasic(json4)
        expect(false) { validateResult4.valid }
        val errors4 = validateResult4.errors ?: fail()
        expect(3) { errors4.size }
        val error40 = errors4[0]
        expect("#") { error40.keywordLocation }
        expect("http://pwall.net/test-string-format#") { error40.absoluteKeywordLocation }
        expect("#") { error40.instanceLocation }
        expect("A subschema had errors") { error40.error }
        val error41 = errors4[1]
        expect("#/properties/dateTest") { error41.keywordLocation }
        expect("http://pwall.net/test-string-format#/properties/dateTest") { error41.absoluteKeywordLocation }
        expect("#/dateTest") { error41.instanceLocation }
        expect("A subschema had errors") { error41.error }
        val error42 = errors4[2]
        expect("#/properties/dateTest/format") { error42.keywordLocation }
        expect("http://pwall.net/test-string-format#/properties/dateTest/format") { error42.absoluteKeywordLocation }
        expect("#/dateTest") { error42.instanceLocation }
        expect("String fails format check: date, was wrong") { error42.error }
    }

    @Test fun `should validate schema with anyOf`() {
        val filename = "src/test/resources/test-anyof.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1) }
        val json2 = JSON.parse("""{"aaa":"2020-07-22"}""")
        expect(true) { schema.validate(json2) }
        val json3 = JSON.parse("""{"aaa":"wrong"}""")
        expect(false) { schema.validate(json3) }
        val validateResult = schema.validateBasic(json3)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(7) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test-anyof#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/aaa") { error1.keywordLocation }
        expect("http://pwall.net/test-anyof#/properties/aaa") { error1.absoluteKeywordLocation }
        expect("#/aaa") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/aaa/anyOf") { error2.keywordLocation }
        expect("http://pwall.net/test-anyof#/properties/aaa/anyOf") { error2.absoluteKeywordLocation }
        expect("#/aaa") { error2.instanceLocation }
        expect("Combination schema \"anyOf\" fails - 0 of 2 valid") { error2.error }
        val error3 = errors[3]
        expect("#/properties/aaa/anyOf/0") { error3.keywordLocation }
        expect("http://pwall.net/test-anyof#/properties/aaa/anyOf/0") { error3.absoluteKeywordLocation }
        expect("#/aaa") { error3.instanceLocation }
        expect("A subschema had errors") { error3.error }
        val error4 = errors[4]
        expect("#/properties/aaa/anyOf/0/const") { error4.keywordLocation }
        expect("http://pwall.net/test-anyof#/properties/aaa/anyOf/0/const") { error4.absoluteKeywordLocation }
        expect("#/aaa") { error4.instanceLocation }
        expect("Does not match constant: \"wrong\"") { error4.error }
        val error5 = errors[5]
        expect("#/properties/aaa/anyOf/1") { error5.keywordLocation }
        expect("http://pwall.net/test-anyof#/properties/aaa/anyOf/1") { error5.absoluteKeywordLocation }
        expect("#/aaa") { error5.instanceLocation }
        expect("A subschema had errors") { error5.error }
        val error6 = errors[6]
        expect("#/properties/aaa/anyOf/1/format") { error6.keywordLocation }
        expect("http://pwall.net/test-anyof#/properties/aaa/anyOf/1/format") { error6.absoluteKeywordLocation }
        expect("#/aaa") { error6.instanceLocation }
        expect("String fails format check: date, was wrong") { error6.error }
    }

    @Test fun `should validate schema with not`() {
        val filename = "src/test/resources/test-not.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"BBB"}""")
        expect(true) { schema.validate(json1) }
        val json2 = JSON.parse("""{"aaa":"AAA"}""")
        expect(false) { schema.validate(json2) }
        val validateResult = schema.validateBasic(json2)
        expect(false) { validateResult.valid }
        val errors = validateResult.errors ?: fail()
        expect(3) { errors.size }
        val error0 = errors[0]
        expect("#") { error0.keywordLocation }
        expect("http://pwall.net/test-not#") { error0.absoluteKeywordLocation }
        expect("#") { error0.instanceLocation }
        expect("A subschema had errors") { error0.error }
        val error1 = errors[1]
        expect("#/properties/aaa") { error1.keywordLocation }
        expect("http://pwall.net/test-not#/properties/aaa") { error1.absoluteKeywordLocation }
        expect("#/aaa") { error1.instanceLocation }
        expect("A subschema had errors") { error1.error }
        val error2 = errors[2]
        expect("#/properties/aaa/not") { error2.keywordLocation }
        expect("http://pwall.net/test-not#/properties/aaa/not") { error2.absoluteKeywordLocation }
        expect("#/aaa") { error2.instanceLocation }
        expect("Schema \"not\" - target was valid") { error2.error }
    }

    @Test fun `should validate string of JSON`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        expect(true) { trueSchema.validate("{}") }
        expect(true) { trueSchema.validateBasic("{}").valid }
    }

    @Test fun `should return true from true schema`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        expect(true) { trueSchema.validate(emptyObject) }
        expect(true) { trueSchema.validateBasic(emptyObject).valid }
    }

    @Test fun `should return false from false schema`() {
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        expect(false) { falseSchema.validate(emptyObject) }
        expect(false) { falseSchema.validateBasic(emptyObject).valid }
    }

    @Test fun `should return false from not true schema`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val notTrueSchema = JSONSchema.Not(null, JSONPointer.root, trueSchema)
        expect(false) { notTrueSchema.validate(emptyObject) }
        expect(false) { notTrueSchema.validateBasic(emptyObject).valid }
    }

    @Test fun `should return true from not false schema`() {
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        val notFalseSchema = JSONSchema.Not(null, JSONPointer.root, falseSchema)
        expect(true) { notFalseSchema.validate(emptyObject) }
        expect(true) { notFalseSchema.validateBasic(emptyObject).valid }
    }

    @Test fun `should give correct result from allOf`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        val allOf1 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema))
        expect(true) { allOf1.validate(emptyObject) }
        expect(true) { allOf1.validateBasic(emptyObject).valid }
        val allOf2 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema))
        expect(true) { allOf2.validate(emptyObject) }
        expect(true) { allOf2.validateBasic(emptyObject).valid }
        val allOf3 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema))
        expect(true) { allOf3.validate(emptyObject) }
        expect(true) { allOf3.validateBasic(emptyObject).valid }
        val allOf4 = JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema))
        expect(false) { allOf4.validate(emptyObject) }
        expect(false) { allOf4.validateBasic(emptyObject).valid }
        val allOf5 = JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, falseSchema))
        expect(false) { allOf5.validate(emptyObject) }
        expect(false) { allOf5.validateBasic(emptyObject).valid }
        val allOf6 = JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema))
        expect(false) { allOf6.validate(emptyObject) }
        expect(false) { allOf6.validateBasic(emptyObject).valid }
        val allOf7 = JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, trueSchema))
        expect(false) { allOf7.validate(emptyObject) }
        expect(false) { allOf7.validateBasic(emptyObject).valid }
        val allOf8 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, falseSchema))
        expect(false) { allOf8.validate(emptyObject) }
        expect(false) { allOf8.validateBasic(emptyObject).valid }
        val allOf9 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema))
        expect(false) { allOf9.validate(emptyObject) }
        expect(false) { allOf9.validateBasic(emptyObject).valid }
    }

    @Test fun `should give correct result from anyOf`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        val anyOf1 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema))
        expect(true) { anyOf1.validate(emptyObject) }
        expect(true) { anyOf1.validateBasic(emptyObject).valid }
        val anyOf2 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema))
        expect(true) { anyOf2.validate(emptyObject) }
        expect(true) { anyOf2.validateBasic(emptyObject).valid }
        val anyOf3 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema))
        expect(true) { anyOf3.validate(emptyObject) }
        expect(true) { anyOf3.validateBasic(emptyObject).valid }
        val anyOf4 = JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema))
        expect(false) { anyOf4.validate(emptyObject) }
        expect(false) { anyOf4.validateBasic(emptyObject).valid }
        val anyOf5 = JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, falseSchema))
        expect(false) { anyOf5.validate(emptyObject) }
        expect(false) { anyOf5.validateBasic(emptyObject).valid }
        val anyOf6 = JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema))
        expect(false) { anyOf6.validate(emptyObject) }
        expect(false) { anyOf6.validateBasic(emptyObject).valid }
        val anyOf7 = JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, trueSchema))
        expect(true) { anyOf7.validate(emptyObject) }
        expect(true) { anyOf7.validateBasic(emptyObject).valid }
        val anyOf8 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, falseSchema))
        expect(true) { anyOf8.validate(emptyObject) }
        expect(true) { anyOf8.validateBasic(emptyObject).valid }
        val anyOf9 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema))
        expect(true) { anyOf9.validate(emptyObject) }
        expect(true) { anyOf9.validateBasic(emptyObject).valid }
    }

    @Test fun `should give correct result from oneOf`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        val oneOf1 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema))
        expect(true) { oneOf1.validate(emptyObject) }
        expect(true) { oneOf1.validateBasic(emptyObject).valid }
        val oneOf2 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema))
        expect(false) { oneOf2.validate(emptyObject) }
        expect(false) { oneOf2.validateBasic(emptyObject).valid }
        val oneOf3 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema))
        expect(false) { oneOf3.validate(emptyObject) }
        expect(false) { oneOf3.validateBasic(emptyObject).valid }
        val oneOf4 = JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema))
        expect(false) { oneOf4.validate(emptyObject) }
        expect(false) { oneOf4.validateBasic(emptyObject).valid }
        val oneOf5 = JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, falseSchema))
        expect(false) { oneOf5.validate(emptyObject) }
        expect(false) { oneOf5.validateBasic(emptyObject).valid }
        val oneOf6 = JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema))
        expect(false) { oneOf6.validate(emptyObject) }
        expect(false) { oneOf6.validateBasic(emptyObject).valid }
        val oneOf7 = JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, trueSchema))
        expect(true) { oneOf7.validate(emptyObject) }
        expect(true) { oneOf7.validateBasic(emptyObject).valid }
        val oneOf8 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, falseSchema))
        expect(true) { oneOf8.validate(emptyObject) }
        expect(true) { oneOf8.validateBasic(emptyObject).valid }
        val oneOf9 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema))
        expect(false) { oneOf9.validate(emptyObject) }
        expect(false) { oneOf9.validateBasic(emptyObject).valid }
    }

    @Test fun `should give correct absolute location`() {
        val schema1 = JSONSchema.True(URI("http://pwall.net/schema/true1"), JSONPointer("/abc/0"))
        expect("http://pwall.net/schema/true1#/abc/0") { schema1.absoluteLocation }
    }

    @Test fun `should validate null`() {
        val filename = "src/test/resources/test-type-null.schema.json"
        val schema = JSONSchema.parse(filename)
        expect(true) { schema.validate("null") }
        expect(true) { schema.validateBasic("null").valid }
        expect(false) { schema.validate("{}") }
        expect(false) { schema.validateBasic("{}").valid }
        expect(false) { schema.validate("123") }
        expect(false) { schema.validateBasic("123").valid }
        expect(false) { schema.validate("[]") }
        expect(false) { schema.validateBasic("[]").valid }
    }

    @Test fun `check assumptions about URIs`() {
        val file = File("src/test/resources/example.json")
        val uri1 = URI("file://${file.absolutePath}")
        expect("file://${file.absolutePath}") { uri1.toString() }
        expect("file") { uri1.scheme }
        expect(file.absolutePath) { uri1.path }
        expect(null) { uri1.fragment }
        expect(null) { uri1.host }
        val uri2 = uri1.resolve("http://pwall.net/schema/true1")
        expect("http://pwall.net/schema/true1") { uri2.toString() }
        expect("http") { uri2.scheme }
        expect("/schema/true1") { uri2.path }
        expect(null) { uri2.fragment }
        expect("pwall.net") { uri2.host }
        val uri3 = uri2.resolve("true2")
        expect("http://pwall.net/schema/true2") { uri3.toString() }
        expect("http") { uri3.scheme }
        expect("/schema/true2") { uri3.path }
        expect(null) { uri3.fragment }
        expect("pwall.net") { uri3.host }
        val uri4 = uri3.resolve("http://pwall.net/schema/true3#")
        expect("http://pwall.net/schema/true3#") { uri4.toString() }
        expect("http") { uri4.scheme }
        expect("/schema/true3") { uri4.path }
        expect("") { uri4.fragment }
        expect("pwall.net") { uri4.host }
        val uri5 = uri3.resolve("#frag1")
        expect("http://pwall.net/schema/true2#frag1") { uri5.toString() }
        expect("http") { uri5.scheme }
        expect("/schema/true2") { uri5.path }
        expect("frag1") { uri5.fragment }
        expect("pwall.net") { uri5.host }
        val uri9 = URI("classpath:/example.json")
        expect("classpath:/example.json") { uri9.toString() }
        expect("classpath") { uri9.scheme }
        expect("/example.json") { uri9.path }
    }

    companion object {

        val emptyObject = JSONObject()

    }

}
