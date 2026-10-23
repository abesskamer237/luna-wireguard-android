package com.luna.vpn

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.StringReader

/**
 * Keeps the backend and Tunnel identity stable across Activity recreation.
 * WSS uses an independent local UDP-to-WebSocket relay; the WireGuard packet
 * format and cryptography remain standard. The relay must be closed on stop or failure.
 */
object WireGuardController {
    @Volatile private var backendInstance: GoBackend? = null
    @Volatile private var currentState: Tunnel.State = Tunnel.State.DOWN
    @Volatile private var activeRelay: WssUdpRelay? = null

    private val tunnel = object : Tunnel {
        override fun getName(): String = "LUNA"
        override fun onStateChange(newState: Tunnel.State) {
            currentState = newState
        }
    }

    private fun backend(context: Context): GoBackend =
        backendInstance ?: synchronized(this) {
            backendInstance ?: GoBackend(context.applicationContext).also { backendInstance = it }
        }

    fun isUp(): Boolean = currentState == Tunnel.State.UP

    @Throws(Exception::class)
    @Synchronized
    fun connect(
        context: Context,
        profileText: String,
        transportConfig: WssTransportConfig? = null
    ) {
        require(profileText.isNotBlank()) { "WireGuard profile is empty" }
        var relay: WssUdpRelay? = null
        try {
            val effectiveProfile = if (transportConfig != null) {
                relay = WssUdpRelay(context.applicationContext, transportConfig)
                relay.start()
                rewriteEndpoint(profileText, "127.0.0.1:${relay.localPort}")
            } else {
                profileText
            }
            val config = Config.parse(StringReader(effectiveProfile).buffered())
            backend(context).setState(tunnel, Tunnel.State.UP, config)
            activeRelay?.close()
            activeRelay = relay
        } catch (error: Exception) {
            runCatching { relay?.close() }
            throw error
        }
    }

    @Throws(Exception::class)
    @Synchronized
    fun disconnect(context: Context) {
        try {
            backend(context).setState(tunnel, Tunnel.State.DOWN, null)
        } finally {
            activeRelay?.close()
            activeRelay = null
        }
    }

    private fun rewriteEndpoint(profileText: String, endpoint: String): String {
        var section = ""
        var replacementCount = 0
        val rewritten = profileText.lineSequence().map { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.substring(1, trimmed.length - 1).lowercase()
                line
            } else if (section == "peer" && trimmed.startsWith("Endpoint", ignoreCase = true)) {
                val separator = line.indexOf('=')
                if (separator <= 0) line else {
                    replacementCount++
                    line.substring(0, separator + 1) + " " + endpoint
                }
            } else {
                line
            }
        }.joinToString("\n")
        require(replacementCount == 1) {
            "WSS mode requires exactly one peer Endpoint in the WireGuard profile"
        }
        return rewritten
    }
}
