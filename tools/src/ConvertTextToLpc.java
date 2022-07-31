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
import speech.*;

import java.io.*;

/**
 * This utility converts a file in a simple LPC text format to binary LPC file.
 *
 * Usage:
 *     java ConvertTextToLpc input.txt output.lpc
 *
 * @see LpcFrame
 */
public class ConvertTextToLpc
{
    public static void main(String[] args)
    throws IOException
    {
        int argIndex = 0;

        String inputTextFileName = args[argIndex++];
        String outputLpcFileName = args[argIndex++];

        try (LpcFrameReader lpcFrameReader =
                 new LpcFrameReader(
                 new BufferedReader(
                 new FileReader(inputTextFileName))))
        {
            try (LpcFrameOutputStream lpcFrameOutputStream =
                     new LpcFrameOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(outputLpcFileName))))
            {
                while (true)
                {
                    LpcFrame lpcFrame =
                        lpcFrameReader.readFrame();

                    if (lpcFrame == null)
                    {
                        break;
                    }

                    lpcFrameOutputStream.writeFrame(lpcFrame);
                }
            }
        }
    }
}
