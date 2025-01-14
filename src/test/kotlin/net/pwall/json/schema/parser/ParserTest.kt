/*
 * @(#) ParserTest.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2021, 2022, 2024 Peter Wall
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
import kotlin.test.fail

import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.nio.file.FileSystems

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeSameInstance
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldStartWith
import io.kstuff.test.shouldThrow

import io.kjson.JSON
import io.kjson.pointer.JSONPointer
import io.kjson.resource.ResourceLoader

import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.JSONSchemaException
import net.pwall.json.schema.parser.Parser.Companion.isPositive
import net.pwall.json.schema.subschema.PropertiesSchema
import net.pwall.json.schema.subschema.RefSchema
import net.pwall.json.schema.subschema.RequiredSchema
import net.pwall.json.schema.validation.EnumValidator
import net.pwall.json.schema.validation.TypeValidator
import net.pwall.text.Wildcard

class ParserTest {

    @Test fun `should parse empty schema`() {
        val filename = "src/test/resources/empty.schema.json"
        val expected = JSONSchema.General("http://json-schema.org/draft/2019-09/schema", null, null,
            URI("http://pwall.net/schema/test/empty"), JSONPointer.root, emptyList())
        JSONSchema.parseFile(filename) shouldBe expected
    }

    @Test fun `should parse true schema`() {
        val filename = "src/test/resources/true.schema.json"
        val uri = File(filename).absoluteFile.toURI()
        JSONSchema.parseFile(filename) shouldBe JSONSchema.True(uri, JSONPointer.root)
    }

    @Test fun `should parse false schema`() {
        val filename = "src/test/resources/false.schema.json"
        val uri = File(filename).absoluteFile.toURI()
        JSONSchema.parseFile(filename) shouldBe JSONSchema.False(uri, JSONPointer.root)
    }

    @Test fun `should parse test schema with type null`() {
        val filename = "src/test/resources/test-type-null.schema.json"
        val uri = URI("http://pwall.net/schema/test/type-null")
        val typeTest = TypeValidator(uri, JSONPointer.root.child("type"), listOf(JSONSchema.Type.NULL))
        val expected = JSONSchema.General("http://json-schema.org/draft/2019-09/schema", null, null, uri,
            JSONPointer.root, listOf(typeTest))
        JSONSchema.parseFile(filename) shouldBe expected
    }

    @Test fun `should fail on invalid schema`() {
        val filename = "src/test/resources/invalid-1.schema.json"
        shouldThrow<JSONSchemaException> { JSONSchema.parseFile(filename) }.let {
            it.message shouldStartWith "Schema is not boolean or object"
        }
    }

    @Test fun `should pre-load directory`() {
        val dirName = "src/test/resources/test1"
        val parser = Parser()
        parser.preLoad(dirName)
        val uriString = "http://pwall.net/test/schema/person"
        parser.parseURI(uriString).uri.toString() shouldBe uriString
        val uri = URI(uriString)
        parser.parse(uri).uri shouldBe uri
    }

    @Test fun `should pre-load directory using Path`() {
        val dirName = "src/test/resources/test1"
        val path = FileSystems.getDefault().getPath(dirName)
        val parser = Parser()
        parser.preLoad(path)
        val uriString = "http://pwall.net/test/schema/person"
        parser.parseURI(uriString).uri.toString() shouldBe uriString
        val uri = URI(uriString)
        parser.parse(uri).uri shouldBe uri
    }

    @Test fun `should pre-load individual file`() {
        val fileName = "src/test/resources/example.schema.json"
        val parser = JSONSchema.parser
        parser.preLoad(File(fileName))
        val uriString = "http://pwall.net/test"
        parser.parseURI(uriString).uri.toString() shouldBe uriString
        val uri = URI(uriString)
        parser.parse(uri).uri shouldBe uri
    }

    @Test fun `should parse reference following pre-load`() {
        val dirName = "src/test/resources/test1"
        val parser = Parser()
        parser.preLoad(dirName)
        val schema = parser.parseFile("$dirName/person/person.schema.json")
        schema.shouldBeType<JSONSchema.General>()
        val propertySchema = (schema.children.find { it is PropertiesSchema }) as PropertiesSchema?
        val idSchema = propertySchema?.properties?.find { it.first == "id" }?.second
        idSchema.shouldBeType<JSONSchema.General>()
        val refSchema = (idSchema.children.find { it is RefSchema }) as RefSchema?
        refSchema?.fragment shouldBe "/\$defs/personId"
        val person = JSON.parse(File("src/test/resources/person.json").readText())
        schema.validate(person) shouldBe true
        val wrongPerson = JSON.parse(File("src/test/resources/person-invalid-uuid.json").readText())
        schema.validate(wrongPerson) shouldBe false
    }

    @Test fun `should parse individual subschema from larger file`() {
        val json = JSON.parse(File("src/test/resources/example.schema.json").readText()) ?: fail()
        val schema = Parser().parseSchema(json, JSONPointer("/properties/stock"), URI("http://pwall.net/test"))
        schema.validate(JSON.parse("""{"warehouse":1,"retail":2}""")) shouldBe true
    }

    @Test fun `should parse schema with description in external file`() {
        val parser = Parser()
        parser.options.allowDescriptionRef = true
        val schema = parser.parseFile("src/test/resources/test-description-ref.schema.yaml")
        schema.shouldBeType<JSONSchema.General>()
        val description = schema.description ?: fail("Description is null")
        description.startsWith("This is an example ") shouldBe true
    }

    @Test fun `should parse a schema from a string`() {
        val string = """{"enum":[1,2,4]}"""
        val schema = Parser().parse(string)
        schema.shouldBeType<JSONSchema.General>()
        schema.children.size shouldBe 1
        val child = schema.children[0]
        child.shouldBeType<EnumValidator>()
    }

    @Test fun `should parse a schema from a string with a URI`() {
        val string = """{"enum":[1,2,4]}"""
        val uri = URI.create("http://test.com/test")
        val schema = JSONSchema.parse(string, uri)
        schema.uri shouldBe uri
    }

    @Test fun `should cache parsed object in JSONReader`() {
        val parser = Parser()
        val file = File("src/test/resources/example.json")
        val object1 = parser.jsonReader.readJSON(file)
        val object2 = parser.jsonReader.readJSON(file)
        object2 shouldBeSameInstance object1
    }

    @Test fun `should parse schema from an opaque URI providing JSON without content type`() {
        val file = File("src/test/resources/example.schema.json")
        val uri = URI.create("opaque:example-schema-json")
        val parser = Parser()
        parser.setExtendedResolver { InputDetails(file.reader()) }
        val schema = parser.parse(uri)
        schema.shouldBeType<JSONSchema.General>()
        schema.title shouldBe "Product"
    }

    @Test fun `should parse schema from an opaque URI providing JSON with content type`() {
        val file = File("src/test/resources/example.schema.json")
        val uri = URI.create("opaque:example-schema-json")
        val parser = Parser()
        parser.setExtendedResolver { InputDetails(file.reader(), "application/json") }
        val schema = parser.parse(uri)
        schema.shouldBeType<JSONSchema.General>()
        schema.title shouldBe "Product"
    }

    @Test fun `should parse schema from an opaque URI providing YAML with content type`() {
        val file = File("src/test/resources/example.schema.yaml")
        val uri = URI.create("opaque:example-schema-yaml")
        val parser = Parser()
        parser.setExtendedResolver { InputDetails(file.reader(), "application/yaml") }
        val schema = parser.parse(uri)
        schema.shouldBeType<JSONSchema.General>()
        schema.title shouldBe "Product"
    }

    @Test fun `should parse schema from a jar URI providing JSON`() {
        val uri = URI.create("jar:file:src/test/resources/jar/example.jar!/example.schema.json")
        val schema = Parser().parse(uri)
        schema.shouldBeType<JSONSchema.General>()
        schema.title shouldBe "Product"
    }

    @Test fun `should parse schema from a jar URI providing YAML`() {
        val uri = URI.create("jar:file:src/test/resources/jar/example.jar!/example.schema.yaml")
        val schema = Parser().parse(uri)
        schema.shouldBeType<JSONSchema.General>()
        schema.title shouldBe "Product"
    }

    @Test fun `should read schema using HTTP`() {
        val parser = Parser()
        parser.setExtendedResolver(parser.defaultExtendedResolver)
        val schema = parser.parse(URI("http://kjson.io/json/http/testhttp1.json"))
        schema.shouldBeType<JSONSchema.General>()
        schema.children.size shouldBe 2
        with(schema.children[0]) {
            shouldBeType<TypeValidator>()
            types shouldBe listOf(JSONSchema.Type.OBJECT)
        }
        with(schema.children[1]) {
            shouldBeType<PropertiesSchema>()
            properties.size shouldBe 1
            with(properties[0]) {
                first shouldBe "xxx"
                with(second) {
                    shouldBeType<JSONSchema.General>()
                    children.size shouldBe 1
                    with(children[0]) {
                        shouldBeType<RefSchema>()
                        with(target) {
                            shouldBeType<JSONSchema.General>()
                            children.size shouldBe 3
                            with(children[0]) {
                                shouldBeType<TypeValidator>()
                                types shouldBe listOf(JSONSchema.Type.OBJECT)
                            }
                            with(children[1]) {
                                shouldBeType<PropertiesSchema>()
                                properties.size shouldBe 1
                                with(properties[0]) {
                                    first shouldBe "aaa"
                                    with(second) {
                                        shouldBeType<JSONSchema.General>()
                                        children.size shouldBe 1
                                        with(children[0]) {
                                            shouldBeType<TypeValidator>()
                                            types shouldBe listOf(JSONSchema.Type.INTEGER)
                                        }
                                    }
                                }
                            }
                            with(children[2]) {
                                shouldBeType<RequiredSchema>()
                                properties shouldBe listOf("aaa")
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
        bigDecimal1.isPositive() shouldBe true
        val bigDecimal2 = BigDecimal.ZERO
        bigDecimal2.isPositive() shouldBe false
        val bigDecimal3 = BigDecimal("-999")
        bigDecimal3.isPositive() shouldBe false
        val bigInteger1 = BigInteger.ONE
        bigInteger1.isPositive() shouldBe true
        val bigInteger2 = BigInteger.ZERO
        bigInteger2.isPositive() shouldBe false
        val bigInteger3 = BigInteger("-9")
        bigInteger3.isPositive() shouldBe false
        val double1 = 0.1
        double1.isPositive() shouldBe true
        val double2 = 0.0
        double2.isPositive() shouldBe false
        val double3 = -9.0
        double3.isPositive() shouldBe false
        val float1 = 0.1F
        (float1.isPositive()) shouldBe true
        val float2 = 0.0F
        (float2.isPositive()) shouldBe false
        val float3 = -10.0F
        (float3.isPositive()) shouldBe false
        val long1 = 1L
        long1.isPositive() shouldBe true
        val long2 = 0L
        long2.isPositive() shouldBe false
        val long3 = -8L
        long3.isPositive() shouldBe false
        val int1 = 1
        int1.isPositive() shouldBe true
        val int2 = 0
        int2.isPositive() shouldBe false
        val int3 = -5
        int3.isPositive() shouldBe false
    }

    @Test fun `should add authorization filter`() {
        val parser = Parser()
        parser.connectionFilters.size shouldBe 0
        parser.addConnectionFilter(
            ResourceLoader.AuthorizationFilter(Wildcard("*.example.com"), "Authorization", "TEST")
        )
        parser.connectionFilters.size shouldBe 1
        with(parser.connectionFilters[0]) {
            shouldBeType<ResourceLoader.AuthorizationFilter>()
            // this test is just rudimentary; the functionality is tested in the resource-loader project
        }
    }

    @Test fun `should add authorization filter using convenience function`() {
        val parser = Parser()
        parser.connectionFilters.size shouldBe 0
        parser.addAuthorizationFilter("*.example.com", "Authorization", "TEST")
        parser.connectionFilters.size shouldBe 1
        with(parser.connectionFilters[0]) {
            shouldBeType<ResourceLoader.AuthorizationFilter>()
            // this test is just rudimentary; the functionality is tested in the resource-loader project
        }
    }

    @Test fun `should add host-and-port-based redirection filter`() {
        val parser = Parser()
        parser.connectionFilters.size shouldBe 0
        parser.addRedirectionFilter(
            fromHost = "example.com",
            fromPort = -1,
            toHost = "localhost",
            toPort = 8080,
        )
        parser.connectionFilters.size shouldBe 1
        with(parser.connectionFilters[0]) {
            shouldBeType<ResourceLoader.RedirectionFilter>()
            // this test is just rudimentary; the functionality is tested in the resource-loader project
        }
    }

    @Test fun `should add prefix-based redirection filter`() {
        val parser = Parser()
        parser.connectionFilters.size shouldBe 0
        parser.addRedirectionFilter(
            fromPrefix = "https://example.com/schema",
            toPrefix = "http://localhost:8080/schema/",
        )
        parser.connectionFilters.size shouldBe 1
        with(parser.connectionFilters[0]) {
            shouldBeType<ResourceLoader.PrefixRedirectionFilter>()
            // this test is just rudimentary; the functionality is tested in the resource-loader project
        }
    }

}
