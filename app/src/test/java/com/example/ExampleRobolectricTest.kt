package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RouterUsageStore
import com.example.upnp.UpnpXmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read bengali app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("রাউটার ট্র্যাকার", appName)
    }

    @Test
    fun `parse rootDesc xml extracts WANCommonInterfaceConfig controlURL`() {
        val xml = """
            <?xml version="1.0"?>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
                <device>
                    <friendlyName>TP-Link Archer C6</friendlyName>
                    <deviceList>
                        <device>
                            <deviceType>urn:schemas-upnp-org:device:WANDevice:1</deviceType>
                            <serviceList>
                                <service>
                                    <serviceType>urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1</serviceType>
                                    <controlURL>/ctl/CmnIfCfg</controlURL>
                                </service>
                            </serviceList>
                        </device>
                    </deviceList>
                </device>
            </root>
        """.trimIndent()

        val endpoint = UpnpXmlParser.parseRootDescription("http://192.168.0.1:1900/igd.xml", xml)
        assertNotNull(endpoint)
        assertEquals("http://192.168.0.1:1900/ctl/CmnIfCfg", endpoint?.controlUrl)
        assertEquals("urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1", endpoint?.serviceType)
        assertEquals("TP-Link Archer C6", endpoint?.friendlyName)
        assertEquals("192.168.0.1", endpoint?.host)
    }

    @Test
    fun `parse soap counter values handles unsigned 32-bit integers`() {
        val soapRx = """
            <?xml version="1.0"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
                <s:Body>
                    <u:GetTotalBytesReceivedResponse xmlns:u="urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1">
                        <NewTotalBytesReceived>4123456789</NewTotalBytesReceived>
                    </u:GetTotalBytesReceivedResponse>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val parsed = UpnpXmlParser.parseSoapCounterValue(soapRx, "NewTotalBytesReceived")
        assertEquals(4_123_456_789L, parsed)
    }

    @Test
    fun `store persists manual prior GB additions and resets cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = RouterUsageStore.getInstance(context)
        store.resetMonthlyTotals()

        val added = store.addPriorUsageGb(downloadGb = 10.0, uploadGb = 2.5)
        assertEquals(10_737_418_240L, added.totalRxBytes)
        assertEquals(2_684_354_560L, added.totalTxBytes)

        val reset = store.resetMonthlyTotals()
        assertEquals(0L, reset.totalRxBytes)
        assertEquals(0L, reset.totalTxBytes)
        assertEquals(0, reset.rebootCount)
    }
}
