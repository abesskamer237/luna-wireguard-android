package com.luna.vpn

import android.net.VpnService
import android.content.Intent
import android.os.IBinder

/** Lifecycle scaffold only: no packet engine is attached. */
class ReferenceVpnService : VpnService() {
    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Do not establish a TUN without a transport; that could black-hole traffic.
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
