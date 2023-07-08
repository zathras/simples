package com.jovial.simples

import android.content.res.AssetManager
import android.os.Environment
import server.SimpleHttp
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import kotlin.concurrent.withLock
import kotlin.reflect.KProperty

/**
 * Place where we collect the parameters for the server, and launch it.
 */
object ServerLauncher {

    private val lock : Lock  = ReentrantLock()
    private val printlnLock : Lock = ReentrantLock()
    private val listeners = mutableListOf<(String) -> Unit>()
    private var listenersCopy : List<(String) -> Unit>? = null
    private val logBuffer = StringBuffer()
    private var server : SimpleHttp? = null
    private val logger = object : SimpleHttp.Logger {
        override fun println(s: String) {
            onLoggerPrintln(s)
        }
    }
    private var tlsInitialized = false

    var port = 6001
        get() = lock.withLock { field }
        set(v) = lock.withLock { field = v }
    var prefix = SecureRandom().nextInt(1_000_000).toString()
        get() = lock.withLock { field }
        set(v) = lock.withLock { field = v }
    var directory = ""
        get() = lock.withLock { field }
        set(v) = lock.withLock { field = v }
    val serverOn
        get() = lock.withLock { server != null }
    var tlsOn = false
        get() = lock.withLock { field }
        set(v) = lock.withLock { field = v }
    var uploadsOn = false
        get() = lock.withLock { field }
        set(v) = lock.withLock { field = v }

    private fun initTLS(mgr : AssetManager) {
        lock.withLock {
            if (tlsInitialized) {
                return
            }
            tlsInitialized = true
            val ks = KeyStore.getInstance("PKCS12")
            val input = BufferedInputStream(mgr.open("simples.pkcs12"))
            // NOTE:  simples.pkcs12 is created offline.  See android/ssl_cert/make_cert.sh
            ks.load(input, "simples".toCharArray())
            input.close()
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(ks, "simples".toCharArray())
            val sslc = SSLContext.getInstance("TLS")
            sslc.init(kmf.getKeyManagers(), null, SecureRandom())
            SSLContext.setDefault(sslc)
        }
    }

    fun addLogListener(l: (String) -> Unit) : String {
        lock.withLock {
            listeners.add(l)
            listenersCopy = null
            return logBuffer.toString()
        }
    }

    fun removeLogListener(l: (String) -> Unit) {
        lock.withLock {
            listeners.remove(l)
            listenersCopy = null
        }
    }

    fun startServer(mgr: AssetManager) {
        lock.withLock {
            val s = server
            if (s != null) {
                s.close()
            }
            val ns = SimpleHttp(File(directory), "/" + prefix + "/", port,
                                uploadsOn, tlsOn, "", logger)
            server = ns
            val directoryCopy = directory
            val t = Thread() {
                if (ns.enableSsl) {
                    initTLS(mgr)
                }
                onLoggerPrintln("Serving files from $directoryCopy at ${ns.publicURL}")
                ns.run()
            }
            t.start()
        }
    }

    fun stopServer() {
        lock.withLock {
            val s = server
            if (s != null) {
                s.close()
            }
            server = null
        }
        onLoggerPrintln("Server stopped.")
    }

    private fun onLoggerPrintln(s: String) {
        // Maintain an ordering on println calls.  We do, necessarily, assume that
        // none of our println handlers waits on a task that waits on the println

        printlnLock.withLock {
            val lineWithNewline = s + "\n"
            val toNotify = lock.withLock {
                if (listenersCopy == null) {
                    listenersCopy = listeners.toList()
                }
                logBuffer.append(lineWithNewline)
                listenersCopy!!
            }
            for (recipient in toNotify) {
                recipient(lineWithNewline)
            }
        }
    }
}
