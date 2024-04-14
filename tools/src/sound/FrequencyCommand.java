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
    static final int TONE1_FREQUENCY = 0x80;
    static final int TONE2_FREQUENCY = 0xa0;
    static final int TONE3_FREQUENCY = 0xc0;
    static final int NOISE_FREQUENCY = 0xe0;


    public int frequency;


    /**
     * Creates a new instance with the given generator and frequency.
     */
    public FrequencyCommand(int generator, int frequency)
    {
        super(generator);

        this.frequency = frequency;
    }


    // Implementations for SoundCommand.

    public int type()
    {
        return TYPE_FREQUENCY;
    }


    public byte[] toBytes()
    {
        return generator == NOISE ?
            new byte[] { (byte)(NOISE_FREQUENCY | frequency) } :
            new byte[] { (byte)(TONE1_FREQUENCY | (generator-TONE1 << 5) | frequency & 0x0f),
                         (byte)(frequency >>> 4)};
    }


    // Implementations for Object.

    public boolean equals(Object o)
    {
        if (!super.equals(o))
        {
            return false;
        }

        FrequencyCommand other = (FrequencyCommand)o;

        return this.frequency == other.frequency;
    }


    public String toString()
    {
        return String.format("Frequency(%s, frequency=0x%03x)", generatorName(), frequency);
    }
}
