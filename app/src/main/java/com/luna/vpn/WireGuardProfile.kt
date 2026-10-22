package com.luna.vpn

data class WireGuardProfile(val interfaceSection: Map<String, String>, val peerSection: Map<String, String>) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        listOf("PrivateKey", "Address").forEach { if (interfaceSection[it].isNullOrBlank()) errors += "[Interface] missing $it" }
        listOf("PublicKey", "Endpoint", "AllowedIPs").forEach { if (peerSection[it].isNullOrBlank()) errors += "[Peer] missing $it" }
        return errors
    }
    fun toConf(): String = buildString {
        appendLine("[Interface]"); interfaceSection.forEach { (k, v) -> appendLine("$k = $v") }
        appendLine(); appendLine("[Peer]"); peerSection.forEach { (k, v) -> appendLine("$k = $v") }
    }
    companion object {
        fun parse(text: String): WireGuardProfile {
            var section = ""
            val iface = linkedMapOf<String, String>(); val peer = linkedMapOf<String, String>()
            text.lineSequence().map { it.substringBefore("#").trim() }.filter { it.isNotEmpty() }.forEach { line ->
                if (line.startsWith("[") && line.endsWith("]")) section = line.substring(1, line.length - 1).lowercase()
                else {
                    val i = line.indexOf('='); require(i > 0) { "Invalid WireGuard line: $line" }
                    val k = line.substring(0, i).trim(); val v = line.substring(i + 1).trim()
                    when (section) { "interface" -> iface[k] = v; "peer" -> peer[k] = v; else -> error("Setting outside a section") }
                }
            }
            require(iface.isNotEmpty() && peer.isNotEmpty()) { "Both [Interface] and [Peer] are required" }
            return WireGuardProfile(iface, peer)
        }
    }
}
