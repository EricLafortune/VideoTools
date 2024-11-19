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

import musicxml.*;
import speech.*;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * This utility converts a given MusicXML file to a file in binary Linear
 * Predictive Coding (LPC) format, for a TMS52xx speech synthesis chip.
 *
 * Usage:
 *     java ConvertMusicXmlToLpc [options...] input.mxl output.lpc
 */
public class ConvertMusicXmlToLpc
{
    private static final String          DEFAULT_PART         = "P1";
    private static final String          DEFAULT_VOICE        = "1";
    private static final int             DEFAULT_STAFF        = -1;
    private static final LpcFrame[]      DEFAULT_HUM;
    private static final LpcQuantization DEFAULT_QUANTIZATION = LpcQuantization.TMS5200;

    static
    {
        LpcFrame[] defaultHum;
        try
        {
            defaultHum = readLpcFile("tee");
        }
        catch (IOException e)
        {
            defaultHum = new LpcFrame[] { new LpcVoicedFrame(14, 28, 0x45d32eab2cL) };
        }

        DEFAULT_HUM = defaultHum;
    }

    private static final boolean DEBUG = false;


    private final int                     startMeasure;
    private final int                     endMeasure;
    private final double                  speed;
    private final LpcChannelSpecification channel;
    private final LpcQuantization         quantization;
    private final boolean                 addStopFrame;


    public static void main(String[] args)
    throws IOException, XMLStreamException
    {
        int argIndex = 0;

        int                     startMeasure = 1;
        int                     endMeasure   = Integer.MAX_VALUE;
        double                  speed        = 1.0;
        LpcChannelSpecification channel      = new LpcChannelSpecification(DEFAULT_PART,
                                                                           DEFAULT_VOICE,
                                                                           DEFAULT_STAFF,
                                                                           DEFAULT_HUM);
        LpcQuantization         quantization = DEFAULT_QUANTIZATION;
        boolean                 addStopFrame = false;

        String hum = "tee";

        while (args[argIndex].startsWith("-"))
        {
            switch (args[argIndex++])
            {
                case "-startmeasure" -> startMeasure      = Integer.parseInt(args[argIndex++]);
                case "-endmeasure"   -> endMeasure        = Integer.parseInt(args[argIndex++]);
                case "-speed"        -> speed             = Double.parseDouble(args[argIndex++]);
                case "-part"         -> channel.part      = args[argIndex++];
                case "-voice"        -> channel.voice     = args[argIndex++];
                case "-staff"        -> channel.staff     = Integer.parseInt(args[argIndex++]);
                case "-attenuate"    -> channel.attenuate = Integer.parseInt(args[argIndex++]);
                case "-transpose"    -> channel.transpose = Integer.parseInt(args[argIndex++]);
                case "-hum"          -> channel.hum       = readLpcFile(args[argIndex++]);
                case "-chip"         -> quantization      = LpcQuantization.valueOf(args[argIndex++].toUpperCase());
                case "-tms5200"      -> quantization      = LpcQuantization.TMS5200;
                case "-tms5220"      -> quantization      = LpcQuantization.TMS5220;
                case "-addstopframe" -> addStopFrame      = true;
                default              -> throw new IllegalArgumentException("Unknown option [" + args[argIndex-1] + "]");
            }
        }

        String inputXmlFileName  = args[argIndex++];
        String outputLpcFileName = args[argIndex++];

        new ConvertMusicXmlToLpc(startMeasure,
                                 endMeasure,
                                 speed,
                                 channel,
                                 quantization,
                                 addStopFrame)
            .process(inputXmlFileName,
                     outputLpcFileName);
    }


    /**
     * Creates a new instance with the given settings.
     */
    public ConvertMusicXmlToLpc(int                     startMeasure,
                                int                     endMeasure,
                                double                  speed,
                                LpcChannelSpecification channel,
                                LpcQuantization         quantization,
                                boolean                 addStopFrame)
    {
        this.startMeasure = startMeasure;
        this.endMeasure   = endMeasure;
        this.speed        = speed;
        this.channel      = channel;
        this.quantization = quantization;
        this.addStopFrame = addStopFrame;
    }


