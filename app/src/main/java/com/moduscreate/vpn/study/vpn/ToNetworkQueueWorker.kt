package com.moduscreate.vpn.study.vpn

import com.moduscreate.vpn.study.extensions.toHex
import com.moduscreate.vpn.study.extensions.toReadableTime
import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.utils.SimpleLogger
import com.moduscreate.vpn.study.utils.SimpleLoggerTag
import com.moduscreate.vpn.study.vpn.tcp.TcpWorkerImproved
import java.io.FileDescriptor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*

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
                SimpleLogger.log("vpnInput.read(readBuffer): $e", SimpleLoggerTag.TcpPacket)
                continue
            }
            if (readCount > 0) {
                readBuffer.flip()
                val byteArray = ByteArray(readCount)
                readBuffer.get(byteArray)

                val byteBuffer = ByteBuffer.wrap(byteArray)
                val packet = Packet(byteBuffer)

                when {
                    packet.isUDP -> deviceToNetworkUDPQueue.offer(packet)
                    packet.isTCP -> {
//                        deviceToNetworkTCPQueue.offer(packet)
                        SimpleLogger.logPacket(packet, byteBuffer, true)
                        TcpWorkerImproved.handlePacket(packet)
                    }
                    else -> SimpleLogger.log("Unknown packet received: ${byteBuffer.toHex()}", SimpleLoggerTag.TcpPacket)
                }
            } else if (readCount < 0) {
                break
            }
            readBuffer.clear()
        }
    }
}