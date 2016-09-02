
/* Read stdin, and convert it to a Kotlin byte array declaration.  */

print("    val keystore = byteArrayOf(");
var i = 0;
while (true) {
    val c = System.`in`.read()
    if (c == -1) {
        break;
    }
    if (i > 0) {
        print(", ")
    }
    if (i % 8 == 0) {
        println()
	print("            ")
    }
    print(c.toByte())
    i++
}
println()
println("    )")
