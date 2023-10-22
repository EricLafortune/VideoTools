# TuneLpcFile

## Summary

Tunes the frequencies of a speech file in our binary [LPC format](LpcFileFormat.md) to the
frequencies of tone generator 1 in a synchronized music file in our [SND
format](SndFileFormat.md).

## Usage

    java TuneLpcFile input.lpc[,start_frame[,stop_frame]] input.snd[,start_frame] [frequency_factor [min_frequency [max_frequency]]] output.lpc

where:

|                    |                                                                                                                                     |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| _input.lpc_        | The input file in LPC format.                                                                                                       |
| _input.snd_        | The input file in SND format.                                                                                                       |
| _start_frame_      | The optional start frame in the input file.                                                                                         |
| _stop_frame_       | The optional end frame (exclusive) in the input file.                                                                               |
| _frequency_factor_ | The optional factor to scale the frequencies of the sound file (default = 1.0).                                                     |
| _min_frequency_    | The optional minimum allowed frequency of in the output file, expressed in Hz (default = 37.0). Any lower frequencies are doubled.  |
| _max_frequency_    | The optional maximum allowed frequency of in the output file, expressed in Hz (default = 534.0). Any higher frequencies are halved. |
| _output.lpc_       | The output file in LPC format.                                                                                                      |

## Example

For example, the first version of our
[Bad Apple demo](https://github.com/EricLafortune/BadApple) 
for the TI-99/4A tuned an LPC file with vocals to a an SND file with music: 

    java TuneLpcFile input.lpc input.snd,1454 0.5 300 535 output.lpc

The parameters are typically based on trial and error. We're starting the SND 
file at its frame 1454, so the vocals and the music are synchronized. We're
halving the frequencies to better match the frequencies of the speech
synthesizer. We're limiting the resulting frequency range to a female voice of 
300 to 535 Hz. 

## Related tools

* [ConvertWavToLpc](ConvertWavToLpc.md)
* [ConvertPraatToLpc](ConvertPraatToLpc.md)
* [CutLpcFile](CutLpcFile.md)
* [ConvertVgmToSnd](ConvertVgmToSnd.md)
* [SimplifySndFile](SimplifySndFile.md)
* [CutSndFile](CutSndFile.md)
* [VideoTools](../README.md)
