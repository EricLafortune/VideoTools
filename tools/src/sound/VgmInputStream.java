/*
 * Video tools for the TI-99/4A home computer.
 *
 * Copyright (c) 2022 Eric Lafortune
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
package sound;

import java.io.*;

/**
 * This class parses and returns a stream of data in Video Game Music (.vgm)
 * format. It ignores all header information, assuming the input only contains
 * SN76489 samples and wait commands. Each frame contains the sample data of
 * a given frame time (duration).
 *
 * @see https://vgmrips.net/wiki/VGM_Specification
 */
public class VgmInputStream
implements   SoundInputStream
{
    private static final int COMMAND_PSG       = 0x50;
    private static final int COMMAND_WAIT      = 0x61;
    private static final int COMMAND_WAIT_60HZ = 0x62;
    private static final int COMMAND_WAIT_50HZ = 0x63;
    private static final int COMMAND_END       = 0x66;

    public static final int FRAME_TIME_50_FPS = 882;
    public static final int FRAME_TIME_60_FPS = 735;



    private final InputStream inputStream;
    private final int         frameTime;
    private       int         streamTime;
    private       int         requestTime;


    /**
     * Creates a new instance that reads its data from the given input stream
     * with a frame time of {@link #FRAME_TIME_50_FPS}.
     */
    public VgmInputStream(InputStream inputStream)
    throws IOException
    {
        this(inputStream, FRAME_TIME_50_FPS);
    }


    /**
     * Creates a new instance that reads its data from the given input stream
     * with the given frame time.
     */
    public VgmInputStream(InputStream inputStream,
                          int         frameTime)
    throws IOException
    {
        this.inputStream = inputStream;
        this.frameTime   = frameTime;

        // Skip the header.
        inputStream.skip(64L);
    }


    // Implementations for SoundInputStream.

    public byte[] readFrame() throws IOException
    {
        requestTime += frameTime;

        // Collect the sound data.
        byte[] soundData      = new byte[64];
        int    soundDataCount = 0;
//System.err.println("Time: "+requestTime+" ~ "+time);
        loop: while (streamTime <= requestTime)
        {
            // Parse the next command.
            int b = inputStream.read();
//System.err.println(String.format("Command %02x", b));
            switch (b)
            {
                case COMMAND_PSG:
                {
                    soundData[soundDataCount++] =
                        (byte)inputStream.read();
                    break;
                }
                case COMMAND_WAIT:
                {
                    streamTime +=
                        (inputStream.read()) |
                        (inputStream.read() << 8);
                    break;
                }
                case COMMAND_WAIT_60HZ:
                {
                    streamTime += FRAME_TIME_60_FPS;
                    break;
                }
                case COMMAND_WAIT_50HZ:
                {
                    streamTime += FRAME_TIME_50_FPS;
                    break;
                }
                case COMMAND_END:
                {
                    // Skip any remaining data.
                    inputStream.skip(999L);
                    break;
                }
                case -1:
                {
                    if (soundDataCount == 0)
                    {
                        return null;
                    }
                    break loop;
                }
                default:
                {
                    System.err.println(String.format("Unsupported VGM command [0x%02x]", b));
                    break;
                }
            }
        }

        // Trim the sound data.
        byte[] trimmedSoundData = new byte[soundDataCount];
        System.arraycopy(soundData, 0,
                         trimmedSoundData, 0, soundDataCount);
//System.err.print("Sound >");
//for (int i = 0; i < trimmedSoundData.length; i++)
//{
//    System.err.print(String.format("%02x",  trimmedSoundData[i]));
//}
//System.err.println();
        return trimmedSoundData;
    }


    public void skipFrame() throws IOException
    {
        readFrame();
    }


    public void skipFrames(int count) throws IOException
    {
        for (int counter = 0; counter < count; counter++)
        {
            skipFrame();
        }
    }


    public void close() throws IOException
    {
        inputStream.close();
    }


    /**
     * Prints out the sample data of the specified VGM file.
     */
    public static void main(String[] args)
    {
        try (VgmInputStream vgmInputStream =
                 new VgmInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0]))))
        {
            int counter = 0;

            byte[] soundData;
            while ((soundData = vgmInputStream.readFrame()) != null && counter < 10000)
            {
                System.out.print(String.format("%4d: %2d bytes:", counter++, soundData.length));

                for (int index = 0; index < soundData.length; index++)
                {
                    System.out.print(String.format(" %02x", soundData[index]));
                }
                System.out.println();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
