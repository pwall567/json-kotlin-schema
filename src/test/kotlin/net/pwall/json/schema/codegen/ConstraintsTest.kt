/*
 * @(#) ConstraintsTest.kt
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

package net.pwall.json.schema.codegen

import kotlin.test.Test
import kotlin.test.expect

import java.net.URI

import net.pwall.json.JSONObject
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.parser.Parser

class ConstraintsTest {

    @Test fun `should recognise type string`() {
        val constraints = Constraints(dummySchema)
        constraints.types.add(JSONSchema.Type.STRING)
        expect(true) { constraints.isIdentifiableType }
        expect(true) { constraints.isString }
        expect(false) { constraints.isInt }
    }

    @Test fun `should recognise type integer when maximum and minimum added`() {
        val constraints = Constraints(dummySchema)
        constraints.types.add(JSONSchema.Type.INTEGER)
        expect(false) { constraints.isInt }
        expect(true) { constraints.isLong }
        constraints.minimum = 0
        expect(false) { constraints.isInt }
        expect(true) { constraints.isLong }
        constraints.maximum = 4000
        expect(true) { constraints.isIdentifiableType }
        expect(false) { constraints.isString }
        expect(true) { constraints.isInt }
        expect(false) { constraints.isLong }
    }

    @Test fun `should convert URI to name usable as class name`() {
        val constraints = Constraints(dummySchema)
        constraints.uri = URI("http://example.com/abc/test")
        expect("Test") { constraints.nameFromURI }
    }

    @Test fun `should convert complex URI to name usable as class name`() {
        val constraints = Constraints(dummySchema)
        constraints.uri = URI("http://example.com/abc/more-complex-test-name.schema.json#/fragment/extra")
        expect("MoreComplexTestName") { constraints.nameFromURI }
    }

    @Test fun `should calculate lcm`() { // well, sorta...
        expect(15) { Constraints.lcm(3, 5) }
        expect(20) { Constraints.lcm(10, 4) }
        expect(256) { Constraints.lcm(256, 16) }
    }

    companion object {
        val dummySchema = Parser().parseSchema(JSONObject(), JSONPointer.root, null)
    }

}
