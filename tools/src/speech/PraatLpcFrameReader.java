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
 * This class parses and returns frame frames from input readers containing
 * pitch values and Linear Predictive Coding (LPC) coefficients (short text
 * form) from the Praat phonetics software.
 *
 * @see https://www.fon.hum.uva.nl/praat/
 */
public class PraatLpcFrameReader
implements   AutoCloseable
{
    private final PraatPitchReader pitchReader;
    private final PraatLpcReader   lpcReader;


    /**
     * Creates a new instance that reads its data from the given readers.
     */
    public PraatLpcFrameReader(Reader pitchReader0,
                               Reader lpcReader0)
    throws IOException
    {
        pitchReader = new PraatPitchReader(pitchReader0);
        lpcReader   = new PraatLpcReader(lpcReader0);

        // Check if the data are compatible.
        if (pitchReader.getXmin() != lpcReader.getXmin() ||
            pitchReader.getXmax() != lpcReader.getXmax())
        {
            throw new IOException("Different time ranges for pitches ["+pitchReader.getXmin()+","+pitchReader.getXmax()+"] and for LPC coefficients ["+lpcReader.getXmin()+","+lpcReader.getXmax()+"]");
        }

        if (Math.abs(pitchReader.getFrameCount() - lpcReader.getFrameCount()) > 10)
        {
            throw new IOException("Very different frame counts for pitches ["+pitchReader.getFrameCount()+"] and for LPC coefficients ["+lpcReader.getFrameCount()+"]");
        }

        if (pitchReader.getDx() != lpcReader.getDx())
        {
            throw new IOException("Different time steps for pitches ["+pitchReader.getDx()+"] and for LPC coefficients ["+lpcReader.getDx()+"]");
        }

        // Synchronize the data streams so they start at approximately the same time.
        int count = (int)Math.round((lpcReader.getX1() - pitchReader.getX1()) / getDx());
        if (count > 0)
        {
            pitchReader.skipFrames(count);
        }
        else
        {
            lpcReader.skipFrames(count);
        }
    }


    public double getXmin()
    {
        return pitchReader.getXmin();
    }


    public double getXmax()
    {
        return pitchReader.getXmax();
    }


    public int getFrameCount()
    {
        return pitchReader.getFrameCount();
    }


    public double getDx()
    {
        return pitchReader.getDx();
    }


    public double getX1()
    {
        return pitchReader.getX1();
    }


    public double getCeiling()
    {
        return pitchReader.getCeiling();
    }


    public int getMaxnCandidates()
    {
        return pitchReader.getMaxnCandidates();
    }


    public double getSamplingPeriod()
    {
        return lpcReader.getSamplingPeriod();
    }


    public int getMaxNCoefficients()
    {
        return lpcReader.getMaxNCoefficients();
    }


    /**
     * Parses and returns the next frame in the reader.
     */
    public PraatLpcFrame readFrame() throws IOException
    {
        // Get the partial frames.
        PraatLpcFrame pitchFrame = pitchReader.readFrame();
        if (pitchFrame == null)
        {
            return null;
        }

        PraatLpcFrame lpcFrame = lpcReader.readFrame();
        if (lpcFrame == null)
        {
            return null;
        }

        // Compose a complete frame.
        return new PraatLpcFrame(pitchFrame.intensity,
                                 pitchFrame.frequency,
                                 lpcFrame.gain,
                                 lpcFrame.predictorCoefficients);
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
        pitchReader.close();
        lpcReader.close();
    }


    /**
     * Prints out the LPC coefficients of the frames in the specified Praat
     * LPC file.
     */
    public static void main(String[] args)
    {
        try (PraatLpcFrameReader lpcFrameReader =
                 new PraatLpcFrameReader(
                 new BufferedReader(
                 new FileReader(args[0])),
                 new BufferedReader(
                 new FileReader(args[1]))))
        {
            int counter = 0;

            while (true)
            {
                PraatLpcFrame frame = lpcFrameReader.readFrame();
                if (frame == null)
                {
                    break;
                }

                double[] coefficients = frame.predictorCoefficients;
                System.out.print(String.format("%4d: intens = %6.4f, freq = %6.2f, gain = %6.4f, %2d coefficients:",
                                               counter++,
                                               frame.intensity,
                                               frame.frequency,
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
