package com.moduscreate.vpn.study.vpn.udp

import android.annotation.SuppressLint
import android.net.VpnService
import com.moduscreate.vpn.study.dataModels.UdpTunnel
import com.moduscreate.vpn.study.extensions.toHex
import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.utils.IpUtil
import com.moduscreate.vpn.study.utils.SimpleLogger
import com.moduscreate.vpn.study.utils.SimpleLoggerTag
import com.moduscreate.vpn.study.vpn.networkToDeviceQueue
import com.moduscreate.vpn.study.vpn.udpNioSelector
import com.moduscreate.vpn.study.vpn.udpSocketMap
import com.moduscreate.vpn.study.vpn.udpTunnelQueue
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("StaticFieldLeak")
object UdpReceiveWorker : Runnable {
    private const val TAG = "UdpReceiveWorker"
    private lateinit var thread: Thread
    private var vpnService: VpnService? = null
    private var ipId = AtomicInteger()
    private const val UDP_HEADER_FULL_SIZE = Packet.IP4_HEADER_SIZE + Packet.UDP_HEADER_SIZE

    fun start(vpnService: VpnService) {
        this.vpnService = vpnService
        thread = Thread(this).apply {
            name = TAG
            start()
        }
    }

    fun stop() {
        thread.interrupt()
    }

    private fun sendUdpPacket(tunnel: UdpTunnel, source: InetSocketAddress, data: ByteArray) {
        val packet = IpUtil.buildUdpPacket(tunnel.remote, tunnel.local, ipId.addAndGet(1))

        // Creating byte buffer
        val byteBuffer = ByteBuffer.allocate(UDP_HEADER_FULL_SIZE + data.size)

        // move position to header size and insert data in buffer
        byteBuffer.apply {
            position(UDP_HEADER_FULL_SIZE)
            put(data)
        }

        // insert packet into buffer
        packet.updateUDPBuffer(byteBuffer, data.size)

        // move buffer position to the last
        byteBuffer.position(UDP_HEADER_FULL_SIZE + data.size)

        // send packet to device
        networkToDeviceQueue.offer(byteBuffer)

//        SimpleLogger.log("UDP Packet: ${byteBuffer.toHex()}", SimpleLoggerTag.PacketToDevice)
    }

    /**
     * Ready data from ready channels and send to device.
     *
     * udpTunnelQueue.poll(): retrieves and removes element from head of this queue.
     *
     * Selector: https://developer.android.com/reference/java/nio/channels/Selector
     */
    override fun run() {
        val receiveBuffer = ByteBuffer.allocate(20000)
        while (!thread.isInterrupted) {
            val readyChannels = udpNioSelector.select()
            while (!thread.isInterrupted) {
                val tunnel = udpTunnelQueue.poll() ?: break
                kotlin.runCatching {
                    val key = tunnel.channel.register(udpNioSelector, SelectionKey.OP_READ, tunnel)
                    key.interestOps(SelectionKey.OP_READ)
                }.exceptionOrNull()?.printStackTrace()
            }
            if (readyChannels == 0) {
                udpNioSelector.selectedKeys().clear()
                continue
            }
            val keys = udpNioSelector.selectedKeys()
            val iterator = keys.iterator()
            while (!thread.isInterrupted && iterator.hasNext()) {
                val key = iterator.next()
                iterator.remove()
                if (key.isValid && key.isReadable) {
                    val tunnel = key.attachment() as UdpTunnel
                    kotlin.runCatching {
                        val inputChannel = key.channel() as DatagramChannel
                        receiveBuffer.clear()
                        inputChannel.read(receiveBuffer)
                        receiveBuffer.flip()
                        val data = ByteArray(receiveBuffer.remaining())
                        receiveBuffer.get(data)
                        sendUdpPacket(
                            tunnel,
                            inputChannel.socket().localSocketAddress as InetSocketAddress,
                            data
                        )
                    }.exceptionOrNull()?.let {
                        it.printStackTrace()
                        synchronized(udpSocketMap) {
                            udpSocketMap.remove(tunnel.id)
                        }
                    }
                }
            }
        }
    }
}