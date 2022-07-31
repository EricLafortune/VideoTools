#!/bin/sh
#
# This script compiles the video tools and creates a jar archive with the
# result.

cd $(dirname "$0") \
&& mkdir -p out \
&& javac -sourcepath src -d out -source 14 -target 14 \
    $(find src -name \*.java) \
&& jar -cf out/videotools.jar \
    $(find out -name \*.class -printf '-C out %P\n') \
|| exit 1
