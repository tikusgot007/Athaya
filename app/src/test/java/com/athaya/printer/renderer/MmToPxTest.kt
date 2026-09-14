package com.athaya.printer.renderer

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plain JVM test: mmToPx is pure math, no Android dependency needed. */
class MmToPxTest {

    @Test
    fun `mmToPx converts using inches times dpi`() {
        // 25.4mm = 1 inch, so at 203dpi that's exactly 203px.
        assertEquals(203, mmToPx(25.4f, 203))
    }

    @Test
    fun `mmToPx is consistent for the same input`() {
        val a = mmToPx(58f, 203)
        val b = mmToPx(58f, 203)
        assertEquals(a, b)
    }

    @Test
    fun `mmToPx scales linearly with dpi`() {
        val at203 = mmToPx(58f, 203)
        val at406 = mmToPx(58f, 406)
        assertTrue(abs(at203 * 2 - at406) <= 1)
    }

    @Test
    fun `mmToPx for known 58mm printer at 203dpi is about 463px`() {
        // 58mm / 25.4 * 203 ~= 463.4 -> truncates to 463
        assertEquals(463, mmToPx(58f, 203))
    }
}
