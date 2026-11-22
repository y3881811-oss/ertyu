package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * 局域网扫主机：拿到本机 IP 后扫同网段 /24，
 * 逐个探测 SMB(445) / SFTP(22) / FTP(21) 端口，能连通的就是一台设备。
 */
object LanScanner {

    data class Host(val ip: String, val ports: List<Int>) {
        val hasSmb: Boolean get() = ports.contains(445)
        val hasSftp: Boolean get() = ports.contains(22)
        val hasFtp: Boolean get() = ports.contains(21)
    }

    private val PROBE_PORTS = listOf(445, 22, 21)
    private const val CONNECT_TIMEOUT_MS = 320
    private const val CONCURRENCY = 32

    fun localIPv4(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (!nif.isUp || nif.isLoopback) continue
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun probe(ip: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun scan(
        onHostFound: suspend (Host) -> Unit,
        onProgress: suspend (done: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val self = localIPv4() ?: throw IllegalStateException("没有连上 Wi-Fi / 局域网")
            val base = self.substringBeforeLast('.')
            val semaphore = Semaphore(CONCURRENCY)
            var done = 0
            val total = 254

            coroutineScope {
                (1..254).map { last ->
                    async {
                        semaphore.withPermit {
                            currentCoroutineContext().ensureActive()
                            val ip = "$base.$last"
                            val open = PROBE_PORTS.filter { port -> probe(ip, port) }
                            if (open.isNotEmpty()) {
                                onHostFound(Host(ip, open))
                            }
                            done++
                            if (done % 8 == 0) onProgress(done, total)
                        }
                    }
                }.awaitAll()
            }
            onProgress(total, total)
            total
        }
    }
}
