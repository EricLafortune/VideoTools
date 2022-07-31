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
import java.util.*;

/**
 * This utility simplifies a file in our custom Sound (.snd) format,
 * optimized for the the TMS9919 / SN76489 sound processor.
 * .
 */
public class SimplifySndFile
{
    public static void main(String[] args)
    throws IOException
    {
        int argIndex = 0;

        boolean addSilenceCommands = false;
        if (args[argIndex].equals("-addsilencecommands"))
        {
            addSilenceCommands = true;
            argIndex++;
        }

        try (SndCommandInputStream sndCommandInputStream =
                 new SndCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[argIndex++])),
                 addSilenceCommands))
        {
            try (SndCommandOutputStream sndCommandOutputStream =
                     new SndCommandOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(args[argIndex++]))))
            {
                FrequencyCommand[] activeFrequencies = new FrequencyCommand[4];
                VolumeCommand[]    activeVolumes     = new VolumeCommand[4];

                while (true)
                {
                    SoundCommand[] commands = sndCommandInputStream.readFrame();
                    if (commands == null)
                    {
                        break;
                    }

                    // Collect all commands in structured arrays, overwriting
                    // any earlier commands of the same generator and type.
                    FrequencyCommand[] frameFrequencies = new FrequencyCommand[4];
                    VolumeCommand[]    frameVolumes     = new VolumeCommand[4];

                    FrequencyCommand[] resultingFrequencies = new FrequencyCommand[4];
                    VolumeCommand[]    resultingVolumes     = new VolumeCommand[4];

                    System.arraycopy(activeFrequencies, 0, resultingFrequencies, 0, 4);
                    System.arraycopy(activeVolumes,     0, resultingVolumes,     0, 4);

                    for (int index = 0; index < commands.length; index++)
                    {
                        SoundCommand command = commands[index];

                        if (command.type() == SoundCommand.TYPE_FREQUENCY)
                        {
                            frameFrequencies[command.generator-1]     =
                            resultingFrequencies[command.generator-1] =
                                (FrequencyCommand)command;
                        }
                        else
                        {
                            frameVolumes[command.generator-1]     =
                            resultingVolumes[command.generator-1] =
                                (VolumeCommand)command;
                        }
                    }

                    // Start collecting all relevant commands.
                    ArrayList<SoundCommand> simplifiedCommandList =
                        new ArrayList<>(commands.length);

                    FrequencyCommand noiseFrequency = resultingFrequencies[SoundCommand.NOISE - 1];
                    VolumeCommand    noiseVolume    = resultingVolumes[SoundCommand.NOISE - 1];

                    // Loop over all generators.
                    for (int generatorIndex = 0; generatorIndex < 4; generatorIndex++)
                    {
                        FrequencyCommand frameFrequency = frameFrequencies[generatorIndex];
                        VolumeCommand    frameVolume    = frameVolumes[generatorIndex];

                        // Write the frequency, if the generator is active.
                        if (// Is the frequency different from before?
                            frameFrequency != null &&
                            !frameFrequency.equals(activeFrequencies[generatorIndex]) &&
                            (// Is the generator active?
                             (resultingVolumes[generatorIndex] != null &&
                              resultingVolumes[generatorIndex].volume != VolumeCommand.SILENT) ||
                             // Is it generator 3 and is the noise generator
                             // actively following it?
                             (generatorIndex+1               == SoundCommand.TONE3   &&
                              noiseVolume                    != null                 &&
                              noiseVolume.volume             != VolumeCommand.SILENT &&
                              noiseFrequency                 != null                 &&
                              (noiseFrequency.frequency & 3) == 3)))
                        {
                            simplifiedCommandList.add(frameFrequency);
                        }

                        // Write the volume, if it has changed.
                        if (// Is the volume different from before?
                            frameVolume != null &&
                            !frameVolume.equals(activeVolumes[generatorIndex]))
                        {
                            simplifiedCommandList.add(frameVolume);
                        }
                    }

                    // Update the active state.
                    activeFrequencies = resultingFrequencies;
                    activeVolumes     = resultingVolumes;;

                    // Write the simplified commands to the output.
                    SoundCommand[] simplifiedCommands =
                        simplifiedCommandList.toArray(new SoundCommand[simplifiedCommandList.size()]);

                    sndCommandOutputStream.writeSoundCommands(simplifiedCommands);
                }
            }
        }
    }
}
