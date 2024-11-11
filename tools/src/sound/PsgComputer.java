/*
 * Video tools for the TI-99/4A home computer.
 *
 * This file was derived from sn76496.cpp with a BSD-3-Clause in Mame:
 *
 * Copyright (c) Nicola Salmoria
 *
 * Conversion to Java, modification, and cleanup:
 *
 * Copyright (c) 2024 Eric Lafortune
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
package sound;

/**
 * This enum represents computers that contain variants of the SN76496 sound
 * chip.
 */
public enum PsgComputer
{
	TI99        (PsgChip.SN76496,  SN76496.NTSC_CLOCK_FREQUENCY),
	COLECOVISION(PsgChip.SN76496,  SN76496.NTSC_CLOCK_FREQUENCY),
	BBC         (PsgChip.SN76496,  SN76496.PAL_CLOCK_FREQUENCY ),
	GAMEGEAR    (PsgChip.GAMEGEAR, SN76496.NTSC_CLOCK_FREQUENCY),
	SMS         (PsgChip.SEGAPSG,  SN76496.NTSC_CLOCK_FREQUENCY);


	public final PsgChip psgChip;
	public final double  clockFrequency;


	/**
	 * Creates a new instance.
	 */
	PsgComputer(PsgChip psgChip,
				double  clockFrequency)
	{
		this.psgChip        = psgChip;
		this.clockFrequency = clockFrequency;
	}


	/**
	 * Returns the frequency of the samples produced by the simulated sound chip.
	 *
	 * @see SN76496
	 */
	public double sampleFrequency()
	{
		// Typically the clock frequency divided by 16.
		return clockFrequency / (2 * psgChip.clockDivider);
	}


	/**
	 * Returns the divider to set to a tone generator of the sound chip to
	 * obtain the given sound frequency.
	 */
	public int divider(double frequency)
	{
		// The divider corresponds to a half-period.
		return
			Math.max(FrequencyCommand.MIN_TONE_DIVIDER,
			Math.min(FrequencyCommand.MAX_TONE_DIVIDER,
				     (int)Math.round(0.5 * sampleFrequency() / frequency)));
	}


	/**
	 * Returns the divider to set to tone generator 2 of the sound chip to
	 * obtain the given sound frequency for the tuned noise generator.
	 */
	public int noiseTuningDivider(double frequency)
	{
		return divider(frequency *
					   (psgChip.feedbackMask == 0x8000 ? 16.0 : 15.0));
	}


	/**
	 * Returns the resulting sound frequency when setting the given divider to
	 * a tone generator of the sound chip.
	 */
	public double frequency(int divider)
	{
		// The divider corresponds to a half-period.
		// For a TI-99, this is "111861 / divider", resulting in a range
		// from 109 Hz (note A2) to inaudible.
		return sampleFrequency() / (2.0 * divider);
	}


	/**
	 * Returns the resulting sound frequency of the tuned noise generator when
	 * setting the given divider to tone generator 2 of the sound chip.
	 */
	public double noiseFrequency(int tuningDivider)
	{
		// For a TI-99, this is "7457 / divider", resulting in a range
		// from 7.3 Hz (note A#-2) to 7457 Hz (note A#8).
		return frequency(tuningDivider) / (psgChip.feedbackMask == 0x8000 ? 16.0 : 15.0);
	}
}
