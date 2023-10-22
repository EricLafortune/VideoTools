# TMS file format

Our binary TMS file format (.tms) encodes optimized video with animations,
music, and speech for systems with the following chips:

* video display processor TMS9918
* sound processor TMS9919/SN76489
* speech synthesizer TMS5200

The format is generic and flexible; it essentially specifies chunks of bytes to
be streamed to the specified processors, without further compression.

The format is defined as a sequence of such chunks. Each chunk consists of a
2-byte header word, followed by a 2-byte address (if applicable), followed by
data (if applicable):

| Header        | Address        | Data               | Explanation |
|---------------|----------------|--------------------|-------------|
| >0000 + _len_ | _VDP address_  | _len_ video bytes  | The video data length, the VDP destination addres (with write bit >4000 set), and the actual data to be streamed to the video display processor |
| >ffe0 + _len_ | /              | _len_ sound bytes  | The sound data length and the actual data to be streamed to the sound processor |
| >ffd0 + _len_ | /              | _len_ speech bytes | The speech data length and the actual data to be streamed to the speech synthesizer |
| >ffcf         | /              | /                  | Wait for VSync |
| >ffce         | /              | /                  | Continue with the next memory bank with input chunks |
| >ffcd         | /              | /                  | End of file |

The header word and the address are stored with the least significant byte
first (little-endian). This is atypical, but slightly more efficient for the
video player.

Because of the encoding, a chunk can contain up to >ffcc bytes of video data,
up to >001f bytes of sound data, or a up to >000f bytes of speech data. With
16K of video memory, the practical limit is >4000 bytes of video data.

A sequence of chunks can represent a complete video (or just music, or just
speech), typically at 50Hz or 60Hz. The video tools contain an encoder
ComposeVideo written in Java, and a player for the TI-99/4A written in TMS9900
assembly.

## Related tools

* [ComposeVideo](ComposeVideo.md)
* [VideoTools](../README.md)
