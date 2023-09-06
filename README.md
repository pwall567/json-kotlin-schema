![Stop the war in Ukraine](https://pwall.net/ukraine1.png)

# json-kotlin-schema

[![Build Status](https://travis-ci.com/pwall567/json-kotlin-schema.svg?branch=main)](https://travis-ci.com/github/pwall567/json-kotlin-schema)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.8.22&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.8.22)
[![Maven Central](https://img.shields.io/maven-central/v/net.pwall.json/json-kotlin-schema?label=Maven%20Central)](https://search.maven.org/search?q=g:%22net.pwall.json%22%20AND%20a:%22json-kotlin-schema%22)

Kotlin implementation of JSON Schema (Draft-07)

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
    val schema = JSONSchema.parseFile("/path/to/example.schema.json")
    val json = File("/path/to/example.json").readText()
    require(schema.validate(json))
```

To see the detail of any errors found during validation:
```kotlin
    val schema = JSONSchema.parseFile("/path/to/example.schema.json")
    val json = File("/path/to/example.json").readText()
    val output = schema.validateBasic(json)
    output.errors?.forEach {
        println("${it.error} - ${it.instanceLocation}")
    }
```

The format of the error object produced by the `validateBasic()` function closely follows the
[Basic output](https://json-schema.org/draft/2019-09/json-schema-core.html#rfc.section.10.4.2) specification.

It is also possible to read a schema from a string in memory:
```kotlin
    val str = File("/path/to/example.schema.json").readText()
    val schema = JSONSchema.parse(str)
```
An optional second parameter on the `parse()` function takes a URI, which will be used to construct the location in
error objects.

## YAML

While it may seem counter-intuitive to use a language other than JSON to express JSON Schema, YAML is a lot easier to
work with, particularly when multi-line descriptions are required.
This library functions equally well with schema representations in JSON or YAML.

For example, the above schema looks like this in YAML:
```yaml
$schema: http://json-schema.org/draft/2019-09/schema
$id: http://pwall.net/test
title: Product
type: object
required:
- id
- name
- price
properties:
  id:
    type: number
    description: Product identifier
  name:
    type: string
    description: Name of the product
  price:
    type: number
    minimum: 0
  tags:
    type: array
    items:
      type: string
  stock:
    type: object
    properties:
      warehouse:
        type: number
      retail:
        type: number
```

To use this schema, simply specify a schema file with an extension of `.yaml` or `.yml` to the schema parser:
```kotlin
    val schema = JSONSchema.parseFile("/path/to/example.schema.yaml")
```

The YAML library used is [this one](https://github.com/pwall567/yaml-simple).
It is not a complete implementation of the YAML specification, but it should be more than adequate for the purpose of
specifying JSON Schema.

## References

At many points in a JSON Schema, the `$ref` construct allows a reference to schema information defined elsewhere.
The reference takes the form of a URL, which may be internal to the current schema document (reference starts with
a `#` character) or external - the reference points to a different document.

Internal references are resolved relative to the root of the schema document in which they appear, for example:
```json
{
  "$ref": "#/$defs/Account"
}
```
This points to a schema named `Account` in the `$defs` section of the current schema document.

An external reference may be relative (in which case the URL will be resolved relative to the location of the document
in which the reference appears) or absolute.
The external reference may include a fragment (a JSON Pointer starting with `#`); if it does not the reference is taken
as pointing to the root of the document.
For example:
```json
{
  "$ref": "common.schema.json#/$defs/Address"
}
```
This will look for a sibling (URL or file) to the current document and attempt to locate the `Address` schema in the
`$defs` section of that document.

## Implemented Subset

This implementation does not implement the full JSON Schema specification.
It covers much of [Draft 07](https://json-schema.org/specification-links.html#draft-7) and a few features from
[Draft 2019-09](https://json-schema.org/specification-links.html#draft-2019-09).

The currently implemented subset includes:

### Core

- `$schema`
- `$id`
- `$ref` (with some reservations)
- `$defs`
- `$comment`
- `title`
- `description`
- `examples`

### Structure

- `properties`
- `patternProperties`
- `additionalProperties`
- `propertyNames`
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
`ipv6`, `json-pointer`, `relative-json-pointer`, `regex`)
- `enum`
- `const`
- `multipleOf`
- `maximum`
- `exclusiveMaximum`
- `minimum`
- `exclusiveMinimum`
- `minProperties`
- `maxProperties`
- `minItems`
- `maxItems`
- `uniqueItems`
- `maxLength`
- `minLength`
- `pattern`
- `required`
- `contains`
- `maxContains`
- `minContains`

## Not Currently Implemented

- `$recursiveRef`
- `$recursiveAnchor`
- `$anchor`
- `$vocabulary`
- `unevaluatedProperties`
- `unevaluatedItems`
- `dependentcies`
- `dependentSchemas`
- `dependentRequired`
- `contentEncoding`
- `contentMediaType`
- `contentSchema`
- `deprecated`
- `readOnly`
- `writeOnly`
- `format` (`idn-email`, `idn-hostname`, `iri`, `iri-reference`, `url-template`)

More documentation to follow.

## Dependency Specification

The latest version of the library is 0.41, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.json</groupId>
      <artifactId>json-kotlin-schema</artifactId>
      <version>0.41</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'net.pwall.json:json-kotlin-schema:0.41'
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.json:json-kotlin-schema:0.41")
```

Peter Wall

2023-09-06
