# ConvertVgmToSnd

## Summary

Extracts any TMS9919/SN76489 sound data from a chip tune in [Video Game
Music](https://vgmrips.net/wiki/VGM_Specification) format to a file in our
binary [SND format](SndFileFormat.md), for replay with TMS9919/SN76489 sound
chips.

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

    java ConvertVgmToSnd 735 music.vgm music.snd

For efficiency, you can simplify the SND file, and at the same time make sure
all sound generators are silenced at the end:

    java SimplifySndFile -addsilencecommands music.snd simplified.snd

You can convert the SND commands to a WAV sound file, in order to quickly check
the quality of the results in any audio player:

    java ConvertSndToWav -framefrequency NTSC simplified.snd music.wav

If you're happy with the results, you can embed the SND commands in an
executable cartridge for the TI-99/4A. First create a TMS video file, in this
example only containing the music:

    java ComposeVideo -ntsc simplified.snd music.tms

Then package the video file with a small default player program in a cartridge:

    java PackageVideoInCartridge -title 'MY MUSIC' music.tms music.rpk

Instead of an RPK file `music.rpk` for the Mame emulator, you can also create
just a raw ROM file like `romc.bin`, for other emulators.

You can then run the application from a RAM cartridge on the computer or in an
emulator like Mame:

    mame ti99_4a -ioport peb -ioport:peb:slot3 speech -cart1 music.rpk

## Related tools

* [SimplifySndFile](SimplifySndFile.md)
* [CutSndFile](CutSndFile.md)
* [ConvertMusicXmlToSnd](docs/ConvertMusicXmlToSnd.md)
* [ComposeVideo](ComposeVideo.md)
* [PackageVideoInCartridge](PackageVideoInCartridge.md)
* [VideoTools](../README.md)
