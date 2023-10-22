# ConvertLpcToWav

## Summary

Converts a file with linear predictive coding (LPC) coefficients to an audio
file. It accurately simulates TMS5200/TMS5220 speech synthesizer chips,
including their simplified interpolation and other quirks.

## Usage

    java ConvertLpcToWav [options...] input.lpc output.wav

where:

|              |                                                                                                                |
|--------------|----------------------------------------------------------------------------------------------------------------|
| _input.lpc_  | The input speech file containing the encoded binary stream of LPC frames for TMS52xx speech synthesizer chips. |
| _output.wav_ | The output sound file in WAV format, at 8000 Hz, mono.                                                         |

The options are:

|            |                                                                                |
|------------|--------------------------------------------------------------------------------|
| `-tms5200` | Decode LPC coefficients for the TMS5200 speech synthesizer chip (the default). |
| `-tms5220` | Decode LPC coefficients for the TMS5220 speech synthesizer chip.               |
| `-analog`  | Generate audio in the analog dynamic range (the default).                      |
| `-digital` | Generate audio in the digital dynamic range.                                   |
| `-precise` | Generate audio with a full 16-bits precision.                                  |

## Example

Convert previously computed LPC coefficients for the TMS5200 back to a sound 
file:

    java ConvertLpcToWav -tms5200 -analog -precise output.lpc output.wav
    
We're choosing the common analog dynamic range (like on the TI-99/4A home
computer), but with a full 16-bit precision, so we can identify any artifacts
more easily. You can play the file with any audio player.

## Related tools

* [ConvertWavToLpc](ConvertWavToLpc.md)
* [ConvertPraatToLpc](ConvertPraatToLpc.md)
* [VideoTools](../README.md)
