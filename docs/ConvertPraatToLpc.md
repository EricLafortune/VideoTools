# ConvertPraatToLpc

## Summary

Converts a pair of speech files in Praat Pitch format and in Praat LPC format
to a file in our binary [LPC format](LpcFileFormat.md). Our LPC format is
optimized for replay with the TMS52xx speech synthesis chips.

[Praat](https://www.fon.hum.uva.nl/praat/) (which is Dutch for "talk") is a
powerful phonetics program by the Phonetic Sciences group of the University
of Amsterdam. It offers many techniques and algorithms to analyze and
synthesize speech. You can operate it from a GUI or from scripts. It's
available for the major platforms. For example, on Debian/Ubuntu:

    sudo apt install praat

This
[introduction](https://resources.lab.hum.uu.nl/resources/phonetics/index.html)
is a good place to start. You can perform a pitch analysis and an LPC analysis
with Praat and then convert the results to our LPC format with this tool.

## Usage

    java ConvertPraatToLpc [options...] input.Pitch input.LPC unvoiced_energy_factor voiced_energy_factor output.lpc

where:

|                          |                                                                                                  |
|--------------------------|--------------------------------------------------------------------------------------------------|
| _input.Pitch_            | The pitch input file in the short text format produced by Praat.                                 |
| _input.LPC_              | The LPC input file in the short text format produced by Praat.                                   |
| _unvoiced_energy_factor_ | A scaling factor for the energy of unvoiced frames. A value around 0.5 may work reasonably well. |
| _voiced_energy_factor_   | A scaling factor for the energy of voiced frames. A value around 0.5 may work reasonably well.   |
| _output.lpc_             | The output file in our raw binary [LPC format](LpcFileFormat.md).                                |

The options are:

|                 |                                                                                                                  |
|-----------------|------------------------------------------------------------------------------------------------------------------|
| `-chip` _name_  | Target the specified speech synthesis chip: one of `TMS5100`, `TMS5110A`. `TMS5200` (the default), or `TMS5220`. |
| `-tms5200`      | Short for `-chip TMS5200` (the default).                                                                         |
| `-tms5220`      | Short for `-chip TMS5220`.                                                                                       |
| `-8kHz`         | Set a sample target rate of 8 kHz (the default).                                                                 |
| `-10kHz`        | Set a sample target rate of 10 kHz.                                                                              |
| `-addstopframe` | Add a stop frame to the LPC frames in the output.                                                                |

## Example workflow

The video tools contain a Praat script that reads a specified WAV file,
analyzes the speech, and writes a Praat pitch file and a Praat LPC file.
You can follow it interactively or execute it from the command-line:

    praat --run tools/lpc.praat \
      /tmp/speech.wav \
      /tmp/speech.PraatPitch \
      /tmp/speech.PraatLPC \
      250 550 0.02 0.40 0.20 0.20 0.03

The header of the script explains the parameters. You can then apply the
conversion tool:

    java ConvertPraatToLpc \
      -addstopframe \
      /tmp/speech.PraatPitch \
      /tmp/speech.PraatLPC \
      0.4 0.6 \
      speech.lpc

You can convert the LPC coefficients to a WAV sound file, in order to quickly
check the quality of the speech in any audio player:

    java ConvertLpcToWav -tms5200 -analog -precise speech.lpc check.wav

We're choosing the common analog dynamic range, but with a full 16-bit
precision, so we can identify any artifacts more easily.

If you're happy with the results, you can embed the LPC speech in an executable
cartridge for the TI-99/4A. First create a TMS video file, in this example only
containing the speech:

    java ComposeVideo -ntsc speech.lpc speech.tms

Then package the video file with a small default player program in a cartridge:

    java PackageVideoInCartridge -title 'MY SONG' speech.tms speech.rpk

Instead of an RPK file `speech.rpk` for the Mame emulator, you can also create
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
* [ComposeVideo](ComposeVideo.md)
* [PackageVideoInCartridge](PackageVideoInCartridge.md)
* [VideoTools](../README.md)
