#!/bin/sh
#
# This script compiles the video tools and creates a jar archive with the
# result.

cd $(dirname "$0") \
&& mkdir -p out \
&& javac -sourcepath src -d out -source 14 -target 14 \
     src/*.java src/*/*.java \
&& cd out \
&& jar -cf videotools.jar \
     *.class */*.class \
|| exit 1