    /**
     * Processes the specified MusicXML file to the specified LPC file.
     */
    private void process(String inputXmlFileName,
                         String outputLpcFileName)
    throws IOException, XMLStreamException
    {
        List<Note> notes = new ArrayList<>();

        // Read the input.
        try (MusicXmlInputStream musicXmlInputStream =
                 new MusicXmlInputStream(startMeasure,
                                         endMeasure,
                                         channel,
                                         new BufferedReader(
                                         xmlReader(inputXmlFileName))))
        {
            Note note;
            while ((note = musicXmlInputStream.readNote()) != null)
            {
                notes.add(note);
            }
        }

        // Start collecting some statistics.
        int    noteCount        = 0;
        double totalOffPitch    = 0.0;
        double maximumOffPitch  = 0.0;
        int    tooLowNoteCount  = 0;
        int    tooHighNoteCount = 0;

        // Write the output.
        try (LpcFrameOutput lpcFrameOutput =
                 new RepeatingLpcFrameOutput(
                 new LpcFrameOutputStream(
                 new BufferedOutputStream(
                 new FileOutputStream(outputLpcFileName)))))
        {
            double  currentTime         = 0.0;
            int     currentNoteTime     = 0;
            int     currentNoteIndex    = 0;
            int     currentEncodedPitch = 0;
            boolean tiePrevious         = false;
            boolean recomputePitch      = true;

            // Compute the duration of a sound frame, expressed in milliseconds
            // (like notes).
            double frameDuration = 1000.0 / TMS52xx.FRAME_FREQUENCY * speed;

            // Loop over the entire music piece.
            while (true)
            {
                // Get the current note.
                Note note = notes.get(currentNoteIndex);

                // Are we still in the same note?
                if (currentTime >= currentNoteTime + note.duration)
                {
                    // Have we passed the last note?
                    if (++currentNoteIndex >= notes.size())
                    {
                        break;
                    }

                    // Update the time to the start of the next note.
                    currentNoteTime += note.duration;

                    // Remember the tie from the previous note.
                    tiePrevious = note.tieNext;

                    // Get the next note.
                    note = notes.get(currentNoteIndex);

                    // Remember to update the pitch.
                    recomputePitch = true;
                }

                // Do we need to recompute the pitch, for the first note or
                // for a new note?
                if (recomputePitch)
                {
                    if (!note.isRest())
                    {
                        // Count the number of notes.
                        noteCount++;

                        // Compute the frequency.
                        double frequency =
                            note.frequency * Math.pow(2, channel.transpose);

                        // Compute the encoded pitch.
                        currentEncodedPitch =
                            quantization.encodePitch(frequency);

                        // Check if the resulting frequency is sufficiently
                        // close to the intended note.
                        double resultingFrequency =
                            quantization.frequency(
                            quantization.pitchTable[currentEncodedPitch]);

                        double offPitch =
                            Math.abs(frequency - resultingFrequency) / frequency;

                        // Accumulate the off-pitch fraction.
                        totalOffPitch += offPitch;

                        // Remember the maximum off-pitch fraction.
                        if (maximumOffPitch < offPitch)
                        {
                            maximumOffPitch = offPitch;
                        }

                        // For a TI-99, the speech range is 38 Hz (~note D#1)
                        // to 533 Hz (note C4).

                        // Count the notes that are too low.
                        if (currentEncodedPitch == quantization.maxEncodedPitch())
                        {
                            tooLowNoteCount++;
                        }

                        // Count the notes that are too high.
                        if (currentEncodedPitch == quantization.minEncodedPitch())
                        {
                            tooHighNoteCount++;
                        }
                    }

                    // We've just computed and checked the pitch.
                    recomputePitch = false;
                }

                LpcFrame frame;
                if (note.isRest())
                {
                    // Rest with a silent frame.
                    frame = new LpcSilenceFrame();
                }
                else
                {
                    // Get a clone of the current hum frame.
                    frame = humFrame(channel.hum,
                                     frameDuration,
                                     currentTime,
                                     currentNoteTime,
                                     note.duration,
                                     tiePrevious,
                                     note.tieNext);

                    LpcEnergyFrame energyFrame = (LpcEnergyFrame)frame;

                    // Compute the attenuation.
                    int attenuation = note.attenuation + channel.attenuate;

                    // Attenuate the cloned frame.
                    energyFrame.energy =
                        Math.max(quantization.minEncodedEnergy(),
                                 energyFrame.energy - attenuation);

                    // Set the frequency of the cloned frame, if applicable.
                    if (frame instanceof LpcPitchFrame)
                    {
                        LpcPitchFrame pitchFrame = (LpcPitchFrame)frame;

                        pitchFrame.pitch = currentEncodedPitch;
                    }
                }

                // Write out the frame.
                lpcFrameOutput.writeFrame(frame);

                currentTime += frameDuration;
            }

            if (addStopFrame)
            {
                lpcFrameOutput.writeFrame(new LpcStopFrame());
            }
        }

        // Print out the statistics.
        System.out.println("Statistics humming '"+channel.part+"' on the "+quantization.name()+" speech synthesis chip:");
        System.out.printf("    Average off-pitch: %5.1f%%\n",   100.0 * totalOffPitch    / noteCount);
        System.out.printf("    Maximum off-pitch: %5.1f%%\n",   100.0 * maximumOffPitch);
        System.out.printf("    Notes too low:     %3.0f  %%\n", 100.0 * tooLowNoteCount  / noteCount);
        System.out.printf("    Notes too high:    %3.0f  %%\n", 100.0 * tooHighNoteCount / noteCount);
    }


