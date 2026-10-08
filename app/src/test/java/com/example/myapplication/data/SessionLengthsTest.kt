package com.example.myapplication.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionLengthsTest {

    @Test
    fun `saved lengths round-trip shortest first`() {
        val raw = SessionLengths.serialize(listOf(15, 3, 7))
        assertEquals("3,7,15", raw)
        assertEquals(listOf(3, 7, 15), SessionLengths.parse(raw))
    }

    @Test
    fun `invalid saved lengths fall back to the defaults`() {
        assertEquals(SessionLengths.Default, SessionLengths.parse(null))
        assertEquals(SessionLengths.Default, SessionLengths.parse(""))
        assertEquals(SessionLengths.Default, SessionLengths.parse("2,5"))
        assertEquals(SessionLengths.Default, SessionLengths.parse("5,5,10"))
        assertEquals(SessionLengths.Default, SessionLengths.parse("2,5,11"))
        assertEquals(SessionLengths.Default, SessionLengths.parse("2,five,10"))
    }

    @Test
    fun `stepping goes minute by minute then coarser`() {
        assertEquals(6, SessionLengths.step(5, longer = true))
        assertEquals(4, SessionLengths.step(5, longer = false))
        assertEquals(12, SessionLengths.step(10, longer = true))
        assertEquals(45, SessionLengths.step(40, longer = true))
    }

    @Test
    fun `stepping stops at the ends of the range`() {
        assertNull(SessionLengths.step(1, longer = false))
        assertNull(SessionLengths.step(60, longer = true))
    }

    @Test
    fun `stepping skips lengths other slots hold`() {
        assertEquals(7, SessionLengths.step(5, longer = true, taken = listOf(6, 10)))
        assertEquals(3, SessionLengths.step(5, longer = false, taken = listOf(4)))
        assertNull(SessionLengths.step(2, longer = false, taken = listOf(1)))
    }

    @Test
    fun `closest keeps a length that's offered, else picks the nearest`() {
        assertEquals(5, SessionLengths.closest(listOf(2, 5, 10), 5))
        assertEquals(10, SessionLengths.closest(listOf(2, 10, 20), 7))
        // A tie goes to the shorter length.
        assertEquals(3, SessionLengths.closest(listOf(3, 7, 15), 5))
    }
}
