# ConvertMusicXmlToSnd

## Summary

Converts a music sheet in MusicXML format to a file playing the music, in our
binary [SND format](SndFileFormat.md), for replay with TMS9919/SN76489 sound
chips.

## Usage

    java ConvertMusicXmlToSnd [options...] input.{mxl,musicxml,xml} output.snd

where:

|                           |                                                               |
|---------------------------|---------------------------------------------------------------|
| _input.{mxl,musicxml,xml} | The input file in (possibly zipped) MusicXML format.          |
| _output.snd_              | The output file in our binary [SND format](SndFileFormat.md). |

The options are:

|                           |                                                                                                                                                                              |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-startmeasure` _number_  | Start humming at the specified measure of the music sheet (default = 1).                                                                                                     |
| `-endmeasure` _number_    | Stop humming at the specified measure of the music sheet (default = last measure).                                                                                           |
| `-speed` _factor_         | Increase or decrease the beats per minute of the music sheet by the given factor (default = 1).                                                                              |
| `-computer` _name_        | Simulate the specified target computer (sound chip and frequencies): one of `TI99` (the default), `COLECOVISION`, `BBC`, `GAMEGEAR`, or `SMS`.                               |
| `-framefrequency` _value_ | Create sound frames at the specified frequency: one of `NTSC` (59.922738 sound frames per second), `PAL` (50.158969 sound frames per second) or a specified frequency in Hz. |
                     
You should specify one or more parts from the music sheet to play, each time specifying a part, followed by associated options: 

|                            |                                                                                                                                                                                                                                       |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-part` _name_             | Play the specified part, given as a part ID, a part name, a part abbreviation, or an instrument name.                                                                                                                                 |
| `-voice` _name_            | Play the specified voice of the part (default = 1).                                                                                                                                                                                   |
| `-staff` _number_          | Play the specified staff number of the part (default = -1, undefined).                                                                                                                                                                |
| `-chordnotes` _count_      | Play at most the specified number of simultaneous notes in a chord (default = 1).                                                                                                                                                     |
| `-attenuate` _attenuation_ | Attenuate the part down (or maybe up) by the given integer number of decibels (sound chip attenuation units, default = 0).                                                                                                            |
| `-transpose` _octaves_     | Transpose the part up (or down) by the given integer number of octaves (default = 0).                                                                                                                                                 |
| `-instrument` _name_       | Play the part with the specified instrument: one of `piano` (the default), `xylophone`, `guitar`, `violin`, `flute`, `saxophone`, `tenor_drum`, `drum`, `bass_drum`, `snare_drum`, `snare_drum1`, `snare_drum2`, `cowbell`, `cymbal`. |
                            
Although the TMS9919/SN76489 sound chips only have 3 tone generators and 1
noise generator, the tool optimizes the sounds that it can play in each
frame. You can generally improve the quality of the output by attenuating
the different parts relative to one another. The more important, louder
parts then get precedence.

## Example workflow
               
First make sure the `java` runtime can find the VideoTools. For example, in a
`bash` shell:

    export CLASSPATH=/path/to/videotools.jar

You can now convert the specified part of a given music sheet in MusicXML
format to humming, in a sound file in our binary SND format:

    java ConvertMusicXmlToSnd \
      -startmeasure 1 -endmeasure 87 \
      -part 'Piano'                                          -transpose 0 -attenuate 0 -instrument piano \
      -part 'Electric Piano' -staff 1 -voice 1 -chordnotes 3 -transpose 0 -attenuate 4 -instrument piano \
      -part 'Electric Piano' -staff 2 -voice 5 -chordnotes 2 -transpose 1 -attenuate 1 -instrument piano \
      -framefrequency NTSC
      music.mxl music.snd
    
You can convert the SND commands to a WAV sound file, in order to quickly check
the quality of the results in any audio player:

    java ConvertSndToWav -framefrequency NTSC music.snd music.wav
    
If you're happy with the results, you can embed the SND commands in an
executable cartridge for the TI-99/4A. First create a TMS video file, in this
example only containing the music (you could also include [humming from the
speech synthesizer](ConvertMusicXmlToLpc.md), and still images or animations):

    java ComposeVideo -ntsc music.snd music.tms

Then package the video file with a small default player program in a cartridge:

    java PackageVideoInCartridge -title 'MY MUSIC' music.tms music.rpk
             
Instead of an RPK file `music.rpk` for the Mame emulator, you can also create
just a raw ROM file like `romc.bin`, for other emulators.

You can then run the application from a RAM cartridge on the computer or in an
emulator like Mame:

    mame ti99_4a -cart1 music.rpk 
    
## Related tools

* [SimplifySndFile](SimplifySndFile.md)
* [TransposeSndFile](TransposeSndFile.md)
* [CutSndFile](CutSndFile.md)
* [ConvertMusicXmlToLpc](ConvertMusicXmlToLpc.md)
* [ComposeVideo](ComposeVideo.md)
* [PackageVideoInCartridge](PackageVideoInCartridge.md)
* [VideoTools](../README.md)
