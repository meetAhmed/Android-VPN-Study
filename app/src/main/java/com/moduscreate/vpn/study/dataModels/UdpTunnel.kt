package com.moduscreate.vpn.study.dataModels

import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

data class UdpTunnel(
    val id: String,
    val local: InetSocketAddress,
    val remote: InetSocketAddress,
    val channel: DatagramChannel
)
