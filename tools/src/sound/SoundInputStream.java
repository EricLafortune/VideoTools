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
package sound;

import java.io.*;

/**
 * This interface provides an input stream for data frames for the
 * TMS9919 / SN76489 sound processor.
 */
public interface SoundInputStream
extends          AutoCloseable
{
    /**
     * Returns the next frame in the input stream. Each frame is a sequence
     * of bytes that can be sent to the sound processor.
     */
    public byte[] readFrame() throws IOException;

    /**
     * Skips a frame.
     */
    public void skipFrame() throws IOException;

    /**
     * Skips the given number of frames.
     */
    public void skipFrames(int count) throws IOException;


    // Refinement for AutoCloseable.

    public void close() throws IOException;
}
