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
package speech;

import java.io.*;

/**
 * This class parses and returns frame data from an input stream in Linear
 * Predictive Coding (LPC) format for the TMS5200 speech synthesizer.
 *
 * @see http://www.unige.ch/medecine/nouspikel/ti99/speech.htm
 */
public class LpcInputStream
implements   AutoCloseable
{
    private static final boolean DEBUG = false;

    private static final byte SPEAK_EXTERNAL = 0x60;

    private static final int SILENCE  = 0x0;
    private static final int STOP     = 0xf;
    private static final int REPEAT   = 1;
    private static final int UNVOICED = 0x00;

    private final InputStream inputStream;
    private       boolean     addSpeakExternalCommand;

    private long dataBits;
    private int  dataBitCount;


    /**
     * Creates a new instance that reads its data from the given input stream.
     */
    public LpcInputStream(InputStream inputStream,
                          boolean     addSpeakExternalCommand)
    throws IOException
    {
        this.inputStream             = inputStream;
        this.addSpeakExternalCommand = addSpeakExternalCommand;
    }


    /**
     * Parses and returns the LPC data of the next frame in the stream
     * (which contains frames at a rate of 25 fps). The resulting sample bytes
     * can be sent to the buffer of the speech synthesizer. The 0 to 7 bytes
     * may already contain bits of the frame after.
     */
    public byte[] readFrame() throws IOException
    {
        // Collect the speech data.
        byte[] dataBytes     = new byte[8];
        int    dataByteCount = 0;

        if (addSpeakExternalCommand)
        {
            addSpeakExternalCommand = false;

            if (DEBUG)
            {
                System.err.print("Command: SPEAK_EXTERNAL (8 bits)");
            }

            dataBytes[dataByteCount++] = SPEAK_EXTERNAL;
        }

        // Collect bytes and bits until they contain at least one complete command.
        while (true)
        {
            if (dataBitCount >= 4)
            {
                int energy = (int)(dataBits >>> (dataBitCount - 4)) & 0xf;

                if (energy == STOP)
                {
                    if (DEBUG)
                    {
                        System.err.print(String.format("Frame: STOP    : energy = %1x                                         ( 4", energy));
                    }

                    dataBitCount = 0;
                    break;
                }

                if (energy == SILENCE)
                {
                    if (DEBUG)
                    {
                        System.err.print(String.format("Frame: SILENCE : energy = %1x                                         ( 4", energy));
                    }

                    dataBitCount -= 4;
                    break;
                }

                if (dataBitCount >= 11)
                {
                    int repeat = (int)(dataBits >>> (dataBitCount -  5)) & 0x1;
                    int pitch  = (int)(dataBits >>> (dataBitCount - 11)) & 0x3f;

                    if (repeat == REPEAT)
                    {
                        if (DEBUG)
                        {
                            System.err.print(String.format("Frame: REPEAT  : energy = %1x, repeat = 1, pitch = %02x                 (11", energy, pitch));
                        }

                        dataBitCount -= 11;
                        break;
                    }

                    if (dataBitCount >= 29)
                    {
                        if (pitch == UNVOICED)
                        {
                            int k = (int)(dataBits >>> (dataBitCount - 29)) & (1 << 18)-1;

                            if (DEBUG)
                            {
                                System.err.print(String.format("Frame: UNVOICED: energy = %1x, repeat = 0, pitch = %02x, k = %05x      (29", energy, pitch, k));
                            }

                            dataBitCount -= 29;
                            break;
                        }

                        if (dataBitCount >= 50)
                        {
                            long k = (dataBits >>> (dataBitCount - 50)) & (1L << 39)-1;

                            if (DEBUG)
                            {
                                System.err.print(String.format("Frame: VOICED  : energy = %1x, repeat = 0, pitch = %02x, k = %010x (50", energy, pitch, k));
                            }

                            dataBitCount -= 50;
                            break;
                        }
                    }
                }
            }

            int b = inputStream.read();
            if (b == -1)
            {
                if (DEBUG)
                {
                    System.err.print("EOF (0");
                }

                return null;
            }

            // Collect the original speech bytes in a byte buffer.
            dataBytes[dataByteCount++] = (byte)b;

            // Collect the speech bits in a long integer.
            // The original bits are stored starting in the least significant bit
            // of each byte, but we can parse them more naturally if they are
            // in reverse order.
            dataBits = (dataBits << 8) | (Integer.reverse(b) >>> 24);
            dataBitCount += 8;
        }

        if (DEBUG)
        {
            System.err.println(String.format(" bits plus %d bits for the next frame: %02x)",
                                             dataBitCount,
                                             dataBits & ((1 << dataBitCount) - 1)));
        }

        // Trim the speech data.
        byte[] trimmedDataBytes = new byte[dataByteCount];
        System.arraycopy(dataBytes, 0,
                         trimmedDataBytes, 0, dataByteCount);

        return trimmedDataBytes;
    }


    /**
     * Skips a frame (only at the start of a stream).
     */
    public void skipFrame() throws IOException
    {
        // Skip bits until they represent at least one complete command.
        while (true)
        {
            if (dataBitCount >= 4)
            {
                int energy = (int)(dataBits >>> (dataBitCount - 4)) & 0xf;

                if (energy == STOP)
                {
                    if (DEBUG)
                    {
                        System.err.print(String.format("Skip frame: STOP    : energy = %1x                                         ( 4", energy));
                    }

                    dataBitCount = 0;
                    break;
                }

                if (energy == SILENCE)
                {
                    if (DEBUG)
                    {
                        System.err.print(String.format("Skip frame: SILENCE : energy = %1x                                         ( 4", energy));
                    }

                    dataBitCount -= 4;
                    break;
                }

                if (dataBitCount >= 11)
                {
                    int repeat = (int)(dataBits >>> (dataBitCount -  5)) & 0x1;
                    int pitch  = (int)(dataBits >>> (dataBitCount - 11)) & 0x3f;

                    if (repeat == REPEAT)
                    {
                        if (DEBUG)
                        {
                            System.err.print(String.format("Skip frame: REPEAT  : energy = %1x, repeat = 1, pitch = %02x                 (11", energy, pitch));
                        }

                        dataBitCount -= 11;
                        break;
                    }

                    if (dataBitCount >= 29)
                    {
                        if (pitch == UNVOICED)
                        {
                            int k = (int)(dataBits >>> (dataBitCount - 29)) & (1 << 18)-1;

                            if (DEBUG)
                            {
                                System.err.print(String.format("Skip frame: UNVOICED: energy = %1x, repeat = 0, pitch = %02x, k = %05x      (29", energy, pitch, k));
                            }

                            dataBitCount -= 29;
                            break;
                        }

                        if (dataBitCount >= 50)
                        {
                            long k = (dataBits >>> (dataBitCount - 50)) & (1L << 39)-1;

                            if (DEBUG)
                            {
                                System.err.print(String.format("Skip frame: VOICED  : energy = %1x, repeat = 0, pitch = %02x, k = %010x (50", energy, pitch, k));
                            }

                            dataBitCount -= 50;
                            break;
                        }
                    }
                }
            }

            int b = inputStream.read();
            if (b == -1)
            {
                throw new IOException("Unexpected EOF");
            }

            // Collect the speech bits in a long integer.
            // The original bits are stored starting in the least significant bit
            // of each byte, but we can parse them more naturally if they are
            // in reverse order.
            dataBits = (dataBits << 8) | (Integer.reverse(b) >>> 24);
            dataBitCount += 8;
        }
    }


    /**
     * Skips the given number of frames (only at the start of a stream).
     */
    public void skipFrames(int count) throws IOException
    {
        for (int counter = 0; counter < count; counter++)
        {
            skipFrame();
        }
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        inputStream.close();
    }


    /**
     * Prints out the sample data of the specified LPC file.
     */
    public static void main(String[] args)
    {
        try (LpcInputStream lpcInputStream =
                 new LpcInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0])),
                 false))
        {
            int counter = 0;

            byte[] speechData;
            while ((speechData = lpcInputStream.readFrame()) != null)
            {
                System.out.print(String.format("%4d: %2d bytes:", counter++, speechData.length));
                for (int index = 0; index < speechData.length; index++)
                {
                    System.out.print(String.format(" %02x", speechData[index]));
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
