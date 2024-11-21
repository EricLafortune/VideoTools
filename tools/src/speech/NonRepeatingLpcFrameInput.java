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
 * This LpcFrameInput replaces repeat frames from its delegate by the intended
 * non-repeating frames.
 *
 * @see LpcFrame
 */
public class NonRepeatingLpcFrameInput
implements   LpcFrameInput
{
    private final LpcFrameInput lpcFrameInput;

    private LpcFrame previousFrame;


    /**
     * Creates a new instance that gets its input from the given delegate.
     */
    public NonRepeatingLpcFrameInput(LpcFrameInput lpcFrameInput)
    {
        this.lpcFrameInput = lpcFrameInput;
    }


    // Implementations for LpcFrameInput.

    public LpcFrame readFrame()
    throws IOException
    {
        LpcFrame frame = lpcFrameInput.readFrame();
        if (frame instanceof LpcRepeatFrame)
        {
            LpcRepeatFrame repeatFrame = (LpcRepeatFrame)frame;

            // Replace the repeat frame by the most recent non-repeating frame.
            frame = previousFrame.clone();

            // Copy over the energy of the repeat frame.
            if (frame instanceof LpcEnergyFrame)
            {
                LpcEnergyFrame energyFrame = (LpcEnergyFrame)frame;
                energyFrame.energy = repeatFrame.energy;

                // Copy over the pitch of the repeat frame.
                if (frame instanceof LpcPitchFrame)
                {
                    LpcPitchFrame pitchFrame = (LpcPitchFrame)frame;
                    pitchFrame.pitch = repeatFrame.pitch;
                }
            }
        }
        else
        {
            // Remember the non-repeating frame.
            previousFrame = frame;
        }

        return frame;
    }


    public void skipFrame()
    throws IOException
    {
        lpcFrameInput.skipFrame();
    }


    public void skipFrames(int count)
    throws IOException
    {
        lpcFrameInput.skipFrames(count);
    }


    // Implementation for Closeable.

    public void close()
    throws IOException
    {
        lpcFrameInput.close();
    }


    /**
     * Prints out the non-repeating frames of the specified LPC file.
     */
    public static void main(String[] args)
    throws IOException
    {
        String inputFileName = args[0];

        try (LpcFrameInput lpcFrameInput =
                 new NonRepeatingLpcFrameInput(
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputFileName)))))
        {
            int counter = 0;

            LpcFrame frame;
            while ((frame = lpcFrameInput.readFrame()) != null)
            {
                System.out.printf("#%03d (%.3f): %s%n",
                                  counter,
                                  counter*0.025,
                                  frame.toString());

                counter++;
            }
        }
    }
}
