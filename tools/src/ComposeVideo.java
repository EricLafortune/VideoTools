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

import display.*;
import sound.*;
import speech.*;
import video.*;

import java.io.*;
import java.util.Arrays;

/**
 * This utility combines animation files (.pbm, .png, .gif, .jpg, .bmp, .zip),
 * sound files (.vgm, .snd), and speech files (.lpc) in a single file (.tms).
 *
 * Usage:
 *     java ComposeVideo [options] [offset:]input.{zip|vgm|snd|lpc}[,skip] ... output.tms
 *
 * where the options are:
 *     -ntsc  specifies a US target system with a display refresh of
 *            60 Hz (actually: 59.922738 Hz) (the default) (only for properly
 *            synchronizing the speech bitstream, fixed at 40 fps).
 *     -pal   specifies a European target system with a display refresh of
 *            50 Hz (actually: 50.158969 Hz).
 *     -videofrequency <framerate> specifies any display refresh rate.
 * The input modifiers are:
 *     offset is the start frame (50 Hz) of the input file in the output.
 *     skip   is the number of leading frames to skip in the input file.
 *
 * For example:
 *     java ComposeVideo animation.zip video.tms
 *
 *     java ComposeVideo speech.lpc speech.tms
 *
 *     java ComposeVideo animation.zip 200:music.snd 1000:vocals.lpc video.tms
 */
public class ComposeVideo
{
    private static final int BANK_SIZE = 8 * 1024;

    private static final double NTSC_FREQUENCY = SN76496.NTSC_FRAME_FREQUENCY;
    private static final double PAL_FREQUENCY  = SN76496.PAL_FRAME_FREQUENCY;

    // We increase the theoretical speech frequency slightly, to feed the
    // speech buffer slightly too fast. If we feed the speech buffer too
    // slowly, e.g. due to complex animations and missed Vsyncs, it eventually
    // underflows. The speech synthesizer temporarily halts the CPU if the CPU
    // tries to overflow the speech buffer (no big problem), but it exits the
    // SPEAK_EXTERNAL mode if the speech buffer underflows (leading to garbled
    // speech).
    private static final double EPSILON = 0.01;

    private static final double LPC_FREQUENCY  = 40.0 + EPSILON;


