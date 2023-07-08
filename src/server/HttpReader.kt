package server

/**
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
import java.util.*


class HttpReader(private val socket: Socket, private val queryListener: QueryListener) : Runnable {
    private var out: PrintWriter? = null
    private var rawOut: OutputStream? = null

    private fun msgStr(c: Int): String {
        if (c < 0) {
            return ""
        } else if (c >= 32 && c < 127) {
            return "" + c.toChar()
        } else {
            return "."
        }
    }

    private fun readLine(input : InputStream) : String? {
        val sb = StringBuffer()
        while (true) {
            val ch = input.read()
            if (ch == -1) {
                if (sb.length == 0) {
                    return null;
                } else {
                    return sb.toString();
                }
            } else if (ch == '\r'.code) {
                // ignore
            } else if (ch == '\n'.code) {
                return sb.toString()
            } else {
                sb.append(ch.toChar())
            }
        }
    }

    override fun run() {
        var input: InputStream? = null
        var query: String? = null
        try {
            input = BufferedInputStream(socket.inputStream)
            rawOut = socket.outputStream
            out = PrintWriter(BufferedWriter(
                    OutputStreamWriter(rawOut)))
            var h: QueryHandler?
            val c1 = input.read()
            if (c1 == -1) {
                return
            }
            val c2 = input.read()
            val c3 = input.read()
            val c4 = input.read()
            if (c1 == 'G'.code && c2 == 'E'.code && c3 == 'T'.code && c4 == ' '.code) {
                val queryBuf = StringBuffer()
                while(true) {
                    val data = input.read()
                    if (data == -1 || data == ' '.code) {
                        break;
                    }
                    val ch = data.toChar()
                    queryBuf.append(ch)
                }
                query = queryBuf.toString()
                query = java.net.URLDecoder.decode(query, "UTF-8")
                h = queryListener.getHandler(query, rawOut!!, out!!)
                if (h == null) {
                    h = ErrorQueryHandler("Query '$query' not implemented", rawOut!!, out!!)
                }
            } else {
                val c5 = input.read()
                if (c1 == 'P'.code && c2 == 'O'.code && c3 == 'S'.code && c4 == 'T'.code
                        && c5 == ' '.code) {
                    readLine(input)     // Skip the rest of the query part of the post
                    val headers = HashMap<String, String>()
                    // println("POST seen:")
                    while(true) {
                        val data = readLine(input)
                        if (data == null || data == "") {
                            break;
                        }
                        val pos = data.indexOf(": ");
                        if (pos != -1) {
                            val key = data.substring(0 .. pos-1)
                            val value = data.substring(pos+2 .. data.length-1)
                            headers[key] = value;
                        }
                    }
                    queryListener.handlePost(headers, input)
                    h = ErrorQueryHandler("POST probably went OK", rawOut!!, out!!)
                } else {
                    val msg = if (c5 == -1) {
                        "EOF on socket read"
                    } else {
                        "Protocol error:  \"" + msgStr(c1) + msgStr(c2) +
                        msgStr(c3) + msgStr(c4) + msgStr(c5) + "\" unrecognized"
                    }
                    h = ErrorQueryHandler(msg, rawOut!!, out!!)
                    System.err.println(msg)
                }
            }
            h.run()
        } catch (ex: IOException) {
            if (query != null) {
                System.err.println("Error processing query \"" + query + "\"")
            }
            ex.printStackTrace()
        } finally {
            if (out != null) {
                out!!.close()
            }
            try {
                if (input != null) {
                    input.close()
                }
            } catch (ignored: IOException) {
            }

            try {
                socket.close()
            } catch (ignored: IOException) {
            }
        }
    }
}
