#!/bin/sh
#
# This script compiles the video tools and assembles the player with a small
# demo.

cd $(dirname "$0") \
&& tools/build.sh \
&& player/build.sh \
&& zip -q --junk-paths  tools/out/videotools.jar \
     player/layout.xml \
     player/out/romc.bin \
&& zip -q tools/out/videotools.jar \
     hum/*.txt \
|| exit 1
