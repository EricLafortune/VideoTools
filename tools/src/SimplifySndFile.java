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
 * optimized for the TMS9919 / SN76489 sound processor.
 *
 * Usage:
 *     java SimplifySndFile [-addsilencecommands] input.snd output.snd
 */
public class SimplifySndFile
{
    private static final boolean DEBUG = false;


    public static void main(String[] args)
    throws IOException
    {
        // Parse the options.
        int argIndex = 0;

        boolean addSilenceCommands = false;

        // Process the input file.
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

        try (SoundCommandInputStream sndCommandInputStream =
                 new SoundCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName)),
                 addSilenceCommands))
        {
            try (SoundCommandOutput soundCommandOutput =
                     new SndCommandOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(outputFileName))))
            {
                FrequencyCommand[] originalFrequencies = new FrequencyCommand[4];
                VolumeCommand[]    originalVolumes     = new VolumeCommand[4];

                FrequencyCommand[] activeFrequencies = new FrequencyCommand[4];
                VolumeCommand[]    activeVolumes     = new VolumeCommand[4];

                // Initialize all generators.
                for (int generator = SoundCommand.TONE0;
                     generator    <= SoundCommand.NOISE;
                     generator++)
                {
                    originalFrequencies[generator] =
                    activeFrequencies[generator] =
                        new FrequencyCommand(generator, 0);

                    originalVolumes[generator] =
                    activeVolumes[generator] =
                        new VolumeCommand(generator, VolumeCommand.SILENT);
                }

                // Process all sound frames.
                while (true)
                {
                    // Read the commands of this frame.
                    SoundCommand[] commands = sndCommandInputStream.readFrame();
                    if (commands == null)
                    {
                        break;
                    }

                    // Update the original state, based on the commands.
                    for (int index = 0; index < commands.length; index++)
                    {
                        SoundCommand command = commands[index];

                        if (command.type() == SoundCommand.TYPE_FREQUENCY)
                        {
                            originalFrequencies[command.generator] =
                                (FrequencyCommand)command;
                        }
                        else
                        {
                            originalVolumes[command.generator] =
                                (VolumeCommand)command;
                        }
                    }

                    // Copy all commands into new arrays.
                    FrequencyCommand[] newFrequencies = new FrequencyCommand[4];
                    VolumeCommand[]    newVolumes     = new VolumeCommand[4];

                    System.arraycopy(activeFrequencies, 0, newFrequencies, 0, 4);
                    System.arraycopy(activeVolumes,     0, newVolumes,     0, 4);

                    VolumeCommand noiseVolume = originalVolumes[SoundCommand.NOISE];

                    // Copy all active frequencies.
                    for (int generator = SoundCommand.TONE0;
                         generator    <= SoundCommand.NOISE;
                         generator++)
                    {
                        FrequencyCommand frequency = originalFrequencies[generator];
                        VolumeCommand    volume    = originalVolumes[generator];

                        // Is the generator active?
                        if (!volume.isSilent() ||
                            (frequency.isNoiseTuningTone() &&
                             !noiseVolume.isSilent()))
                        {
                            // Copy the tone frequency.
                            newFrequencies[generator] = frequency;
                        }
                    }

                    // Copy all original volumes.
                    System.arraycopy(originalVolumes, 0, newVolumes, 0, 4);

                    if (DEBUG)
                    {
                        // Print out the original state.
                        for (int generator = SoundCommand.TONE0;
                             generator    <= SoundCommand.NOISE;
                             generator++)
                        {
                            System.out.printf("%04x %1x ",
                                              originalFrequencies[generator].divider,
                                              originalVolumes[generator].attenuation);
                        }
                        System.out.print("   ");

                        // Print out the changes of the new state.
                        for (int generator = SoundCommand.TONE0;
                             generator    <= SoundCommand.NOISE;
                             generator++)
                        {
                            FrequencyCommand newFrequency = newFrequencies[generator];
                            VolumeCommand    newVolume    = newVolumes[generator];

                            // Has the frequency changed?
                            if (!newFrequency.equals(activeFrequencies[generator]))
                            {
                                System.out.printf("%04x ", newFrequencies[generator].divider);
                            }
                            else
                            {
                                System.out.print(".... ");
                            }

                            // Has the volume changed?
                            if (!newVolume.equals(activeVolumes[generator]))
                            {
                                System.out.printf("%1x ", newVolumes[generator].attenuation);
                            }
                            else
                            {
                                System.out.print(". ");
                            }
                        }
                        System.out.println();
                    }

                    // Start collecting all changes between the active state
                    // and the new state.
                    ArrayList<SoundCommand> simplifiedCommandList =
                        new ArrayList<>(commands.length);

                    // Collect the changed frequency and volume commands.
                    for (int generator = SoundCommand.TONE0;
                         generator    <= SoundCommand.NOISE;
                         generator++)
                    {
                        FrequencyCommand newFrequency = newFrequencies[generator];
                        VolumeCommand    newVolume    = newVolumes[generator];

                        // Has the frequency changed?
                        if (!newFrequency.equals(activeFrequencies[generator]))
                        {
                            // Collect it.
                            simplifiedCommandList.add(newFrequency);
                        }

                        // Has the volume changed?
                        if (!newVolume.equals(activeVolumes[generator]))
                        {
                            // Collect it.
                            simplifiedCommandList.add(newVolume);
                        }
                    }

                    // Update the active state.
                    activeFrequencies = newFrequencies;
                    activeVolumes     = newVolumes;

                    // Write the simplified commands to the output.
                    SoundCommand[] simplifiedCommands =
                        simplifiedCommandList.toArray(new SoundCommand[simplifiedCommandList.size()]);

                    soundCommandOutput.writeFrame(simplifiedCommands);
                }
            }
        }
    }
}
