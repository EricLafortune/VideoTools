/**
 * Video tools for the TI-99/4A home computer.
 *
 * This file was derived from sn76496.cpp with a BSD-3-Clause in Mame:
 *
 * Copyright (c) Nicola Salmoria
 *
 * Conversion to Java, modification, and cleanup:
 *
 * Copyright (c) 2024 Eric Lafortune
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

import javax.sound.sampled.*;
import java.io.*;

/**
 * This utility converts a binary file in our custom Sound (SND) format to a
 * WAV file (signed, 16 bits, mono), based on simulation with an SN76496
 * Programmable Sound Generator (PSG) chip, or one of its variants
 *
 * Usage:
 *     java ConvertSndToWav [options] input.snd output.wav
 */
public class ConvertSndToWav
{
    public static void main(String[] args)
    throws IOException
    {
        PsgComputer psgComputer           = PsgComputer.TI99;
        double      frameFrequency        = SN76496.NTSC_FRAME_FREQUENCY;
        double      targetSampleFrequency = 20000.0;

        int argIndex = 0;

        while (args[argIndex].startsWith("-"))
        {
            switch (args[argIndex++])
            {
                case "-computer"              -> psgComputer           = PsgComputer.valueOf(args[argIndex++].toUpperCase());
                case "-framefrequency"        -> frameFrequency        = frameFrequency(args[argIndex++]);
                case "-targetsamplefrequency" -> targetSampleFrequency = Double.parseDouble(args[argIndex++]);
                default                       -> throw new IllegalArgumentException("Unknown option [" + args[--argIndex] + "]");
            }
        }

        // Our subsampling has to be an integer.
        int subsampling = (int)Math.round(psgComputer.sampleFrequency() /
                                          targetSampleFrequency);

        // Our number of samples per frame has to be an integer.
        int sampleFrameSize = (int)Math.round(psgComputer.sampleFrequency() /
                                              (subsampling * frameFrequency));

        // The actual sample frequency differ slightly from the target.
        double sampleFrequency = psgComputer.sampleFrequency() / subsampling;

        String inputFileName  = args[argIndex++];
        String outputFileName = args[argIndex++];

        // Count the number of sound frames in the input file.
        int frameCount = 0;

        try (SoundCommandInput sndCommandInput =
                 new SoundCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName))))
        {
            while (sndCommandInput.readFrame() != null)
            {
                frameCount++;
            }
        }

        // Convert the SND frames to signed 16-bit samples and write these
        // samples to a WAV file.
        try (AudioInputStream audioInputStream =
                 new AudioInputStream(
                 new SndSampleInputStream(psgComputer.psgChip, subsampling, sampleFrameSize,
                 new SoundCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName)))),
                 new AudioFormat((float)sampleFrequency, 16, 1, true, false),
                 (long)frameCount * sampleFrameSize))
        {
            AudioSystem.write(audioInputStream,
                              AudioFileFormat.Type.WAVE,
                              new File(outputFileName));
        }
    }


    private static double clockFrequency(String clockFrequency)
    {
        return switch (clockFrequency.toUpperCase())
        {
            case "NTSC" -> SN76496.NTSC_CLOCK_FREQUENCY;
            case "PAL"  -> SN76496.PAL_CLOCK_FREQUENCY;
            default     -> Double.parseDouble(clockFrequency);
        };
    }


    private static double frameFrequency(String frameFrequency)
    {
        return switch (frameFrequency.toUpperCase())
        {
            case "NTSC" -> SN76496.NTSC_FRAME_FREQUENCY;
            case "PAL"  -> SN76496.PAL_FRAME_FREQUENCY;
            default     -> Double.parseDouble(frameFrequency);
        };
    }
}
