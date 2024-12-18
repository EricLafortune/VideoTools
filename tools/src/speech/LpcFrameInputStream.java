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
package speech;

import java.io.*;

/**
 * This LpcFrameInput parses and returns frames from an input stream in binary
 * Linear Predictive Coding (LPC) format.
 *
 * @see LpcFrame
 */
public class LpcFrameInputStream
implements   LpcFrameInput
{
    private static final int SILENCE  = 0x0;
    private static final int STOP     = 0xf;
    private static final int REPEAT   = 1;
    private static final int UNVOICED = 0x00;

    private final InputStream inputStream;
    private       boolean     addStopCommand;

    private long dataBits;
    private int  dataBitCount;


    /**
     * Creates a new instance that reads its data from the given input stream.
     */
    public LpcFrameInputStream(InputStream inputStream)
    {
        this(inputStream, false);
    }


    /**
     * Creates a new instance that reads its data from the given input stream
     * and optionally adds a stop frame at the end (if there isn't one yet).
     */
    public LpcFrameInputStream(InputStream inputStream,
                               boolean     addStopCommand)
    {
        this.inputStream    = inputStream;
        this.addStopCommand = addStopCommand;
    }


    // Implementations for LpcFrameInput.

    public LpcFrame readFrame() throws IOException
    {
        // Collect bits until they contain at least one complete command.
        while (true)
        {
            if (dataBitCount >= 4)
            {
                int energy = (int)(dataBits >>> (dataBitCount - 4)) & 0xf;

                if (energy == STOP)
                {
                    dataBitCount = 0;

                    addStopCommand = false;

                    return new LpcStopFrame();
                }

                if (energy == SILENCE)
                {
                    dataBitCount -= 4;

                    return new LpcSilenceFrame();
                }

                if (dataBitCount >= 11)
                {
                    int repeat = (int)(dataBits >>> (dataBitCount -  5)) & 0x1;
                    int pitch  = (int)(dataBits >>> (dataBitCount - 11)) & 0x3f;

                    if (repeat == REPEAT)
                    {
                        dataBitCount -= 11;

                        return new LpcRepeatFrame(energy, pitch);
                    }

                    if (dataBitCount >= 29)
                    {
                        if (pitch == UNVOICED)
                        {
                            long k = (dataBits >>> (dataBitCount - 29)) & (1L << 18)-1;

                            dataBitCount -= 29;

                            return new LpcUnvoicedFrame(energy, k);
                        }

                        if (dataBitCount >= 50)
                        {
                            long k = (dataBits >>> (dataBitCount - 50)) & (1L << 39)-1;

                            dataBitCount -= 50;

                            return new LpcVoicedFrame(energy, pitch, k);
                        }
                    }
                }
            }

            int b = inputStream.read();
            if (b == -1)
            {
                dataBitCount = 0;

                if (addStopCommand)
                {
                    addStopCommand = false;

                    return new LpcStopFrame();
                }

                return null;
            }

            // Collect the speech bits in a long integer.
            // The original bits are stored starting in the least significant bit
            // of each byte, but we can use them more naturally if they are
            // in reverse order.
            dataBits = (dataBits << 8) | (Integer.reverse(b) >>> 24);
            dataBitCount += 8;
        }
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        inputStream.close();
    }


    /**
     * Prints out the frames of the specified LPC file.
     */
    public static void main(String[] args)
    throws IOException
    {
        LpcQuantization quantization = null;

        int argIndex = 0;

        while (args[argIndex].startsWith("-"))
        {
            quantization = switch (args[argIndex++])
            {
                case "-tms5200" -> LpcQuantization.TMS5200;
                case "-tms5220" -> LpcQuantization.TMS5220;
                default         -> throw new IllegalArgumentException("Unknown option [" + args[--argIndex] + "]");
            };
        }

        String inputFileName  = args[argIndex++];

        try (LpcFrameInput lpcFrameInput =
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName))))
        {
            int counter = 0;

            LpcFrame frame;
            while ((frame = lpcFrameInput.readFrame()) != null)
            {
                System.out.printf("#%03d (%.3f): %s%n",
                                  counter,
                                  counter*0.025,
                                  quantization == null ?
                                      frame.toString() :
                                      frame.toString(quantization));

                counter++;
            }
        }
    }
}
