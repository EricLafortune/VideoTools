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
 * This class specifies of a channel from a MusicXML file: a subset of the
 * music sheet that contains a single sequence of notes. It includes optional
 * modifications like attenuation and transposition.
 */
public class ChannelSpecification
{
    public String part;
    public String voice;
    public int    staff;
    public int    chordNotes;

    public int attenuate;
    public int transpose;


    public ChannelSpecification()
    {
        this("P1");
    }

    public ChannelSpecification(String part)
    {
        this(part,
             "1");
    }


    public ChannelSpecification(String part,
                                String voice)
    {
        this(part,
             voice,
             -1);
    }


    public ChannelSpecification(String part,
                                String voice,
                                int    staff)
    {
        this(part,
             voice,
             staff,
             1);
    }


    public ChannelSpecification(String part,
                                String voice,
                                int    staff,
                                int    chordNotes)
    {
        this.part       = part;
        this.voice      = voice;
        this.staff      = staff;
        this.chordNotes = chordNotes;
    }
}
