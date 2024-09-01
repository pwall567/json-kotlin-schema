/*
 * @(#) FormatValidatorTest.kt
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

package net.pwall.json.schema.validation

import kotlin.test.Test
import kotlin.test.expect

import io.kjson.JSONBoolean
import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.JSONObject
import io.kjson.JSONString

class FormatValidatorTest {

    @Test fun `should validate int64 correctly`() {
        expect(true) { FormatValidator.Int64FormatChecker.check(JSONString("test")) }
        expect(true) { FormatValidator.Int64FormatChecker.check(JSONInt(12345)) }
        expect(true) { FormatValidator.Int64FormatChecker.check(JSONLong(12345)) }
        expect(true) { FormatValidator.Int64FormatChecker.check(JSONBoolean.TRUE) }
        expect(true) { FormatValidator.Int64FormatChecker.check(JSONObject.EMPTY) }
        expect(true) { FormatValidator.Int64FormatChecker.check(JSONDecimal("12345.00")) }
        expect(false) { FormatValidator.Int64FormatChecker.check(JSONDecimal("12345.50")) }
        expect(false) { FormatValidator.Int64FormatChecker.check(JSONDecimal("123456789123456789123456789")) }
    }

    @Test fun `should validate int32 correctly`() {
        expect(true) { FormatValidator.Int32FormatChecker.check(JSONString("test")) }
        expect(true) { FormatValidator.Int32FormatChecker.check(JSONInt(12345)) }
        expect(true) { FormatValidator.Int32FormatChecker.check(JSONLong(12345)) }
        expect(false) { FormatValidator.Int32FormatChecker.check(JSONLong(12345678912345)) }
        expect(true) { FormatValidator.Int32FormatChecker.check(JSONBoolean.TRUE) }
        expect(true) { FormatValidator.Int32FormatChecker.check(JSONObject.EMPTY) }
        expect(true) { FormatValidator.Int32FormatChecker.check(JSONDecimal("12345.00")) }
        expect(false) { FormatValidator.Int32FormatChecker.check(JSONDecimal("12345.50")) }
        expect(false) { FormatValidator.Int32FormatChecker.check(JSONDecimal("123456789123456789")) }
    }

    @Test fun `should validate date-time correctly`() {
        expect(true) { FormatValidator.DateTimeFormatChecker.check(JSONString("2021-01-23T22:29:54+11:00")) }
        expect(true) { FormatValidator.DateTimeFormatChecker.check(JSONString("2021-01-23t22:29:54.123+11:00")) }
        expect(true) { FormatValidator.DateTimeFormatChecker.check(JSONString("2021-01-23T11:29:54Z")) }
        expect(true) { FormatValidator.DateTimeFormatChecker.check(JSONString("2021-01-23T11:29:54z")) }
        expect(false) { FormatValidator.DateTimeFormatChecker.check(JSONString("2021-01-23")) }
        expect(false) { FormatValidator.DateTimeFormatChecker.check(JSONString("")) }
        expect(false) { FormatValidator.DateTimeFormatChecker.check(JSONString("wrong")) }
        expect(true) { FormatValidator.DateTimeFormatChecker.check(JSONInt(0)) }
        expect(true) { FormatValidator.DateTimeFormatChecker.check(JSONLong(-1)) }
        expect(true) { FormatValidator.DateTimeFormatChecker.check(JSONBoolean.TRUE) }
        expect(true) { FormatValidator.DateTimeFormatChecker.check(JSONObject.EMPTY) }
    }

    @Test fun `should validate date correctly`() {
        expect(true) { FormatValidator.DateFormatChecker.check(JSONString("2021-01-23")) }
        expect(false) { FormatValidator.DateFormatChecker.check(JSONString("2021-01-23T22:29:54+11:00")) }
        expect(false) { FormatValidator.DateFormatChecker.check(JSONString("")) }
        expect(false) { FormatValidator.DateFormatChecker.check(JSONString("wrong")) }
        expect(true) { FormatValidator.DateFormatChecker.check(JSONInt(0)) }
        expect(true) { FormatValidator.DateFormatChecker.check(JSONLong(-1)) }
        expect(true) { FormatValidator.DateFormatChecker.check(JSONBoolean.TRUE) }
        expect(true) { FormatValidator.DateFormatChecker.check(JSONObject.EMPTY) }
    }

}
