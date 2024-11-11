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
package musicxml;

import sound.FrequencyCommand;

/**
 * This enum defines a number of instruments.
 *
 * Each instrument has volume attenuation curves for attack(+decay), sustain
 * (optionally), and release. The curves are expressed in units of 2 dB,
 * ranging from 0 (loudest) to 15 (silent). They are represented as discrete
 * samples per 1/60th of a second. The samples are stored as delta values
 * (positive for increasing attack and for decreasing sustain and release).
 * This way, they can more easily be repeated, truncated, and appended.
 *
 * Each instrument can optionally have fixed dividers for the noise generator
 * and for a tone generator (which is tone 2 in the case of tuned noise).
 */
public enum Instrument
{
    PIANO(
        new int[]
        {
            14, // 1
            1, // 0
        },
        new int[]
        {
            0, // 0
            0, // 0
            1, // 1
            0, // 1
            1, // 2
            0, // 2
            0, // 2
            1, // 3
            0, // 3
            1, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            1, // 5
            0, // 5
            0, // 5
            1, // 6
            0, // 6
            0, // 6
            0, // 6
            1, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            1, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            0, // 8
            1, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            0, // 9
            1, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
        },
        new int[]
        {
            1, // 11
            0, // 11
            1, // 12
            1, // 13
            0, // 13
            1, // 14
        }),

    XYLOPHONE(
        new int[]
        {
            15, // 0
        },
        new int[]
        {
            0, // 0
            1, // 1
            0, // 1
            1, // 2
            2, // 4
            2, // 6
            1, // 7
            0, // 7
            0, // 7
            -1, // 6
            0, // 6
            0, // 6
            0, // 6
            1, // 7
            1, // 8
            -1, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
        },
        new int[]
        {
            1, // 8
            -1, // 7
            0, // 7
            1, // 8
            0, // 8
            0, // 8
            1, // 9
            0, // 9
            1, // 10
            0, // 10
            -1, // 9
            0, // 9
            0, // 9
            0, // 9
            1, // 10
            1, // 11
            0, // 11
            0, // 11
            0, // 11
            1, // 12
            1, // 13
            0, // 13
            3, // 16
        }),

    GUITAR(
        new int[]
        {
            14, // 1
            1, // 0
        },
        new int[]
        {
            0, // 0
            0, // 0
            1, // 1
            0, // 1
            0, // 1
            0, // 1
            1, // 2
            0, // 2
            0, // 2
            0, // 2
            0, // 2
            1, // 3
            0, // 3
            0, // 3
            0, // 3
            1, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            1, // 5
            -1, // 4
            1, // 5
            -1, // 4
            1, // 5
            0, // 5
            0, // 5
            0, // 5
            0, // 5
            0, // 5
            0, // 5
            0, // 5
            0, // 5
            1, // 6
            0, // 6
            0, // 6
            1, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
            0, // 7
        },
        new int[]
        {
            1, // 8
            0, // 8
            0, // 8
            1, // 9
            0, // 9
            0, // 9
            1, // 10
            0, // 10
            1, // 11
            0, // 11
            1, // 12
            -1, // 11
            0, // 11
            0, // 11
            -1, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            0, // 10
            1, // 11
            0, // 11
            0, // 11
            1, // 12
            0, // 12
            0, // 12
            1, // 13
            0, // 13
            0, // 13
            0, // 13
            0, // 13
            -1, // 12
            0, // 12
            0, // 12
            0, // 12
            0, // 12
            0, // 12
            0, // 12
            1, // 13
            0, // 13
            0, // 13
            0, // 13
            0, // 13
            0, // 13
            0, // 13
            0, // 13
            0, // 13
            1, // 14
            0, // 14
            0, // 14
            0, // 14
            0, // 14
            0, // 14
            0, // 14
            0, // 14
            0, // 14
            0, // 14
            1, // 15
        }),

