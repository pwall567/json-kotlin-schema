/*
 * @(#) JSONReaderTest.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2024 Peter Wall
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

import java.net.URI

import io.kstuff.test.shouldBe

import net.pwall.json.schema.parser.JSONReader.Companion.extendedPath

class JSONReaderTest {

    @Test fun `should extract path from non-standard URI`() {
        URI("file:/dummy.path").extendedPath() shouldBe "/dummy.path"
        URI("file:/dummy.path#frag1").extendedPath() shouldBe "/dummy.path"
        URI("https://example.com/remote.path").extendedPath() shouldBe "/remote.path"
        URI("https://example.com/remote.path?x=1").extendedPath() shouldBe "/remote.path"
        URI("https://example.com/remote.path?x=1#frag1").extendedPath() shouldBe "/remote.path"
        URI("jar:file:/path.to.jar!/jar.path").extendedPath() shouldBe "/jar.path"
        URI("jar:file:/path.to.jar!/jar.path#frag2").extendedPath() shouldBe "/jar.path"
        URI("urn:uuid:adc71b0e-ab57-11ee-b333-f346650819c3").extendedPath() shouldBe "UNKNOWN"
    }

}
