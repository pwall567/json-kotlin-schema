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

import net.pwall.json.parseJSON
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.parser.Parser

class TestSuiteTests {

    @Test fun `should run test suite tests`() {
        var totalPassed = 0
        var totalFailed = 0
        var totalSkipped = 0
        val parser = Parser { uri ->
            if (uri.scheme == "http" && uri.host == "localhost" && uri.port == 1234)
                File("$testSuiteBase/remotes/${uri.path}").inputStream()
            else
                fail("Can't resolve URI $uri")
        }
        val baseDirectory = File("$testSuiteBase/tests/draft2019-09")
        baseDirectory.listFiles()?.forEach { file ->
            if (file.isFile) {
                println("***File ${file.name}")
                val groups = file.readText().parseJSON<List<TestGroup>>() ?: fail("File contains null")
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
        expect(0) { totalFailed }
    }

    companion object {

        const val testSuiteBase = "src/test/resources/test-suite"

        val skipFiles = listOf(
                "anchor.json",
                "content.json",
                "dependentRequired.json",
                "dependentSchemas.json",
                "unevaluatedItems.json",
                "unevaluatedProperties.json",
                "recursiveRef.json",
                "format.json" // doesn't do anything - everything is valid!
        )

        val skipTests = listOf(
                "refRemote.json" to "base URI change - change folder",
                "refRemote.json" to "base URI change - change folder in subschema",
                "id.json" to "Invalid use of fragments in location-independent \$id",
                "id.json" to "Valid use of empty fragments in location-independent \$id",
                "id.json" to "Unnormalized \$ids are allowed but discouraged",
                "id.json" to "\$id inside an enum is not a real identifier",
                "items.json" to "items and subitems",
                "defs.json" to "validate definition against metaschema",
                "ref.json" to "root pointer ref",
                "ref.json" to "relative pointer ref to object",
                "ref.json" to "relative pointer ref to array",
                "ref.json" to "escaped pointer ref",
                "ref.json" to "nested refs",
                "ref.json" to "ref applies alongside sibling keywords",
                "ref.json" to "remote ref, containing refs itself",
                "ref.json" to "property named \$ref, containing an actual \$ref",
                "ref.json" to "\$ref to boolean schema true",
                "ref.json" to "\$ref to boolean schema false",
                "ref.json" to "Recursive references between schemas",
                "ref.json" to "refs with quote",
                "ref.json" to "ref creates new scope when adjacent to keywords",
                "infinite-loop-detection.json" to "evaluating the same schema location against the same data location twice is not a sign of an infinite loop",
                "unevaluatedProperties.json" to "unevaluatedProperties with \$ref",
                "unevaluatedItems.json" to "unevaluatedItems with \$ref",
                "recursiveRef.json" to "multiple dynamic paths to the \$recursiveRef keyword"
        )

    }

}
