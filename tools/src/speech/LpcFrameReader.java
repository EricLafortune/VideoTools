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
package speech;

import java.io.*;

/**
 * This LpcFrameInput parses and returns frames from a reader in a simple text
 * Linear Predictive Coding (LPC) format.
 *
 * @see LpcFrame
 */
public class LpcFrameReader
implements   LpcFrameInput
{
    private final LineNumberReader reader;


    /**
     * Creates a new instance that reads its data from the given reader.
     */
    public LpcFrameReader(Reader reader)
    {
        this.reader = new LineNumberReader(reader);
    }


    // Implementations for LpcFrameInput.

    public LpcFrame readFrame() throws IOException
    {
        while (true)
        {
            String line = reader.readLine();
            if (line == null)
            {
                return null;
            }

            // Remove any comments.
            int commentIndex = line.indexOf('#');
            if (commentIndex >= 0)
            {
                line = line.substring(0, commentIndex);
            }

            // Remove any surrounding whitespace.
            line = line.trim();

            // Skip any empty lines.
            if (line.length() == 0)
            {
                continue;
            }

            try
            {
                // Parse the line.
                String[] columns = line.split("\\s+");
                if (columns.length == 3)
                {
                    return new LpcVoicedFrame(Integer.parseInt(columns[0], 16),
                                              Integer.parseInt(columns[1], 16),
                                              Long.parseLong(columns[2], 16));
                }
                else if (columns.length == 2 && columns[1].length() > 2)
                {
                    return new LpcUnvoicedFrame(Integer.parseInt(columns[0], 16),
                                                Long.parseLong(columns[1], 16));
                }
                else if (columns.length == 2)
                {
                    return new LpcRepeatFrame(Integer.parseInt(columns[0], 16),
                                              Integer.parseInt(columns[1], 16));
                }
                else if (columns.length == 1 && columns[0].equals("0"))
                {
                    return new LpcSilenceFrame();
                }
                else if (columns.length == 1 && columns[0].equals("f"))
                {
                    return new LpcStopFrame();
                }
                else
                {
                    throw new IOException("Invalid frame ["+line+"] at line "+(reader.getLineNumber()-1));
                }
            }
            catch (NumberFormatException e)
            {
                throw new IOException("Invalid number in frame ["+line+"] at line "+(reader.getLineNumber()-1), e);
            }
        }
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        reader.close();
    }


    /**
     * Prints out the frames of the specified LPC file.
     */
    public static void main(String[] args)
    {
        try (LpcFrameReader lpcFrameReader =
                 new LpcFrameReader(
                 new BufferedReader(
                 new FileReader(args[0]))))
        {
            int counter = 0;

            LpcFrame frame;
            while ((frame = lpcFrameReader.readFrame()) != null)
            {
                System.out.println("#"+counter+" ("+String.format("%.3f", counter*0.025)+"): "+frame);

                counter++;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
