package server


import java.io.OutputStream
import java.io.PrintWriter
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**

 * @author      Bill Foote
 */


abstract class QueryHandler(protected val rawOut: OutputStream, protected val out: PrintWriter) {

    abstract fun run()

    protected open fun writeAdditionalHeaders() {
    }

    protected fun encodeForURL(s: String): String {
        try {
            return URLEncoder.encode(s, "UTF-8")
        } catch (ex: UnsupportedEncodingException) {
            // Should never happen
            ex.printStackTrace()
        }

        return s
    }

    // 
    // Send out the http result code.  200 means "good," 404 means "bad."
    //
    protected fun startHttpResult(code: Int) {
        out.println("HTTP/1.1 $code OK")
        out.println("Cache-Control: no-cache")
        out.println("Pragma: no-cache")
        writeAdditionalHeaders()
        out.println()
    }

    protected fun startHtml(title: String) {
        out.println("<html>")
        out.println("<head>")
        out.println("""<meta http-equiv="Content-Type" content="text/html; charset=utf-8">""")
        out.println("</head>")
        out.print("<title>")
        safePrint(title)
        out.println("</title>")
        out.println("<body bgcolor=\"#ffffff\"><center><h1>")
        safePrint(title)
        out.println("</h1></center>")
    }

    protected fun endHtml() {
        out.println("</body></html>")
    }

    protected fun safePrint(str: String) {
        for (i in 0..str.length - 1) {
            val ch = str[i]
            if (ch == '<') {
                out.print("&lt;")
            } else if (ch == '>') {
                out.print("&gt;")
            } else if (ch == '"') {
                out.print("&quot;")
            } else if (ch == '&') {
                out.print("&amp;")
            } else if (ch < ' ') {
                // do nothing
            } else {
                out.print(ch)
            }
        }
    }

}
