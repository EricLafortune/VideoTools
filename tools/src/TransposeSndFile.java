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
import sound.*;

import java.io.*;
import java.util.ArrayList;

/**
 * This utility transposes the frequencies and attenuates the volume of a file
 * in our custom Sound (.snd) format. It can be useful to transfer chip tunes
 * between systems on which the TMS9919 / SN76489 sound processors run at
 * different clock frequencies (e.g. TI-99/4A and BBC Micro) or with different
 * noise feedback shift register widths (e.g. TI-99/4A and Sega Master System).
 *
 * Usage:
 *     java TransposeSndFile [options] input.snd output.snd
 * where standard options are
 *     -frequencies from_clock_frequency to_clock_frequency
 *     -noiseshiftregisters from_noise_shift_register_bits to_noise_shift_register_bits
 *     -addsilencecommands
 *     -attenuation attenuation_shift
 * You can resolve conflicts between audible tone 2 and audible tuned periodic
 * noise with one of
 *     -transposeconflictingtonegenerator
 *     -transposeconflictingnoisegenerator
 *     -silencequietestconflictinggenerator
 *     -silenceconflictingtonegenerator
 *     -silenceconflictingnoisegenerator
 */
public class TransposeSndFile
{
    private static final boolean DEBUG = false;


    public static void main(String[] args)
    throws IOException
    {
        // Parse the options.
        int argIndex = 0;

        boolean transposeConflictingToneGenerator   = false;
        boolean transposeConflictingNoiseGenerator  = false;
        boolean silenceQuietestConflictingGenerator = false;
        boolean silenceConflictingToneGenerator     = false;
        boolean silenceConflictingNoiseGenerator    = false;
        boolean addSilenceCommands                  = false;
        int     attenuation                         = 0;
        double  frequencyDividerFactor              = 1.;
        double  periodicNoiseFrequencyDividerFactor = 1.;

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
                case "-transposeconflictingtonegenerator"   -> transposeConflictingToneGenerator   = true;
                case "-transposeconflictingnoisegenerator"  -> transposeConflictingNoiseGenerator  = true;
                case "-silencequietestconflictinggenerator" -> silenceQuietestConflictingGenerator = true;
                case "-silenceconflictingtonegenerator"     -> silenceConflictingToneGenerator     = true;
                case "-silenceconflictingnoisegenerator"    -> silenceConflictingNoiseGenerator    = true;
                case "-addsilencecommands"                  -> addSilenceCommands                  = true;
                case "-attenuation"                         -> attenuation =
                    Integer.parseInt(args[argIndex++]);
                case "-frequencies"                         -> frequencyDividerFactor =
                    1.0 / frequency(args[argIndex++]) *
                    frequency(args[argIndex++]);
                case "-noiseshiftregisters"                 -> periodicNoiseFrequencyDividerFactor =
                    (double)shiftRegisterWidth(args[argIndex++]) /
                    (double)shiftRegisterWidth(args[argIndex++]);
                default -> throw new IllegalArgumentException("Unknown option [" + arg + "]");
            }
        }

        // Process the input file.
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

                    // Update the original, untransposed state, based on the
                    // commands.
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
                            VolumeCommand volumeCommand =
                            originalVolumes[command.generator] =
                                (VolumeCommand)command;

                            // Adjust the attenuation right away.
                            volumeCommand.attenuation =
                                attenuation(volumeCommand.attenuation,
                                            attenuation);
                        }
                    }

                    // Transpose all commands into new arrays.
                    FrequencyCommand[] newFrequencies = new FrequencyCommand[4];
                    VolumeCommand[]    newVolumes     = new VolumeCommand[4];

                    System.arraycopy(activeFrequencies, 0, newFrequencies, 0, 4);
                    System.arraycopy(activeVolumes,     0, newVolumes,     0, 4);

                    FrequencyCommand noiseFrequency = originalFrequencies[SoundCommand.NOISE];
                    VolumeCommand    noiseVolume    = originalVolumes[SoundCommand.NOISE];
                    VolumeCommand    tuningVolume   = originalVolumes[SoundCommand.TONE2];

                    // Transposing periodic noise is done through its tuning
                    // tone 2. This may even lead to a tuning conflict if both
                    // generators are audible:
                    //     https://gist.github.com/simondotm/84a08066469866d7f2e6aad875e598be
                    // Are the tone 2 generator and the tuned periodic noise
                    // generator audible at the same time, and is the changing
                    // noise shift register width causing one of them to get
                    // out of tune?
                    boolean conflictingNoiseGenerator =
                        noiseFrequency.isTunedPeriodicNoise() &&
                        !noiseVolume.isSilent()               &&
                        !tuningVolume.isSilent()              &&
                        periodicNoiseFrequencyDividerFactor != 1.;

                    String comment = "";

                    // Transpose all active tone frequencies.
                    for (int generator = SoundCommand.TONE0;
                         generator    <= SoundCommand.TONE2;
                         generator++)
                    {
                        FrequencyCommand frequency = originalFrequencies[generator];
                        VolumeCommand    volume    = originalVolumes[generator];

                        // Is the generator active?
                        if (!volume.isSilent() ||
                            (frequency.isNoiseTuningTone() &&
                             !noiseVolume.isSilent()))
                        {
                            // Compute the transposed frequency, accounting
                            // for the noise shift register if necessary.
                            double generatorFrequencyDividerFactor =
                                frequency.isNoiseTuningTone()         &&
                                noiseFrequency.isTunedPeriodicNoise() &&
                                (!conflictingNoiseGenerator         ||
                                 transposeConflictingNoiseGenerator ||
                                 silenceConflictingToneGenerator    ||
                                 !transposeConflictingToneGenerator &&
                                 !silenceConflictingNoiseGenerator  &&
                                 noiseVolume.attenuation < tuningVolume.attenuation) ?
                                    frequencyDividerFactor * periodicNoiseFrequencyDividerFactor :
                                    frequencyDividerFactor;

                            if (DEBUG &&
                                conflictingNoiseGenerator &&
                                frequency.isNoiseTuningTone())
                            {
                                comment = "Factor="+generatorFrequencyDividerFactor+" ";
                            }

                            int transposedFrequency =
                                transposedFrequencyDivider(frequency.divider,
                                                           generatorFrequencyDividerFactor);

                            // Overwrite the tone frequency.
                            newFrequencies[generator] =
                                new FrequencyCommand(generator,
                                                     transposedFrequency);
                        }
                    }

                    // Copy the noise frequency, if it's active.
                    if (!noiseVolume.isSilent())
                    {
                        // Copy the noise frequency.
                        newFrequencies[SoundCommand.NOISE] = noiseFrequency;
                    }

                    // Copy all original volumes.
                    System.arraycopy(originalVolumes, 0, newVolumes, 0, 4);

                    // Silence a generator if requested in case of conflict,
                    // that is, if both the tuned periodic noise generator
                    // and the tone 2 generator are audible, with a shift
                    // feedback register that is not 16 bits.
                    if (conflictingNoiseGenerator &&
                        (silenceQuietestConflictingGenerator ||
                         silenceConflictingToneGenerator     ||
                         silenceConflictingNoiseGenerator))
                    {
                        // Which generator shall we silence?
                        int generator =
                            silenceConflictingToneGenerator                    ? SoundCommand.TONE2 :
                            silenceConflictingNoiseGenerator                   ? SoundCommand.NOISE :
                            noiseVolume.attenuation < tuningVolume.attenuation ? SoundCommand.TONE2 :
                                                                                 SoundCommand.NOISE;
                        if (DEBUG)
                        {
                            comment += " Silencing "+generator;
                        }

                        // Overwrite the volume.
                        newVolumes[generator] =
                            new VolumeCommand(generator, VolumeCommand.SILENT);
                    }

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
                        System.out.println(comment);
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

                    sndCommandOutputStream.writeSoundCommands(simplifiedCommands);
                }
            }
        }
    }


    /**
     * Parses the given frequency string.
     */
    private static double frequency(String frequencyString)
    {
        return switch (frequencyString)
        {
            case "ti99",
                 "ntsc" -> 3579545;
            case "pal"  -> 3546893;
            case "bbc"  -> 4000000;
            default     -> Double.parseDouble(frequencyString);
        };
    }


    /**
     * Parses the given noise feedback shift register width string.
     */
    private static int shiftRegisterWidth(String shiftRegisterWidthString)
    {
        return switch (shiftRegisterWidthString)
        {
            case "ti99",
                 "bbc" -> 15;
            case "sms" -> 16;
            default    -> Integer.parseInt(shiftRegisterWidthString);
        };
    }


    /**
     * Sums the given attenuations, within the supported bounds.
     */
    private static int attenuation(int attenuation1,
                                   int attenuation2)
    {
        return Integer.max(0,
               Integer.min(VolumeCommand.SILENT,
                           attenuation1 + attenuation2));
    }


    /**
     * Transposes the given frequency divider, within the supported bounds.
     */
    private static int transposedFrequencyDivider(int    frequencyDivider,
                                                  double frequencyDividerFactor)
    {
        return Integer.max(FrequencyCommand.MIN_DIVIDER,
               Integer.min(FrequencyCommand.MAX_DIVIDER,
                           (int)Math.round(frequencyDivider * frequencyDividerFactor)));
    }
}
