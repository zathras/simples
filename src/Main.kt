/**
 * Created by w.foote on 3/31/2016.
 */

import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.security.SecureRandom

import server.ErrorQueryHandler
import server.QueryHandler
import server.QueryListener
import java.io.FileInputStream

private fun usage() {
    System.err.println()
    System.err.println("Usage:  java -jar simples.jar [-u] [-s] [-p port] dir [prefix]")
    System.err.println("         -u       Enable uploads")
    System.err.println("         -s       https: (insecure self-signed certificate)")
    System.err.println("         -p port  Set port (default 6001)")
    System.err.println("         dir      Directory to serve")
    System.err.println("         prefix   Use fixed prefix instead of random number.  \"/\" for no prefix.")
    System.err.println()
    System.err.println("If no prefix is specified, a random number will be used.  If you're on an open")
    System.err.println("network, the prefix makes it unlikely an attacker will be able to do anything.")
    System.err.println("")
    System.err.println("You might need to configure your firewall to allow incoming connections.")
    System.err.println("For example, on Ubuntu Linux, I use \"ufw allow 6001/tcp\".")
    System.err.println("Remember to deny access when you're done!")
    System.err.println("")
    System.exit(1)
}

fun main(args:  Array<String>) {
    var enableUpload = false;
    var port = 6001
    var urlBase = ""
    var argsUsed = 0;
    var enableSsl = false;
    while (args.size > argsUsed && args[argsUsed].startsWith("-")) {
        if (args[argsUsed] == "-u") {
            enableUpload = true;
            argsUsed++;
        } else if (args[argsUsed] == "-s") {
            enableSsl = true;
            argsUsed++;
        } else if (args[argsUsed] == "-p" && args.size > argsUsed+1) {
            argsUsed++;
            try {
                port = args[argsUsed++].toInt();
            } catch (ex: NumberFormatException) {
                System.err.println("Bad port number:  " + ex)
                usage();
            }
        } else {
            usage();
        }
    }
    if (argsUsed >= args.size || argsUsed < args.size - 2) {
        usage();
    } else {
        if (args.size == argsUsed + 2) {
            if (args[argsUsed+1] == "/") {
                urlBase = "/"
            } else {
                urlBase = "/" + args[argsUsed + 1] + "/"
            }
        } else {
            val sr = SecureRandom()
            urlBase = "/" + sr.nextInt(10000000) + "/"
        }
    }
    val baseDir = File(args[argsUsed+0])
    if (enableSsl) {
        generateSslKeystore(localInetAddress.hostAddress)
    }

    val sh = SimpleHttp(baseDir, urlBase, port, enableUpload, enableSsl)
    println()
    println("Serving files from " + baseDir.canonicalPath
            + " at " + sh.publicURL)
    println()
    println("If you have a problem with port blocking in a browser, see the class header")
    println("in SimpleHttp.java.  You can always use curl, too.")
    println()
    sh.run()
}
