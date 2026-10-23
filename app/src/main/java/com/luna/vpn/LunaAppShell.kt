package com.luna.vpn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DarkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LunaBackground = Color(0xFF0A0910)
private val LunaSurface = Color(0xFF1D1928)
private val LunaSurfaceRaised = Color(0xFF262033)
private val LunaPurple = Color(0xFFBD83FF)
private val LunaPurpleDeep = Color(0xFF6C3D9F)
private val LunaGreen = Color(0xFF49D994)
private val LunaText = Color(0xFFF8F5FC)
private val LunaMuted = Color(0xFFAAA3B8)
private val LunaAmber = Color(0xFFFFC857)
private val LunaRed = Color(0xFFFF6E8A)

enum class LunaTransport(val title: String, val summary: String, val available: Boolean, val icon: ImageVector) {
    WIREGUARD_UDP("WireGuard UDP", "Standard • stable • compatible", true, Icons.Filled.Security),
    WSS("WSS sécurisé", "Relais WebSocket + TLS requis", false, Icons.Filled.Lock),
    CAMOUFLAGE("Camouflage", "Transport dédié à construire", false, Icons.Filled.Language),
    SLOWDNS("SlowDNS", "Relais DNS dédié à construire", false, Icons.Filled.Dns)
}

private enum class LunaTab(val title: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Filled.Home),
    ACTIVITY("Activité", Icons.Filled.ShowChart),
    SERVERS("Serveurs", Icons.Filled.Router),
    SETTINGS("Réglages", Icons.Filled.Tune),
    HELP("Aide", Icons.Filled.HelpOutline)
}