    VIOLIN(
        new int[]
        {
            9, // 6
            2, // 4
            0, // 4
            0, // 4
            0, // 4
            1, // 3
            0, // 3
            0, // 3
            0, // 3
            0, // 3
            0, // 3
            1, // 2
            1, // 3
            0, // 3
            1, // 2
            0, // 2
            1, // 1
            0, // 1
            0, // 1
            0, // 1
            0, // 1
            1, // 0
        },
        new int[]
        {
            0, // 0
            0, // 0
            0, // 0
            0, // 0
            0, // 0
            0, // 0
            1, // 1
            -1, // 0
            0, // 0
            1, // 1
            0, // 1
            0, // 1
            0, // 1
            -1, // 0
            0, // 0
            0, // 0
            1, // 1
            0, // 1
            0, // 1
            0, // 1
            -1, // 0
            0, // 0
            1, // 1
            0, // 1
            0, // 1
            -1, // 0
            0, // 0
            0, // 0
            0, // 0
            1, // 1
            0, // 1
            0, // 1
            0, // 1
            -1, // 0
            0, // 0
            1, // 1
            0, // 1
            0, // 1
            -1, // 0
            0, // 0
            1, // 1
            0, // 1
            1, // 2
            0, // 2
            -2, // 0
            0, // 0
            0, // 0
            0, // 0
            2, // 2
            0, // 2
            -1, // 1
            0, // 1
            0, // 1
            0, // 1
            0, // 1
            0, // 1
            0, // 1
            -1, // 0
            0, // 0
            0, // 0
            1, // 1
            0, // 1
            -1, // 0
            0, // 0
            0, // 0
            0, // 0
            1, // 1
            0, // 1
            0, // 1
            -1, // 0
            0, // 0
            0, // 0
            1, // 1
            0, // 1
            -1, // 0
            0, // 0
            0, // 0
            0, // 0
            1, // 1
            0, // 1
            0, // 1
            -1, // 0
            0, // 0
            0, // 0
            0, // 0
            1, // 1
            -1, // 0
            0, // 0
            0, // 0
            0, // 0
            0, // 0
        },
        new int[]
        {
            1, // 1
            0, // 1
            0, // 1
            1, // 2
            0, // 2
            2, // 4
            1, // 5
        }),

    FLUTE(
        new int[]
        {
            8, // 7
            3, // 4
            2, // 2
            1, // 1
            1, // 0
        },
        new int[]
        {
            0, // 0
        },
        new int[]
        {
            1, // 1
            0, // 1
            0, // 1
            0, // 1
            0, // 1
            0, // 1
            0, // 1
            0, // 1
            1, // 2
            0, // 2
            0, // 2
            0, // 2
        }),

    SAXOPHONE(
        new int[]
        {
            8, // 7
            3, // 4
            2, // 2
            1, // 1
            1, // 0
            -1, // 1
            -1, // 2
            -1, // 3
        },
        new int[]
        {
            0, // 3
            0, // 3
            1, // 4
            0, // 4
            0, // 4
            -1, // 3
        },
        new int[]
        {
            1, // 4
            0, // 4
            0, // 4
            0, // 4
            0, // 4
            1, // 5
            0, // 5
            0, // 5
            0, // 5
            1, // 6
            0, // 6
            0, // 6
            0, // 6
        },
        FrequencyCommand.TUNED_PERIODIC_NOISE_DIVIDER),

    TENOR_DRUM(
        Constants.DRUM_ATTACK,
        null,
        Constants.DRUM_RELEASE,
        FrequencyCommand.PERIODIC_NOISE0_DIVIDER),

    DRUM(
        Constants.DRUM_ATTACK,
        null,
        Constants.DRUM_RELEASE,
        FrequencyCommand.PERIODIC_NOISE1_DIVIDER),

    BASS_DRUM(
        Constants.DRUM_ATTACK,
        null,
        Constants.DRUM_RELEASE,
        FrequencyCommand.PERIODIC_NOISE2_DIVIDER),

    SNARE_DRUM(
        Constants.DRUM_ATTACK,
        null,
        Constants.DRUM_RELEASE,
        FrequencyCommand.WHITE_NOISE0_DIVIDER),

    SNARE_DRUM1(
        Constants.DRUM_ATTACK,
        null,
        Constants.DRUM_RELEASE,
        FrequencyCommand.WHITE_NOISE1_DIVIDER),

    SNARE_DRUM2(
        Constants.DRUM_ATTACK,
        null,
        Constants.DRUM_RELEASE,
        FrequencyCommand.WHITE_NOISE2_DIVIDER),

    COWBELL(
        Constants.DRUM_ATTACK,
        null,
        Constants.DRUM_RELEASE,
        -1,
        0x3fa), // Note A

    CYMBAL(
        Constants.CYMBAL_ATTACK,
        null,
        Constants.CYMBAL_RELEASE,
        FrequencyCommand.TUNED_WHITE_NOISE_DIVIDER,
        1);


