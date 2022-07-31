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
package display;

import java.io.IOException;

/**
 * This interface provides an input stream for {@link Display} objects.
 */
public interface DisplayInputStream extends AutoCloseable
{
    /**
     * Returns the next frame in the input stream.
     */
    public Display readFrame() throws IOException;

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
