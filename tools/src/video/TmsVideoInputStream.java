/*
 * Video tools for the TI-99/4A home computer.
 *
 * Copyright (c) 2022-2024 Eric Lafortune
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package video;

import util.FrameInput;

import java.io.*;

/**
 * This SndInput returns raw data frames from an input stream in our custom
 * video (.tms) format.
 */
public class TmsVideoInputStream
implements   FrameInput<TmsVideoInputStream.VideoData>
{
    public static final int DISPLAY   = 0;
    public static final int SOUND     = 1;
    public static final int SPEECH    = 2;
    public static final int VSYNC     = 3;
    public static final int NEXT_BANK = 4;
    public static final int EOF       = 5;


    private final InputStream inputStream;
    private final int         bankSize;

    private int bankByteCount;

    /**
     * Creates a new instance that reads its data from the given input stream.
     */
    public TmsVideoInputStream(InputStream inputStream, int bankSize)
    {
        this.inputStream = inputStream;
        this.bankSize    = bankSize;
    }


    // Implementations for SndInput.

    public VideoData readFrame() throws IOException
    {
        int marker = inputStream.read();
        if (marker == -1)
        {
            return null;
        }

        marker |= (inputStream.read() << 8);

        if (marker >= BinaryVideoOutputStream.SOUND_SIZE_DELTA)
        {
            int len = marker - BinaryVideoOutputStream.SOUND_SIZE_DELTA;

            bankByteCount += len + 2;

            return new VideoData(SOUND, inputStream.readNBytes(len));
        }

        if (marker >= BinaryVideoOutputStream.SPEECH_SIZE_DELTA)
        {
            int len = marker - BinaryVideoOutputStream.SPEECH_SIZE_DELTA;

            bankByteCount += len + 2;

            return new VideoData(SPEECH, inputStream.readNBytes(len));
        }

        if (marker == (BinaryVideoOutputStream.VSYNC & 0xffff))
        {
            bankByteCount += 2;

            return new VideoData(VSYNC);
        }

        if (marker == (BinaryVideoOutputStream.NEXT_BANK & 0xffff))
        {
            bankByteCount += 2;

            inputStream.skipNBytes(bankSize - bankByteCount);

            bankByteCount = 0;

            return new VideoData(NEXT_BANK);
        }

        if (marker == (BinaryVideoOutputStream.EOF & 0xffff))
        {
            bankByteCount += 2;

            return new VideoData(EOF);
        }

        bankByteCount += marker + 4;

        int address = inputStream.read() | (inputStream.read() << 8);

        return new VideoData(DISPLAY,
                             address,
                             inputStream.readNBytes(marker));
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        inputStream.close();
    }


    /**
     * Prints out the sample data of the specified sound file.
     */
    public static void main(String[] args)
    throws IOException
    {
        try (TmsVideoInputStream videoInput =
                 new TmsVideoInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0])), 8 * 1024))
        {
            int frameCounter = 0;
            int vsyncCounter = 0;

            VideoData videoData;
            while ((videoData = videoInput.readFrame()) != null)
            {
                System.out.printf("[%4d] [%4d]: ",
                                  frameCounter,
                                  vsyncCounter);

                System.out.print(markerName(videoData.marker));

                if (videoData.marker == VSYNC)
                {
                    vsyncCounter++;
                }

                if (videoData.address >=0)
                {
                    System.out.printf(", address = %04x", videoData.address);
                }

                if (videoData.data != null)
                {
                    System.out.print(", "+videoData.data.length+" bytes:");

                    int count = Math.min(16, videoData.data.length);

                    for (int index = 0; index < count; index++)
                    {
                        System.out.printf(" %02x", videoData.data[index]);
                    }

                    if (count < videoData.data.length)
                    {
                        System.out.println(" ...");
                    }
                    else
                    {
                        System.out.println();
                    }
                }
                else
                {
                    System.out.println();
                }

                frameCounter++;
            }
        }
    }


    private static String markerName(int marker)
    {
        return switch (marker)
        {
            case DISPLAY   -> "Display";
            case SOUND     -> "Sound";
            case SPEECH    -> "Speech";
            case VSYNC     -> "Vsync";
            case NEXT_BANK -> "Next bank";
            case EOF       -> "Eof";
            default        -> "Unknown";
        };
    }


    public static class VideoData
    {
        public int    marker;
        public int    address;
        public byte[] data;


        public VideoData(int marker)
        {
            this(marker, null);
        }


        public VideoData(int marker, byte[] data)
        {
            this(marker, -1, data);
        }


        public VideoData(int marker, int address, byte[] data)
        {
            this.marker  = marker;
            this.address = address;
            this.data    = data;
        }
    }
}
