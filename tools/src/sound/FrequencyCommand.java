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
package sound;

/**
 * This SoundCommand represents a frequency command. The frequency is
 * represented in its encoded integer form. For tones, the corresponding
 * frequency in Herz is 111860.8 / frequency (on a system with a 4 MHz
 * clock, such as the TI-99/4A).
 */
public class FrequencyCommand
extends      SoundCommand
{
    public static final int MIN_TONE_DIVIDER = 0x0001;
    public static final int MAX_TONE_DIVIDER = 0x03ff;

    public static final int MIN_NOISE_DIVIDER = 0x0;
    public static final int MAX_NOISE_DIVIDER = 0x7;

    public static final int PERIODIC_NOISE0_DIVIDER      = 0x0;
    public static final int PERIODIC_NOISE1_DIVIDER      = 0x1;
    public static final int PERIODIC_NOISE2_DIVIDER      = 0x2;
    public static final int TUNED_PERIODIC_NOISE_DIVIDER = 0x3;
    public static final int WHITE_NOISE0_DIVIDER         = 0x4;
    public static final int WHITE_NOISE1_DIVIDER         = 0x5;
    public static final int WHITE_NOISE2_DIVIDER         = 0x6;
    public static final int TUNED_WHITE_NOISE_DIVIDER    = 0x7;

    static final int TONE0_FREQUENCY = 0x80;
    static final int TONE1_FREQUENCY = 0xa0;
    static final int TONE2_FREQUENCY = 0xc0;
    static final int NOISE_FREQUENCY = 0xe0;


    public int divider;


    /**
     * Creates a new instance with the given generator and frequency divider.
     */
    public FrequencyCommand(int generator, int divider)
    {
        super(generator);

        //if (generator < NOISE ?
        //        divider < MIN_TONE_DIVIDER ||
        //        divider > MAX_TONE_DIVIDER :
        //        divider < MIN_NOISE_DIVIDER ||
        //        divider > MAX_NOISE_DIVIDER)
        //{
        //    throw new IllegalArgumentException("Frequency divider ["+divider+"] outside of valid range for "+generatorName());
        //}

        this.divider = divider;
    }


    public boolean isPeriodicNoise()
    {
        return isNoise() && divider <= TUNED_PERIODIC_NOISE_DIVIDER;
    }


    public boolean isWhiteNoise()
    {
        return isNoise() && divider >= WHITE_NOISE0_DIVIDER;
    }


    public boolean isTunedNoise()
    {
        return isNoise() && (divider == TUNED_PERIODIC_NOISE_DIVIDER ||
                             divider == TUNED_WHITE_NOISE_DIVIDER);
    }


    public boolean isTunedPeriodicNoise()
    {
        return isNoise() && divider == TUNED_PERIODIC_NOISE_DIVIDER;
    }


    public boolean isTunedWhiteNoise()
    {
        return isNoise() && divider == TUNED_WHITE_NOISE_DIVIDER;
    }


    // Implementations for SoundCommand.

    public int type()
    {
        return TYPE_FREQUENCY;
    }


    public byte[] toBytes()
    {
        return generator == NOISE ?
            new byte[] { (byte)(NOISE_FREQUENCY | divider) } :
            new byte[] { (byte)(TONE0_FREQUENCY | (generator-TONE0 << 5) | divider & 0x0f),
                         (byte)(divider >>> 4)};
    }


    // Implementations for Object.

    public boolean equals(Object o)
    {
        if (!super.equals(o))
        {
            return false;
        }

        FrequencyCommand other = (FrequencyCommand)o;

        return this.divider == other.divider;
    }


    public String toString()
    {
        return String.format("Frequency(%s, divider=0x%03x)", generatorName(), divider);
    }
}
