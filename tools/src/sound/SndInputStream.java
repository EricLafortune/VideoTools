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
 * This SndInput returns raw data frames from an input stream in our custom
 * Sound (.snd) format for the TMS9919 / SN76489 sound processor.
 */
public class SndInputStream
implements   SndInput
{
    private final InputStream inputStream;


    /**
     * Creates a new instance that reads its data from the given input stream.
     */
    public SndInputStream(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }


    // Implementations for SndInput.

    public byte[] readFrame() throws IOException
    {
        int size = inputStream.read();
        if (size == -1)
        {
            return null;
        }

        return inputStream.readNBytes(size);
    }


    public void skipFrame() throws IOException
    {
        int size = inputStream.read();
        if (size == -1)
        {
            return;
        }

        inputStream.skipNBytes((long)size);
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
    {
        try (SndInput sndInput =
                 new SndInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0]))))
        {
            int counter = 0;

            byte[] soundData;
            while ((soundData = sndInput.readFrame()) != null && counter < 10000)
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
