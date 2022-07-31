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
package video;

import java.io.*;

/**
 * This VideoOutputStream writes its output in binary form.
 */
public class BinaryVideoOutputStream
implements   VideoOutputStream
{
    private final OutputStream outputStream;
    private final int          bankSize;

    private int vsyncCount;
    private int bankCount;
    private int bankByteCount;


    public BinaryVideoOutputStream(OutputStream outputStream, int bankSize)
    {
        this.outputStream = outputStream;
        this.bankSize     = bankSize;
    }


    // Implementations for VideoOutputStream.

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
        // The binary format doesn't contain any comments.
    }


    public void writeComment(String comment)
    throws IOException
    {
        // The binary format doesn't contain any comments.
    }


    public void startDisplayFragment(int address, int size)
    throws IOException
    {
        checkBank(4 + size);

        writeWord(swapBytes(size));
        writeSwappedWord(0x4000 | address);

        bankByteCount += 4 + size;
    }


    public void writeDisplayData(byte data)
    throws IOException
    {
        outputStream.write(data);
    }


    public void writeDisplayData(long data)
    throws IOException
    {
        for (int counter = 0; counter < 8; counter++)
        {
            outputStream.write((int)(data >>> 56 - counter * 8));
        }
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

                writeSwappedWord(size + SOUND_SIZE_DELTA);

                outputStream.write(soundData);

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

                writeSwappedWord(size + SPEECH_SIZE_DELTA);

                outputStream.write(speechData);

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

        outputStream.close();
    }


    // Small utility methods.

    private void checkBank(int requiredByteCount)
    throws IOException
    {
        if (bankByteCount > bankSize - requiredByteCount - 2)
        {
            writeMarker(NEXT_BANK);

            outputStream.write(new byte[bankSize - bankByteCount]);

            bankCount++;
            bankByteCount = 0;
        }
    }


    private void writeMarker(short marker)
    throws IOException
    {
        writeSwappedWord(marker);

        bankByteCount += 2;
    }


    private void writeSwappedWord(int word)
    throws IOException
    {
        writeWord(swapBytes(word));
    }


    private void writeWord(int word)
    throws IOException
    {
        outputStream.write(word >>> 8);
        outputStream.write(word);
    }


    private int swapBytes(int word)
    {
        return ((word & 0x00ff) << 8) |
               ((word & 0xff00) >> 8);
    }
}
