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
 * This class parses and returns sound commands from an input stream in our
 * custom Sound (.snd) format.
 *
 * This format contains an optimized stream of bytes with chunks that can be
 * sent to the video the sound processor (TMS9919 or SN76489).
 *
 * @see SoundCommand
 */
public class SoundCommandInputStream
implements   SoundCommandInput
{
    private static final SoundCommand[] SILENCE_COMMANDS = new SoundCommand[]
    {
        new VolumeCommand(SoundCommand.TONE0, VolumeCommand.SILENT),
        new VolumeCommand(SoundCommand.TONE1, VolumeCommand.SILENT),
        new VolumeCommand(SoundCommand.TONE2, VolumeCommand.SILENT),
        new VolumeCommand(SoundCommand.NOISE, VolumeCommand.SILENT),
    };

    private final InputStream inputStream;
    private       boolean     addSilenceCommands;


    /**
     * Creates a new instance that reads its data from the given input stream.
     */
    public SoundCommandInputStream(InputStream inputStream)
    {
        this(inputStream, false);
    }


    /**
     * Creates a new instance that reads its data from the given input stream
     * and optionally adds a frame to silence all sound generators at the end.
     */
    public SoundCommandInputStream(InputStream inputStream,
                                   boolean     addSilenceCommands)
    {
        this.inputStream        = inputStream;
        this.addSilenceCommands = addSilenceCommands;
    }


    // Implementations for SoundCommandInputStream.

    public SoundCommand[] readFrame() throws IOException
    {
        int size = inputStream.read();

        if (size == -1)
        {
            if (addSilenceCommands)
            {
                addSilenceCommands = false;

                return SILENCE_COMMANDS;
            }

            return null;
        }

        return new SoundCommandParser()
            .parseSoundCommands(inputStream.readNBytes(size));
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
     * Prints out the sample data of the specified VGM file.
     */
    public static void main(String[] args)
    {
        try (SoundCommandInput soundCommandInput =
                 new SoundCommandInputStream(
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
