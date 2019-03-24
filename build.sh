#!/bin/bash -x
cd `dirname $0`
rm -rf out
mkdir -p out
kotlinc -include-runtime -d out/simples.jar `find src -name '*.kt' -print`
echo "Note to self:  cf. ~/lib/simples.jar"
