package net.pwall.json.schema.parser

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect

import java.io.File
import java.net.URI

import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchemaException
import net.pwall.json.schema.validation.TypeValidator

class ParserTest {

    @Test fun `should parse empty schema`() {
        val filename = "src/test/resources/empty.schema.json"
        val expected = JSONSchema.General("http://json-schema.org/draft/2019-09/schema", null, null,
                URI("http://pwall.net/schema/test/empty"), JSONPointer.root, emptyList())
        expect(expected) { JSONSchema.parse(filename) }
    }

    @Test fun `should parse true schema`() {
        val filename = "src/test/resources/true.schema.json"
        val uri = URI("file://${File(filename).absolutePath}")
        expect(JSONSchema.True(uri, JSONPointer.root)) { JSONSchema.parse(filename) }
    }

    @Test fun `should parse false schema`() {
        val filename = "src/test/resources/false.schema.json"
        val uri = URI("file://${File(filename).absolutePath}")
        expect(JSONSchema.False(uri, JSONPointer.root)) { JSONSchema.parse(filename) }
    }

    @Test fun `should parse test schema with type null`() {
        val filename = "src/test/resources/test-type-null.schema.json"
        val uri = URI("http://pwall.net/schema/test/type-null")
        val typeTest = TypeValidator(uri, JSONPointer.root.child("type"), listOf(JSONSchema.Type.NULL))
        val expected = JSONSchema.General("http://json-schema.org/draft/2019-09/schema", null, null, uri,
                JSONPointer.root, listOf(typeTest))
        expect(expected) { JSONSchema.parse(filename) }
    }

    @Test fun `should fail on invalid schema`() {
        val filename = "src/test/resources/invalid-1.schema.json"
        val errorMessage = assertFailsWith<JSONSchemaException> {
            JSONSchema.parse(filename)
        }
        expect("Schema is not boolean or object - root") { errorMessage.message }
    }

}
