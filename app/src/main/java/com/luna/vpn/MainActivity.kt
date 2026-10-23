package com.luna.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.wireguard.config.Config
import org.json.JSONObject
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var importedText by mutableStateOf("")
    private var statusText by mutableStateOf("Prêt — aucun tunnel actif")
    private var subscriptionText by mutableStateOf("Aucun abonnement chargé")
    private var tunnelUp by mutableStateOf(WireGuardController.isUp())
    private var apiUrlText by mutableStateOf(BuildConfig.DEFAULT_API_BASE_URL)
    private var enrollmentTokenText by mutableStateOf("")
    private var selectedTransport by mutableStateOf(LunaTransport.WIREGUARD_UDP)
    private var peerTransportOptions by mutableStateOf<PeerTransportOptions?>(null)

    

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) connectTunnel()
        else statusText = "Autorisation VPN refusée par Android"
    }

    private val importProfile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Impossible de lire le fichier")
            }.onSuccess { text ->
                importedText = text
                peerTransportOptions = null
                statusText = runCatching {
                    Config.parse(StringReader(text).buffered())
                    "Profil WireGuard reconnu — prêt à se connecter"
                }.getOrElse { e -> "Profil invalide : ${e.message ?: "erreur de format"}" }
            }.onFailure { e ->
                statusText = "Import impossible : ${e.message ?: "erreur de lecture"}"
            }
        }
    }

    private val exportProfile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && importedText.isNotBlank()) {
            runCatching {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(importedText) }
                    ?: error("Impossible d'écrire le fichier")
            }.onFailure { e ->
                statusText = "Export impossible : ${e.message ?: "erreur d'écriture"}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tunnelUp = WireGuardController.isUp()
        handleEnrollmentIntent(intent)
        setContent {
            LunaAppShell(
                statusText = statusText,
                subscriptionText = subscriptionText,
                tunnelUp = tunnelUp,
                apiUrlText = apiUrlText,
                enrollmentTokenText = enrollmentTokenText,
                importedText = importedText,
                selectedTransport = selectedTransport,
                wssConfigured = peerTransportOptions != null,
                onApiUrlChange = { apiUrlText = it },
                onTokenChange = { enrollmentTokenText = it },
                onTransportSelected = { mode ->
                    selectedTransport = mode
                    statusText = when {
                        mode == LunaTransport.WIREGUARD_UDP -> "WireGuard UDP sélectionné."
                        isTransportConfigured(mode) -> "${mode.title} sélectionné. Le relais sera vérifié au démarrage."
                        mode == LunaTransport.SLOWDNS -> "SlowDNS n’est pas disponible : résolveur et client DNS manquants."
                        else -> "Récupère un profil depuis un serveur LUNA configuré pour WSS."
                    }
                },
                onConnect = {
                    if (!isTransportConfigured(selectedTransport)) {
                        statusText = "${selectedTransport.title} n’est pas configuré. Le tunnel n’a pas été démarré."
                    } else {
                        val permissionIntent = VpnService.prepare(this@MainActivity)
                        if (permissionIntent != null) vpnPermission.launch(permissionIntent)
                        else connectTunnel()
                    }
                },
                onDisconnect = { disconnectTunnel() },
                onImport = { importProfile.launch(arrayOf("text/*", "application/octet-stream")) },
                onExport = {
                    if (importedText.isNotBlank()) exportProfile.launch("luna-wireguard.conf")
                    else statusText = "Aucun profil à exporter."
                },
                onRequestProfile = { requestProfileFromApi(apiUrlText, enrollmentTokenText) },
                onClearProfile = {
                    importedText = ""
                    peerTransportOptions = null
                    subscriptionText = "Aucun abonnement chargé"
                    statusText = "Profil effacé de l’écran."
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEnrollmentIntent(intent)
    }

    private fun handleEnrollmentIntent(source: Intent?) {
        if (source?.action != Intent.ACTION_VIEW) return
        val rawLink = source.dataString ?: return
        val link = EnrollmentLinkParser.parse(rawLink)
        if (link == null) {
            statusText = "Lien LUNA invalide ou au format incorrect"
            return
        }
        apiUrlText = link.apiBaseUrl
        enrollmentTokenText = link.token
        statusText = "Lien chargé. Vérifie le domaine HTTPS affiché avant d’envoyer le code."
    }

    private fun requestProfileFromApi(apiBase: String, enrollmentToken: String) {
        val base = apiBase.trim().trimEnd('/')
        val token = enrollmentToken.trim()
        if (!base.startsWith("https://", ignoreCase = true)) {
            statusText = "Sécurité : l'API doit utiliser HTTPS"
            return
        }
        if (token.isBlank()) {
            statusText = "Entre un code d'activation à usage unique"
            return
        }
        peerTransportOptions = null
        statusText = "Demande de configuration sécurisée…"
        lifecycleScope.launch(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(base + "/v1/enroll").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    instanceFollowRedirects = false
                    useCaches = false
                    connectTimeout = 15000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-store")
                }
                val body = JSONObject().put("token", token).toString().toByteArray(Charsets.UTF_8)
                connection.outputStream.use { it.write(body) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "{}"
                val response = JSONObject(responseText)
                if (code !in 200..299) {
                    throw IllegalStateException(response.optString("error", "Erreur HTTP $code"))
                }
                val config = response.optString("config")
                if (config.isBlank()) throw IllegalStateException("La réponse ne contient pas de configuration")
                Config.parse(StringReader(config).buffered())
                val subscription = response.optJSONObject("subscription")
                val plan = subscription?.optString("plan").orEmpty()
                val expiry = subscription?.optString("expiresAt").orEmpty()
                val maxDevices = subscription?.optInt("maxDevices", 0) ?: 0
                val transport = response.optJSONObject("transport")
                val wss = transport?.optJSONObject("wss")
                val camouflage = transport?.optJSONObject("camouflage")
                val wssEndpoint = wss?.optString("endpoint").orEmpty()
                val wssToken = wss?.optString("token").orEmpty()
                val camouflageEndpoint = camouflage?.optString("endpoint").orEmpty()
                val camouflageToken = camouflage?.optString("token").orEmpty()
                val options = if (
                    wssEndpoint.startsWith("wss://") && wssToken.isNotBlank() &&
                    camouflageEndpoint.startsWith("wss://") && camouflageToken.isNotBlank()
                ) {
                    PeerTransportOptions(
                        wss = WssTransportConfig(wssEndpoint, wssToken),
                        camouflage = WssTransportConfig(camouflageEndpoint, camouflageToken, camouflage = true)
                    )
                } else null
                withContext(Dispatchers.Main) {
                    importedText = config
                    peerTransportOptions = options
                    subscriptionText = if (plan.isNotBlank() && expiry.isNotBlank()) {
                        "$plan — expire le $expiry — appareils max : $maxDevices"
                    } else {
                        "Profil importé sans métadonnées d'abonnement"
                    }
                    statusText = if (options != null) {
                        "Profil validé. WireGuard UDP, WSS et Camouflage fournis par le serveur."
                    } else {
                        "Profil validé. WireGuard UDP disponible ; WSS doit être activé côté serveur."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText = "Échec de l'enrôlement : ${e.message ?: e.javaClass.simpleName}"
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun isTransportConfigured(mode: LunaTransport): Boolean = when (mode) {
        LunaTransport.WIREGUARD_UDP -> true
        LunaTransport.WSS -> peerTransportOptions?.wss?.endpoint?.startsWith("wss://") == true
        LunaTransport.CAMOUFLAGE -> peerTransportOptions?.camouflage?.endpoint?.startsWith("wss://") == true
        LunaTransport.SLOWDNS -> false
    }

    private fun connectTunnel() {
        val profile = importedText
        if (profile.isBlank()) {
            statusText = "Importe d'abord un profil WireGuard .conf"
            return
        }
        if (!isTransportConfigured(selectedTransport)) {
            statusText = "${selectedTransport.title} indisponible pour ce profil."
            return
        }
        val transportConfig = when (selectedTransport) {
            LunaTransport.WIREGUARD_UDP -> null
            LunaTransport.WSS -> peerTransportOptions?.wss
            LunaTransport.CAMOUFLAGE -> peerTransportOptions?.camouflage
            LunaTransport.SLOWDNS -> null
        }

        statusText = "Validation du profil et démarrage du tunnel…"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                WireGuardController.connect(applicationContext, profile, transportConfig)
                withContext(Dispatchers.Main) {
                    tunnelUp = true
                    statusText = "Tunnel activé. Vérifie le handshake et le trafic réel."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tunnelUp = false
                    statusText = "Échec de connexion : ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }
    private fun disconnectTunnel() {
        statusText = "Arrêt du tunnel…"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                WireGuardController.disconnect(applicationContext)
                withContext(Dispatchers.Main) {
                    tunnelUp = false
                    statusText = "Tunnel arrêté"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText = "Échec de l'arrêt : ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }
}
