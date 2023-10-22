# CutLpcFile

## Summary

Copies a section from a file in our binary [LPC format](LpcFileFormat.md).

## Usage

    java CutLpcFile input.lpc start_frame stop_frame output.lpc

where:

|               |                                              |
|---------------|----------------------------------------------|
| _input.lpc_   | The input file in LPC format.                |
| _start_frame_ | The start frame in the input file.           |
| _stop_frame_  | The end frame (exclusive) in the input file. |
| _output.lpc_  | The output file in LPC format.               |

## Example

Extract speech frames 10 to 20 (exclusive) from an LPC file:

    java CutLpcFile input.lpc 10 20 output.lpc

## Related tools

* [ConvertWavToLpc](ConvertWavToLpc.md)
* [ConvertPraatToLpc](ConvertPraatToLpc.md)
* [VideoTools](../README.md)
