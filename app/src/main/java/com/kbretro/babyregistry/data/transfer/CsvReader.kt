package com.kbretro.babyregistry.data.transfer

/** Lector de CSV tolerante: detecta el separador (, ; o tabulador) y respeta las comillas. */
object CsvReader {

    fun read(text: String): List<List<String>> {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return emptyList()
        val delimiter = listOf(',', ';', '\t').maxBy { d -> lines.first().count { it == d } }
        return parse(text, delimiter).filter { row -> row.any { it.isNotBlank() } }
    }

    /** Convierte cada fila en "cabecera valor cabecera valor…" para que el analizador vea el contexto. */
    fun toRecords(text: String): List<String> {
        val rows = read(text)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first()
        val hasHeader = header.none { cell -> cell.any(Char::isDigit) }
        if (!hasHeader) return rows.map { it.joinToString(" ") }
        return rows.drop(1).map { row ->
            row.mapIndexedNotNull { i, value ->
                if (value.isBlank()) null else "${header.getOrElse(i) { "" }} $value".trim()
            }.joinToString(" · ")
        }
    }

    private fun parse(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> {
                    cell.append('"')
                    i++
                }
                c == '"' -> quoted = !quoted
                !quoted && c == delimiter -> {
                    row.add(cell.toString().trim())
                    cell.clear()
                }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row.add(cell.toString().trim())
                    cell.clear()
                    rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString().trim())
            rows.add(row)
        }
        return rows
    }
}
