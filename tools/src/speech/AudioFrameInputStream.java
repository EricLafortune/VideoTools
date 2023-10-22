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

import javax.sound.sampled.*;
import java.io.*;
import java.util.Arrays;

/**
 * This class parses and returns frames of samples from an audio input stream
 * (mono, 8000 Hz, 16 bits). The frames have a given frame size and may overlap
 * by specifying a smaller step size. The final frame is zero-padded.
 */
public class AudioFrameInputStream
implements   AutoCloseable
{
    private final AudioInputStream audioInputStream;
    private final boolean          isBigEndian;
    private final int              frameSize;
    private final int              stepSize;

    private byte[] buffer;
    private int    bufferOffset;
    private int    bufferCount;
    private int    sampleCount;


    /**
     * Creates a new instance.
     * @param audioInputStream the audio input stream that provides the
     *                         samples.
     * @param frameSize        the number of samples in each single frame to
     *                         be read.
     */
    public AudioFrameInputStream(AudioInputStream audioInputStream,
                                 int              frameSize)
    throws IOException
    {
        this(audioInputStream, frameSize, frameSize);
    }


    /**
     * Creates a new instance.
     * @param audioInputStream the audio input stream that provides the
     *                         samples.
     * @param frameSize        the number of samples in each single frame to
     *                         be read.
     * @param stepSize         the number of samples between the beginnings of
     *                         subsequent frames. This can be the frame size
     *                         itself, or a smaller value to get overlapping
     *                         frames.
     */
    public AudioFrameInputStream(AudioInputStream audioInputStream,
                                 int              frameSize,
                                 int              stepSize)
    throws IOException
    {
        this(audioInputStream, frameSize, stepSize, 0);
    }


    /**
     * Creates a new instance.
     * @param audioInputStream the audio input stream that provides the
     *                         samples.
     * @param frameSize        the number of samples in each single frame to
     *                         be read.
     * @param stepSize         the number of samples between the beginnings of
     *                         subsequent frames. This can be the frame size
     *                         itself, or a smaller value to get overlapping
     *                         frames.
     * @param skipCount        the number of samples to skip at the start of
     *                         the audio input stream.
     */
    public AudioFrameInputStream(AudioInputStream audioInputStream,
                                 int              frameSize,
                                 int              stepSize,
                                 int              skipCount)
    throws IOException
    {
        AudioFormat format = audioInputStream.getFormat();
        if (format.getChannels() != 1)
        {
            throw new IOException("Expecting single channel, not "+format.getChannels());
        }
        if (format.getSampleRate() != 8000)
        {
            throw new IOException("Expecting sample rate 8000, not "+format.getSampleRate());
        }
        if (format.getSampleSizeInBits() != 16)
        {
            throw new IOException("Expecting sample size of 16 bits, not "+format.getSampleSizeInBits());
        }

        this.audioInputStream = audioInputStream;
        this.isBigEndian      = format.isBigEndian();
        this.frameSize        = frameSize;
        this.stepSize         = stepSize;

        buffer = new byte[(frameSize + stepSize - 1) / stepSize * stepSize * 2];

        // Start with reading a full window of bytes.
        bufferOffset = 0;
        bufferCount  = buffer.length;

        // Skip the specified number of samples at the start.
        audioInputStream.skip(2 * skipCount);
    }


    /**
     * Parses and returns the next frame of samples in the stream.
     * @return the the frame, or null when the end of the input is reached.
     */
    public double[] readFrame() throws IOException
    {
        double[] samples = new double[frameSize];

        return readFrame(samples) > 0 ? samples : null;
    }


    /**
     * Parses the next frame of samples in the stream.
     * @param samples a buffer in which to copy the samples.
     * @return the number of actual samples in the frame. This may be smaller
     *         than the frame size near emd of the input, or 0 when the end of
     *         the input is reached.
     */
    public int readFrame(double[] samples) throws IOException
    {
        // Read the window of bytes in a rolling buffer.
        // The first time, this is a complete buffer.
        // Then, it is the step size.
        int readCount =
            audioInputStream.read(buffer,
                                  bufferOffset,
                                  bufferCount);

        if (readCount < 0)
        {
            readCount = 0;
        }

        // Fill any final incomplete window with zeros.
        Arrays.fill(buffer,
                    bufferOffset + readCount,
                    bufferOffset + bufferCount,
                    (byte)0);

        // Compute the start of the data for this window.
        bufferOffset =
            (bufferOffset + bufferCount) % buffer.length;

        // Convert the bytes to doubles.
        extractSamples(samples);

        // Henceforth, only read the bytes of a single step.
        bufferCount = stepSize * 2;

        // Count the number of actual samples in the buffer.
        sampleCount += readCount / 2;
        int returnSamples = sampleCount;
        sampleCount -= stepSize;
        if (sampleCount < 0)
        {
            sampleCount = 0;
        }

        return returnSamples;
    }


    /**
     * Skips a frame.
     */
    public void skipFrame() throws IOException
    {
        readFrame(new double[frameSize]);
    }


    /**
     * Skips the given number of frames.
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
        audioInputStream.close();
    }


    // Small utility methods.

    /**
     * Extracts a frame of samples from the circular byte buffer into the given
     * double array.
     */
    private void extractSamples(double[] samples)
    {
        int msbOffset = isBigEndian ? 0 : 1;
        int lsbOffset = 1 - msbOffset;

        // Convert the signed 16-bits samples to doubles.
        int index;
        for (index = 0; index < frameSize; index++)
        {
            // Compute the offsets in the rolling byte buffer.
            int offset = (bufferOffset + index * 2) % buffer.length;

            samples[index] =
                (short)((buffer[offset + msbOffset] << 8) |
                        (buffer[offset + lsbOffset] & 0xff)) /
                (double)Short.MAX_VALUE;
        }

        while (index < samples.length)
        {
            samples[index++] = 0.0;
        }
    }


    /**
     * Prints out frames of the specified sound file.
     *
     * Usage:
     *   java speech.AudioFrameInputStream filename [framesize [stepsize]]
     */
    public static void main(String[] args)
    throws IOException, UnsupportedAudioFileException
    {
        int argIndex = 0;

        String fileName  = args[argIndex++];
        int    frameSize = argIndex < args.length ? Integer.parseInt(args[argIndex++]) : 200;
        int    stepSize  = argIndex < args.length ? Integer.parseInt(args[argIndex++]) : frameSize;

        try (AudioFrameInputStream audioFrameInputStream =
                 new AudioFrameInputStream(
                 AudioSystem.getAudioInputStream(
                 new BufferedInputStream(
                 new FileInputStream(fileName))),
                 frameSize,
                 stepSize))
        {
            double[] samples = new double[frameSize];

            int frameCounter = 0;

            while (audioFrameInputStream.readFrame(samples) > 0)
            {
                System.out.println("# Frame "+frameCounter);
                for (int index = 0; index < samples.length; index++)
                {
                    System.out.println(samples[index]);
                }

                System.out.println();
                System.out.println();

                frameCounter++;
            }
        }
    }
}
