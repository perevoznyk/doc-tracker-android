package com.example.doctracker.data

/** Minimal RFC-4180 CSV writer/parser. The parser auto-detects ',' or ';' (Excel in many locales uses ';'). */
object Csv {
    val HEADER = listOf("name", "category", "valid_from", "valid_until", "reminders")

    fun write(rows: List<List<String>>, delimiter: Char = ','): String =
        rows.joinToString("\r\n", postfix = "\r\n") { row ->
            row.joinToString(delimiter.toString()) { escape(it, delimiter) }
        }

    private fun escape(s: String, d: Char): String =
        if (s.any { it == d || it == '"' || it == '\n' || it == '\r' })
            "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun parse(text: String): List<List<String>> {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val d = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','

        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ }
                        else inQuotes = false
                    } else field.append(c)
                }
                c == '"' -> inQuotes = true
                c == d -> { row.add(field.toString()); field.clear() }
                c == '\n' || c == '\r' -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(field.toString()); field.clear()
                    rows.add(row); row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }
}
