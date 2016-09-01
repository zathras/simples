package server

/**

 * @version     1.14, 03/06/98
 * *
 * @author      Bill Foote
 */


import java.net.Socket
import java.net.ServerSocket
import java.net.InetAddress

import java.io.InputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.io.Writer
import java.io.BufferedWriter
import java.io.PrintWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.BufferedOutputStream
import java.util.HashMap
import javax.net.ssl.SSLServerSocketFactory

abstract class QueryListener(val port: Int, val enableSsl : Boolean) : Runnable {

    abstract fun getHandler(query: String, rawOut: OutputStream, out: PrintWriter): QueryHandler?

    abstract fun handlePost(headers: HashMap<String, String>, input: BufferedInputStream)

    override fun run() {
        try {
            waitForRequests()
        } catch (ex: IOException) {
            ex.printStackTrace()
            System.exit(1)
        }

    }

    @Throws(IOException::class)
    private fun waitForRequests() {
        val ss = if (enableSsl) {
            SSLServerSocketFactory.getDefault().createServerSocket(port)
        } else {
            ServerSocket(port)
        }
        var last: Thread? = null
        while (true) {
            val s = ss.accept()
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
