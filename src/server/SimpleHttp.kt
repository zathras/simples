/**
 * Simple HTTP server, using code swiped from hat.
 * <p>
 *
 * Starts an http server on port 6001 in the cwd.  As a security measure,
 * the base URL for all files is http:///.  It screens
 * against having ..'s in the path such that the server goes outside the
 * cwd.
 * <p>
 *
 * These days, modern browsers may have a security policy that defaults to
 * blocking requests to unusual ports, like 6000.  As of this writing, 6000
 * was blocked in Mozilla, because it's used for X/11.  If this is a problem,
 * you can always just use curl to get around this.  If you want to use
 * Mozilla and it someday blocks port 6001, this can be overridden
 * with the network.security.ports.banned.override config property,
 * which contains a comma-delimited list of allowed ports.  Go into
 * about:config, search for that property, and if it's not there, create
 * it (right-click in the results area, New -> String).  cf.
 * https://support.mozilla.org/en-US/questions/1083282 ,
 * http://kb.mozillazine.org/Network.security.ports.banned.override
 * <p>
 *
 * Version 1.0 written 3/27/15

 * @author Bill Foote
 * *
 * @version 1.1, 4/4/16
 * @version 1.2, 8/9/16 (adds TLS/SSL support)
 */

package server

import java.io.*
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

public val localInetAddress = getAddress()

private fun getAddress() : InetAddress {
    val interfaces = ArrayList<NetworkInterface>();
    for (ne in NetworkInterface.getNetworkInterfaces()) {
        interfaces.add(ne);
    }
    interfaces.sortBy { ne -> -ne.mtu };    // Prefer wifi
    for (ne in interfaces) {
        for (ie in ne.getInetAddresses()) {
            if (!ie.isLoopbackAddress() && ie is Inet4Address) {
                return ie;
            }
        }
    }
    for (ne in interfaces) {
        for (ie in ne.getInetAddresses()) {
            if (!ie.isLoopbackAddress()) {
                return ie;
            }
        }
    }
    return InetAddress.getLocalHost()
}

class SimpleHttp(private val baseDir: File,
                 private val urlBase: String,
                 port: Int,
                 private val enableUpload : Boolean,
                 enableSsl: Boolean,
                 private val uploadsSuffix: String,
                 val logger: Logger)
     : QueryListener(port, enableSsl) {

    interface Logger {
        public fun println(s: String);
    }

    override fun getHandler(query: String, rawOut: OutputStream, out: PrintWriter): QueryHandler? {
        var q = query;
        if (!q.startsWith(urlBase)) {
            if (urlBase.length > 1 && q.startsWith(urlBase.dropLast(1))) {
                logger.println("Query missing trailing slash:  $q")
                return ErrorQueryHandler("Missing trailing slash", rawOut, out)
            } else {
                logger.println("Query does not start with $urlBase:  $q")
                return null
            }
        }
        q = q.substring(urlBase.length)
        val f = File(baseDir, q)
        try {
            if (!f.canonicalPath.startsWith(baseDir.canonicalPath)) {
                logger.println("Rejected query outside directory:  "
                        + f + " (" + f.canonicalPath + ")")
                return ErrorQueryHandler("Illegal directory", rawOut, out)
            } else if (!f.exists()) {
                logger.println("File does not exist:  " + f)
                return ErrorQueryHandler("File does note exist:  " + f, rawOut, out)
            }
            if (f.isDirectory) {
                return DirectoryQuery(publicURL, baseDir, f, rawOut, out, enableUpload)
            } else {
                return FileQuery(f, rawOut, out, this)
            }
        } catch (ex: IOException) {
            logger.println("Query:  " + q)
            logger.println("error:  " + ex)
            return ErrorQueryHandler(ex.toString(), rawOut, out)
        }

    }

    override fun handlePost(headers: HashMap<String, String>, input: BufferedInputStream) {
        if (!enableUpload) {
            logger.println("POST error:  Uploads disabled")
            return;
        }
        val fileName = headers["X-File-Name"]
        val contentLength = headers["Content-Length"]?.toLong()
        val contentType = headers["Content-Type"]
        if (fileName == null) {
            logger.println("POST error:  No file name")
            return;
        }
        if (contentLength == null) {
            logger.println("POST error:  No content length")
            return;
        }
        if (contentType != "application/octet-stream") {
            logger.println("POST warning:  contentType is $contentType, not application/octet-stream")
            logger.println("I'll upload it, but you get what you get.")
        }
        val dir = if (uploadsSuffix == "") {
            baseDir
        } else {
            File(baseDir, uploadsSuffix)
        }
        dir.mkdirs();
        val outFile = File(dir, fileName);
        logger.println("POST data uploading $contentLength bytes to ${outFile.getCanonicalPath()}")
        val out = BufferedOutputStream(FileOutputStream(outFile))
        try {
            val buffer = ByteArray(1024 * 1024)
            var remaining = contentLength;
            val dotEvery = 10 * 1024 * 1024;
            var dotCount = 0;
            while (true) {
                if (remaining <= 0) {
                    logger.println("Upload complete");
                    break;
                }
                val limit = if (remaining < buffer.size) remaining.toInt() else buffer.size;
                val read = input.read(buffer, 0, limit)
                if (read == -1) {
                    logger.println("Upload error:  EOF with $remaining bytes left to read")
                    break;
                }
                remaining -= read;
                out.write(buffer, 0, read);
                dotCount += read;
                while (dotCount > dotEvery) {
                    print(".");
                    System.out.flush()
                    dotCount -= dotEvery;
                }
            }
        } finally {
            out.close()
        }
    }

    val publicURL: String @Throws(IOException::class)
        get() {
            val scheme = if (enableSsl) "https" else "http"
            return scheme + "://" + localInetAddress.hostAddress + ":" + port + urlBase
        }

}

