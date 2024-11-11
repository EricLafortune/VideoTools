# Video tools for the TI-99/4A

These command-line tools convert, manipulate, compress and combine images,
animations, music, and speech in videos for the TI-99/4A home computer. They
produce an optimized video player that essentially sends a stream of bytes to
the video processor (TMS9918), the sound processor (TMS9919/SN76489), and the
speech synthesizer (TMS5200).

## Requirements

* A Java runtime environment, version 14 or higher.
* The [xdt99](https://github.com/endlos99/xdt99) cross-development tools, for
  the `xas99` assembler.
* A TI-99/4A home computer with a programmable ROM cartridge, or an emulator
  for the TI-99/4A, such as Mame.

## Downloading

You can download the latest binary version from Github:

* A [jar file](https://github.com/EricLafortune/VideoTools/releases/latest) of
  the video tools.
* The [source
  code](https://github.com/EricLafortune/VideoTools/tree/master/player/src)
  of the video player.

## Building

If you prefer, you can build the tools and the player yourself.
On Linux, you can run the build script:

    ./build.sh

Alternatively, you can run its commands manually.

You'll then have
* the video tools library `tools/out/videotools.jar`,
* a raw cartridge ROM file `player/out/romc.bin` with a small demo video.
* a corresponding cartridge file `player/out/video.rpk` that is suitable for
  emulators like Mame.

## Creating a video and a video player

### Environment

You can run the tools by adding the video tools jar to your Java class path.
For example, in a Linux shell:

    export CLASSPATH=tools/out/videotools.jar

### Preparing display animations

The canonical input formats for visuals are PBM, GIF, PNG or BMP for static
images, and ZIP for more dynamic sequences of images. You can create these
with external tools like Gimp, ImageMagick, `ffmpeg`, etc.

The target image frame rate for the video player is fixed at 30 fps on US
systems and 25 fps on European systems.

### Preparing music

The canonical input format for music is our optimized
[SND format](docs/SndFileFormat.md) for the TMS9919/SN76489 processors. The
target sound frame rate for the video player is fixed at 60 fps on US
systems and 50 fps on European systems.

* [ConvertMusicXmlToSnd](docs/ConvertMusicXmlToSnd.md): convert a MusicXML
  file to an SND file.
* [ConvertVgmToSnd](docs/ConvertVgmToSnd.md): convert a VGM file to an SND
  file.
* [CutSndFile](docs/CutSndFile.md): copy a section from an SND file.
* [SimplifySndFile](docs/SimplifySndFile.md): simplify the sound commands in
  an SND file.
* [TransposeSndFile](docs/TransposeSndFile.md): transpose the frequencies in
  an SND file.
* [ConvertSndToWav](docs/ConvertSndToWav.md): convert an SND file to a WAV
  file, by simulating a sound chip.

### Preparing speech and vocals

The canonical input format for speech and vocals is our binary
[Linear Predictive Coding (LPC) format](docs/LpcFileFormat.md) for the TMS52xx
speech synthesizer.

* [ConvertWavToLpc](docs/ConvertWavToLpc.md): convert a WAV file to a binary
  LPC file.
* [ConvertPraatToLpc](docs/ConvertPraatToLpc.md): convert Praat Pitch/LPC files
  to a binary LPC file.
* [ConvertMusicXmlToLpc](docs/ConvertMusicXmlToLpc.md): convert a MusicXML
  file to an LPC file humming to the tune.
* [CutLpcFile](docs/CutLpcFile.md): copy a section from a binary LPC file.
* [TuneLpcFile](docs/TuneLpcFile.md): tune a binary LPC speech file to the
  frequencies in a synchronized SND music file.
* [ConvertLpcToText](docs/ConvertLpcToText.md): convert a binary LPC file to a
  text LPC file.
* [ConvertTextToLpc](docs/ConvertTextToLpc.md): convert a text LPC file to a
  binary LPC file.
* [ConvertLpcToWav](docs/ConvertLpcToWav.md): convert an LPC file to a WAV
  file, by simulating a speech synthesis chip.

### Composing final videos

The final format suitable for our player is our optimized
[TMS format](docs/TmsFileFormat.md). It contains the combination of animations,
music, and speech.

* [ComposeVideo](docs/ComposeVideo.md): merge and compress images, animations,
  music, and speech in the above formats in a TMS file.

### Packaging videos in cartridge ROMs

You can package the video file along with a standard player in a standard
ROM (with multiple memory banks) for the TI-99/4A.

* [PackageVideoInCartridge](docs/PackageVideoInCartridge.md): package a TMS
  video file in an RPK cartridge or BIN ROM.

Alternatively, if you want more control over the video player, you can
assemble it yourself with the [xdt99](https://github.com/endlos99/xdt99) tools.
The video file gets hardcoded inside the resulting binary.

    cd player
    xas99.py --register-symbols --binary --output out/romc.bin src/player.asm

The frame rate of the player is synchronized to the vertical sync of the
display, which is 60 Hz on US systems and 50 Hz on European systems.

## Example

By default, the video player in this project is assembled with a small demo
video, consisting of a static title screen and a speech snippet ("hello").
You can see how it is created in `player/build.sh`.

## Running the video player

The easiest way is to use an emulator, such as Mame.

On Linux, you can run the script to launch Mame with the proper options:

    ./run.sh

Alternatively, you can run the Mame command manually.

Once Mame is running and showing the TI-99/4A home screen:

1. Press any key.
2. Press <kbd>2</kbd> for "VIDEO WITH SPEECH" or <kbd>3</kbd> for "VIDEO
   WITHOUT SPEECH".

You can exit Mame by pressing <kbd>Scroll Lock</kbd> and then <kbd>Esc</kbd>.

## Larger example

My open source [Bad Apple demo](https://github.com/EricLafortune/BadApple/)
for the TI-99/4A creates a complete video. The build script uses many of the
video tools.

* It transforms the original Bad Apple video, music and vocals in traditional
  formats (.webm, .wav) to raw data files in our optimized formats (.zip, .snd,
  .lpc).
* It creates animated titles and credits.
* It then combines these raw data files to a raw TMS video file (.tms).
* Finally, it assembles the player with the raw video file to a module file
  for the TI-99/4A (.bin, .rpk).

## License

The video tools are released under the GNU General Public License, version 2.

Enjoy!

Eric Lafortune
