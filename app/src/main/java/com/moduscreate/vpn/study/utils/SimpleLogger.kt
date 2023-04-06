package com.moduscreate.vpn.study.utils

import android.util.Log
import com.moduscreate.vpn.study.extensions.toHex
import com.moduscreate.vpn.study.extensions.toReadableTime
import com.moduscreate.vpn.study.protocol.Packet
import java.nio.ByteBuffer
import java.util.*

object SimpleLogger {
    @JvmStatic
    fun log(text: String?, tag: SimpleLoggerTag = SimpleLoggerTag.General) {
        Log.i(tag.tagValue, "[${Date().toReadableTime()}] $text")
    }

    @JvmStatic
    fun logBuffer(text: String, byteBuffer: ByteBuffer?) {
        log(text + " " + byteBuffer?.toHex(), SimpleLoggerTag.TcpPacket)
    }

    fun logPacket(packet: Packet, text: String) {
        val key = packet.ip4Header.sourceAddress.hostName.trim() + " -->> " +
                packet.ip4Header.destinationAddress.hostName.trim()
        log(
            "[Device] [${key}] ${packet.tcpHeader?.printSimple()} [$text]",
            SimpleLoggerTag.TcpPacket
        )
    }

    fun logPacket(byteBuffer: ByteBuffer, text: String) {
        val byteBuffer2 = byteBuffer.duplicate()
        byteBuffer2.position(0)
        log(
            "[Network] ${byteBuffer2.toHex()} [$text]]",
            SimpleLoggerTag.TcpPacket
        )
    }

    fun logPacket(packet: Packet, byteBuffer: ByteBuffer, fromDevice: Boolean, text: String? = null) {
        val key = if (fromDevice) {
            packet.ip4Header.sourceAddress.hostName.trim() + " -->> " +
                    packet.ip4Header.destinationAddress.hostName.trim()
        } else {
            packet.ip4Header.destinationAddress.hostName.trim() + " <<-- " +
                    packet.ip4Header.sourceAddress.hostName.trim()
        }
        log(
            "[${
                if (fromDevice) {
                    "Device"
                } else {
                    "Network"
                }
            }] [${key}] ${packet.tcpHeader?.printSimple()} ${byteBuffer.toHex()} [IPv4 Header Length: ${packet.ip4Header?.totalLength ?: 0}] [$text]",
            SimpleLoggerTag.TcpPacket
        )
    }
}

enum class SimpleLoggerTag(val tagValue: String) {
    General("SimpleLogger"),
    PacketToDevice("Packet_To_Device"),
    PacketFromDevice("Packet_From_Device"),
    TcpPacket("Packet_TCP"),
}