    // The above enum constructors can only refer to constants in a separate class.
    private static class Constants
    {
        public static final int[] DRUM_ATTACK =
        {
            15, // 0
        };
        public static final int[] DRUM_RELEASE =
        {
            4, // 4
            4, // 8
            4, // 12
            1, // 13
        };

        public static final int[] CYMBAL_ATTACK =
        {
            15, // 0
        };
        public static final int[] CYMBAL_RELEASE =
        {
            0, // 0
            1, // 1
            0, // 1
            1, // 2
            2, // 4
            2, // 6
            1, // 7
            0, // 7
            -1, // 6
            0, // 6
            0, // 6
            1, // 7
            1, // 8
            -1, // 7
            0, // 7
            0, // 7
            0, // 7
            1, // 8
            -1, // 7
            0, // 7
            1, // 8
            0, // 8
            0, // 8
            1, // 9
            0, // 9
            1, // 10
            0, // 10
            -1, // 9
            0, // 9
            0, // 9
            0, // 9
            1, // 10
            1, // 11
            0, // 11
            1, // 12
            1, // 13
            0, // 13
            1, // 14
            1, // 15
        };
    }


    public final int[] attack;
    public final int[] sustain;
    public final int[] release;
    public final int   fixedNoiseDivider;
    public final int   fixedToneDivider;


    /**
     * Creates a new instance with the given attenuation profile.
     */
    Instrument(int[] attack,
               int[] sustain,
               int[] release)
    {
        this(attack, sustain, release, -1);
    }


    /**
     * Creates a new instance with the given attenuation profile.
     */
    Instrument(int[] attack,
               int[] sustain,
               int[] release,
               int   fixedNoiseDivider)
    {
        this(attack, sustain, release, fixedNoiseDivider, -1);
    }


    /**
     * Creates a new instance with the given attenuation profile.
     */
    Instrument(int[] attack,
               int[] sustain,
               int[] release,
               int   fixedNoiseDivider,
               int   fixedToneDivider)
    {
        this.attack            = attack;
        this.sustain           = sustain;
        this.release           = release;
        this.fixedNoiseDivider = fixedNoiseDivider;
        this.fixedToneDivider  = fixedToneDivider;
    }


    /**
     * Returns whether the instrument plays only a single tuned note.
     */
    public boolean hasFixedTone()
    {
        return fixedToneDivider >= 0;
    }


    /**
     * Returns whether the instrument is noise.
     */
    public boolean isNoise()
    {
        return fixedNoiseDivider >= 0;
    }


    /**
     * Returns whether the instrument is tuned noise.
     */
    public boolean isUntunedNoise()
    {
        return fixedNoiseDivider >= FrequencyCommand.PERIODIC_NOISE0_DIVIDER &&
               fixedNoiseDivider <= FrequencyCommand.PERIODIC_NOISE2_DIVIDER ||
               fixedNoiseDivider >= FrequencyCommand.WHITE_NOISE0_DIVIDER &&
               fixedNoiseDivider <= FrequencyCommand.WHITE_NOISE2_DIVIDER;
    }


    /**
     * Returns whether the instrument is tuned noise.
     */
    public boolean isTunedNoise()
    {
        return fixedNoiseDivider == FrequencyCommand.TUNED_PERIODIC_NOISE_DIVIDER ||
               fixedNoiseDivider == FrequencyCommand.TUNED_WHITE_NOISE_DIVIDER;
    }


    /**
     * Returns the attenuation delta at the given index, for a note of the
     * given length.
     */
    public int attenuationDelta(int index,
                                int length)
    {
        return  attenuationDelta(index, length, false, false);
    }


    /**
     * Returns the attenuation delta at the given index, for a note of the
     * given length, considering that the note may be tied to a previous
     * note and/or a next note.
     */
    public int attenuationDelta(int     index,
                                int     length,
                                boolean tiePrevious,
                                boolean tieNext)
    {
        return
            // Attack phase?
            index < attack.length && !tiePrevious            ? -attack[index] :
            // Release without sustain phase?
            sustain == null                                  ? release[Math.max(0, Math.min(index - attack.length, release.length-1))] :
            // Release phase?
            release.length - length + index >= 0 && !tieNext ? release[release.length - length + index] :
            // Otherwise sustain, repeating if necessary.
                                                               sustain[(index - (tiePrevious ? 0 : attack.length)) % sustain.length];
    }
}
