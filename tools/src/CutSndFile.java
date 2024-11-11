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
import sound.*;

import java.io.*;

/**
 * This utility copies a section from a file in the custom TMS9919 / SN76489
 * Sound (.snd) format.
 *
 * Usage:
 *     java CutSndFile input.snd start_frame stop_frame output.snd
 *
 * where
 *     start_frame is the start frame in the input file.
 *     stop_frame  is the end frame in the input file.
 */
public class CutSndFile
{
    public static void main(String[] args)
    throws IOException
    {
        String inputSndFileName  = args[0];
        int    startFrame        = Integer.parseInt(args[1]);
        int    stopFrame         = Integer.parseInt(args[2]);
        String outputSndFileName = args[3];

        try (SoundCommandInputStream sndCommandInputStream =
                 new SoundCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputSndFileName))))
        {
            try (SoundCommandOutput soundCommandOutput =
                     new SndCommandOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(outputSndFileName))))
            {
                sndCommandInputStream.skipFrames(startFrame);

                for (int counter = startFrame; counter < stopFrame; counter++)
                {
                    SoundCommand[] frame =
                        sndCommandInputStream.readFrame();

                    if (frame == null)
                    {
                        break;
                    }

                    soundCommandOutput.writeFrame(frame);
                }
            }
        }
    }
}
