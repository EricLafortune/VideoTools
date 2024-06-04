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
package sound;

import static sound.FrequencyCommand.*;
import static sound.VolumeCommand.*;

/**
 * This class parses and returns sound commands for the TMS9919 / SN76489 sound
 * chip.
 *
 * @see SoundCommand
 */
class SoundCommandParser
{
    /**
     * Parses, collects, and returns all sound commands from the given sound
     * data.
     */
    public SoundCommand[] parseSoundCommands(byte[] soundData)
    {
        // Collect the sound command.
        SoundCommand[] soundCommands     = new SoundCommand[32];
        int            soundCommandCount = 0;

        for (int index = 0; index < soundData.length;)
        {
            byte b = soundData[index++];

            SoundCommand soundCommand;

            int commandNibble = b & 0xf0;
            switch (commandNibble)
            {
                case TONE0_FREQUENCY:
                case TONE1_FREQUENCY:
                case TONE2_FREQUENCY:
                {
                    int generator = ((commandNibble - TONE0_FREQUENCY) >>> 5) + SoundCommand.TONE0;
                    int frequency = ((soundData[index++] & 0x3f) << 4) | (b & 0x0f);

                    soundCommand = new FrequencyCommand(generator, frequency);
                    break;
                }
                case NOISE_FREQUENCY:
                {
                    int generator = SoundCommand.NOISE;
                    int frequency = b & 0x0f;

                    soundCommand = new FrequencyCommand(generator, frequency);
                    break;
                }
                case TONE0_VOLUME:
                case TONE1_VOLUME:
                case TONE2_VOLUME:
                case NOISE_VOLUME:
                {
                    int generator = ((commandNibble - TONE0_VOLUME) >>> 5) + SoundCommand.TONE0;
                    int volume    = b & 0x0f;

                    soundCommand = new VolumeCommand(generator, volume);
                    break;
                }
                default:
                {
                    throw new IllegalArgumentException(String.format("Unknown sound byte (0x%02x)", b));
                }
            }

            soundCommands[soundCommandCount++] = soundCommand;
        }

        // Trim the sound command.
        SoundCommand[] trimmedSoundCommand = new SoundCommand[soundCommandCount];
        System.arraycopy(soundCommands, 0,
                         trimmedSoundCommand, 0, soundCommandCount);

        return trimmedSoundCommand;
    }
}
