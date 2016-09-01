package server

import java.io.OutputStream
import java.io.PrintWriter

import java.net.URLEncoder
import java.io.UnsupportedEncodingException

/**

 * @author      Bill Foote
 */


class ErrorQueryHandler(private val message: String, rawOut: OutputStream, out: PrintWriter) : QueryHandler(rawOut, out) {

    override fun run() {
        startHttpResult(404)
        startHtml("Error")
        safePrint(message)
        endHtml()
    }

}
