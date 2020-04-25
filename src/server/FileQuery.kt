package server
import java.io.*

class FileQuery(var file: File, rawOut: OutputStream, out: PrintWriter,
                val parent: SimpleHttp) : QueryHandler(rawOut, out) 
{

    override fun writeAdditionalHeaders() {
        val l = file.length()
        if (l > 0) {
            out.println("Content-Length: " + l + "\r")
        }
    }

    override fun run() {
        var started = false
        var written: Long = 0
        try {
            val `is` = FileInputStream(file)
            val buffer = ByteArray(256 * 256)
            while (true) {
                val len = `is`.read(buffer)
                if (!started) {
                    started = true
                    startHttpResult(200)
                    out.flush()
                }
                if (len == -1) {
                    break
                }
                rawOut.write(buffer, 0, len)
                written += len.toLong()
            }
            parent.logger.println("$file:  Wrote $written bytes.")
            rawOut.flush()
            `is`.close()
        } catch (ex: IOException) {
            ex.printStackTrace()
            startHttpResult(404)
            startHtml("I/O Error")
            safePrint(ex.toString())
            endHtml()
        }

    }
}
