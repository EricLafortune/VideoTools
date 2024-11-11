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
package musicxml;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.*;
import java.util.*;

/**
 * This class parses and returns notes from an input stream in the
 * MusicXML (.xml) format.
 */
public class MusicXmlInputStream
implements   AutoCloseable
{
    private static final int MILLISECONDS_PER_MINUTE = 60000;

    private static final boolean DEBUG = false;


    private final int                    startMeasure;
    private final int                    endMeasure;
    private final ChannelSpecification[] channels;
    private final Reader                 inputReader;
    private final XMLEventReader         reader;

    private HashSet<String>                          parts                   = new HashSet<>();
    private HashMap<String,String>                   partIDs                 = new HashMap<>();
    private HashMap<String,HashMap<String,Integer>>  partVoiceChannelIndices = new HashMap<>();
    private HashMap<String,HashMap<Integer,Integer>> partStaffChannelIndices = new HashMap<>();

    private int                   skipNestedCounter;

    private List<Integer>         metronomePerMinutes = new ArrayList<>();

    private String                currentPartID;
    private Map<String, Integer>  currentVoiceChannelIndices;
    private Map<Integer, Integer> currentStaffChannelIndices;
    private int                   currentMeasureNumber;
    private int                   currentMeasureTime;
    private boolean               currentRepeatStoring;
    private int                   currentDivisions = 1;
    private int                   currentBeats;
    private int                   currentBeatType;
    private String                currentMetronomeBeatUnit;
    private int                   currentMetronomePerMinute = 60;
    private int                   currentBackup;
    private Note                  currentNote;
    private Note                  previousNote;
    private List<Note>            currentRepeatNotes = new ArrayList<>();
    private int                   currentRepeatNoteIndex;
    private boolean               currentChord;
    private boolean               currentTie;
    private boolean               currentTieChange;
    private boolean[]             currentTies;


    /**
     * Creates a new instance that reads a single channel of notes (part,
     * voice, staff) from the given reader.
     */
    public MusicXmlInputStream(int                  startMeasure,
                               int                  endMeasure,
                               ChannelSpecification channel,
                               Reader               inputReader)
    throws XMLStreamException
    {
        this(startMeasure,
             endMeasure,
             new ChannelSpecification[] { channel },
             inputReader);
    }

    /**
     * Creates a new instance that reads one or more channels of notes (part,
     * voice, staff) from the given reader.
     */
    public MusicXmlInputStream(int                    startMeasure,
                               int                    endMeasure,
                               ChannelSpecification[] channels,
                               Reader                 inputReader)
    throws XMLStreamException
    {
        this.startMeasure = startMeasure;
        this.endMeasure   = endMeasure;
        this.channels     = channels;
        this.inputReader  = inputReader;

        XMLInputFactory factory =
            XMLInputFactory.newInstance();

        // Disable all external access from the XML reader.
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
        factory.setXMLResolver(new XMLResolver()
        {
            public Object resolveEntity(String publicID,
                                        String systemID,
                                        String baseURI,
                                        String namespace)
            throws XMLStreamException
            {
                return systemID.endsWith("/partwise.dtd") ?
                    new ByteArrayInputStream(new byte[0]) :
                    null;
            }
        });

        this.reader = factory.createXMLEventReader(inputReader);

        // Collect the channel IDs/names/abbreviations/instruments in a set,
        // for efficient checks later on.
        Arrays.stream(channels).forEach(channel -> parts.add(channel.part));

        // Parse the "part-list" header of the document.
        if (!parseDocument("part-list"))
        {
            throw new IllegalArgumentException("Can't find part list");
        }

        // Fill out the nested maps to efficiently look up the channel
        // specifications later on:
        //   part ID -> voice -> channel specification index.
        //   part ID -> staff -> channel specification index.
        for (int index = 0; index < channels.length; index++)
        {
            ChannelSpecification channel = channels[index];

            String partID = partIDs.get(channel.part);

            if (partID == null)
            {
                throw new IllegalArgumentException("Can't find part ["+channel.part+"]");
            }

            if (channel.voice != null && channel.voice.length() > 0)
            {
                partVoiceChannelIndices
                    .computeIfAbsent(partID, key -> new HashMap<>())
                    .put(channel.voice, index);
            }
            else
            {
                partStaffChannelIndices
                    .computeIfAbsent(partID, key -> new HashMap<>())
                    .put(channel.staff, index);
            }
        }

        // Ties can cross measures, with interleaved staffs, so we need to
        // keep track of them for each channel.
        currentTies = new boolean[channels.length];
    }


    /**
     * Reads and returns a note from the input, or null.
     */
    public Note readNote()
    throws XMLStreamException
    {
        // Do we have repeat notes that we must return first?
        if (!currentRepeatStoring &&
            !currentRepeatNotes.isEmpty())
        {
            Note note = currentRepeatNotes.get(currentRepeatNoteIndex++);

            // Was this the last one?
            if (currentRepeatNoteIndex >= currentRepeatNotes.size())
            {
                // Clear all repeat notes.
                currentRepeatNotes.clear();
                currentRepeatNoteIndex = 0;
            }

            if (DEBUG)
            {
                System.out.println("  "+note+" (repeated)");
            }

            return note;
        }

        // Do we have a current note that we still must return
        // (possibly stored after a repeat section)?
        if (currentNote != null)
        {
            if (DEBUG)
            {
                System.out.println("  "+currentNote+" (after repeat)");
            }

            // Remember, then clear the current note.
            previousNote = currentNote;
            currentNote  = null;

            return previousNote;
        }

        // Try to read a new note.
        while (parseDocument("forward", "note") &&
               currentNote != null              &&
               currentChord)
        {
            // It's still a chord note.
            if (DEBUG)
            {
                System.out.println("  "+currentNote+" (chord)");
            }

            // Add the chord note to the end of the chord list.
            previousNote.chord = currentNote;
            previousNote       = currentNote;
            currentNote        = null;
        }

        // Do we have a new note that we can complete?
        if (currentNote != null)
        {
            // Update the tie state of the channel, if necessary,
            if (currentTieChange)
            {
                currentTies[currentNote.channelIndex] = currentTie;
                currentTieChange = false;
            }

            // Set the tie state of the note.
            currentNote.tieNext = currentTies[currentNote.channelIndex];
        }

        // Do we have a new note that we should remember for repeating?
        if (currentRepeatStoring &&
            currentNote != null)
        {
            currentRepeatNotes.add(currentNote);
        }

        // Do we have repeat notes that we must return first?
        if (!currentRepeatStoring &&
            !currentRepeatNotes.isEmpty())
        {
            Note note = currentRepeatNotes.get(currentRepeatNoteIndex++);

            // Was this the last one?
            if (currentRepeatNoteIndex >= currentRepeatNotes.size())
            {
                // Clear all repeat notes.
                currentRepeatNotes.clear();
                currentRepeatNoteIndex = 0;
            }

            if (DEBUG)
            {
                System.out.println("  "+note+" (first repeated)");
            }

            return note;
        }

        // Do we have a new note that we can return?
        if (currentNote != null)
        {
            if (DEBUG)
            {
                System.out.println("  "+currentNote);
            }

            // Remember, then clear the current note.
            previousNote = currentNote;
            currentNote  = null;

            return previousNote;
        }

        return null;
    }


    /**
     * Parses the document, up to and including the specified element or elements.
     * Returns whether the element was found.
     */
    private boolean parseDocument(String... untilElement)
    throws XMLStreamException
    {
        while (reader.hasNext())
        {
            XMLEvent event = reader.nextEvent();

            if  (DEBUG)
            {
                System.out.println("["+skipNestedCounter+"] "+event.getClass().getSimpleName()+": "+event.toString().replace('\n', ' '));
            }

            if (event.isStartElement())
            {
                // Parse the start element, even if it is skipped.
                parseAnyStartElement(reader,
                                     event.asStartElement());

                // Parse the start element, if it is not skipped.
                if (skipNestedCounter > 0 ||
                    !parseStartElement(reader,
                                       event.asStartElement()))
                {
                    skipNestedCounter++;
                }
            }
            else if (event.isEndElement())
            {
                if (skipNestedCounter == 0)
                {
                    // Have we reached the target end element?
                    String endElementName = event.asEndElement().getName().getLocalPart();

                    if (Arrays.binarySearch(untilElement, endElementName) >= 0)
                    {
                        return true;
                    }
                }
                else
                {
                    // We have skipped a start/end element pair.
                    skipNestedCounter--;
                }
            }
        }

        return false;
    }


    /**
     * Extracts useful global information from any given start element, even
     * if it is not part of specified channels.
     */
    private void parseAnyStartElement(XMLEventReader reader,
                                      StartElement   startElement)
    throws XMLStreamException
    {
        switch (startElement.getName().getLocalPart())
        {
            case     "measure"            : currentMeasureNumber      = intAttribute(startElement, "number"); if (metronomePerMinutes.size() < currentMeasureNumber) metronomePerMinutes.add(currentMetronomePerMinute); break;
            case             "per-minute" : currentMetronomePerMinute = intText(reader);                                                                             metronomePerMinutes.set(currentMeasureNumber-1, currentMetronomePerMinute); break;
        }
    }


    /**
     * Extracts information from the given start element that is part of
     * specified channels. Returns whether it sub-elements should be parsed.
     */
    private boolean parseStartElement(XMLEventReader reader,
                                      StartElement   startElement)
    throws XMLStreamException
    {
        switch (startElement.getName().getLocalPart())
        {
            // The header, with all part declarations.
            case "score-partwise"           : return true;
            case   "part-list"              : return true;
            case     "score-part"           : insertPartID(currentPartID = attribute(startElement, "id")); return true;
            case       "part-name",
                       "part-abbreviation"  : insertPartID(text(reader)); return false;
            case       "score-instrument"   : return true;
            case         "instrument-name"  : insertPartID(text(reader)); return false;

            // All score parts.
            case   "part"                   : return parsePartElement(startElement);
            case     "measure"              : return parseMeasureElement(startElement);
            case       "barline"            : return true;
            case         "repeat"           : currentRepeatStoring = attribute(startElement, "direction").equals("forward"); return false;
            case       "attributes"         : return true;
            case         "divisions"        : currentDivisions = intText(reader); return false;
            case         "time"             : return true;
            case           "beats"          : currentBeats    = intText(reader); return false;
            case           "beat-type"      : currentBeatType = intText(reader); return false;
            case       "direction"          : return true;
            case         "direction-type"   : return true;
            case           "metronome"      : return true;
            case             "beat-unit"    : currentMetronomeBeatUnit  = text(reader); return false;
            case             "per-minute"   : currentMetronomePerMinute = intText(reader); return false;
            case       "backup"             : currentMeasureTime = 0; return false;
            // TODO: The "forward" note (rest) needs to be duplicated and end up in all the corresponding channels.
            case       "forward"            : currentNote = new Note(); if (currentVoiceChannelIndices != null) currentNote.channelIndex = currentVoiceChannelIndices.values().iterator().next(); currentChord = false; return true;
          // Handled like an element of "note".
          //case         "duration"         : ...
          //case         "staff"            : ...
            case       "note"               : currentNote = new Note(); currentNote.attenuation = attenuation(currentMeasureTime); currentChord = false; return true;
            case         "chord"            : currentChord = true; return false;
            case         "pitch"            : currentNote.frequency = 16.35160156; return true;
            case           "step"           : currentNote.frequency *= Math.pow(2.0, noteNumber(text(reader)) / 12.0); return false;
            case           "alter"          : currentNote.frequency *= Math.pow(2.0, intText(reader) / 12.0); return false;
            case           "octave"         : currentNote.frequency *= Math.pow(2.0, intText(reader)); return false;
            case         "unpitched"        : currentNote.frequency = 16.35160156; return true;
          //case           "display-step"   : currentNote.frequency *= Math.pow(2.0, noteNumber(text(reader)) / 12.0); return false;
          //case           "display-octave" : currentNote.frequency *= Math.pow(2.0, intText(reader)); return false;
            case         "rest"             : currentNote.frequency = 0.0; return false;
            case         "tie"              : currentTie = attribute(startElement, "type").equals("start"); currentTieChange = true; return false;
            // "duration" and "staff" inside "forward" or "note".
            case         "duration"         : return parseDuration(reader);
            case         "staff"            : return parseStaffElement(reader);
            case         "voice"            : return parseVoiceElement(reader);
            case         "notations"        : return true;
            case           "articulations"  : return true;
            case             "accent",
                             "strong-accent",
                             "stress",
                             "spiccato",
                             "staccato",      // These should also affect the sustained note.
                             "staccatissimo",
                             "tenuto"       : currentNote.attenuation = 0; return false;
            case             "soft-accent"  : currentNote.attenuation = 1; return false;
            case             "unstress"     : currentNote.attenuation = 2; return false;
          // Already accounted for in the duration, although the beat attenuation is unclear.
          //case         "time-modification": return true;
          //case           "actual-notes"   : if (currentNote != null) currentNote.duration /= intText(reader); return false;
          //case           "normal-notes"   : if (currentNote != null) currentNote.duration *= intText(reader); return false;
            default                         : return false;
        }
    }


    /**
     * Links the given part name to the current part ID, if the part name is
     * one of the configured parts.
     */
    private void insertPartID(String partName)
    {
        if (parts.contains(partName))
        {
            partIDs.putIfAbsent(partName, currentPartID);
        }
    }


    /**
     * Extracts information from the given "part" start element.
     * Returns whether it sub-elements should be parsed.
     */
    private boolean parsePartElement(StartElement startElement)
    {
        currentPartID = attribute(startElement, "id");

        // Does the part have specified voices or staffs?
        currentVoiceChannelIndices = partVoiceChannelIndices.get(currentPartID);
        currentStaffChannelIndices = partStaffChannelIndices.get(currentPartID);

        return currentVoiceChannelIndices != null ||
               currentStaffChannelIndices != null;
    }


    /**
     * Extracts information from the given "measure" start element.
     * Returns whether it sub-elements should be parsed.
     */
    private boolean parseMeasureElement(StartElement startElement)
    {
        currentMeasureNumber = intAttribute(startElement, "number");

        currentMeasureTime = 0;

        return currentMeasureNumber >= startMeasure &&
               currentMeasureNumber <= endMeasure;
    }


    /**
     * Extracts information from the given "duration" start element.
     * Returns whether it sub-elements should be parsed.
     */
    private boolean parseDuration(XMLEventReader reader)
    throws XMLStreamException
    {
        int duration = intText(reader);

        currentNote.duration = duration(duration);

        if (!currentChord)
        {
            currentMeasureTime += duration;
        }

        return false;
    }


    /**
     * Extracts information from the given "voice" start element.
     * Returns whether it sub-elements should be parsed.
     */
    private boolean parseVoiceElement(XMLEventReader reader)
    throws XMLStreamException
    {
        assignChannelIndex(currentVoiceChannelIndices,
                           text(reader));

        return false;
    }


    /**
     * Extracts information from the given "staff" start element.
     * Returns whether it sub-elements should be parsed.
     */
    private boolean parseStaffElement(XMLEventReader reader)
    throws XMLStreamException
    {
        assignChannelIndex(currentStaffChannelIndices,
                           intText(reader));

        return false;
    }


    /**
     * Assigns the named channel index from the given map to the current note,
     * or skips the note if the name is not in the map.
     */
    private <T> void assignChannelIndex(Map<T, Integer> nameChannelIndices,
                                        T               name)
    {
        // Avoid the "staff" element inside the "direction" element.
        if (currentNote != null && nameChannelIndices != null)
        {
            Integer channelIndex = nameChannelIndices.get(name);
            if (channelIndex != null)
            {
                currentNote.channelIndex = channelIndex;
            }
            else
            {
                skipNote();
            }
        }
    }


    /**
     * Returns the specified integer attribute of the given start element.
     */
    private static int intAttribute(StartElement startElement, String name)
    {
        return Integer.parseInt(attribute(startElement, name));
    }


    /**
     * Returns the specified attribute of the given start element.
     */
    private static String attribute(StartElement startElement, String name)
    {
        return startElement.getAttributeByName(new QName(name)).getValue();
    }


    /**
     * Returns the integer text of the current start element, without affecting
     * the event reader.
     */
    private static int intText(XMLEventReader reader)
    throws XMLStreamException
    {
        return Integer.parseInt(text(reader));
    }


    /**
     * Returns the text of the current start element, without affecting
     * the event reader.
     */
    private static String text(XMLEventReader reader) throws XMLStreamException
    {
        XMLEvent peek = reader.peek();

        return peek.isCharacters() ? peek.asCharacters().getData() : "";
    }


    /**
     * Returns the number of the given note: C=0, C#=1, D=2,..., B=11.
     */
    private int noteNumber(String note)
    {
        return switch (note.charAt(0))
        {
            case 'C'  ->  0;
        //  case 'C#' ->  1;
            case 'D'  ->  2;
        //  case 'D#' ->  3;
            case 'E'  ->  4;
            case 'F'  ->  5;
        //  case 'F#' ->  6;
            case 'G'  ->  7;
        //  case 'G#' ->  8;
            case 'A'  ->  9;
        //  case 'A#' -> 10;
            case 'B'  -> 11;
            default -> throw new IllegalArgumentException("Unknown note ["+note+"]");
        };
    }


    /**
     * Returns the duration in milliseconds for the note duration in the
     * current settings.
     */
    private int duration(int duration)
    {
        int metronomePerMinute = metronomePerMinutes.get(currentMeasureNumber-1);

        // Compute the rounded duration as a difference between to rounded
        // measure times, to avoid rounding errors that would accumulate.
        return
            MILLISECONDS_PER_MINUTE * (currentMeasureTime + duration) / currentDivisions / metronomePerMinute -
            MILLISECONDS_PER_MINUTE *  currentMeasureTime             / currentDivisions / metronomePerMinute;
    }


    /**
     * Returns the attenuation (0, 1, or 2) of the given time in a measure,
     * expressed in the number of divisions.
     */
    private int attenuation(int measureTime)
    {
        return
            measureTime == 0                                                 ? 0 : // First beat of the measure.
            currentBeats % 3 == 0 && measureTime % (3*currentDivisions) == 0 ? 1 : // For 3,6,9,12 beats, every third beat.
                                                                               2;  // All other beats.
    }


    /**
     * Skips a note, while immediately inside one of its elements.
     */
    private void skipNote()
    {
        currentNote       = null;
        currentTie        = false;
        skipNestedCounter = 1;
    }


    // Implementation for AutoCloseable.

    public void close() throws IOException
    {
        inputReader.close();
    }


    /**
     * Prints out the notes of the specified part of the MusicXML file.
     */
    public static void main(String[] args)
    throws IOException, XMLStreamException
    {
        String inputFileName = args[0];
        String part          = args.length < 2 ? "P1"              : args[1];
        String voice         = args.length < 3 ? "1"               : args[2];
        int    staff         = args.length < 4 ? -1                : Integer.parseInt(args[3]);
        int    startMeasure  = args.length < 5 ? 1                 : Integer.parseInt(args[4]);
        int    endMeasure    = args.length < 6 ? Integer.MAX_VALUE : Integer.parseInt(args[5]);

        try (MusicXmlInputStream musicXmlInputStream =
                 new MusicXmlInputStream(startMeasure, endMeasure, new ChannelSpecification(part, voice, staff),
                 new BufferedReader(
                 new FileReader(inputFileName))))
        {
            int counter = 0;
            int time    = 0;

            Note note;
            while ((note = musicXmlInputStream.readNote()) != null)
            {
                // Print out the root note.
                System.out.print("["+counter+"] ("+time+") "+note);

                counter++;
                time += note.duration;

                // Print out any chord notes.
                while (note.chord != null)
                {
                    note = note.chord;
                    System.out.print(", "+note);
                }
                System.out.println();
            }

            System.out.println("Total time = "+time);
        }
    }
}