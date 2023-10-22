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
package speech;

import javax.sound.sampled.*;
import java.io.*;

/**
 * This class simulates a simplified version of the TMS52xx speech synthesizer
 * chip. It plays LPC frames directly, without interpolation or other delays.
 */
public class SimplifiedTMS52xx
extends      TMS52xx
{
    private static final boolean DEBUG = false;


    /**
     * Creates a new instance with a given quantization and 8 bits analog
     * output precision.
     * @param lpcQuantization the encoding and quantization of the chip
     *                        (either TMS5200 or TMS5220).
     */
    public SimplifiedTMS52xx(LpcQuantization lpcQuantization)
    {
        super(lpcQuantization);
    }


    /**    /**
     * Creates a new instance with a given quantization.
     * @param lpcQuantization     the encoding and quantization of the chip
     *                            (either TMS5200 or TMS5220).
     * @param digitalOutputRange  specifies whether the supported output range
     *                            should be truncated to the analog (12 bits)
     *                            range or the digital (15 bits) range. In any
     *                            case, sample values are finally shifted to a
     *                            16-bits range.
     * @param fullOutputPrecision specifies whether the output precision should
     *                            be the standard analog (8 bits) or digital
     *                            (10 bits) precision, or the full analog (12
     *                            bits) or digital (15 bits) precision.
     */
    public SimplifiedTMS52xx(LpcQuantization lpcQuantization,
                             boolean         digitalOutputRange,
                             boolean         fullOutputPrecision)
    {
        super(lpcQuantization, digitalOutputRange, fullOutputPrecision);
    }


    // Implementations for TMS52xx.

    public void play(LpcFrame lpcFrame, short[] samples)
    {
        if (samples.length != FRAME_SIZE)
        {
            throw new IllegalArgumentException("Sample buffer size must be "+FRAME_SIZE+", not "+samples.length);
        }

        // Extract the speech parameters from the LPC frame.
        parseLpcFrame(lpcFrame);

        if (DEBUG)
        {
            System.out.println(lpcFrame+":");
        }

        // Set the speech parameters to their indexed values, straight away.
        setParameters();

        for (int index = 0; index < samples.length; index++)
        {
            // Computes the excitation: noise or chirp.
            int excitation = excitation(pitchIndex);

            // Update the random number generator.
            updateRNG();

            if (DEBUG)
            {
                System.out.println(String.format("%s: [%03d] ene=%d, pit=%d",
                                                 lpcFrame.toString(),
                                                 index,
                                                 energy,
                                                 pitch));
            }

            // Compute the sample.
            int sample = latticeFilter(excitation);

            // Clip the output to 8 or 10 bits precision.
            samples[index] = clip(sample);

            // Update the chirp index, resetting it if necessary.
            updateChirpIndex();
        }
    }


    /**
     * Converts the speech frames of the specified LPC file to sound in the
     * specified new WAV file (signed, 16 bits, mono), based on simulation
     * with a simplified TMS5200 speech synthesis chip.
     */
    public static void main(String[] args)
    throws IOException
    {
        LpcQuantization quantization = LpcQuantization.TMS5200;

        int argIndex = 0;

        while (args[argIndex].startsWith("-"))
        {
            quantization = switch (args[argIndex++])
            {
                case "-tms5200" -> LpcQuantization.TMS5200;
                case "-tms5220" -> LpcQuantization.TMS5220;
                default         -> throw new IllegalArgumentException("Unknown option [" + args[--argIndex] + "]");
            };
        }

        String inputFileName  = args[argIndex++];
        String outputFileName = args[argIndex++];

        // Count the number of LPC coefficient frames in the input file.
        int frameCount = 0;

        try (LpcFrameInputStream lpcFrameInputStream =
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName))))
        {
            LpcFrame frame;
            while ((frame = lpcFrameInputStream.readFrame()) != null)
            {
                frameCount++;

                if (frame instanceof LpcStopFrame)
                {
                    break;
                }
            }
        }

        // Convert the LPC frames to signed 16-bit samples and write these
        // samples to a WAV file.
        try (AudioInputStream audioInputStream =
                 new AudioInputStream(
                 new LpcSampleInputStream(new SimplifiedTMS52xx(quantization, false, true),
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName)))),
                 new AudioFormat(8000f, 16, 1, true, false),
                 frameCount * FRAME_SIZE))
        {
            AudioSystem.write(audioInputStream,
                              AudioFileFormat.Type.WAVE,
                              new File(outputFileName));
        }
    }
}
