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

/**
 * This class represents a (scientific view of a) note.
 */
public class Note
{
    /**
     * The index of the channel of the note.
     */
    public int     channelIndex;

    /**
     * The duration, expressed in milliseconds.
     */
    public int     duration;

    /**
     * The attenuation (emphasis):
     *   0 for the first beat of the measure,
     *   1 for every third beat of a measure in 3/6/9/12 beats.
     *   2 for every other beat.
     */
    public int     attenuation;

    /**
     * The frequency, expressed in Hz.
     */
    public double  frequency;

    /**
     * A flag whether this note should tie into the next note.
     */
    public boolean tieNext;

    /**
     * A linked list of notes in the same chord.
     */
    public Note    chord;


    /**
     * Creates a new instance.
     */
    public Note()
    {
    }


    /**
     * Creates a new instance.
     */
    public Note(int channelIndex, int duration)
    {
        this.channelIndex = channelIndex;
        this.duration     = duration;
    }


    /**
     * Returns whether the note is actually a rest.
     */
    public boolean isRest()
    {
        return frequency == 0.0;
    }


    /**
     * Returns the number of notes in this chord, or just 1 if it's a simple
     * note.
     */
    public int chordNoteCount()
    {
        return chord == null ? 1 : chord.chordNoteCount() + 1;
    }


    // Implementations for Object.

    public String toString()
    {
        return "Note(channel "+channelIndex+", "+duration+" ms, "+(frequency==0.0?"rest":attenuation+" dB, "+frequency+" Hz")+(tieNext?", tie next":"")+(chord != null?", "+chordNoteCount()+" notes":"")+")";
    }
}
