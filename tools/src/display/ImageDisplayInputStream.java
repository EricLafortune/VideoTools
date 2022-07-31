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
 * This DisplayInputStream reads its subsequent frames (often just one)
 * from an image file (PNG, GIF,...) of 256x192 pixels.
 */
public class ImageDisplayInputStream
implements   DisplayInputStream
{
    private static final int FRAME_HEIGHT = 192;
    private static final int FRAME_WIDTH  = 256;

    // RGB colors from Mame.
    private static final int[] COLORS_MAME = new int[]
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

    // RGB colors from F18A and Classic99.
    private static final int[] COLORS_F18A = new int[]
    {
        0x000000, // 0x0 transparent
        0x000000, // 0x1 black
        0x22cc33, // 0x2 medium green
        0x55dd66, // 0x3 light green
        0x5544ff, // 0x4 dark blue
        0x7766ff, // 0x5 light blue
        0xdd5544, // 0x6 dark red
        0x44eeff, // 0x7 cyan
        0xff5544, // 0x8 medium red
        0xff7766, // 0x9 light red
        0xddcc33, // 0xa dark yellow
        0xeedd66, // 0xb light yellow
        0x22bb22, // 0xc dark green
        0xcc55cc, // 0xd magenta
        0xcccccc, // 0xe gray
        0xffffff, // 0xf white
    };

    private static final Map<Integer,Byte> RGB_NUMBERS = new HashMap<Integer,Byte>();
    static
    {
        for (int index = 1; index < 16; index++)
        {
            RGB_NUMBERS.put(COLORS_MAME[index], (byte)index);
            RGB_NUMBERS.put(COLORS_F18A[index], (byte)index);
        }
    }

    private final InputStream inputStream;


    /**
     * Creates a new instance that reads its frames from the given input
     * stream.
     */
    public ImageDisplayInputStream(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }


    // Implementations for DisplayInputStream.

    public Display readFrame() throws IOException
    {
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null)
        {
            return null;
        }

        byte[] pixels = pixels(image);

        Display display = new Display();

        for (int sectionIndex = 0; sectionIndex < 3; sectionIndex++)
        {
            Display.Section section = display.section[sectionIndex];
            byte[] screenImageTable = section.screenImageTable;
            long[] patternTable     = section.patternTable;
            long[] colorTable       = section.colorTable;

            for (int character = 0; character < 256; character++)
            {
                long pattern = 0L;
                long colors  = 0L;

                int characterX =                     (character % 32) * 8;
                int characterY = sectionIndex * 64 + (character / 32) * 8;

                // Find a suitable default foreground color and the default
                // background color of this character of 8x8 pixels.
                byte defaultColor1 = -1;
                byte defaultColor2 = -1;

                for (int y = characterY; y < characterY+8; y++)
                {
                    for (int x = characterX; x < characterX+8; x++)
                    {
                        byte color = pixels[y * FRAME_WIDTH + x];
                        if (defaultColor1 < 0)
                        {
                            defaultColor1 = color;
                        }
                        else if (color != defaultColor1)
                        {
                            defaultColor2 = color;
                        }
                    }
                }

                if (defaultColor2 < 0)
                {
                    defaultColor2 = defaultColor1 <= 0x1 ? (byte)0xf : (byte)0x1;
                }

                // Determine the foreground color, the background color, and
                // the pattern of each row of 8 pixels.
                for (int y = characterY; y < characterY+8; y++)
                {
                    // Find a suitable foreground color and background color.
                    byte color1 = -1;
                    byte color2 = -1;

                    for (int x = characterX; x < characterX+8; x++)
                    {
                        byte color = pixels[y * FRAME_WIDTH + x];
                        if (color1 < 0)
                        {
                            color1 = color;
                        }
                        else if (color != color1)
                        {
                            color2 = color;
                        }
                    }

                    if (color2 < 0)
                    {
                        color2 = color1 == defaultColor2 ?
                            defaultColor1 :
                            defaultColor2;
                    }

                    int foregroundColor = Math.max(color1, color2);
                    int backgroundColor = Math.min(color1, color2);

                    // Pack the colors in the long value.
                    colors =       (colors          << 8) |
                             (long)(foregroundColor << 4) |
                             (long)(backgroundColor     );

                    // Determine the pattern and pack it in the long value.
                    for (int x = characterX; x < characterX+8; x++)
                    {
                        pattern <<= 1;

                        byte color = pixels[y * FRAME_WIDTH + x];
                        if (color == foregroundColor)
                        {
                            pattern |= 1L;
                        }
                    }
                }

                // Fill out the tables for this character.
                screenImageTable[character] = (byte)character;
                patternTable[character]     = pattern;
                colorTable[character]       = colors;
            }
        }

        return display;
    }


    public void skipFrame() throws IOException
    {
        readFrame();
    }


    public void skipFrames(int count) throws IOException
    {
        for (int counter = 0; counter < count; counter++)
        {
            skipFrame();
        }
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        inputStream.close();
    }


    // Small utility methods.


    private byte[] pixels(BufferedImage image)
    {
        byte[] pixels = new byte[FRAME_HEIGHT * FRAME_WIDTH];

        int index = 0;

        for (int y = 0; y < FRAME_HEIGHT; y++)
        {
            for (int x = 0; x < FRAME_WIDTH; x++)
            {
                pixels[index++] = colorNumber(image.getRGB(x, y) & 0xffffff);
            }
        }

        return pixels;
    }


    private byte colorNumber(int rgb)
    {
        Byte number = RGB_NUMBERS.get(rgb);
        if (number != null)
        {
            return number;
        }

        int  minDistance   = Integer.MAX_VALUE;
        byte closestNumber = 1;

        Iterator<Map.Entry<Integer,Byte>> iterator = RGB_NUMBERS.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<Integer,Byte> entry = iterator.next();
            int tms_rgb = entry.getKey();

            int deltaRed   = ((rgb >>> 16) & 0xff) - ((tms_rgb >>> 16) & 0xff);
            int deltaGreen = ((rgb >>>  8) & 0xff) - ((tms_rgb >>>  8) & 0xff);
            int deltaBlue  = ((rgb       ) & 0xff) - ((tms_rgb       ) & 0xff);

            int distance = deltaRed   * deltaRed +
                           deltaGreen * deltaGreen +
                           deltaBlue  * deltaBlue;

            if (distance < minDistance)
            {
                minDistance   = distance;
                closestNumber = entry.getValue();
            }
        }

        return closestNumber;
    }


    /**
     * Prints out the display characters of the specified image file.
     */
    public static void main(String[] args)
    {
        try (ImageDisplayInputStream displayInputStream =
                 new ImageDisplayInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0]))))
        {
            Display display = displayInputStream.readFrame();

            System.out.println("Screen image table:");
            for (int sectionIndex = 0; sectionIndex < 3; sectionIndex++)
            {
                byte[] screenImageTable = display.section[sectionIndex].screenImageTable;

                for (int row = 0; row < 8; row++)
                {
                    for (int col = 0; col < 32; col++)
                    {
                        System.out.print(String.format("%02x ", screenImageTable[row * 32 + col]));
                    }

                    System.out.println();
                }
            }
            System.out.println();

            System.out.println("Pattern table:");
            for (int sectionIndex = 0; sectionIndex < 3; sectionIndex++)
            {
                long[] patternTable = display.section[sectionIndex].patternTable;

                for (int row = 0; row < 8; row++)
                {
                    for (int col = 0; col < 8; col++)
                    {
                        System.out.print(String.format("%016x ", patternTable[row * 32 + col]));
                    }

                    System.out.println("...");
                }
            }
            System.out.println();

            System.out.println("Color table:");
            for (int sectionIndex = 0; sectionIndex < 3; sectionIndex++)
            {
                long[] colorTable = display.section[sectionIndex].colorTable;

                for (int row = 0; row < 8; row++)
                {
                    for (int col = 0; col < 8; col++)
                    {
                        System.out.print(String.format("%016x ", colorTable[row * 32 + col]));
                    }

                    System.out.println("...");
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
