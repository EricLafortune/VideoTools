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
 * This interface represents a frame in Linear Predictive Coding
 * (LPC) format for the TMS52xx speech synthesizer.
 *
 * @see http://www.unige.ch/medecine/nouspikel/ti99/speech.htm
 */
public interface LpcFrame
extends          Cloneable
{
    public int bitCount();

    public long toBits();

    public String toString(LpcQuantization quantization);

    public default long toReversedBits()
    {
        return Long.reverse(toBits()) >>> (64 - bitCount());
    }

    public LpcFrame clone();
}
