# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

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
