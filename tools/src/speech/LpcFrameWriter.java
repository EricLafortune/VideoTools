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
package speech;

import java.io.*;

/**
 * This class writes LPC frames to a writer in a simple text format.
 *
 * @see LpcFrame
 */
public class LpcFrameWriter
implements   LpcFrameOutput
{
    private final Writer writer;


    /**
     * Creates a new instance that writes its data to the given writer.
     */
    public LpcFrameWriter(Writer writer)
    throws IOException
    {
        this.writer = writer;
    }


    // Implementation for LpcFrameOutput.

    public void writeFrame(LpcFrame frame) throws IOException
    {
        if      (frame instanceof LpcVoicedFrame)
        {
            LpcVoicedFrame voicedFrame = (LpcVoicedFrame)frame;
            writer.write(String.format("%1x %02x %010x\n",
                                       voicedFrame.energy,
                                       voicedFrame.pitch,
                                       voicedFrame.k));
        }
        else if (frame instanceof LpcUnvoicedFrame)
        {
            LpcUnvoicedFrame unvoicedFrame = (LpcUnvoicedFrame)frame;
            writer.write(String.format("%1x %05x\n",
                                       unvoicedFrame.energy,
                                       unvoicedFrame.k));
        }
        else if (frame instanceof LpcRepeatFrame)
        {
            LpcRepeatFrame repeatFrame = (LpcRepeatFrame)frame;
            writer.write(String.format("%1x %02x\n",
                                       repeatFrame.energy,
                                       repeatFrame.pitch));
        }
        else if (frame instanceof LpcSilenceFrame)
        {
            writer.write("0\n");
        }
        else if (frame instanceof LpcStopFrame)
        {
            writer.write("f\n");
        }
        else
        {
            throw new IOException("Unsupported LPC frame type ["+frame.getClass().getName()+"]");
        }
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        writer.close();
    }


    /**
     * Copies the frames of the specified LPC file to the specified new file.
     */
    public static void main(String[] args)
    {
        try (LpcFrameInput lpcFrameInput =
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(args[0]))))
        {
            try (LpcFrameOutput lpcFrameOutput =
                     new LpcFrameWriter(
                     new BufferedWriter(
                     new FileWriter(args[1]))))
            {
                LpcFrame frame;
                while ((frame = lpcFrameInput.readFrame()) != null)
                {
                    lpcFrameOutput.writeFrame(frame);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
