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
package video;

import java.io.IOException;

/**
 * This interface provide methods to write output in our custom TMS video
 * format, containing any combination of animation, sound, and speech.
 *
 * This format contains an optimized stream of bytes with chunks that can be
 * sent to the video display processor (TMS9918), to the sound processor
 * (TMS9919 or SN76489), and to the speech synthesizer (TMS5200). It also
 * contains optional markers to wait for a vertical sync in the video output
 * and to switch to the next memory bank in the source stream.
 */
public interface VideoOutputStream
extends          AutoCloseable
{
    public static final int   SOUND_SIZE_DELTA  = 0xffe0;
    public static final int   SPEECH_SIZE_DELTA = 0xffd0;
    public static final short VSYNC             = (short)0xffcf;
    public static final short NEXT_BANK         = (short)0xffce;
    public static final short EOF               = (short)0xffcd;


    public int getVsyncCount();

    public int getBankCount();

    public int getBankByteCount();

    public void writeComment0(String comment)
    throws IOException;

    public void writeComment(String comment)
    throws IOException;

    public void startDisplayFragment(int address, int size)
    throws IOException;

    public void writeDisplayData(byte data)
    throws IOException;

    public void writeDisplayData(long data)
    throws IOException;

    public void endDisplayFragment()
    throws IOException;

    public void writeSoundData(byte[] soundData)
    throws IOException;

    public void writeSpeechData(byte[] speechData)
    throws IOException;

    public void writeVSync()
    throws IOException;

    public void close()
    throws IOException;
}
