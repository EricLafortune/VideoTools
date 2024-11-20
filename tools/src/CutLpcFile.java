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
 * This utility copies a section from a file in Linear Predictive Coding
 * (LPC) format for the TMS5200 speech synthesizer.
 *
 * Usage:
 *     java CutLpcFile [-addstopframe] input.lpc start_frame stop_frame output.lpc
 *
 * where
 *     start_frame is the start frame in the input file.
 *     stop_frame  is the end frame in the input file.
 *
 * @see LpcFrame
 */
public class CutLpcFile
{
    public static void main(String[] args)
    throws IOException
    {
        int argIndex = 0;

        boolean addStopFrame = false;

        while (true)
        {
            String arg = args[argIndex];
            if (!arg.startsWith("-"))
            {
                break;
            }

            if (arg.equals("-addstopframe"))
            {
                addStopFrame = true;
            }
            else
            {
                throw new IllegalArgumentException("Unknown option ["+arg+"]");
            }

            argIndex++;
        }

        String inputLpcFileName  = args[argIndex++];
        int    startFrame        = Integer.parseInt(args[argIndex++]);
        int    stopFrame         = Integer.parseInt(args[argIndex++]);
        String outputLpcFileName = args[argIndex++];

        try (LpcFrameInput lpcFrameInput =
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputLpcFileName))))
        {
            try (LpcFrameOutputStream lpcFrameOutputStream =
                     new LpcFrameOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(outputLpcFileName))))
            {
                lpcFrameInput.skipFrames(startFrame);

                for (int counter = startFrame; counter < stopFrame; counter++)
                {
                    LpcFrame lpcFrame =
                        lpcFrameInput.readFrame();

                    if (lpcFrame == null)
                    {
                        break;
                    }

                    lpcFrameOutputStream.writeFrame(lpcFrame);
                }

                if (addStopFrame)
                {
                    lpcFrameOutputStream.writeFrame(new LpcStopFrame());
                }
            }
        }
    }
}
