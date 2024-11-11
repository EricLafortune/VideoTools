# ConvertSndToWav

## Summary

Converts a file with commands for the TMS9919/SN76489 sound processor to an
audio file. The input file in our binary [SND file format](SndFileFormat.md).
The tool accurately simulates the tone generators and the noise generator of
the TMS9919/SN76489 sound processor and its variants.

## Usage

    java ConvertSndToWav [options...] input.snd output.wav

where:

|              |                                                                                             |
|--------------|---------------------------------------------------------------------------------------------|
| _input.snd_  | The input speech file containing sound frames in our custom [SND format](SndFileFormat.md). |
| _output.wav_ | The output sound file in mono WAV format.                                                   |

The options are:

|                                  |                                                                                                                                                                                 |
|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-computer` _name_               | Simulate the specified target computer (sound chip and frequencies): one of `TI99` (the default), `COLECOVISION`, `BBC`, `GAMEGEAR`, or `SMS`.                                  |
| `-framefrequency` _value_        | Play the sound frames at the specified frequency: one of `NTSC` (59.922738 sound frames per second), `PAL` (50.158969 sound frames per second), or a specified frequency in Hz. |
| `-targetsamplefrequency` _value_ | Generate an output file with (approximately) the specified sample rate. The default is 20000 Hz.                                                                                |
             
## Example

Convert an SND file for the TI-99/4A home computer to an audio file:

    java ConvertSndToWav -computer TI99 input.snd output.wav
    
You can play the resulting file with any audio player.

## Related tools

* [ConvertMusicXmlToSnd](docs/ConvertMusicXmlToSnd.md) 
* [ConvertVgmToSnd](docs/ConvertVgmToSnd.md)
* [CutSndFile](CutSndFile.md)
* [SimplifySndFile](SimplifySndFile.md)
* [TransposeSndFile](TransposeSndFile.md)
* [VideoTools](../README.md)
