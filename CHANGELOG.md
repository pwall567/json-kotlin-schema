# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [0.37] - 2022-10-16
### Changed
- `pom.xml`: bumped dependency versions

## [0.36] - 2022-06-30
### Changed
- `pom.xml`: bumped dependency versions

## [0.35] - 2022-06-13
### Changed
- `pom.xml`: bumped dependency versions

## [0.34] - 2022-04-22
### Changed
- `JSONReader`: added cache check to `preLoad()` for `Path`

## [0.33] - 2022-04-20
### Changed
- `Parser`: improved error reporting on invalid `pattern`
- `pom.xml`: bumped dependency version

## [0.32] - 2022-02-20
### Changed
- `Parser`, `JSONReader`: added extended resolver mechanism (helps with http/s)
### Added
- `InoutDetails`: new

## [0.31] - 2021-12-11
### Changed
- `AdditionalItemsSchema`: fixed recursion bug in `hashCode()` and `equals()`
- `AdditionalPropertiesSchema`: fixed recursion bug in `hashCode()` and `equals()`
- `Parser`: made `jsonReader` publicly-accessible

## [0.30] - 2021-11-07
### Changed
- `pom.xml`: updated to Kotlin 1.5.20

## [0.29] - 2021-09-29
### Changed
- `pom.xml`: bumped dependency version

## [0.28] - 2021-09-21
### Changed
- `pom.xml`: bumped dependency version

## [0.27] - 2021-09-21
### Changed
- `pom.xml`: bumped dependency version

## [0.26] - 2021-09-21
### Changed
- `pom.xml`: bumped dependency version

## [0.25] - 2021-09-21
### Changed
- `pom.xml`: bumped dependency version

## [0.24] - 2021-09-16
### Changed
- `pom.xml`: bumped dependency versions

## [0.23] - 2021-07-28
### Changed
- `JSONSchema`, `JSONReader`, `Parser`: Updated string reading functions to take URI; added tests

## [0.22] - 2021-07-15
### Changed
- `JSONSchema`, `JSONReader`, `Parser`: Added functions to read schema from string

## [0.21] - 2021-06-21
### Changed
- `pom.xml`: updated dependency versions
- `FormatValidator`: Added regex format validation

## [0.20] - 2021-05-20
### Changed
- `pom.xml`: updated dependency version
- tests: switched test suite tests to use draft-07 files

## [0.19] - 2021-04-20
### Changed
- `Parser`: add identifier for draft 2020-12
- `NumberValidator`, `Parser`: fixed bugs in floating point
- `TypeValidator`: fixed interpretation of integer
- `AdditionalPropertiesSchema`, `PatternPropertiesSchema`: fixed bug in Regex handling
- `Parser`: added `contains`, `minContains` and `maxContains`
- `Parser`: added `minProperties` and `maxProperties`
- `AdditionalItemsSchema`: fixed bug
- `NumberValidator`: fixed bug in `multipleOf`
- `Parser`: added `propertyNames`
- `pom.xml`: updated dependency (with consequent changes)
### Added
- `ContainsValidator`: new
- `PropertiesValidator`: new
- `PropertyNamesSchema`: new

## [0.18.1] - 2021-04-06
### Changed
- `PatternValidator`: bug fix - wrong function used
- `Parser`, `JSONReader`: minor optimisation - use of $id

## [0.18] - 2021-02-26
### Changed
- `Parser`: adding handling of `uniqueItems`
### Added
- `UniqueItemsValidator`: new
- `pom.xml`: updated dependency version

## [0.17] - 2021-01-26
### Changed
- `FormatValidator`: added `int32` and `int64`
- `FormatValidator`: allow multiple delegating validators
- `FormatValidator`: bug fix
- `pom.xml`: updated dependency versions

## [0.16] - 2021-01-17
### Changed
- `FormatValidator`: changed handling of format duration, added json-pointer and relative-json-pointer

## [0.15] - 2021-01-05
### Changed
- `Parser`: changed handling of nonstandard formats to better reflect spec.
- `Parser`: added handling of $ref in description (controlled by option)

## [0.14] - 2021-01-04
### Changed
- `JSONReader`: minor improvements to usage of `Path`
- `Parser`: added handling of nonstandard formats

## [0.13] - 2020-12-02
### Changed
- `JSONReader`: allow ".yml" as alternative to ".yaml" file extension
- `JSONReader`: added ability to specify files using `Path`
- `README.md`: added badges, expanded documentation
- `pom.xml`: updated dependency versions

## [0.12] - 2020-10-27
### Added
- `Parser`: minor change to handling of extension mapping

## [0.11] - 2020-10-25
### Added
- `ExtensionSchema`: allows "x-something" extension data to be stored in schema

## [0.10.1] - 2020-10-19
### Changed
- `pom.xml`: bumped dependency version
- `README.md`: minor additions

## [0.10] - 2020-10-13
### Changed
- `Parser`: added ability to add custom validations
### Added
- `DelegatingValidator`: new

## [0.9.1] - 2020-10-12
### Changed
- `pom.xml`: updated version of `yaml-simple`
- `Parser`: make `parseSchema()` public

## [0.9] - 2020-10-07
### Added
- tests against meta-schema
- allow the use of YAML to define schema
### Changed
- `pom.xml`: switched to `json-pointer`

## [0.8] - 2020-09-17
### Changed
- `pom.xml`: update to Kotlin 1.4.0

## [0.7] - 2020-09-13
### Added
- `items`: extended form with array of schema
- `additionalItems`, `additionalProperties`, `patternProperties`

## [0.6] - 2020-08-23
### Added
- `minItems`, `maxItems`
- `format` types: `uri`, `uri-reference`, `ipv4`, `ipv6`

## [0.5] - 2020-08-18
### Added
- `if` / `then` / `else` schema constructs
### Changed
- simplified interface to `validate()`
### Removed
- code generation classes - moved to separate project `json-kotlin-schema-codegen`

## [0.4] - 2020-08-16
### Changed
- Multiple changes, including separation of `validate`, `validateBasic`, `validateDetailed` and their respective
output classes

## [0.3.7] - 2020-08-10
### Changed
- Fix bug in handling of identically-named nested classes in code generation

## [0.3.6] - 2020-08-10
### Changed
- Allow for identically-named nested classes in code generation

## [0.3.5] - 2020-08-09
### Changed
- Fix bug in handling of nested classes in code generation

## [0.3.4] - 2020-08-09
### Changed
- change handling of schema id uri

## [0.3.4] - 2020-08-09
### Changed
- Change handling of schema id URI

## [0.3.3] - 2020-08-09
### Changed
- Sanitise class names on code generation

## [0.3.2] - 2020-08-09
### Changed
- Fix non-alphanumeric directory names on code generation

## [0.3.1] - 2020-08-09
### Changed
- allow specification of custom parser during code generation

## [0.3] - 2020-08-09
### Added
- added initial version of code generation

## [0.2] - 2020-07-26
### Changed
- added copyright notice, removed dependency on `json-kotlin`

## [0.1] - 2020-07-23
### Added
- all files: initial version (work in progress)
