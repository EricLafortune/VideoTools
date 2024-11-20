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
package sound;

import java.io.*;

/**
 * This SoundCommandOutput writes sound commands to an output stream in our
 * custom Sound (.snd) format.
 *
 * This format contains an optimized stream of bytes with chunks that can be
 * sent to the video the sound processor (TMS9919 or SN76489).
 *
 * @see SoundCommand
 */
public class SndCommandOutputStream
implements   SoundCommandOutput
{
    private final OutputStream outputStream;


    /**
     * Creates a new instance that writes its data to the given output stream.
     */
    public SndCommandOutputStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }


    // Implementations for SoundCommandOutput.

    public void writeFrame(SoundCommand[] soundFrame)
    throws IOException
    {
        // Compute and write the total size of all commands.
        int dataByteCount = 0;
        for (int index = 0; index < soundFrame.length; index++)
        {
            dataByteCount += soundFrame[index].toBytes().length;
        }

        outputStream.write(dataByteCount);

        // Write the commands themselves.
        for (int index = 0; index < soundFrame.length; index++)
        {
            outputStream.write(soundFrame[index].toBytes());
        }
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        outputStream.close();
    }


    /**
     * Copies the commands of the specified SND file to the specified new file.
     */
    public static void main(String[] args)
    {
        try (SoundCommandInput soundCommandInput =
                 new SoundCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0]))))
        {
            try (SoundCommandOutput soundCommandOutput =
                     new SndCommandOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(args[1]))))
            {
                SoundCommand[] commands;
                while ((commands = soundCommandInput.readFrame()) != null)
                {
                    soundCommandOutput.writeFrame(commands);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