    public static void main(String[] args)
    throws IOException
    {
        double videoFrequency = NTSC_FREQUENCY;

        // Parse any options.
        int argIndex = 0;

        while (args[argIndex].startsWith("-"))
        {
            videoFrequency = videoFrequency(switch (args[argIndex++])
            {
                case "-videofrequency" -> args[argIndex++].toUpperCase();
                default                -> args[argIndex-1].substring(1).toUpperCase();
            });
        }

        // Collect the input files name, sorted on their start frame indices.
        FileAtFrame[] inputFiles     = inputFiles(args, argIndex, args.length-1);
        String        outputFileName = args[args.length - 1];

        // Start reading from the input files, merging data from the input
        // streams into the video output stream.
        DisplayInput displayInput = null;
        SndInput     sndInput     = null;
        LpcInput     lpcInput     = null;

        try (VideoOutputStream videoOutputStream =
                 createVideoOutputStream(outputFileName))
        {
            // Wrap the video output stream in a utility stream that
            // optimizes and writes the display deltas for us.
            DisplayDeltaOutputStream displayOutputStream =
                new DisplayDeltaOutputStream(videoOutputStream);

            int inputFileIndex    = 0;
            int displayFrameCount = 0;
            int soundFrameCount   = 0;
            int speechFrameCount  = 0;

            int speechStartVsync    = 0;
            int speechSuppressCount = 0;

            // Create output frames as long as we have any input.
            while (inputFileIndex < inputFiles.length ||
                   displayInput != null               ||
                   sndInput     != null               ||
                   lpcInput     != null               ||
                   !displayOutputStream.readyToWriteDisplayDelta1())
            {
                // Open new files for which we've reached the start frame.
                while (inputFileIndex < inputFiles.length)
                {
                    // Is it too early to open the file?
                    FileAtFrame inputFile = inputFiles[inputFileIndex];
                    if (inputFile.frameIndex > videoOutputStream.getVsyncCount())
                    {
                        // Continue with the current input streams.
                        break;
                    }

                    // We'll open the input stream.
                    String inputFileName = inputFile.fileName;
                    int    skipCount     = inputFile.skipCount;

                    // Set up the right input stream, depending on the file
                    // name extension.
                    if (inputFileName.endsWith(".zip"))
                    {
                        if (displayInput != null)
                        {
                            displayInput.close();
                        }

                        displayInput =
                            new ZipDisplayInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        displayInput.skipFrames(skipCount);
                    }
                    if (inputFileName.endsWith(".pbm"))
                    {
                        if (displayInput != null)
                        {
                            displayInput.close();
                        }

                        displayInput =
                            new PbmDisplayInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        displayInput.skipFrames(skipCount);
                    }
                    if (inputFileName.endsWith(".png") ||
                        inputFileName.endsWith(".gif") ||
                        inputFileName.endsWith(".jpg") ||
                        inputFileName.endsWith(".bmp"))
                    {
                        if (displayInput != null)
                        {
                            displayInput.close();
                        }

                        displayInput =
                            new ImageDisplayInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        displayInput.skipFrames(skipCount);
                    }
                    else if (inputFileName.endsWith(".vgm"))
                    {
                        if (sndInput != null)
                        {
                            sndInput.close();
                        }

                        sndInput =
                            new VgmInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        sndInput.skipFrames(skipCount);
                    }
                    else if (inputFileName.endsWith(".snd"))
                    {
                        if (sndInput != null)
                        {
                            sndInput.close();
                        }

                        sndInput =
                            new SndInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        sndInput.skipFrames(skipCount);
                    }
                    else if (inputFileName.endsWith(".lpc"))
                    {
                        if (lpcInput != null)
                        {
                            lpcInput.close();
                        }

                        lpcInput =
                            new LpcInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)),
                            true);

                        lpcInput.skipFrames(skipCount);

                        // Already start filling the 16-byte speech buffer,
                        // to get the speech started (after the initial
                        // SPEAK_EXTERNAL byte, then the 9th LPC byte, then
                        // another 50 microseconds) and to then reduce the
                        // risk of a buffer underflow.
                        // We'll suppress these first speech frames in their
                        // regular slots later on.
                        // If we can write 2 speech frames now, 1 will be
                        // playing and 1 will still be in the buffer by the
                        // time the first regular slot comes up, so we don't
                        // want to suppress any slots in that case.
                        speechStartVsync    = videoOutputStream.getVsyncCount();
                        speechSuppressCount = -2;

                        // Collect multiple initial speech frames in a single
                        // buffer.
                        byte[] speechBuffer      = new byte[17];
                        int    speechBufferCount = 0;
                        while (speechBufferCount < 1 + 10)
                        {
                            // The first speech frame starts with the
                            // SPEAK_EXTERNAL byte.
                            // A standard speech frame can be between 0 bytes
                            // (4 bits for a silence that was already included
                            // in the previous frame) and 7 bytes (50 bits for
                            // a voiced frame).
                            byte[] speechData = lpcInput.readFrame();
                            if (speechData != null)
                            {
                                System.arraycopy(speechData,
                                                 0,
                                                 speechBuffer,
                                                 speechBufferCount,
                                                 speechData.length);

                                speechBufferCount += speechData.length;

                                speechSuppressCount++;

                                speechFrameCount++;
                            }
                            else
                            {
                                lpcInput.close();
                                lpcInput = null;
                                break;
                            }
                        }

                        // Write the collected initial speech data.
                        byte[] speechData =
                            Arrays.copyOf(speechBuffer, speechBufferCount);

                        videoOutputStream.writeSpeechData(speechData);
                    }

                    inputFileIndex++;
                }

                // Write the animation data (interleaving two parts,
                // resulting in 25 or 30 fps).
                if (displayOutputStream.readyToWriteDisplayDelta1())
                {
                    // See if we can get a new animation frame.
                    if (displayInput != null)
                    {
                        Display display = displayInput.readFrame();
                        if (display != null)
                        {
                            // Write out the first part of the display.
                            displayOutputStream.writeDisplayDelta1(display);

                            displayFrameCount++;
                        }
                        else
                        {
                            displayInput.close();
                            displayInput = null;
                        }
                    }
                }
                else
                {
                    // Write out the second part of the display.
                    displayOutputStream.writeDisplayDelta2();
                }

                // Wait for the VSync (at 50 or 60 fps).
                videoOutputStream.writeVSync();

                // Write the sound data (at 50 or 60 fps).
                if (sndInput != null)
                {
                    byte[] soundData = sndInput.readFrame();
                    if (soundData != null)
                    {
                        videoOutputStream.writeSoundData(soundData);

                        soundFrameCount++;
                    }
                    else
                    {
                        sndInput.close();
                        sndInput = null;
                    }
                }

