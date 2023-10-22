# SimplifySndFile

## Summary

Simplifies the contents of a file in our binary [SND format](SndFileFormat.md), by removing any
duplicate or other unnecessary sound commands. It reduces the size of the
file and slightly improves its efficiency as a result. Notably files that
have been converted from VGM music files may benefit.

## Usage

    java SimplifySndFile [options] input.snd output.snd

where:

|              |                                |
|--------------|--------------------------------|
| _input.snd_  | The input file in SND format.  |
| _output.snd_ | The output file in SND format. |

The only option is:

|                       |                                                                      |
|-----------------------|----------------------------------------------------------------------|
| `-addsilencecommands` | Add commands to silence all sound generators at the end of the file. |

## Example

Simplify an SND file, making sure all sound generators are silenced at the end:

    java SimplifySndFile -addsilencecommands input.snd output.snd

## Related tools

* [ConvertVgmToSnd](ConvertVgmToSnd.md)
* [CutSndFile](CutSndFile.md)
* [VideoTools](../README.md)
