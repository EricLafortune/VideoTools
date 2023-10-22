# ConvertVgmToSnd

## Summary

Extracts any TMS9919/SN76489 sound data from a file in [Video Game
Music](https://vgmrips.net/wiki/VGM_Specification) format to a file in our
binary [SND format](SndFileFormat.md). Our SND format is optimized for replay
with TMS9919/SN76489 sound chips.

## Usage

    java ConvertVgmToSnd [frame_time] input.vgm output.snd

where:

|               |                                                                                                                                                                                                                                                              |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| _frame_time_  | The time between frames of the output SND file, expressed in the VGM sample delta of 1/44100 s. The default is 735 for 60 fps on US systems. A common alternative is 882 for 50 fps on European systems. You can tweak the playback speed with other values. |
| _input.vgm_   | The input music file in VGM format.                                                                                                                                                                                                                          |
| _output.snd_  | The output file in SND format.                                                                                                                                                                                                                               |

## Example workflow

Convert a VGM file to an SND file, targeting 60 fps:

    java ConvertVgmToSnd 735 input.wav output.snd

For efficiency, you can simplify the SND file, and at the same time make sure 
all sound generators are silenced at the end:

    java SimplifySndFile -addsilencecommands output.snd simplified.snd

You can then embed the SND data in an executable cartridge for the TI-99/4A.
First create a video file, in this case only containing the music:

    java ComposeVideo simplified.snd player/data/video.tms
    
Then assemble the player with the video file embedded (with the
[xdt99](https://github.com/endlos99/xdt99) tools), and package the code as an
RPK cartridge file:

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

* [SimplifySndFile](SimplifySndFile.md)
* [CutSndFile](CutSndFile.md)
* [ComposeVideo](ComposeVideo.md)
* [VideoTools](../README.md)
