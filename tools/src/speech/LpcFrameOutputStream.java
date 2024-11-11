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
 * This LpcFrameOutput writes LPC frames to a given output stream.
 *
 * @see LpcFrame
 */
public class LpcFrameOutputStream
implements   LpcFrameOutput
{
    private final OutputStream outputStream;

    private long dataBits;
    private int  dataBitCount;


    /**
     * Creates a new instance that writes its data to the given output stream.
     */
    public LpcFrameOutputStream(OutputStream outputStream)
    throws IOException
    {
        this.outputStream = outputStream;
    }


    // Implementation for LpcFrameOutput.

    public void writeFrame(LpcFrame frame) throws IOException
    {
        int bitCount = frame.bitCount();

        // Prepend the data bits of the frame.
        dataBits |= frame.toReversedBits() << dataBitCount;
        dataBitCount += bitCount;

        // Write out all complete bytes.
        while (dataBitCount >= 8)
        {
            // Write the 8 least-significant bits.
            outputStream.write((int)dataBits);
            dataBits >>>= 8;
            dataBitCount -= 8;
        }
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        if (dataBitCount > 0)
        {
            // Write the remaining 0..7 bits, masked.
            outputStream.write((int)dataBits & (1 << dataBitCount) - 1);
        }

        outputStream.close();
    }


    /**
     * Copies the frames of the specified LPC file to the specified new file.
     */
    public static void main(String[] args)
    throws IOException
    {
        try (LpcFrameInput lpcFrameInput =
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0]))))
        {
            try (LpcFrameOutput lpcFrameOutput =
                     new LpcFrameOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(args[1]))))
            {
                LpcFrame frame;
                while ((frame = lpcFrameInput.readFrame()) != null)
                {
                    lpcFrameOutput.writeFrame(frame);
                }
            }
        }
    }
}
