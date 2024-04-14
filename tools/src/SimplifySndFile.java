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
import speech.LpcQuantization;

import java.io.*;
import java.util.*;

/**
 * This utility simplifies a file in our custom Sound (.snd) format,
 * optimized for the TMS9919 / SN76489 sound processor.
 *
 * Usage:
 *     java SimplifySndFile [-addsilencecommands] input.snd output.snd
 */
public class SimplifySndFile
{
    public static void main(String[] args)
    throws IOException
    {
        int argIndex = 0;

        boolean addSilenceCommands = false;
        while (true)
        {
            String arg = args[argIndex];
            if (!arg.startsWith("-"))
            {
                break;
            }

            argIndex++;

            switch (arg)
            {
                case "-addsilencecommands" -> addSilenceCommands = true;
                default -> throw new IllegalArgumentException("Unknown option [" + arg + "]");
            }
        }

        String inputFileName  = args[argIndex++];
        String outputFileName = args[argIndex++];

        try (SndCommandInputStream sndCommandInputStream =
                 new SndCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName)),
                 addSilenceCommands))
        {
            try (SndCommandOutputStream sndCommandOutputStream =
                     new SndCommandOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(outputFileName))))
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
                    FrequencyCommand[] newFrequencies = new FrequencyCommand[4];
                    VolumeCommand[]    newVolumes     = new VolumeCommand[4];

                    System.arraycopy(activeFrequencies, 0, newFrequencies, 0, 4);
                    System.arraycopy(activeVolumes,     0, newVolumes,     0, 4);

                    for (int index = 0; index < commands.length; index++)
                    {
                        SoundCommand command = commands[index];

                        if (command.type() == SoundCommand.TYPE_FREQUENCY)
                        {
                            newFrequencies[command.generator-1] =
                                (FrequencyCommand)command;
                        }
                        else
                        {
                            newVolumes[command.generator-1] =
                                (VolumeCommand)command;
                        }
                    }

                    // Start collecting all relevant commands.
                    ArrayList<SoundCommand> simplifiedCommandList =
                        new ArrayList<>(commands.length);

                    FrequencyCommand noiseFrequency = newFrequencies[SoundCommand.NOISE - 1];
                    VolumeCommand    noiseVolume    = newVolumes[SoundCommand.NOISE - 1];

                    // Loop over all generators.
                    for (int generatorIndex = 0; generatorIndex < 4; generatorIndex++)
                    {
                        FrequencyCommand newFrequency = newFrequencies[generatorIndex];
                        VolumeCommand    newVolume    = newVolumes[generatorIndex];

                        // Write the frequency, if the generator is active.
                        if (// Is the frequency different from before?
                            newFrequency != null &&
                            !newFrequency.equals(activeFrequencies[generatorIndex]) &&
                            (// Is the generator active?
                             (newVolume != null &&
                              newVolume.volume != VolumeCommand.SILENT) ||
                             // Is it generator 3 and is the noise generator
                             // actively following it?
                             (generatorIndex+1               == SoundCommand.TONE3   &&
                              noiseVolume                    != null                 &&
                              noiseVolume.volume             != VolumeCommand.SILENT &&
                              noiseFrequency                 != null                 &&
                              (noiseFrequency.frequency & 3) == 3)))
                        {
                            simplifiedCommandList.add(newFrequency);
                        }

                        // Write the volume, if it has changed.
                        if (// Is the volume different from before?
                            newVolume != null &&
                            !newVolume.equals(activeVolumes[generatorIndex]))
                        {
                            simplifiedCommandList.add(newVolume);
                        }
                    }

                    // Update the active state.
                    activeFrequencies = newFrequencies;
                    activeVolumes     = newVolumes;

                    // Write the simplified commands to the output.
                    SoundCommand[] simplifiedCommands =
                        simplifiedCommandList.toArray(new SoundCommand[simplifiedCommandList.size()]);

                    sndCommandOutputStream.writeSoundCommands(simplifiedCommands);
                }
            }
        }
    }
}
