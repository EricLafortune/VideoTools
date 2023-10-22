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

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.*;
import java.util.*;

/**
 * This class writes a Display frame to a PNG image file of 256x192 pixels.
 */
public class ImageDisplayOutputStream
implements   AutoCloseable
{
    private static final int FRAME_HEIGHT = 192;
    private static final int FRAME_WIDTH  = 256;

    // RGB colors from Mame.
    private static final int[] COLORS = new int[]
    {
        0x000000, // 0x0 transparent
        0x000000, // 0x1 black
        0x21c842, // 0x2 medium green
        0x5edc78, // 0x3 light green
        0x5455ed, // 0x4 dark blue
        0x7d76fc, // 0x5 light blue
        0xd4524d, // 0x6 dark red
        0x42ebf5, // 0x7 cyan
        0xfc5554, // 0x8 medium red
        0xff7978, // 0x9 light red
        0xd4c154, // 0xa dark yellow
        0xe6ce80, // 0xb light yellow
        0x21b03b, // 0xc dark green
        0xc95bba, // 0xd magenta
        0xcccccc, // 0xe gray
        0xffffff, // 0xf white
    };

    private final OutputStream outputStream;


    /**
     * Creates a new instance that writes its frames from the given output
     * stream.
     */
    public ImageDisplayOutputStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }


    public void writeFrame(Display display) throws IOException
    {
        IndexColorModel colorModel =
            new IndexColorModel(8, 16, COLORS, 0, false, 0, DataBuffer.TYPE_BYTE);

        BufferedImage image =
            new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_BYTE_INDEXED, colorModel);

        WritableRaster raster = image.getRaster();

        // Convert all 3 sections.
        for (int sectionIndex = 0; sectionIndex < Display.SECTION_COUNT; sectionIndex++)
        {
            Display.Section section = display.section[sectionIndex];
            byte[] screenImageTable = section.screenImageTable;
            long[] patternTable     = section.patternTable;
            long[] colorTable       = section.colorTable;

            // Convert all 8x32 characters in a section.
            for (int screenY = 0; screenY < 8; screenY++)
            {
                for (int screenX = 0; screenX < 32; screenX++)
                {
                    // Get the character and its pattern and colors.
                    int  character = screenImageTable[screenY * 32 + screenX] & 0xff;
                    long pattern   = patternTable[character];
                    long colors    = colorTable[character];

                    // Set all 8x8 pixels in the raster.
                    for (int patternY = 0; patternY < 8; patternY++)
                    {
                        for (int patternX = 0; patternX < 8; patternX++)
                        {
                            int patternShift = 63 - patternY * 8 - patternX;
                            int foreground   = (int)(pattern >>> patternShift) & 0x1;
                            int colorShift   = (15 - patternY * 2 - 1 + foreground) * 4;
                            int color        = (int)(colors >>> colorShift) & 0xf;

                            raster.setSample(screenX * 8 + patternX,
                                             sectionIndex * 64 + screenY * 8 + patternY,
                                             0,
                                             color);
                        }
                    }
                }
            }
        }

        ImageIO.write(image, "PNG", outputStream);
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        outputStream.close();
    }


    /**
     * Copies the frame of the specified image file to the specified new file.
     */
    public static void main(String[] args)
    throws IOException
    {
        try (ImageDisplayInputStream displayInputStream =
                 new ImageDisplayInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0]))))
        {
            Display display = displayInputStream.readFrame();

            try (ImageDisplayOutputStream displayOutputStream =
                     new ImageDisplayOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(args[1]))))
            {
                displayOutputStream.writeFrame(display);
            }
        }
    }
}
