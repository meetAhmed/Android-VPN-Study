package com.moduscreate.vpn.study.vpn.tcp

import android.net.VpnService
import com.moduscreate.vpn.study.dataModels.TCBStatus
import com.moduscreate.vpn.study.extensions.closeRst
import com.moduscreate.vpn.study.extensions.doConnect
import com.moduscreate.vpn.study.extensions.doRead
import com.moduscreate.vpn.study.extensions.doWrite
import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.utils.SimpleLogger
import com.moduscreate.vpn.study.vpn.tcpNioSelector
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

class TcpPipe(val tunnelKey: String, packet: Packet) {
    companion object {
        const val TAG = "TcpPipe"
        var tunnelIds = 0
    }

    var mySequenceNum: Long = 0
    var theirSequenceNum: Long = 0
    var myAcknowledgementNum: Long = 0
    var theirAcknowledgementNum: Long = 0
    val tunnelId = tunnelIds++

    val sourceAddress: InetSocketAddress = InetSocketAddress(
        packet.ip4Header.sourceAddress,
        packet.tcpHeader.sourcePort
    )

    val destinationAddress: InetSocketAddress = InetSocketAddress(
        packet.ip4Header.destinationAddress,
        packet.tcpHeader.destinationPort
    )

    val remoteSocketChannel: SocketChannel = SocketChannel.open().also {
        it.configureBlocking(false)
    }

    // Registering channel
    // Interest set: Connect
    val remoteSocketChannelKey: SelectionKey = remoteSocketChannel.register(
        tcpNioSelector,
        SelectionKey.OP_CONNECT
    ).also { it.attach(this) }

    var tcbStatus: TCBStatus = TCBStatus.SYN_SENT
    var remoteOutBuffer: ByteBuffer? = null

    var upActive = true
    var downActive = true
    var packId = 1
    var timestamp = System.currentTimeMillis()
    var synCount = 0

    fun tryConnect(vpnService: VpnService): Result<Boolean> {
        val result = kotlin.runCatching {
            vpnService.protect(remoteSocketChannel.socket())
            remoteSocketChannel.connect(destinationAddress)
        }
        return result
    }
}