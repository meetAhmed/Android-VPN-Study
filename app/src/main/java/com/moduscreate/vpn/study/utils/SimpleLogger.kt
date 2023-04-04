package com.moduscreate.vpn.study.utils

import android.util.Log
import com.moduscreate.vpn.study.extensions.toHex
import java.nio.ByteBuffer

object SimpleLogger {
    @JvmStatic
    fun log(text: String?, tag: SimpleLoggerTag = SimpleLoggerTag.General) {
        Log.i(tag.tagValue, text ?: "Null")
    }

    @JvmStatic
    fun logBuffer(text: String, byteBuffer: ByteBuffer?) {
        log(text + " " + byteBuffer?.toHex(), SimpleLoggerTag.TcpPacket)
    }
}

enum class SimpleLoggerTag(val tagValue: String) {
    General("SimpleLogger"),
    PacketToDevice("Packet_To_Device"),
    PacketFromDevice("Packet_From_Device"),
    TcpPacket("Packet_TCP"),
}