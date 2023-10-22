# ConvertLpcToText

## Summary

Converts a speech file with linear predictive coding (LPC) coefficients from
our [binary LPC format](LpcFileFormat.md) to a file in our readable [text LPC
format](LpcFileFormat.md).

You can then inspect the text file and if necessary even edit and convert it
back.

## Usage

    java ConvertLpcToText input.lpc output.txt

where:

|              |                                                                                                         |
|--------------|---------------------------------------------------------------------------------------------------------|
| _input.lpc_  | The input file containing the encoded binary stream of LPC frames for TMS52xx speech synthesizer chips. |
| _output.txt_ | The output file containing the same data in readable text format.                                       |

## Example

Convert previously computed LPC coefficients to a readable format:

    java ConvertLpcToText input.lpc output.txt
    
## Related tools

* [ConvertTextToLpc](ConvertTextToLpc.md)
* [TuneLpcFile](TuneLpcFile.md)
* [CutLpcFile](CutLpcFile.md)
* [VideoTools](../README.md)
