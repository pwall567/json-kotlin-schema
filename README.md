# json-kotlin-schema

Kotlin implementation of JSON Schema

## Quick Start

Given the following schema file (Taken from the [Wikipedia article on JSON](https://en.wikipedia.org/wiki/JSON)):
```json
{
  "$schema": "http://json-schema.org/draft/2019-09/schema",
  "$id": "http://pwall.net/test",
  "title": "Product",
  "type": "object",
  "required": ["id", "name", "price"],
  "properties": {
    "id": {
      "type": "number",
      "description": "Product identifier"
    },
    "name": {
      "type": "string",
      "description": "Name of the product"
    },
    "price": {
      "type": "number",
      "minimum": 0
    },
    "tags": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "stock": {
      "type": "object",
      "properties": {
        "warehouse": {
          "type": "number"
        },
        "retail": {
          "type": "number"
        }
      }
    }
  }
}
```
and this JSON (from the same article):
```json
{
  "id": 1,
  "name": "Foo",
  "price": 123,
  "tags": [
    "Bar",
    "Eek"
  ],
  "stock": {
    "warehouse": 300,
    "retail": 20
  }
}
```
the following code will validate that the JSON matches the schema:
```kotlin
    val schema = JSONSchema.parse("/path/to/example.schema.json")
    val json = File("/path/to/example.json").readText()
    require(schema.validate(json))
```

To see the detail of any errors found during validation:
```kotlin
    val schema = JSONSchema.parse("/path/to/example.schema.json")
    val json = File("/path/to/example.json").readText()
    val output = schema.validateBasic(json)
    output?.errors.forEach {
        println("${it.error} - ${it.instanceLocation}")
    }
```

## Implemented Subset

This implementation does not implement the full JSON Schema specification.
The currently implemented subset includes:

### Core

- `$schema`
- `$id`
- `$defs`
- `$comment`
- `title`
- `description`
- `examples`

### Structure

- `properties`
- `patternProperties`
- `additionalProperties`
- `items`
- `additionalItems`
- `allOf`
- `anyOf`
- `oneOf`
- `if`
- `then`
- `else`
- `default`

### Validation

- `type` (`null`, `boolean`, `object`, `array`, `number`, `string`, `integer`)
- `format` (`date-time`, `date`, `time`, `duration`, `email`, `hostname`, `uri`, `uri-reference`, `uuid`, `ipv4`,
`ipv6`)
- `enum`
- `const`
- `multipleOf`
- `maximum`
- `exclusiveMaximum`
- `minimum`
- `exclusiveMinimum`
- `minItems`
- `maxItems`
- `maxLength`
- `minLength`
- `pattern`
- `required`

## Not Currently Implemented

- `$recursiveRef`
- `$recursiveAnchor`
- `$anchor`
- `$vocabulary`
- `unevaluatedProperties`
- `unevaluatedItems`
- `dependentSchemas`
- `dependentRequired`
- `contains`
- `uniqueItems`
- `maxContains`
- `minContains`
- `contentEncoding`
- `contentMediaType`
- `contentSchema`
- `deprecated`
- `readOnly`
- `writeOnly`
- `format` (`idn-email`, `idn-hostname`, `iri`, `iri-reference`, `url-template`, `json-pointer`,
`relative-json-pointer`)

More documentation to follow.

## Dependency Specification

The latest version of the library is 0.7, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.json</groupId>
      <artifactId>json-kotlin-schema</artifactId>
      <version>0.7</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'net.pwall.json:json-kotlin-schema:0.7'
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.json:json-kotlin-schema:0.7")
```

Peter Wall

2020-09-13
