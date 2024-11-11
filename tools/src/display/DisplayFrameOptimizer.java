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
    //private static final boolean DEBUG = false;


    private int[] nextOptimizedCharacters = { 0, 0, 0 };

    //private int frameNumber;


    /**
     * Returns a visually equivalent version of the given target display,
     * starting from a given reference display.
     */
    public Display optimizeDisplay(Display referenceDisplay,
                                   Display targetDisplay)
    {
        Display optimizedDisplay = new Display();

        for (int sectionIndex = 0; sectionIndex < Display.SECTION_COUNT; sectionIndex++)
        {
            optimizeSection(sectionIndex,
                            referenceDisplay.section[sectionIndex],
                            targetDisplay.section[sectionIndex],
                            optimizedDisplay.section[sectionIndex]);
        }

        //if (DEBUG)
        //{
        //    try (ImageDisplayOutputStream displayOutputStream =
        //             new ImageDisplayOutputStream(
        //             new BufferedOutputStream(
        //             new FileOutputStream("/tmp/frame" + frameNumber + ".0.png"))))
        //    {
        //        // Write the original display.
        //        displayOutputStream.writeFrame(targetDisplay);
        //    }
        //    catch (IOException e)
        //    {
        //        e.printStackTrace();
        //    }
        //
        //    try (ImageDisplayOutputStream displayOutputStream =
        //             new ImageDisplayOutputStream(
        //             new BufferedOutputStream(
        //             new FileOutputStream("/tmp/frame" + frameNumber + ".1.png"))))
        //    {
        //        // Write the display with the updated patterns and colors,
        //        // but with the original screen table (first vsync).
        //        Display h = new Display();
        //        h.section[1].screenImageTable = referenceDisplay.section[1].screenImageTable;
        //        h.section[1].patternTable     = optimizedDisplay.section[1].patternTable;
        //        h.section[1].colorTable       = optimizedDisplay.section[1].colorTable;
        //        displayOutputStream.writeFrame(h);
        //    }
        //    catch (IOException e)
        //    {
        //        e.printStackTrace();
        //    }
        //
        //    try (ImageDisplayOutputStream displayOutputStream =
        //             new ImageDisplayOutputStream(
        //             new BufferedOutputStream(
        //             new FileOutputStream("/tmp/frame" + frameNumber + ".2.png"))))
        //    {
        //        // Write the completely updated display (second vsync).
        //        displayOutputStream.writeFrame(optimizedDisplay);
        //    }
        //    catch (IOException e)
        //    {
        //        e.printStackTrace();
        //    }
        //    frameNumber++;
        //}

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
        HashMap<CharacterDefinition, Byte> definitionCharacterMap = new HashMap<>();

        for (int character = 0; character < referencePatternTable.length; character++)
        {
            long pattern = referencePatternTable[character];
            long colors  = referenceColorTable[character];

            definitionCharacterMap.put(new CharacterDefinition(pattern, colors),
                                       (byte)character);
        }

        // Collect and mark the characters used in the reference display.
        // We collect them again to make sure they get precedence.
        // We'll try not to overwrite their definitions.
        // Also collect characters that are used more than once in the screen
        // image table.
        BitSet softProtectedCharacters = new BitSet(256);
        BitSet repeatedCharacters      = new BitSet(256);

        //softProtectedCharacters.set(0);
        //softProtectedCharacters.set(1);

        for (int index = 0; index < referenceScreenImageTable.length; index++)
        {
            int  character = referenceScreenImageTable[index] & 0xff;
            long pattern   = referencePatternTable[character];
            long colors    = referenceColorTable[character];

            definitionCharacterMap.put(new CharacterDefinition(pattern, colors),
                                       (byte)character);

            if (softProtectedCharacters.get(character))
            {
                repeatedCharacters.set(character);
            }

            softProtectedCharacters.set(character);
        }

        // Mark the characters whose patterns and colors are recurring in the
        // target display. We'll never overwrite their definitions.
        BitSet hardProtectedCharacters = new BitSet(256);

        for (int index = 0; index < targetScreenImageTable.length; index++)
        {
            int  targetCharacter = targetScreenImageTable[index] & 0xff;
            long pattern         = targetPatternTable[targetCharacter];
            long colors          = targetColorTable[targetCharacter];

            CharacterDefinition characterDefinition =
                new CharacterDefinition(pattern, colors);

            Byte c = definitionCharacterMap.get(characterDefinition);
            if (c != null)
            {
                int character = c & 0xff;

                softProtectedCharacters.set(character);
                hardProtectedCharacters.set(character);
            }
        }

        // The pattern table and the color table start out the same.
        System.arraycopy(referencePatternTable, 0,
                         optimizedPatternTable, 0,
                         optimizedPatternTable.length);
        System.arraycopy(referenceColorTable, 0,
                         optimizedColorTable, 0,
                         optimizedColorTable.length);

        BitSet screenImageTableMask = new BitSet(targetScreenImageTable.length);

        // Apply suitable strategies to repeated characters.
        // We'd like to avoid them being redefined, if we don't have sufficient
        // unused characters, so we try to get them unused characters first.
        for (CharacterPicker strategy : new CharacterPicker[]
        {
            // Avoid characters from the reference screen image table.
            new SameColorCharacterPicker(referenceColorTable, softProtectedCharacters),
            new UnusedCharacterPicker(softProtectedCharacters),
        })
        {
            // Remap the characters, patterns, and colors in the target display.
            pickCharacters(sectionIndex,
                           referenceScreenImageTable,
                           referencePatternTable,
                           referenceColorTable,
                           targetScreenImageTable,
                           targetPatternTable,
                           targetColorTable,
                           optimizedScreenImageTable,
                           optimizedPatternTable,
                           optimizedColorTable,
                           definitionCharacterMap,
                           softProtectedCharacters,
                           hardProtectedCharacters,
                           screenImageTableMask,
                           repeatedCharacters,
                           strategy);
        }

        // Apply suitable strategies to all remaining characters.
        for (CharacterPicker strategy : new CharacterPicker[]
        {
            // Avoid characters from the reference screen image table.
            new SameColorCharacterPicker(referenceColorTable, softProtectedCharacters),
            new UnusedCharacterPicker(softProtectedCharacters),

            // We may not have found sufficient characters to use.
            // Just avoid characters from the resulting screen image table.
            new SameCharacterPicker(referenceScreenImageTable, hardProtectedCharacters),
            new UnusedCharacterPicker(hardProtectedCharacters),
        })
        {
            // Remap the characters, patterns, and colors in the target display.
            pickCharacters(sectionIndex,
                           referenceScreenImageTable,
                           referencePatternTable,
                           referenceColorTable,
                           targetScreenImageTable,
                           targetPatternTable,
                           targetColorTable,
                           optimizedScreenImageTable,
                           optimizedPatternTable,
                           optimizedColorTable,
                           definitionCharacterMap,
                           softProtectedCharacters,
                           hardProtectedCharacters,
                           screenImageTableMask,
                           null,
                           strategy);
        }
    }


    private void pickCharacters(int                                sectionIndex,
                                byte[]                             referenceScreenImageTable,
                                long[]                             referenceColorTable,
                                long[]                             referencePatternTable,
                                byte[]                             targetScreenImageTable,
                                long[]                             targetPatternTable,
                                long[]                             targetColorTable,
                                byte[]                             optimizedScreenImageTable,
                                long[]                             optimizedPatternTable,
                                long[]                             optimizedColorTable,
                                HashMap<CharacterDefinition, Byte> definitionCharacterMap,
                                BitSet                             softProtectedCharacters,
                                BitSet                             hardProtectedCharacters,
                                BitSet                             screenImageTableMask,
                                BitSet                             referenceCharacterMask,
                                CharacterPicker                    characterPicker)
    {
        // Do we have any characters that still need to be picked?
        if (screenImageTableMask.cardinality() < 256)
        {
            // Try to pick all characters that haven't been picked yet.
            for (int index = 0; index < targetScreenImageTable.length; index++)
            {
                if (!screenImageTableMask.get(index) &&
                    (referenceCharacterMask == null ||
                     referenceCharacterMask.get(referenceScreenImageTable[index] & 0xff)))
                {
                    int  targetCharacter = targetScreenImageTable[index] & 0xff;
                    long pattern         = targetPatternTable[targetCharacter];
                    long colors          = targetColorTable[targetCharacter];

                    int optimizedCharacter;

                    CharacterDefinition characterDefinition =
                        new CharacterDefinition(pattern, colors);

                    // Is the character definition present?
                    Byte c = definitionCharacterMap.get(characterDefinition);
                    if (c != null)
                    {
                        // Reuse the character and the definition.
                        optimizedCharacter = c & 0xff;
                    }
                    else
                    {
                        // Try to find a character that is completely unused.
                        // We'll look sequentially.
                        int nextOptimizedCharacter =
                            nextOptimizedCharacters[sectionIndex];

                        optimizedCharacter =
                            characterPicker.pickCharacter(index,
                                                          characterDefinition,
                                                          nextOptimizedCharacter);

                        // Did we find a completely unused character?
                        if (optimizedCharacter > 255)
                        {
                            // There aren't any unused characters left.
                            // We'll find a solution later.
                            continue;
                        }

                        nextOptimizedCharacters[sectionIndex] =
                            optimizedCharacter + 1;

                        optimizedPatternTable[optimizedCharacter] = pattern;
                        optimizedColorTable[optimizedCharacter]   = colors;

                        definitionCharacterMap.put(characterDefinition,
                                                   (byte)optimizedCharacter);
                    }

                    // Store the resulting character.
                    optimizedScreenImageTable[index] = (byte)optimizedCharacter;

                    // Avoid redefining or using it again.
                    softProtectedCharacters.set(optimizedCharacter);
                    hardProtectedCharacters.set(optimizedCharacter);
                    screenImageTableMask.set(index);
                }
            }
        }
    }


    /**
     * This interface provides a method to pick a character number for a given
     * definition. Different implementations can provide different strategies.
     */
    private interface CharacterPicker
    {
        /**
         * Returns a character number for a given definition.
         * @param index               the index in the screen image table.
         * @param characterDefinition the required definition.
         * @param suggestedCharacter  a suggested character number (following
         *                            the previous number).
         * @return the character number if one was found, or 256 otherwise.
         */
        public int pickCharacter(int                 index,
                                 CharacterDefinition characterDefinition,
                                 int                 suggestedCharacter);
    }


    /**
     * This CharacterPicker returns unused characters, if it can find any
     * that are not protected from being used.
     */
    private static class UnusedCharacterPicker
    implements           CharacterPicker
    {
        private final BitSet protectedCharacters;


        public UnusedCharacterPicker(BitSet protectedCharacters)
        {
            this.protectedCharacters = protectedCharacters;
        }


        // Implementations for CharacterPicker.

        public int pickCharacter(int                 index,
                                 CharacterDefinition characterDefinition,
                                 int                 suggestedCharacter)
        {
            // Try to find a character that is completely unused.
            // We'll look sequentially.
            return findClearBit(protectedCharacters,
                                suggestedCharacter);
        }
    }


    /**
     * This CharacterPicker returns characters that have the same colors,
     * if it can find any that are not protected from being used.
     */
    private static class SameColorCharacterPicker
    implements           CharacterPicker
    {
        private final long[] referenceColorTable;
        private final BitSet protectedCharacters;


        public SameColorCharacterPicker(long[] referenceColorTable,
                                        BitSet protectedCharacters)
        {
            this.referenceColorTable = referenceColorTable;
            this.protectedCharacters = protectedCharacters;
        }


        // Implementations for CharacterPicker.

        public int pickCharacter(int                 index,
                                 CharacterDefinition characterDefinition,
                                 int                 suggestedCharacter)
        {
            // Try to find a character that is completely unused.
            // We'll look sequentially.
            int firstUnusedCharacter = findClearBit(protectedCharacters,
                                                    suggestedCharacter);
            if (firstUnusedCharacter > 255)
            {
                return 256;
            }

            int unusedCharacter = firstUnusedCharacter;
            while (true)
            {
                if (referenceColorTable[unusedCharacter] == characterDefinition.colors)
                {
                    return unusedCharacter;
                }

                unusedCharacter = findClearBit(protectedCharacters,
                                               unusedCharacter + 1);

                if (unusedCharacter == firstUnusedCharacter)
                {
                    return 256;
                }
            }
        }
    }


    /**
     * This CharacterPicker returns characters from the reference screen image
     * table, if they have the same colors and aren't protected from being
     * used.
     */
    private static class SameColorSameCharacterPicker
    implements           CharacterPicker
    {
        private final byte[] referenceScreenImageTable;
        private final long[] referenceColorTable;
        private final BitSet protectedCharacters;


        public SameColorSameCharacterPicker(byte[] referenceScreenImageTable,
                                            long[] referenceColorTable,
                                            BitSet protectedCharacters)
        {
            this.referenceScreenImageTable = referenceScreenImageTable;
            this.referenceColorTable       = referenceColorTable;
            this.protectedCharacters       = protectedCharacters;
        }


        // Implementations for CharacterPicker.

        public int pickCharacter(int                 index,
                                 CharacterDefinition characterDefinition,
                                 int                 suggestedCharacter)
        {
            int referenceCharacter = referenceScreenImageTable[index] & 0xff;

            return
                !protectedCharacters.get(referenceCharacter) &&
                referenceColorTable[referenceCharacter] == characterDefinition.colors ?
                    referenceCharacter :
                    256;
        }
    }


    /**
     * This CharacterPicker returns characters from the reference screen image
     * table, if they aren't protected from being used.
     */
    private static class SameCharacterPicker
    implements           CharacterPicker
    {
        private final byte[] referenceScreenImageTable;
        private final BitSet protectedCharacters;


        public SameCharacterPicker(byte[] referenceScreenImageTable,
                                   BitSet protectedCharacters)
        {
            this.referenceScreenImageTable = referenceScreenImageTable;
            this.protectedCharacters       = protectedCharacters;
        }


        // Implementations for CharacterPicker.

        public int pickCharacter(int                 index,
                                 CharacterDefinition characterDefinition,
                                 int                 suggestedCharacter)
        {
            int referenceCharacter = referenceScreenImageTable[index] & 0xff;

            return
                !protectedCharacters.get(referenceCharacter) ?
                    referenceCharacter :
                    256;
        }
    }


    /**
     * Returns the first clear bit from the given bit set, starting from
     * the given index, wrapping around if necessary.
     */
    private static int findClearBit(BitSet bitSet, int preferredFromIndex)
    {
        int optimizedCharacter = bitSet.nextClearBit(preferredFromIndex);
        if (optimizedCharacter >= bitSet.length())
        {
            optimizedCharacter = bitSet.nextClearBit(0);
        }

        return optimizedCharacter;
    }


    /**
     * Prints out the optimized display characters of the specified ZIP/PBM/PNG
     * files.
     */
    public static void main(String[] args)
    {
        DisplayFrameOptimizer optimizer =
            new DisplayFrameOptimizer();

        Display referenceDisplay = new Display();

        for (int argIndex = 0; argIndex < args.length; argIndex++)
        {
            String inputImageFileName  = args[argIndex];

            try (DisplayInput dsplayInputStream =
                     inputImageFileName.endsWith(".zip") ? new ZipDisplayInputStream(
                                                           new BufferedInputStream(
                                                           new FileInputStream(inputImageFileName))) :

                     inputImageFileName.endsWith(".pbm") ? new PbmDisplayInputStream(
                                                           new BufferedInputStream(
                                                           new FileInputStream(inputImageFileName))) :

                                                           new ImageDisplayInputStream(
                                                           new BufferedInputStream(
                                                           new FileInputStream(inputImageFileName))))
            {
                Display targetDisplay =
                    dsplayInputStream.readFrame();

                Display optimizedDisplay =
                    optimizer.optimizeDisplay(referenceDisplay,
                                              targetDisplay);

                for (int sectionIndex = 0; sectionIndex < Display.SECTION_COUNT; sectionIndex++)
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
