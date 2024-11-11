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
package util;

import java.io.IOException;

/**
 * This interface provides an input stream of frames. Frames are typically
 * raw or decoded sound frames, speech frames or display frames that can be
 * played back at a certain pace, for example 60 fps.
 */
public interface FrameInput<T>
extends          AutoCloseable
{
    /**
     * Returns the next frame in the input stream.
     */
    public T readFrame() throws IOException;


    /**
     * Skips a frame.
     */
    public default void skipFrame() throws IOException
    {
        readFrame();
    }


    /**
     * Skips the given number of frames.
     */
    public default void skipFrames(int count) throws IOException
    {
        for (int counter = 0; counter < count; counter++)
        {
            skipFrame();
        }
    }


    // Specialization for AutoCloseable.

    public void close() throws IOException;
}
