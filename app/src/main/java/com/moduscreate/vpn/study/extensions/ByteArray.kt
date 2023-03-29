package com.moduscreate.vpn.study.extensions

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte ->
    "%02x".format(eachByte)
}