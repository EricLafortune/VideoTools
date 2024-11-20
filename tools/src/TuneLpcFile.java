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
import sound.*;
import speech.*;

import java.io.*;

/**
 * This utility tunes a given speech file (.lpc) for the TMS5200 speech
 * synthesizer, based on the frequencies of generator 0 in a given Sound
 * file (.snd).
 *
 * Usage:
 *     java TuneLpcFile input.lpc[,start_frame[,stop_frame]] input.snd[,start_frame] [frequency_factor [min_frequency [max_frequency]]] output.lpc
 *
 * where
 *     start_frame      is the start frame in the input file (40 Hz or 50 Hz).
 *     stop_frame       is the end frame in the input file (40 Hz or 50 Hz).
 *     frequency_factor is the optional factor to scale the frequencies of the
 *                      sound file.
 *     min_frequency    is the optional minimum allowed frequency of in the
 *                      output file.
 *     max_frequency    is the optional maximum allowed frequency of in the
 *                      output file.
 *
 * For example:
 *     java TuneLpcFile input.lpc input.snd,20000 0.5 output.lpc
 *
 * @see LpcFrame
 */
public class TuneLpcFile
{
    private static final boolean DEBUG = false;

    // The pitch table of the TMS5200.
    private static final int[] INTERNAL_LPC_PITCHES = new int[]
    {
          0,  14,  15,  16,  17,  18,  19,  20,
         21,  22,  23,  24,  25,  26,  27,  28,
         29,  30,  31,  32,  34,  36,  38,  40,
         41,  43,  45,  48,  49,  51,  54,  55,
         57,  60,  62,  64,  68,  72,  74,  76,
         81,  85,  87,  90,  96,  99, 103, 107,
        112, 117, 122, 127, 133, 139, 145, 151,
        157, 164, 171, 178, 186, 194, 202, 211
    };


    public static void main(String[] args)
    throws IOException
    {
        int argIndex = 0;

        String[] inputLpc           = args[argIndex++].split(",");
        String   inputLpcFileName   = inputLpc[0];
        int      inputLpcStartFrame = inputLpc.length >= 2 ? Integer.parseInt(inputLpc[1]) : 0;
        int      inputLpcStopFrame  = inputLpc.length >= 3 ? Integer.parseInt(inputLpc[2]) : Integer.MAX_VALUE;

        String[] inputSnd           = args[argIndex++].split(",");
        String   inputSndFileName   = inputSnd[0];
        int      inputSndStartFrame = inputSnd.length >= 2 ? Integer.parseInt(inputSnd[1]) : 0;

        double   frequencyFactor    = args.length > 3 ? Double.parseDouble(args[argIndex++]) : 1.0;
        double   minFrequency       = args.length > 4 ? Double.parseDouble(args[argIndex++]) : 37.0;
        double   maxFrequency       = args.length > 5 ? Double.parseDouble(args[argIndex++]) : 534.0;

        String   outputLpcFileName  = args[argIndex++];

        try (LpcFrameInput lpcFrameInput =
                 new LpcFrameInputStream(
                 new BufferedInputStream(
                 new FileInputStream(inputLpcFileName))))
        {
            try (SoundCommandInputStream sndCommandInputStream =
                     new SoundCommandInputStream(
                     new BufferedInputStream(
                     new FileInputStream(inputSndFileName))))
            {
                try (LpcFrameOutputStream lpcFrameOutputStream =
                         new LpcFrameOutputStream(
                         new BufferedOutputStream(
                         new FileOutputStream(outputLpcFileName))))
                {
                    lpcFrameInput.skipFrames(inputLpcStartFrame);
                    sndCommandInputStream.skipFrames(inputSndStartFrame);

                    int currentSoundFrequency = (int)minFrequency;

                    int inputLpcFrameIndex = inputLpcStartFrame;
                    int inputSndFrameIndex = inputSndStartFrame;

                    while (inputLpcFrameIndex < inputLpcStopFrame)
                    {
                        if (DEBUG)
                        {
                            System.out.println("#"+inputLpcFrameIndex+","+inputSndFrameIndex+" -> "+(inputLpcFrameIndex-inputLpcStartFrame)+":");
                        }

                        SoundCommand[] soundCommands =
                            sndCommandInputStream.readFrame();

                        // Retrieve the current sound frequency, if specified
                        // (at 50 fps).
                        currentSoundFrequency =
                            soundFrequency(soundCommands,
                                           currentSoundFrequency);

                        // Update the frequency of the LPC frame, if applicable
                        // (at 40 fps).
                        if (inputSndFrameIndex++ % 5 != 0)
                        {
                            inputLpcFrameIndex++;

                            LpcFrame lpcFrame =
                                lpcFrameInput.readFrame();

                            if (lpcFrame == null)
                            {
                                break;
                            }

                            updateLpcFrame(currentSoundFrequency,
                                           frequencyFactor,
                                           minFrequency,
                                           maxFrequency,
                                           lpcFrame);

                            lpcFrameOutputStream.writeFrame(lpcFrame);
                        }
                    }
                }
            }
        }
    }


