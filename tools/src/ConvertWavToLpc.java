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
import java.util.Arrays;

/**
 * This utility converts a WAV file (signed, 16 bits, mono) containing speech to
 * a binary file in Linear Predictive Coding (LPC) format, based on optimization
 * and simulation for a TMS52xx speech synthesis chip.
 *
 * Usage:
 *     java ConvertWavToLpc [options...] input.wav output.lpc
 */
public class ConvertWavToLpc
{
    private static final boolean DEBUG = false;

    private static final int STEP_SIZE = TMS52xx.FRAME_SIZE;

    private static final int    OUTLIER_WINDOW_FRAME_COUNT = 5;    // The sliding window (number of frames) over which to look for outliers.
    private static final double PITCH_OUTLIER_THRESHOLD    = 0.25; // The relative threshold for outlier pitches.
    private static final int    MIN_VOICE_FRAME_COUNT      = 2;    // The minimum number of consecutive voiced/unvoiced frames.

    private final LpcQuantization quantization;
    private final double          amplification;
    private final double          preemphasis;
    private final int             minPitch;
    private final int             maxPitch;
    private final double          voicedThreshold;
    private final int             frameOversampling;
    private final int             lpcWindowSize;
    private final int             lpcWindowSizePow2;
    private final boolean         fixPitchOutliers;
    private final boolean         fixVoicedJittering;
    private final boolean         optimizeFrames;
    private final int             optimizationWindowSize;
    private final int             optimizationWindowSizePow2;
    private final double          linearPowerShift;
    private final boolean         fixEnergyTransitions;
    private final boolean         fixClampedSamples;
    private final boolean         trimSilenceFrames;
    private final boolean         addStopFrame;


    public static void main(String[] args)
    throws IOException, UnsupportedAudioFileException
    {
        int argIndex = 0;

        LpcQuantization quantization             = LpcQuantization.TMS5200;
        double          amplification            = 1.0;
        double          preemphasis              = 0.9373;
        double          minFrequency             = 30.0;
        double          maxFrequency             = 600.0;
        double          voicedThreshold          = 0.25;
        int             lpcWindowSize            = 400;
        boolean         fixPitchOutliers         = true;
        boolean         fixVoicedJittering       = true;
        int             frameOversampling        = 1;
        boolean         optimizeFrames           = true;
        int             optimizationWindowSize   = 256;
        double          linearPowerShift         = 0.1;
        boolean         fixEnergyTransitions     = true;
        boolean         fixClampedSamples        = true;
        boolean         trimSilenceFrames        = false;
        boolean         addStopFrame             = false;

        while (args[argIndex].startsWith("-"))
        {
            switch (args[argIndex++])
            {
                case "-chip"                     -> quantization           = LpcQuantization.valueOf(args[argIndex++].toUpperCase());
                case "-tms5200"                  -> quantization           = LpcQuantization.TMS5200;
                case "-tms5220"                  -> quantization           = LpcQuantization.TMS5220;
                case "-amplification"            -> amplification          = Double.parseDouble(args[argIndex++]);
                case "-preemphasis"              -> preemphasis            = Double.parseDouble(args[argIndex++]);
                case "-minfrequency"             -> minFrequency           = Double.parseDouble(args[argIndex++]);
                case "-maxfrequency"             -> maxFrequency           = Double.parseDouble(args[argIndex++]);
                case "-voicedthreshold"          -> voicedThreshold        = Double.parseDouble(args[argIndex++]);
                case "-frameoversampling"        -> frameOversampling      = Integer.parseInt(args[argIndex++]);
                case "-lpcwindowsize"            -> lpcWindowSize          = Integer.parseInt(args[argIndex++]);
                case "-dontfixpitchoutliers"     -> fixPitchOutliers       = false;
                case "-dontfixvoicedjittering"   -> fixVoicedJittering     = false;
                case "-dontoptmizeframes"        -> optimizeFrames         = false;
                case "-optimizationwindowsize"   -> optimizationWindowSize = Integer.parseInt(args[argIndex++]);
                case "-linearpowershift"         -> linearPowerShift       = Double.parseDouble(args[argIndex++]);
                case "-dontfixenergytransitions" -> fixEnergyTransitions   = false;
                case "-dontfixclampedsamples"    -> fixClampedSamples      = false;
                case "-trimsilenceframes"        -> trimSilenceFrames      = true;
                case "-addstopframe"             -> addStopFrame           = true;
                default                          -> throw new IllegalArgumentException("Unknown option [" + args[argIndex-1] + "]");
            }
        }

        String inputWavFileName  = args[argIndex++];
        String outputLpcFileName = args[argIndex++];

        new ConvertWavToLpc(quantization,
                            amplification,
                            preemphasis,
                            minFrequency,
                            maxFrequency,
                            voicedThreshold,
                            frameOversampling,
                            lpcWindowSize,
                            fixPitchOutliers,
                            fixVoicedJittering,
                            optimizeFrames,
                            optimizationWindowSize,
                            linearPowerShift,
                            fixEnergyTransitions,
                            fixClampedSamples,
                            trimSilenceFrames,
                            addStopFrame)
            .process(inputWavFileName,
                     outputLpcFileName);
    }


    public ConvertWavToLpc(LpcQuantization quantization,
                           double          amplification,
                           double          preemphasis,
                           double          minFrequency,
                           double          maxFrequency,
                           double          voicedThreshold,
                           int             frameOversampling,
                           int             lpcWindowSize,
                           boolean         fixPitchOutliers,
                           boolean         fixVoicedJittering,
                           boolean         optimizeFrames,
                           int             optimizationWindowSize,
                           double          linearPowerShift,
                           boolean         fixEnergyTransitions,
                           boolean         fixClampedSamples,
                           boolean         trimSilenceFrames,
                           boolean         addStopFrame)
    {
        this.quantization               = quantization;
        this.amplification              = amplification;
        this.preemphasis                = preemphasis;
        this.minPitch                   = quantization.pitch(maxFrequency);
        this.maxPitch                   = quantization.pitch(minFrequency);
        this.voicedThreshold            = voicedThreshold;
        this.frameOversampling          = frameOversampling;
        this.lpcWindowSize              = lpcWindowSize;
        this.lpcWindowSizePow2          = Integer.highestOneBit(lpcWindowSize - 1) << 1;
        this.fixPitchOutliers           = fixPitchOutliers;
        this.fixVoicedJittering         = fixVoicedJittering;
        this.optimizeFrames             = optimizeFrames;
        this.optimizationWindowSize     = optimizationWindowSize;
        this.optimizationWindowSizePow2 = Integer.highestOneBit(optimizationWindowSize - 1) << 1;
        this.linearPowerShift           = linearPowerShift;
        this.fixEnergyTransitions       = fixEnergyTransitions;
        this.fixClampedSamples          = fixClampedSamples;
        this.trimSilenceFrames          = trimSilenceFrames;
        this.addStopFrame               = addStopFrame;
    }


