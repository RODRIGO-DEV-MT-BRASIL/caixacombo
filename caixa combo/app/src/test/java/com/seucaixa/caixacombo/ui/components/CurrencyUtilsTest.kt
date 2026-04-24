package com.seucaixa.caixacombo.ui.components

import org.junit.Assert.*
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun `toDoubleSafe should return 0_0 for null string`() {
        val result = "null".toDoubleSafe()
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `toDoubleSafe should return 0_0 for empty string`() {
        val result = "".toDoubleSafe()
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `toDoubleSafe should return 0_0 for invalid string`() {
        val result = "abc".toDoubleSafe()
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `toDoubleSafe should return correct value for valid string`() {
        val result = "123.45".toDoubleSafe()
        assertEquals(123.45, result, 0.001)
    }

    @Test
    fun `toDoubleSafe with default should return default for null string`() {
        val result = "null".toDoubleSafe(99.99)
        assertEquals(99.99, result, 0.001)
    }

    @Test
    fun `toDoubleSafe with default should return correct value for valid string`() {
        val result = "123.45".toDoubleSafe(99.99)
        assertEquals(123.45, result, 0.001)
    }
}
