/*
 * @(#) JSONSchemaOneOfTest.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2021 Peter Wall
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

import io.kjson.JSON

import net.pwall.json.schema.parser.Parser

class JSONSchemaOneOfTest {

    @Test fun `should validate schema with oneOf`() {
        val parser = Parser { uri ->
            if (uri.scheme == "https" && uri.host == "pwall.net")
                JSONSchemaOneOfTest::class.java.getResourceAsStream(uri.path)
            else
                throw IllegalArgumentException("Can't locate $uri")
        }
        val filename = "src/test/resources/test-oneof.schema.json"
        val schema = parser.parseFile(filename)
        expect(true) { schema.validate(JSON.parse(domestic)) }
        expect(true) { schema.validate(JSON.parse(international)) }
    }

    companion object {

        const val domestic = """
{
  "id": "bd6b289a-cda9-11eb-9b1b-abf2b9f89e1d",
  "name": "Local Company",
  "customerType": "domestic",
  "domesticAddress": {
    "line1": "27 Sunshine Rd",
    "line2": "Sunshine",
    "state": "VIC",
    "postcode": "3333"
  }
}
"""

        const val international = """
{
  "id": "cf3f5556-cdad-11eb-ae01-4b733757ef12",
  "name": "Overseas Company",
  "customerType": "international",
  "internationalAddress": {
    "line1": "27 Sunshine Rd",
    "line2": "Sunshine",
    "country": "USA"
  }
}
"""

    }

}
