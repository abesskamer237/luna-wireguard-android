package com.luna.vpn

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
        setContent {
            MaterialTheme {
                var apiUrl by androidx.compose.runtime.remember { mutableStateOf(BuildConfig.DEFAULT_API_BASE_URL) }
                var enrollmentToken by androidx.compose.runtime.remember { mutableStateOf("") }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("LUNA", style = MaterialTheme.typography.headlineSmall)
                    Text("WireGuard standard — moteur userspace officiel")
                    Text("État : $statusText")
                    Text("Abonnement : $subscriptionText")

                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        label = { Text("URL de base de l'API HTTPS") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = enrollmentToken,
                        onValueChange = { enrollmentToken = it },
                        label = { Text("Code d'activation à usage unique") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { requestProfileFromApi(apiUrl, enrollmentToken) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Obtenir une configuration") }

                    OutlinedButton(
                        onClick = { importProfile.launch(arrayOf("text/*", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Importer un profil .conf") }

                    OutlinedButton(
                        onClick = {
                            if (importedText.isNotBlank()) exportProfile.launch("luna-wireguard.conf")
                            else statusText = "Importe d'abord un profil WireGuard"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Exporter le profil") }

                    OutlinedTextField(
                        value = importedText,
                        onValueChange = { importedText = it },
                        label = { Text("Profil WireGuard secret — ne pas partager") },
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                    )

                    Button(
                        onClick = {
                            val permissionIntent = VpnService.prepare(this@MainActivity)
                            if (permissionIntent != null) vpnPermission.launch(permissionIntent)
                            else connectTunnel()
                        },
                        enabled = !tunnelUp,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Se connecter") }

                    OutlinedButton(
                        onClick = { disconnectTunnel() },
                        enabled = tunnelUp,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Se déconnecter") }

                    Text(
                        "Utilise un code d'activation à usage unique créé par l'administrateur. " +
                            "Le profil contient une clé privée : ne le partage pas."
                    )
                }
            }
        }
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
                withContext(Dispatchers.Main) {
                    importedText = config
                    subscriptionText = if (plan.isNotBlank() && expiry.isNotBlank()) {
                        "$plan — expire le $expiry — appareils max : $maxDevices"
                    } else {
                        "Profil importé sans métadonnées d'abonnement"
                    }
                    statusText = "Configuration reçue et validée. Appuie sur Se connecter."
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

    private fun connectTunnel() {
        val profile = importedText
        if (profile.isBlank()) {
            statusText = "Importe d'abord un profil WireGuard .conf"
            return
        }

        statusText = "Validation du profil et démarrage du tunnel…"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                WireGuardController.connect(applicationContext, profile)
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
