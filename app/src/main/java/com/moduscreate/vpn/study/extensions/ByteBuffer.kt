package com.moduscreate.vpn.study.extensions

import java.nio.ByteBuffer

fun ByteBuffer.toHex(): String {
    val buffer: ByteBuffer? = this.duplicate()
    var result = ""
    buffer?.let {
        it.position(0)
        for (i in it.position() until it.limit()) {
            val b: Byte = it.get(i)
            result += String.format("%02X ", b)
        }
    }
    return result
}