#!/bin/bash -x
rm -f self.jks
echo "" | keytool -genkey -dname "CN=not.very.secure.com, OU=None, O=None, L=None, ST=None, C=None" -keyalg RSA -alias self -keystore self.jks -storepass simples -validity 360000 -keysize 2048
echo ""
echo -n "    // self-signed keystore auto-generated " > out
date >> out
echo "    //" >> out
echo "    // See ../ssl/run.sh" >> out
echo "    //" >> out
kotlinc -script convert.kts < self.jks >> out
rm self.jks
