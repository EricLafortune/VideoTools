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
import speech.*;

import javax.sound.sampled.*;
import java.io.*;

/**
 * This utility converts a binary file in Linear Predictive Coding (LPC) format
 * to a WAV file (signed, 16 bits, mono), based on simulation with a TMS5200
 * speech synthesis chip.
 *
 * Usage:
 *     java ConvertLpcToWav [-tms5200|-tms5220] [-analog|digital] [-precise] input.lpc output.wav
 */
public class ConvertLpcToWav
{
    public static void main(String[] args)
    throws IOException
    {
        int argIndex = 0;

        LpcQuantization quantization        = LpcQuantization.TMS5200;
        boolean         digitalOutputRange  = false;
        boolean         fullOutputPrecision = false;

        while (true)
        {
            String arg = args[argIndex];
            if (!arg.startsWith("-"))
            {
                break;
            }

            switch (arg)
            {
                case "-tms5200" -> quantization        = LpcQuantization.TMS5200;
                case "-tms5220" -> quantization        = LpcQuantization.TMS5220;
                case "-analog"  -> digitalOutputRange  = false;
                case "-digital" -> digitalOutputRange  = true;
                case "-precise" -> fullOutputPrecision = true;
                default         -> throw new IllegalArgumentException("Unknown option [" + arg + "]");
            }

            argIndex++;
        }

        String inputLpcFileName  = args[argIndex++];
        String outputWavFileName = args[argIndex++];

        // Count the number of LPC coefficient frames in the input file.
        int frameCount = 0;

        try (LpcFrameInputStream lpcFrameInputStream =
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputLpcFileName))))
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
                 new LpcSampleInputStream(quantization, digitalOutputRange, fullOutputPrecision,
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputLpcFileName)))),
                 new AudioFormat(8000f, 16, 1, true, false),
                 frameCount * TMS52xx.FRAME_SIZE))
        {
            AudioSystem.write(audioInputStream,
                              AudioFileFormat.Type.WAVE,
                              new File(outputWavFileName));
        }
    }
}
