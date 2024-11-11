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

import speech.LpcFrame;

/**
 * This ChannelSpecification specifies of a channel targeting the speech
 * synthesizer.
 */
public class LpcChannelSpecification
extends      ChannelSpecification
{
    public LpcFrame[] hum;


    public LpcChannelSpecification()
    {
        super();
    }


    public LpcChannelSpecification(String part)
    {
        super(part);
    }


    public LpcChannelSpecification(String part,
                                   String voice)
    {
        super(part, voice);
    }


    public LpcChannelSpecification(String part,
                                   String voice,
                                   int    staff)
    {
        super(part, voice, staff);
    }


    public LpcChannelSpecification(String     part,
                                   String     voice,
                                   int        staff,
                                   LpcFrame[] hum)
    {
        super(part, voice, staff);

        this.hum = hum;
    }
}
