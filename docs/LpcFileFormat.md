# LPC file format

Our LPC file formats store Linear Predictive Coding coeefficients for the
TMS5200/TMS5220 speech synthesizers.

## Binary format (.lpc)

The binary version of the format contains the raw bytes of LPC speech data to
be streamed to the processors, without further compression. The bytes do not
include the SPEAK_EXTERNAL command (>60).

## Text format (.txt)

The text version of the format is defined as a sequence of LPC speech frames.
Each frame is defined on a new line:

| Frame type | Format               |
|------------|----------------------|
| Voiced     | _energy_ _pitch_ _k_ |
| Unvoiced   | _energy_ _k_         |
| Repeat     | _energy_ _pitch_     |
| Silence    | 0                    |
| Stop       | f                    |

where:
* _energy_ is a 1-digit hexadecimal value, specifying the encoded energy.
* _pitch_ is a 2-digit hexadecimal value, specifying the encoded pitch.
* _k_ is a 10-digit hexadecimal value, specifying the encoded reflection coeffients.

Empty lines and comments starting with '#' are ignored.

The text version is useful for reading and basic editing. You can convert
between binary and text with the video tools
[ConvertLpcToText](ConvertLpcToText.md) and
[ConvertTextToLpc](ConvertTextToLpc.md).

## Related tools

* [ConvertWavToLpc](ConvertWavToLpc.md)
* [ConvertLpcToText](ConvertLpcToText.md)
* [ConvertTextToLpc](ConvertTextToLpc.md)
* [VideoTools](../README.md)
