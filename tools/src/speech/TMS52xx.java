/**
 * Video tools for the TI-99/4A home computer.
 *
 * This file was derived from tms5220.cpp with a BSD-3-Clause in Mame:
 *
 * Copyright (c) Frank Palazzolo, Aaron Giles, Jonathan Gevaryahu,
 *               Raphael Nabet, Couriersud, Michael Zapf
 *
 * Conversion to Java, modification, and cleanup:
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
 * This class simulates a TMS52xx speech synthesizer chip. It accepts LPC
 * frames and returns the corresponding sound samples (200 samples per frame).
 */
public class TMS52xx
implements   Cloneable
{
    private static final boolean DEBUG = false;

    public static final double FRAME_FREQUENCY = 40.0;
    public static final double SOUND_FREQUENCY = 8000.0;
    public static final int    FRAME_SIZE      = 200;

    private static final int[] INTERPOLATION_SHIFT_COEFFICIENTS =
        { 0, 3, 3, 3, 2, 2, 1, 1 };


    private final LpcQuantization lpcQuantization;
    private final boolean         digitalOutputRange;
    private final boolean         fullOutputPrecision;

    // Needed for repeat frames.
    protected int   energyIndex;
    protected int   pitchIndex;
    protected int[] kIndices;

    // The current values.
    protected int   energy;
    protected int   pitch;
    protected int[] k = new int[10];

    // Used in the lattice filter.
    private int[] u = new int[11];
    private int[] x = new int[10];

    // Needed for the lattice filter to match the patent.
    private int   previousEnergy;

    // The random noise generator configuration is: 1 + x + x^3 + x^4 + x^13.
    private short rng = 0x1fff;

    // Circuit 412: the pitch counter is forced to zero under certain circumstances.
    private boolean resetChirpIndex;
    private int     chirpIndex;


    /**
     * Creates a new instance with a given quantization and 8 bits analog
     * output range and precision.
     * @param lpcQuantization the encoding and quantization of the chip
     *                        (either TMS5200 or TMS5220).
     */
    public TMS52xx(LpcQuantization lpcQuantization)
    {
        this(lpcQuantization, false, false);
    }


    /**
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
    public TMS52xx(LpcQuantization lpcQuantization,
                   boolean         digitalOutputRange,
                   boolean         fullOutputPrecision)
    {
        this.lpcQuantization     = lpcQuantization;
        this.digitalOutputRange  = digitalOutputRange;
        this.fullOutputPrecision = fullOutputPrecision;
    }


    /**
     * Returns the 200 8kHz samples that correspond to the given LPC frame.
     * They are returned as signed 16 bits values, although their precision
     * depends on the settings.
     */
    public short[] play(LpcFrame lpcFrame)
    {
        short[] samples = new short[FRAME_SIZE];

        play(lpcFrame, samples);

        return samples;
    }


    /**
     * Computes the 200 8kHz samples that correspond to the given LPC frame.
     * They are returned as signed 16 bits values, although their precision
     * depends on the settings.
     */
    public void play(LpcFrame lpcFrame, short[] samples)
    {
        if (samples.length != FRAME_SIZE)
        {
            throw new IllegalArgumentException("Sample buffer size must be "+FRAME_SIZE+", not "+samples.length);
        }

        // Needed to decide on interpolation.
        int oldEnergyIndex = energyIndex;
        int oldPitchIndex  = pitchIndex;

        // Extract the speech parameters from the LPC frame.
        parseLpcFrame(lpcFrame);

        // Set the interpolation flag. Interpolation inhibit cases:
        // * Old frame was voiced, new is unvoiced
        // * Old frame was unvoiced, new is voiced
        // * Old frame was silence/zero energy, new has non-zero energy
        // * Old frame was unvoiced, new frame is silence/zero energy
        boolean interpolate =
            unvoiced(oldPitchIndex) == unvoiced(pitchIndex)  &&
            (silence(energyIndex) ?
                 !unvoiced(oldPitchIndex) :
                 !silence(oldEnergyIndex));

        if (DEBUG)
        {
            System.out.println(lpcFrame+": interpolate="+interpolate);
        }

        for (int index = 0; index < samples.length; index++)
        {                                                     // For 200 samples:
            int interpolationPeriod   = (index / 25 + 1) % 8; // (25x1 ... 25x7 25x0)
            int interpolatedParameter = index % 25 / 2;       // 8 x (0 0  1 1 ... 11 11  12)
            int subcycle              = index % 25 % 2 + 1;   // 8 x (1 2  1 2 ...  1  2   1)

            // Update speech parameters every subcycle 2 (B cycle).
            if (subcycle == 2)
            {
                // Reset the chirp around IP = 0, B subcycle.
                if (interpolationPeriod == 0 && interpolatedParameter == 0)
                {
                    resetChirpIndex(false);
                }

                // Interpolate or set the speech parameters.
                // For most of the frame, the samples are largely based on the
                // old indices (energy, pitch,...) Notably, without
                // interpolation, the parameters only switch to the new ones
                // during the last interpolation period (25 samples out of 200).
                if (interpolate || interpolationPeriod == 0)
                {
                    interpolateParameter(interpolatedParameter,
                                         interpolationPeriod);
                }
            }

            // Computes the excitation: noise or chirp.
            int excitation = excitation(oldPitchIndex);

            // Update the random number generator.
            updateRNG();

            // Compute the sample.
            int sample = latticeFilter(excitation);

            // Clip the output to 8 or 10-bit precision.
            samples[index] = clip(sample);

            if (DEBUG)
            {
                System.out.println(String.format("%s: [%03d] intPeriod=%2d, intParam=%2d, subc=%d, %s, ene=%2d, pit=%2d, chi=%2d, exc=%4d, sam=%4d -> %6d",
                                                 lpcFrame.toString(),
                                                 index,
                                                 interpolationPeriod,
                                                 interpolatedParameter,
                                                 subcycle,
                                                 unvoiced(oldPitchIndex)?"unvoic":"voiced",
                                                 energy,
                                                 pitch,
                                                 chirpIndex,
                                                 excitation,
                                                 sample,
                                                 samples[index]));
            }

            // Are we at the end of an interpolation period?
            if (interpolatedParameter == 12 && subcycle == 1) // RESETF3
            {
                // Circuit 412 in the patent acts a reset, resetting the pitch
                // counter to 0 if INHIBIT was true during the most recent
                // frame transition. The exact time this occurs is betwen IP=7,
                // PC=12 sub=0, T=t12 and m_IP = 0, PC=0 sub=0, T=t12, a period
                // of exactly 20 cycles, which overlaps the time OLDE and OLDP
                // are updated at IP=7 PC=12 T17 (and hence INHIBIT itself 2
                // t-cycles later). According to testing the pitch zeroing
                // lasts approximately 2 samples. We set the zeroing latch here,
                // and unset it on PC=1 in the generator.

                // Are we at the end of the last-but-one interpolation period?
                if (interpolationPeriod == 7) // RESETL4
                {
                    // Latch the old indices.
                    oldEnergyIndex = energyIndex;
                    oldPitchIndex  = pitchIndex;

                    if (!interpolate)
                    {
                        resetChirpIndex(true);
                    }
                }
            }

            // Update the chirp index, resetting it if necessary.
            updateChirpIndex();
        }
    }


    /**
     * Extracts the speech parameters from the given LPC frame.
     */
    protected void parseLpcFrame(LpcFrame lpcFrame)
    {
        // Parse the frame.
        if (lpcFrame instanceof LpcStopFrame)
        {
            energyIndex = 0xf;
        }
        else if (lpcFrame instanceof LpcSilenceFrame)
        {
            energyIndex = lpcQuantization.silenceEnergy();
        }
        else if (lpcFrame instanceof LpcRepeatFrame)
        {
            LpcRepeatFrame frame = (LpcRepeatFrame)lpcFrame;

            energyIndex = frame.energy;
        }
        else if (lpcFrame instanceof LpcUnvoicedFrame)
        {
            LpcUnvoicedFrame frame = (LpcUnvoicedFrame)lpcFrame;

            energyIndex = frame.energy;
            pitchIndex  = 0;
            kIndices    = lpcQuantization.decodeLpcCoefficients(frame.k, false);
            // TODO: null k coefficients?
        }
        else if (lpcFrame instanceof LpcVoicedFrame)
        {
            LpcVoicedFrame frame = (LpcVoicedFrame)lpcFrame;

            energyIndex = frame.energy;
            pitchIndex  = frame.pitch;
            kIndices    = lpcQuantization.decodeLpcCoefficients(frame.k, true);
        }
    }


    /**
     * Interpolates the specified speech parameter.
     */
    private void interpolateParameter(int interpolatedParameter,
                                      int interpolationPeriod)
    {
        // Which parameter should be interpolated?
        switch (interpolatedParameter)
        {
            case 0:
            {
                // PC = 0, B cycle: write updated energy.
                energy += (lpcQuantization.energyTable[energyIndex] - energy)
                          >> INTERPOLATION_SHIFT_COEFFICIENTS[interpolationPeriod];
                break;
            }
            case 1:
            {
                // PC = 1, B cycle: write updated pitch.
                pitch += (lpcQuantization.pitchTable[pitchIndex] - pitch)
                         >> INTERPOLATION_SHIFT_COEFFICIENTS[interpolationPeriod];
                break;
            }
            case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9: case 10: case 11:
            {
                // PC = 2 through 11, B cycle: write updated K1 through K10.
                int kIndex = interpolatedParameter - 2;
                int targetK = kIndices == null || kIndex >= kIndices.length ? 0 :
                    lpcQuantization.lpcCoefficientTable[kIndex][kIndices[kIndex]];

                k[kIndex] += (targetK - k[kIndex])
                             >> INTERPOLATION_SHIFT_COEFFICIENTS[interpolationPeriod];
                break;
            }
            default:
            {
                // PC = 12 doesn't have a subcycle 2.
                throw new IllegalStateException();
            }
        }
    }


    /**
     * Sets the specified speech parameter to its final indexed value.
     */
    private void setParameter(int interpolatedParameter)
    {
        // Which parameter should be set?
        switch (interpolatedParameter)
        {
            case 0:
            {   // PC = 0, B cycle: write updated energy.
                energy = lpcQuantization.energyTable[energyIndex];
                break;
            }
            case 1:
            {   // PC = 1, B cycle: write updated pitch.
                pitch = lpcQuantization.pitchTable[pitchIndex];
                break;
            }
            case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9: case 10: case 11:
            {   // PC = 2 through 11, B cycle: write updated K1 through K10.
                int kIndex = interpolatedParameter - 2;
                k[kIndex] = kIndices == null || kIndex >= kIndices.length ? 0 :
                    lpcQuantization.lpcCoefficientTable[kIndex][kIndices[kIndex]];
                break;
            }
            default:
            {   // PC = 12 doesn't have a subcycle 2.
                throw new IllegalStateException();
            }
        }
    }


    /**
     * Sets the speech parameters to their indexed values.
     */
    protected void setParameters()
    {
        energy = lpcQuantization.energyTable[energyIndex];
        pitch  = lpcQuantization.pitchTable[pitchIndex];

        for (int index = 0; index < 10; index++)
        {
            k[index] = kIndices == null || index >= kIndices.length ? 0 :
                lpcQuantization.lpcCoefficientTable[index][kIndices[index]];
        }
    }


    /**
     * Computes the excitation: noise or chirp.
     */
    protected int excitation(int pitchIndex)
    {
        return unvoiced(pitchIndex) ?
            // Unvoiced: noise generator.
            // According to the patent it is (either + or -) half of the
            // maximum value in the chirp table, so either 01000000(0x40) or
            // 11000000(0xC0), based on the least significant bit of the RNG.
            (rng & 1) != 0 ?
                ~0x3F :
                0x40 :
            // Voiced: chirp table.
            // US patent 4331836 Figure 14B shows, and logic would hold,
            // that a pitch based chirp function has a chirp/peak and then
            // a long chain of zeroes. The last entry of the chirp rom is
            // at address 0b110011 (51d), the 52nd sample, and if the
            // address reaches that point the ADDRESS incrementer is
            // disabled, forcing all samples beyond 51d to be == 51d.
            chirpIndex >= lpcQuantization.chirpTable.length ?
                lpcQuantization.chirpTable[lpcQuantization.chirpTable.length-1] :
                lpcQuantization.chirpTable[chirpIndex];
    }


    /**
     * Updates the random number generator.
     */
    protected void updateRNG()
    {
        // Update LFSR[] 20* times every sample (once per T cycle), like
        // patent shows.
        for (int i=0; i<20; i++)
        {
            int bit = ((rng >> 12) & 1) ^
                      ((rng >>  3) & 1) ^
                      ((rng >>  2) & 1) ^
                      ((rng >>  0) & 1);
            rng <<= 1;
            rng |= bit;
        }
    }


    /**
     * Executes one 'full run' of the lattice filter on a specific byte of
     * excitation data, and specific values of all the current k constants,  and returns the
     * resulting sample.
     */
    int latticeFilter(int excitationData)
    {
        // Originally copied verbatim from table I in US patent 4,209,804, now
        // updated to be in same order as the actual chip does it, not that it matters.
        // notation equivalencies from table:
        //   Yn(i) == u[n-1]
        //   Kn = k[n-1]
        //   bn = x[n-1]

        // int ep = matrix_multiply(m_previous_energy, (m_excitation_data<<6));  //Y(11)
        // m_u[10] = ep;
        // for (int i = 0; i < 10; i++)
        // {
        //   int ii = 10-i; // for m = 10, this would be 11 - i, and since i is from 1 to 10, then ii ranges from 10 to 1
        //   //int jj = ii+1; // this variable, even on the fortran version, is
        //                    // never used. It probably was intended to be used on the two lines
        //                    // below the next one to save some redundant additions on each.
        //   ep = ep - (((m_current_k[ii-1][]  m_x[ii-1])>>9)|1); // subtract reflection from lower stage 'top of lattice'
        //   m_u[ii-1] = ep;
        //   m_x[ii] = m_x[ii-1] + (((m_current_k[ii-1][]  ep)>>9)|1); // add reflection from upper stage 'bottom of lattice'
        // }
        // m_x[0] = ep; // feed the last section of the top of the lattice directly to the bottom of the lattice

        // Compute the elements of the array u.
        u[k.length] = matrixMultiply(previousEnergy, excitationData << 6);  //Y(11)
        for (int i = k.length-1; i >= 0; i--)
        {
            u[i] = u[i + 1] - matrixMultiply(k[i], x[i]);
        }

        // Update the elements of the array x.
        for (int i = k.length-1; i > 0; i--)
        {
            x[i] = x[i - 1] + matrixMultiply(k[i - 1], u[i - 1]);
        }
        x[0] = u[0];

        previousEnergy = energy;

        return u[0];
    }


    /**
     * Multiplies and shifts for the lattice filter.
     * @param a the k coefficient, which is left-truncated to 10 bits (9 bits
     *          plus a sign bit, -512 to 511).
     * @param b the running result, which is left-truncated to 15 bits (14 bits
     *          plus a sign bit, -16384 to 16383). The documentation of
     *          tms5220.cpp incorrectly states that this is 14 bits.
     * @return the product, which is downshifted to 16 bits, The documentation
     *         of tms5220.cpp incorrectly states that this is 14 bits.
     */
    private int matrixMultiply(int a, int b)
    {
        // Left-truncate the reflection coefficient to 10 bits (already so in
        // practice).
        while (a >  0x01ff) { a -= 0x0400; }
        while (a < -0x0200) { a += 0x0400; }

        // Left-truncate the running result to 15 bits.
        while (b >  0x3fff) { b -= 0x8000; }
        while (b < -0x4000) { b += 0x8000; }

        // Shift the result from 25 bits to 16 bits. The documentation of
        // tms5220.cpp incorrectly states that this is 14 bits and later
        // implies that this is 15 bits.
        // This isn't technically right to the chip, which truncates the lowest
        // result bit, but it causes glitches otherwise.
        return (a * b) >> 9;
    }


    /**
     * Clips the output of the lattice filter to 8-bit analog precision or
     * 10-bit digital precision.
     */
    protected short clip(int sample)
    {
        // Left-truncate the sample to 15 bits (since its possible that the
        // addition at the final (k1) stage of the lattice overflowed). The
        // documentation of tms5220.cpp incorrectly states that this is 14 bits.
        // Output: ssbc defg hijk lmno
        while (sample >  0x3fff) sample -= 0x8000;
        while (sample < -0x4000) sample += 0x8000;

        // The TMS52xx has two different ways of providing output data: the
        // analog speaker pin (which was usually used) and the digital I/O pin.
        // The internal DAC used to feed the analog pin is only 8 bits, and
        // has the funny clipping/clamping logic, while the digital pin gives
        // full 11-bit resolution of the output data.
        return digitalOutputRange ?
            clipDigital(sample) :
            clipAnalog(sample);
    }


    /**
     * Clips the 15-bit output of the lattice filter to its digital 11-bit
     * value, and upshifts/range extends this to 16 bits.
     */
    private short clipDigital(int sample)
    {
        if (fullOutputPrecision)
        {
            // Upshift 15 bits to 16 bits.
            return (short)(sample << 1);
        }

        // Right-truncate 15 bits to 11 bits.
        // Input:  ssbc defg hijk lmno
        // Output: ssbc defg hijk 0000
        sample &= ~0xf;

        // Upshift to 16 bits.
        // Input:  ssbc defg hijk 0000
        // Taps: :   ^^ ^^^            = 0x3e00;
        // Output: sbcd efgh ijkb cdef
        return (short)(( sample            << 1) |
                       ((sample & 0x3e00) >>> 9));
    }


    /**
     * Clips the 15-bit output of the lattice filter to its analog 8-bit
     * value, and upshifts/range extends this to 16 bits. The documentation
     * of tms5220.cpp incorrectly states that this is from 14 bits to 10 bits.
     */
    private short clipAnalog(int sample)
    {
        // Patent: clip 14 bits to 8 bits.
        // 1x xxxx xxxx xxxx -> 1000 0000
        // 11 1bcd efgh xxxx -> 1bcd efgh
        // 00 0bcd efgh xxxx -> 0bcd efgh
        // 0x xxxx xxxx xxxx -> 0111 1111

        // Step 1: clamp 15 bits to 12 bits.
        // Input:  ssbc defg hijk lmno
        // Output: ssss sefg hijk lmno
        if      (sample >  0x07ff) sample =  0x07ff;
        else if (sample < -0x0800) sample = -0x0800;

        if (fullOutputPrecision)
        {
            // Upshift 12 bits to 16 bits.
            return (short)(sample << 4);
        }

        // Step 2: right-truncate to 8 bits.
        // Input:  ssss sbcd efgh ijkl
        // Output: ssss sbcd efgh 0000
        sample &= ~0xf;

        // Upshift to 16 bits.
        // Input:  ssss sbcd efgh 0000
        // Taps:         ^^^ ^^^^      = 0x07f0
        // Taps:         ^             = 0x0400
        // Output: sbcd efgh bcde fghb
        return (short)(( sample            <<  4) |
                       ((sample & 0x07f0) >>>  3) |
                       ((sample & 0x0400) >>> 10));
    }


    /**
     * Sets the flag to force resetting the chirp index after each sample.
     */
    private void resetChirpIndex(boolean flag)
    {
        resetChirpIndex = flag;
    }


    /**
     * Updates the chirp index, resetting it if necessary.
     */
    protected void updateChirpIndex()
    {
        if (resetChirpIndex || ++chirpIndex >= pitch)
        {
            chirpIndex = 0;
        }
    }


    private boolean silence(int energyIndex)
    {
        return energyIndex == lpcQuantization.silenceEnergy();
    }

    private boolean unvoiced(int pitchIndex)
    {
        return pitchIndex == 0x0;
    }


    // Implementation for Cloneable.

    /**
     * Returns a deep clone of this instance. The clone can then run
     * independently, continuing from the current state.
     */
    public TMS52xx clone()
    {
        try
        {
            TMS52xx clone = (TMS52xx)super.clone();

            //clone.lpcQuantization = this.lpcQuantization;

            //clone.energyIndex     = this.energyIndex;
            //clone.pitchIndex      = this.pitchIndex;
            if (this.kIndices != null)
            {
                clone.kIndices    = this.kIndices.clone();
            }

            //clone.energy          = this.energy;
            //clone.pitch           = this.pitch;
            clone.k               = this.k.clone();

            clone.u               = this.u.clone();
            clone.x               = this.x.clone();
            //clone.rng             = this.rng;

            //clone.resetChirpIndex = this.resetChirpIndex;
            //clone.chirpIndex      = this.chirpIndex;

            //clone.previousEnergy  = this.previousEnergy;

            return clone;
        }
        catch (CloneNotSupportedException e)
        {
            throw new Error(e);
        }
    }


    /**
     * Converts the speech frames of the specified LPC file to sound in the
     * specified new WAV file (signed, 16 bits, mono), based on simulation
     * with a TMS5200 or TMS5220 speech synthesis chip.
     */
    public static void main(String[] args)
    throws IOException
    {
        LpcQuantization quantization = LpcQuantization.TMS5200;

        int argIndex = 0;

        while (args[argIndex].startsWith("-"))
        {
            switch (args[argIndex++])
            {
                case "-quantization" -> quantization = LpcQuantization.valueOf(args[argIndex++].toUpperCase());
                case "-tms5200"      -> quantization = LpcQuantization.TMS5200;
                case "-tms5220"      -> quantization = LpcQuantization.TMS5220;
                default              -> throw new IllegalArgumentException("Unknown option [" + args[--argIndex] + "]");
            };
        }

        String inputFileName  = args[argIndex++];
        String outputFileName = args[argIndex++];

        // Count the number of LPC coefficient frames in the input file.
        int frameCount = 0;

        try (LpcFrameInput lpcFrameInput =
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName))))
        {
            LpcFrame frame;
            while ((frame = lpcFrameInput.readFrame()) != null)
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
                 new LpcSampleInputStream(new TMS52xx(quantization, false, true),
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
