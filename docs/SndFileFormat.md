# SND file format

Our binary SND file format (.snd) stores sound commands for the
TMS9919/SN76489 sound processor.

The format specifies chunks of bytes to be streamed to the sound processor.

The format is defined as a sequence of such chunks. Each chunk consists of a
header of a single unsigned byte specifying the length, followed by the data,
without further compression..

A sequence of chunks can represent sounds or music, typically at 50Hz or 60Hz.
The video tools contain a tool to extract sound data from VGM music files. The
SND format is not as generic but significantly more compact and convenient for
specialized players.

## Related tools

* [ConvertVgmToSnd](ConvertVgmToSnd.md)
* [CutSndFile](CutSndFile.md)
* [SimplifySndFile](SimplifySndFile.md)
* [VideoTools](../README.md)
