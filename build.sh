#!/bin/sh
#
# This script compiles the video tools and assembles the player with a small
# demo.

cd $(dirname "$0") \
&& tools/build.sh \
&& player/build.sh
