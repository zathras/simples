
/**
 * Trivial keystore.  This hard-coded keystore contains a self-signed
 * certificate, for not.very.secure.com.  See ../ssl for the code that
 * generates it.  This is enough to make SSL work, if you don't mind
 * adding a security exception to your browser.  That's good enough, if
 * you goal is just so someone with a wifi snooper can't see your
 * traffic in the clear.
 */
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.security.SecureRandom
import java.security.KeyStore
import java.security.Security
import java.security.cert.Certificate
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

/**
 *  Make keystore with a self-signed certificate, using the BouncyCastle library.
 *  Set the SSLContext to use it.
 *  h/t http://blog.thilinamb.com/2010/01/how-to-generate-self-signed.html
 */
fun generateSslKeystore(hostName : String) {
    println("Generating self-signed certificate with keytool...")
    val dir = Files.createTempDirectory("simples").toFile()
    val ksFile = File(dir, "simples.jks")
    val pb = ProcessBuilder("keytool", "-genkey", "-dname",
            "CN=$hostName, OU=None, O=None, L=None, ST=None, C=None",
            "-keyalg", "RSA", "-alias", "self",
            "-keystore", ksFile.canonicalPath,
            "-storepass", "simples", "-keypass", "simples",
            "-validity", "3600",
            "-keysize", "2048")
    pb.inheritIO();
    val proc = pb.start()
    proc.waitFor()
    println("Generated self-signed certificate.")

    val ks = KeyStore.getInstance(KeyStore.getDefaultType())
    val input = BufferedInputStream(ksFile.inputStream())
    ks.load(input, "simples".toCharArray())
    input.close()
    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(ks, "simples".toCharArray())
    val sslc = SSLContext.getInstance("TLS")
    sslc.init(kmf.getKeyManagers(), null, SecureRandom())
    SSLContext.setDefault(sslc)
    if (!ksFile.delete()) {
        println("Warning:  Could not delete $ksFile")
    }
    if (!dir.delete()) {
    }
}
