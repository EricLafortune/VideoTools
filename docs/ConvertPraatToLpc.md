# ConvertPraatToLpc

## Summary

Converts a pair of speech files in Praat Pitch format and in Praat LPC format
to a file in our binary [LPC format](LpcFileFormat.md). Our LPC format is
optimized for replay with the TMS5200/TMS5220 speech synthesizer chips.

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

    java ConvertPraatToLpc [options] input.Pitch input.LPC unvoiced_energy_factor voiced_energy_factor output.lpc

where:

|                          |                                                                                                  |
|--------------------------|--------------------------------------------------------------------------------------------------|
| _input.Pitch_            | The pitch input file in the short text format produced by Praat.                                 |
| _input.LPC_              | The LPC input file in the short text format produced by Praat.                                   |
| _unvoiced_energy_factor_ | A scaling factor for the energy of unvoiced frames. A value around 0.5 may work reasonably well. |
| _voiced_energy_factor_   | A scaling factor for the energy of voiced frames. A value around 0.5 may work reasonably well.   |

The options are:

|                 |                                                                                 |
|-----------------|---------------------------------------------------------------------------------|
| `-tms5200`      | Compute LPC coefficients for the TMS5200 speech synthesizer chip (the default). |
| `-tms5220`      | Compute LPC coefficients for the TMS5220 speech synthesizer chip.               |
| `-8kHz`         | Set a sample target rate of 8 kHz (the default).                                |
| `-10kHz`        | Set a sample target rate of 10 kHz.                                             |
| `-addstopframe` | Add a stop frame to the LPC frames in the output.                               |

## Example workflow

The video tools contain a Praat script that reads a specified WAV file,
analyzes the speech, and writes a Praat pitch file and a Praat LPC file.
You can follow it interactively or execute it from the command-line:

    praat --run tools/lpc.praat \
      /tmp/input.wav \
      /tmp/output.PraatPitch \
      /tmp/output.PraatLPC \
      250 550 0.02 0.40 0.20 0.20 0.03

The header of the script explains the parameters. You can then apply the
conversion tool:

    java ConvertPraatToLpc \
      -addstopframe \
      /tmp/output.PraatPitch \
      /tmp/output.PraatLPC \
      0.4 0.6 \
      /tmp/output.lpc

You cam convert the LPC coefficients back to a sound file, in order to check
the quality of the results:

    java ConvertLpcToWav -tms5200 -analog -precise /tmp/output.lpc output.wav
    
We're choosing the common analog dynamic range, but with a full 16-bit
precision, so we can identify any artifacts more easily. You can play the file
with any audio player.

You can also embed the LPC speech in an executable cartridge for the TI-99/4A.
First create a video file, in this case only containing the speech:

    java ComposeVideo /tmp/output.lpc player/data/video.tms
    
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

* [ConvertWavToLpc](ConvertWavToLpc.md)
* [ConvertLpcToWav](ConvertLpcToWav.md)
* [TuneLpcFile](TuneLpcFile.md)
* [CutLpcFile](CutLpcFile.md)
* [ConvertLpcToText](ConvertLpcToText.md)
* [ComposeVideo](ComposeVideo.md)
* [VideoTools](../README.md)