                // Write the speech data (at 40 fps).
                if (lpcInput != null &&
                    readyForLpcFrame(videoFrequency,
                                     speechStartVsync,
                                     videoOutputStream))
                {
                    if (speechSuppressCount > 0)
                    {
                        speechSuppressCount--;
                    }
                    else
                    {
                        byte[] speechData = lpcInput.readFrame();
                        if (speechData != null)
                        {
                            videoOutputStream.writeSpeechData(speechData);

                            speechFrameCount++;
                        }
                        else
                        {
                            lpcInput.close();
                            lpcInput = null;
                        }
                    }
                }
            }

            int bankCount = videoOutputStream.getBankCount();

            System.out.println("Created video file [" + outputFileName + "]:");
            System.out.println("  Input files:    " + inputFiles.length);
            System.out.println("  Vsyncs:         " + videoOutputStream.getVsyncCount());
            System.out.println("  Display frames: " + displayFrameCount);
            System.out.println("  Sound frames:   " + soundFrameCount);
            System.out.println("  Speech frames:  " + speechFrameCount);
            System.out.println("  Memory banks:   " + (bankCount + 1));
            System.out.println(String.format("  Size:           %d bytes = %.1f MB",
                                             (bankCount + 1) * BANK_SIZE,
                                             (bankCount + 1) * BANK_SIZE / 1024. / 1024.));
        }
        finally
        {
            if (displayInput != null)
            {
                displayInput.close();
            }
            if (sndInput != null)
            {
                sndInput.close();
            }
            if (lpcInput != null)
            {
                lpcInput.close();
            }
        }
    }


    private static FileAtFrame[] inputFiles(String[] fileNames,
                                            int      startIndex,
                                            int      endIndex)
    {
        // Collect all input file names and their start frame indices.
        FileAtFrame[] inputFiles = new FileAtFrame[endIndex - startIndex];

        for (int index = 0; index < inputFiles.length; index++)
        {
            String inputFileName = fileNames[startIndex + index];

            // Extract the (optional) start in the final video, and the
            // file name.
            int frameIndex = 0;
            int colonIndex = inputFileName.indexOf(':');
            if (colonIndex > 0)
            {
                try
                {
                    frameIndex    = Integer.parseInt(inputFileName.substring(0, colonIndex));
                    inputFileName = inputFileName.substring(colonIndex+1);

                }
                catch (NumberFormatException e)
                {
                    // The colon may just be part of the file name.
                }
            }

            // Extract the (optional) start in the input file
            // (frame skip count), and the file name.
            int skipCount  = 0;
            int commaIndex = inputFileName.indexOf(',');
            if (commaIndex > 1)
            {
                try
                {
                    skipCount     = Integer.parseInt(inputFileName.substring(commaIndex+1));
                    inputFileName = inputFileName.substring(0, commaIndex);
                }
                catch (NumberFormatException e)
                {
                    // The comma may just be part of the file name.
                }
            }

            inputFiles[index] = new FileAtFrame(frameIndex,
                                                inputFileName,
                                                skipCount);
        }

        // Make sure the file names are sorted on the start frame indices.
        Arrays.sort(inputFiles);

        return inputFiles;
    }


    private static VideoOutputStream createVideoOutputStream(String outputFileName)
    throws IOException
    {
        if (outputFileName.endsWith(".asm"))
        {
            return
                new TextVideoOutputStream(
                new PrintStream(
                new BufferedOutputStream(
                new FileOutputStream(outputFileName))),
                BANK_SIZE);
        }
        else if (outputFileName.endsWith(".tms"))
        {
            return
                new BinaryVideoOutputStream(
                new BufferedOutputStream(
                new FileOutputStream(outputFileName)),
                BANK_SIZE);
        }
        else
        {
            throw new IllegalArgumentException("Unknown type of output file ["+outputFileName+"]");
        }
    }


    /**
     * Returns whether it is time to insert an LPC speech frame into the
     * stream.
     * @param videoFrequency    the frequency of the Vsyncs.
     * @param speechStartVsync  the sequence number of the Vsync at which the
     *                          speech started.
     * @param videoOutputStream the output stream that is accumulating all
     *                          video/sound/speech/Vsync/... frames.
     */
    private static boolean readyForLpcFrame(double            videoFrequency,
                                            int               speechStartVsync,
                                            VideoOutputStream videoOutputStream)
    {
        int vsyncCount = videoOutputStream.getVsyncCount() - speechStartVsync;

        // Was the previous Vsync in a different speech frame?
        return (int)((vsyncCount+1) / videoFrequency * LPC_FREQUENCY) !=
               (int)((vsyncCount+2) / videoFrequency * LPC_FREQUENCY);
    }


    private static double videoFrequency(String frameFrequency)
    {
        return switch (frameFrequency.toUpperCase())
        {
            case "NTSC",
                 "60HZ" -> NTSC_FREQUENCY;
            case "PAL",
                 "50HZ" -> PAL_FREQUENCY;
            default     -> Double.parseDouble(frameFrequency);
        };
    }


    /**
     * This class represents a media file with a target start frame index in a
     * target video.
     */
    private static class FileAtFrame
    implements           Comparable
    {
        public int    frameIndex;
        public String fileName;
        public int    skipCount;


        public FileAtFrame(int    frameIndex,
                           String fileName,
                           int    skipCount)
        {
            this.frameIndex = frameIndex;
            this.fileName   = fileName;
            this.skipCount  = skipCount;
        }


        // Implementations for Comparable.

        public int compareTo(Object o)
        {
            FileAtFrame other = (FileAtFrame)o;

            return Integer.compare(frameIndex, other.frameIndex);
        }
    }
}
