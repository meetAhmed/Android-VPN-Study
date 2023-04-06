package com.moduscreate.vpn.study.vpn.tcp

import android.annotation.SuppressLint
import android.net.VpnService
import android.os.Build
import com.moduscreate.vpn.study.extensions.*
import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.protocol.TCPHeader
import com.moduscreate.vpn.study.utils.SimpleLogger
import com.moduscreate.vpn.study.vpn.tcpNioSelector
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import kotlin.experimental.or

@SuppressLint("StaticFieldLeak")
object TcpWorkerImproved : Runnable {
    var vpnService: VpnService? = null
    val pipeMap = HashMap<String, TcpPipe>()
    lateinit var thread: Thread

    fun start(vpnService: VpnService) {
        this.vpnService = vpnService
        thread = Thread(this).apply {
            start()
        }
    }

    fun stop() {
        thread.interrupt()
        vpnService = null
    }

    /**
     * Read packet from TCP queue
     */
    override fun run() {
        while (!thread.isInterrupted) {
            if (vpnService == null) {
                throw IllegalStateException("TcpWorkerImproved run() VPN service is null.")
            }
            handleSockets()
        }
    }

    fun handlePacket(packet: Packet) {
        val tcpHeader = packet.tcpHeader
        val tcpPipe = getTcpPipe(packet)
        when {
            // Sync
            tcpHeader.isSYN -> {
                SimpleLogger.logPacket(packet, "Handling sync.")
                handleSyn(packet, tcpPipe)
            }
            // Reset
            tcpHeader.isRST -> {
                handleRst(tcpPipe)
            }
            // Connection close
            tcpHeader.isFIN -> {
                handleFin(packet, tcpPipe)
            }
            // Acknowledgement
            tcpHeader.isACK -> {
                handleAck(packet, tcpPipe)
            }
            else -> {
                SimpleLogger.logPacket(packet, "handlePacket() case not handled")
            }
        }
        //     SimpleLogger.logPacket(packet, "handleReadFromVpn()")
    }
}

fun TcpWorkerImproved.getTcpPipe(packet: Packet): TcpPipe {
    val vpnService = vpnService
        ?: throw IllegalStateException("VpnService should not be null")
    val destinationAddress = packet.ip4Header.destinationAddress
    val tcpHeader = packet.tcpHeader
    val destinationPort = tcpHeader.destinationPort
    val sourcePort = tcpHeader.sourcePort

    val ipAndPort = (destinationAddress.hostAddress?.plus(":")
        ?: "unknown-host-address") + destinationPort + ":" + sourcePort

    return if (pipeMap.containsKey(ipAndPort)) {
        pipeMap[ipAndPort]
            ?: throw IllegalStateException("There should be no null key in pipeMap:$ipAndPort")
    } else {
        val pipe = TcpPipe(ipAndPort, packet)
        pipeMap[ipAndPort] = pipe
        pipe.tryConnect(vpnService)
        pipe
    }
}

private fun TcpWorkerImproved.handleSockets() {
    while (!thread.isInterrupted && tcpNioSelector.selectNow() > 0) {
        val keys = tcpNioSelector.selectedKeys()
        val iterator = keys.iterator()
        while (!thread.isInterrupted && iterator.hasNext()) {
            val key = iterator.next()
            iterator.remove()
            val tcpPipe: TcpPipe? = key?.attachment() as? TcpPipe
            if (key.isValid) {
                runCatching {
                    when {
                        key.isAcceptable -> {
                            throw RuntimeException("key.isAcceptable")
                        }
                        key.isReadable -> {
                            tcpPipe?.doRead()
                        }
                        key.isConnectable -> {
                            tcpPipe?.doConnect()
                        }
                        key.isWritable -> {
                            tcpPipe?.doWrite()
                        }
                        else -> {
                            tcpPipe?.closeRst()
                        }
                    }
                    null
                }.exceptionOrNull()?.let {
                    SimpleLogger.log("TcpWorker error $it ${tcpPipe?.destinationAddress?.hostName}")
                    it.printStackTrace()
                    tcpPipe?.closeRst()
                }
            }
        }
    }
}

/**
 * Write data to remote channel.
 * Data sent from device to Destination.
 *
 * buffer?.compact(): Element between position and limit are copied to beginning.
 */
fun TcpWorkerImproved.tryFlushWrite(tcpPipe: TcpPipe): Boolean {
    val channel: SocketChannel = tcpPipe.remoteSocketChannel
    val buffer = tcpPipe.remoteOutBuffer

    /**
     * Check if remote connection is closed and buffer still has some data to send,
     * then send FIN + ACK packet to device.
     */
    if (tcpPipe.remoteSocketChannel.socket().isOutputShutdown && buffer?.remaining() != 0) {
        TcpWorker.sendTcpPack(tcpPipe, TCPHeader.FIN.toByte() or TCPHeader.ACK.toByte())
        buffer?.compact()
        return false
    }

    /**
     * Channel is not connected yet.
     * Get key for this channel and add Write Interest.
     * Return from here, since we can not write to channel right now.
     */
    if (!channel.isConnected) {
        val key = tcpPipe.remoteSocketChannelKey
        val ops = key.interestOps() or SelectionKey.OP_WRITE
        key.interestOps(ops)
        buffer?.compact()
        return false
    }

    /**
     * Write buffer data to remote channel.
     * If write is failed, compact buffer and add Write Interest in key.
     */
    while (!thread.isInterrupted && buffer?.hasRemaining() == true) {
        val n = kotlin.runCatching {
            channel.write(buffer)
        }
        if (n.isFailure) return false
        if (n.getOrThrow() <= 0) {
            val key = tcpPipe.remoteSocketChannelKey
            val ops = key.interestOps() or SelectionKey.OP_WRITE
            key.interestOps(ops)
            buffer.compact()
            return false
        }
    }

    buffer?.clear()

    // if remote channel is not active, close connection
    if (!tcpPipe.upActive) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tcpPipe.remoteSocketChannel.shutdownOutput()
        } else {
            //todo The following sentence will cause the socket to not be processed correctly, but is it okay to not handle it here?
            tcpPipe.remoteSocketChannel.close()
        }
    }
    return true
}