package com.moduscreate.vpn.study

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi

var isMyVpnServiceRunning = false

class MyVpnService : VpnService() {

    private lateinit var vpnInterface: ParcelFileDescriptor

    private val mConfigureIntent: PendingIntent by lazy {
        var activityFlag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activityFlag += PendingIntent.FLAG_MUTABLE
        }
        PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), activityFlag)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent?.action == ACTION_DISCONNECT) {
            disconnect()
            START_NOT_STICKY
        } else {
            connect()
            START_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }

    private fun connect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateForegroundNotification()
        }
        vpnInterface = createVPNInterface()
        val fileDescriptor = vpnInterface.fileDescriptor
        ToNetworkQueueWorker.start(fileDescriptor)
        isMyVpnServiceRunning = true
    }

    private fun disconnect() {
        vpnInterface.close()
        stopForeground(true)
        isMyVpnServiceRunning = false
    }

    private fun createVPNInterface(): ParcelFileDescriptor {
        return Builder()
            .setSession("VPN-Demo")
            .addAddress("10.8.0.2", 32)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)
            .establish() ?: throw IllegalStateException("createVPNInterface illegal state")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateForegroundNotification() {
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentText("VPN Running")
            .setContentIntent(mConfigureIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "MyVpnService.Example"
        const val ACTION_CONNECT = "com.moduscreate.vpn.study.CONNECT"
        const val ACTION_DISCONNECT = "com.moduscreate.vpn.study.DISCONNECT"
    }
}