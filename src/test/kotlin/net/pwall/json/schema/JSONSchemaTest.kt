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

import java.io.File
import java.net.URI

import net.pwall.json.JSONObject
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.JSON
import net.pwall.json.JSONFormat

class JSONSchemaTest {

    @Test fun `should validate example schema`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example.json"))
        expect(true) { schema.validate(json).valid }
    }

    @Test fun `should validate example schema with missing property`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example-error1.json"))
        val validateResult = schema.validate(json)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate example schema with wrong property type`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example-error2.json"))
        val validateResult = schema.validate(json)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate example schema with value out of range`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example-error3.json"))
        val validateResult = schema.validate(json)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate example schema with array item of wrong type`() {
        val filename = "src/test/resources/example.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/example-error4.json"))
        val validateResult = schema.validate(json)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate example schema with reference`() {
        val filename = "src/test/resources/test-ref.schema.json"
        val schema = JSONSchema.parse(filename)
        val json = JSON.parse(File("src/test/resources/test-ref.json"))
        val validateResult = schema.validate(json)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate enum`() {
        val filename = "src/test/resources/test-enum.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1).valid }
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        val validateResult = schema.validate(json2)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate const`() {
        val filename = "src/test/resources/test-const.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1).valid }
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        val validateResult = schema.validate(json2)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate string length`() {
        val filename = "src/test/resources/test-string-length.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1).valid }
        val json2 = JSON.parse("""{"aaa":"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"}""")
        val validateResult = schema.validate(json2)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate string pattern`() {
        val filename = "src/test/resources/test-string-pattern.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"A001"}""")
        expect(true) { schema.validate(json1).valid }
        val json2 = JSON.parse("""{"aaa":"9999"}""")
        val validateResult = schema.validate(json2)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate string format`() {
        val filename = "src/test/resources/test-string-format.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"dateTimeTest":"2020-07-22T19:29:33.456+10:00"}""")
        expect(true) { schema.validate(json1).valid }
        val json2 = JSON.parse("""{"dateTimeTest":"wrong"}""")
        val validateResult2 = schema.validate(json2)
        expect(false) { validateResult2.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult2.toJSON()))
        val json3 = JSON.parse("""{"dateTest":"2020-07-22"}""")
        expect(true) { schema.validate(json3).valid }
        val json4 = JSON.parse("""{"dateTest":"wrong"}""")
        val validateResult4 = schema.validate(json4)
        expect(false) { validateResult4.valid }
        println(formatter.format(validateResult4.toJSON()))
    }

    @Test fun `should validate schema with anyOf`() {
        val filename = "src/test/resources/test-anyof.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"AAA"}""")
        expect(true) { schema.validate(json1).valid }
        val json2 = JSON.parse("""{"aaa":"2020-07-22"}""")
        expect(true) { schema.validate(json2).valid }
        val json3 = JSON.parse("""{"aaa":"wrong"}""")
        val validateResult = schema.validate(json3)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate schema with not`() {
        val filename = "src/test/resources/test-not.schema.json"
        val schema = JSONSchema.parse(filename)
        val json1 = JSON.parse("""{"aaa":"BBB"}""")
        expect(true) { schema.validate(json1).valid }
        val json2 = JSON.parse("""{"aaa":"AAA"}""")
        val validateResult = schema.validate(json2)
        expect(false) { validateResult.valid }
        val formatter = JSONFormat()
        println(formatter.format(validateResult.toJSON()))
    }

    @Test fun `should validate string of JSON`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        expect(true) { trueSchema.validate("{}").valid }
    }

    @Test fun `should return true from true schema`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        expect(true) { trueSchema.validate(emptyObject).valid }
    }

    @Test fun `should return false from false schema`() {
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        expect(false) { falseSchema.validate(emptyObject).valid }
    }

    @Test fun `should return false from not true schema`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val notTrueSchema = JSONSchema.Not(null, JSONPointer.root, trueSchema)
        expect(false) { notTrueSchema.validate(emptyObject).valid }
    }

    @Test fun `should return true from not false schema`() {
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        val notFalseSchema = JSONSchema.Not(null, JSONPointer.root, falseSchema)
        expect(true) { notFalseSchema.validate(emptyObject).valid }
    }

    @Test fun `should give correct result from allOf`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        val allOf1 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema))
        expect(true) { allOf1.validate(emptyObject).valid }
        val allOf2 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema))
        expect(true) { allOf2.validate(emptyObject).valid }
        val allOf3 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema))
        expect(true) { allOf3.validate(emptyObject).valid }
        val allOf4 = JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema))
        expect(false) { allOf4.validate(emptyObject).valid }
        val allOf5 = JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, falseSchema))
        expect(false) { allOf5.validate(emptyObject).valid }
        val allOf6 = JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema))
        expect(false) { allOf6.validate(emptyObject).valid }
        val allOf7 = JSONSchema.allOf(null, JSONPointer.root, listOf(falseSchema, trueSchema))
        expect(false) { allOf7.validate(emptyObject).valid }
        val allOf8 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, falseSchema))
        expect(false) { allOf8.validate(emptyObject).valid }
        val allOf9 = JSONSchema.allOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema))
        expect(false) { allOf9.validate(emptyObject).valid }
    }

    @Test fun `should give correct result from anyOf`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        val anyOf1 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema))
        expect(true) { anyOf1.validate(emptyObject).valid }
        val anyOf2 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema))
        expect(true) { anyOf2.validate(emptyObject).valid }
        val anyOf3 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema))
        expect(true) { anyOf3.validate(emptyObject).valid }
        val anyOf4 = JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema))
        expect(false) { anyOf4.validate(emptyObject).valid }
        val anyOf5 = JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, falseSchema))
        expect(false) { anyOf5.validate(emptyObject).valid }
        val anyOf6 = JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema))
        expect(false) { anyOf6.validate(emptyObject).valid }
        val anyOf7 = JSONSchema.anyOf(null, JSONPointer.root, listOf(falseSchema, trueSchema))
        expect(true) { anyOf7.validate(emptyObject).valid }
        val anyOf8 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, falseSchema))
        expect(true) { anyOf8.validate(emptyObject).valid }
        val anyOf9 = JSONSchema.anyOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema))
        expect(true) { anyOf9.validate(emptyObject).valid }
    }

    @Test fun `should give correct result from oneOf`() {
        val trueSchema = JSONSchema.True(null, JSONPointer.root)
        val falseSchema = JSONSchema.False(null, JSONPointer.root)
        val oneOf1 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema))
        expect(true) { oneOf1.validate(emptyObject).valid }
        val oneOf2 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema))
        expect(false) { oneOf2.validate(emptyObject).valid }
        val oneOf3 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, trueSchema))
        expect(false) { oneOf3.validate(emptyObject).valid }
        val oneOf4 = JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema))
        expect(false) { oneOf4.validate(emptyObject).valid }
        val oneOf5 = JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, falseSchema))
        expect(false) { oneOf5.validate(emptyObject).valid }
        val oneOf6 = JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, falseSchema, falseSchema))
        expect(false) { oneOf6.validate(emptyObject).valid }
        val oneOf7 = JSONSchema.oneOf(null, JSONPointer.root, listOf(falseSchema, trueSchema))
        expect(true) { oneOf7.validate(emptyObject).valid }
        val oneOf8 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, falseSchema))
        expect(true) { oneOf8.validate(emptyObject).valid }
        val oneOf9 = JSONSchema.oneOf(null, JSONPointer.root, listOf(trueSchema, trueSchema, falseSchema))
        expect(false) { oneOf9.validate(emptyObject).valid }
    }

    @Test fun `should give correct absolute location`() {
        val schema1 = JSONSchema.True(URI("http://pwall.net/schema/true1"), JSONPointer("/abc/0"))
        expect("http://pwall.net/schema/true1#/abc/0") { schema1.absoluteLocation }
    }

    @Test fun `should validate null`() {
        val filename = "src/test/resources/test-type-null.schema.json"
        val schema = JSONSchema.parse(filename)
        expect(true) { schema.validate("null").valid }
        expect(false) { schema.validate("{}").valid }
        expect(false) { schema.validate("123").valid }
        expect(false) { schema.validate("[]").valid }
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
    }

    companion object {

        val emptyObject = JSONObject()

    }

}
