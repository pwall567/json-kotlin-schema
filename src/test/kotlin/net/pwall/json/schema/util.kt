/*
 * @(#) util.kt
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

package net.pwall.json.schema

import io.kjson.JSONArray
import io.kjson.JSONObject

import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.DetailedOutput
import net.pwall.json.schema.output.Output

fun Output.toJSON(): JSONObject = JSONObject.build {
    add("valid", valid)
    if (this@toJSON is BasicOutput) {
        errors?.let {
            add("errors", JSONArray.from(it.map { e -> e.toJSON() }))
        }
    }
    if (this@toJSON is DetailedOutput) {
        add("keywordLocation", keywordLocation)
        absoluteKeywordLocation?.let { add("absoluteKeywordLocation", it) }
        add("instanceLocation", instanceLocation)
        error?.let { add("error", it) }
        annotation?.let { add("annotation", it) }
        errors?.let {
            add("errors", JSONArray.from(it.map { e -> e.toJSON() }))
        }
        annotations?.let {
            add("annotations", JSONArray.from(it.map { e -> e.toJSON() }))
        }
    }
}

fun BasicErrorEntry.toJSON(): JSONObject = JSONObject.build {
    add("keywordLocation", keywordLocation)
    absoluteKeywordLocation?.let { add("absoluteKeywordLocation", it) }
    add("instanceLocation", instanceLocation)
    add("error", error)
}
