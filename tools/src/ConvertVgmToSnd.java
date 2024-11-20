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
import sound.*;

import java.io.*;

/**
 * This utility converts a file in Video Game Music (.vgm) format to a file
 * in our optimized TMS9919/SN76489 Sound (.snd) format.
 *
 * Usage:
 *     java ConvertVgmToSnd [frame_time] input.vgm output.snd
 */
public class ConvertVgmToSnd
{
    public static void main(String[] args)
    throws IOException
    {
        int index = 0;

        int frameTime = args.length > 2 ?
            Integer.parseInt(args[index++]) :
            VgmInputStream.FRAME_TIME_60_FPS;

        try (SoundCommandInput soundCommandInput =
                 new VgmCommandInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[index++])),
                 frameTime))
        {
            try (SoundCommandOutput soundCommandOutput =
                     new SndCommandOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(args[index++]))))
            {
                SoundCommand[] commands;
                while ((commands = soundCommandInput.readFrame()) != null)
                {
                    soundCommandOutput.writeFrame(commands);
                }
            }
        }
    }
}
