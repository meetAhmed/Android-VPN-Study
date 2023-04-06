package com.moduscreate.vpn.study.extensions

import com.moduscreate.vpn.study.dataModels.TCBStatus
import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.protocol.TCPHeader
import com.moduscreate.vpn.study.utils.SimpleLogger
import com.moduscreate.vpn.study.vpn.tcp.TcpPipe
import com.moduscreate.vpn.study.vpn.tcp.TcpWorker
import com.moduscreate.vpn.study.vpn.tcp.TcpWorkerImproved
import com.moduscreate.vpn.study.vpn.tcp.tryFlushWrite
import kotlin.experimental.or

/**
 * Sync packet received, reply with sync-ack packet.
 * TCP hand shake.
 */
fun handleSyn(packet: Packet, tcpPipe: TcpPipe) {
    if (tcpPipe.tcbStatus == TCBStatus.SYN_SENT) {
        tcpPipe.tcbStatus = TCBStatus.SYN_RECEIVED
    }
    val tcpHeader = packet.tcpHeader
    tcpPipe.apply {
        mySequenceNum = tcpHeader.sequenceNumber + 1
        theirSequenceNum = tcpHeader.sequenceNumber
        myAcknowledgementNum = tcpHeader.sequenceNumber + 1
        theirAcknowledgementNum = tcpHeader.acknowledgementNumber
        SimpleLogger.logPacket(packet, "Sending packet")
        TcpWorker.sendTcpPack(this, TCPHeader.SYN.toByte() or TCPHeader.ACK.toByte())
    }
}

/**
 * Reset packet received.
 * Close socket.
 */
fun TcpWorkerImproved.handleRst(tcpPipe: TcpPipe) {
    tcpPipe.apply {
        upActive = false
        downActive = false
        clean()
        tcbStatus = TCBStatus.CLOSE_WAIT
    }
}

/**
 * Fin packet received.
 * Close socket.
 */
fun TcpWorkerImproved.handleFin(packet: Packet, tcpPipe: TcpPipe) {
    tcpPipe.myAcknowledgementNum = packet.tcpHeader.sequenceNumber + 1
    tcpPipe.theirAcknowledgementNum = packet.tcpHeader.acknowledgementNumber + 1
    TcpWorker.sendTcpPack(tcpPipe, TCPHeader.ACK.toByte())
    tcpPipe.closeUpStream()
    tcpPipe.tcbStatus = TCBStatus.CLOSE_WAIT
}

/**
 * TCP hand shake.
 * Packet sent from device to network.
 * Write packet data to Destination, if it is not empty.
 * Send back ACK to device.
 */
fun TcpWorkerImproved.handleAck(packet: Packet, tcpPipe: TcpPipe) {
    if (tcpPipe.tcbStatus == TCBStatus.SYN_RECEIVED) {
        tcpPipe.tcbStatus = TCBStatus.ESTABLISHED
    }

    val tcpHeader = packet.tcpHeader
    val payloadSize = packet.backingBuffer.remaining()

    if (payloadSize == 0) {
        return
    }

    val newAck = tcpHeader.sequenceNumber + payloadSize
    if (newAck <= tcpPipe.myAcknowledgementNum) {
        return
    }

    tcpPipe.apply {
        myAcknowledgementNum = tcpHeader.sequenceNumber + payloadSize
        theirAcknowledgementNum = tcpHeader.acknowledgementNumber
        remoteOutBuffer = packet.backingBuffer
        tryFlushWrite(this)
        TcpWorker.sendTcpPack(this, TCPHeader.ACK.toByte())
    }
}
