/*
 * @(#) ParserTest.kt
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

package net.pwall.json.schema.parser

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect
import kotlin.test.fail

import java.io.File
import java.net.URI
import java.nio.file.FileSystems

import net.pwall.json.JSON
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchemaException
import net.pwall.json.schema.subschema.PropertiesSchema
import net.pwall.json.schema.subschema.RefSchema
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
        val errorMessage = assertFailsWith<JSONSchemaException> {
            JSONSchema.parseFile(filename)
        }
        expect("Schema is not boolean or object - root") { errorMessage.message }
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

}