@Composable
fun LunaAppShell(
    statusText: String,
    subscriptionText: String,
    tunnelUp: Boolean,
    apiUrlText: String,
    enrollmentTokenText: String,
    importedText: String,
    selectedTransport: LunaTransport,
    onApiUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onTransportSelected: (LunaTransport) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onRequestProfile: () -> Unit,
    onClearProfile: () -> Unit
) {
    val scheme = darkColorScheme(
        primary = LunaPurple,
        secondary = LunaGreen,
        background = LunaBackground,
        surface = LunaSurface,
        onPrimary = Color(0xFF170C22),
        onSecondary = Color(0xFF062217),
        onBackground = LunaText,
        onSurface = LunaText
    )
    MaterialTheme(colorScheme = scheme) {
        var selectedTab by remember { androidx.compose.runtime.mutableStateOf(LunaTab.HOME) }
        Surface(modifier = Modifier.fillMaxSize(), color = LunaBackground) {
            androidx.compose.material3.Scaffold(
                containerColor = LunaBackground,
                bottomBar = {
                    NavigationBar(
                        containerColor = LunaSurface,
                        tonalElevation = 0.dp
                    ) {
                        LunaTab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title, fontSize = 10.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = LunaPurple,
                                    selectedTextColor = LunaPurple,
                                    indicatorColor = LunaPurple.copy(alpha = 0.15f),
                                    unselectedIconColor = LunaMuted,
                                    unselectedTextColor = LunaMuted
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LunaHeader(tunnelUp)
                    when (selectedTab) {
                        LunaTab.HOME -> HomeScreen(
                            statusText, subscriptionText, tunnelUp, importedText,
                            selectedTransport, onConnect, onDisconnect,
                            onNavigateServers = { selectedTab = LunaTab.SERVERS },
                            onNavigateSettings = { selectedTab = LunaTab.SETTINGS }
                        )
                        LunaTab.ACTIVITY -> ActivityScreen(tunnelUp, statusText)
                        LunaTab.SERVERS -> ServersScreen(
                            apiUrlText, enrollmentTokenText, importedText,
                            onApiUrlChange, onTokenChange, onRequestProfile, onImport, onExport, onClearProfile
                        )
                        LunaTab.SETTINGS -> SettingsScreen(selectedTransport, onTransportSelected)
                        LunaTab.HELP -> HelpScreen(tunnelUp, statusText)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LunaHeader(tunnelUp: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(LunaPurple),
            contentAlignment = Alignment.Center
        ) {
            Text("L", color = Color(0xFF180D24), fontSize = 30.sp, fontWeight = FontWeight.Black)
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("LUNA", fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("CONNEXION PRIVÉE", fontSize = 9.sp, color = LunaMuted, letterSpacing = 1.5.sp)
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(50),
            color = if (tunnelUp) LunaGreen.copy(alpha = 0.12f) else LunaSurfaceRaised,
            border = BorderStroke(1.dp, if (tunnelUp) LunaGreen.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.06f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (tunnelUp) LunaGreen else LunaMuted))
                Text(if (tunnelUp) "Connecté" else "Déconnecté", color = if (tunnelUp) LunaGreen else LunaMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    statusText: String,
    subscriptionText: String,
    tunnelUp: Boolean,
    importedText: String,
    transport: LunaTransport,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNavigateServers: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val endpoint = importedText.lineSequence()
        .firstOrNull { it.trim().startsWith("Endpoint", ignoreCase = true) }
        ?.substringAfter("=")?.trim().orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Votre connexion.", fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
            Text("Simple. Privée. Sous votre contrôle.", color = LunaMuted, fontSize = 14.sp)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(206.dp)
                    .border(2.dp, LunaPurpleDeep.copy(alpha = 0.7f), CircleShape)
                    .padding(11.dp)
                    .border(2.dp, LunaPurple.copy(alpha = 0.34f), CircleShape)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(LunaSurfaceRaised)
                    .clickable { if (tunnelUp) onDisconnect() else onConnect() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(LunaPurpleDeep.copy(alpha = 0.52f))
                        .border(1.dp, LunaPurple.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = if (tunnelUp) Icons.Filled.CloudDone else Icons.Filled.PowerSettingsNew,
                        contentDescription = if (tunnelUp) "Déconnecter" else "Connecter",
                        tint = LunaText,
                        modifier = Modifier.size(62.dp)
                    )
                }
            }
            Text(
                if (tunnelUp) "Connexion protégée" else "Prêt à vous connecter",
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                if (tunnelUp) "Appuyez sur le bouton pour arrêter le tunnel" else "Touchez le bouton pour démarrer",
                fontSize = 12.sp, color = LunaMuted
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = if (tunnelUp) LunaGreen.copy(alpha = 0.12f) else LunaPurple.copy(alpha = 0.13f)
            ) {
                Text(
                    if (tunnelUp) "WIREGUARD ACTIF" else "PROTECTION EN VEILLE",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (tunnelUp) LunaGreen else LunaPurple,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
            }
        }
        StatusCard(statusText)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Security,
                title = "Protocole",
                value = transport.title,
                tint = LunaPurple
            )
            MiniInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.NetworkCheck,
                title = "Serveur",
                value = if (endpoint.isNotBlank()) endpoint else "Non configuré",
                tint = LunaGreen
            )
        }
        SectionTitle("VOTRE FORFAIT")
        Card(
            colors = CardDefaults.cardColors(containerColor = LunaSurface),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundIcon(Icons.Filled.CreditCard, LunaPurple)
                    Column(Modifier.weight(1f)) {
                        Text("Abonnement LUNA", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(subscriptionText, color = LunaMuted, fontSize = 12.sp)
                    }
                }
                Text("Votre accès est géré par l’abonnement associé au code d’activation.", color = LunaMuted, fontSize = 12.sp, lineHeight = 17.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onNavigateServers,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LunaPurpleDeep),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Configuration", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = onNavigateSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Transports", fontSize = 12.sp) }
                }
            }
        }
        SectionTitle("ACCÈS RAPIDE")
        ActionRow(Icons.Filled.VpnKey, "Importer un profil WireGuard", "Utiliser un fichier .conf existant", LunaPurple, onNavigateServers)
        ActionRow(Icons.Filled.Tune, "Choisir un transport", "UDP stable ou modes expérimentaux", LunaGreen, onNavigateSettings)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun StatusCard(statusText: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LunaSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoundIcon(
                if (statusText.contains("Échec", ignoreCase = true) || statusText.contains("invalide", ignoreCase = true)) Icons.Filled.ErrorOutline else Icons.Filled.Info,
                if (statusText.contains("Échec", ignoreCase = true) || statusText.contains("invalide", ignoreCase = true)) LunaRed else LunaPurple
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("État du service", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(statusText, color = LunaMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun MiniInfoCard(modifier: Modifier, icon: ImageVector, title: String, value: String, tint: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = LunaSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundIcon(icon, tint)
            Text(title, color = LunaMuted, fontSize = 11.sp)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ActivityScreen(tunnelUp: Boolean, statusText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle("Votre activité", "L’état du tunnel, sans inventer de statistiques.")
        Card(colors = CardDefaults.cardColors(containerColor = LunaSurface), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("État du tunnel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(50), color = if (tunnelUp) LunaGreen.copy(alpha=0.12f) else LunaSurfaceRaised) {
                        Text(
                            if (tunnelUp) "ACTIF" else "EN ATTENTE",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 10.sp,
                            color = if (tunnelUp) LunaGreen else LunaMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricPanel(Modifier.weight(1f), Icons.Filled.Download, "Réception", "—", "MB/s", LunaPurple)
                    MetricPanel(Modifier.weight(1f), Icons.Filled.Upload, "Envoi", "—", "MB/s", LunaGreen)
                }
                androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                ActivityLine("Session", if (tunnelUp) "Tunnel local actif" else "Aucune session")
                ActivityLine("Dernier état", statusText)
                Text(
                    "Les débits réels et le temps de session seront affichés lorsque le moteur exposera ses compteurs. Aucune valeur fictive n’est affichée.",
                    color = LunaMuted, fontSize = 12.sp, lineHeight = 17.sp
                )
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = LunaSurface), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Filled.Security, LunaGreen)
                Column {
                    Text("Chiffrement WireGuard", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Le statut actif ne garantit pas encore un handshake distant. Vérifiez le serveur.", color = LunaMuted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricPanel(modifier: Modifier, icon: ImageVector, title: String, value: String, unit: String, tint: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = LunaSurfaceRaised), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
                Text(title, fontSize = 11.sp, color = LunaMuted)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(unit, fontSize = 10.sp, color = LunaMuted, modifier = Modifier.padding(bottom = 4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(12) { index ->
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(tint.copy(alpha = if (index < 2) 0.55f else 0.15f)))
                }
            }
        }
    }
}

@Composable
private fun ActivityLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = LunaMuted, fontSize = 12.sp, modifier = Modifier.width(94.dp))
        Text(value, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ServersScreen(
    apiUrlText: String,
    enrollmentTokenText: String,
    importedText: String,
    onApiUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onRequestProfile: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onClearProfile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle("Serveurs & profils", "Importez votre profil ou récupérez une configuration activée.")
        Card(colors = CardDefaults.cardColors(containerColor = LunaSurface), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundIcon(Icons.Filled.Lock, LunaPurple)
                    Column {
                        Text("Code d’activation", fontWeight = FontWeight.SemiBold)
                        Text("À usage unique", color = LunaMuted, fontSize = 11.sp)
                    }
                }
                OutlinedTextField(
                    value = apiUrlText, onValueChange = onApiUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL de l’API HTTPS") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = enrollmentTokenText, onValueChange = onTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Code d’activation") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Button(
                    onClick = onRequestProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LunaPurpleDeep),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Récupérer la configuration") }
                Text("Le lien luna:// peut préremplir ces champs. Vérifiez toujours le domaine HTTPS.", color = LunaMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = LunaSurface), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundIcon(Icons.Filled.VpnKey, LunaGreen)
                    Column {
                        Text("Profil WireGuard", fontWeight = FontWeight.SemiBold)
                        Text(if (importedText.isBlank()) "Aucun profil chargé" else "Profil présent sur l’appareil", color = LunaMuted, fontSize = 11.sp)
                    }
                }
                Text(
                    if (importedText.isBlank()) "Importez un fichier .conf ou récupérez-le avec un code d’activation." else "La clé privée n’est jamais affichée ici.",
                    color = LunaMuted, fontSize = 12.sp, lineHeight = 17.sp
                )
                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LunaSurfaceRaised),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Importer un fichier .conf", color = LunaText) }
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    androidx.compose.material3.Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Exporter le profil")
                }
                OutlinedButton(onClick = onClearProfile, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Effacer le profil de l’écran")
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(selected: LunaTransport, onSelect: (LunaTransport) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle("À votre façon", "Choisissez le transport. Les modes non prêts restent bloqués.")
        SectionTitle("TRANSPORT DE CONNEXION")
        LunaTransport.values().forEach { mode ->
            TransportCard(mode, selected == mode, onClick = { onSelect(mode) })
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = LunaSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                RoundIcon(Icons.Filled.Info, LunaAmber)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("À propos des transports", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "WSS, camouflage et SlowDNS nécessitent chacun un transport client natif et un relais serveur compatibles. Ils ne seront pas activés tant que le test bout en bout n’est pas terminé.",
                        color = LunaMuted, fontSize = 12.sp, lineHeight = 17.sp
                    )
                }
            }
        }
        SectionTitle("CONFORT")
        SettingsInfoRow(Icons.Filled.Speed, "Mesures réseau", "Les statistiques s’afficheront avec les compteurs du moteur.")
        SettingsInfoRow(Icons.Filled.Lock, "Sécurité", "Aucune clé privée dans les liens d’activation.")
        SettingsInfoRow(Icons.Filled.Settings, "Moteur", "WireGuard standard via le moteur officiel Android.")
    }
}

@Composable
private fun TransportCard(mode: LunaTransport, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) LunaPurpleDeep.copy(alpha = 0.26f) else LunaSurface),
        shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, if (isSelected) LunaPurple.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIcon(mode.icon, if (mode.available) LunaGreen else LunaPurple)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(mode.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(mode.summary, color = LunaMuted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isSelected) {
                    androidx.compose.material3.Icon(Icons.Filled.CheckCircle, contentDescription = "Sélectionné", tint = LunaPurple, modifier = Modifier.size(20.dp))
                } else {
                    androidx.compose.material3.Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, tint = LunaMuted, modifier = Modifier.size(13.dp))
                }
                Text(if (mode.available) "PRÊT" else "EXPÉRIMENTAL", color = if (mode.available) LunaGreen else LunaAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
            }
        }
    }
}

