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

import video.VideoOutputStream;

import java.io.*;
import java.util.Arrays;
import java.util.zip.*;

/**
 * This output stream computes and writes incremental differences between
 * subsequent {@link Display} instances to a given video output stream.
 */
public class DisplayDeltaOutputStream
implements   AutoCloseable
{
    private final VideoOutputStream videoOutputStream;

    private final DisplayFrameOptimizer optimizer = new DisplayFrameOptimizer();

    private Display referenceDisplay = new Display();
    private Display targetDisplay;


    /**
     * Creates a new instance that writes its incremental differences to
     * the given video output stream.
     */
    public DisplayDeltaOutputStream(VideoOutputStream videoOutputStream)
    {
        this.videoOutputStream = videoOutputStream;
    }


    /**
     * Returns whether the instance is ready to write the first part of the
     * display update, that is, if the second part of the previous update has
     * been written.
     */
    public boolean readyToWriteDisplayDelta1()
    {
        return targetDisplay == null;
    }


    /**
     * Writes the first part of the display update.
     */
    public void writeDisplayDelta1(Display display)
    throws IOException
    {
        targetDisplay =
            optimizer.optimizeDisplay(referenceDisplay,
                                      display);

        videoOutputStream.writeComment("* Delta frame.");

        // Write the changed contents of the pattern table and the color
        // table (at 25 fps).
        for (int sectionIndex = 0; sectionIndex < Display.SECTION_COUNT; sectionIndex++)
        {
            Display.Section referenceSection = referenceDisplay.section[sectionIndex];
            Display.Section targetSection    = targetDisplay.section[sectionIndex];

            writeDeltaSpans(0x0000 + sectionIndex * 0x0800,
                            referenceSection.patternTable,
                            targetSection.patternTable);

            writeDeltaSpans(0x2000 + sectionIndex * 0x0800,
                            referenceSection.colorTable,
                            targetSection.colorTable);
        }
    }


    /**
     * Writes the second part of the display update.
     */
    public void writeDisplayDelta2()
    throws IOException
    {
        // Write the changed characters of the screen image table (at 25 fps).
        for (int sectionIndex = 0; sectionIndex < Display.SECTION_COUNT; sectionIndex++)
        {
            Display.Section referenceSection = referenceDisplay.section[sectionIndex];
            Display.Section targetSection    = targetDisplay.section[sectionIndex];

            writeDeltaSpans(0x1800 + sectionIndex * 0x0100,
                            referenceSection.screenImageTable,
                            targetSection.screenImageTable);
        }

        // Update the displays.
        referenceDisplay = targetDisplay;
        targetDisplay    = null;
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        videoOutputStream.close();
    }


    // Small utility methods.

    private void writeDeltaSpans(int    sectionOffset,
                                 long[] referenceTable,
                                 long[] targetTable)
    throws IOException
    {
        // Find and write different spans.
        int startCharacter;
        int endCharacter = 0;

        while ((startCharacter = findDifferent(referenceTable,
                                               targetTable,
                                               endCharacter)) <
               targetTable.length)
        {
            endCharacter = findSame(referenceTable,
                                    targetTable,
                                    startCharacter);

            videoOutputStream.startDisplayFragment(sectionOffset +
                                                   startCharacter * 8,
                                                   (endCharacter - startCharacter) * 8);
            videoOutputStream.writeComment("");

            for (int character = startCharacter; character < endCharacter; character++)
            {
                videoOutputStream.writeDisplayData(targetTable[character]);
            }

            videoOutputStream.endDisplayFragment();
        }
    }


    private void writeDeltaSpans(int    sectionOffset,
                                 byte[] referenceTable,
                                 byte[] targetTable)
    throws IOException
    {
        // Find and write different spans.
        int startIndex;
        int endIndex = 0;

        while ((startIndex = findDifferent(referenceTable,
                                           targetTable,
                                           endIndex)) <
               targetTable.length)
        {
            endIndex = findSame(referenceTable,
                                targetTable,
                                startIndex,
                                5);

            videoOutputStream.startDisplayFragment(sectionOffset + startIndex,
                                                   endIndex - startIndex);
            videoOutputStream.writeComment0(", >");

            for (int index = startIndex; index < endIndex; index++)
            {
                videoOutputStream.writeDisplayData(targetTable[index]);
            }

            videoOutputStream.endDisplayFragment();
            videoOutputStream.writeComment("");
        }
    }


    private int find(long[] array, long value, int index)
    {
        while (index < array.length &&
               array[index] != value)
        {
            index++;
        }

        return index;
    }


    private int findDifferent(long[] array, long value, int index)
    {
        while (index < array.length &&
               array[index] == value)
        {
            index++;
        }

        return index;
    }


    private int findSame(long[] array1, long[] array2, int index)
    {
        while (index < array1.length &&
               array1[index] != array2[index])
        {
            index++;
        }

        return index;
    }


    private int findDifferent(long[] array1, long[] array2, int index)
    {
        while (index < array1.length &&
               array1[index] == array2[index])
        {
            index++;
        }

        return index;
    }


    private int findSame(byte[] array1, byte[] array2, int index)
    {
        while (index < array1.length &&
               array1[index] != array2[index])
        {
            index++;
        }

        return index;
    }


    private int findSame(byte[] array1, byte[] array2, int index, int minimumCount)
    {
        while (index < array1.length &&
               !Arrays.equals(array1, index, Math.min(index + minimumCount, array1.length),
                              array2, index, Math.min(index + minimumCount, array2.length)))
        {
            index++;
        }

        return index;
    }


    private int findDifferent(byte[] array1, byte[] array2, int index)
    {
        while (index < array1.length &&
               array1[index] == array2[index])
        {
            index++;
        }

        return index;
    }
}
