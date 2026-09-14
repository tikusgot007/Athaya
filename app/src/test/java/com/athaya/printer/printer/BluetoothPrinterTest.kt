package com.athaya.printer.printer

import android.graphics.Bitmap
import com.athaya.printer.escpos.EscPosEncoder
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Everything here runs against FakeBluetoothTransport — no real Bluetooth
 * adapter or physical printer involved. This covers the parts of Phase 5
 * that CAN be verified without hardware: connection lifecycle, error
 * mapping to human-readable messages, "no second socket on double
 * connect", cut() gated by supportsCut, the profile actually reaching
 * EscPosEncoder, and that concurrent print calls never interleave bytes.
 *
 * What this suite does NOT and CANNOT prove: that AndroidBluetoothTransport
 * itself talks correctly to a real BluetoothAdapter/BluetoothSocket, or
 * that a physical 58mm/80mm printer prints the bytes correctly. See the
 * README's Phase 5 hardware test plan for that.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BluetoothPrinterTest {

    private val testDevice = BondedDeviceHandle(name = "Test Printer 58mm", address = "00:11:22:33:44:55")
    private val discoveredTestDevice = DiscoveredPrinter(name = testDevice.name, address = testDevice.address)

    private fun profile() = PrinterProfile.test58mm()

    private fun smallBitmap(width: Int = 8, height: Int = 1) = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    @Test
    fun `printBitmap while disconnected throws NotConnected with a clear message`() = runTest {
        val printer = BluetoothPrinter(FakeBluetoothTransport())

        val ex = assertThrows(BluetoothPrinterException.NotConnected::class.java) {
            kotlinx.coroutines.runBlocking { printer.printBitmap(smallBitmap(), profile()) }
        }
        assertEquals("Printer belum terhubung.", ex.message)
    }

    @Test
    fun `connect transitions from Disconnected to Connected on success`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)

        assertEquals(PrinterConnectionState.Disconnected, printer.connectionState.value)

        val result = printer.connect(discoveredTestDevice)

        assertTrue(result is PrinterConnectionState.Connected)
        assertEquals(testDevice.name, (result as PrinterConnectionState.Connected).deviceName)
        assertEquals(result, printer.connectionState.value)
    }

    @Test
    fun `connect reports a human-readable error and does not crash when permission is missing`() = runTest {
        val transport = FakeBluetoothTransport(connectPermission = false, bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)

        val result = printer.connect(discoveredTestDevice)

        assertTrue(result is PrinterConnectionState.Error)
        assertEquals("Izin Bluetooth belum diberikan. Berikan izin untuk menghubungkan printer.", (result as PrinterConnectionState.Error).message)
        assertTrue(transport.openedSockets.isEmpty())
    }

    @Test
    fun `connect reports a human-readable error when the device is not paired`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = emptyList())
        val printer = BluetoothPrinter(transport)

        val result = printer.connect(discoveredTestDevice)

        assertTrue(result is PrinterConnectionState.Error)
        assertTrue((result as PrinterConnectionState.Error).message.contains(discoveredTestDevice.address))
    }

    @Test
    fun `double connect does not open a second socket`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)

        val first = printer.connect(discoveredTestDevice)
        val second = printer.connect(discoveredTestDevice)

        assertEquals(1, transport.openedSockets.size)
        assertEquals(first, second)
    }

    @Test
    fun `disconnect closes the socket and returns to Disconnected`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)
        printer.connect(discoveredTestDevice)

        printer.disconnect()

        assertEquals(PrinterConnectionState.Disconnected, printer.connectionState.value)
        assertTrue(transport.openedSockets.single().closed)
    }

    @Test
    fun `cut does nothing when profile does not support it`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)
        printer.connect(discoveredTestDevice)
        val noCutProfile = profile().copy(supportsCut = false)

        printer.cut(noCutProfile)

        assertEquals(0, transport.openedSockets.single().buffer.size())
    }

    @Test
    fun `cut sends the cut command when profile supports it`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)
        printer.connect(discoveredTestDevice)

        printer.cut(profile().copy(supportsCut = true))

        assertArrayEquals(byteArrayOf(0x1D, 0x56, 0x00), transport.openedSockets.single().buffer.toByteArray())
    }

    @Test
    fun `printBitmap sends exactly what EscPosEncoder produces for the given profile`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)
        printer.connect(discoveredTestDevice)

        val bitmap = smallBitmap(width = 16, height = 2)
        val activeProfile = profile()
        printer.printBitmap(bitmap, activeProfile)

        val expectedBytes = EscPosEncoder.encode(bitmap, activeProfile)
        assertArrayEquals(expectedBytes, transport.openedSockets.single().buffer.toByteArray())
    }

    @Test
    fun `write failure surfaces as WriteFailed and closes the socket`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)
        printer.connect(discoveredTestDevice)
        transport.openedSockets.single().failWritesWith = IOException("simulated disconnect")

        assertThrows(BluetoothPrinterException.WriteFailed::class.java) {
            kotlinx.coroutines.runBlocking { printer.printBitmap(smallBitmap(), profile()) }
        }
        assertTrue(printer.connectionState.value is PrinterConnectionState.Error)
        assertTrue(transport.openedSockets.single().closed)
    }

    @Test
    fun `concurrent printBitmap calls never interleave bytes on the output stream`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)
        printer.connect(discoveredTestDevice)

        val activeProfile = profile()
        val bitmapA = smallBitmap(width = 8, height = 1)
        val bitmapB = smallBitmap(width = 16, height = 3)
        val expectedA = EscPosEncoder.encode(bitmapA, activeProfile)
        val expectedB = EscPosEncoder.encode(bitmapB, activeProfile)

        val jobA = async { printer.printBitmap(bitmapA, activeProfile) }
        val jobB = async { printer.printBitmap(bitmapB, activeProfile) }
        awaitAll(jobA, jobB)

        val written = transport.openedSockets.single().buffer.toByteArray()
        assertEquals(expectedA.size + expectedB.size, written.size)

        // Regardless of which job's mutex turn ran first, the two byte
        // sequences must appear back-to-back and intact -- never mixed.
        val aThenB = expectedA + expectedB
        val bThenA = expectedB + expectedA
        val matchesOneOrder = written.contentEquals(aThenB) || written.contentEquals(bThenA)
        assertTrue("expected one full sequence followed by the other, got neither ordering intact", matchesOneOrder)
    }

    @Test
    fun `scan returns paired devices mapped from the transport`() = runTest {
        val transport = FakeBluetoothTransport(bondedDevices = listOf(testDevice))
        val printer = BluetoothPrinter(transport)

        val result = printer.scan()

        assertEquals(listOf(discoveredTestDevice), result)
    }

    @Test
    fun `scan reports Bluetooth disabled clearly`() = runTest {
        val transport = FakeBluetoothTransport(enabled = false)
        val printer = BluetoothPrinter(transport)

        val ex = assertThrows(BluetoothPrinterException.BluetoothDisabled::class.java) {
            kotlinx.coroutines.runBlocking { printer.scan() }
        }
        assertEquals("Bluetooth tidak aktif. Aktifkan Bluetooth terlebih dahulu.", ex.message)
    }
}
