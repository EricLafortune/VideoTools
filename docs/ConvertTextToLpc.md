# ConvertTextToLpc

## Summary

Converts a speech file with linear predictive coding (LPC) coefficients in our
readable [text LPC format](LpcFileFormat.md) to a file in our [binary LPC
format](LpcFileFormat.md).

## Usage

    java ConvertTextToLpc input.lpc output.txt

where:

|              |                                                                                                          |
|--------------|----------------------------------------------------------------------------------------------------------|
| _input.txt_  | The input file containing the LPC frames for TMS52xx speech synthesizer chips in a readable text format. |
| _output.lpc_ | The output file containing the same data as an encoded binary stream.                                    |

## Example

Convert previously computed LPC coefficients from a binary stream to a
readable format:

    java ConvertLpcToText input.lpc output.txt
    
You can then edit the text file, for example duplicating frames, changing
pitches, cleaning up unwanted frames, or concatenating files. You can convert
the text file back to binary format with this tool:

    java ConvertTextToLpc input.txt output.lpc
    
## Related tools

* [ConvertLpcToText](ConvertLpcToText.md)
* [TuneLpcFile](TuneLpcFile.md)
* [CutLpcFile](CutLpcFile.md)
* [VideoTools](../README.md)
