package com.luna.vpn

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.StringReader

/**
 * Keeps the backend and Tunnel identity stable across Activity recreation.
 * GoBackend associates a running tunnel with the Tunnel instance.
 */
object WireGuardController {
    @Volatile private var backendInstance: GoBackend? = null
    @Volatile private var currentState: Tunnel.State = Tunnel.State.DOWN

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
    fun connect(context: Context, profileText: String) {
        require(profileText.isNotBlank()) { "WireGuard profile is empty" }
        val config = Config.parse(StringReader(profileText).buffered())
        backend(context).setState(tunnel, Tunnel.State.UP, config)
    }

    @Throws(Exception::class)
    fun disconnect(context: Context) {
        backend(context).setState(tunnel, Tunnel.State.DOWN, null)
    }
}
