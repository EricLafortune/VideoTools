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
import speech.*;

import java.io.*;

/**
 * This utility converts a pair of files in Praat pitch format and in Praat LPC
 * format to a file in LPC format for the TMS5200 speech synthesizer.
 *
 * Usage:
 *     java ConvertPraatToLpc [-chip name|-tms5200|-tms5220] [-8kHz|-10kHz] [-addstopframe] input.Pitch input.LPC unvoiced_energy_factor voiced_energy_factor output.lpc
 */
public class ConvertPraatToLpc
{
    private static final double DX8                 = 0.025;
    private static final double DX10                = 0.020;
    private static final double SAMPLING_PERIOD8    = 0.000125;
    private static final double SAMPLING_PERIOD10   = 0.000100;
    private static final int    MAX_N_COEFFICIENTS  = 10;


    public static void main(String[] args)
    throws IOException
    {
        int argIndex = 0;

        LpcQuantization quantization    = LpcQuantization.TMS5200;
        double          dx0             = DX8;
        double          samplingPeriod0 = SAMPLING_PERIOD8;
        boolean         addStopFrame    = false;

        while (args[argIndex].startsWith("-"))
        {
            switch (args[argIndex++])
            {
                case "-chip"         -> quantization = LpcQuantization.valueOf(args[argIndex++].toUpperCase());
                case "-tms5200"      -> quantization = LpcQuantization.TMS5200;
                case "-tms5220"      -> quantization = LpcQuantization.TMS5220;
                case "-8kHz"         -> { dx0 = DX8;  samplingPeriod0 = SAMPLING_PERIOD8;  }
                case "-10kHz"        -> { dx0 = DX10; samplingPeriod0 = SAMPLING_PERIOD10; }
                case "-addstopframe" -> addStopFrame = true;
                default              -> throw new IllegalArgumentException("Unknown option [" + args[argIndex-1] + "]");
            }
        }

        String praatPitchFileName   = args[argIndex++];
        String praatLpcFileName     = args[argIndex++];
        double unvoicedEnergyFactor = Double.parseDouble(args[argIndex++]);
        double voicedEnergyFactor   = Double.parseDouble(args[argIndex++]);
        String lpcFileName          = args[argIndex++];

        try (PraatLpcFrameReader lpcFrameReader =
                 new PraatLpcFrameReader(
                 new BufferedReader(
                 new FileReader(praatPitchFileName)),
                 new BufferedReader(
                 new FileReader(praatLpcFileName))))
        {

            // Check a few properties of the input.
            double dx = lpcFrameReader.getDx();
            if (dx != dx0)
            {
                throw new IOException("Unexpected frame time ["+dx+"] instead of ["+dx0+"]");
            }

            double samplingPeriod = lpcFrameReader.getSamplingPeriod();
            if (samplingPeriod != samplingPeriod0)
            {
                throw new IOException("Unexpected sampling period ["+samplingPeriod+"] instead of ["+samplingPeriod0+"]");
            }

            int maxNCoefficients = lpcFrameReader.getMaxNCoefficients();
            if (maxNCoefficients < MAX_N_COEFFICIENTS)
            {
                throw new IOException("Unexpected maximum number of coefficients ["+maxNCoefficients+"] instead of ["+MAX_N_COEFFICIENTS+"]");
            }

            // Convert all frames.
            try (LpcFrameOutput lpcFrameOutput =
                     new RepeatingLpcFrameOutput(
                     new LpcFrameOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(lpcFileName)))))
            {
                // Read all input frames and write the corresponding
                // output frames.
                while (true)
                {
                    PraatLpcFrame inputFrame = lpcFrameReader.readFrame();
                    if (inputFrame == null)
                    {
                        break;
                    }

                    LpcFrame outputFrame =
                        createLpcFrame(quantization,
                                       unvoicedEnergyFactor,
                                       voicedEnergyFactor,
                                       inputFrame.intensity,
                                       inputFrame.frequency,
                                       inputFrame.gain,
                                       inputFrame.predictorCoefficients);

                    lpcFrameOutput.writeFrame(outputFrame);
                }

                if (addStopFrame)
                {
                    lpcFrameOutput.writeFrame(new LpcStopFrame());
                }
            }
        }
    }


    /**
     * Creates an LPC frame, suitable for TMS52xx chips, based on the
     * given information.
     * @param quantization          the quantization strategy of the chip.
     * @param unvoicedEnergyFactor  a factor for scaling all unvoiced energy.
     * @param voicedEnergyFactor    a factor for scaling all voiced energy.
     * @param intensity             the intensity of the frame.
     * @param frequency             the frequency, expressed in Hz.
     * @param gain                  the gain of the predictor filter.
     * @param predictorCoefficients the coefficients of the predictor
     */
    private static LpcFrame createLpcFrame(LpcQuantization quantization,
                                           double          unvoicedEnergyFactor,
                                           double          voicedEnergyFactor,
                                           double          intensity,
                                           double          frequency,
                                           double          gain,
                                           double[]        predictorCoefficients)
    {
        // Praat has already converted the reflection coefficients (bounded
        // between -1 and 1, suitable for use in the lattice filters of the
        // TMS52xx chips) to predictor coefficients (used in the direct
        // predictor form of the filter). We have to convert them back.
        // Note that predictor coefficients are very different when computed
        // for order 4 (unvoiced) compared to order 10 (voiced), but their
        // derived reflection coefficients are just truncated versions, so
        // we can extract them from the same computation for order 10.
        double[] reflectionCoefficients =
            reflectionCoefficientsFromPredictorCoefficients(predictorCoefficients);

        if (frequency <= 0.)
        {
            int energy = quantization.encodeEnergy(unvoicedEnergyFactor *
                                                   (intensity));
            if (energy == 0)
            {
                return new LpcSilenceFrame();
            }

            return new LpcUnvoicedFrame(energy,
                                        quantization.encodeLpcCoefficients(reflectionCoefficients, false));
        }
        else
        {
            int energy = quantization.encodeEnergy(voicedEnergyFactor *
                                                   energyFromGain(gain));
            if (energy == 0)
            {
                return new LpcSilenceFrame();
            }

            return new LpcVoicedFrame(energy,
                                      quantization.encodePitch(frequency),
                                      quantization.encodeLpcCoefficients(reflectionCoefficients, true));
        }
    }


    private static double[] reflectionCoefficientsFromPredictorCoefficients(double[] a)
    {
        int n = a.length;

        double[] k = new double[n];
        double[] b = new double[n];
        double[] c = new double[n];

        // Create a working copy of the array of predictor coefficients.
        System.arraycopy(a, 0, b, 0, n);

        for (int i = n-1; i >= 0; i--)
        {
            k[i] = b[i];

            double e = 1.0 - k[i] * k[i];

            // Create a temporary copy of the first part of the b array.
            System.arraycopy(b, 0, c, 0, i);

            // Update the first part of the b array.
            for (int j = 0; j < i; j++)
            {
                b[j] = (c[j] - k[i]*c[i-j-1]) / e;
            }
        }

        return k;
    }


    private static double energyFromIntensity(double intensity)
    {
        return Math.sqrt(intensity);
    }


    private static double energyFromGain(double gain)
    {
        return Math.sqrt(gain);
    }
}
