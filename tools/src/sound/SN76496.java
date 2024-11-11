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
package sound;

import javax.sound.sampled.*;
import java.io.*;

/**
 * This class simulates an SN76496 Programmable Sound Generator (PSG) chip,
 * or one of its variants. It accepts sound commands and returns the
 * corresponding sound samples (200 samples per frame).
 */
public class SN76496
{
    public static final double NTSC_CLOCK_FREQUENCY = 3579545;
    public static final double PAL_CLOCK_FREQUENCY  = 3546893;

    public static final double NTSC_FRAME_FREQUENCY = 59.922738;
    public static final double PAL_FRAME_FREQUENCY  = 50.158969;

    private static final int   MAX_OUTPUT   = 0x7fff;

    // Volume table (for 4-bit attenuation to db conversion).
	private static final int[] VOLUME_TABLE = new int[16];
    {
        // Four channels, each gets 1/4 of the total range.
        double out = MAX_OUTPUT / 4.0;

        // Build the volume table (2dB per step).
        for (int index = 0; index < VOLUME_TABLE.length; index++)
        {
            VOLUME_TABLE[index] = (int)Math.round(out);

            out /= 1.258925412; // = 10 ^ (2/20) = 2dB
        }
        VOLUME_TABLE[VOLUME_TABLE.length - 1] = 0;
    }


    private final PsgChip psgChip;
    private final int     subsampling;

	private int       rng;                      // Noise generator LFSR.
    private boolean   tunedNoise;
    private boolean   whiteNoise;
    private int[]     periods = new int[4];     // Length of 1/2 of waveform.
    private int[]     counts  = new int[4];     // Position within the waveform.
    private boolean[] outputs = new boolean[4]; // Output of each channel, pre-volume.
    private int[]     volumes = new int[4];     // dB volume of voice 0-2 and noise.


    /**
     * Creates a new instance with a given variant.
     * @param psgChip     the variant of the chip.
     * @param subsampling the number of chip samples (at the chip's clock rate)
     *                    to be averaged per output sample (at a lower clock
     *                    rate).
     */
    public SN76496(PsgChip psgChip,
                   int     subsampling)
    {
        this.psgChip     = psgChip;
        this.subsampling = subsampling;

        resetRNG();

        outputs[SoundCommand.NOISE] = (rng & 1) != 0;
    }


    /**
     * Sends the given sound commands to the processor.
     */
    public void play(SoundCommand[] soundFrame)
    {
        for (SoundCommand soundCommand: soundFrame)
        {
            play(soundCommand);
        }
    }


    /**
     * Sends the given sound command to the processor.
     */
    public void play(SoundCommand soundCommand)
    {
        // update the output buffer before changing the registers
        //psgVariant.m_sound.update();

        int generator = soundCommand.generator;

        if (soundCommand instanceof FrequencyCommand)
        {
            FrequencyCommand frequencyCommand = (FrequencyCommand)soundCommand;

            int divider = frequencyCommand.divider;

            if (generator == SoundCommand.NOISE)
            {
                // The NCR-style PSG resets the LFSR only on a mode write,
                // which actually changes the state of bit 2 of register 6.
                if (!psgChip.ncrStylePsg ||
                    frequencyCommand.isWhiteNoise() != whiteNoise)
                {
                    resetRNG();
                }

                // Update the noise generator types.
                tunedNoise = frequencyCommand.isTunedNoise();
                whiteNoise = frequencyCommand.isWhiteNoise();

                // Update the noise generator whole-period.
                periods[SoundCommand.NOISE] = tunedNoise ?
                    periods[SoundCommand.TONE2] << 1 :
                    1 << 5 + (divider & 3);
            }
            else
            {
                // Update the tone generator half-period.
                periods[generator] =
                    psgChip.segaStylePsg && divider == 0 ? 0x400 :
                        divider;

                // Update the noise generator whole-period (if tuned),
                // to two times tone generator 2's half-period.
                // The resulting frequencies are the same.
                if (generator == SoundCommand.TONE2 && tunedNoise)
                {
                    periods[SoundCommand.NOISE] =
                        periods[SoundCommand.TONE2] << 1;
                }
            }
        }
        else
        {
            VolumeCommand volumeCommand = (VolumeCommand)soundCommand;

            volumes[generator] = VOLUME_TABLE[volumeCommand.attenuation];
        }
    }


