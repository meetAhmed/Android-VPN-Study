package com.moduscreate.vpn.study.dataModels

import java.nio.channels.DatagramChannel

data class ManagedDatagramChannel(
    val id: String,
    val channel: DatagramChannel,
    var lastTime: Long = System.currentTimeMillis()
)
