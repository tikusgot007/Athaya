package com.athaya.printer.renderer

import com.athaya.printer.model.NotaData
import com.athaya.printer.model.NotaItem
import com.athaya.printer.printer.PrinterProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotaRendererTest {

    private val renderer = NotaRenderer()

    private fun sampleData() = NotaData(
        storeName = "Toko Athaya",
        invoiceNumber = "INV-001",
        date = "2026-09-14",
        customerName = "Budi",
        items = listOf(
            NotaItem(name = "Kopi Susu", quantity = 2, price = 15000),
            NotaItem(name = "Roti Bakar Coklat Keju Spesial", quantity = 1, price = 20000),
            NotaItem(name = "Teh Manis", quantity = 3, price = 5000),
        ),
        discount = 2000,
        note = "Terima kasih sudah berbelanja di toko kami, ditunggu kedatangannya kembali.",
    )

    @Test
    fun `production template is 100x60mm before rotation`() {
        assertEquals(100f, NotaTemplateSpec.PRODUCTION.widthMm)
        assertEquals(60f, NotaTemplateSpec.PRODUCTION.heightMm)
    }

    @Test
    fun `production template becomes 60x100mm after rotation`() {
        assertEquals(60f, NotaTemplateSpec.PRODUCTION.rotatedWidthMm)
        assertEquals(100f, NotaTemplateSpec.PRODUCTION.rotatedHeightMm)
    }

    @Test
    fun `rendered production nota on 80mm profile has rotated pixel dimensions`() {
        val profile = PrinterProfile.productionIwareXs80()
        val bitmap = renderer.renderWithTemplate(sampleData(), profile, NotaTemplateSpec.PRODUCTION)

        val expectedWidth = mmToPx(NotaTemplateSpec.PRODUCTION.rotatedWidthMm, profile.dpi)
        val expectedHeight = mmToPx(NotaTemplateSpec.PRODUCTION.rotatedHeightMm, profile.dpi)

        // Width and height must land exactly as rotation implies -- never swapped.
        assertEquals(expectedWidth, bitmap.width)
        assertEquals(expectedHeight, bitmap.height)
        assertTrue("rotated width must fit printable dots", bitmap.width <= profile.printableDots)
    }

    @Test
    fun `test template on 58mm profile does not exceed printableDots`() {
        val profile = PrinterProfile.test58mm()
        val bitmap = renderer.renderWithTemplate(sampleData(), profile, NotaTemplateSpec.TEST_58MM)

        assertTrue(
            "printable width ${bitmap.width} must not exceed profile.printableDots ${profile.printableDots}",
            bitmap.width <= profile.printableDots,
        )
        assertEquals(mmToPx(56f, profile.dpi), bitmap.width)
        assertEquals(mmToPx(100f, profile.dpi), bitmap.height)
    }

    @Test(expected = PrintableWidthExceededException::class)
    fun `production template on 58mm profile throws instead of auto-scaling`() {
        val profile = PrinterProfile.test58mm()
        renderer.renderWithTemplate(sampleData(), profile, NotaTemplateSpec.PRODUCTION)
    }
}
