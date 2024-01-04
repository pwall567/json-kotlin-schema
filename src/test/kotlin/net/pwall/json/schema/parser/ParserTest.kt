/*
 * @(#) ParserTest.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2021, 2022 Peter Wall
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

package net.pwall.json.schema.parser

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect
import kotlin.test.fail

import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.nio.file.FileSystems

import net.pwall.json.JSON
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchemaException
import net.pwall.json.schema.parser.Parser.Companion.defaultExtendedResolver
import net.pwall.json.schema.parser.Parser.Companion.isPositive
import net.pwall.json.schema.subschema.PropertiesSchema
import net.pwall.json.schema.subschema.RefSchema
import net.pwall.json.schema.subschema.RequiredSchema
import net.pwall.json.schema.validation.EnumValidator
import net.pwall.json.schema.validation.TypeValidator

class ParserTest {

    @Test fun `should parse empty schema`() {
        val filename = "src/test/resources/empty.schema.json"
        val expected = JSONSchema.General("http://json-schema.org/draft/2019-09/schema", null, null,
                URI("http://pwall.net/schema/test/empty"), JSONPointer.root, emptyList())
        expect(expected) { JSONSchema.parseFile(filename) }
    }

    @Test fun `should parse true schema`() {
        val filename = "src/test/resources/true.schema.json"
        val uri = URI("file://${File(filename).absolutePath}")
        expect(JSONSchema.True(uri, JSONPointer.root)) { JSONSchema.parseFile(filename) }
    }

    @Test fun `should parse false schema`() {
        val filename = "src/test/resources/false.schema.json"
        val uri = URI("file://${File(filename).absolutePath}")
        expect(JSONSchema.False(uri, JSONPointer.root)) { JSONSchema.parseFile(filename) }
    }

    @Test fun `should parse test schema with type null`() {
        val filename = "src/test/resources/test-type-null.schema.json"
        val uri = URI("http://pwall.net/schema/test/type-null")
        val typeTest = TypeValidator(uri, JSONPointer.root.child("type"), listOf(JSONSchema.Type.NULL))
        val expected = JSONSchema.General("http://json-schema.org/draft/2019-09/schema", null, null, uri,
                JSONPointer.root, listOf(typeTest))
        expect(expected) { JSONSchema.parseFile(filename) }
    }

    @Test fun `should fail on invalid schema`() {
        val filename = "src/test/resources/invalid-1.schema.json"
        with(assertFailsWith<JSONSchemaException> { JSONSchema.parseFile(filename) }.message) {
            assertTrue(this != null)
            assertTrue(startsWith("Schema is not boolean or object"))
        }
    }

    @Test fun `should pre-load directory`() {
        val dirName = "src/test/resources/test1"
        val parser = Parser()
        parser.preLoad(dirName)
        val uriString = "http://pwall.net/test/schema/person"
        expect(true) { parser.parseURI(uriString).uri.toString() == uriString }
        val uri = URI(uriString)
        expect(true) { parser.parse(uri).uri == uri }
    }

    @Test fun `should pre-load directory using Path`() {
        val dirName = "src/test/resources/test1"
        val path = FileSystems.getDefault().getPath(dirName)
        val parser = Parser()
        parser.preLoad(path)
        val uriString = "http://pwall.net/test/schema/person"
        expect(true) { parser.parseURI(uriString).uri.toString() == uriString }
        val uri = URI(uriString)
        expect(true) { parser.parse(uri).uri == uri }
    }

    @Test fun `should pre-load individual file`() {
        val fileName = "src/test/resources/example.schema.json"
        val parser = JSONSchema.parser
        parser.preLoad(File(fileName))
        val uriString = "http://pwall.net/test"
        expect(true) { parser.parseURI(uriString).uri.toString() == uriString }
        val uri = URI(uriString)
        expect(true) { parser.parse(uri).uri == uri }
    }

    @Test fun `should parse reference following pre-load`() {
        val dirName = "src/test/resources/test1"
        val parser = Parser()
        parser.preLoad(dirName)
        val schema = parser.parseFile("$dirName/person/person.schema.json")
        if (schema !is JSONSchema.General)
            fail("Unexpected schema type")
        val propertySchema = (schema.children.find { it is PropertiesSchema }) as PropertiesSchema?
        val idSchema = propertySchema?.properties?.find { it.first == "id" }?.second
        if (idSchema !is JSONSchema.General)
            fail("id unexpected schema type")
        val refSchema = (idSchema.children.find { it is RefSchema }) as RefSchema?
        expect("/\$defs/personId") { refSchema?.fragment }
        val person = JSON.parse(File("src/test/resources/person.json"))
        expect(true) { schema.validate(person) }
        val wrongPerson = JSON.parse(File("src/test/resources/person-invalid-uuid.json"))
        expect(false) { schema.validate(wrongPerson) }
    }

    @Test fun `should parse individual subschema from larger file`() {
        val json = JSON.parse(File("src/test/resources/example.schema.json"))
        val schema = Parser().parseSchema(json, JSONPointer("/properties/stock"), URI("http://pwall.net/test"))
        expect(true) { schema.validate(JSON.parse("""{"warehouse":1,"retail":2}""")) }
    }

    @Test fun `should parse schema with description in external file`() {
        val parser = Parser()
        parser.options.allowDescriptionRef = true
        val schema = parser.parseFile("src/test/resources/test-description-ref.schema.yaml")
        if (schema !is JSONSchema.General)
            fail("Unexpected schema type")
        val description = schema.description ?: fail("Description is null")
        expect(true) { description.startsWith("This is an example ") }
    }

    @Test fun `should parse a schema from a string`() {
        val string = """{"enum":[1,2,4]}"""
        val schema = Parser().parse(string)
        assertTrue(schema is JSONSchema.General)
        expect(1) { schema.children.size }
        val child = schema.children[0]
        assertTrue(child is EnumValidator)
    }

    @Test fun `should parse a schema from a string with a URI`() {
        val string = """{"enum":[1,2,4]}"""
        val uri = URI.create("http://test.com/test")
        val schema = JSONSchema.parse(string, uri)
        expect(uri) { schema.uri }
    }

    @Test fun `should cache parsed object in JSONReader`() {
        val parser = Parser()
        val file = File("src/test/resources/example.json")
        val object1 = parser.jsonReader.readJSON(file)
        val object2 = parser.jsonReader.readJSON(file)
        assertSame(object1, object2)
    }

    @Test fun `should parse schema from an opaque URI providing JSON without content type`() {
        val file = File("src/test/resources/example.schema.json")
        val uri = URI.create("opaque:example-schema-json")
        val parser = Parser()
        parser.setExtendedResolver { InputDetails(file.reader()) }
        val schema = parser.parse(uri)
        assertTrue(schema is JSONSchema.General)
    }

    @Test fun `should parse schema from an opaque URI providing JSON with content type`() {
        val file = File("src/test/resources/example.schema.json")
        val uri = URI.create("opaque:example-schema-json")
        val parser = Parser()
        parser.setExtendedResolver { InputDetails(file.reader(), "application/json") }
        val schema = parser.parse(uri)
        assertTrue(schema is JSONSchema.General)
    }

    @Test fun `should parse schema from an opaque URI providing YAML with content type`() {
        val file = File("src/test/resources/example.schema.yaml")
        val uri = URI.create("opaque:example-schema-yaml")
        val parser = Parser()
        parser.setExtendedResolver { InputDetails(file.reader(), "application/yaml") }
        val schema = parser.parse(uri)
        assertTrue(schema is JSONSchema.General)
    }

    @Test fun `should parse schema from a jar URI providing JSON`() {
        val uri = URI.create("jar:file:src/test/resources/jar/example.jar!/example.schema.json")
        val schema = Parser().parse(uri)
        assertTrue(schema is JSONSchema.General)
    }

    @Test fun `should parse schema from a jar URI providing YAML`() {
        val uri = URI.create("jar:file:src/test/resources/jar/example.jar!/example.schema.yaml")
        val schema = Parser().parse(uri)
        assertTrue(schema is JSONSchema.General)
    }

    @Test fun `should read schema using HTTP`() {
        val parser = Parser()
        parser.setExtendedResolver(defaultExtendedResolver)
        val schema = parser.parse(URI("http://kjson.io/json/http/testhttp1.json"))
        assertTrue(schema is JSONSchema.General)
        expect(2) { schema.children.size }
        with(schema.children[0]) {
            assertTrue(this is TypeValidator)
            expect(listOf(JSONSchema.Type.OBJECT)) { types }
        }
        with(schema.children[1]) {
            assertTrue(this is PropertiesSchema)
            expect(1) { properties.size }
            with(properties[0]) {
                expect("xxx") { first }
                with(second) {
                    assertTrue(this is JSONSchema.General)
                    expect(1) { children.size }
                    with(children[0]) {
                        assertTrue(this is RefSchema)
                        with(target) {
                            assertTrue(this is JSONSchema.General)
                            expect(3) { children.size }
                            with(children[0]) {
                                assertTrue(this is TypeValidator)
                                expect(listOf(JSONSchema.Type.OBJECT)) { types }
                            }
                            with(children[1]) {
                                assertTrue(this is PropertiesSchema)
                                expect(1) { properties.size }
                                with(properties[0]) {
                                    expect("aaa") { first }
                                    with(second) {
                                        assertTrue(this is JSONSchema.General)
                                        expect(1) { children.size }
                                        with(children[0]) {
                                            assertTrue(this is TypeValidator)
                                            expect(listOf(JSONSchema.Type.INTEGER)) { types }
                                        }
                                    }
                                }
                            }
                            with(children[2]) {
                                assertTrue(this is RequiredSchema)
                                expect(listOf("aaa")) { properties }
                            }
                        }
                    }
                }
            }
        }
    }

    // Test functions in companion object (flagged as errors in IntelliJ)
    // For some unknown reason IntelliJ shows some branches of the "isPositive" function as "always false"
    // This test is just to reassure myself that the function is working as expected

    @Test fun `should test isPositive correctly`() {
        val bigDecimal1 = BigDecimal("0.1")
        assertTrue(bigDecimal1.isPositive())
        val bigDecimal2 = BigDecimal.ZERO
        assertFalse(bigDecimal2.isPositive())
        val bigDecimal3 = BigDecimal("-999")
        assertFalse(bigDecimal3.isPositive())
        val bigInteger1 = BigInteger.ONE
        assertTrue(bigInteger1.isPositive())
        val bigInteger2 = BigInteger.ZERO
        assertFalse(bigInteger2.isPositive())
        val bigInteger3 = BigInteger("-9")
        assertFalse(bigInteger3.isPositive())
        val double1 = 0.1
        assertTrue(double1.isPositive())
        val double2 = 0.0
        assertFalse(double2.isPositive())
        val double3 = -9.0
        assertFalse(double3.isPositive())
        val float1 = 0.1F
        assertTrue((float1.isPositive()))
        val float2 = 0.0F
        assertFalse((float2.isPositive()))
        val float3 = -10.0F
        assertFalse((float3.isPositive()))
        val long1 = 1L
        assertTrue(long1.isPositive())
        val long2 = 0L
        assertFalse(long2.isPositive())
        val long3 = -8L
        assertFalse(long3.isPositive())
        val int1 = 1
        assertTrue(int1.isPositive())
        val int2 = 0
        assertFalse(int2.isPositive())
        val int3 = -5
        assertFalse(int3.isPositive())
    }

}
