package com.walley.app.data.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {

    @Test
    fun `splits simple comma separated lines`() {
        val lines = parseCsvLines("a,b,c\n1,2,3\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), lines)
    }

    @Test
    fun `handles a quoted field containing a comma`() {
        val lines = parseCsvLines("name,note\nAAPL,\"Apple, Inc.\"\n")
        assertEquals(listOf(listOf("name", "note"), listOf("AAPL", "Apple, Inc.")), lines)
    }

    @Test
    fun `handles an escaped double quote inside a quoted field`() {
        val lines = parseCsvLines("name\n\"Say \"\"hi\"\"\"\n")
        assertEquals(listOf(listOf("name"), listOf("Say \"hi\"")), lines)
    }

    @Test
    fun `works without a trailing newline`() {
        val lines = parseCsvLines("a,b\n1,2")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), lines)
    }

    @Test
    fun `drops blank lines`() {
        val lines = parseCsvLines("a,b\n\n1,2\n\n")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), lines)
    }

    @Test
    fun `splits on a semicolon delimiter when requested`() {
        val lines = parseCsvLines("a;b;c\n1;2;3\n", delimiter = ';')
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), lines)
    }

    @Test
    fun `decodeCsvBytes reads valid UTF-8 as-is`() {
        val text = decodeCsvBytes("ilość,wartość".toByteArray(Charsets.UTF_8))
        assertEquals("ilość,wartość", text)
    }

    @Test
    fun `decodeCsvBytes falls back to windows-1250 for non-UTF-8 bytes`() {
        val original = "ilość,wartość"
        val bytes = original.toByteArray(charset("windows-1250"))
        assertEquals(original, decodeCsvBytes(bytes))
    }
}
