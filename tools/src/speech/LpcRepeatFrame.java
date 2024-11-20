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
package speech;

/**
 * This LpcFrame represents a repeat frame.
 */
public class LpcRepeatFrame
extends      LpcPitchFrame
implements   LpcFrame
{
    /**
     * Creates a new instance with the given energy and pitch.
     */
    public LpcRepeatFrame(int energy, int pitch)
    {
        super(energy, pitch);
    }


    // Implementations for LpcFrame.

    public int bitCount()
    {
        return 11;
    }


    public long toBits()
    {
        return
            ((long)energy << 7) |
            (          1L << 6) |
            ((long) pitch     );
    }


    public String toString(LpcQuantization quantization)
    {
        return String.format("Repeat(energy=%3d, pitch=%3d)",
                             quantization.energyTable[energy],
                             quantization.pitchTable[pitch]);
    }


    // Implementation for Cloneable.

    public LpcRepeatFrame clone()
    {
        return (LpcRepeatFrame)super.clone();
    }


    // Implementations for Object.

    public String toString()
    {
        return String.format("Repeat(energy=%01x, pitch=%02x)", energy, pitch);
    }
}
