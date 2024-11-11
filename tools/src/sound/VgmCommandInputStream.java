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
 * This class parses and returns sound commands from an input stream in Video
 * Game Music (.vgm) format. It ignores all header information, assuming the
 * input only contains SN76489 samples and wait commands. Each frame contains
 * the sample data of a given frame time (duration).
 *
 * @see https://vgmrips.net/wiki/VGM_Specification
 * @see SoundCommand
 */
public class VgmCommandInputStream
    implements SoundCommandInput
{
    private final VgmInputStream vgmInputStream;


    /**
     * Creates a new instance that reads its data from the given input stream
     * with a frame time of {@link VgmInputStream#FRAME_TIME_50_FPS}.
     */
    public VgmCommandInputStream(InputStream inputStream)
    throws IOException
    {
        this(inputStream, VgmInputStream.FRAME_TIME_50_FPS);
    }


    /**
     * Creates a new instance that reads its data from the given input stream
     * with the given frame time.
     */
    public VgmCommandInputStream(InputStream inputStream,
                                 int         frameTime)
    throws IOException
    {
        this.vgmInputStream = new VgmInputStream(inputStream, frameTime);
    }


    // Implementations for SoundCommandInputStream.

    /**
     * Parses, collects, and returns all SN76489 sound commands between the
     * previous request and this request, 1/50th of a second later.
     * @return the sound commands, which may be empty if there haven't been
     *         any samples in this period.
     */
    public SoundCommand[] readFrame() throws IOException
    {
        byte[] soundData = vgmInputStream.readFrame();

        return soundData == null ? null :
            new SoundCommandParser().parseSoundCommands(soundData);
    }


    public void skipFrame() throws IOException
    {
        vgmInputStream.skipFrame();
    }


    public void skipFrames(int count) throws IOException
    {
        vgmInputStream.skipFrames(count);
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        vgmInputStream.close();
    }


    /**
     * Prints out the sample data of the specified VGM file.
     */
    public static void main(String[] args)
    {
        try (SoundCommandInput soundCommandInput =
                 new VgmCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0]))))
        {
            int counter = 0;

            SoundCommand[] soundCommands;
            while ((soundCommands = soundCommandInput.readFrame()) != null)
            {
                System.out.println("#"+(counter++)+":");

                for (int index = 0; index < soundCommands.length; index++)
                {
                    System.out.println("  "+soundCommands[index]);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
