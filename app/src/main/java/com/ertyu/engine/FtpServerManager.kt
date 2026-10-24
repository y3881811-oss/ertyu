package com.ertyu.engine

import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File

object FtpServerManager {
    private var server: FtpServer? = null

    fun start(port: Int, username: String, password: String, rootDir: File): Result<Unit> {
        return runCatching {
            if (server != null) stop()
            
            val serverFactory = FtpServerFactory()
            val listenerFactory = ListenerFactory()
            listenerFactory.port = port
            serverFactory.addListener("default", listenerFactory.createListener())

            val user = BaseUser()
            user.name = username
            user.password = password
            user.homeDirectory = rootDir.absolutePath
            user.authorities = listOf(WritePermission()) // 赋予读写权限
            
            val userManager = serverFactory.userManager
            userManager.save(user)

            server = serverFactory.createServer()
            server?.start()
        }
    }

    fun stop() {
        try {
            server?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        server = null
    }
}