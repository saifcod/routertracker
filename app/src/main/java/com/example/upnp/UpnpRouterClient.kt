package com.example.upnp

import android.content.Context
import android.net.wifi.WifiManager
import com.example.data.RouterEndpoint
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Pure standard-library UPnP / SSDP client (no third-party network dependencies).
 *
 * Discovers local InternetGatewayDevice (v1 and v2) routers via SSDP multicast (239.255.255.250:1900),
 * fetches and parses rootDesc.xml for WANCommonInterfaceConfig, and polls
 * GetTotalBytesReceived / GetTotalBytesSent via SOAP HTTP POST.
 */
class UpnpRouterClient(private val context: Context) {

    data class TrafficCounters(
        val totalBytesReceived: Long,
        val totalBytesSent: Long
    )

    fun discoverRouter(): RouterEndpoint? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = runCatching {
            wifiManager?.createMulticastLock("RouterTrackerSsdpLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }.getOrNull()

        return try {
            val locationUrls = linkedSetOf<String>()
            for (target in SSDP_SEARCH_TARGETS) {
                locationUrls.addAll(sendSsdpMSearch(target))
                // Try resolving immediately if we already discovered a valid IGD location
                for (location in locationUrls) {
                    val endpoint = fetchAndParseEndpoint(location)
                    if (endpoint != null) {
                        return endpoint
                    }
                }
            }
            null
        } finally {
            runCatching {
                if (multicastLock != null && multicastLock.isHeld) {
                    multicastLock.release()
                }
            }
        }
    }

    private fun sendSsdpMSearch(searchTarget: String): Set<String> {
        val locations = linkedSetOf<String>()
        val requestPayload = buildString {
            append("M-SEARCH * HTTP/1.1\r\n")
            append("HOST: $SSDP_MULTICAST_ADDRESS:$SSDP_PORT\r\n")
            append("MAN: \"ssdp:discover\"\r\n")
            append("MX: 2\r\n")
            append("ST: $searchTarget\r\n")
            append("USER-AGENT: Android/UPnP/1.1 RouterTracker/1.0\r\n")
            append("\r\n")
        }.toByteArray(StandardCharsets.UTF_8)

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                reuseAddress = true
                soTimeout = SSDP_LISTEN_TIMEOUT_MS
            }
            val group = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
            val sendPacket = DatagramPacket(requestPayload, requestPayload.size, group, SSDP_PORT)

            // Send twice for UDP reliability on congested home Wi-Fi
            socket.send(sendPacket)
            socket.send(sendPacket)

            val deadline = System.currentTimeMillis() + SSDP_LISTEN_WINDOW_MS
            val buffer = ByteArray(4096)
            while (System.currentTimeMillis() < deadline) {
                val receivePacket = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(receivePacket)
                    val responseText = String(
                        receivePacket.data,
                        receivePacket.offset,
                        receivePacket.length,
                        StandardCharsets.UTF_8
                    )
                    extractLocationHeader(responseText)?.let { locations.add(it) }
                } catch (_: SocketTimeoutException) {
                    break
                }
            }
        } catch (_: Exception) {
            // Wi-Fi may be off or multicast unavailable; caller handles empty result gracefully
        } finally {
            runCatching { socket?.close() }
        }
        return locations
    }

    private fun extractLocationHeader(ssdpResponse: String): String? {
        return ssdpResponse
            .lineSequence()
            .firstOrNull { line -> line.startsWith("LOCATION:", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.takeIf { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
    }

    private fun fetchAndParseEndpoint(locationUrl: String): RouterEndpoint? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(locationUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = HTTP_TIMEOUT_MS
                readTimeout = HTTP_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "text/xml, application/xml")
            }
            if (connection.responseCode !in 200..299) {
                return null
            }
            val xml = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8))
                .use { it.readText() }
            UpnpXmlParser.parseRootDescription(locationUrl, xml)
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    @Throws(IOException::class)
    fun queryTrafficCounters(endpoint: RouterEndpoint): TrafficCounters {
        val rxBytes = executeSoapCounterQuery(
            endpoint = endpoint,
            actionName = ACTION_GET_TOTAL_BYTES_RECEIVED,
            responseTagName = TAG_NEW_TOTAL_BYTES_RECEIVED
        )
        val txBytes = executeSoapCounterQuery(
            endpoint = endpoint,
            actionName = ACTION_GET_TOTAL_BYTES_SENT,
            responseTagName = TAG_NEW_TOTAL_BYTES_SENT
        )
        return TrafficCounters(
            totalBytesReceived = rxBytes,
            totalBytesSent = txBytes
        )
    }

    @Throws(IOException::class)
    private fun executeSoapCounterQuery(
        endpoint: RouterEndpoint,
        actionName: String,
        responseTagName: String
    ): Long {
        val soapEnvelope = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:$actionName xmlns:u="${endpoint.serviceType}"/>
                </s:Body>
            </s:Envelope>
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(endpoint.controlUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = HTTP_TIMEOUT_MS
                readTimeout = HTTP_TIMEOUT_MS
                doOutput = true
                doInput = true
                useCaches = false
                setFixedLengthStreamingMode(soapEnvelope.size)
                setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                setRequestProperty("SOAPAction", "\"${endpoint.serviceType}#$actionName\"")
                setRequestProperty("Connection", "close")
            }

            connection.outputStream.use { os ->
                os.write(soapEnvelope)
                os.flush()
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("UPnP SOAP $actionName failed with HTTP $code")
            }

            val responseBody = BufferedReader(
                InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)
            ).use { it.readText() }

            return UpnpXmlParser.parseSoapCounterValue(responseBody, responseTagName)
                ?: throw IOException("Missing $responseTagName in SOAP response")
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val SSDP_MULTICAST_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SSDP_LISTEN_TIMEOUT_MS = 1800
        private const val SSDP_LISTEN_WINDOW_MS = 2200L
        private const val HTTP_TIMEOUT_MS = 2500

        private const val ACTION_GET_TOTAL_BYTES_RECEIVED = "GetTotalBytesReceived"
        private const val ACTION_GET_TOTAL_BYTES_SENT = "GetTotalBytesSent"
        private const val TAG_NEW_TOTAL_BYTES_RECEIVED = "NewTotalBytesReceived"
        private const val TAG_NEW_TOTAL_BYTES_SENT = "NewTotalBytesSent"

        private val SSDP_SEARCH_TARGETS = listOf(
            "urn:schemas-upnp-org:device:InternetGatewayDevice:2",
            "urn:schemas-upnp-org:device:InternetGatewayDevice:1"
        )
    }
}
