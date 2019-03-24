package server

/**

 * @version     1.14, 03/06/98
 * *
 * @author      Bill Foote
 */


import java.net.SocketException
import java.net.ServerSocket

import java.io.BufferedInputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.OutputStream
import java.util.HashMap
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.SSLServerSocketFactory
import kotlin.concurrent.withLock

abstract class QueryListener(val port: Int, val enableSsl : Boolean) : Runnable {
    private val lock = ReentrantLock()
    private var serverSocket : ServerSocket? = null
    private var closed = false;

    abstract fun getHandler(query: String, rawOut: OutputStream, out: PrintWriter): QueryHandler?

    abstract fun handlePost(headers: HashMap<String, String>, input: BufferedInputStream)

    override fun run() {
        lock.withLock {
            if (closed) {
                return
            }
            serverSocket = if (enableSsl) {
                SSLServerSocketFactory.getDefault().createServerSocket(port)
            } else {
                ServerSocket(port)
            }
        }
        try {
            waitForRequests()
        } catch (ex: SocketException) {
            if (!closed) {
                ex.printStackTrace()
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    fun close() {
        lock.withLock {
            closed = true
            val ss = serverSocket
            if (ss != null) {
                ss.close()
            }
        }
    }

    @Throws(IOException::class)
    private fun waitForRequests() {
        var last: Thread? = null
        while (true) {
            val s = serverSocket!!.accept()
            val t = Thread(HttpReader(s, this))
            t.priority = Thread.NORM_PRIORITY - 1
            if (last != null) {
                try {
                    last.priority = Thread.NORM_PRIORITY - 2
                } catch (ignored: Throwable) {
                }

                // If the thread is no longer alive, we'll get a 
                // NullPointerException
            }
            t.start()
            last = t
        }
    }

}
