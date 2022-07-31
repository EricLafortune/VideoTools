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
package display;

/**
 * This data class represents the graphical definition of a character in the
 * bitmap mode of the TMS9918 Video Display Processor. This means that it
 * defines a binary pattern of 8x8 pixels and a foreground/background color
 * for each of the 8 rows.
 */
public class CharacterDefinition
{
    public long pattern;
    public long colors;


    public CharacterDefinition(long pattern,
                               long colors)
    {
        this.pattern = pattern;
        this.colors  = colors;
    }


    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || this.getClass() != o.getClass())
        {
            return false;
        }

        CharacterDefinition that = (CharacterDefinition)o;

        return this.pattern == that.pattern &&
               this.colors  == that.colors;
    }


    public int hashCode()
    {
        return (int)((pattern >> 32) + pattern) * 31 +
               (int)((colors  >> 32) + colors);
    }
}
