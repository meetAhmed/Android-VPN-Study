package com.moduscreate.vpn.study.extensions

import android.os.Build
import com.moduscreate.vpn.study.protocol.TCPHeader
import com.moduscreate.vpn.study.dataModels.TCBStatus
import com.moduscreate.vpn.study.vpn.tcp.TcpPipe
import com.moduscreate.vpn.study.vpn.tcp.TcpWorker
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import kotlin.experimental.or

fun TcpPipe.closeRst() {
    clean()
    TcpWorker.sendTcpPack(this, TCPHeader.RST.toByte())
    upActive = false
    downActive = false
}

/**
 * Read from remote channel and send packet to device.
 */
fun TcpPipe.doRead() {
    val buffer = ByteBuffer.allocate(4096)
    var isQuitType = false

    while (!TcpWorker.thread.isInterrupted) {
        buffer.clear()
        val length = remoteSocketChannel.read(buffer)
        if (length == -1) {
            isQuitType = true
            break
        } else if (length == 0) {
            break
        } else {
            if (tcbStatus != TCBStatus.CLOSE_WAIT) {
                buffer.flip()
                val dataByteArray = ByteArray(buffer.remaining())
                buffer.get(dataByteArray)
                TcpWorker.sendTcpPack(this, TCPHeader.ACK.toByte(), dataByteArray)
            }
        }
    }

    if (isQuitType) {
        closeDownStream()
    }
}

fun TcpPipe.doConnect() {
    remoteSocketChannel.finishConnect()
    timestamp = System.currentTimeMillis()
    remoteOutBuffer?.flip()
    remoteSocketChannelKey.interestOps(SelectionKey.OP_READ or SelectionKey.OP_WRITE)
}

/**
 * Write data to remote channel.
 * Add Read as channel interest, to read data from server.
 */
fun TcpPipe.doWrite() {
    if (TcpWorker.tryFlushWrite(this)) {
        remoteSocketChannelKey.interestOps(SelectionKey.OP_READ)
    }
}

fun TcpPipe.clean() {
    kotlin.runCatching {
        if (remoteSocketChannel.isOpen) {
            remoteSocketChannel.close()
        }
        remoteOutBuffer = null
        TcpWorker.pipeMap.remove(tunnelKey)
    }.exceptionOrNull()?.printStackTrace()
}

fun TcpPipe.closeUpStream() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        kotlin.runCatching {
            if (remoteSocketChannel.isOpen && remoteSocketChannel.isConnected) {
                remoteSocketChannel.shutdownOutput()
            }
        }.exceptionOrNull()?.printStackTrace()
        upActive = false

        if (!downActive) {
            clean()
        }
    } else {
        upActive = false
        downActive = false
        clean()
    }
}

fun TcpPipe.closeDownStream() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        kotlin.runCatching {
            if (remoteSocketChannel.isConnected) {
                remoteSocketChannel.shutdownInput()
                val ops = remoteSocketChannelKey.interestOps() and SelectionKey.OP_READ.inv()
                remoteSocketChannelKey.interestOps(ops)
            }
            TcpWorker.sendTcpPack(this, (TCPHeader.FIN.toByte() or TCPHeader.ACK.toByte()))
            downActive = false
            if (!upActive) {
                clean()
            }
        }
    } else {
        TcpWorker.sendTcpPack(this, (TCPHeader.FIN.toByte() or TCPHeader.ACK.toByte()))
        upActive = false
        downActive = false
        clean()
    }
}