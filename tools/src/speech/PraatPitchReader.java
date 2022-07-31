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
 * This class parses and returns frame data from an input reader containing
 * intensity and pitch values (short text form) from the Praat phonetics
 * software.
 *
 * @see https://www.fon.hum.uva.nl/praat/
 */
public class PraatPitchReader
implements   AutoCloseable
{
    private static final String FILE_TYPE_HEADER    = "File type = \"ooTextFile\"";
    private static final String OBJECT_CLASS_HEADER = "Object class = \"Pitch 1\"";
    private static final double DX                  = 0.025;
    private static final double SAMPLING_PERIOD     = 0.000125;
    private static final int    MAX_N_COEFFICIENTS  = 10;

    private static final boolean DEBUG       = false;

    private final LineNumberReader reader;

    private final double xmin;
    private final double xmax;
    private final int    frameCount;
    private final double dx;
    private final double x1;
    private final double ceiling;
    private final int    maxnCandidates;

    private int frameCounter;


    /**
     * Creates a new instance that reads its data from the given reader.
     */
    public PraatPitchReader(Reader reader)
    throws IOException
    {
        this.reader = new LineNumberReader(reader);

        String line = readString();
        if (!line.equals(FILE_TYPE_HEADER))
        {
            throw new IOException("Unexpected header ["+line+"] instead of ["+FILE_TYPE_HEADER+"] on line "+(this.reader.getLineNumber()-1));
        }

        line = readString();
        if (!line.equals(OBJECT_CLASS_HEADER))
        {
            throw new IOException("Unexpected header ["+line+"] instead of ["+OBJECT_CLASS_HEADER+"] on line "+(this.reader.getLineNumber()-1));
        }

        readString(); // Empty line.

        xmin           = readDouble();
        xmax           = readDouble();
        frameCount     = readInt();
        dx             = readDouble();
        x1             = readDouble();
        ceiling        = readDouble();
        maxnCandidates = readInt();

        if (dx != DX)
        {
            throw new IOException("Unexpected frame time ["+dx+"] instead of ["+DX+"] on line "+(this.reader.getLineNumber()-1));
        }
    }


    public double getXmin()
    {
        return xmin;
    }


    public double getXmax()
    {
        return xmax;
    }


    public int getFrameCount()
    {
        return frameCount;
    }


    public double getDx()
    {
        return dx;
    }


    public double getX1()
    {
        return x1;
    }


    public double getCeiling()
    {
        return ceiling;
    }


    public int getMaxnCandidates()
    {
        return maxnCandidates;
    }


    /**
     * Parses and returns the intensity and the frequency of the pitch contour
     * for the next frame in the reader. The returned frame does not contain a
     * gain or prediction coefficients.
     */
    public PraatLpcFrame readFrame() throws IOException
    {
        if (frameCounter >= frameCount)
        {
            return null;
        }

        double intensity = readDouble();

        int nCandidates = readInt();
        if (nCandidates < 1)
        {
            throw new IOException("Unexpected number of candidates ["+nCandidates+"] in frame "+frameCount+" on line "+(reader.getLineNumber()-1));
        }

        double frequency = readDouble();

        readString(); // strength.

        // Skip any other candidates.
        for (int index = 1; index < nCandidates; index++)
        {
            readString(); // frequency.
            readString(); // strength.
        }

        frameCounter++;

        return new PraatLpcFrame(intensity, frequency, 0.0, null);
    }


    /**
     * Skips a frame (only at the start of a reader).
     */
    public void skipFrame() throws IOException
    {
        readFrame();
    }


    /**
     * Skips the given number of frames (only at the start of a reader).
     */
    public void skipFrames(int count) throws IOException
    {
        for (int counter = 0; counter < count; counter++)
        {
            skipFrame();
        }
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        reader.close();
    }


    // Small utility methods.

    private String readString() throws IOException
    {
        return this.reader.readLine();
    }


    private int readInt() throws IOException
    {
        return Integer.parseInt(this.reader.readLine());
    }


    private double readDouble() throws IOException
    {
        return Double.parseDouble(this.reader.readLine());
    }


    /**
     * Prints out the intensity and pitch values of the frames in the
     * specified Praat pitch file.
     */
    public static void main(String[] args)
    {
        try (PraatPitchReader pitchReader =
                 new PraatPitchReader(
                 new BufferedReader(
                 new FileReader(args[0]))))
        {
            int counter = 0;

            while (true)
            {
                PraatLpcFrame frame = pitchReader.readFrame();
                if (frame == null)
                {
                    break;
                }

                System.out.println(String.format("%4d: %6.4f %6.2f",
                                                 counter++,
                                                 frame.intensity,
                                                 frame.frequency));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
