# ConvertLpcToWav

## Summary

Converts a file with linear predictive coding (LPC) coefficients for TMS52xx
speech chips to an audio file. The input file can be a raw binary file or a
file in our custom text format. The tool accurately simulates the speech
synthesis chips, including their simplified interpolation and other quirks.

## Usage

    java ConvertLpcToWav [options...] input.{lpc,txt} output.wav

where:

|              |                                                                                           |
|--------------|-------------------------------------------------------------------------------------------|
| _input.lpc_  | The input speech file containing LPC frames in raw [binary LPC format](LpcFileFormat.md). |
| _input.txt_  | The input speech file containing LPC frames in our [text LPC format](LpcFileFormat.md).   |
| _output.wav_ | The output sound file in WAV format, at 8000 Hz, mono.                                    |

The options are:

|                |                                                                                                                    |
|----------------|--------------------------------------------------------------------------------------------------------------------|
| `-chip` _name_ | Simulate the specified speech synthesis chip: one of `TMS5100`, `TMS5110A`. `TMS5200` (the default), or `TMS5220`. |
| `-tms5200`     | Short for `-chip TMS5200` (the default).                                                                           |
| `-tms5220`     | Short for `-chip TMS5220`.                                                                                         |
| `-analog`      | Generate audio in the analog dynamic range (the default).                                                          |
| `-digital`     | Generate audio in the digital dynamic range.                                                                       |
| `-precise`     | Generate audio with a full 16-bits precision.                                                                      |

## Example

Convert previously computed LPC coefficients for the TMS5200 back to a sound
file:

    java ConvertLpcToWav -tms5200 -analog -precise input.lpc output.wav

We're choosing the common analog dynamic range (like on the TI-99/4A home
computer), but with a full 16-bit precision, so we can identify any artifacts
more easily. You can play the file with any audio player.

## Related tools

* [ConvertMusicXmlToLpc](ConvertMusicXmlToLpc.md)
* [ConvertWavToLpc](ConvertWavToLpc.md)
* [ConvertPraatToLpc](ConvertPraatToLpc.md)
* [VideoTools](../README.md)
