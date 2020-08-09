package net.pwall.json.schema.codegen

import net.pwall.json.JSONObject
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.parser.Parser
import java.net.URI
import kotlin.test.Test
import kotlin.test.expect

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
