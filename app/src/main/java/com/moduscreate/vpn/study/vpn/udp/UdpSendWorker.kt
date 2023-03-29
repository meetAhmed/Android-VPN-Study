package com.moduscreate.vpn.study.vpn.udp

import android.annotation.SuppressLint
import android.net.VpnService
import com.moduscreate.vpn.study.dataModels.ManagedDatagramChannel
import com.moduscreate.vpn.study.dataModels.UdpTunnel
import com.moduscreate.vpn.study.utils.SimpleLogger
import com.moduscreate.vpn.study.vpn.*
import java.net.ConnectException
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

@SuppressLint("StaticFieldLeak")
object UdpSendWorker : Runnable {
    private lateinit var thread: Thread
    private var vpnService: VpnService? = null

    fun start(vpnService: MyVpnService) {
        UdpSendWorker.vpnService = vpnService
        udpTunnelQueue.clear()
        thread = Thread(this).apply {
            start()
        }
    }

    fun stop() {
        if (this::thread.isInitialized) {
            thread.interrupt()
        }
        vpnService = null
    }

    /**
     * Take packet from deviceToNetworkUDPQueue.
     * Check if udpSocketMap queue contains packet ip and port, then utilize it for writing packet
     * buffer otherwise create new socket and connect to dest ip and port.
     *
     * Backing buffer is Data part of packet.
     *
     * vpnService?.protect(socket): Protect a socket from VPN connections. After protecting, data
     *                              sent through this socket will go directly to the underlying
     *                              network, so its traffic will not be forwarded through the VPN.
     *
     * Selector: https://developer.android.com/reference/java/nio/channels/Selector
     */
    override fun run() {
        while (!thread.isInterrupted) {
            val packet = deviceToNetworkUDPQueue.take()

            val destinationAddress = packet.ip4Header.destinationAddress
            val udpHeader = packet.udpHeader
            val destinationPort = udpHeader.destinationPort
            val sourcePort = udpHeader.sourcePort
            val ipAndPort = (destinationAddress.hostAddress?.plus(":")
                ?: "unknownHostAddress") + destinationPort + ":" + sourcePort

            // if udpSocketMap does not contain ipAndPort then create new socket
            val managedChannel = if (!udpSocketMap.containsKey(ipAndPort)) {
                val channel = DatagramChannel.open()
                var channelConnectSuccess = false
                channel.apply {
                    val socket = socket()
                    vpnService?.protect(socket)
                    try {
                        connect(InetSocketAddress(destinationAddress, destinationPort))
                        channelConnectSuccess = true
                    } catch (_: ConnectException) {
                    }
                    configureBlocking(false)
                }

                if (!channelConnectSuccess) {
                    continue
                }

                val tunnel = UdpTunnel(
                    ipAndPort,
                    InetSocketAddress(packet.ip4Header.sourceAddress, udpHeader.sourcePort),
                    InetSocketAddress(
                        packet.ip4Header.destinationAddress,
                        udpHeader.destinationPort
                    ),
                    channel
                )

                udpTunnelQueue.offer(tunnel)
                udpNioSelector.wakeup()

                val managedDatagramChannel = ManagedDatagramChannel(ipAndPort, channel)
                synchronized(udpSocketMap) {
                    udpSocketMap[ipAndPort] = managedDatagramChannel
                }
                managedDatagramChannel
            } else {
                synchronized(udpSocketMap) {
                    udpSocketMap[ipAndPort]
                        ?: throw IllegalStateException("udp:udpSocketMap[$ipAndPort] should not be null")
                }
            }

            managedChannel.lastTime = System.currentTimeMillis()
            val buffer = packet.backingBuffer

            kotlin.runCatching {
                while (!thread.isInterrupted && buffer.hasRemaining()) {
                    managedChannel.channel.write(buffer)
                }
            }.exceptionOrNull()?.let {
                SimpleLogger.log("Error sending UDP Packet: $it")
                managedChannel.channel.close()
                synchronized(udpSocketMap) {
                    udpSocketMap.remove(ipAndPort)
                }
            }
        }
    }
}