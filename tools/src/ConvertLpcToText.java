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
import speech.*;

import java.io.*;

/**
 * This utility converts a binary file in Linear Predictive Coding (LPC) format
 * to a simple LPC text format.
 *
 * Usage:
 *     java ConvertLpcToText input.lpc output.txt
 *
 * @see LpcFrame
 */
public class ConvertLpcToText
{
    public static void main(String[] args)
    throws IOException
    {
        int argIndex = 0;

        String inputLpcFileName   = args[argIndex++];
        String outputTextFileName = args[argIndex++];

        try (LpcFrameInput lpcFrameInput =
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputLpcFileName))))
        {
            try (LpcFrameWriter lpcFrameWriter =
                     new LpcFrameWriter(
                     new BufferedWriter(
                     new FileWriter(outputTextFileName))))
            {
                while (true)
                {
                    LpcFrame lpcFrame =
                        lpcFrameInput.readFrame();

                    if (lpcFrame == null)
                    {
                        break;
                    }

                    lpcFrameWriter.writeFrame(lpcFrame);
                }
            }
        }
    }
}
