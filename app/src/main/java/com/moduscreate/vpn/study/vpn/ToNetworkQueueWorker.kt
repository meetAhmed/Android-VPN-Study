package com.moduscreate.vpn.study.vpn

import com.moduscreate.vpn.study.dataModels.ManagedDatagramChannel
import com.moduscreate.vpn.study.dataModels.UdpTunnel
import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.utils.SimpleLogger
import java.io.FileDescriptor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.Selector
import java.util.concurrent.ArrayBlockingQueue

val deviceToNetworkUDPQueue = ArrayBlockingQueue<Packet>(1024)
val udpTunnelQueue = ArrayBlockingQueue<UdpTunnel>(1024)
val udpSocketMap = HashMap<String, ManagedDatagramChannel>()
val udpNioSelector: Selector = Selector.open()
val networkToDeviceQueue = ArrayBlockingQueue<ByteBuffer>(1024)
const val UDP_SOCKET_IDLE_TIMEOUT = 60

object ToNetworkQueueWorker : Runnable {
    private const val TAG = "ToNetworkQueueWorker"
    private lateinit var thread: Thread
    private lateinit var vpnInput: FileChannel
    var totalInputCount = 0L

    fun start(vpnFileDescriptor: FileDescriptor) {
        if (this::thread.isInitialized && thread.isAlive) throw IllegalStateException("ToNetworkQueueWorker start() IllegalStateException")
        vpnInput = FileInputStream(vpnFileDescriptor).channel
        thread = Thread(this).apply {
            name = TAG
            start()
        }
    }

    fun stop() {
        if (this::thread.isInitialized) {
            thread.interrupt()
        }
    }

    /**
     * ByteBuffer.allocate(20000) - Creating ByteBuffer with capacity 20,000
     * vpnInput.read(readBuffer) - read data into buffer
     *
     * e.g: 8000 bytes read
     * position = 8000, limit = 20,000
     *
     * readBuffer.flip() - flip from 'write' mode to 'read' mode
     *
     * position = 0, limit = 8000, capacity = 20,000
     *
     * create byte array, to copy actual data from readBuffer into byteBuffer
     */
    override fun run() {
        val readBuffer = ByteBuffer.allocate(20000)
        while (!thread.isInterrupted) {
            var readCount = 0
            try {
                readCount = vpnInput.read(readBuffer)
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
            if (readCount > 0) {
                readBuffer.flip()
                val byteArray = ByteArray(readCount)
                readBuffer.get(byteArray)

                val byteBuffer = ByteBuffer.wrap(byteArray)
                totalInputCount += readCount

                val packet = Packet(byteBuffer)
                if (packet.isUDP) {
                    deviceToNetworkUDPQueue.offer(packet)
                }

                SimpleLogger.log("byteBuffer read: $packet")
            } else if (readCount < 0) {
                break
            }
            readBuffer.clear()
        }
    }
}