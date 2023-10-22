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
 * This InputStream wraps an LpcFrameInputStream, returning its speech as
 * signed 16-bits mono samples (little endian).
 */
public class LpcSampleInputStream
extends      InputStream
implements   AutoCloseable
{
    private final LpcFrameInputStream lpcFrameInputStream;
    private final TMS52xx             tms52xx;

    private short[] samples = new short[200];
    private int     offset  = Integer.MAX_VALUE;


    /**
     * Creates a new instance.
     * @param lpcQuantization     the encoding and quantization of the chip
     *                            (either TMS5200 or TMS5220).
     * @param digitalOutputRange  specifies whether the supported output range
     *                            should be truncated to the analog (12 bits)
     *                            range or the digital (15 bits) range. In any
     *                            case, sample values are finally shifted to a
     *                            16-bits range.
     * @param fullOutputPrecision specifies whether the output precision should
     *                            be the standard analog (8 bits) or digital
     *                            (10 bits) precision, or the full analog (12
     *                            bits) or digital (15 bits) precision.
     * @param lpcFrameInputStream the input stream that provides LPC frames.
     */
    public LpcSampleInputStream(LpcQuantization     lpcQuantization,
                                boolean             digitalOutputRange,
                                boolean             fullOutputPrecision,
                                LpcFrameInputStream lpcFrameInputStream)
    {
        this(new TMS52xx(lpcQuantization, digitalOutputRange, fullOutputPrecision),
             lpcFrameInputStream);
    }


    /**
     * Creates a new instance.
     * @param tms52xx             the speech synthesis chip (either TMS5200 or
     *                            TMS5220, possibly simplified).
     * @param lpcFrameInputStream the input stream that provides LPC frames.
     */
    public LpcSampleInputStream(TMS52xx             tms52xx,
                                LpcFrameInputStream lpcFrameInputStream)
    {
        this.tms52xx             = tms52xx;
        this.lpcFrameInputStream = lpcFrameInputStream;
    }


    // Implementation for InputStream.

    public int read() throws IOException
    {
        // Read 200 new samples if necessary.
        if ((offset >>> 1) >= samples.length)
        {
            LpcFrame lpcFrame = lpcFrameInputStream.readFrame();
            if (lpcFrame == null)
            {
                return -1;
            }

            tms52xx.play(lpcFrame, samples);
            offset = 0;
        }

        // Get the next short sample.
        int sample = samples[offset >>> 1];

        // Get the proper byte from the short sample (little endian).
        if ((offset & 1) == 1)
        {
            sample >>>= 8;
        }

        offset++;

        return sample & 0xff;
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        lpcFrameInputStream.close();
    }
}
