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

import java.io.*;
import java.util.*;

/**
 * This DisplayInputStream reads its subsequent frames (often just one)
 * from a PBM image file of 256x192 pixels.
 */
public class PbmDisplayInputStream
implements   DisplayInputStream
{
    private static final int FRAME_HEIGHT = 192;
    private static final int FRAME_WIDTH  = 256;


    private final InputStream inputStream;


    /**
     * Creates a new instance that reads its frames from the given input
     * stream.
     */
    public PbmDisplayInputStream(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }


    // Implementations for DisplayInputStream.

    public Display readFrame() throws IOException
    {
        byte[] pixels = readPbmData();

        Display display = new Display();

        for (int sectionIndex = 0; sectionIndex < Display.SECTION_COUNT; sectionIndex++)
        {
            Display.Section section = display.section[sectionIndex];
            byte[] screenImageTable = section.screenImageTable;
            long[] patternTable     = section.patternTable;

            // Leave the colors white on black.
            //long[] colorTable       = section.colorTable;

            HashMap<Long, Byte> patternCharacters = new HashMap<>();
            BitSet              usedCharacters    = new BitSet(256);

            // Set the two uniform patterns (all black and all white).
            patternTable[0] = 0x0000000000000000L;
            patternTable[1] = 0xffffffffffffffffL;

            // Fix the patterns, so they don't get overwritten.
            patternCharacters.put(0x0000000000000000L, (byte)0);
            patternCharacters.put(0xffffffffffffffffL, (byte)1);

            usedCharacters.set(0);
            usedCharacters.set(1);

            for (int row = 0; row < 8; row++)
            {
                for (int col = 0; col < 32; col++)
                {
                    long pattern = extractPattern(pixels, sectionIndex * 8 + row, col);
                    int  character;

                    Byte earlierCharacter = patternCharacters.get(pattern);
                    if (earlierCharacter == null)
                    {
                        character = usedCharacters.nextClearBit(0);

                        patternCharacters.put(pattern, (byte)character);
                        usedCharacters.set(character);

                        patternTable[character] = pattern;
                    }
                    else
                    {
                        character = earlierCharacter;
                    }

                    screenImageTable[row * 32 + col] = (byte)character;
                }
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

    private byte[] readPbmData() throws IOException
    {
        skipLine(inputStream);
        skipLine(inputStream);

        byte[] frame = new byte[FRAME_HEIGHT * FRAME_WIDTH / 8];

        for (int index = 0; index < frame.length; index++)
        {
            frame[index] = (byte)~inputStream.read();
        }

        return frame;
    }


    private void skipLine(InputStream inputStream) throws IOException
    {
        while (inputStream.read() != '\n')
        {
            // Just discard the character.
        }
    }


    private long extractPattern(byte[] frame, int row, int col)
    {
        long pattern = 0L;

        for (int patternRow = 0; patternRow < 8; patternRow++)
        {
            pattern = (pattern << 8) | (frame[row * 256 + patternRow * 32 + col] & 0xffL);
        }

        return pattern;
    }


    /**
     * Prints out the display characters of the specified PBM file.
     */
    public static void main(String[] args)
    {
        try (PbmDisplayInputStream pbmDisplayInputStream =
                 new PbmDisplayInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0]))))
        {
            Display display = pbmDisplayInputStream.readFrame();

            for (int sectionIndex = 0; sectionIndex < Display.SECTION_COUNT; sectionIndex++)
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
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
