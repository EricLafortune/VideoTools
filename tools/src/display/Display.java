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
package display;

import java.util.Arrays;

/**
 * This data class represents a frame of 256x192 pixels in the bitmap mode of
 * the TMS9918 Video Display Processor. This means that it has 3 sections,
 * subsequent horizontal bands of 256x64 pixels.
 */
public class Display
{
    public static final int SECTION_COUNT = 3;


    public Section[] section = new Section[]
    {
        new Section(),
        new Section(),
        new Section(),
    };


    // Implementations for Object.

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        Display that = (Display)o;

        return Arrays.equals(this.section, that.section);
    }


    public int hashCode()
    {
        return Arrays.hashCode(section);
    }


    /**
     * This data class represents a band of 256x64 pixels in the bitmap mode
     * of the TMS9918 Video Display Processor. This means that it has a screen
     * image table with 32x8 characters, a pattern table with the pixel
     * definitions of the characters (8x8 binary pixels per character), and
     * a color table with the color definitions of the characters (8 rows
     * of foreground/background color per character).
     */
    public static class Section
    {
        public byte[] screenImageTable = new byte[256];
        public long[] patternTable     = new long[256];
        public long[] colorTable       = new long[256];

        {
            // Initialize the colors to white on black.
            Arrays.fill(colorTable, 0xf1f1f1f1f1f1f1f1L);
        }

        // Implementations for Object.

        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            Section that = (Section)o;

            return Arrays.equals(this.screenImageTable, that.screenImageTable) &&
                   Arrays.equals(this.patternTable,     that.patternTable)     &&
                   Arrays.equals(this.colorTable,       that.colorTable);
        }


        public int hashCode()
        {
            return Arrays.hashCode(screenImageTable) * 31 +
                   Arrays.hashCode(patternTable) * 17 +
                   Arrays.hashCode(colorTable);
        }
    }
}
