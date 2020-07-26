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

import net.pwall.json.JSONArray
import net.pwall.json.JSONObject
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.Output

fun Output.toJSON(): JSONObject = JSONObject().also { result ->
        result.putValue("valid", valid)
        if (this is BasicOutput) {
            result.putValue("keywordLocation", keywordLocation)
            absoluteKeywordLocation?.let { result.putValue("absoluteKeywordLocation", it) }
            result.putValue("instanceLocation", instanceLocation)
            error?.let { result.putValue("error", it) }
            annotation?.let { result.putValue("annotation", it) }
            errors?.let {
                result.put("errors", JSONArray(it.map { e -> e.toJSON() }))
            }
            annotations?.let {
                result.put("annotations", JSONArray(it.map { e -> e.toJSON() }))
            }
        }
    }
