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
import sound.*;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * This utility converts a given MusicXML file to a file in our optimized
 * Sound (.snd) format for the TMS9919/SN76489 sound processor.
 *
 * Usage:
 *     java ConvertMusicXmlToSnd [options...] input.mxl output.snd
 */
public class ConvertMusicXmlToSnd
{
    private static final String      DEFAULT_VOICE           = "1";
    private static final int         DEFAULT_STAFF           = -1;
    private static final Instrument  DEFAULT_INSTRUMENT      = Instrument.PIANO;
    private static final PsgComputer DEFAULT_COMPUTER        = PsgComputer.TI99;
    private static final double      DEFAULT_FRAME_FREQUENCY = SN76496.NTSC_FRAME_FREQUENCY;

    private static final int ATTENUATION_LOUDNESS_DAMPING = 1;
    private static final int DIVIDER_LOUDNESS_DAMPING     = 1000;

    private static final boolean DEBUG = false;

    private final int                       startMeasure;
    private final int                       endMeasure;
    private final double                    speed;
    private final SndChannelSpecification[] channels;
    private final PsgComputer               psgComputer;
    private final double                    frameFrequency;


    public static void main(String[] args)
    throws IOException, XMLStreamException
    {
        int argIndex = 0;

        int                           startMeasure   = 1;
        int                           endMeasure     = Integer.MAX_VALUE;
        double                        speed          = 1.0;
        List<SndChannelSpecification> channels       = new ArrayList<>();
        SndChannelSpecification       currentChannel = null;
        PsgComputer                   psgComputer    = DEFAULT_COMPUTER;
        double                        frameFrequency = DEFAULT_FRAME_FREQUENCY;

        while (args[argIndex].startsWith("-"))
        {
            switch (args[argIndex++])
            {
                case "-startmeasure"   -> startMeasure                     = Integer.parseInt(args[argIndex++]);
                case "-endmeasure"     -> endMeasure                       = Integer.parseInt(args[argIndex++]);
                case "-speed"          -> speed                            = Double.parseDouble(args[argIndex++]);
                case "-part"           -> channels.add(currentChannel      = new SndChannelSpecification(args[argIndex++], DEFAULT_VOICE, DEFAULT_STAFF, DEFAULT_INSTRUMENT));
                case "-voice"          -> check(currentChannel).voice      = args[argIndex++];
                case "-staff"          -> check(currentChannel).staff      = Integer.parseInt(args[argIndex++]);
                case "-chordnotes"     -> check(currentChannel).chordNotes = Integer.parseInt(args[argIndex++]);
                case "-attenuate"      -> check(currentChannel).attenuate  = Integer.parseInt(args[argIndex++]);
                case "-transpose"      -> check(currentChannel).transpose  = Integer.parseInt(args[argIndex++]);
                case "-instrument"     -> check(currentChannel).instrument = Instrument.valueOf(args[argIndex++].toUpperCase());
                case "-computer"       -> psgComputer                      = PsgComputer.valueOf(args[argIndex++].toUpperCase());
                case "-framefrequency" -> frameFrequency                   = frameFrequency(args[argIndex++]);
                default                -> throw new IllegalArgumentException("Unknown option [" + args[argIndex-1] + "]");
            }
        }

        String inputXmlFileName  = args[argIndex++];
        String outputSndFileName = args[argIndex++];

        new ConvertMusicXmlToSnd(startMeasure,
                                 endMeasure,
                                 speed,
                                 channels.toArray(new SndChannelSpecification[channels.size()]),
                                 psgComputer,
                                 frameFrequency)
            .process(inputXmlFileName,
                     outputSndFileName);
    }


    /**
     * Creates a new instance with the given settings.
     */
    public ConvertMusicXmlToSnd(int                       startMeasure,
                                int                       endMeasure,
                                double                    speed,
                                SndChannelSpecification[] channels,
                                PsgComputer               psgComputer,
                                double                    frameFrequency)
    {
        this.startMeasure   = startMeasure;
        this.endMeasure     = endMeasure;
        this.speed          = speed;
        this.channels       = channels;
        this.psgComputer    = psgComputer;
        this.frameFrequency = frameFrequency;
    }


