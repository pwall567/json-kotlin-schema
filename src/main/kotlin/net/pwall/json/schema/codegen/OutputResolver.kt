package net.pwall.json.schema.codegen

import java.io.Writer

typealias OutputResolver = (String, List<String>, String, String) -> Writer
