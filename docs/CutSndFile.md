# CutSndFile

## Summary

Copies a section from a file in our binary [SND format](SndFileFormat.md).

## Usage

    java CutSndFile input.snd start_frame stop_frame output.snd

where:

|               |                                              |
|---------------|----------------------------------------------|
| _input.snd_   | The input file in SND format.                |
| _start_frame_ | The start frame in the input file.           |
| _stop_frame_  | The end frame (exclusive) in the input file. |
| _output.snd_  | The output file in SND format.               |

## Example

Extract sound frames 100 to 200 (exclusive) from an SND file:

    java CutSndFile input.snd 100 200 output.snd

## Related tools

* [ConvertVgmToSnd](ConvertVgmToSnd.md)
* [SimplifySndFile](SimplifySndFile.md)
* [VideoTools](../README.md)
