package com.moduscreate.vpn.study.extensions

import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.protocol.TCPHeader
import com.moduscreate.vpn.study.dataModels.TCBStatus
import com.moduscreate.vpn.study.vpn.tcp.TcpPipe
import com.moduscreate.vpn.study.vpn.tcp.TcpWorker
import kotlin.experimental.or

/**
 * Sync packet received, reply with sync-ack packet.
 * TCP hand shake.
 */
fun TcpWorker.handleSyn(packet: Packet, tcpPipe: TcpPipe) {
    if (tcpPipe.tcbStatus == TCBStatus.SYN_SENT) {
        tcpPipe.tcbStatus = TCBStatus.SYN_RECEIVED
    }
    val tcpHeader = packet.tcpHeader
    tcpPipe.apply {
        if (synCount == 0) {
            mySequenceNum = 1
            theirSequenceNum = tcpHeader.sequenceNumber
            myAcknowledgementNum = tcpHeader.sequenceNumber + 1
            theirAcknowledgementNum = tcpHeader.acknowledgementNumber
            sendTcpPack(this, TCPHeader.SYN.toByte() or TCPHeader.ACK.toByte())
        } else {
            myAcknowledgementNum = tcpHeader.sequenceNumber + 1
        }
        synCount++
    }
}

/**
 * Reset packet received.
 * Close socket.
 */
fun TcpWorker.handleRst(tcpPipe: TcpPipe) {
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
fun TcpWorker.handleFin(packet: Packet, tcpPipe: TcpPipe) {
    tcpPipe.myAcknowledgementNum = packet.tcpHeader.sequenceNumber + 1
    tcpPipe.theirAcknowledgementNum = packet.tcpHeader.acknowledgementNumber + 1
    sendTcpPack(tcpPipe, TCPHeader.ACK.toByte())
    tcpPipe.closeUpStream()
    tcpPipe.tcbStatus = TCBStatus.CLOSE_WAIT
}

/**
 * TCP hand shake. Send packet if device data size is not 0.
 */
fun TcpWorker.handleAck(packet: Packet, tcpPipe: TcpPipe) {
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
        sendTcpPack(this, TCPHeader.ACK.toByte())
    }
}
