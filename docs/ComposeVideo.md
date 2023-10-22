# ComposeVideo

## Summary

Merges and compresses images, animations, music, and speech in a single video
file in our binary [TMS format](TmsFileFormat.md). The TMS format is optimized
and encoded for replay with TMSxxxx chips:

* the video display processor TMS9918
* the sound processor TMS9919/SN76489
* the speech synthesizer TMS5200
  
The video tools contain a player for the TMS format, for the TI-99/4A.

## Usage

    java ComposeVideo [options] [start_frame:]input.{pbm,gif,png,bmp,zip,vgm,snd,lpc}[,skip_frames] ... output.{tms,asm}

where:

|               |                                                                                             |
|---------------|---------------------------------------------------------------------------------------------|
| _input.pbm_   | An input image file in PBM format, 256x192 pixels, 2 colors.                                |
| _input.gif_   | An input image file in GIF format, 256x192 pixels, 16 colors.                               |
| _input.png_   | An input image file in PNG format, 256x192 pixels, 16 colors.                               |
| _input.bmp_   | An input image file in BMP format, 256x192 pixels, 16 colors.                               |
| _input.zip_   | A sequence of the above image files in ZIP format.                                          |
| _input.vgm_   | An input music file in VGM format.                                                          |
| _input.snd_   | An input music file in our [SND format](SndFileFormat.md).                                  |
| _input.lpc_   | An input speech file in our [binary LPC format](LpcFileFormat.md).                          |
| _start_frame_ | A target offset for the input file in the final video file, expressed in video frames.      |
| _skip_frames_ | An optional number of leading frames to skip in the input file.                             |
| _output.tms_  | The output video file containing the encoded streams in our [TMS format](TmsFileFormat.md). |
| _output.asm_  | The output video file containing the encoded streams in the text ASM format.                |

The options are:

|         |                                                                   |
|---------|-------------------------------------------------------------------|
| `-60Hz` | Target a US system with a display refresh of 60 Hz (the default). |
| `-50Hz` | Target a European system with a display refresh of 50 Hz.         |
 
These options are only relevant for properly synchronizing the speech 
bitstream, which is always fixed at 40 fps.

## Example workflow

Convert a VGM music file to an SND file, targeting 60 fps
(see [ConvertVgmToSnd](ConvertVgmToSnd.md)):

    java ConvertVgmToSnd 735 music.wav music.snd

Convert a WAV sound file to an LPC speech file
(see [ConvertWavToLpc](ConvertWavToLpc.md)):

    java ConvertWavToLpc -tms5200 -minfrequency 50 -maxfrequency 250 speech.wav speech.lpc

You can then combine animations, the music, and the speech in a single video file:

    java ComposeVideo animation.zip 100:music.snd 200:speech.lpc player/data/video.tms
    
In this example, we're starting the music at frame 100 and the speech at
frame 200. You can start multiple animations, music pieces, and speech fragments
at different times.    
    
Finally, you can assemble the player with the video file embedded (with the
[xdt99](https://github.com/endlos99/xdt99) tools), and package the code as an
RPK cartridge file for the TI-99/4A:

    cd player
    xas99.py --register-symbols --binary --output out/romc.bin src/player.asm
    zip -q --junk-paths out/video.rpk layout.xml out/romc.bin

You can run such an RPK file in the Mame emulator:

    mame ti99_4a \
      -nomouse -window -resolution 1024x768 -nounevenstretch \
      -ioport peb \
      -ioport:peb:slot3 speech \
      -cart1 out/video.rpk

## Related tools

* [ConvertVgmToSnd](ConvertVgmToSnd.md)
* [ConvertWavToLpc](ConvertWavToLpc.md)
* [ConvertPraatToLpc](ConvertPraatToLpc.md)
* [VideoTools](../README.md)
