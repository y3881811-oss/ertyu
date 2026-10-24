package com.ertyu.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Collections
import java.net.Inet4Address
import java.net.NetworkInterface

class WebShareServer(
    private val port: Int,
    private val rootDir: File
) {
    private var serverSocket: ServerSocket? = null
    private var running = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        running = true
        serverSocket = ServerSocket(port)
        scope.launch {
            while (running) {
                val socket = try {
                    serverSocket?.accept()
                } catch (e: Exception) {
                    if (running) null else break
                } ?: continue
                launch { handleClient(socket) }
            }
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        scope.cancel()
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 30000
            val input = socket.getInputStream().buffered()
            val output = socket.getOutputStream().buffered()

            val requestLine = readLine(input) ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 3) return
            val method = parts[0].uppercase()
            val rawUri = parts[1]

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(input) ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
            }

            // 处理跨域和 Options
            if (method == "OPTIONS") {
                writeResponse(output, 200, "text/plain", ByteArray(0), mapOf("Access-Control-Allow-Origin" to "*", "Access-Control-Allow-Methods" to "GET, PUT, OPTIONS"))
                return
            }

            if (method == "GET" && (rawUri == "/" || rawUri.startsWith("/index")) ) {
                // 返回前端 HTML
                val html = getHtmlPage()
                writeResponse(output, 200, "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))
                return
            }

            if (method == "PUT" && rawUri.startsWith("/upload?name=")) {
                // 处理上传
                val fileName = URLDecoder.decode(rawUri.substringAfter("name="), "UTF-8")
                val file = File(rootDir, fileName)
                val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L
                
                file.outputStream().buffered().use { out ->
                    var remaining = contentLength
                    val buf = ByteArray(64 * 1024)
                    while (remaining > 0) {
                        val toRead = minOf(buf.size.toLong(), remaining).toInt()
                        val n = input.read(buf, 0, toRead)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        remaining -= n
                    }
                }
                writeResponse(output, 200, "application/json", "{\"status\":\"ok\"}".toByteArray(), mapOf("Access-Control-Allow-Origin" to "*"))
                return
            }

            writeResponse(output, 404, "text/plain", "Not Found".toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun readLine(input: java.io.InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\r'.code) {
                val next = input.read()
                if (next == '\n'.code) return sb.toString()
                if (next >= 0) sb.append(next.toChar())
                return sb.toString()
            }
            if (b == '\n'.code) return sb.toString()
            sb.append(b.toChar())
        }
    }

    private fun writeResponse(
        output: OutputStream,
        status: Int,
        contentType: String,
        body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val headers = buildString {
            append("HTTP/1.1 $status OK\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Connection: close\r\n")
            extraHeaders.forEach { (k, v) -> append("$k: $v\r\n") }
            append("\r\n")
        }
        output.write(headers.toByteArray(Charsets.US_ASCII))
        if (body.isNotEmpty()) output.write(body)
        output.flush()
    }

    private fun getHtmlPage(): String {
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>MTFM 局域网快传</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background-color: #f4f6f8; padding: 40px; text-align: center; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
                    .drop-zone { border: 2px dashed #ccc; border-radius: 8px; padding: 60px 20px; margin: 20px 0; cursor: pointer; transition: all 0.3s; }
                    .drop-zone.dragover { border-color: #6200ee; background-color: #f3e5f5; }
                    .progress { width: 100%; background: #e0e0e0; border-radius: 4px; margin-top: 10px; display: none; }
                    .progress-bar { width: 0%; height: 8px; background: #6200ee; border-radius: 4px; transition: width 0.2s; }
                    h1 { color: #333; }
                    p { color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>📂 局域网网页快传</h1>
                    <p>将文件拖拽到下方区域，或点击选择文件。</p>
                    <div class="drop-zone" id="dropZone">
                        <span>拖拽文件到这里 / 点击上传</span>
                        <input type="file" id="fileInput" multiple style="display: none;">
                    </div>
                    <div id="status" style="margin-top: 20px; color: #6200ee; font-weight: bold;"></div>
                    <div class="progress" id="progressContainer">
                        <div class="progress-bar" id="progressBar"></div>
                    </div>
                </div>
                <script>
                    const dropZone = document.getElementById('dropZone');
                    const fileInput = document.getElementById('fileInput');
                    const status = document.getElementById('status');
                    const progressContainer = document.getElementById('progressContainer');
                    const progressBar = document.getElementById('progressBar');

                    dropZone.addEventListener('click', () => fileInput.click());
                    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('dragover'); });
                    dropZone.addEventListener('dragleave', () => { dropZone.classList.remove('dragover'); });
                    dropZone.addEventListener('drop', (e) => {
                        e.preventDefault();
                        dropZone.classList.remove('dragover');
                        if (e.dataTransfer.files.length) {
                            handleFiles(e.dataTransfer.files);
                        }
                    });
                    fileInput.addEventListener('change', () => {
                        if (fileInput.files.length) {
                            handleFiles(fileInput.files);
                        }
                    });

                    async function handleFiles(files) {
                        for (let i = 0; i < files.length; i++) {
                            const file = files[i];
                            status.innerText = `正在上传 (${'$'}{i+1}/${'$'}{files.length}): ${'$'}{file.name}`;
                            progressContainer.style.display = 'block';
                            
                            try {
                                await uploadFile(file);
                            } catch (e) {
                                status.innerText = `上传失败: ${'$'}{file.name}`;
                                status.style.color = 'red';
                                return;
                            }
                        }
                        status.innerText = '全部上传完成！';
                        status.style.color = 'green';
                        progressContainer.style.display = 'none';
                    }

                    function uploadFile(file) {
                        return new Promise((resolve, reject) => {
                            const xhr = new XMLHttpRequest();
                            xhr.open('PUT', '/upload?name=' + encodeURIComponent(file.name), true);
                            
                            xhr.upload.onprogress = (e) => {
                                if (e.lengthComputable) {
                                    const percent = (e.loaded / e.total) * 100;
                                    progressBar.style.width = percent + '%';
                                }
                            };
                            
                            xhr.onload = () => xhr.status === 200 ? resolve() : reject();
                            xhr.onerror = () => reject();
                            xhr.send(file);
                        });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                for (iface in Collections.list(interfaces)) {
                    if (!iface.isUp || iface.isLoopback) continue
                    for (addr in Collections.list(iface.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress ?: ""
                        }
                    }
                }
            } catch (e: Exception) {}
            return ""
        }
    }
}