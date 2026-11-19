package com.example.upnp

import android.util.Xml
import com.example.data.RouterEndpoint
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URI
import java.net.URL

/**
 * Pure Android XmlPullParser utilities for UPnP IGD rootDesc.xml and SOAP responses.
 * Requires zero third-party XML or network libraries.
 */
object UpnpXmlParser {

    private const val WAN_COMMON_SERVICE_PREFIX =
        "urn:schemas-upnp-org:service:WANCommonInterfaceConfig:"

    /**
     * Parses a router's UPnP device description XML (rootDesc.xml) and extracts the
     * WANCommonInterfaceConfig serviceType and resolved absolute controlURL.
     */
    fun parseRootDescription(locationUrl: String, xmlContent: String): RouterEndpoint? {
        if (xmlContent.isBlank()) return null

        var urlBase: String? = null
        var rootFriendlyName: String? = null
        var inService = false
        var currentServiceType: String? = null
        var currentControlUrl: String? = null

        var matchedServiceType: String? = null
        var matchedControlUrl: String? = null

        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                setInput(StringReader(xmlContent.trim()))
            }

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name ?: ""
                        when {
                            tag.equals("URLBase", ignoreCase = true) -> {
                                urlBase = parser.nextText().trim().ifEmpty { null }
                            }
                            tag.equals("friendlyName", ignoreCase = true) && rootFriendlyName == null -> {
                                rootFriendlyName = parser.nextText().trim().ifEmpty { null }
                            }
                            tag.equals("service", ignoreCase = true) -> {
                                inService = true
                                currentServiceType = null
                                currentControlUrl = null
                            }
                            inService && tag.equals("serviceType", ignoreCase = true) -> {
                                currentServiceType = parser.nextText().trim()
                            }
                            inService && tag.equals("controlURL", ignoreCase = true) -> {
                                currentControlUrl = parser.nextText().trim()
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val tag = parser.name ?: ""
                        if (tag.equals("service", ignoreCase = true)) {
                            val svcType = currentServiceType
                            val ctrlUrl = currentControlUrl
                            if (!svcType.isNullOrBlank() &&
                                !ctrlUrl.isNullOrBlank() &&
                                svcType.startsWith(WAN_COMMON_SERVICE_PREFIX, ignoreCase = true)
                            ) {
                                matchedServiceType = svcType
                                matchedControlUrl = ctrlUrl
                                break
                            }
                            inService = false
                            currentServiceType = null
                            currentControlUrl = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Fall through to lightweight regex fallback for malformed router XML
        }

        val finalServiceType = matchedServiceType ?: extractServiceByRegex(xmlContent)?.first
        val finalControlRaw = matchedControlUrl ?: extractServiceByRegex(xmlContent)?.second
        if (finalServiceType.isNullOrBlank() || finalControlRaw.isNullOrBlank()) {
            return null
        }

        val resolvedControlUrl = resolveControlUrl(urlBase ?: locationUrl, finalControlRaw)
            ?: return null
        val host = runCatching { URI(resolvedControlUrl).host }.getOrNull()
            ?: runCatching { URI(locationUrl).host }.getOrNull()
            ?: locationUrl

        return RouterEndpoint(
            locationUrl = locationUrl,
            controlUrl = resolvedControlUrl,
            serviceType = finalServiceType,
            friendlyName = rootFriendlyName,
            host = host
        )
    }

    /**
     * Resolves a relative or absolute UPnP controlURL against URLBase or LOCATION.
     */
    fun resolveControlUrl(baseUrl: String, controlUrl: String): String? {
        return runCatching {
            val trimmed = controlUrl.trim()
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                URL(URL(baseUrl.trim()), trimmed).toString()
            }
        }.getOrNull()
    }

    /**
     * Parses the numeric counter tag (e.g. NewTotalBytesReceived / NewTotalBytesSent)
     * from a UPnP SOAP HTTP response body.
     */
    fun parseSoapCounterValue(soapXml: String, tagName: String): Long? {
        if (soapXml.isBlank()) return null

        // Primary: XmlPullParser
        runCatching {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                setInput(StringReader(soapXml.trim()))
            }
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG &&
                    parser.name.equals(tagName, ignoreCase = true)
                ) {
                    val raw = parser.nextText().trim()
                    val parsed = parseUnsignedLong(raw)
                    if (parsed != null) return parsed
                }
                eventType = parser.next()
            }
        }

        // Resilient fallback for routers with unescaped XML entities or namespace quirks
        val regex = Regex(
            "<(?:[\\w-]+:)?${Regex.escape(tagName)}(?:\\s[^>]*)?>\\s*([0-9]+)\\s*</(?:[\\w-]+:)?${Regex.escape(tagName)}>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val match = regex.find(soapXml) ?: return null
        return parseUnsignedLong(match.groupValues[1].trim())
    }

    private fun parseUnsignedLong(raw: String): Long? {
        if (raw.isEmpty()) return null
        return raw.toLongOrNull()
            ?: raw.toBigIntegerOrNull()?.let { big ->
                if (big.signum() < 0) null else big.toLong().coerceAtLeast(0L)
            }
    }

    private fun extractServiceByRegex(xmlContent: String): Pair<String, String>? {
        val serviceBlockRegex = Regex(
            "<(?:[\\w-]+:)?service>(.*?)</(?:[\\w-]+:)?service>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val typeRegex = Regex(
            "<(?:[\\w-]+:)?serviceType>\\s*([^<]+?)\\s*</(?:[\\w-]+:)?serviceType>",
            RegexOption.IGNORE_CASE
        )
        val controlRegex = Regex(
            "<(?:[\\w-]+:)?controlURL>\\s*([^<]+?)\\s*</(?:[\\w-]+:)?controlURL>",
            RegexOption.IGNORE_CASE
        )

        for (match in serviceBlockRegex.findAll(xmlContent)) {
            val block = match.groupValues[1]
            val svcType = typeRegex.find(block)?.groupValues?.getOrNull(1)?.trim() ?: continue
            if (svcType.startsWith(WAN_COMMON_SERVICE_PREFIX, ignoreCase = true)) {
                val ctrlUrl = controlRegex.find(block)?.groupValues?.getOrNull(1)?.trim()
                if (!ctrlUrl.isNullOrBlank()) {
                    return svcType to ctrlUrl
                }
            }
        }
        return null
    }
}
