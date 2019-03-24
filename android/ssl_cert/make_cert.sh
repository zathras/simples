#!/bin/bash -x
cd `dirname $0`
OUT=../app/src/main/assets/simples.pkcs12
keytool -genkey -dname "CN=simples, OU=None, O=None, L=None, ST=None, C=None" \
    -keyalg RSA -alias self -keystore $OUT -storepass simples \
    -keypass simples -validity 7200 -keysize 2048 \
    -deststoretype PKCS12
