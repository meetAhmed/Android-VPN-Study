package com.moduscreate.vpn.study.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import com.moduscreate.vpn.study.MainActivity
import com.moduscreate.vpn.study.vpn.tcp.TcpWorkerImproved
import com.moduscreate.vpn.study.vpn.udp.UdpReceiveWorker
import com.moduscreate.vpn.study.vpn.udp.UdpSendWorker
import com.moduscreate.vpn.study.vpn.udp.UdpSocketCleanWorker

class MyVpnService : VpnService() {
    private lateinit var vpnInterface: ParcelFileDescriptor

    private val mConfigureIntent: PendingIntent by lazy {
        var activityFlag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activityFlag += PendingIntent.FLAG_MUTABLE
        }
        PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), activityFlag)
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "MyVpnService.Example"
        const val ACTION_CONNECT = "com.moduscreate.vpn.study.CONNECT"
        const val ACTION_DISCONNECT = "com.moduscreate.vpn.study.DISCONNECT"
    }

    override fun onCreate() {
        UdpSendWorker.start(this)
        UdpReceiveWorker.start(this)
        UdpSocketCleanWorker.start()
//        TcpWorker.start(this)
        TcpWorkerImproved.start(this)
//        PacketLogsWorker.start()
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
        ToDeviceQueueWorker.start(fileDescriptor)
        isMyVpnServiceRunning = true
    }

    private fun disconnect() {
        ToNetworkQueueWorker.stop()
        vpnInterface.close()
        stopForeground(true)
        isMyVpnServiceRunning = false

        UdpSendWorker.stop()
        UdpReceiveWorker.stop()
        UdpSocketCleanWorker.stop()
//        TcpWorker.stop()
        TcpWorkerImproved.stop()
//        PacketLogsWorker.stop()
    }

    /**
     * ParcelFileDescriptor: A file descriptor is an object, which a, process uses to read or write to an open file and open network sockets.
     *
     * VPNService.Builder: Helper class to create a VPN interface.
     *
     * Builder().addAddress(): Add a network address to the VPN interface.
     *                         IP Address that VPN Server will assign to user on connecting to the server.
     *                         In simple words, it will be the source address.
     *
     * Builder().addDnsServer(): Convenience method to add a DNS server to the VPN connection using a numeric address string.
     *
     * Builder().addRoute(): which routes should VPN handle?
     *                       Route everything - Adding 0.0.0.0/0 (for IPv4), and ::/0 (for IPv6) would route traffic for all destinations through VPN.
     *                       Route specific - IP addresses which gets routed through tun interface.
     *
     * Builder().establish(): Create a VPN interface using the parameters supplied to this builder.
     *
     * https://developer.android.com/reference/android/net/VpnService.Builder
     *
     * IPv6:
     *  builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
     *  builder.addRoute("0:0:0:0:0:0:0:0", 0)
     */
    private fun createVPNInterface(): ParcelFileDescriptor {
        return Builder()
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("114.114.114.114")
            .setSession("VPN-Demo")
            .setBlocking(true)
            .setConfigureIntent(mConfigureIntent)
            .also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.setMetered(false)
                }
            }
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
}