    /**
     * Receives sound samples from the processor, storing them in the given
     * array. The frequency of the samples is the clock frequency of the
     * processor (typically the NTSC or PAL frequency) divided by 16 and
     * divided by the sample divider.
     */
    public void listen(short[] samples)
    {
        for (int sampleIndex = 0; sampleIndex < samples.length; sampleIndex++)
        {
            int sample = 0;

            for (int count = 0; count < subsampling; count++)
            {
                // Update the tone generator states and outputs.
                for (int generatorIndex = SoundCommand.TONE0;
                     generatorIndex    <= SoundCommand.TONE2;
                     generatorIndex++)
                {
                    counts[generatorIndex]--;
                    if (counts[generatorIndex] <= 0)
                    {
                        // Toggle the output and reset the counter to the
                        // half-period.
                        outputs[generatorIndex] = !outputs[generatorIndex];
                        counts[generatorIndex]  = periods[generatorIndex];
                    }
                }

                // Update the noise generator state and output.
                counts[SoundCommand.NOISE]--;
                if (counts[SoundCommand.NOISE] <= 0)
                {
                    // The chip only updates the RNG when the count has
                    // reached 0, resulting in a noise period of 15 or 16
                    // times the specified period.
                    updateRNG();

                    // Get the output and reset the counter to the period.
                    outputs[SoundCommand.NOISE] = (rng & 1) != 0;
                    counts[SoundCommand.NOISE]  = periods[SoundCommand.NOISE];
                }

                // Compose the current sample from the generator outputs
                // and volumes.
                for (int generatorIndex = SoundCommand.TONE0;
                     generatorIndex    <= SoundCommand.NOISE;
                     generatorIndex++)
                {
                    if (outputs[generatorIndex])
                    {
                        sample += volumes[generatorIndex];
                    }
                }
            }

            sample /= subsampling;

            if (psgChip.negate)
            {
                sample = -sample;
            }

            samples[sampleIndex] = (short)sample;
        }
    }


    /**
     * Resets the random number generator.
     */
    private void resetRNG()
    {
        rng = psgChip.feedbackMask;
    }


    /**
     * Updates the random number generator.
     */
    private void updateRNG()
    {
        // If the noise mode is 1, both taps are enabled.
        // If the noise mode is 0, the lower tap, whitenoisetap2, is held at 0.
        // The != was a bit-XOR (^) before.
        if (((rng & psgChip.whiteNoiseTap1) != 0) !=
            (((rng & psgChip.whiteNoiseTap2) != (psgChip.ncrStylePsg ? psgChip.whiteNoiseTap2 : 0)) &&
             whiteNoise))
        {
            rng >>= 1;
            rng |= psgChip.feedbackMask;
        }
        else
        {
            rng >>= 1;
        }
    }


    /**
     * Converts the sound frames of the specified SND file to sound in the
     * specified new WAV file (signed, 16 bits, mono), based on simulation
     * with a Programmable Sound Generator.
     */
    public static void main(String[] args)
    throws IOException
    {
        PsgComputer psgComputer           = PsgComputer.TI99;
        double      frameFrequency        = NTSC_FRAME_FREQUENCY;
        double      targetSampleFrequency = 20000.0;

        int argIndex = 0;

        while (args[argIndex].startsWith("-"))
        {
            switch (args[argIndex++])
            {
                case "-computer"       -> psgComputer    = PsgComputer.valueOf(args[argIndex++].toUpperCase());
                case "-framefrequency" -> frameFrequency = frameFrequency(args[argIndex++]);
                default                -> throw new IllegalArgumentException("Unknown option [" + args[--argIndex] + "]");
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

        try (SoundCommandInputStream sndCommandInputStream =
                 new SoundCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName))))
        {
            while (sndCommandInputStream.readFrame() != null)
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
            case "NTSC" -> NTSC_CLOCK_FREQUENCY;
            case "PAL"  -> PAL_CLOCK_FREQUENCY;
            default     -> Double.parseDouble(clockFrequency);
        };
    }


    private static double frameFrequency(String frameFrequency)
    {
        return switch (frameFrequency.toUpperCase())
        {
            case "NTSC" -> NTSC_FRAME_FREQUENCY;
            case "PAL"  -> PAL_FRAME_FREQUENCY;
            default     -> Double.parseDouble(frameFrequency);
        };
    }
}
