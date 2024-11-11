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
 * This enum represents the variants of the SN76496 sound chip.
 */
public enum PsgChip
{
	SN76496 (0x10000, 0x04, 0x08, false, false, 8, false, true ),
	Y2404   (0x10000, 0x04, 0x08, false, false, 8, false, true ),
	SN76489 ( 0x4000, 0x01, 0x02, true,  false, 8, false, true ),
	SN76489A(0x10000, 0x04, 0x08, false, false, 8, false, true ),
	SN76494 (0x10000, 0x04, 0x08, false, false, 1, false, true ),
	SN94624 ( 0x4000, 0x01, 0x02, true,  false, 1, false, true ),
	NCR8496 ( 0x8000, 0x02, 0x20, true,  false, 8, true,  true ),
	PSSJ3   ( 0x8000, 0x02, 0x20, false, false, 8, true,  true ),
	GAMEGEAR( 0x8000, 0x01, 0x08, true,  true,  8, false, false),
	SEGAPSG ( 0x8000, 0x01, 0x08, true,  false, 8, false, false);


	public final int     feedbackMask;   // Mask for feedback.
	public final int     whiteNoiseTap1; // Mask for white noise tap 1 (higher one, usually bit 14).
	public final int     whiteNoiseTap2; // Mask for white noise tap 2 (lower one, usually bit 13).
	public final boolean negate;         // Output negate flag.
	public final boolean stereo;         // Whether we're dealing with stereo or not.
	public final int     clockDivider;   // Clock divider.
	public final boolean ncrStylePsg;    // Flag to ignore writes to regs 1,3,5,6,7 with bit 7 low.
	public final boolean segaStylePsg;   // Flag to make frequency zero acts as if it is one more than max (0x3ff+1) or if it acts like 0; the initial register is pointing to 0x3 instead of 0x0; the volume reg is preloaded with 0xF instead of 0x0.


	PsgChip(int     feedbackMask,
			int     whiteNoiseTap1,
			int     whiteNoiseTap2,
			boolean negate,
			boolean stereo,
			int     clockDivider,
			boolean ncrStylePsg,
			boolean segaStylePsg)
	{
		this.feedbackMask   = feedbackMask;
		this.whiteNoiseTap1 = whiteNoiseTap1;
		this.whiteNoiseTap2 = whiteNoiseTap2;
		this.negate         = negate;
		this.stereo         = stereo;
		this.clockDivider   = clockDivider;
		this.ncrStylePsg    = ncrStylePsg;
		this.segaStylePsg   = segaStylePsg;
	}
}
