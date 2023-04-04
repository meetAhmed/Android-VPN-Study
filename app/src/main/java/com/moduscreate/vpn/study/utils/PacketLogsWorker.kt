package com.moduscreate.vpn.study.utils

import com.moduscreate.vpn.study.extensions.toHex
import com.moduscreate.vpn.study.extensions.toReadableTime
import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.vpn.tcpDumDataQueue
import java.nio.ByteBuffer
import java.util.*

object PacketLogsWorker : Runnable {
    private lateinit var thread: Thread
    private const val INTERVAL_TIME = 3L

    fun start() {
        thread = Thread(this).apply {
            start()
        }
    }

    fun stop() {
        thread.interrupt()
    }

    fun addPacketToQueue(byteBuffer: ByteBuffer, packet: Packet, fromDevice: Boolean = true) {
        val sourceAndDest = if (fromDevice) {
            packet.ip4Header.sourceAddress.hostName.trim() + ":" +
                    packet.ip4Header.destinationAddress.hostName.trim()
        } else {
            packet.ip4Header.destinationAddress.hostName.trim() + ":" +
                    packet.ip4Header.sourceAddress.hostName.trim()
        }
        val byteBuffer2 = byteBuffer.duplicate()
        val item = TcpDumpDataItem(byteBuffer2, fromDevice)
        val list = ArrayList<TcpDumpDataItem>()
        list.add(item)

        if (tcpDumDataQueue.containsKey(sourceAndDest)) {
            tcpDumDataQueue[sourceAndDest]?.packets?.add(item)
        } else {
            tcpDumDataQueue[sourceAndDest] = TcpDumpData(list)
        }
    }

    override fun run() {
        while (!thread.isInterrupted) {
            synchronized(tcpDumDataQueue) {
                val iterator = tcpDumDataQueue.iterator()
                while (!thread.isInterrupted && iterator.hasNext()) {
                    val dumpData: Map.Entry<String, TcpDumpData> = iterator.next()
                    dumpData.value.packets.sortedBy { it.timestamp }.forEach { item ->
                        item.byteBuffer.position(0)
                        val packet = Packet(item.byteBuffer)
                        SimpleLogger.log(
                            "[${item.timestamp.toReadableTime()}] [${
                                if (item.fromDevice) {
                                    "Device"
                                } else {
                                    "Network"
                                }
                            }] [${dumpData.key}] ${packet.tcpHeader?.printSimple()} ${item.byteBuffer.toHex()} [IPv4 Header Length: ${packet.ip4Header?.totalLength ?: 0}]",
                            SimpleLoggerTag.TcpPacket
                        )
                    }
//                    iterator.remove()
                    SimpleLogger.log(
                        "------------------------------------------",
                        SimpleLoggerTag.TcpPacket
                    )
                    SimpleLogger.log(
                        "------------------------------------------",
                        SimpleLoggerTag.TcpPacket
                    )
                }
            }
            SimpleLogger.log(
                "------------------------------------------",
                SimpleLoggerTag.TcpPacket
            )
            SimpleLogger.log(
                "------------------------------------------",
                SimpleLoggerTag.TcpPacket
            )
            SimpleLogger.log(
                "------------------------------------------",
                SimpleLoggerTag.TcpPacket
            )
            Thread.sleep(INTERVAL_TIME * 1000)
        }
    }
}

data class TcpDumpData(
    val packets: ArrayList<TcpDumpDataItem> = ArrayList()
)

data class TcpDumpDataItem(
    val byteBuffer: ByteBuffer,
    val fromDevice: Boolean,
    val timestamp: Date = Date()
)