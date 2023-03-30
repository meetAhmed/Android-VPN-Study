package com.moduscreate.vpn.study.vpn.tcp

import android.annotation.SuppressLint
import android.net.VpnService
import android.os.Build
import com.moduscreate.vpn.study.extensions.*
import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.protocol.TCPHeader
import com.moduscreate.vpn.study.utils.IpUtil
import com.moduscreate.vpn.study.utils.SimpleLogger
import com.moduscreate.vpn.study.vpn.deviceToNetworkTCPQueue
import com.moduscreate.vpn.study.vpn.networkToDeviceQueue
import com.moduscreate.vpn.study.vpn.tcpNioSelector
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import kotlin.experimental.and
import kotlin.experimental.or

@SuppressLint("StaticFieldLeak")
object TcpWorker : Runnable {
    private const val TAG = "TcpSendWorker"

    private const val TCP_HEADER_SIZE = Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE

    lateinit var thread: Thread

    val pipeMap = HashMap<String, TcpPipe>()

    private var vpnService: VpnService? = null

    fun start(vpnService: VpnService) {
        this.vpnService = vpnService
        thread = Thread(this).apply {
            name = TAG
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
                throw IllegalStateException("TcpWorker run() VPN service is null.")
            }
            handleReadFromVpn()
            handleSockets()
            Thread.sleep(1)
        }
    }

    /**
     * Take packet from deviceToNetworkTCPQueue.
     * Check if we have already a connection to Destination, if not then try to connect
     */
    private fun handleReadFromVpn() {
        while (!thread.isInterrupted) {
            val vpnService = this.vpnService ?: return
            val packet = deviceToNetworkTCPQueue.poll() ?: return
            val destinationAddress = packet.ip4Header.destinationAddress
            val tcpHeader = packet.tcpHeader
            val destinationPort = tcpHeader.destinationPort
            val sourcePort = tcpHeader.sourcePort

            val ipAndPort = (destinationAddress.hostAddress?.plus(":")
                ?: "unknown-host-address") + destinationPort + ":" + sourcePort

            val tcpPipe = if (!pipeMap.containsKey(ipAndPort)) {
                val pipe = TcpPipe(ipAndPort, packet)
                pipe.tryConnect(vpnService)
                pipeMap[ipAndPort] = pipe
                pipe
            } else {
                pipeMap[ipAndPort] ?: throw IllegalStateException("There should be no null key in pipeMap:$ipAndPort")
            }
            handlePacket(packet, tcpPipe)
        }
    }

    /**
     * Get selected keys for processing.
     * tcpNioSelector.selectedKeys(): each key represents a registered channel which is ready for an operation.
     */
    private fun handleSockets() {
        while (!thread.isInterrupted && tcpNioSelector.selectNow() > 0) {
            val keys = tcpNioSelector.selectedKeys()
            val iterator = keys.iterator()
            while (!thread.isInterrupted && iterator.hasNext()) {
                val key = iterator.next()
                iterator.remove()
                val tcpPipe: TcpPipe? = key?.attachment() as? TcpPipe
                if (key.isValid) {
                    kotlin.runCatching {
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
                        SimpleLogger.log("TcpWorker error $it")
                        it.printStackTrace()
                        tcpPipe?.closeRst()
                    }
                }
            }
        }
    }

    /**
     * Check packet flag(s) and perform accordingly
     */
    private fun handlePacket(packet: Packet, tcpPipe: TcpPipe) {
        val tcpHeader = packet.tcpHeader
        when {
            // Sync
            tcpHeader.isSYN -> {
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
        }
    }

    /**
     * Send packet to device.
     */
    fun sendTcpPack(tcpPipe: TcpPipe, flag: Byte, data: ByteArray? = null) {
        val dataSize = data?.size ?: 0

        val packet = IpUtil.buildTcpPacket(
            tcpPipe.destinationAddress,
            tcpPipe.sourceAddress,
            flag,
            tcpPipe.myAcknowledgementNum,
            tcpPipe.mySequenceNum,
            tcpPipe.packId
        )

        tcpPipe.packId++

        val byteBuffer = ByteBuffer.allocate(TCP_HEADER_SIZE + dataSize)
        byteBuffer.position(TCP_HEADER_SIZE)

        data?.let {
            byteBuffer.put(it)
        }

        packet?.updateTCPBuffer(
            byteBuffer,
            flag,
            tcpPipe.mySequenceNum,
            tcpPipe.myAcknowledgementNum,
            dataSize
        )
        packet?.release()

        byteBuffer.position(TCP_HEADER_SIZE + dataSize)

        networkToDeviceQueue.offer(byteBuffer)

        if ((flag and TCPHeader.SYN.toByte()) != 0.toByte()) {
            tcpPipe.mySequenceNum++
        }
        if ((flag and TCPHeader.FIN.toByte()) != 0.toByte()) {
            tcpPipe.mySequenceNum++
        }
        if ((flag and TCPHeader.ACK.toByte()) != 0.toByte()) {
            tcpPipe.mySequenceNum += dataSize
        }
    }

    /**
     * Write data to remote channel.
     *
     * buffer?.compact(): Element between position and limit are copied to begining.
     */
    fun tryFlushWrite(tcpPipe: TcpPipe): Boolean {
        val channel: SocketChannel = tcpPipe.remoteSocketChannel
        val buffer = tcpPipe.remoteOutBuffer

        /**
         * Check if remote connection is closed and buffer still has some data to send,
         * then send FIN + ACK packet.
         */
        if (tcpPipe.remoteSocketChannel.socket().isOutputShutdown && buffer?.remaining() != 0) {
            sendTcpPack(tcpPipe, TCPHeader.FIN.toByte() or TCPHeader.ACK.toByte())
            buffer?.compact()
            return false
        }

        /**
         * Channel is not connected yet.
         * Get key for this channel and add Write Interest.
         * Return from here, since we can not read from channel right now.
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

        /// if remote channel is not active, close connection
        if (!tcpPipe.upActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tcpPipe.remoteSocketChannel.shutdownOutput()
            } else {
                //todo The following sentence will cause the socket to not be processed correctly, but is it okay to not handle it here?
//                tcpPipe.remoteSocketChannel.close()
            }
        }
        return true
    }
}