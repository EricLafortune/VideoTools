# ConvertMusicXmlToLpc

## Summary

Converts a music sheet in MusicXML format to a file humming to the tune, in our
binary [LPC format](LpcFileFormat.md) for the TMS52xx speech synthesis chips.
More precisely, the output is "singing with non-lexical vocables", such as
"la la la" or "tee tee tee".

## Usage

    java ConvertMusicXmlToLpc [options...] input.{mxl,musicxml,xml} output.lpc

where:

|                           |                                                               |
|---------------------------|---------------------------------------------------------------|
| _input.{mxl,musicxml,xml} | The input file in (possibly zipped) MusicXML format.          |
| _output.lpc_              | The output file in raw binary [LPC format](LpcFileFormat.md). |

The options are:

|                            |                                                                                                                                                                                                                                                           |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-startmeasure` _number_   | Start humming at the specified measure of the music sheet (default = 1).                                                                                                                                                                                  |
| `-endmeasure` _number_     | Stop humming at the specified measure of the music sheet (default = last measure).                                                                                                                                                                        |
| `-speed` _factor_          | Increase or decrease the beats per minute of the music sheet by the given factor (default = 1).                                                                                                                                                           |
| `-part` _name_             | Hum the specified part, given as a part ID, a part name, a part abbreviation, or an instrument name (default = P1).                                                                                                                                       |
| `-voice` _number_          | Hum the specified voice number of the part (default = 1).                                                                                                                                                                                                 |
| `-staff` _number_          | Hum the specified staff number of the part (default = -1, undefined).                                                                                                                                                                                     |
| `-attenuate` _attenuation_ | Attenuate the humming down (or maybe up) by the given integer number of decibels (speech synthesizer energy units, default = 0).                                                                                                                          |
| `-transpose` _octaves_     | Transpose the humming up (or down) by the given integer number of octaves (default = 0).                                                                                                                                                                  |
| `-hum` _name_              | Hum with the specified hum: one of `ah`, `aye`, `be`, `du`, `goo`, `ho`, `la`, `li`, `me`, `moo`, `oh`, `pah`, `si`, `so`, `ta`, `tee` (the default), `to`, `um`, `wa`, `yea`, or a file with a hum in our binary or text [LPC format](LpcFileFormat.md). |
| `-chip` _name_             | Target the specified speech synthesis chip: one of `TMS5100`, `TMS5110A`. `TMS5200` (the default), or `TMS5220`.                                                                                                                                          |
| `-tms5200`                 | Short for `-chip TMS5200` (the default).                                                                                                                                                                                                                  |
| `-tms5220`                 | Short for `-chip TMS5220`.                                                                                                                                                                                                                                |
| `-addstopframe`            | Add a stop frame to the LPC frames in the output.                                                                                                                                                                                                         |

## Example workflow
               
First make sure the `java` runtime can find the VideoTools. For example, in a
`bash` shell:

    export CLASSPATH=/path/to/videotools.jar

You can now convert the specified part of a given music sheet in MusicXML
format to humming, in a speech file in a raw binary LPC format:

    java ConvertMusicXmlToLpc \
      -startmeasure 1 -endmeasure 87 \
      -part 'Alto Saxophone' -transpose -2 -attenuate 2 -hum ta \
      -tms5200 -addstopframe \
      song.mxl song.lpc
    
You can convert the LPC coefficients to a WAV sound file, in order to quickly
check the quality of the vocals in any audio player:

    java ConvertLpcToWav -tms5200 -analog -precise song.lpc song.wav
    
We're choosing the common analog dynamic range, but with a full 16-bit
precision, so we can identify any artifacts more easily.

If you're happy with the results, you can embed the LPC vocals in an executable
cartridge for the TI-99/4A. First create a TMS video file, in this example only
containing the vocals (you could also include [instrumental parts from the
sound chip](ConvertMusicXmlToSnd.md), and still images or animations):

    java ComposeVideo -ntsc song.lpc song.tms

Then package the video file with a small default player program in a cartridge:

    java PackageVideoInCartridge -title 'MY SONG' song.tms song.rpk
             
Instead of an RPK file `song.rpk` for the Mame emulator, you can also create
just a raw ROM file like `romc.bin`, for other emulators.

You can then run the application from a RAM cartridge on the computer or in an
emulator like Mame:

    mame ti99_4a -ioport peb -ioport:peb:slot3 speech -cart1 song.rpk 
    
## Related tools

* [ConvertWavToLpc](ConvertWavToLpc.md)
* [ConvertLpcToWav](ConvertLpcToWav.md)
* [TuneLpcFile](TuneLpcFile.md)
* [CutLpcFile](CutLpcFile.md)
* [ConvertLpcToText](ConvertLpcToText.md)
* [ConvertMusicXmlToSnd](ConvertMusicXmlToSnd.md)
* [ComposeVideo](ComposeVideo.md)
* [PackageVideoInCartridge](PackageVideoInCartridge.md)
* [VideoTools](../README.md)
