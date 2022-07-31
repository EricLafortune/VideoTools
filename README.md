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
  of the video player, to be assembled with a video.

## Building

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

The canonical input format for music is our optimized TMS9919/SN76489 Sound
format. The target sound frame rate for the video player is fixed at 60 fps
on US systems and 50 fps on European systems.

**ConvertVgmToSnd**: convert a file in [Video Game
Music](https://vgmrips.net/wiki/VGM_Specification) format to a file in
our SND format.

    java ConvertVgmToSnd [frame_time] input.vgm output.snd

where
* `frame_time` is the time between frames of the output SND file, expressed in
  the VGM sample delta of 1/44100 s. The default is 735 for 60 fps on US
  systems. A common alternative is 882 for 50 fps on European systems. You can
  tweak the playback speed with other values.  
* `input.vgm` is the input VGM file.
* `output.snd` is the output SND file.

**CutSndFile**: copy a section from a file in our SND format.

    java CutSndFile input.snd start_frame stop_frame output.snd

where
* `input.snd` is the input SND file.
* `start_frame` is the start frame in the input file.
* `stop_frame` is the end frame (exclusive) in the input file.
* `output.snd` is the output SND file.

**SimplifySndFile**: simplify the sound commands in a SND file. The
TMS9919/SN76489 commands set the frequencies and volumes of the tone/noise
generators. Frames may specify them redundantely, so cleaning them up can
reduce the size of the sound file.

    java SimplifySndFile input.snd output.snd

where
* `input.snd` is the input SND file.
* `output.snd` is the output SND file.

### Preparing speech

The canonical input format for speech and vocals is the binary
Linear Predictive Coding (LPC) format for the TMS52xx speech synthesizer. 

**ConvertLpcToText**: convert a file in binary LPC format to a file in a
simple text format. The text format is more conveniently viewable and
editable, for instance to tweak speech frequencies or frame repeats.

    java ConvertLpcToText input.lpc output.txt

where
* `input.lpc` is the input binary LPC file.
* `output.txt` is the output text LPC file.

**ConvertTextToLpc**: convert a text file with Linear Predictive Coding (LPC)
data to a binary LPC file.

    java ConvertTextToLpc input.txt output.lpc

where
* `input.txt` is the input text LPC file.
* `output.lpc` is the output binary LPC file.

**ConvertPraatToLpc**: convert a pair of files in Praat Pitch format and in
Praat LPC format to a file in the binary LPC format for the TMS52xx speech
synthesizers. You can thus let Praat perform the pitch analysis and LPC
analysis and convert the results to our LPC format with this tool.

    java ConvertPraatToLpc [-tms5200|-tms5220] [-8kHz|-10kHz] [-addstopframe] input.Pitch input.LPC unvoiced_energy_factor voiced_energy_factor output.lpc

where
* `-tms5200` or `-tms5220` specifies the target speech synthesizer. The default
  is TMS5200.
* `-8kHz` or `-10kHz` specifies the target sample rate. The default is 8 kHz.
* `-addstopframe` optionally specifies to add a stop frame to the output LPC
  frames.
* `input.Pitch` is the pitch input file in the short text format produced by
  Praat.
* `input.LPC` is the LPC input file in the short text format produced by Praat.
* `unvoiced_energy_factor` is a scaling factor for the energy of unvoiced
  frames. A value around 0.5 may work reasonably well.
* `voiced_energy_factor` is a scaling factor for the energy of voiced frames.
  A value around 0.5 may work reasonably well.
* `output.lpc` is the output binary LPC file.

**CutLpcFile**: copy a section from a file in binary LPC format.

    java CutLpcFile [-addstopframe] input.lpc start_frame stop_frame output.lpc

where
* `input.lpc` is the input binary LPC file.
* `start_frame` is the start frame in the input file.
* `stop_frame` is the end frame (exclusive) in the input file.
* `output.lpc` is the output binary LPC file.

**TuneLpcFile**: tune the frequencies of a binary LPC speech file to the
frequencies of tone generator 1 in a synchronized SND music file.

    java TuneLpcFile input.lpc[,start_frame[,stop_frame]] input.snd[,start_frame] [frequency_factor [min_frequency [max_frequency]]] output.lpc

where
* `input.lpc` is the input binary LPC file.
* `input.snd` is the input SND file.
* `start_frame` is the optional start frame in the input file.
* `stop_frame` is the optional end frame (exclusive) in the input file.
* `frequency_factor` is the optional factor to scale the frequencies of the
  sound file.
* `min_frequency` is the optional minimum allowed frequency of in the output
  file. Any lower frequencies are doubled.
* `max_frequency` is the optional maximum allowed frequency of in the output
  file. Any higher frequencies are halved.
* `output.lpc` is the output binary LPC file.

The target speech frame rate for the video player is fixed at 40 fps.

### Composing final videos

**ComposeVideo**: merge and compress images, animations, music, and speech in
a single video file in our TMS format, optimized for streaming to the TMS
processors.

    java ComposeVideo [start_frame:]input.{pbm,gif,png,bmp,zip,vgm,snd,lpc}[,skip_frames] ... output.{tms,asm}

where

* `-50Hz` specifies a European target system with a display refresh of 50 Hz
  (only for properly synchronizing the speech bitstream, fixed at 40 fps).
* `-60Hz` specifies a US target system with a display refresh of 60 Hz
 (only for properly synchronizing the speech bitstream, fixed at 40 fps).
* `start_frame` is the target offset of the input file in the final video
  file.
* `input.xxx` is an input file in one of the supported formats. A zip file
  should contain a sequence of image frames.
* `skip` is the optional number of leading frames to skip in the input file.
  The default is to start from the start of the file.
* `output.tms` (or `output.asm`) is the resulting video file, in binary format
  (or in text format for the `xas99` assembler).

You can then assemble the player and the video file in a single cartridge file
for the TI-99/4A:

    xas99 --register-symbols --binary --output out/romc.bin player/src/player.asm

The frame rate of the player is synchronized to the vertical sync of the
display, which is 60 Hz on US systems and 50 Hz on European systems.

## Example

By default, the video player in this project is assembled with a small demo
video, consisting of a static title screen and a speech snippet ("hello").
You can see how it is created in `player/build.sh`. 

## Running the video player

The easiest way is to use the Mame emulator.

On Linux, you can run the script to launch Mame with the proper options:

    ./run.sh

Alternatively, you can run the Mame command manually. 

Once Mame is running and showing the TI-99/4A home screen:

1. Press any key.
2. Press `1` for "VIDEO" or `2` for "VIDEO WITH SPEECH".

You can exit Mame by pressing `Insert` and then `Esc`.

## Larger example

My open source [Bad Apple demo](https://github.com/EricLafortune/BadApple/)
for the TI-99/4A creates a complete video. The build script uses all the
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
