package com.walley.app.data.csv

import java.math.BigDecimal

/**
 * Minimal RFC4180 CSV tokenizer: handles quoted fields (including embedded commas and newlines)
 * and `""` as an escaped quote. Blank lines are dropped. Not a general-purpose library — just enough
 * for the app's own CSV exports/imports. [delimiter] defaults to comma; some bank/brokerage exports
 * (e.g. BOSSA) use semicolons instead, since Polish locale uses comma as the decimal separator.
 */
fun parseCsvLines(text: String, delimiter: Char = ','): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var field = StringBuilder()
    var row = mutableListOf<String>()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes -> {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            }
            c == '"' -> inQuotes = true
            c == delimiter -> {
                row.add(field.toString())
                field = StringBuilder()
            }
            c == '\r' -> Unit
            c == '\n' -> {
                row.add(field.toString())
                rows.add(row)
                row = mutableListOf()
                field = StringBuilder()
            }
            else -> field.append(c)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        rows.add(row)
    }
    return rows.filter { line -> line.any { it.isNotBlank() } }
}

/**
 * Decodes CSV bytes as UTF-8, falling back to Windows-1250 (the default Polish Windows encoding,
 * used by exports like BOSSA's) if the UTF-8 decoding produced replacement characters — a sign the
 * bytes weren't actually UTF-8.
 */
fun decodeCsvBytes(bytes: ByteArray): String {
    val utf8 = bytes.toString(Charsets.UTF_8)
    return if (utf8.contains('�')) bytes.toString(charset("windows-1250")) else utf8
}

/**
 * Parses a Polish-locale decimal (comma as the separator, e.g. "138,82") into a [BigDecimal].
 * Also strips space thousands separators (plain or non-breaking, e.g. "14 200,00"), which some
 * exports include but brokerage reports like BOSSA/XTB never do.
 */
fun parsePolishDecimal(text: String): BigDecimal? =
    text.trim().replace(" ", "").replace(" ", "").replace(",", ".").toBigDecimalOrNull()
