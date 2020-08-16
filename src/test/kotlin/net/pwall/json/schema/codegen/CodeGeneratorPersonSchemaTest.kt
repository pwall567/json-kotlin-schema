/*
 * @(#) CodeGeneratorPersonSchemaTest.kt
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

class CodeGeneratorPersonSchemaTest {

    @Test fun `should output simple data class`() {
        val input = File("src/test/resources/simple")
        val codeGenerator = CodeGenerator()
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver =
                CodeGeneratorTestUtil.outputCapture("dummy", emptyList(), "TestPerson", "kt", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expected1) { stringWriter.toString() }
    }

    @Test fun `should output simple data class to Java`() {
        val input = File("src/test/resources/simple")
        val codeGenerator = CodeGenerator(templates = "java", suffix = "java")
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver =
                CodeGeneratorTestUtil.outputCapture("dummy", emptyList(), "TestPerson", "java", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expectedJava) { stringWriter.toString() }
    }

    @Test fun `should output simple data class to TypeScript`() {
        val input = File("src/test/resources/simple")
        val codeGenerator = CodeGenerator(templates = "typescript", suffix = "ts")
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver =
                CodeGeneratorTestUtil.outputCapture("dummy", emptyList(), "TestPerson", "ts", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expectedTypeScript) { stringWriter.toString() }
    }

    companion object {

        const val expected1 =
"""package com.example

/**
 * A test class.
 */
data class TestPerson(
        val name: String,
        val nickname: String? = null,
        val age: Int
) {

    init {
        require(age <= 120) { "age > maximum 120 - ${'$'}age" }
        require(age >= 0) { "age < minimum 0 - ${'$'}age" }
    }

}
"""

        const val expectedJava =
"""package com.example;

/**
 * A test class.
 */
public class TestPerson {

    private final String name;
    private final String nickname;
    private final int age;

    public TestPerson(
            String name,
            String nickname,
            int age
    ) {
        if (name == null)
            throw new IllegalArgumentException("Must not be null - name");
        this.name = name;
        this.nickname = nickname;
        if (age > 120)
            throw new IllegalArgumentException("age > maximum 120 - " + age);
        if (age < 0)
            throw new IllegalArgumentException("age < minimum 0 - " + age);
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof TestPerson))
            return false;
        TestPerson typedOther = (TestPerson)other;
        if (!name.equals(typedOther.name))
            return false;
        if (nickname == null && typedOther.nickname != null ||
                nickname != null && !nickname.equals(typedOther.nickname))
            return false;
        if (age != typedOther.age)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash ^= name.hashCode();
        hash ^= nickname != null ? nickname.hashCode() : 0;
        hash ^= age;
        return hash;
    }

}
"""

        const val expectedTypeScript =
"""/**
 * A test class.
 */
interface TestPerson {
    name: string,
    nickname?: string,
    age: number
}
"""

    }

}
