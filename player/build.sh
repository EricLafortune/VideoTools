#!/bin/sh
#
# This script assembles the video player (including the video itself) and
# creates an RPK cartridge image with the result.
#
# We're packaging the output as an RPK for Mame,
# using the ROM naming convention for FinalGROM 99.
#
# Useful xas99 option for debugging:
#   --listing-file out/romc.lst

export CLASSPATH=../tools/out/videotools.jar

cd $(dirname "$0") \
&& mkdir -p out \
&& java ConvertTextToLpc data/hello.txt data/hello.lpc \
&& java ComposeVideo data/title.png data/hello.lpc data/video.tms \
&& xas99.py --register-symbols --binary --output out/romc.bin src/player.asm \
&& rm -f out/video.rpk \
&& zip -q --junk-paths out/video.rpk layout.xml out/romc.bin
