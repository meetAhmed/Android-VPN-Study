package com.moduscreate.vpn.study.extensions

import java.nio.ByteBuffer


fun ByteBuffer.toHex(): String {
    this.flip()
    var result = "position = ${this.position()} limit = ${this.limit()} packet: "
    for (i in this.position() until this.limit()) {
        val b: Byte = this.get(i)
        result += String.format("%02X ", b)
    }
    return result
}