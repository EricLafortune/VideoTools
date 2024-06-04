# TransposeSndFile

## Summary

Transposes the frequencies and attenuates the volume of a file
in our binary [SND format](SndFileFormat.md). It can be useful to transfer chip tunes
between systems on which the TMS9919 / SN76489 sound processors run at
different clock frequencies (e.g. TI-99/4A and BBC Micro) or with different
noise feedback shift register widths (e.g. TI-99/4A and Sega Master System).

## Usage

    java TransposeSndFile [options] input.snd output.snd

where:

|              |                                |
|--------------|--------------------------------|
| _input.snd_  | The input file in SND format.  |
| _output.snd_ | The output file in SND format. |

The basic options are:

|                                                                                        |                                                                                                      |
|----------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `-addsilencecommands`                                                                  | Add commands to silence all sound generators at the end of the file.                                 |
| `-frequencies` _from_clock_frequency_ _to_clock_frequency_                             | Adjust for the clock frequencies of the sound chip (in Hz, or `ntsc`, `pal`, `ti99`, or `bbc`).      |
| `-noiseshiftregisters` _from_noise_shift_register_bits_ _to_noise_shift_register_bits_ | Adjust for the different lengths of the noise shift registers (`15`, `16`, `ti99`, `bbc`, or `sms`). |
| `-attenuation` _attenuation_shift_                                                     | Change the attenuation by a certain value (positive values = quieter; negative values = louder).     |

The options for resolving tuning conflicts between tone generator 2 and the
noise generator are:

|                                                                                        |                                              |
|----------------------------------------------------------------------------------------|----------------------------------------------|
| `-transposeconflictingtonegenerator`                                                   | Transpose the tone generator correctly.      |
| `-transposeconflictingnoisegenerator`                                                  | Transpose the noise generator correctly.     |
| `-silencequietestconflictinggenerator`                                                 | Suppress the quietest conflicting generator. |
| `-silenceconflictingtonegenerator`                                                     | Suppress the tone generator.                 |
| `-silenceconflictingnoisegenerator`                                                    | Suppress the noise generator.                |

The default is to transpose the loudest generator correctly, possibly leaving
the other generator out of tune. 
 
## Example

Transpose an SND file from the Sega Master System to the TI-99/4A, reducing
the volume, and making sure all sound generators are silenced at the end:

    java TransposeSndFile \
      -noiseshiftregisters 16 15 \
      -silencequietestconflictinggenerator \
      -attenuation 2 \
      -addsilencecommands \
      input.snd output.snd

## Related tools

* [ConvertVgmToSnd](ConvertVgmToSnd.md)
* [SimplifySndFile](SimplifySndFile.md)
* [CutSndFile](CutSndFile.md)
* [VideoTools](../README.md)
