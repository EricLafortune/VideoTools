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
 * This base class represents a sound command for the TMS9919 / SN76489 sound
 * chip.
 *
 * @see http://www.unige.ch/medecine/nouspikel/ti99/tms9919.htm
 */
public abstract class SoundCommand
{
    public static final int TONE0 = 0;
    public static final int TONE1 = 1;
    public static final int TONE2 = 2;
    public static final int NOISE = 3;

    public static final int TYPE_FREQUENCY = 0;
    public static final int TYPE_VOLUME    = 1;


    public int generator;


    public SoundCommand(int generator)
    {
        this.generator = generator;
    }


    public boolean isNoiseTuningTone()
    {
        return generator == TONE2;
    }


    public boolean isNoise()
    {
        return generator == NOISE;
    }


    public abstract int type();


    public String generatorName()
    {
        switch (generator)
        {
            case TONE0: return "Tone0";
            case TONE1: return "Tone1";
            case TONE2: return "Tone2";
            case NOISE: return "Noise";
            default:    return "Unknown";
        }
    }


    public abstract byte[] toBytes();


    // Implementations for Object.

    public boolean equals(Object o)
    {
        if (o == null || this.getClass() != o.getClass())
        {
            return false;
        }

        SoundCommand other = (SoundCommand)o;

        return this.generator == other.generator;
    }
}
