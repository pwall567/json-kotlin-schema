/*
 * @(#) JSONReader.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020, 2022 Peter Wall
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

package net.pwall.json.schema.parser

import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

import net.pwall.json.JSON
import net.pwall.json.JSONValue
import net.pwall.json.schema.JSONSchemaException
import net.pwall.json.schema.parser.Parser.Companion.dropFragment
import net.pwall.yaml.YAMLSimple

class JSONReader(val uriResolver: (URI) -> InputStream?) {

    var extendedResolver: ((URI) -> InputDetails?)? = null

    private val jsonCache: MutableMap<URI, JSONValue> = mutableMapOf()

    fun preLoad(filename: String) {
        preLoad(File(filename))
    }

    fun preLoad(file: File) {
        when {
            file.isDirectory -> file.listFiles()?.forEach { if (!it.name.startsWith('.')) preLoad(it) }
            file.isFile -> {
                val uri = file.toURI()
                when {
                    jsonCache.containsKey(uri) -> {}
                    file.name.endsWith(".json", ignoreCase = true) -> {
                        JSON.parse(file)?.let {
                            jsonCache[uri] = it
                            it.cacheById()
                        }
                    }
                    looksLikeYAML(file.name) -> {
                        YAMLSimple.process(file).rootNode?.let {
                            jsonCache[uri] = it
                            it.cacheById()
                        }
                    }
                }
            }
        }
    }

    fun preLoad(path: Path) {
        when {
            Files.isDirectory(path) -> {
                Files.newDirectoryStream(path).use { dir ->
                    dir.forEach {
                        if (!it.fileName.toString().startsWith('.'))
                            preLoad(it)
                    }
                }
            }
            Files.isRegularFile(path) -> {
                val fileName = path.fileName?.toString() ?: throw JSONSchemaException("Path filename is null")
                val uri = path.toUri()
                when {
                    jsonCache.containsKey(uri) -> {}
                    fileName.endsWith(".json", ignoreCase = true) -> {
                        Files.newBufferedReader(path).use { reader ->
                            JSON.parse(reader)?.let {
                                jsonCache[path.toUri()] = it
                                it.cacheById()
                            }
                        }
                    }
                    looksLikeYAML(fileName) -> {
                        Files.newBufferedReader(path).use { reader ->
                            YAMLSimple.process(reader).rootNode?.let {
                                jsonCache[path.toUri()] = it
                                it.cacheById()
                            }
                        }
                    }
                }
            }
        }
    }

    fun readJSON(file: File): JSONValue {
        val uri = file.toURI()
        return jsonCache[uri] ?: try {
            when {
                looksLikeYAML(file.name) ->
                    YAMLSimple.process(file).rootNode ?: throw JSONSchemaException("Schema file is null - $file")
                else -> JSON.parse(file) ?: throw JSONSchemaException("Schema file is null - $file")
            }
        }
        catch (e: JSONSchemaException) {
            throw e
        }
        catch (e: Exception) {
            throw JSONSchemaException("Error reading schema file - $file", e)
        }.also {
            jsonCache[uri] = it
            it.cacheById()
        }
    }

    fun readJSON(string: String, uri: URI? = null): JSONValue {
        if (uri != null) {
            return jsonCache[uri] ?: JSON.parse(string)?.also {
                jsonCache[uri] = it
                it.cacheById()
            } ?: throw JSONSchemaException("Schema is null")
        }
        return JSON.parse(string) ?: throw JSONSchemaException("Schema is null")
    }

    fun readJSON(uri: URI): JSONValue {
        return jsonCache[uri] ?: try {
            readByResolver(uri)
        }
        catch (e: JSONSchemaException) {
            throw e
        }
        catch (e: Exception) {
            throw JSONSchemaException("Error reading schema file - $uri", e)
        }.also {
            jsonCache[uri] = it
            it.cacheById()
        }
    }

    private fun readByResolver(uri: URI): JSONValue {
        extendedResolver?.let { resolver ->
            val inputDetails = resolver(uri) ?: throw JSONSchemaException("Can't resolve name - $uri")
            return inputDetails.reader.use {
                when {
                    looksLikeYAML(uri.path, inputDetails.contentType) ->
                        YAMLSimple.process(it).rootNode ?: throw JSONSchemaException("Schema file is null - $uri")
                    else -> JSON.parse(it) ?: throw JSONSchemaException("Schema file is null - $uri")
                }
            }
        }
        val inputStream = uriResolver(uri) ?: throw JSONSchemaException("Can't resolve name - $uri")
        return inputStream.use {
            when {
                looksLikeYAML(uri.path) ->
                    YAMLSimple.process(it).rootNode ?: throw JSONSchemaException("Schema file is null - $uri")
                else -> JSON.parse(it) ?: throw JSONSchemaException("Schema file is null - $uri")
            }
        }
    }

    fun readJSON(path: Path): JSONValue {
        val uri = path.toUri()
        return jsonCache[uri] ?: try {
            val fileName = path.fileName?.toString() ?: throw JSONSchemaException("Path filename is null")
            Files.newBufferedReader(path).use { reader ->
                when {
                    looksLikeYAML(fileName) ->
                        YAMLSimple.process(reader).rootNode ?: throw JSONSchemaException("Schema file is null - $path")
                    else -> JSON.parse(reader) ?: throw JSONSchemaException("Schema file is null - $path")
                }
            }
        }
        catch (e: Exception) {
            throw JSONSchemaException("Error reading schema file - $path", e)
        }.also {
            jsonCache[uri] = it
            it.cacheById()
        }
    }

    private fun JSONValue.cacheById() {
        Parser.getIdOrNull(this)?.let {
            jsonCache[URI(it).dropFragment()] = this
        }
    }

    companion object {

        fun looksLikeYAML(path: String?, contentType: String? = null): Boolean {
            contentType?.let {
                if (it.contains("yaml", ignoreCase = true) || it.contains("yml", ignoreCase = true))
                    return true
                if (it.contains("json", ignoreCase = true))
                    return false
            }
            return path?.let {
                it.endsWith(".yaml", ignoreCase = true) || it.endsWith(".yml", ignoreCase = true)
            } ?: false // null path and contentType do not look like YAML
        }

    }

}
