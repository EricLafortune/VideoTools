# PackageVideoInCartridge

## Summary

Packages a video in our custom [TMS file format](TmsFileFormat.md) with a
simple video player as a program, in a ROM cartridge file in RPK format (.rpk)
or raw ROM format (romc.bin) for the TI-99/4A. You can run such a file in the
Mame emulator, other emulators, or in a RAM cartridge on an actual console.

## Usage

    java PackageVideoInCartridge [options...] input.tms output.{rpk,bin)

where:

|              |                                                                                      |
|--------------|--------------------------------------------------------------------------------------|
| _input.tms_  | The input file in our custom [TMS file format](TmsFileFormat.md).                    |
| _output.rpk_ | The output file in RPK file format (a zip file) for the Mame emulator.               |
| _output.bin_ | The output file in raw binary ROM format for other emulators (typically `romc.bin`). |

The options are:

|                                |                                                                                                                                          |
|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `-title` _string_              | The program title for the video with animation, sound, and speech enabled (default = `VIDEO WITH SPEECH`).                               |
| `-titlewithoutspeech` _string_ | An optional additional program title for the video with only animation and sound enabled, not speech (default = `VIDEO WITHOUT SPEECH`). |
                    
## Example workflow
               
First make sure the `java` runtime can find the VideoTools. For example, in a
`bash` shell:

    export CLASSPATH=/path/to/videotools.jar

First create a TMS video file; in this example with a single static image,
but you can also include [animations](ComposeVideo.md),
[chip tunes](ConvertVgmToSnd.md), [music](ConvertMusicXmlToSnd.md),
[speech](ConvertWavToLpc.md), and [vocals](ConvertMusicXmlToLpc.md):

    java ComposeVideo image.png video.tms

Then package the video file with the small default player program in a
cartridge:

    java PackageVideoInCartridge -title 'MY VIDEO' video.tms video.rpk
             
Instead of an RPK file `video.rpk` for the Mame emulator, you can also create
just a raw ROM file like `romc.bin`, for other emulators.

You can then run the application from a RAM cartridge on the computer or in an
emulator like Mame:

    mame ti99_4a -cart1 video.rpk 

## Alternative

If you want more control over the video player, you can assemble it yourself
with the [xdt99](https://github.com/endlos99/xdt99) tools. The video file gets
hardcoded inside the resulting binary. 

    cd player
    xas99.py --register-symbols --binary --output out/romc.bin src/player.asm

You can then package the binary as an RPK cartridge file:

    rm -f out/video.rpk
    zip -q --junk-paths out/video.rpk layout.xml out/romc.bin

## Related tools

* [ComposeVideo](ComposeVideo.md)
* [VideoTools](../README.md)
