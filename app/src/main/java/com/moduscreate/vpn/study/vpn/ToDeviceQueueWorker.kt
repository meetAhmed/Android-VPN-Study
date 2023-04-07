package com.moduscreate.vpn.study.vpn

import com.moduscreate.vpn.study.utils.SimpleLogger
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.nio.channels.FileChannel

object ToDeviceQueueWorker : Runnable {
    private const val TAG = "ToDeviceQueueWorker"
    private lateinit var thread: Thread
    var totalOutputCount = 0L
    private lateinit var vpnOutput: FileChannel

    fun start(vpnFileDescriptor: FileDescriptor) {
        if (this::thread.isInitialized && thread.isAlive) throw IllegalStateException("ToDeviceQueueWorker start IllegalStateException")
        vpnOutput = FileOutputStream(vpnFileDescriptor).channel
        thread = Thread(this).apply {
            name = TAG
            start()
        }
    }

    fun stop() {
        if (this::thread.isInitialized) {
            thread.interrupt()
        }
    }

    /**
     * Send network packet to device.
     * Both UDP and TCP.
     */
    override fun run() {
        try {
            while (!thread.isInterrupted) {
                val byteBuffer = networkToDeviceQueue.take()
                byteBuffer.flip()
                while (byteBuffer.hasRemaining()) {
                    val count = vpnOutput.write(byteBuffer)
                    if (count > 0) {
                        totalOutputCount += count
                    }
                }
            }
        } catch (error: Exception) {
//            SimpleLogger.log("ToDeviceQueueWorker: run() error $error")
        }
    }
}