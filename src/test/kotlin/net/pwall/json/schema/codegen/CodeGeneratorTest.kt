package net.pwall.json.schema.codegen

import kotlin.test.Test
import kotlin.test.expect
import kotlin.test.fail

import java.io.File
import java.io.StringWriter

class CodeGeneratorTest {

    @Test fun `should output simple data class`() {
        val input = File("src/test/resources/simple")
        val codeGenerator = CodeGenerator()
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver = outputCapture("dummy", emptyList(), "TestPerson", "kt", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expected1) { stringWriter.toString() }
    }

    @Test fun `should output simple data class to Java`() {
        val input = File("src/test/resources/simple")
        val codeGenerator = CodeGenerator(templates = "java", suffix = "java")
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver = outputCapture("dummy", emptyList(), "TestPerson", "java", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expectedJava) { stringWriter.toString() }
    }

    @Test fun `should output simple data class to TypeScript`() {
        val input = File("src/test/resources/simple")
        val codeGenerator = CodeGenerator(templates = "typescript", suffix = "ts")
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver = outputCapture("dummy", emptyList(), "TestPerson", "ts", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expectedTypeScript) { stringWriter.toString() }
    }

    @Test fun `should process test directory and write to generated-sources`() {
        val input = File("src/test/resources/test1")
        val outputDirectory = "target/generated-test-sources/json-kotlin-schema/net/pwall/json/schema/test"
        val codeGenerator = CodeGenerator()
        codeGenerator.baseDirectoryName = outputDirectory
        codeGenerator.basePackageName = "net.pwall.json.schema.test"
        codeGenerator.generate(input)
        expect(expected2) { File("$outputDirectory/person/Person.kt").readText() }
    }

    @Test fun `should output test class to Java`() {
        val input = File("src/test/resources/test1")
        val codeGenerator = CodeGenerator(templates = "java", suffix = "java")
        codeGenerator.baseDirectoryName = "dummy1"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver = outputCapture("dummy1", listOf("person"), "Person", "java", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expected2Java) { stringWriter.toString() }
    }

    @Test fun `should use custom templates`() {
        val input = File("src/test/resources/test1")
        val codeGenerator = CodeGenerator()
        codeGenerator.setTemplateDirectory(File("src/test/resources/dummy-template"))
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver = outputCapture("dummy", listOf("person"), "Person", "kt", stringWriter)
        codeGenerator.basePackageName = "net.pwall.json.schema.test"
        codeGenerator.generate(input)
        expect("// Dummy\n") { stringWriter.toString() }
    }

    @Test fun `should output example data class`() {
        val input = File("src/test/resources/example.schema.json")
        val codeGenerator = CodeGenerator()
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver = outputCapture("dummy", emptyList(), "Test", "kt", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expectedExample) { stringWriter.toString() }
    }

    @Test fun `should output deeply nested class class`() {
        val input = File("src/test/resources/test-nested-object.schema.json")
        val codeGenerator = CodeGenerator()
        codeGenerator.baseDirectoryName = "dummy"
        val stringWriter = StringWriter()
        codeGenerator.outputResolver = outputCapture("dummy", emptyList(), "TestNestedObject", "kt", stringWriter)
        codeGenerator.basePackageName = "com.example"
        codeGenerator.generate(input)
        expect(expectedNested) { stringWriter.toString() }
    }

    companion object {

        private fun outputCapture(expectedBaseDirectory: String, expectedSubdirectories: List<String>,
                expectedClassName: String, expectedSuffix: String, stringWriter: StringWriter): OutputResolver =
                { baseDirectory, subDirectories, className, suffix ->
            if (baseDirectory == expectedBaseDirectory && subDirectories == expectedSubdirectories &&
                    className == expectedClassName && suffix == expectedSuffix)
                stringWriter
            else
                fail("Output resolver fail - $baseDirectory $subDirectories $className $suffix")
        }

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

        const val expectedExample =
"""package com.example

import java.math.BigDecimal

data class Test(
        /** Product identifier */
        val id: BigDecimal,
        /** Name of the product */
        val name: String,
        val price: BigDecimal,
        val tags: List<String>,
        val stock: Stock? = null
) {

    data class Stock(
            val warehouse: BigDecimal,
            val retail: BigDecimal
    )

}
"""

        const val expectedNested =
"""package com.example

data class TestNestedObject(
        val nested: Nested
) {

    data class Nested(
            val deeper: Deeper
    )

    data class Deeper(
            val deepest: String
    )

}
"""

        const val expected2 =
"""package net.pwall.json.schema.test.person

import java.util.UUID

/**
 * A class to represent a person
 */
data class Person(
        /** Id of the person */
        val id: UUID,
        /** Name of the person */
        val name: String
)
"""

        const val expected2Java =
"""package com.example.person;

import java.util.UUID;

/**
 * A class to represent a person
 */
public class Person {

    private final UUID id;
    private final String name;

    public Person(
            UUID id,
            String name
    ) {
        if (id == null)
            throw new IllegalArgumentException("Must not be null - id");
        this.id = id;
        if (name == null)
            throw new IllegalArgumentException("Must not be null - name");
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Person))
            return false;
        Person typedOther = (Person)other;
        if (!id.equals(typedOther.id))
            return false;
        if (!name.equals(typedOther.name))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash ^= id.hashCode();
        hash ^= name.hashCode();
        return hash;
    }

}
"""

    }

}
