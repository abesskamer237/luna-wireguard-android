package com.luna.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketFactory
import java.security.SecureRandom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

data class WssTransportConfig(
    val endpoint: String,
    val token: String,
    val camouflage: Boolean = false
)

data class PeerTransportOptions(
    val wss: WssTransportConfig,
    val camouflage: WssTransportConfig
)

/**
 * Local UDP-to-WSS bridge.
 *
 * WireGuard itself remains standard WireGuard. GoBackend sends its encrypted UDP
 * datagrams to this loopback socket; the relay carries those datagrams inside an
 * authenticated TLS WebSocket. It is LUNA's independent transport, not Geny's protocol.
 */
class WssUdpRelay(context: Context, private val config: WssTransportConfig) : AutoCloseable {
    private val running = AtomicBoolean(false)
    private val opened = CountDownLatch(1)
    private val secureRandom = SecureRandom()
    private val udp = DatagramSocket(null)
    private val directNetwork = findDirectNetwork(context)
        ?: throw IOException("A direct Wi-Fi/mobile network is required to start WSS")
    private val httpClient = OkHttpClient.Builder()
        .socketFactory(NetworkBoundSocketFactory(directNetwork))
        .retryOnConnectionFailure(false)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var lastClient: SocketAddress? = null
    @Volatile private var openFailure: String? = null
    @Volatile private var socketOpen = false

    val localPort: Int get() = udp.localPort

    fun start() {
        require(config.endpoint.startsWith("wss://", ignoreCase = true)) {
            "WSS endpoint must use wss://"
        }
        require(config.token.matches(Regex("[A-Za-z0-9_-]{43}"))) {
            "Invalid WSS transport credential"
        }
        udp.reuseAddress = false
        udp.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0))

        val request = Request.Builder()
            .url(config.endpoint)
            .header("Authorization", "Bearer ${config.token}")
            .header("Cache-Control", "no-store")
            .build()
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                socketOpen = true
                opened.countDown()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val frame = bytes.toByteArray()
                val payload = if (config.camouflage) decodeCamouflageFrame(frame) else frame
                if (payload == null || payload.isEmpty() || payload.size > MAX_DATAGRAM) return
                val destination = lastClient ?: return
                runCatching {
                    udp.send(DatagramPacket(payload, payload.size, destination))
                }.onFailure {
                    openFailure = it.message ?: "Could not return a WireGuard packet"
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                openFailure = t.message ?: "WSS connection failed"
                socketOpen = false
                opened.countDown()
                running.set(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socketOpen = false
                running.set(false)
            }
        })

        if (!opened.await(15, TimeUnit.SECONDS)) {
            close()
            throw IOException("WSS connection timed out")
        }
        if (!socketOpen || !openFailure.isNullOrBlank()) {
            val reason = openFailure ?: "WSS did not open"
            close()
            throw IOException(reason)
        }
        running.set(true)
        thread(name = "LUNA-WSS-UDP", isDaemon = true) { relayUdpPackets() }
    }

    private fun relayUdpPackets() {
        val buffer = ByteArray(MAX_DATAGRAM + 1)
        while (running.get() && !udp.isClosed) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                udp.receive(packet)
                if (packet.length < 1 || packet.length > MAX_DATAGRAM) continue
                lastClient = packet.socketAddress
                val data = packet.data.copyOfRange(packet.offset, packet.offset + packet.length)
                val frame = if (config.camouflage) encodeCamouflageFrame(data) else data
                val sent = webSocket?.send(ByteString.of(*frame)) ?: false
                if (!sent) throw IOException("WSS socket is not writable")
            } catch (e: Exception) {
                if (running.get()) {
                    openFailure = e.message ?: "Local UDP relay failed"
                    running.set(false)
                    runCatching { webSocket?.close(1011, "local relay failure") }
                }
                break
            }
        }
    }

    override fun close() {
        running.set(false)
        runCatching { webSocket?.close(1000, "LUNA tunnel stopped") }
        runCatching { webSocket?.cancel() }
        runCatching { udp.close() }
        runCatching { httpClient.dispatcher.executorService.shutdownNow() }
        runCatching { httpClient.connectionPool.evictAll() }
    }

    private class NetworkBoundSocketFactory(private val network: Network) : SocketFactory() {
        override fun createSocket(): Socket {
            return Socket().also { socket -> network.bindSocket(socket) }
        }

        override fun createSocket(host: String, port: Int): Socket =
            createSocket().apply { connect(InetSocketAddress(host, port)) }

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
            createSocket().apply {
                bind(InetSocketAddress(localHost, localPort))
                connect(InetSocketAddress(host, port))
            }

        override fun createSocket(host: InetAddress, port: Int): Socket =
            createSocket().apply { connect(InetSocketAddress(host, port)) }

        override fun createSocket(host: InetAddress, port: Int, localHost: InetAddress, localPort: Int): Socket =
            createSocket().apply {
                bind(InetSocketAddress(localHost, localPort))
                connect(InetSocketAddress(host, port))
            }
    }

    private fun encodeCamouflageFrame(payload: ByteArray): ByteArray {
        val header = byteArrayOf(
            0x4c, 0x55, 0x4e, 0x41, 1, 1,
            ((payload.size ushr 8) and 0xff).toByte(),
            (payload.size and 0xff).toByte()
        )
        val padding = ByteArray(12 + secureRandom.nextInt(85))
        secureRandom.nextBytes(padding)
        return header + payload + padding
    }

    private fun decodeCamouflageFrame(frame: ByteArray): ByteArray? {
        if (frame.size < 8 ||
            frame[0] != 0x4c.toByte() ||
            frame[1] != 0x55.toByte() ||
            frame[2] != 0x4e.toByte() ||
            frame[3] != 0x41.toByte() ||
            frame[4] != 1.toByte() ||
            frame[5] != 1.toByte()) return null
        val size = ((frame[6].toInt() and 0xff) shl 8) or (frame[7].toInt() and 0xff)
        if (size !in 1..MAX_DATAGRAM || 8 + size > frame.size) return null
        return frame.copyOfRange(8, 8 + size)
    }

    private fun findDirectNetwork(context: Context): Network? {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.allNetworks.firstOrNull { network ->
            val caps = manager.getNetworkCapabilities(network) ?: return@firstOrNull false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        }
    }

    private companion object {
        const val MAX_DATAGRAM = 4096
    }
}