    /**
     * Processes the specified MusicXML file to the specified LPC file.
     */
    private void process(String inputXmlFileName,
                         String outputSndFileName)
    throws IOException, XMLStreamException
    {
        // Set up lists for the notes.
        List<Note>[] notes = new List[channels.length];
        for (int index = 0; index < channels.length; index++)
        {
            notes[index] = new ArrayList<>();
        }

        // Read the input.
        try (MusicXmlInputStream musicXmlInputStream =
                 new MusicXmlInputStream(startMeasure,
                                         endMeasure,
                                         channels,
                                         new BufferedReader(
                                         xmlReader(inputXmlFileName))))
        {
            Note note;
            while ((note = musicXmlInputStream.readNote()) != null)
            {
                notes[note.channelIndex].add(note);
            }
        }

        // Check the input.
        for (int channelIndex = 0; channelIndex < channels.length; channelIndex++)
        {
            SndChannelSpecification channel      = channels[channelIndex];
            List<Note>              channelNotes = notes[channelIndex];
            if (channelNotes.isEmpty())
            {
                throw new IllegalArgumentException("Couldn't find any notes in part ["+channel.part+"], voice ["+channel.voice+"], staff ["+channel.staff+"]");
            }

            if (DEBUG)
            {
                int channelDuration =
                    channelNotes.stream()
                        .map(note -> note.duration)
                        .mapToInt(Integer::intValue)
                        .peek(v -> System.out.print(v+","))
                        .sum();

                System.out.println();

                System.out.println("Channel "+channelIndex+" ["+channel.part+", "+channel.staff+", "+channel.voice+"]:  "+channelNotes.size()+" notes, "+channelDuration+" ms");
            }
        }

        // Write the output.
        try (SoundCommandOutput soundCommandOutput =
                 new SndCommandOutputStream(
                 new BufferedOutputStream(
                 new FileOutputStream(outputSndFileName))))
        {
            // State for the channels.
            int[]     currentNoteIndices = new int[channels.length];
            int[]     currentNoteTimes   = new int[channels.length];
            boolean[] tiePrevious        = new boolean[channels.length];

            Arrays.fill(currentNoteIndices, -1);

            // State for the channels and their chords.
            // First compute the number of them.
            int chordChannelCount =
                Arrays.stream(channels)
                    .map(channel -> channel.chordNotes)
                    .mapToInt(Integer::intValue)
                    .sum();

            int[] targetAttenuations = new int[chordChannelCount];
            int[] targetDividers     = new int[chordChannelCount];
            int[] targetGenerators   = new int[chordChannelCount];

            Arrays.fill(targetAttenuations, VolumeCommand.SILENT);
            Arrays.fill(targetDividers,     -1);
            Arrays.fill(targetGenerators,   -1);

            // State for the generators.
            int[] currentGeneratorAttenuations = new int[4];
            int[] currentGeneratorDividers     = new int[4];

            Arrays.fill(currentGeneratorAttenuations, VolumeCommand.SILENT);
            Arrays.fill(currentGeneratorDividers,     -1);

            // Collect some statistics.
            int[]    chordChannelNoteCounts        = new int[chordChannelCount];
            double[] chordChannelTotalOffPitch     = new double[chordChannelCount];
            double[] chordChannelMaximumOffPitch   = new double[chordChannelCount];
            int[]    chordChannelTooLowNoteCounts  = new int[chordChannelCount];
            int[]    chordChannelTooHighNoteCounts = new int[chordChannelCount];

            int[] chordChannelSoundFrameCounts  = new int[chordChannelCount];
            int[] chordChannelPlayedFrameCounts = new int[chordChannelCount];

            // Compute the duration of a sound frame, expressed in milliseconds
            // (like notes).
            double frameDuration = 1000.0 * speed / frameFrequency;

            // The time into the music piece.
            double currentTime = 0.0;

            int activeChannelCount = channels.length;

            // Loop over the entire music piece.
            while (activeChannelCount > 0 || !allSilent(targetAttenuations))
            {
                int chordChannelIndex = 0;

                for (int channelIndex = 0; channelIndex < channels.length; channelIndex++)
                {
                    SndChannelSpecification channel      = channels[channelIndex];
                    Instrument              instrument   = channel.instrument;
                    List<Note>              channelNotes = notes[channelIndex];

                    // Start with an unchanged target attenuation.
                    int attenuation = targetAttenuations[chordChannelIndex];

                    // Get the currently playing note.
                    int noteIndex = currentNoteIndices[channelIndex];

                    Note note = noteIndex < 0                   ? new Note(channelIndex, 0) :
                                noteIndex < channelNotes.size() ? channelNotes.get(currentNoteIndices[channelIndex]) :
                                                                  new Note(channelIndex, Integer.MAX_VALUE / 2);

                    // Are we still in the same note?
                    if (currentTime >= currentNoteTimes[channelIndex] + note.duration)
                    {
                        // Update the time to the start of the next note.
                        currentNoteTimes[channelIndex] += note.duration;

                        // Get the next note.
                        currentNoteIndices[channelIndex] = ++noteIndex;
                        if (noteIndex < channelNotes.size())
                        {
                            // Remember the tie from the previous note.
                            tiePrevious[channelIndex] = note.tieNext;

                            note = channelNotes.get(noteIndex);

                            // Is it an actual note?
                            if (!note.isRest())
                            {
                                // Not tied to the previous note?
                                if (!tiePrevious[channelIndex])
                                {
                                    // Then reset the target attenuation.
                                    attenuation = VolumeCommand.SILENT +
                                                  channel.attenuate +
                                                  note.attenuation;
                                }

                                // Compute and assign the target dividers. They
                                // are different for all chord notes, but the
                                // same for all frames of the note.
                                computeTargetDividers(chordChannelIndex,
                                                      channel,
                                                      note,
                                                      targetDividers,
                                                      chordChannelNoteCounts,
                                                      chordChannelTotalOffPitch,
                                                      chordChannelMaximumOffPitch,
                                                      chordChannelTooLowNoteCounts,
                                                      chordChannelTooHighNoteCounts);
                            }
                        }
                        else
                        {
                            if (DEBUG)
                            {
                                System.out.println("["+channelIndex+", "+chordChannelIndex+"] Out of notes");
                            }

                            // We're out of notes.
                            note = new Note(channelIndex, Integer.MAX_VALUE / 2);
                            activeChannelCount--;
                        }
                    }

                    // Compute and assign the attenuation. It's the same for
                    // all chord notes, but different for each frame.
                    computeTargetAttenuations(chordChannelIndex,
                                              channel,
                                              note,
                                              attenuation,
                                              tiePrevious[channelIndex],
                                              instrument,
                                              currentNoteTimes[channelIndex],
                                              currentTime,
                                              frameDuration,
                                              targetDividers,
                                              targetAttenuations);

                    chordChannelIndex += channel.chordNotes;
                }

                // Find the 3 loudest tone channels and the loudest
                // tuned/untuned noise channels.
                int loudToneChordChannelIndex0   = -1;
                int loudToneChordChannelIndex1   = -1;
                int loudToneChordChannelIndex2   = -1;
                int loudTunedNoiseChannelIndex   = -1;
                int loudUntunedNoiseChannelIndex = -1;

                Instrument loudTunedNoiseInstrument   = null;
                Instrument loudUntunedNoiseInstrument = null;

                int toneChannelLoudness0        = Integer.MIN_VALUE;
                int toneChannelLoudness1        = Integer.MIN_VALUE;
                int toneChannelLoudness2        = Integer.MIN_VALUE;
                int tunedNoiseChannelLoudness   = Integer.MIN_VALUE;
                int untunedNoiseChannelLoudness = Integer.MIN_VALUE;

                chordChannelIndex = 0;

                for (int channelIndex = 0; channelIndex < channels.length; channelIndex++)
                {
                    SndChannelSpecification channel    = channels[channelIndex];
                    Instrument              instrument = channel.instrument;

                    for (int chordNote = 0; chordNote < channel.chordNotes; chordNote++)
                    {
                        // Compute the loudness of the channel (actually a
                        // negative quietness).
                        int targetAttenuation = targetAttenuations[chordChannelIndex];
                        int targetDivider     = targetDividers[chordChannelIndex];

                        if (targetAttenuation < VolumeCommand.SILENT)
                        {
                            // Remember how often this chord channel wasn't
                            // silent.
                            chordChannelSoundFrameCounts[chordChannelIndex]++;

                            int loudness =
                                -(targetAttenuation + ATTENUATION_LOUDNESS_DAMPING) *
                                (targetDivider + DIVIDER_LOUDNESS_DAMPING);

                            if (instrument.isTunedNoise())
                            {
                                if (loudness > tunedNoiseChannelLoudness)
                                {
                                    loudTunedNoiseChannelIndex = chordChannelIndex;
                                    tunedNoiseChannelLoudness  = loudness;
                                    loudTunedNoiseInstrument   = instrument;
                                }
                            }
                            else if (instrument.isUntunedNoise())
                            {
                                if (loudness > untunedNoiseChannelLoudness)
                                {
                                    loudUntunedNoiseChannelIndex = chordChannelIndex;
                                    untunedNoiseChannelLoudness  = loudness;
                                    loudUntunedNoiseInstrument   = instrument;
                                }
                            }
                            else
                            {
                                if (loudness > toneChannelLoudness0)
                                {
                                    loudToneChordChannelIndex2 = loudToneChordChannelIndex1;
                                    loudToneChordChannelIndex1 = loudToneChordChannelIndex0;
                                    loudToneChordChannelIndex0 = chordChannelIndex;

                                    toneChannelLoudness2  = toneChannelLoudness1;
                                    toneChannelLoudness1  = toneChannelLoudness0;
                                    toneChannelLoudness0  = loudness;
                                }
                                else if (loudness > toneChannelLoudness1)
                                {
                                    loudToneChordChannelIndex2 = loudToneChordChannelIndex1;
                                    loudToneChordChannelIndex1 = chordChannelIndex;

                                    toneChannelLoudness2  = toneChannelLoudness1;
                                    toneChannelLoudness1  = loudness;
                                }
                                else if (loudness > toneChannelLoudness2)
                                {
                                    loudToneChordChannelIndex2 = chordChannelIndex;

                                    toneChannelLoudness2  = loudness;
                                }
                            }
                        }

                        // Advance to the next channel in the chord.
                        chordChannelIndex++;
                    }
                }

                if (DEBUG)
                {
                    System.out.printf("Loudest %2d %2d %2d %2d %2d -> channels ",
                                      loudToneChordChannelIndex0,
                                      loudToneChordChannelIndex1,
                                      loudToneChordChannelIndex2,
                                      loudTunedNoiseChannelIndex,
                                      loudUntunedNoiseChannelIndex);
                }

                // Assign the loudest target attenuations/dividers to the
                // available generators.
                int[] newGeneratorAttenuations = new int[] { VolumeCommand.SILENT, VolumeCommand.SILENT, VolumeCommand.SILENT, VolumeCommand.SILENT };
                int[] newGeneratorDividers     = new int[] { -1, -1, -1, -1 };

                // Is the untuned noise louder than the tuned noise?
                if (untunedNoiseChannelLoudness >= tunedNoiseChannelLoudness)
                {
                    // Then forget the tuned noise.
                    loudTunedNoiseChannelIndex = -1;
                    tunedNoiseChannelLoudness  = Integer.MIN_VALUE;
                    loudTunedNoiseInstrument   = null;
                }

                // Is the tuned noise louder than at least one tone?
                if (tunedNoiseChannelLoudness > toneChannelLoudness2)
                {
                    // Assign the tuned noise.
                    newGeneratorAttenuations[SoundCommand.NOISE] =
                        targetAttenuations[loudTunedNoiseChannelIndex];

                    newGeneratorDividers[SoundCommand.NOISE] =
                        loudTunedNoiseInstrument.fixedNoiseDivider;

                    // Assign tuning tone 2.
                    newGeneratorAttenuations[SoundCommand.TONE2] =
                        VolumeCommand.SILENT;

                    newGeneratorDividers[SoundCommand.TONE2] =
                        targetDividers[loudTunedNoiseChannelIndex];

                    // Forget the quietest tone.
                    loudToneChordChannelIndex2 = -1;
                    toneChannelLoudness2       = Integer.MIN_VALUE;

                    // Forget the untuned noise.
                    loudUntunedNoiseChannelIndex = -1;
                    untunedNoiseChannelLoudness  = Integer.MIN_VALUE;
                    loudUntunedNoiseInstrument   = null;
                }

                // Do we have any untuned noise?
                if (loudUntunedNoiseChannelIndex >= 0)
                {
                    // Assign the untuned noise.
                    newGeneratorAttenuations[SoundCommand.NOISE] =
                        targetAttenuations[loudUntunedNoiseChannelIndex];

                    newGeneratorDividers[SoundCommand.NOISE] =
                        loudUntunedNoiseInstrument.fixedNoiseDivider;

                    // Forget the tuned noise.
                    loudTunedNoiseChannelIndex = -1;
                    loudTunedNoiseInstrument   = null;
                }

                // Look for generators that already have the target dividers.
                boolean assigned0 = assignCurrentGenerator(loudToneChordChannelIndex0, targetAttenuations, targetDividers, targetGenerators, currentGeneratorDividers, newGeneratorAttenuations, newGeneratorDividers);
                boolean assigned1 = assignCurrentGenerator(loudToneChordChannelIndex1, targetAttenuations, targetDividers, targetGenerators, currentGeneratorDividers, newGeneratorAttenuations, newGeneratorDividers);
                boolean assigned2 = assignCurrentGenerator(loudToneChordChannelIndex2, targetAttenuations, targetDividers, targetGenerators, currentGeneratorDividers, newGeneratorAttenuations, newGeneratorDividers);

                // TODO: Otherwise look for unused generators with the right frequencies.

                // Otherwise look for unused generators.
                assignAvailableGenerator(assigned0, loudToneChordChannelIndex0, targetAttenuations, targetDividers, targetGenerators, newGeneratorAttenuations, newGeneratorDividers);
                assignAvailableGenerator(assigned1, loudToneChordChannelIndex1, targetAttenuations, targetDividers, targetGenerators, newGeneratorAttenuations, newGeneratorDividers);
                assignAvailableGenerator(assigned2, loudToneChordChannelIndex2, targetAttenuations, targetDividers, targetGenerators, newGeneratorAttenuations, newGeneratorDividers);

                // Count the channels that could be played.
                if (loudToneChordChannelIndex0   >= 0) chordChannelPlayedFrameCounts[loudToneChordChannelIndex0]++;
                if (loudToneChordChannelIndex1   >= 0) chordChannelPlayedFrameCounts[loudToneChordChannelIndex1]++;
                if (loudToneChordChannelIndex2   >= 0) chordChannelPlayedFrameCounts[loudToneChordChannelIndex2]++;
                if (loudTunedNoiseChannelIndex   >= 0) chordChannelPlayedFrameCounts[loudTunedNoiseChannelIndex]++;
                if (loudUntunedNoiseChannelIndex >= 0) chordChannelPlayedFrameCounts[loudUntunedNoiseChannelIndex]++;

                // Collect the necessary sound commands to update the
                // generators.
                List<SoundCommand> soundFrame = new ArrayList<>();

                for (int generator = SoundCommand.TONE0;
                     generator    <= SoundCommand.NOISE;
                     generator++)
                {
                    // Does the generator still have the same attenuation?
                    int newGeneratorAttenuation = newGeneratorAttenuations[generator];

                    if (newGeneratorAttenuation != currentGeneratorAttenuations[generator])
                    {
                        // Update the current attenuation.
                        currentGeneratorAttenuations[generator] = newGeneratorAttenuation;

                        // Play the attenuation.
                        soundFrame.add(new VolumeCommand(generator,
                                                         newGeneratorAttenuation));
                    }

                    // Does the generator still have the same divider?
                    int newGeneratorDivider = newGeneratorDividers[generator];

                    if (newGeneratorDivider >= 0 &&
                        newGeneratorDivider != currentGeneratorDividers[generator])
                    {
                        // Update the current divider.
                        currentGeneratorDividers[generator] = newGeneratorDivider;

                        // Play the frequency.
                        soundFrame.add(new FrequencyCommand(generator,
                                                            newGeneratorDivider));
                    }
                }

                // Write the collected sound commands.
                soundCommandOutput.writeFrame(soundFrame.toArray(new SoundCommand[soundFrame.size()]));

                if (DEBUG)
                {
                    for (chordChannelIndex = 0; chordChannelIndex < chordChannelCount; chordChannelIndex++)
                    {
                        // Compute the loudness of the channel (actually a
                        // negative quietness).
                        int targetAttenuation = targetAttenuations[chordChannelIndex];
                        int targetDivider     = targetDividers[chordChannelIndex];

                        String generatorString =
                            chordChannelIndex == loudToneChordChannelIndex0   ? "0" :
                            chordChannelIndex == loudToneChordChannelIndex1   ? "1" :
                            chordChannelIndex == loudToneChordChannelIndex2   ? "2" :
                            chordChannelIndex == loudTunedNoiseChannelIndex   ? "N" :
                            chordChannelIndex == loudUntunedNoiseChannelIndex ? "n" :
                                                                                " ";

                        System.out.printf("%s(%1x,%03x) ", generatorString, targetAttenuation, targetDivider);
                    }

                    System.out.printf(" -> generators (%1x,%03x) (%1x,%03x) (%1x,%03x) (%1x,%1x)\n",
                                      currentGeneratorAttenuations[SoundCommand.TONE0], currentGeneratorDividers[SoundCommand.TONE0],
                                      currentGeneratorAttenuations[SoundCommand.TONE1], currentGeneratorDividers[SoundCommand.TONE1],
                                      currentGeneratorAttenuations[SoundCommand.TONE2], currentGeneratorDividers[SoundCommand.TONE2],
                                      currentGeneratorAttenuations[SoundCommand.NOISE], currentGeneratorDividers[SoundCommand.NOISE]);

                    for (int index = 0; index < soundFrame.size(); index++)
                    {
                        System.out.println("  "+soundFrame.get(index));
                    }
                }

                // Update the current time in the piece.
                currentTime += frameDuration;
            }

            if (DEBUG)
            {
                for (int channelIndex = 0; channelIndex < channels.length; channelIndex++)
                {
                    SndChannelSpecification channel = channels[channelIndex];

                    System.out.println("Channel "+channelIndex+" ["+channel.part+", "+channel.staff+", "+channel.voice+"]: final note index "+currentNoteIndices[channelIndex]+" / "+notes[channelIndex].size());
                }
            }

            System.out.println("Off-pitch statistics playing notes on the "+psgComputer+" (average, maximum, too low, too high):");
            int chordChannelIndex = 0;

            for (int channelIndex = 0; channelIndex < channels.length; channelIndex++)
            {
                SndChannelSpecification channel = channels[channelIndex];

                String header =
                    "  Channel " + channelIndex +
                    ": part '" + channel.part + "'" +
                    (channel.staff < 0       ? "" : ", staff "  + channel.staff) +
                    (channel.voice.isEmpty() ? "" : ", voice '" + channel.voice + "'") +
                    ", chord note ";

                for (int chordNoteIndex = 0; chordNoteIndex < channel.chordNotes; chordNoteIndex++)
                {
                    int noteCount = chordChannelNoteCounts[chordChannelIndex];

                    System.out.print(chordNoteIndex == 0 ? header : " ".repeat(header.length()));
                    System.out.printf("%d:\t%5.1f%%,%5.1f%%,%3.0f%%,%3.0f%%\n",
                                      chordNoteIndex,
                                      100.0 * chordChannelTotalOffPitch[chordChannelIndex]     / noteCount,
                                      100.0 * chordChannelMaximumOffPitch[chordChannelIndex],
                                      100.0 * chordChannelTooLowNoteCounts[chordChannelIndex]  / noteCount,
                                      100.0 * chordChannelTooHighNoteCounts[chordChannelIndex] / noteCount);

                    chordChannelIndex++;
                }
            }

            System.out.println();
            System.out.println("Effectively played fractions of sounds on the available generators (ideally 100%):");
            chordChannelIndex = 0;

            for (int channelIndex = 0; channelIndex < channels.length; channelIndex++)
            {
                SndChannelSpecification channel = channels[channelIndex];

                String header =
                    "  Channel " + channelIndex +
                    ": part '" + channel.part + "'" +
                    (channel.staff < 0       ? "" : ", staff "  + channel.staff) +
                    (channel.voice.isEmpty() ? "" : ", voice '" + channel.voice + "'") +
                    ", chord note ";

                for (int chordNoteIndex = 0; chordNoteIndex < channel.chordNotes; chordNoteIndex++)
                {
                    int playedCount = chordChannelPlayedFrameCounts[chordChannelIndex];
                    int soundCount  = chordChannelSoundFrameCounts[chordChannelIndex];

                    System.out.print(chordNoteIndex == 0 ? header : " ".repeat(header.length()));
                    System.out.printf("%d:\t%3.0f%%\n",
                                      chordNoteIndex,
                                      soundCount == 0 ? 0 : 100.0 * playedCount / soundCount);

                    chordChannelIndex++;
                }
            }
        }
    }


