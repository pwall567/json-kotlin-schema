/*
 * @(#) CodeGeneratorExampleTest.kt
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

import java.io.File
import java.io.StringWriter

class CodeGeneratorExampleTest {

    @Test fun `should output example data class`() {
        val input = File("src/test/resources/example.schema.json")
        val codeGenerator = CodeGenerator()
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver =
                CodeGeneratorTestUtil.outputCapture("dummy", emptyList(), "Test", "kt", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expectedExample) { stringWriter.toString() }
    }

    companion object {

        const val expectedExample =
"""package com.example

import java.math.BigDecimal

data class Test(
        /** Product identifier */
        val id: BigDecimal,
        /** Name of the product */
        val name: String,
        val price: BigDecimal,
        val tags: List<String>? = null,
        val stock: Stock? = null
) {

    data class Stock(
            val warehouse: BigDecimal? = null,
            val retail: BigDecimal? = null
    )

}
"""

    }

}
