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
package video;

import java.io.*;

/**
 * This VideoOutputStream writes its output in source form for the xas99
 * assembler.
 */
public class TextVideoOutputStream
implements   VideoOutputStream
{
    private final PrintStream printStream;
    private final int         bankSize;

    private int vsyncCount;
    private int bankCount;
    private int bankByteCount;


    public TextVideoOutputStream(PrintStream printStream, int bankSize)
    {
        this.printStream = printStream;
        this.bankSize    = bankSize;
    }


    // Implementations for AnimationOutputStream.

    public int getVsyncCount()
    {
        return vsyncCount;
    }


    public int getBankCount()
    {
        return bankCount;
    }


    public int getBankByteCount()
    {
        return bankByteCount;
    }


    public void writeComment0(String comment)
    throws IOException
    {
        printStream.print(comment);
    }


    public void writeComment(String comment)
    throws IOException
    {
        printStream.println(comment);
    }


    public void startDisplayFragment(int address, int size)
    throws IOException
    {
        checkBank(4 + size);

        printStream.print(String.format("    text >%04x, >%04x",
                                        swapBytes(size),
                                        swapBytes(0x4000 | address)));

        bankByteCount += 4 + size;
    }


    public void writeDisplayData(byte data)
    throws IOException
    {
        printStream.print(String.format("%02x", data));
    }


    public void writeDisplayData(long data)
    throws IOException
    {
        printStream.println(String.format("    text >%016x", data));
    }


    public void endDisplayFragment()
    throws IOException
    {
    }

    public void writeSoundData(byte[] soundData)
    throws IOException
    {
        if (soundData != null)
        {
            int size = soundData.length;
            if (size > 0)
            {
                checkBank(2 + size);

                printStream.print(String.format("    text >%04x, >",
                                                swapBytes(size + SOUND_SIZE_DELTA)));

                for (int index = 0; index < size; index++)
                {
                    printStream.print(String.format("%02x", soundData[index]));
                }

                printStream.println();

                bankByteCount += 2 + size;
            }
        }
    }


    public void writeSpeechData(byte[] speechData)
    throws IOException
    {
        if (speechData != null)
        {
            int size = speechData.length;
            if (size > 0)
            {
                checkBank(2 + size);

                printStream.print(String.format("    text >%04x, >",
                                                swapBytes(size + SPEECH_SIZE_DELTA)));

                for (int index = 0; index < size; index++)
                {
                    printStream.print(String.format("%02x", speechData[index]));
                }

                printStream.println();

                bankByteCount += 2 + size;
            }
        }
    }


    public void writeVSync()
    throws IOException
    {
        checkBank(2);

        writeMarker(VSYNC);

        vsyncCount++;
    }


    // Implementation for AutoCloseable.

    public void close()
    throws IOException
    {
        checkBank(2);

        writeMarker(EOF);

        printStream.close();
    }


    // Small utility methods.

    private void checkBank(int requiredByteCount)
    {
        if (bankByteCount > bankSize - requiredByteCount - 2)
        {
            writeMarker(NEXT_BANK);

            printStream.println(String.format("    bss  %d", bankSize - bankByteCount));
            printStream.println("* New memory bank.");

            bankCount++;
            bankByteCount = 0;
        }
    }


    private void writeMarker(short marker)
    {
        printStream.println(String.format("    text >%04x", swapBytes(marker)));

        bankByteCount += 2;
    }


    private int swapBytes(int word)
    {
        return ((word & 0x00ff) << 8) |
               ((word & 0xff00) >> 8);
    }
}