    /**
     * Checks whether the given channel is not null.
     */
    private static SndChannelSpecification check(SndChannelSpecification channel)
    {
        if (channel == null)
        {
            throw new IllegalArgumentException("Please specify '-part' first");
        }

        return channel;
    }


    /**
     * Returns the double value of the given frame frequency name or string.
     */
    private static double frameFrequency(String frameFrequency)
    {
        return switch (frameFrequency.toUpperCase())
        {
            case "NTSC" -> SN76496.NTSC_FRAME_FREQUENCY;
            case "PAL"  -> SN76496.PAL_FRAME_FREQUENCY;
            default     -> Double.parseDouble(frameFrequency);
        };
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
            // Return the XML file contents directly.
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
     * Computes the target attenuations for all chord notes of the given note,
     * starting at the given chord channel index.
     */
    private void computeTargetAttenuations(int                     chordChannelIndex,
                                           SndChannelSpecification channel,
                                           Note                    note,
                                           int                     currentAttenuation,
                                           boolean                 tiePrevious,
                                           Instrument              instrument,
                                           int                     currentNoteTime,
                                           double                  currentTime,
                                           double                  frameDuration,
                                           int[]                   targetDividers,
                                           int[]                   targetAttenuations)
    {
        // Compute the attenuation.
        int attenuation =
            clampedAttenuation(attenuation(note,
                                           currentAttenuation,
                                           tiePrevious,
                                           instrument,
                                           currentNoteTime,
                                           currentTime,
                                           frameDuration));

        boolean isRest = note.isRest();

        // Apply it to all active chord notes.
        for (int chordNote = 0; chordNote < channel.chordNotes; chordNote++)
        {
            // Apply the attenuation, or silence if there is a root note but
            // no chord note.
            targetAttenuations[chordChannelIndex] =
                note == null && !isRest ?
                    VolumeCommand.SILENT :
                        attenuation;

            if (note != null)
            {
                // Get the next note in the chord.
                note = note.chord;
            }

            // Advance to the next channel in the chord.
            chordChannelIndex++;
        }
    }


    /**
     * Computes the target dividers for all chord notes of the given note,
     * starting at the given chord channel index.
     *
     * At the same time updates the statistics about the accuracy of the
     * dividers.
     */
    private void computeTargetDividers(int                     chordChannelIndex,
                                       SndChannelSpecification channel,
                                       Note                    note,
                                       int[]                   targetDividers,
                                       int[]                   chordChannelNoteCounts,
                                       double[]                chordChannelTotalOffPitch,
                                       double[]                chordChannelMaximumOffPitch,
                                       int[]                   chordChannelTooLowNoteCounts,
                                       int[]                   chordChannelTooHighNoteCounts)
    {
        // Loop over all chord notes.
        for (int chordNote = 0; chordNote < channel.chordNotes; chordNote++)
        {
            if (note != null && !note.isRest())
            {
                // Compute and apply the target divider.
                targetDividers[chordChannelIndex] =
                    divider(channel,
                            note,
                            chordChannelNoteCounts,
                            chordChannelTotalOffPitch,
                            chordChannelMaximumOffPitch,
                            chordChannelTooLowNoteCounts,
                            chordChannelTooHighNoteCounts,
                            chordChannelIndex);

                // Get the next note in the chord.
                note = note.chord;
            }

            if (DEBUG)
            {
                if (note != null)
                {
                    System.out.printf("[%d] %s => (%03x)\n",
                                      chordChannelIndex,
                                      note,
                                      targetDividers[chordChannelIndex]);
                }
            }

            // Advance to the next channel in the chord.
            chordChannelIndex++;
        }
    }


    /**
     * Returns the attenuation of the given note at the given time.
     */
    private int attenuation(Note       note,
                            int        currentAttenuation,
                            boolean    tiePrevious,
                            Instrument instrument,
                            int        currentNoteTime,
                            double     currentTime,
                            double     frameDuration)
    {
        if (note.isRest())
        {
            // Fade out the note.
            currentAttenuation += 2;
        }
        else
        {
            // Compute the number of sound frames in this note, and
            // the index in its range.
            int noteFrameCount =
                (int)Math.round(note.duration / frameDuration);
            int noteFrameIndex =
                Math.max(0,
                Math.min(noteFrameCount - 1,
                         (int)((currentTime - currentNoteTime) / frameDuration)));

            // Determine the attenuation change.
            currentAttenuation +=
                instrument.attenuationDelta(noteFrameIndex,
                                            noteFrameCount,
                                            tiePrevious,
                                            note.tieNext);
        }

        return currentAttenuation;
    }


    /**
     * Returns the attenuation clamped to its valid range.
     */
    private int clampedAttenuation(int attenuation)
    {
        return Math.max(VolumeCommand.MAX,
               Math.min(VolumeCommand.SILENT,
                        attenuation));
    }


    /**
     * Returns the divider of the given note.
     *
     * At the same time updates the statistics about the accuracy of the
     * dividers.
     */
    private int divider(SndChannelSpecification channel,
                        Note                    note,
                        int[]                   chordChannelNoteCounts,
                        double[]                chordChannelTotalOffPitch,
                        double[]                chordChannelMaximumOffPitch,
                        int[]                   chordChannelTooLowNoteCounts,
                        int[]                   chordChannelTooHighNoteCounts,
                        int                     chordChannelIndex)
    {
        // Count the number of notes (excluding rests, including chords).
        chordChannelNoteCounts[chordChannelIndex]++;

        Instrument instrument = channel.instrument;

        // An instrument with a single note has a fixed divider.
        if (instrument.hasFixedTone())
        {
            return instrument.fixedToneDivider >>> channel.transpose;
        }

        // An untuned noise instrument doesn't have a divider.
        if (instrument.isUntunedNoise())
        {
            return 0;
        }

        // Compute the divider.
        double frequency =
            note.frequency * Math.pow(2, channel.transpose);

        int divider = instrument.isTunedNoise() ?
            psgComputer.noiseTuningDivider(frequency) :
            psgComputer.divider(frequency);

        // Collect statistics on the resulting frequency.
        checkFrequency(chordChannelNoteCounts,
                       chordChannelTotalOffPitch,
                       chordChannelMaximumOffPitch,
                       chordChannelTooLowNoteCounts,
                       chordChannelTooHighNoteCounts,
                       chordChannelIndex,
                       instrument.isTunedNoise(),
                       divider,
                       frequency);

        return divider;
    }


    /**
     * Checks the attained accuracy of the given divider compared to the given
     * target frequency, printing out a warning if necessary.
     */
    private void checkFrequency(int[]    chordChannelNoteCounts,
                                double[] chordChannelTotalOffPitch,
                                double[] chordChannelMaximumOffPitch,
                                int[]    chordChannelTooLowNoteCounts,
                                int[]    chordChannelTooHighNoteCounts,
                                int      chordChannelIndex,
                                boolean  isTunedNoise,
                                int      divider,
                                double   frequency)
    {
        // Check if the resulting frequency is sufficiently
        // close to the intended note.
        double resultingFrequency = isTunedNoise ?
            psgComputer.noiseFrequency(divider) :
            psgComputer.frequency(divider);

        double offPitch = Math.abs(frequency - resultingFrequency) / frequency;

        // Accumulate the off-pitch fraction.
        chordChannelTotalOffPitch[chordChannelIndex] += offPitch;

        // Remember the maximum off-pitch fraction.
        if (chordChannelMaximumOffPitch[chordChannelIndex] < offPitch)
        {
            chordChannelMaximumOffPitch[chordChannelIndex] = offPitch;
        }

		// For a TI-99, the tone range is 109 Hz (note A2) to inaudible,
        // and the noise range is 7.3 Hz (note A#-2) to 7457 Hz (note A#8).

        // Count the notes that are too low.
        if (divider == FrequencyCommand.MAX_TONE_DIVIDER)
        {
            chordChannelTooLowNoteCounts[chordChannelIndex]++;
        }

        // Count the notes that are too high.
        if (divider == FrequencyCommand.MIN_TONE_DIVIDER)
        {
            chordChannelTooHighNoteCounts[chordChannelIndex]++;
        }
    }


    /**
     * Returns whether all given attenuations are silences.
     */
    private boolean allSilent(int[] attenuations)
    {
        for (int index = 0; index < attenuations.length; index++)
        {
             if (attenuations[index] < VolumeCommand.SILENT)
             {
                 return false;
             }
        }

        return true;
    }


    /**
     * Tries to assign the specified channel to the same generator as before,
     * if its divider hasn't changed.
     * Returns whether it has succeeded.
     */
    private boolean assignCurrentGenerator(int   chordChannelIndex,
                                           int[] targetAttenuations,
                                           int[] targetDividers,
                                           int[] targetGenerators,
                                           int[] currentDividers,
                                           int[] attenuations,
                                           int[] dividers)
    {
        // Do we actually have a channel?
        if (chordChannelIndex >= 0)
        {
            // Try to find the target divider in the dividers.
            int generator = targetGenerators[chordChannelIndex];
            if (generator >= SoundCommand.TONE0 &&
                currentDividers[generator] == targetDividers[chordChannelIndex])
            {
                assignGenerator(chordChannelIndex,
                                generator,
                                targetAttenuations,
                                targetDividers,
                                targetGenerators,
                                attenuations,
                                dividers);

                // The channel is assigned now.
                return true;
            }

            // The channel still needs to be assigned.
            return false;
        }

        // There was no channel to be assigned.
        return true;
    }


    /**
     * Tries to assign the specified channel to an unused generator.
     */
    private void assignAvailableGenerator(boolean alreadyAssigned,
                                          int     chordChannelIndex,
                                          int[]   targetAttenuations,
                                          int[]   targetDividers,
                                          int[]   targetGenerators,
                                          int[]   attenuations,
                                          int[]   dividers)
    {
        // Has the channel already been assigned?
        if (!alreadyAssigned)
        {
            // Find an unused target divider in the dividers.
            for (int generator = SoundCommand.TONE0;
                 generator    <= SoundCommand.TONE2;
                 generator++)
            {
                if (attenuations[generator] == VolumeCommand.SILENT ||
                    dividers[generator] < 0)
                {
                    assignGenerator(chordChannelIndex,
                                    generator,
                                    targetAttenuations,
                                    targetDividers,
                                    targetGenerators,
                                    attenuations,
                                    dividers);

                    // The channel is assigned now.
                    return;
                }
            }
        }
    }


    /**
     * Assigns the specified channel to the specified generator.
     */
    private void assignGenerator(int   chordChannelIndex,
                                 int   generator,
                                 int[] targetAttenuations,
                                 int[] targetDividers,
                                 int[] targetGenerators,
                                 int[] attenuations,
                                 int[] dividers)
    {
        // Assign the new attenuation and divider to the generator.
        attenuations[generator] = targetAttenuations[chordChannelIndex];
        dividers[generator]     = targetDividers[chordChannelIndex];

        // Remember that this is also the preferred generator next time.
        targetGenerators[chordChannelIndex] = generator;
    }
}