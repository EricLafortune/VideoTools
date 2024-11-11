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

import java.io.*;
import java.util.zip.*;

/**
 * This DisplayInput reads its subsequent frames from an archive stream in
 * ZIP format that contains image files (PBM, PNG, GIF,...) of 256x192 pixels.
 */
public class ZipDisplayInputStream
implements   DisplayInput
{
    private final ZipInputStream zipInputStream;


    /**
     * Creates a new instance that reads its frames from the given input
     * stream.
     */
    public ZipDisplayInputStream(InputStream inputStream)
    {
        zipInputStream = new ZipInputStream(inputStream);
    }


    // Implementations for DisplayInput.

    public Display readFrame() throws IOException
    {
        // Can we get another frame?
        ZipEntry entry = zipInputStream.getNextEntry();
        if (entry == null)
        {
            return null;
        }

        try
        {
            try
            {
                // Delegate decoding and returning the frame to a display
                // input stream that is suitable for the given zip entry.
                return entry.getName().endsWith(".pbm") ?
                    new PbmDisplayInputStream(zipInputStream).readFrame() :
                    new ImageDisplayInputStream(zipInputStream).readFrame();
            }
            catch (IOException e)
            {
                throw new IOException("Can't decode image ["+entry.getName()+"]", e);
            }
        }
        finally
        {
            zipInputStream.closeEntry();
        }
    }


    public void skipFrame() throws IOException
    {
        zipInputStream.getNextEntry();
        zipInputStream.closeEntry();
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        zipInputStream.close();
    }
}
