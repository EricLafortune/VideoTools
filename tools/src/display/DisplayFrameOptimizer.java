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
 * This class optimizes sequences of {@link Display} frames, so they are best
 * suited for fast animation. Notably, they are restructured to minimize the
 * differences between subsequent frames (in a lossless way).
 */
public class DisplayFrameOptimizer
{
    private int[] nextOptimizedCharacters = { 2, 2, 2 };


    public Display optimizeDisplay(Display referenceDisplay,
                                   Display targetDisplay)
    {
        Display optimizedDisplay = new Display();

        for (int sectionIndex = 0; sectionIndex < 3; sectionIndex++)
        {
            optimizeSection(sectionIndex,
                            referenceDisplay.section[sectionIndex],
                            targetDisplay.section[sectionIndex],
                            optimizedDisplay.section[sectionIndex]);
        }

        return optimizedDisplay;
    }


    private void optimizeSection(int             sectionIndex,
                                 Display.Section referenceSection,
                                 Display.Section targetSection,
                                 Display.Section optimizedSection)
    {
        byte[] referenceScreenImageTable = referenceSection.screenImageTable;
        long[] referencePatternTable     = referenceSection.patternTable;
        long[] referenceColorTable       = referenceSection.colorTable;

        byte[] targetScreenImageTable = targetSection.screenImageTable;
        long[] targetPatternTable     = targetSection.patternTable;
        long[] targetColorTable       = targetSection.colorTable;

        byte[] optimizedScreenImageTable = optimizedSection.screenImageTable;
        long[] optimizedPatternTable     = optimizedSection.patternTable;
        long[] optimizedColorTable       = optimizedSection.colorTable;

        // Collect the patterns and colors present in the reference display
        // (may be used or unused).
        HashMap<CharacterDefinition, Byte> definitionCharacters = new HashMap<>();

        for (int character = 0; character < referencePatternTable.length; character++)
        {
            long pattern = referencePatternTable[character];
            long colors  = referenceColorTable[character];

            definitionCharacters.put(new CharacterDefinition(pattern, colors),
                                     (byte)character);
        }

        // Collect and mark the characters used in the reference display.
        // We collect them again to make sure they get precedence.
        BitSet protectedCharacters = new BitSet(256);

        protectedCharacters.set(0);
        protectedCharacters.set(1);

        for (int index = 0; index < referenceScreenImageTable.length; index++)
        {
            int  character = referenceScreenImageTable[index] & 0xff;
            long pattern   = referencePatternTable[character];
            long colors    = referenceColorTable[character];

            definitionCharacters.put(new CharacterDefinition(pattern, colors),
                                     (byte)character);

            protectedCharacters.set(character);
        }

        // Mark the characters whose patterns and colors are recurring in the
        // target display.
        for (int index = 0; index < targetScreenImageTable.length; index++)
        {
            int  targetCharacter = targetScreenImageTable[index] & 0xff;
            long pattern         = targetPatternTable[targetCharacter];
            long colors          = targetColorTable[targetCharacter];

            CharacterDefinition characterDefinition =
                new CharacterDefinition(pattern, colors);

            Byte c = definitionCharacters.get(characterDefinition);
            if (c != null)
            {
                int character = c & 0xff;

                protectedCharacters.set(character);
            }
        }

        // The pattern table and the color table start out the same.
        System.arraycopy(referencePatternTable, 0,
                         optimizedPatternTable, 0,
                         optimizedPatternTable.length);
        System.arraycopy(referenceColorTable, 0,
                         optimizedColorTable, 0,
                         optimizedColorTable.length);

        // Remap the characters, patterns, and colors from the target display.
        for (int index = 0; index < targetScreenImageTable.length; index++)
        {
            int  character = targetScreenImageTable[index] & 0xff;
            long pattern   = targetPatternTable[character];
            long colors    = targetColorTable[character];

            int optimizedCharacter;

            CharacterDefinition characterDefinition =
                new CharacterDefinition(pattern, colors);

            Byte c = definitionCharacters.get(characterDefinition);
            if (c != null)
            {
                optimizedCharacter = c & 0xff;
            }
            else
            {
                optimizedCharacter =
                    protectedCharacters.nextClearBit(nextOptimizedCharacters[sectionIndex]);

                if (optimizedCharacter > 255)
                {
                    optimizedCharacter=
                        protectedCharacters.nextClearBit(2);
                }

                nextOptimizedCharacters[sectionIndex] = optimizedCharacter + 1;

                optimizedPatternTable[optimizedCharacter] = pattern;
                optimizedColorTable[optimizedCharacter]   = colors;

                definitionCharacters.put(characterDefinition,
                                         (byte)optimizedCharacter);
            }

            optimizedScreenImageTable[index] = (byte)optimizedCharacter;
        }
    }


    /**
     * Prints out the optimized display characters of the specified PBM files.
     */
    public static void main(String[] args)
    {
        DisplayFrameOptimizer optimizer =
            new DisplayFrameOptimizer();

        Display referenceDisplay = new Display();

        for (int argIndex = 0; argIndex < args.length; argIndex++)
        {
            try (PbmDisplayInputStream pbmDisplayInputStream =
                     new PbmDisplayInputStream(
                     new BufferedInputStream(
                     new FileInputStream(args[argIndex]))))
            {
                Display targetDisplay =
                    pbmDisplayInputStream.readFrame();

                Display optimizedDisplay =
                    optimizer.optimizeDisplay(referenceDisplay,
                                              targetDisplay);

                for (int sectionIndex = 0; sectionIndex < 3; sectionIndex++)
                {
                    byte[] screenImageTable = optimizedDisplay.section[sectionIndex].screenImageTable;

                    for (int row = 0; row < 8; row++)
                    {
                        for (int col = 0; col < 32; col++)
                        {
                            System.out.print(String.format("%02x ", screenImageTable[row * 32 + col]));
                        }

                        System.out.println();
                    }
                }

                referenceDisplay = optimizedDisplay;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            System.out.println();
        }
    }
}
