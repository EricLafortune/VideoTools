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
import sound.*;

import java.io.*;

/**
 * This utility converts a file in Video Game Music (.vgm) format at 50 Hz
 * to a file in our optimized TMS9919/SN76489 Sound (.snd) format at 60 Hz.
 * It heuristically splits every 5th sound command into two commands, in a
 * way that works for the Bad Apple VGM.
 *
 * Usage:
 *     java ConvertVgmToSnd input.vgm output.snd
 */
public class Convert50HzVgmTo60HzSnd
{
    public static void main(String[] args)
    throws IOException
    {
        int index = 0;

        int frameTime = args.length > 2 ?
            Integer.parseInt(args[index++]) :
            VgmInputStream.FRAME_TIME_50_FPS;

        try (VgmCommandInputStream vgmCommandInputStream =
                 new VgmCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[index++])),
                 frameTime))
        {
            try (SndCommandOutputStream sndCommandOutputStream =
                     new SndCommandOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(args[index++]))))
            {
                int streamTime = 0;

                boolean previousSplit = false;

                SoundCommand[] commands;
                while ((commands = vgmCommandInputStream.readFrame()) != null)
                {
                    streamTime += frameTime;

                    int frameIndex = (streamTime-1) / VgmInputStream.FRAME_TIME_50_FPS;

                    // Is this a 5th frame?
                    boolean split = frameIndex % 5 == 4;

                    if (!previousSplit && split && commands.length > 1)
                    {
                        // Find a place to split the list of commands into two.
                        int splitIndex = 1;
                        while (splitIndex < commands.length)
                        {
                            SoundCommand command = commands[splitIndex];

                            if (splitIndex > 1 &&
                                command.generator == SoundCommand.TONE2)
                            {
                                break;
                            }

                            splitIndex++;

                            if (command instanceof VolumeCommand &&
                                command.generator == SoundCommand.NOISE)
                            {
                                break;
                            }
                        }

                        // Split the list of commands.
                        SoundCommand[] commands1 = new SoundCommand[splitIndex];
                        SoundCommand[] commands2 = new SoundCommand[commands.length - splitIndex];

                        System.arraycopy(commands, 0,
                                         commands1, 0,
                                         commands1.length);

                        System.arraycopy(commands, splitIndex,
                                         commands2, 0,
                                         commands2.length);

                        // Write out the lists.
                        sndCommandOutputStream.writeSoundCommands(commands1);
                        sndCommandOutputStream.writeSoundCommands(commands2);
                    }
                    else
                    {
                        sndCommandOutputStream.writeSoundCommands(commands);
                    }

                    previousSplit = split;
                }
            }
        }
    }
}
