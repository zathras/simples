

This directory contains an implementation of generating a self-signing
cert using BouncyCastle.  It works fine, but I was annoyed at bundling
4MB worth of BouncyCastle library just for an insecure self-signed SSL
cert.  Also, it uses a deprecated API to generate the cert; the new
X509 cert builder API is in yet another BouncyCastle JAR.


Bill Foote
8/30/16