@Composable
private fun HelpScreen(tunnelUp: Boolean, statusText: String) {
    var expandedFaq by remember { mutableIntStateOf(-1) }
    val questions = listOf(
        "Comment me connecter ?" to "Importez un profil .conf valide ou récupérez une configuration avec un code d’activation. Vérifiez le serveur puis appuyez sur le bouton de connexion.",
        "La connexion ne démarre pas" to "Vérifiez la date de validité de l’abonnement, l’endpoint du profil, le réseau mobile/Wi-Fi et l’autorisation VPN Android.",
        "Pourquoi Internet ne passe pas ?" to "Un tunnel démarré localement ne prouve pas qu’un handshake WireGuard a eu lieu. Le serveur doit être joignable en UDP, le pair accepté et le routage/NAT activé.",
        "WSS, camouflage ou SlowDNS ne fonctionnent pas" to "Ces options sont visibles mais ne démarrent pas le tunnel dans cette version. Un moteur client personnalisé et le relais serveur correspondant doivent être ajoutés puis testés.",
        "Comment partager une configuration ?" to "Partagez un code d’activation à usage unique via un lien luna://. Ne partagez pas le fichier WireGuard sans comprendre qu’il contient une clé privée."
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle("On vous accompagne", "Des explications courtes pour vous dépanner.")
        Card(colors = CardDefaults.cardColors(containerColor = LunaSurface), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIcon(Icons.Filled.NetworkCheck, LunaPurple)
                Text("État actuel", fontWeight = FontWeight.SemiBold)
                Text(if (tunnelUp) "Le backend local signale un tunnel actif." else "Le tunnel est arrêté.", color = LunaMuted, fontSize = 12.sp)
                Text(statusText, color = LunaMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        SectionTitle("QUESTIONS FRÉQUENTES")
        questions.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { expandedFaq = if (expandedFaq == index) -1 else index },
                colors = CardDefaults.cardColors(containerColor = LunaSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.first, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(if (expandedFaq == index) "−" else "+", color = LunaPurple, fontSize = 22.sp)
                    }
                    if (expandedFaq == index) Text(item.second, color = LunaMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
        Text(subtitle, color = LunaMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = LunaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
}

@Composable
private fun RoundIcon(icon: ImageVector, tint: Color) {
    Box(
        Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = LunaSurface),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            RoundIcon(icon, tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 11.sp, color = LunaMuted)
            }
            androidx.compose.material3.Icon(Icons.Filled.ArrowForwardIos, null, tint = LunaMuted, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        RoundIcon(icon, LunaPurple)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 11.sp, color = LunaMuted, lineHeight = 15.sp)
        }
    }
}
