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
 * This class parses and returns frame data from an input reader containing
 * Linear Predictive Coding (LPC) coefficients (short text form) from the
 * Praat phonetics software.
 *
 * @see https://www.fon.hum.uva.nl/praat/
 */
public class PraatLpcReader
implements   AutoCloseable
{
    private static final String FILE_TYPE_HEADER    = "File type = \"ooTextFile\"";
    private static final String OBJECT_CLASS_HEADER = "Object class = \"LPC 1\"";

    private static final boolean DEBUG       = false;

    private final LineNumberReader reader;

    private final double xmin;
    private final double xmax;
    private final int    frameCount;
    private final double dx;
    private final double x1;
    private final double samplingPeriod;
    private final int    maxNCoefficients;

    private int frameCounter;


    /**
     * Creates a new instance that reads its data from the given reader.
     */
    public PraatLpcReader(Reader reader)
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

        xmin             = readDouble();
        xmax             = readDouble();
        frameCount       = readInt();
        dx               = readDouble();
        x1               = readDouble();
        samplingPeriod   = readDouble();
        maxNCoefficients = readInt();
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


    public double getSamplingPeriod()
    {
        return samplingPeriod;
    }


    public int getMaxNCoefficients()
    {
        return maxNCoefficients;
    }


    /**
     * Parses and returns the gain and the predictor coefficients of the
     * next frame in the reader. The returned frame does not contain an
     * intensity or a pitch.
     */
    public PraatLpcFrame readFrame() throws IOException
    {
        if (frameCounter >= frameCount)
        {
            return null;
        }

        int nCoefficients = readInt();
        if (nCoefficients < 0 ||
            nCoefficients > maxNCoefficients)
        {
            throw new IOException("Unexpected number of coefficients ["+nCoefficients+"] in frame "+frameCount+" on line "+(reader.getLineNumber()-1));
        }

        double[] coefficients = new double[nCoefficients];
        for (int index = 0; index < nCoefficients; index++)
        {
            coefficients[index] = readDouble();
        }

        // The code
        //     https://github.com/praat/praat/blob/master/LPC/Sound_and_LPC.cpp#L728
        // multiplies the final amplitudes by the (interpolated) square root
        // of the gain (not just the excitation signal).
        double gain = readDouble();

        frameCounter++;

        return new PraatLpcFrame(0.0, 0.0, gain, coefficients);
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
     * Prints out the LPC coefficients of the frames in the specified Praat
     * LPC file.
     */
    public static void main(String[] args)
    {
        try (PraatLpcReader lpcReader =
                 new PraatLpcReader(
                 new BufferedReader(
                 new FileReader(args[0]))))
        {
            int counter = 0;

            while (true)
            {
                PraatLpcFrame frame = lpcReader.readFrame();
                if (frame == null)
                {
                    break;
                }

                double[] coefficients = frame.predictorCoefficients;
                System.out.print(String.format("%4d: gain = %6.4f, %2d coefficients:",
                                               counter++,
                                               frame.gain,
                                               coefficients.length));
                for (int index = 0; index < coefficients.length; index++)
                {
                    System.out.print(String.format(" %7.4f", coefficients[index]));
                }
                System.out.println();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
