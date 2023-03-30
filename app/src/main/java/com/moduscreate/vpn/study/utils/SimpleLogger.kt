package com.moduscreate.vpn.study.utils

import android.util.Log

object SimpleLogger {
    fun log(text: String?, tag: SimpleLoggerTag = SimpleLoggerTag.General) {
        Log.i(tag.tagValue, text ?: "Null")
    }
}

enum class SimpleLoggerTag(val tagValue: String) {
    General("SimpleLogger"),
    PacketToDevice("Packet_To_Device"),
    PacketFromDevice("Packet_From_Device"),
}