package com.luna.vpn

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Parses the shareable activation URI:
 * luna://enroll?api=https%3A%2F%2Fapi.example.com&token=<one-time-code>
 *
 * Never place a WireGuard private key or administrator token in this URI.
 */
data class EnrollmentLink(val apiBaseUrl: String, val token: String)

object EnrollmentLinkParser {
    fun parse(raw: String?): EnrollmentLink? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val link = URI(raw)
            require(link.scheme.equals("luna", ignoreCase = true))
            require(link.host.equals("enroll", ignoreCase = true))
            require(link.rawUserInfo == null && link.rawFragment == null)
            val query = link.rawQuery ?: error("missing query")
            val values = linkedMapOf<String, String>()
            query.split('&').forEach { part ->
                if (part.isBlank()) return@forEach
                val separator = part.indexOf('=')
                require(separator > 0) { "invalid query item" }
                val key = URLDecoder.decode(part.substring(0, separator), StandardCharsets.UTF_8.name())
                val value = URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8.name())
                require(values.put(key, value) == null) { "duplicate query item" }
            }
            require(values.keys == setOf("api", "token"))
            val apiBase = values.getValue("api").trim().trimEnd('/')
            val api = URI(apiBase)
            require(api.scheme.equals("https", ignoreCase = true))
            require(!api.host.isNullOrBlank())
            require(api.rawUserInfo == null && api.rawQuery == null && api.rawFragment == null)
            val token = values.getValue("token")
            require(token.matches(Regex("[A-Za-z0-9_-]{43}")))
            EnrollmentLink(apiBase, token)
        }.getOrNull()
    }
}
