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
 * This LpcFrameOutput sends frames to its delegate, replacing frames
 * by repeat frames whenever possible.
 *
 * @see LpcFrame
 */
public class RepeatingLpcFrameOutput
implements   LpcFrameOutput
{
    private static final int UNVOICED = 0x00;


    private final LpcFrameOutput lpcFrameOutput;

    private LpcFrame previousFrame;


    /**
     * Creates a new instance that sends its output to the given delegate.
     */
    public RepeatingLpcFrameOutput(LpcFrameOutput lpcFrameOutput)
    {
        this.lpcFrameOutput = lpcFrameOutput;
    }


    // Implementations for LpcFrameOutput.

    public void writeFrame(LpcFrame frame)
    throws IOException
    {
        // Copy over the energy and pitch of the repeat frame.
        if (!(frame instanceof LpcRepeatFrame))
        {
            if (previousFrame != null &&
                frame.getClass().equals(previousFrame.getClass()))
            {
                // Do the unvoiced frames have the same coefficients?
                if (frame instanceof LpcUnvoicedFrame &&
                    ((LpcUnvoicedFrame)frame).k == ((LpcUnvoicedFrame)previousFrame).k)
                {
                    // Replace the unvoiced frame.
                    frame = new LpcRepeatFrame(((LpcEnergyFrame)frame).energy,
                                               UNVOICED);
                }
                // Do the voiced frames have the same coefficients?
                else if (frame instanceof LpcVoicedFrame &&
                         ((LpcVoicedFrame)frame).k == ((LpcVoicedFrame)previousFrame).k)
                {
                    // Replace the voiced frame.
                    frame = new LpcRepeatFrame(((LpcEnergyFrame)frame).energy,
                                               ((LpcVoicedFrame)frame).pitch);
                }
                else
                {
                    // Remember the non-repeating frame.
                    previousFrame = frame;
                }
            }
            else
            {
                // Remember the non-repeating frame.
                previousFrame = frame;
            }
        }

        lpcFrameOutput.writeFrame(frame);
    }


    // Implementation for Closeable.

    public void close()
    throws IOException
    {
        lpcFrameOutput.close();
    }
}