    /**
     * Extracts the sound frequency of Tone0 from the given sound commands,
     * if specified, or returns the given default sound frequency otherwise.
     */
    private static int soundFrequency(SoundCommand[] soundCommands,
                                      int            soundFrequency)
    {
        for (int index = 0; index < soundCommands.length; index++)
        {
            SoundCommand soundCommand = soundCommands[index];
            if (soundCommand instanceof FrequencyCommand)
            {
                FrequencyCommand frequencyCommand = (FrequencyCommand)soundCommand;

                if (frequencyCommand.generator == FrequencyCommand.TONE0)
                {
                    soundFrequency = frequencyCommand.divider;

                    if (DEBUG)
                    {
                        System.out.println("    Sound: "+soundFrequency+" ("+soundFrequencyInHz(soundFrequency)+" Hz)");
                    }
                }
            }
        }

        return soundFrequency;
    }


    /**
     * Updates the pitch of the given LPC frame (voiced or repeat) based on the
     * given sound frequency, if applicable.
     */
    private static void updateLpcFrame(int      soundFrequency,
                                       double   frequencyFactor,
                                       double   minFrequency,
                                       double   maxFrequency,
                                       LpcFrame lpcFrame)
    {
        if (lpcFrame instanceof LpcVoicedFrame)
        {
            LpcVoicedFrame lpcVoicedFrame = (LpcVoicedFrame)lpcFrame;

            int oldPitch = lpcVoicedFrame.pitch;

            lpcVoicedFrame.pitch =
                encodedLpcPitch(internalLpcPitch(usableFrequencyInHz(soundFrequencyInHz(soundFrequency), frequencyFactor, minFrequency, maxFrequency)));

            if (DEBUG)
            {
                System.out.println("    Pitch: "+oldPitch+" -> "+lpcVoicedFrame.pitch+" ("+lpcFrequencyInHz(INTERNAL_LPC_PITCHES[lpcVoicedFrame.pitch])+" Hz)");
            }
        }
        else if (lpcFrame instanceof LpcRepeatFrame)
        {
            LpcRepeatFrame lpcRepeatFrame = (LpcRepeatFrame)lpcFrame;
            if (lpcRepeatFrame.pitch > 0)
            {
                lpcRepeatFrame.pitch =
                    encodedLpcPitch(internalLpcPitch(usableFrequencyInHz(soundFrequencyInHz(soundFrequency), frequencyFactor, minFrequency, maxFrequency)));
            }
        }
    }


    private static double soundFrequencyInHz(int soundFrequency)
    {
        return 111860.8 / soundFrequency;
    }


    private static double usableFrequencyInHz(double frequencyInHz,
                                              double frequencyFactor,
                                              double minFrequency,
                                              double maxFrequency)
    {
        frequencyInHz *= frequencyFactor;

        while  (frequencyInHz < minFrequency)
        {
            frequencyInHz *= 2;
        }
        while  (frequencyInHz > maxFrequency)
        {
            frequencyInHz /= 2;
        }

        return frequencyInHz;
    }


    private static double lpcFrequencyInHz(int internalLpcPitch)
    {
        return 8000.0 / (internalLpcPitch + 1);
    }


    private static double internalLpcPitch(double frequencyInHz)
    {
        return 8000.0 / frequencyInHz - 1.0;
    }


    private static int encodedLpcPitch(double internalLpcPitch)
    {
        int smallerEncodedLpcPitch = smallerEncodedLpcPitch(internalLpcPitch);
        int largerEncodedLpcPitch  = smallerEncodedLpcPitch + 1;

        return
            largerEncodedLpcPitch >= INTERNAL_LPC_PITCHES.length ?
                INTERNAL_LPC_PITCHES.length - 1 :
            internalLpcPitch - INTERNAL_LPC_PITCHES[smallerEncodedLpcPitch] <
            INTERNAL_LPC_PITCHES[largerEncodedLpcPitch] - internalLpcPitch ?
                smallerEncodedLpcPitch :
                largerEncodedLpcPitch;
    }


    private static int smallerEncodedLpcPitch(double internalLpcPitch)
    {
        for (int index = 1; index < INTERNAL_LPC_PITCHES.length; index++)
        {
            if (INTERNAL_LPC_PITCHES[index] >= internalLpcPitch)
            {
                return index - 1;
            }
        }

        return INTERNAL_LPC_PITCHES.length - 1;
    }
}
