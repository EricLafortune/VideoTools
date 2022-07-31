#!/bin/sh
#
# This script launches Mame with the TI-99/4A, the speech synthesizer, and
# the demo video cartridge mounted.


RPK=player/out/video.rpk

if [ ! -f $RPK ]
then
  echo "Can't find the cartridge image $RPK"
  echo "Please build it with the build.sh script"
  exit 1
fi

mame ti99_4a \
  -nomouse -window -resolution 1024x768 -nounevenstretch \
  -ioport peb \
  -ioport:peb:slot3 speech \
  -cart1 $RPK
