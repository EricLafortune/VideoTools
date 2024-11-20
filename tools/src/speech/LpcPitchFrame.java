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
 * This LpcFrame represents a frame that has am energy and a pitch.
 */
public abstract class LpcPitchFrame
extends               LpcEnergyFrame
implements            LpcFrame
{
    public int pitch;


    /**
     * Creates a new instance with the given energy and pitch.
     */
    public LpcPitchFrame(int energy, int pitch)
    {
        super(energy);

        this.pitch = pitch;
    }


    // Implementation for Cloneable.

    public LpcPitchFrame clone()
    {
        return (LpcPitchFrame)super.clone();
    }
}