    /**
     * Returns a reader with the uncompressed XML contents from the
     * specified uncompressed or compressed MusicXML file.
     */
    private static Reader xmlReader(String inputFileName)
    throws IOException
    {
        if (inputFileName.endsWith(".musicxml") ||
            inputFileName.endsWith(".xml"))
        {
            // Return the .xml file contents directly.
            return new FileReader(inputFileName);
        }
        else
        {
            // Look for the first XML file in the .mxl zip file.
            ZipInputStream mxlInputStream =
                new ZipInputStream(
                new BufferedInputStream(
                new FileInputStream(inputFileName)));

            while (true)
            {
                ZipEntry entry = mxlInputStream.getNextEntry();
                if (entry == null)
                {
                    throw new IOException("Can't find .musicxml file or .xml file in compressed MusicXML file ["+inputFileName+"]");
                }

                String name = entry.getName();
                if (name.indexOf('/') < 0 &&
                    (name.endsWith(".musicxml") ||
                     name.endsWith(".xml")))
                {
                    // Return the packaged XML file contents.
                    return new InputStreamReader(mxlInputStream);
                }
            }
        }
    }


    /**
     * Reads the specified LPC file and returns it as an array of LPC frames,
     * leaving out any silence frames or stop frames.
     */
    private static LpcFrame[] readLpcFile(String fileName)
    throws IOException
    {
        List<LpcFrame> frames = new ArrayList<>();

        try (LpcFrameInput lpcFrameInput =
                 new NonRepeatingLpcFrameInput(
                 lpcFrameInput(fileName)))
        {
            LpcFrame frame;
            while ((frame = lpcFrameInput.readFrame()) != null)
            {
                if (frame instanceof LpcEnergyFrame)
                {
                    frames.add(frame);
                }
            }
        }

        return frames.toArray(new LpcFrame[frames.size()]);
    }


    /**
     * Returns an LpcFrameInput from the specified file or resource file.
     * in binary LPC format or our custom text LPC format.
     */
    private static LpcFrameInput lpcFrameInput(String fileName)
    throws IOException
    {
        return
            fileName.endsWith(".lpc") ?
                new LpcFrameInputStream(
                new BufferedInputStream(
                new FileInputStream(fileName))) :

            fileName.endsWith(".txt") ?
                new LpcFrameReader(
                new BufferedReader(
                new FileReader(fileName))) :

                new LpcFrameReader(
                new BufferedReader(
                new InputStreamReader(
                ConvertMusicXmlToLpc.class
                    .getResource("hum/" + fileName + ".txt")
                    .openStream())));
    }


    /**
     * Returns the cloned LPC frame from the hum phrase, for the given time.
     */
    private static LpcFrame humFrame(LpcFrame[] hum,
                                     double     frameDuration,
                                     double     time,
                                     int        noteTime,
                                     int        noteDuration,
                                     boolean    tiePrevious,
                                     boolean    tieNext)
    {
        LpcFrame frame;

        // Compute the number of sound frames in this note, and
        // the index in its range.
        int frameCount =
            (int)Math.round(noteDuration / frameDuration);
        int frameIndex =
            Math.max(0,
            Math.min(frameCount - 1,
            (int)((time - noteTime) / frameDuration)));

        // Pick the right frame from the hum.
        // We're splitting the attack and the release at 2/3's of the hum.
        int splitIndex = hum.length * 2 / 3;

        // Short note?
        if (frameCount <= hum.length)
        {
            // Subsample the hum.
            frame = hum[frameIndex * hum.length / frameCount].clone();

            if (DEBUG)
            {
                System.out.println("  ["+frameIndex+"/"+frameCount+"] Subsample ["+frameIndex+"] "+frame);
            }
        }
        // Attack phase?
        else if (frameIndex <= splitIndex && !tiePrevious)
        {
            // Attack, with the first part of the hum.
            frame = hum[frameIndex].clone();

            if (DEBUG)
            {
                System.out.println("  ["+frameIndex+"/"+frameCount+"] Attack    ["+frameIndex+"] "+frame);
            }
        }
        // Release phase?
        else if (hum.length - frameCount + frameIndex > splitIndex && !tieNext)
        {
            // Release, with the second part of the hum.
            frame = hum[hum.length - frameCount + frameIndex].clone();

            if (DEBUG)
            {
                System.out.println("  [" + frameIndex + "/" + frameCount + "] Release   [" + (hum.length - frameCount + frameIndex) + "] " + frame);
            }
        }
        // Sustain phase.
        else
        {
            // Sustain, repeating the split frame of the hum.
            // We're adding some tremolo to make it sound slightly
            // more natural.
            frame = new LpcRepeatFrame(((LpcEnergyFrame)hum[splitIndex]).energy - (int)(2 * Math.random()), 0);

            if (DEBUG)
            {
                System.out.println("  ["+frameIndex+"/"+frameCount+"] Sustain   ["+splitIndex+"] "+frame);
            }
        }

        return frame;
    }
}