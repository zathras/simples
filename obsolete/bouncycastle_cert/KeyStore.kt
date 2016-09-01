
/**
 * Trivial keystore.  This hard-coded keystore contains a self-signed
 * certificate, for not.very.secure.com.  See ../ssl for the code that
 * generates it.  This is enough to make SSL work, if you don't mind
 * adding a security exception to your browser.  That's good enough, if
 * you goal is just so someone with a wifi snooper can't see your
 * traffic in the clear.
 */
import org.bouncycastle.jce.X509Principal
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.x509.X509V3CertificateGenerator
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
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
fun generateSslKeystore() {
    println("Generating self-signed certificate...")
    Security.addProvider(BouncyCastleProvider())
    val ks = KeyStore.getInstance(KeyStore.getDefaultType())
    ks.load(null, "simples".toCharArray())      // Initializes empty keystore

    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    val keyPair = keyPairGenerator.generateKeyPair()
    val certGen = X509V3CertificateGenerator()
    certGen.setSerialNumber(BigInteger.valueOf(SecureRandom().nextInt(Integer.MAX_VALUE).toLong()))
    val domain = "not.terribly.secure.com"
    certGen.setIssuerDN(X509Principal("CN=$domain, OU=None, O=None, L=None, ST=None, C=None"))
    certGen.setNotBefore(Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30))  // a month ago
    val years = 1000
    certGen.setNotAfter(Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * years))
    certGen.setSubjectDN(X509Principal("CN=$domain, OU=None, O=None, L=None, ST=None, C=None"))
    certGen.setPublicKey(keyPair.getPublic())
    certGen.setSignatureAlgorithm("SHA1WithRSAEncryption")
    val cert : Certificate = certGen.generateX509Certificate(keyPair.getPrivate())
    ks.setKeyEntry("self", keyPair.getPrivate(), "simples".toCharArray(), arrayOf(cert))

    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(ks, "simples".toCharArray())
    val sslc = SSLContext.getInstance("TLS")
    sslc.init(kmf.getKeyManagers(), null, SecureRandom())
    SSLContext.setDefault(sslc)
    println("Generated self-signed certificate.")
}
