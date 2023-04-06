package com.moduscreate.vpn.study.vpn

import com.moduscreate.vpn.study.protocol.Packet
import com.moduscreate.vpn.study.utils.SimpleLogger
import com.moduscreate.vpn.study.utils.SimpleLoggerTag
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

object ToDeviceQueueWorker {
    private var vpnOutput: FileChannel? = null

    fun start(vpnFileDescriptor: FileDescriptor) {
        vpnOutput = FileOutputStream(vpnFileDescriptor).channel
    }

    fun sendPacketToDevice(byteBuffer: ByteBuffer) {
//        val byteBuffer2 = byteBuffer.duplicate().also { it.position(0) }
//        val packet = Packet(byteBuffer2)
        SimpleLogger.log( "sendTcpPack Point D", SimpleLoggerTag.TcpPacket)
//        val logTextBefore = "position = ${byteBuffer.position()} limit = ${byteBuffer.limit()}"
        byteBuffer.position(0)
//        val logTextAfter = "position = ${byteBuffer.position()} limit = ${byteBuffer.limit()}"
        while (byteBuffer.hasRemaining()) {
            SimpleLogger.log( "sendTcpPack Point E", SimpleLoggerTag.TcpPacket)
            SimpleLogger.logPacket(
                byteBuffer,
                ""
            )
            vpnOutput?.write(byteBuffer)
        }
    }
}