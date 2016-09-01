

This directory makes a quick-and-dirty keystore with a self-signed
cert.  This allows simples to implement https, though of course the
browser will complain about the cert.

run.sh makes the keystore, then runs a quick program to make it into a
Kotlin byte array constant, so it can be hard-coded into the program
with copy-paste.  see ../src/KeyStore.kt

I kind of like the idea of generating a self-signed cert when running
the program, so I switched to that (by calling keytool at runtime).
Also, making this into a proper build would mean I'd either have to
mess around with ant or learn gradle, which seems like overkill.

Bill Foote
8/30/16

