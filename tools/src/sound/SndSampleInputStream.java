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
 * This InputStream wraps an SoundCommandInputStream, returning its sound as
 * signed 16-bits mono samples (little endian).
 */
public class SndSampleInputStream
extends      InputStream
implements   AutoCloseable
{
    private final SoundCommandInput soundCommandInputStream;
    private final SN76496           sn76496;

    private short[] samples;
    private int     offset = Integer.MAX_VALUE;


    /**
     * Creates a new instance.
     * @param soundCommandInputStream the input stream that provides sound
     *                                frames.
     */
    public SndSampleInputStream(PsgChip           psgChip,
                                int               subsampling,
                                int               sampleFrameSize,
                                SoundCommandInput soundCommandInputStream)
    {
        this(new SN76496(psgChip, subsampling),
             sampleFrameSize,
             soundCommandInputStream);

    }


    /**
     * Creates a new instance.
     * @param sn76496                 the programmable sound generator.
     * @param soundCommandInputStream the input stream that provides sound
     *                                frames.
     */
    public SndSampleInputStream(SN76496           sn76496,
                                int               sampleFrameSize,
                                SoundCommandInput soundCommandInputStream)
    {
        this.sn76496                 = sn76496;
        this.soundCommandInputStream = soundCommandInputStream;
        this.samples                 = new short[sampleFrameSize];
    }


    // Implementation for InputStream.

    public int read() throws IOException
    {
        // Read new samples if necessary.
        // The offset is the byte offset in the short array.
        if ((offset >>> 1) >= samples.length)
        {
            SoundCommand[] soundFrame = soundCommandInputStream.readFrame();
            if (soundFrame == null)
            {
                return -1;
            }

            sn76496.play(soundFrame);
            sn76496.listen(samples);

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
        soundCommandInputStream.close();
    }
}
