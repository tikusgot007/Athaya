package com.athaya.printer.renderer

import com.athaya.printer.model.AddressData
import com.athaya.printer.printer.PrinterProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AddressRendererTest {

    private val renderer = AddressRenderer()

    private fun sampleData() = AddressData(
        recipientName = "Muhammad Anshar",
        phone = "0812xxxxxxx",
        address = "Jl. Merdeka No. 123, Komplek Griya Asri Blok C, RT 05 RW 02",
        city = "Jakarta Selatan",
        postalCode = "12345",
        note = "Titip di satpam jika tidak ada orang di rumah",
    )

    @Test
    fun `test profile does not exceed printer printable width`() {
        val profile = PrinterProfile.test58mm()
        val bitmap = renderer.renderWithTemplate(sampleData(), profile, AddressTemplateSpec.TEST_58MM)

        assertEquals(mmToPx(56f, profile.dpi), bitmap.width)
        assertTrue(
            "printable width ${bitmap.width} must not exceed profile.printableDots ${profile.printableDots}",
            bitmap.width <= profile.printableDots,
        )
    }

    @Test
    fun `production profile renders full 80mm width on 80mm printer`() {
        val profile = PrinterProfile.productionIwareXs80()
        val bitmap = renderer.renderWithTemplate(sampleData(), profile, AddressTemplateSpec.PRODUCTION)

        assertEquals(mmToPx(80f, profile.dpi), bitmap.width)
        assertTrue(bitmap.width <= profile.printableDots)
    }

    @Test
    fun `height grows to fit long content beyond the minimum template height`() {
        val profile = PrinterProfile.productionIwareXs80()
        val longNoteData = sampleData().copy(
            note = "Catatan sangat panjang ".repeat(20),
        )
        val bitmap = renderer.renderWithTemplate(longNoteData, profile, AddressTemplateSpec.PRODUCTION)
        val minHeightPx = mmToPx(AddressTemplateSpec.PRODUCTION.minHeightMm, profile.dpi)

        assertTrue(
            "bitmap height ${bitmap.height} should grow past the ${minHeightPx}px minimum for long content",
            bitmap.height >= minHeightPx,
        )
    }

    @Test(expected = PrintableWidthExceededException::class)
    fun `production width on 58mm profile throws instead of auto-scaling`() {
        val profile = PrinterProfile.test58mm()
        renderer.renderWithTemplate(sampleData(), profile, AddressTemplateSpec.PRODUCTION)
    }
}
