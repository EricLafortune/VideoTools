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
 * This LpcFrame represents an unvoiced frame.
 */
public class LpcUnvoicedFrame
extends      LpcEnergyFrame
implements   LpcFrame
{
    public long k;


    /**
     * Creates a new instance with the given energy and reflection coefficients.
     */
    public LpcUnvoicedFrame(int energy, long k)
    {
        super(energy);

        this.k = k;
    }


    // Implementations for LpcFrame.

    public int bitCount()
    {
        return 29;
    }


    public long toBits()
    {
        return
            ((long)energy << 25) |
            (           k      );
    }


    public String toString(LpcQuantization quantization)
    {
        int[] ks = quantization.decodeLpcCoefficients(k, false);

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < ks.length; index++)
        {
            builder.append(String.format("%4d",
                                         quantization.lpcCoefficientTable[index][ks[index]]));
            if (index < ks.length-1)
            {
                builder.append(',');
            }
        }

        return String.format("Unvoiced(energy=%3d, k={%s})",
                             quantization.energyTable[energy],
                             builder.toString());
    }


    // Implementation for Cloneable.

    public LpcUnvoicedFrame clone()
    {
        return (LpcUnvoicedFrame)super.clone();
    }


    // Implementations for Object.

    public String toString()
    {
        return String.format("Unvoiced(energy=%01x, k=%05x)", energy, k);
    }
}
