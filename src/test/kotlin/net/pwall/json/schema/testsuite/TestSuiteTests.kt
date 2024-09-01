/*
 * @(#) TestSuiteTests.kt
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

package net.pwall.json.schema.testsuite

import kotlin.test.Test
import kotlin.test.expect
import kotlin.test.fail

import java.io.File

import io.kjson.parseJSON
import io.kjson.pointer.JSONPointer

import net.pwall.json.schema.parser.Parser

class TestSuiteTests {

    @Test fun `should run test suite tests`() {
        val parser = Parser { uri ->
            if ((uri.scheme == "http" || uri.scheme == "https") && uri.host == "localhost" && uri.port == 1234)
                File("$testSuiteBase/remotes/${uri.path}").inputStream()
            else
                uri.toURL().openStream()
        }
        testDirectory(File("$testSuiteBase/tests/draft7"), parser)
    }

    private fun testDirectory(baseDirectory: File, parser: Parser) {
        var totalPassed = 0
        var totalFailed = 0
        var totalSkipped = 0
        println()
        baseDirectory.listFiles()?.forEach { file ->
            if (file.isFile) {
                println("***File ${file.name}")
                val groups = file.readText().parseJSON<List<TestGroup>>()
                if (skipFiles.any { it == file.name }) {
                    println("FILE SKIPPED\n")
                    totalSkipped += groups.size
                }
                else {
                    var filePassed = 0
                    var fileFailed = 0
                    var fileSkipped = 0
                    groups.forEach { group ->
                        println("**Group ${group.description}")
                        if (skipTests.any { it.first == file.name && it.second == group.description }) {
                            println("SKIPPED")
                            fileSkipped++
                        }
                        else {
                            var groupPassed = 0
                            var groupFailed = 0
                            val parsedSchema = parser.parseSchema(group.schema, JSONPointer.root, null)
                            group.tests.forEach { test ->
                                val result = parsedSchema.validate(test.data)
                                if (result == test.valid)
                                    groupPassed++
                                else {
                                    groupFailed++
                                    println("*Test ${test.description} FAILED")
                                }
                            }
                            println("**Group totals - passed $groupPassed, failed $groupFailed")
                            filePassed += groupPassed
                            fileFailed += groupFailed
                        }
                    }
                    println("***File totals - passed $filePassed, failed $fileFailed, skipped $fileSkipped\n")
                    totalPassed += filePassed
                    totalFailed += fileFailed
                    totalSkipped += fileSkipped
                }
            }
        }
        println()
        println("Total passed: $totalPassed; failed: $totalFailed, skipped $totalSkipped")
        println()
        expect(0) { totalFailed }
    }

    companion object {

        const val testSuiteBase = "src/test/resources/test-suite"

        val skipFiles = listOf(
                "id.json",
                "ref.json",
                "definitions.json",
                "dependencies.json"
        )

        val skipTests = listOf(
                "refRemote.json" to "base URI change - change folder in subschema"
        )

    }

}
