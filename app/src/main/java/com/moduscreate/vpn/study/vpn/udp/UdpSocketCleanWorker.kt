package com.moduscreate.vpn.study.vpn.udp

import com.moduscreate.vpn.study.utils.SimpleLogger
import com.moduscreate.vpn.study.vpn.UDP_SOCKET_IDLE_TIMEOUT
import com.moduscreate.vpn.study.vpn.udpSocketMap

object UdpSocketCleanWorker : Runnable {
    private lateinit var thread: Thread
    private const val INTERVAL_TIME = 5L

    fun start() {
        thread = Thread(this).apply {
            start()
        }
    }

    fun stop() {
        thread.interrupt()
    }

    override fun run() {
        while (!thread.isInterrupted) {
            synchronized(udpSocketMap) {
                val iterator = udpSocketMap.iterator()
                var removeCount = 0
                while (!thread.isInterrupted && iterator.hasNext()) {
                    val managedDatagramChannel = iterator.next()
                    if (System.currentTimeMillis() - managedDatagramChannel.value.lastTime > UDP_SOCKET_IDLE_TIMEOUT * 1000) {
                        kotlin.runCatching {
                            managedDatagramChannel.value.channel.close()
                        }.exceptionOrNull()?.printStackTrace()
                        iterator.remove()
                        removeCount++
                    }
                }
                if (removeCount > 0) {
                    SimpleLogger.log("UdpSocketCleanWorker: Removed $removeCount timeout inactive UDPs, Queue Size Now: ${udpSocketMap.size}")
                }
            }
            Thread.sleep(INTERVAL_TIME * 1000)
        }
    }
}