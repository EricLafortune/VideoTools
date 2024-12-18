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
 * This LpcFrame represents a stop frame.
 */
public class LpcStopFrame
implements   LpcFrame
{
    // Implementations for LpcFrame.

    public int bitCount()
    {
        return 4;
    }


    public long toBits()
    {
        return 0xfL;
    }


    public String toString(LpcQuantization quantization)
    {
        return toString();
    }


    // Implementation for Cloneable.

    public LpcStopFrame clone()
    {
        try
        {
            return (LpcStopFrame)super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new Error(e);
        }
    }


    // Implementations for Object.

    public String toString()
    {
        return "Stop";
    }
}
