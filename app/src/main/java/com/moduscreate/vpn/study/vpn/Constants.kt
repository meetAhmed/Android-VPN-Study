package com.moduscreate.vpn.study.vpn

import com.moduscreate.vpn.study.dataModels.ManagedDatagramChannel
import com.moduscreate.vpn.study.dataModels.UdpTunnel
import com.moduscreate.vpn.study.protocol.Packet
import java.nio.ByteBuffer
import java.nio.channels.Selector
import java.util.concurrent.ArrayBlockingQueue

// TCP
val deviceToNetworkTCPQueue = ArrayBlockingQueue<Packet>(1024 * 3)
//val deviceToNetworkTCPQueue = LinkedBlockingQueue<Packet>()
val tcpNioSelector: Selector = Selector.open()

// UDP
val deviceToNetworkUDPQueue = ArrayBlockingQueue<Packet>(1024)
val udpTunnelQueue = ArrayBlockingQueue<UdpTunnel>(1024)
val udpSocketMap = HashMap<String, ManagedDatagramChannel>()
val udpNioSelector: Selector = Selector.open()
const val UDP_SOCKET_IDLE_TIMEOUT = 60

// Network
val networkToDeviceQueue = ArrayBlockingQueue<ByteBuffer>(1024)
var isMyVpnServiceRunning = false