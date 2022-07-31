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
import speech.LpcInputStream;
import sound.*;
import display.*;
import video.*;

import java.io.*;
import java.util.*;

/**
 * This utility combines animation files (.zip with .pbm), sound files (.vgm,
 * .snd), and speech files (.lpc) in a single file (.tms).
 *
 * Usage:
 *     java ComposeVideo [-50Hz|-60Hz][offset:]input.{zip|vgm|snd|lpc}[,skip] ... output.tms
 *
 * where
 *     -50Hz  specifies a European target system with a display refresh of
 *            50 Hz (only for properly synchronizing the speech bitstream,
 *            fixed at 40 fps).
 *     -60Hz  specifies a US target system with a display refresh of
 *            60 Hz (only for properly synchronizing the speech bitstream,
 *            fixed at 40 fps).
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


    public static void main(String[] args)
    throws IOException
    {
        // Parse any options.
        int argIndex = 0;

        // Skip 1 in 3 vsyncs when filling the speech buffer.
        int lpcStreamSkipRate = 3;

        if (args[argIndex].equals("-50Hz"))
        {
            // European system at 50 Hz.
            lpcStreamSkipRate = 5;
            argIndex++;
        }
        else if (args[argIndex].equals("-60Hz"))
        {
            // US system at 60 Hz.
            lpcStreamSkipRate = 3;
            argIndex++;
        }

        // Collect the input files name, sorted on their start frame indices.
        FileAtFrame[] inputFiles     = inputFiles(args, argIndex, args.length-1);
        String        outputFileName = args[args.length - 1];

        // Start reading from the input files, merging data from the input
        // streams into the video output stream.
        DisplayInputStream displayInputStream = null;
        SoundInputStream   soundInputStream   = null;
        LpcInputStream     lpcInputStream     = null;

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

            // Create output frames as long as we have any input.
            while (inputFileIndex < inputFiles.length ||
                   displayInputStream != null         ||
                   soundInputStream   != null         ||
                   lpcInputStream     != null         ||
                   !displayOutputStream.readyToWriteDisplayDelta1())
            {
                // Open new files for which we've reached the start frame.
                while (inputFileIndex < inputFiles.length)
                {
                    // Is it to early to open the file?
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
                        if (displayInputStream != null)
                        {
                            displayInputStream.close();
                        }

                        displayInputStream =
                            new ZipDisplayInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        displayInputStream.skipFrames(skipCount);
                    }
                    if (inputFileName.endsWith(".pbm"))
                    {
                        if (displayInputStream != null)
                        {
                            displayInputStream.close();
                        }

                        displayInputStream =
                            new PbmDisplayInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        displayInputStream.skipFrames(skipCount);
                    }
                    if (inputFileName.endsWith(".png") ||
                        inputFileName.endsWith(".gif") ||
                        inputFileName.endsWith(".jpg") ||
                        inputFileName.endsWith(".bmp"))
                    {
                        if (displayInputStream != null)
                        {
                            displayInputStream.close();
                        }

                        displayInputStream =
                            new ImageDisplayInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        displayInputStream.skipFrames(skipCount);
                    }
                    else if (inputFileName.endsWith(".vgm"))
                    {
                        if (soundInputStream != null)
                        {
                            soundInputStream.close();
                        }

                        soundInputStream =
                            new VgmInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        soundInputStream.skipFrames(skipCount);
                    }
                    else if (inputFileName.endsWith(".snd"))
                    {
                        if (soundInputStream != null)
                        {
                            soundInputStream.close();
                        }

                        soundInputStream =
                            new SndInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)));

                        soundInputStream.skipFrames(skipCount);
                    }
                    else if (inputFileName.endsWith(".lpc"))
                    {
                        if (lpcInputStream != null)
                        {
                            lpcInputStream.close();
                        }

                        lpcInputStream =
                            new LpcInputStream(
                            new BufferedInputStream(
                            new FileInputStream(inputFileName)),
                            true);

                        lpcInputStream.skipFrames(skipCount);
                    }

                    inputFileIndex++;
                }

                // Write the animation data (interleaving two parts,
                // resulting in 25 or 30 fps).
                if (displayOutputStream.readyToWriteDisplayDelta1())
                {
                    // See if we can get a new animation frame.
                    if (displayInputStream != null)
                    {
                        Display display = displayInputStream.readFrame();
                        if (display != null)
                        {
                            // Write out the first part of the display.
                            displayOutputStream.writeDisplayDelta1(display);

                            displayFrameCount++;
                        }
                        else
                        {
                            displayInputStream.close();
                            displayInputStream = null;
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
                if (soundInputStream != null)
                {
                    byte[] soundData = soundInputStream.readFrame();
                    if (soundData != null)
                    {
                        videoOutputStream.writeSoundData(soundData);

                        soundFrameCount++;
                    }
                    else
                    {
                        soundInputStream.close();
                        soundInputStream = null;
                    }
                }

                // Write the speech data (at 40 fps).
                if (lpcInputStream != null &&
                    videoOutputStream.getVsyncCount() % lpcStreamSkipRate != 0)
                {
                    byte[] speechData = lpcInputStream.readFrame();
                    if (speechData != null)
                    {
                        videoOutputStream.writeSpeechData(speechData);

                        speechFrameCount++;
                    }
                    else
                    {
                        lpcInputStream.close();
                        lpcInputStream = null;
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
            if (displayInputStream != null)
            {
                displayInputStream.close();
            }
            if (soundInputStream != null)
            {
                soundInputStream.close();
            }
            if (lpcInputStream != null)
            {
                lpcInputStream.close();
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
