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
 * This SoundCommand represents a volume command. The volume is
 * a value between 0 and 15, inclusive, with 15 meaning off.
 */
public class VolumeCommand
extends      SoundCommand
{
    public static final int SILENT = 0xf;

    static final int TONE1_VOLUME = 0x90;
    static final int TONE2_VOLUME = 0xb0;
    static final int TONE3_VOLUME = 0xd0;
    static final int NOISE_VOLUME = 0xf0;


    public int volume;


    /**
     * Creates a new instance with the given generator and volume.
     */
    public VolumeCommand(int generator, int volume)
    {
        super(generator);

        this.volume = volume;
    }


    // Implementations for SoundCommand.

    public int type()
    {
        return TYPE_VOLUME;
    }


    public byte[] toBytes()
    {
        return
            new byte[] { (byte)(TONE1_VOLUME | (generator-TONE1 << 5) | volume) };
    }


    // Implementations for Object.

    public boolean equals(Object o)
    {
        if (!super.equals(o))
        {
            return false;
        }

        VolumeCommand other = (VolumeCommand)o;

        return this.volume == other.volume;
    }


    public String toString()
    {
        return String.format("Volume(%s, volume=0x%1x)", generatorName(), volume);
    }
}