    private void process(String inputWavFileName,
                         String outputLpcFileName)
    throws IOException, UnsupportedAudioFileException
    {
        int stepSize = STEP_SIZE / frameOversampling;

        // Allocate reusable object instances and buffers.
        short[] lpcBuffer = new short[TMS52xx.FRAME_SIZE];

        int[]    pitches         = new int[1000];
        double[] maxCorrelations = new double[1000];

        int frameCounter = 0;

        // There are two ways to extract/match the spectral envelope:
        // - With linear prediction, e.g. via autocorrelation.
        // - With cepstral smoothing.
        // https://ccrma.stanford.edu/~jos/sasp/Spectral_Envelope_Extraction.html

        // Estimate pitches and maximum autocorrelations.
        // The window size is the (large) initial window size.
        // The step size is the oversampling step size.
        // +-------+
        //   +-------+
        //     +-------+
        //       +-------+
        try (AudioFrameInputStream audioFrameInputStream =
                 new AudioFrameInputStream(
                 AudioSystem.getAudioInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputWavFileName))),
                     lpcWindowSize,
                     stepSize))
        {
            FFT fft = new FFT();
            double[] wavSamples = new double[lpcWindowSize];

            // Read frames from the WAV file.
            while (audioFrameInputStream.readFrame(wavSamples) > 0)
            {
                // The scale doesn't matter yet at this point.
                //fft.scale(wavSamples, amplification);

                // Apply a window and preemphasize the high frequencies.
                filterSamples(fft,
                              wavSamples,
                              lpcWindowSize);

                // Estimate and remember the pitch.
                // For men,    50 - 250 Hz => pitch 32 - 160 samples (at 8 kHz).
                // For women, 120 - 500 Hz => pitch 16 -  67 samples.
                // We could use a low-pass filter to reduce noise (for voiced speech).
                int pitch = fft.estimatePitch(wavSamples, minPitch, maxPitch);

                pitches = addValue(pitches, frameCounter, pitch);

                // Compute and remember the corresponding autocorrelation, to
                // distinguish between voiced speech and unvoiced speech later
                // on.
                double normalization = fft.autocorrelation(wavSamples, 0);

                double autoCorrelation = normalization == 0 ? 0.0 :
                    fft.autocorrelation(wavSamples, pitch) / normalization;

                maxCorrelations = addValue(maxCorrelations,
                                           frameCounter,
                                           autoCorrelation);

                frameCounter++;
            }
        }

        if (fixPitchOutliers)
        {
            // Fix any outliers of the pitches.
            fixPitchOutliers(pitches, frameCounter);
        }

        if (fixVoicedJittering)
        {
            // Fix any outliers of the autocorrelations.
            fixVoicedJittering(maxCorrelations, frameCounter);
        }

        ParameterizedFrame[] parameterizedFrames =
            new ParameterizedFrame[frameCounter];

        // Compute an initial solution.
        // Convert all frames, independently, with simple, non-interpolating LPC.
        // The window size is the (large) initial window size.
        // The step size is the oversampling step size.
        // +-------+
        //   +-------+
        //     +-------+
        //       +-------+
        try (AudioFrameInputStream audioFrameInputStream =
                 new AudioFrameInputStream(
                 AudioSystem.getAudioInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputWavFileName))),
                     lpcWindowSize,
                     stepSize))
        {
            // We'll use a separate speech processor for each oversampled
            // frame.
            TMS52xx[] tms52xxs = new TMS52xx[frameOversampling];
            Arrays.setAll(tms52xxs, i -> new SimplifiedTMS52xx(quantization, false, true));

            int lpcDelay = 0;

            FFT fft = new FFT();
            double[] wavSamples = new double[lpcWindowSizePow2];
            double[] lpcSamples = new double[lpcWindowSizePow2];

            double[] r = new double[11];
            double[] k = new double[10];

            frameCounter = 0;

            // Read frames from the WAV file.
            while (audioFrameInputStream.readFrame(wavSamples) > 0)
            {
                TMS52xx tms52xx = tms52xxs[frameCounter % frameOversampling];

                fft.scale(wavSamples, amplification);

                // Apply a window and preemphasize the high frequencies.
                filterSamples(fft,
                              wavSamples,
                              lpcWindowSize);

                fft.computeAutocorrelation(wavSamples, r);
                fft.estimateReflectionCoefficientsLeRouxGueguen(r, k);
                //fft.estimateReflectionCoefficientsLevinsonDurbin(r, k);

                // Compute the energy of the raw sound frame.
                double wavEnergy = fft.rms(wavSamples, 0, lpcWindowSize);

                int pitch = pitches[frameCounter];

                // There are many possible criteria to decide between voiced
                // or unvoiced excitation: zero-crossing count, log energy,
                // normalized autocorrelation coefficient at a delay of 1
                // sample, first predictor coefficient, normalized prediction
                // error:
                // https://www.clear.rice.edu/elec532/PROJECTS00/vocode/uv/uvdet.html
                //boolean voiced = r[1] / r[0] > 0.25;
                //boolean voiced = k[0] < 0.25;
                // We use the normalized maximum autocorrelation:
                // http://www.isle.illinois.edu/speech_web_lg/coursematerials/ece401/fa2020/slides/lec17.pdf
                boolean voiced = maxCorrelations[frameCounter] >= voicedThreshold;

                // Create a voiced frame or an unvoiced frame that we can
                // still optimize.
                ParameterizedFrame parameterizedFrame =
                    voiced ?
                        new ParameterizedVoicedFrame(new LpcVoicedFrame(quantization.maxEncodedEnergy(),
                                                                        quantization.encodePitch(pitch),
                                                                        quantization.encodeLpcCoefficients(k, true))) :
                        new ParameterizedUnvoicedFrame(new LpcUnvoicedFrame(quantization.maxEncodedEnergy(),
                                                                            quantization.encodeLpcCoefficients(k, false)));

                // Set the best matching frame energy (simple RMS of the samples).
                LpcEnergyFrame frame =
                    (LpcEnergyFrame)parameterizedFrame.getFrame();

                optimizeEnergy(wavEnergy,
                               frame,
                               fft,
                               tms52xx,
                               lpcBuffer,
                               lpcDelay,
                               lpcSamples,
                               lpcWindowSize);

                // Don't try to optimize the LPC parameters of a silent frame,
                // only the energy.
                if (frame.energy == 0)
                {
                    parameterizedFrame =
                        new ParameterizedEnergyFrame(frame);
                }

                // Remember the parameterized frame, so we can optimize it in a moment.
                parameterizedFrames[frameCounter] = parameterizedFrame;

                // Apply the frame to the speech synthesizer.
                tms52xx.play(frame, lpcBuffer);

                frameCounter++;
            }
        }

        LpcFrame[] frames = new LpcFrame[frameCounter];

        // Optimize on the solution in subsequent iterations.
        // The window size is the (smaller) optimization window size.
        // The step size is the oversampling step size.
        // The windows are centered on the corresponding initial windows.
        // +-------+       (initial)
        //   +-------+
        //     +-------+
        //       +-------+
        //   +===+         (optimization)
        //     +===+
        //       +===+
        //         +===+
        if (optimizeFrames)
        {
            // Convert all frames, combined in a sequence, with interpolating LPC.
            // Window: +======+              (overlapping windows of 256 samples)
            // WAV:    +====+=---+----+----+ (frames of of 200 samples)
            // LPC:    +---=+====+----+----+ (optimized frames to cover windows)
            int skipCount = (lpcWindowSize - optimizationWindowSize) / 2;

            try (AudioFrameInputStream audioFrameInputStream =
                     new AudioFrameInputStream(
                     AudioSystem.getAudioInputStream(
                     new BufferedInputStream(
                     new FileInputStream(inputWavFileName))),
                         optimizationWindowSizePow2,
                         stepSize,
                         skipCount))
            {
                FFT fft = new FFT();

                double[] wavSamples  = new double[optimizationWindowSizePow2];
                double[] lpcSamples  = new double[optimizationWindowSizePow2];
                double[] samplesImag = new double[optimizationWindowSizePow2];
                double[] wavPower    = new double[optimizationWindowSizePow2 / 2 + 1];
                double[] lpcPower    = new double[optimizationWindowSizePow2 / 2 + 1];

                // We'll use a separate speech processor for each oversampled
                // frame.
                TMS52xx[] tms52xxs = new TMS52xx[frameOversampling];
                Arrays.setAll(tms52xxs, i -> new SimplifiedTMS52xx(quantization, false, true));

                int lpcDelay = 0;

                frameCounter = 0;

                // Read frames from the WAV file.
                while (audioFrameInputStream.readFrame(wavSamples) > 0)
                {
                    TMS52xx tms52xx = tms52xxs[frameCounter % frameOversampling];

                    // Optimize the corresponding frame.
                    ParameterizedFrame parameterizedFrame =
                        parameterizedFrames[frameCounter];

                    int nextFrameIndex =
                        Math.min(frameCounter+1, parameterizedFrames.length-1);

                    LpcFrame nextFrame =
                        parameterizedFrames[nextFrameIndex].getFrame();

                    fft.scale(wavSamples, amplification);

                    // Apply a window and preemphasize the high frequencies.
                    filterSamples(fft,
                                  wavSamples,
                                  optimizationWindowSize);

                    computeLogPowerSpectrum(fft,
                                            wavSamples,
                                            samplesImag,
                                            optimizationWindowSize,
                                            wavPower);

                    double error =
                        optimizeFrame(wavPower,
                                      parameterizedFrame,
                                      nextFrame,
                                      tms52xx,
                                      lpcBuffer,
                                      lpcDelay,
                                      lpcSamples,
                                      samplesImag,
                                      fft,
                                      lpcPower);

                    // Retrieve the frame, which is now optimized.
                    LpcFrame frame = parameterizedFrame.getFrame();

                    frames[frameCounter] = frame;

                    // Apply the frame to the speech synthesizer.
                    tms52xx.play(frame, lpcBuffer);

                    frameCounter++;
                }
            }
        }
        else
        {
            // Just copy the unoptimized frames.
            for (int index = 0; index < frames.length; index++)
            {
                frames[index] = parameterizedFrames[index].getFrame();
            }
        }

        int skipCount = (lpcWindowSize - optimizationWindowSize) / 2;

        if (frameOversampling > 1)
        {
            // Pick the best frames from the set of oversampled alternatives,
            // based on an actual TMS52xx with its quirky interpolation.
            // The window size is the (smaller) optimization window size.
            // The step size is the oversampling step size.
            // The windows are centered on the corresponding initial windows.
            // +-------+       (initial)
            //   +-------+
            //     +-------+
            //       +-------+
            //   +===+         (optimization)
            //     +===+
            //       +===+
            //         +===+
            try (AudioFrameInputStream audioFrameInputStream =
                     new AudioFrameInputStream(
                     AudioSystem.getAudioInputStream(
                     new BufferedInputStream(
                     new FileInputStream(inputWavFileName))),
                         optimizationWindowSizePow2,
                         stepSize,
                         skipCount))
            {
                FFT fft = new FFT();

                double[] wavSamples  = new double[optimizationWindowSizePow2];
                double[] lpcSamples  = new double[optimizationWindowSizePow2];
                double[] samplesImag = new double[optimizationWindowSizePow2];
                double[] wavPower    = new double[optimizationWindowSizePow2 / 2 + 1];
                double[] lpcPower    = new double[optimizationWindowSizePow2 / 2 + 1];

                TMS52xx tms52xx = new TMS52xx(quantization, false, true);

                int lpcDelay = 150;

                frameCounter = 0;

                double minError = Double.MAX_VALUE;

                LpcFrame bestFrame = null;

                // Read frames from the WAV file.
                while (audioFrameInputStream.readFrame(wavSamples) > 0)
                {
                    int nextFrameIndex =
                        Math.min(frameCounter%2*2+1, frames.length-1);

                    LpcFrame frame     = frames[frameCounter];
                    LpcFrame nextFrame = frames[nextFrameIndex];

                    fft.scale(wavSamples, amplification);

                    // Apply a window and preemphasize the high frequencies.
                    filterSamples(fft,
                                  wavSamples,
                                  optimizationWindowSize);

                    computeLogPowerSpectrum(fft,
                                            wavSamples,
                                            samplesImag,
                                            optimizationWindowSize,
                                            wavPower);

                    double error = error(wavPower,
                                         frame,
                                         nextFrame,
                                         tms52xx,
                                         lpcBuffer,
                                         lpcDelay,
                                         fft,
                                         lpcSamples,
                                         samplesImag,
                                         optimizationWindowSize,
                                         lpcPower);

                    // Are we at the first oversampled frame?
                    // Or did we find a smaller error?
                    if (frameCounter % frameOversampling == 0 ||
                        error < minError)
                    {
                        minError  = error;
                        bestFrame = frame;
                    }

                    // Are we at the last oversampled frame?
                    if (frameCounter % frameOversampling == frameOversampling - 1)
                    {
                        // Store the best frame.
                        frames[frameCounter / frameOversampling] = bestFrame;

                        // Apply the frame to the speech synthesizer.
                        tms52xx.play(frames[frameCounter / frameOversampling], lpcBuffer);
                    }

                    frameCounter++;
                }
            }
        }

        // Optimize the energies of the final frames,
        // based on an actual TMS52xx with its quirky interpolation.
        // The window size is the (smaller) optimization window size.
        // The step size is the standard frame size (200 samples).
        // The windows are centered on the corresponding initial windows.
        // +-------+       (initial)
        //   +===+         (energy)
        try (AudioFrameInputStream audioFrameInputStream =
                 new AudioFrameInputStream(
                 AudioSystem.getAudioInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputWavFileName))),
                     optimizationWindowSize,
                     STEP_SIZE,
                     skipCount))
        {
            FFT fft = new FFT();

            double[] wavSamples  = new double[optimizationWindowSize];
            double[] lpcSamples  = new double[optimizationWindowSize];
            //double[] wavSamplesCopy = new double[wavSamples.length];

            TMS52xx tms52xx = new TMS52xx(quantization, false, true);

            // Write the resulting audio to a WAV file.
            //final PipedOutputStream audioOutputStream =
            //    new PipedOutputStream();
            //
            //final PipedInputStream pipedInputStream =
            //    new PipedInputStream(audioOutputStream);
            //
            //final int frameCount = frameCounter;
            //Runnable writeWav = () ->
            //{
            //    try (AudioInputStream audioInputStream =
            //             new AudioInputStream(
            //             pipedInputStream,
            //             new AudioFormat(8000f, 16, 2, true, false),
            //             frameCount * TMS52xx.FRAME_SIZE))
            //    {
            //        AudioSystem.write(audioInputStream,
            //                          AudioFileFormat.Type.WAVE,
            //                          new File("/tmp/t.wav"));
            //    }
            //    catch (IOException e)
            //    {
            //        e.printStackTrace();
            //    }
            //};
            //new Thread(writeWav).start();

            int lpcDelay = 100;

            frameCounter = 0;

            // Read frames from the WAV file.
            while (audioFrameInputStream.readFrame(wavSamples) > 0)
            {
                fft.scale(wavSamples, amplification);

                // We don't want preemphasis at this point.
                //filterSamples(fft,
                //              lpcSamples,
                //              lpcWindowSize);

                //System.arraycopy(wavSamples, 0,
                //                 wavSamplesCopy, 0,
                //                 wavSamples.length);

                // Apply a window.
                fft.applyHammingWindow(lpcSamples, optimizationWindowSize);

                // Compute the energy of the raw sound frame.
                double wavEnergy = fft.rms(wavSamples, 0, optimizationWindowSize);

                LpcFrame frame = frames[frameCounter];

                // Set the best matching frame energy (simple RMS of the samples).
                optimizeEnergy(wavEnergy,
                               (LpcEnergyFrame)frame,
                               fft,
                               tms52xx,
                               lpcBuffer,
                               lpcDelay,
                               lpcSamples,
                               optimizationWindowSize);

                // Apply the frame to the speech synthesizer.
                tms52xx.play(frame, lpcBuffer);

                //for (int index = 0; index < TMS52xx.FRAME_SIZE; index++)
                //{
                //    short wav = (short)(wavSamplesCopy[index] * Short.MAX_VALUE);
                //    audioOutputStream.write(wav);
                //    audioOutputStream.write(wav >>> 8);
                //
                //    short lpc = lpcBuffer[index];
                //    audioOutputStream.write(lpc);
                //    audioOutputStream.write(lpc >>> 8);
                //}

                frameCounter++;
            }

            //audioOutputStream.close();
        }

        // Fix the energy of transitions from unvoiced to voiced frames.
        if (fixEnergyTransitions)
        {
            fixEnergyTransitions(frames, frameCounter);
        }

        // Fix the energy of frames with clamped samples.
        if (fixClampedSamples)
        {
            fixClampedSamples(frames, frameCounter);
        }

        // Replace quiet voiced and unvoiced frames by silent frames.
        fixSilences(frames, frameCounter);

        // Write the output.
        try (LpcFrameOutput lpcFrameOutput =
                 new RepeatingLpcFrameOutput(
                 new LpcFrameOutputStream(
                 new BufferedOutputStream(
                 new FileOutputStream(outputLpcFileName)))))
        {
            int startIndex = 0;
            int endIndex   = frameCounter;

            // Adjust the start index and end index to trim silence frames.
            if (trimSilenceFrames)
            {
                while (startIndex < endIndex &&
                       frames[startIndex] instanceof LpcSilenceFrame)
                {
                    startIndex++;
                }

                while (endIndex > startIndex &&
                       frames[endIndex-1] instanceof LpcSilenceFrame)
                {
                    endIndex--;
                }
            }

            for (int index = startIndex; index < endIndex; index++)
            {
                // Write out the frame.
                lpcFrameOutput.writeFrame(frames[index]);
            }

            if (addStopFrame)
            {
                lpcFrameOutput.writeFrame(new LpcStopFrame());
            }
        }
    }


    /**
     * Adds a given int value to a given array, extending it if necessary.
     */
    private int[] addValue(int[] values, int count, int value)
    {
        if (count >= values.length)
        {
            int[] newValues = new int[values.length * 2];
            System.arraycopy(values, 0,
                             newValues, 0,
                             values.length);
            values = newValues;
        }

        values[count] = value;

        return values;
    }


    /**
     * Adds a given double value to a given array, extending it if necessary.
     */
    private double[] addValue(double[] values, int count, double value)
    {
        if (count >= values.length)
        {
            double[] newValues = new double[values.length * 2];
            System.arraycopy(values, 0,
                             newValues, 0,
                             values.length);
            values = newValues;
        }

        values[count] = value;

        return values;
    }


    /**
     * Sets the frame energy that best matches the given target energy.
     */
    private void optimizeEnergy(double         targetEnergy,
                                LpcEnergyFrame frame,
                                FFT            fft,
                                TMS52xx        tms52xx,
                                short[]        lpcBuffer,
                                int            lpcDelay,
                                double[]       lpcSamples,
                                int            windowSize)
    {
        double previousLpcEnergy = 0.0;

        // Try all possible encoded frame energies.
        for (int energy = quantization.minEncodedEnergy();
             energy <= quantization.maxEncodedEnergy();
             energy++)
        {
            frame.energy = energy;

            computeLpcSamples(frame,
                              frame,
                              tms52xx.clone(),
                              lpcBuffer,
                              lpcDelay,
                              lpcSamples);

            // We don't want preemphasis at this point.
            //filterSamples(fft,
            //              lpcSamples,
            //              lpcWindowSize);

            // Apply a window.
            fft.applyHammingWindow(lpcSamples, windowSize);

            // Compute the energy of the raw LPC frame.
            double lpcEnergy = fft.rms(lpcSamples, 0, windowSize);

            // Is the frame energy larger than the target energy?
            if (lpcEnergy > targetEnergy)
            {
                // Pick the closest frame energy of the previous one and
                // the current one.
                if (targetEnergy - previousLpcEnergy < lpcEnergy - targetEnergy)
                {
                    frame.energy--;
                }

                // We're done.
                return;
            }

            previousLpcEnergy = lpcEnergy;
        }

        // Even the largest frame energy is smaller than the target energy.
        // We'll leave it at the maximum.
    }


    /**
     * Sets the frame parameters that best match the given target power
     * spectrum.
     */
    private double optimizeFrame(double[]           wavPower,
                                 ParameterizedFrame parameterizedFrame,
                                 LpcFrame           nextFrame,
                                 TMS52xx            tms52xx,
                                 short[]            lpcBuffer,
                                 int                lpcDelay,
                                 double[]           lpcSamples,
                                 double[]           samplesImag,
                                 FFT                fft,
                                 double[]           lpcPower)
    {
        LpcFrame frame = parameterizedFrame.getFrame();

        // Remember the smallest error and the
        // corresponding parameter value.
        double minError = error(wavPower,
                                frame,
                                nextFrame,
                                tms52xx,
                                lpcBuffer,
                                lpcDelay,
                                fft,
                                lpcSamples,
                                samplesImag,
                                optimizationWindowSize,
                                lpcPower);

        if (DEBUG)
        {
            System.out.printf("#              %s -> %s: err=%f%n",
                              frame.toString(),
                              nextFrame.toString(),
                              minError);
        }

        // Iterate until its parameters no longer change.
        for (int frameIteration = 0; frameIteration < 10; frameIteration++)
        {
            boolean frameChanged = false;

            // Optimize all subsequent parameters once.
            for (Parameter parameter : parameterizedFrame.getParameters())
            {
                int bestValue = parameter.getValue();

                boolean parameterChanged = false;

                // Try smaller values.
                for (int value = bestValue - 1;
                     value >= parameter.minValue();
                     value--)
                {
                    parameter.setValue(value);

                    // Remember the smallest error and the
                    // corresponding parameter value.
                    double error = error(wavPower,
                                         frame,
                                         nextFrame,
                                         tms52xx,
                                         lpcBuffer,
                                         lpcDelay,
                                         fft,
                                         lpcSamples,
                                         samplesImag,
                                         optimizationWindowSize,
                                         lpcPower);

                    if (error > minError)
                    {
                        break;
                    }

                    bestValue        = value;
                    minError         = error;
                    parameterChanged = true;
                    frameChanged     = true;

                    if (DEBUG)
                    {
                        System.out.printf("#              [%d] %s err=%f down!!!%n",
                                          frameIteration,
                                          parameter.toString(),
                                          error);
                    }
                }

                // Haven't we found a better smaller value?
                if (!parameterChanged)
                {
                    // Try larger values.
                    for (int value = bestValue + 1;
                         value <= parameter.maxValue();
                         value++)
                    {
                        parameter.setValue(value);

                        // Remember the smallest error and the
                        // corresponding parameter value.
                        double error = error(wavPower,
                                             frame,
                                             nextFrame,
                                             tms52xx,
                                             lpcBuffer,
                                             lpcDelay,
                                             fft,
                                             lpcSamples,
                                             samplesImag,
                                             optimizationWindowSize,
                                             lpcPower);

                        if (error > minError)
                        {
                            break;
                        }

                        bestValue        = value;
                        minError         = error;
                        parameterChanged = true;
                        frameChanged     = true;

                        if (DEBUG)
                        {
                            System.out.printf("#              [%d] %s err=%f up!!!%n",
                                              frameIteration,
                                              parameter.toString(),
                                              error);
                        }
                    }
                }

                parameter.setValue(bestValue);
            }

            // Stop iterating if the parameters no longer change anyway.
            if (!frameChanged)
            {
                break;
            }
        }

        return minError;
    }


    /**
     * Fixes any outliers of the given pitches and autocorrelations.
     */
    private void fixPitchOutliers(int[]    pitches,
                                  int      count)
    {
        int[] pitchesCopy = pitches.clone();

        for (int index = 0; index < count; index++)
        {
            int windowDelta = OUTLIER_WINDOW_FRAME_COUNT * frameOversampling / 2;

            int startIndex = Math.max(0,     index - windowDelta);
            int endIndex   = Math.min(count, index + windowDelta + 1);

            // Compute the average pitch around this frame.
            int averagePitch = 0;
            for (int sumIndex = startIndex; sumIndex < endIndex; sumIndex++)
            {
                averagePitch += pitchesCopy[sumIndex];
            }
            averagePitch /= endIndex - startIndex;

            // Is this pitch an outlier?
            int pitch = pitchesCopy[index];
            if (relativeDifference(pitch, averagePitch) > PITCH_OUTLIER_THRESHOLD)
            {
                if (DEBUG)
                {
                    System.err.print("#"+index+" Fixing pitch "+pitch);
                }

                // Fix it.
                if (relativeDifference(2 * pitch, averagePitch) <= PITCH_OUTLIER_THRESHOLD)
                {
                    // Double the pitch seems a better match.
                    pitches[index] = 2 * pitch;
                }
                else if (relativeDifference(pitch / 2, averagePitch) <= PITCH_OUTLIER_THRESHOLD)
                {
                    // Half the pitch seems a better match.
                    pitches[index] = pitch / 2;
                }
                else
                {
                    // We'll just use the average.
                    pitches[index] = averagePitch;
                }

                if (DEBUG)
                {
                    System.err.println(" to " + pitches[index]);
                }
            }
        }

        if (DEBUG)
        {
            System.err.println();
            System.err.println();
        }
    }


    /**
     * Fixes any rapid back and forth jitter between voiced and unvoiced of
     * the given autocorrelations.
     */
    private void fixVoicedJittering(double[] maxCorrelations,
                                    int      count)
    {
        FFT fft = new FFT();

        int windowDelta   = OUTLIER_WINDOW_FRAME_COUNT * frameOversampling / 2;
        int minVoiceCount = MIN_VOICE_FRAME_COUNT * frameOversampling;

        boolean iterate;
        do
        {
            if (DEBUG)
            {
                System.err.println("# Iterating for max autocorrelation");
            }

            iterate = false;

            double[] maxCorrelationsCopy = maxCorrelations.clone();

            for (int index = 0; index < count; index++)
            {
                // Is the frame voiced?
                boolean voiced = maxCorrelationsCopy[index] > voicedThreshold;

                // Count the surrounding similar frames.
                int voiceCount = 1;
                for (int leftIndex = index - 1;
                     leftIndex >= 0             &&
                     voiceCount < minVoiceCount &&
                     ((maxCorrelationsCopy[leftIndex] > voicedThreshold) == voiced);
                     leftIndex--)
                {
                    voiceCount++;
                }
                for (int rightIndex = index + 1;
                     rightIndex < count         &&
                     voiceCount < minVoiceCount &&
                     ((maxCorrelationsCopy[rightIndex] > voicedThreshold) == voiced);
                     rightIndex++)
                {
                    voiceCount++;
                }

                if (DEBUG)
                {
                    System.err.println("#" + index + " Voiced("+voiced+") frames = " + voiceCount);
                }

                // Do we have sufficiently long sequence of similar frames?
                if (voiceCount < minVoiceCount)
                {
                    // Fix the maximum autocorrelation by taking the average
                    // in a window (which is equivalent to linear
                    // interpolation).
                    int startIndex = Math.max(0,     index - windowDelta);
                    int endIndex   = Math.min(count, index + windowDelta + 1);

                    maxCorrelations[index] = fft.average(maxCorrelationsCopy,
                                                         startIndex,
                                                         endIndex);

                    if (DEBUG)
                    {
                        System.err.println("#" + index + " Fixing max autocorrelation from " + maxCorrelationsCopy[index] + " to " + maxCorrelations[index]);
                    }

                   iterate = true;
                }
            }

            if (DEBUG)
            {
                System.err.println();
            }
        }
        while (iterate);

        if (DEBUG)
        {
            System.err.println();
        }
    }


    /**
     * Fixes the energies for transitions from unvoiced frames to voiced frames.
     * Although the TMS52xx does not interpolate between the frames in such
     * cases, it sets the voiced energy a few samples before it switches the
     * excitation from unvoiced to voiced. This can cause a loud click if the
     * voiced energy is high.
     */
    private void fixEnergyTransitions(LpcFrame[] frames, int count)
    {
        for (int index = 1; index < count; index++)
        {
            LpcEnergyFrame previousFrame = (LpcEnergyFrame)frames[index - 1];
            LpcEnergyFrame frame         = (LpcEnergyFrame)frames[index];
            if (previousFrame instanceof LpcUnvoicedFrame &&
                frame         instanceof LpcVoicedFrame)
            {
                int previousEnergy = previousFrame.energy;
                int energy         = frame.energy;

                // We're averaging the energies if the voiced energy is
                // higher.
                if (energy > previousEnergy)
                {
                    frame.energy = (previousEnergy + energy) / 2;

                    if (DEBUG)
                    {
                        System.err.println("#" + index + " Fixing unvoiced to voiced energy ("+energy+" -> "+frame.energy+")");
                    }
                }
            }
        }
    }


    /**
     * Fixes the energies of frames for which the TMS52xx produces clamped
     * samples. Clamped samples generally produce unwanted clicks.
     */
    private void fixClampedSamples(LpcFrame[] frames, int count)
    {
        short[] lpcSamples0  = new short[TMS52xx.FRAME_SIZE];
        short[] lpcSamples1  = new short[TMS52xx.FRAME_SIZE];

        TMS52xx tms52xx = new TMS52xx(quantization, false, true);

        for (int index = 0; index < count-1; index++)
        {
            LpcEnergyFrame frame0 = (LpcEnergyFrame)frames[index];
            LpcFrame       frame1 =                 frames[index+1];

            // Correct the first frame as much as necessary.
            while (frame0.energy > 0)
            {
                // Play the next two frames.
                TMS52xx tms52xxClone = tms52xx.clone();
                tms52xxClone.play(frame0, lpcSamples0);
                tms52xxClone.play(frame1, lpcSamples1);

                // Check the resulting samples.
                // We're checking the second half of the first samples,
                // and the first half of the second samples.
                if (!isClamped(lpcSamples0, TMS52xx.FRAME_SIZE/2, TMS52xx.FRAME_SIZE) &&
                    !isClamped(lpcSamples1, 0,                    TMS52xx.FRAME_SIZE/2))
                {
                    break;
                }

                frame0.energy--;

                if (DEBUG)
                {
                    System.err.println("#" + index + " Fixing clamped sample (energy = "+frame0.energy+")");
                }
            }

            // Play the possibly corrected frame.
            tms52xx.play(frame0);
        }
    }


    /**
     * Returns whether any of the given TMS52xx samples was clamped.
     */
    private boolean isClamped(short[] lpcSamples,
                              int     startIndex,
                              int     endIndex)
    {
        for (int index = startIndex; index < endIndex; index++)
        {
            if (isClamped(lpcSamples[index]))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns whether the given TMS52xx sample was clamped.
     */
    private static boolean isClamped(short lpcSample)
    {
        return lpcSample == 0x00007ff0 ||
               lpcSample == 0xffff8000;
    }


    /**
     * Replaces any quiet voiced and unvoiced frames by silent frames.
     */
    private void fixSilences(LpcFrame[] frames, int count)
    {
        LpcFrame[] framesCopy = new LpcFrame[count];
        System.arraycopy(frames, 0,
                         framesCopy, 0,
                         count);

        for (int index = 0; index < count; index++)
        {

            if ( ((LpcEnergyFrame)framesCopy[                    index   ]).energy == 0 ||
                (((LpcEnergyFrame)framesCopy[Math.max(0,         index-1)]).energy <= 1 &&
                 ((LpcEnergyFrame)framesCopy[                    index   ]).energy <= 1 &&
                 ((LpcEnergyFrame)framesCopy[Math.min(count - 1, index+1)]).energy <= 1))
            {
                frames[index] = new LpcSilenceFrame();

                if (DEBUG)
                {
                    System.err.println("#" + index + " Fixing silence");
                }
            }
        }
    }


    private double relativeDifference(double x, double y)
    {
        return Math.abs(x - y) / y;
    }


    private void printFrameData(int      iteration,
                                int      frameCounter,
                                FFT      fft,
                                double[] wavSamples,
                                double[] wavPower,
                                LpcFrame frame,
                                LpcFrame nextFrame,
                                TMS52xx  tms52xx,
                                short[]  lpcBuffer,
                                int      lpcDelay,
                                double[] lpcSamples,
                                double[] samplesImag,
                                int      windowSize,
                                double[] lpcPower,
                                double   minError)
    {
        System.out.printf("# Iter #%d, frame #%d (%.3f) %s -> %s err=%.4f%n",
                          iteration,
                          frameCounter,
                          frameCounter * TMS52xx.FRAME_SIZE / TMS52xx.SOUND_FREQUENCY,
                          frame.toString(),
                          nextFrame == null ? "same" : nextFrame.toString(),
                          minError);
        System.out.printf("# %d:%d%n",
                          iteration,
                          frameCounter);

        double[] lpcSamples0 = new double[lpcSamples.length];
        double[] lpcFft0     = new double[lpcSamples.length];
        double[] lpcPower0   = new double[lpcPower.length];

        computeLpcSamples(frame,
                          nextFrame == null ? frame : nextFrame,
                          tms52xx.clone(),
                          lpcBuffer,
                          lpcDelay,
                          lpcSamples0);

        System.arraycopy(lpcSamples0, 0,
                         lpcFft0, 0,
                         lpcSamples.length);

        filterSamples(fft,
                      lpcFft0,
                      windowSize);

        computeLogPowerSpectrum(fft,
                                lpcFft0,
                                samplesImag,
                                windowSize,
                                lpcPower0);

        for (int index = 0; index < wavSamples.length; index++)
        {
            System.out.print(wavSamples[index] + " " + lpcSamples0[index]);
            if (index < wavPower.length)
                System.out.print(" " + wavPower[index] + " " + lpcPower0[index]);
            else
                System.out.println();
        }
        System.out.println();
        System.out.println();
    }


    private double error(double[] wavPower,
                         LpcFrame lpcFrame1,
                         LpcFrame lpcFrame2,
                         TMS52xx  tms52xx,
                         short[]  lpcBuffer,
                         int      lpcDelay,
                         FFT      fft,
                         double[] lpcSamples,
                         double[] samplesImag,
                         int      windowSize,
                         double[] lpcPower)
    {
        computeLpcSamples(lpcFrame1,
                          lpcFrame2,
                          tms52xx.clone(),
                          lpcBuffer,
                          lpcDelay,
                          lpcSamples);

        // Apply a window and preemphasize the high frequencies.
        filterSamples(fft,
                      lpcSamples,
                      windowSize);

        computeLogPowerSpectrum(fft,
                                lpcSamples,
                                samplesImag,
                                windowSize,
                                lpcPower);

        // TODO: Include DC component?
        return fft.difference(wavPower, lpcPower, 1, wavPower.length);
    }


    private void extractSamples(byte[]   buffer,
                                int      bufferOffset,
                                boolean  isBigEndian,
                                double[] samples)
    {
        int msbOffset = isBigEndian ? 0 : 1;
        int lsbOffset = 1 - msbOffset;

        // Convert the signed 16-bits samples to doubles.
        for (int index = 0; index < samples.length; index++)
        {
            // Compute the offsets in the rolling byte buffer.
            int offset = (bufferOffset + index * 2) % buffer.length;

            samples[index] =
                (short)((buffer[offset + msbOffset] << 8) |
                        (buffer[offset + lsbOffset] &  0xff)) /
                (double)Short.MAX_VALUE;
        }
    }


    private void extractSamples(short[]  buffer,
                                double[] samples)
    {
        // Convert the signed 16-bits samples to doubles.
        for (int index = 0; index < samples.length; index++)
        {
            samples[index] = (double)buffer[index] /
                             (double)Short.MAX_VALUE;
        }
    }


    private void computeLpcSamples(LpcFrame lpcFrame1,
                                   LpcFrame lpcFrame2,
                                   TMS52xx  tms52xx,
                                   short[]  lpcBuffer,
                                   int      lpcDelay,
                                   double[] windowSamples)
    {
        // Fill the buffer with the samples of the first frame.
        tms52xx.play(lpcFrame1, lpcBuffer);

        for (int windowIndex = 0; windowIndex < windowSamples.length; windowIndex++)
        {
            // Skip ahead the specified number of samples in the buffer.
            int bufferIndex = (windowIndex + lpcDelay) % lpcBuffer.length;

            // Fill the buffer with the samples of the next frame if necessary.
            if (windowIndex > 0 && bufferIndex == 0)
            {
                tms52xx.play(lpcFrame2, lpcBuffer);
            }

            // Convert the short from the buffer to a double in the window.
            windowSamples[windowIndex] = (double)lpcBuffer[bufferIndex] /
                                         (double)Short.MAX_VALUE;
        }
    }


    private void filterSamples(FFT      fft,
                               double[] samples,
                               int      windowSize)
    {
        // Emphasize high frequencies.
        fft.applyPreemphasis(samples, preemphasis);

        // Apply the window filter.
        //fft.applyGaussianWindow(samples, windowSize);
        fft.applyHammingWindow(samples, windowSize);
        //fft.applyBlackmanWindow(samples, windowSize);
    }


    private void computeLogPowerSpectrum(FFT      fft,
                                         double[] samples,
                                         double[] samplesImag,
                                         int      windowSize,
                                         double[] power)
    {
        // Clear the imaginary components.
        Arrays.fill(samplesImag, 0.0);

        // Go to the frequency domain.
        fft.computeFFT(samples, samplesImag);

        // Compute the log power spectrum in the frequency domain.
        fft.computePowerSpectrum(samples, samplesImag, power);
        fft.shift(power, linearPowerShift);
        fft.computeLog(power);

        // Smoothen the power spectrum.
        fft.applyGaussianFilter(power,
                                (int)Math.round(0.02 * power.length),
                                1,
                                power.length);

        // Applying cepstral filtering smoothens the spectrum too, but it
        // doesn't seem to improve the results. It erases part of the power,
        // which introduces problems when matching spectra.

        //// Go to the cepstral domain.
        //// The signal is real there, because taking the power spectrum
        //// has removed all phase information (set the phases to 0).
        //Arrays.fill(samplesImag, 0.0);
        //fft.computeInverseFFT(samples, samplesImag);
        //
        //// Apply a low pass filter in the cepstral domain,
        //// which smoothens the spectrum in the frequency domain.
        //// https://ccrma.stanford.edu/~jos/SpecEnv/Cepstral_Smoothing.html
        //// https://ccrma.stanford.edu/~jos/sasp/Cepstral_Windowing.html
        //fft.applyLowPassCepstralWindow(samples,     cepstralFilterPitch);
        //
        //// Go to the frequency domain again.
        //// The spectrum is real now, because the cepstrum doesn't have
        //// any phase information (all phases are 0).
        //fft.computeFFT(samples, samplesImag);
        //
        //System.arraycopy(samples, 0,
        //                 melPower, 0,
        //                 melPower.length);
        //
        //fft.computePowerSpectrum(samples, samplesImag, power);

        // Going to the Mel domain doesn't seem to improve the results.

        //fft.convertPowerSpectrumToMel(power, 4000.0, melPower);
        //fft.shift(melPower, linearPowerShift);
        //fft.computeLog(melPower);
    }


    private interface ParameterizedFrame
    extends           Cloneable
    {
        public LpcFrame    getFrame();
        public Parameter[] getParameters();

        public ParameterizedFrame clone();
    }


    private class ParameterizedFixedFrame
    implements    ParameterizedFrame
    {
        private final LpcFrame    frame;


        public ParameterizedFixedFrame(LpcFrame frame)
        {
            this.frame = frame;
        }


        // Implementations for Parameter.

        public LpcFrame getFrame()
        {
            return frame;
        }

        public Parameter[] getParameters()
        {
            return new Parameter[0];
        }


        // Implementation for Cloneable.

        public ParameterizedFixedFrame clone()
        {
            return new ParameterizedFixedFrame(frame.clone());
        }
    }




    private class ParameterizedEnergyFrame
    implements    ParameterizedFrame
    {
        private final LpcEnergyFrame energyFrame;


        public ParameterizedEnergyFrame(LpcEnergyFrame energyFrame)
        {
            this.energyFrame = energyFrame;
        }


        // Implementations for ParameterizedFrame.

        public LpcFrame getFrame()
        {
            return energyFrame;
        }

        public Parameter[] getParameters()
        {
            // Optimize the energy parameter again at the end, because the
            // reflection coefficients affect the energy of the frame.
            return new Parameter[]
            {
                new EnergyParameter(energyFrame),
            };
        }


        // Implementation for Cloneable.

        public ParameterizedEnergyFrame clone()
        {
            return new ParameterizedEnergyFrame(energyFrame.clone());
        }
    }


    private class ParameterizedUnvoicedFrame
    implements    ParameterizedFrame
    {
        private final LpcUnvoicedFrame unvoicedFrame;


        public ParameterizedUnvoicedFrame(LpcUnvoicedFrame unvoicedFrame)
        {
            this.unvoicedFrame = unvoicedFrame;
        }


        // Implementations for ParameterizedFrame.

        public LpcFrame getFrame()
        {
            return unvoicedFrame;
        }

        public Parameter[] getParameters()
        {
            // Optimize the energy parameter again at the end, because the
            // reflection coefficients affect the energy of the frame.
            return new Parameter[]
            {
                new EnergyParameter(unvoicedFrame),
                new UnvoicedCoefficientParameter(unvoicedFrame, 0),
                new UnvoicedCoefficientParameter(unvoicedFrame, 1),
                new UnvoicedCoefficientParameter(unvoicedFrame, 2),
                new UnvoicedCoefficientParameter(unvoicedFrame, 3),
                new EnergyParameter(unvoicedFrame),
            };
        }


        // Implementation for Cloneable.

        public ParameterizedUnvoicedFrame clone()
        {
            return new ParameterizedUnvoicedFrame(unvoicedFrame.clone());
        }
    }


    private class ParameterizedVoicedFrame
    implements    ParameterizedFrame
    {
        private final LpcVoicedFrame voicedFrame;


        public ParameterizedVoicedFrame(LpcVoicedFrame voicedFrame)
        {
            this.voicedFrame = voicedFrame;
        }


        // Implementations for ParameterizedFrame.

        public LpcFrame getFrame()
        {
            return voicedFrame;
        }

        public Parameter[] getParameters()
        {
            // Don't optimize the pitch, because especially low frequencies
            // are poorly defined over short time intervals.
            // Optimize the energy parameter again at the end, because the
            // reflection coefficients affect the energy of the frame.
            return new Parameter[]
            {
                new EnergyParameter(voicedFrame),
                //new PitchParameter(voicedFrame),
                new VoicedCoefficientParameter(voicedFrame, 0),
                new VoicedCoefficientParameter(voicedFrame, 1),
                new VoicedCoefficientParameter(voicedFrame, 2),
                new VoicedCoefficientParameter(voicedFrame, 3),
                new VoicedCoefficientParameter(voicedFrame, 4),
                new VoicedCoefficientParameter(voicedFrame, 5),
                new VoicedCoefficientParameter(voicedFrame, 6),
                new VoicedCoefficientParameter(voicedFrame, 7),
                new VoicedCoefficientParameter(voicedFrame, 8),
                new VoicedCoefficientParameter(voicedFrame, 9),
                new EnergyParameter(voicedFrame),
            };
        }


        // Implementation for Cloneable.

        public ParameterizedVoicedFrame clone()
        {
            return new ParameterizedVoicedFrame(voicedFrame.clone());
        }
    }


    private interface Parameter
    {
        public int  minValue();
        public int  maxValue();
        public int  getValue();
        public void setValue(int value);
    }


    /**
     * This Parameter allows to set the energy of a given voiced LPC frame.
     */
    private class FixedParameter
    implements    Parameter
    {
        public FixedParameter()
        {
        }

        // Implementations for Parameter.

        public int minValue()
        {
            return 0;
        }

        public int maxValue()
        {
            return 0;
        }

        public int getValue()
        {
            return 0;
        }

        public void setValue(int value)
        {
        }

        // Implementations for Object.

        public String toString()
        {
            return "fixed";
        }
    }


    /**
     * This Parameter allows to set the energy of a given voiced LPC frame.
     */
    private class EnergyParameter
    implements    Parameter
    {
        private final LpcEnergyFrame energyFrame;
        private final int            maxValue;


        public EnergyParameter(LpcEnergyFrame energyFrame)
        {
            this.energyFrame = energyFrame;
            this.maxValue    = (1 << quantization.energyBitCount) - 2;
        }

        // Implementations for Parameter.

        public int minValue()
        {
            // We allow an energy of 0, as long as such a frame is replaced by
            // a silent frame later on.
            return 0;
        }

        public int maxValue()
        {
            return maxValue;
        }

        public int getValue()
        {
            return energyFrame.energy;
        }

        public void setValue(int value)
        {
            energyFrame.energy = value;
        }

        // Implementations for Object.

        public String toString()
        {
            return energyFrame.toString() + ",energy=0x"+Integer.toHexString(getValue());
        }
    }


    /**
     * This Parameter allows to set the pitch of a given voiced LPC frame.
     */
    private class PitchParameter
    implements    Parameter
    {
        private final LpcPitchFrame pitchFrame;
        private final int           minValue;
        private final int           maxValue;

        public PitchParameter(LpcPitchFrame pitchFrame)
        {
            this.pitchFrame = pitchFrame;
            this.minValue   = quantization.encodePitch(minPitch);
            this.maxValue   = quantization.encodePitch(maxPitch);
        }

        // Implementations for Parameter.

        public int minValue()
        {
            return minValue;
        }

        public int maxValue()
        {
            return maxValue;
        }

        public int getValue()
        {
            return pitchFrame.pitch;
        }

        public void setValue(int value)
        {
            pitchFrame.pitch = value;
        }

        // Implementations for Object.

        public String toString()
        {
            return pitchFrame.toString() + ",pitch=0x"+Integer.toHexString(getValue());
        }
    }


    /**
     * This Parameter allows to set a specified LPC coefficient of a given
     * unvoiced LPC frame.
     */
    private class UnvoicedCoefficientParameter
    implements    Parameter
    {
        private final LpcUnvoicedFrame unvoicedFrame;
        private final int              coefficientIndex;
        private final int              maxValue;
        private final int              shift;


        public UnvoicedCoefficientParameter(LpcUnvoicedFrame  unvoicedFrame,
                                            int               coefficientIndex)
        {
            this.unvoicedFrame    = unvoicedFrame;
            this.coefficientIndex = coefficientIndex;
            this.maxValue         = (1 << quantization.lpcCoefficientBitCounts[coefficientIndex]) - 1;

            int shift = 0;
            for (int index = quantization.unvoicedLpcCoefficientCount-1;
                 index > coefficientIndex;
                 index--)
            {
                shift += quantization.lpcCoefficientBitCounts[index];
            }

            this.shift = shift;
        }

        // Implementations for Parameter.

        public int minValue()
        {
            return 0;
        }

        public int maxValue()
        {
            return maxValue;
        }

        public int getValue()
        {
            return (int)(unvoicedFrame.k >>> shift) & maxValue;
        }

        public void setValue(int value)
        {
            unvoicedFrame.k = unvoicedFrame.k
                            & ~((long)maxValue << shift)
                            | ((long)value << shift);
        }

        // Implementations for Object.

        public String toString()
        {
            return unvoicedFrame.toString() + ",k["+coefficientIndex+"]=0x"+Integer.toHexString(getValue());
        }
    }


    /**
     * This Parameter allows to set a specified LPC coefficient of a given
     * voiced LPC frame.
     */
    private class VoicedCoefficientParameter
    implements    Parameter
    {
        private final LpcVoicedFrame voicedFrame;
        private final int            coefficientIndex;
        private final int            maxValue;
        private final int            shift;


        public VoicedCoefficientParameter(LpcVoicedFrame  voicedFrame,
                                          int             coefficientIndex)
        {
            this.voicedFrame      = voicedFrame;
            this.coefficientIndex = coefficientIndex;
            this.maxValue         = (1 << quantization.lpcCoefficientBitCounts[coefficientIndex]) - 1;

            int shift = 0;
            for (int index = quantization.voicedLpcCoefficientCount-1;
                 index > coefficientIndex;
                 index--)
            {
                shift += quantization.lpcCoefficientBitCounts[index];
            }

            this.shift = shift;
        }

        // Implementations for Parameter.

        public int minValue()
        {
            return 0;
        }

        public int maxValue()
        {
            return maxValue;
        }

        public int getValue()
        {
            return (int)(voicedFrame.k >>> shift) & maxValue;
        }

        public void setValue(int value)
        {
            voicedFrame.k = voicedFrame.k
                            & ~((long)maxValue << shift)
                            | ((long)value << shift);
        }

        // Implementations for Object.

        public String toString()
        {
            return voicedFrame.toString() + ",k["+coefficientIndex+"]=0x"+Integer.toHexString(getValue());
        }
    }
}
