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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/**
 * This utility packages a given input video file in our optimized video format
 * (.tms) in an application in the Mame cartridge format (.rpk) or the raw ROM
 * format (romc.bin), for the TI-99/4A and its emulators.
 *
 * Usage:
 *     java PackageVideoInCartridge [options...] input.tms output.{rpk,bin}
 */
public class PackageVideoInCartridge
{
    private static final String DEFAULT_TITLE                = "VIDEO WITH SPEECH";
    private static final String DEFAULT_TITLE_WITHOUT_SPEECH = "VIDEO WITHOUT SPEECH";

    private static final int MAX_TITLE_LENGTH = 31;
    private static final int MEMORY_BANK_SIZE = 0x2000;
    private static final int HEADER_SIZE      = 0x0054;


    private final String title;
    private final String titleWithoutSpeech;


    public static void main(String[] args)
    throws IOException
    {
        int argIndex = 0;

        String title              = DEFAULT_TITLE;
        String titleWithoutSpeech = DEFAULT_TITLE_WITHOUT_SPEECH;

        while (args[argIndex].startsWith("-"))
        {
            switch (args[argIndex++])
            {
                case "-title"              -> { title              = args[argIndex++]; titleWithoutSpeech = null; }
                case "-titlewithoutspeech" -> { titleWithoutSpeech = args[argIndex++]; }
                default                    -> throw new IllegalArgumentException("Unknown option [" + args[argIndex-1] + "]");
            }
        }

        String inputTmsFileName = args[argIndex++];
        String outputFileName   = args[argIndex++];

        new PackageVideoInCartridge(title,
                                    titleWithoutSpeech)
            .process(inputTmsFileName,
                     outputFileName);
    }


    /**
     * Creates a new instance with the given settings.
     */
    public PackageVideoInCartridge(String title,
                                   String titleWithoutSpeech)
    {
        this.title              = title;
        this.titleWithoutSpeech = titleWithoutSpeech;
    }


    /**
     * Processes the specified MusicTms file to the specified LPC file.
     */
    private void process(String inputTmsFileName,
                         String outputFileName)
    throws IOException
    {
        // Should we write an RPK file or a raw ROM file?
        if (outputFileName.endsWith(".rpk"))
        {
            // Write the output RPK file (a zip file).
            try (ZipOutputStream zipOutputStream =
                     new ZipOutputStream(
                     new BufferedOutputStream(
                     new FileOutputStream(outputFileName))))
            {
                // Write the layout zip entry.
                zipOutputStream.putNextEntry(new ZipEntry("layout.xml"));
                writeFile("layout.xml", zipOutputStream);

                // Write the ROM zip entry.
                zipOutputStream.putNextEntry(new ZipEntry("romc.bin"));
                writeRom("romc.bin", inputTmsFileName, zipOutputStream);

                zipOutputStream.finish();
            }
        }
        else
        {
            // Write the raw output ROM file.
            try (OutputStream outputStream =
                     new BufferedOutputStream(
                     new FileOutputStream(outputFileName)))
            {
                writeRom("romc.bin", inputTmsFileName, outputStream);
            }
        }
    }


    /**
     * Copies the specified resource file to the given output stream.
     */
    private void writeFile(String      inputResourceFileName,
                          OutputStream outputStream)
    throws IOException
    {
        // Copy the file.
        try (BufferedInputStream inputStream =
                new BufferedInputStream(
                PackageVideoInCartridge.class
                    .getResource(inputResourceFileName)
                    .openStream()))
        {
            // Copy the file data.
            outputStream.write(inputStream.readAllBytes());
        }
    }


    /**
     * Combines and writes the specified ROM resource file and the specified
     * TMS video file to the given output stream.
     */
    private void writeRom(String       inputRomResourceFileName,
                          String       inputTmsFileName,
                          OutputStream outputStream)
    throws IOException
    {
        // Copy the modified header and assembly code.
        try (BufferedInputStream inputStream =
                new BufferedInputStream(
                PackageVideoInCartridge.class
                    .getResource(inputRomResourceFileName)
                    .openStream()))
        {
            // Copy the module header.
            outputStream.write(inputStream.readNBytes(12));

            // Write the elements of the program list.
            // Note that they are shown in reverse order.
            if (titleWithoutSpeech != null)
            {
                // Copy the pointer to the next element and
                // the pointer to the program start without speech.
                outputStream.write(inputStream.readNBytes(4));

                // Skip the original first title.
                inputStream.skipNBytes(1 + MAX_TITLE_LENGTH);

                // Write a new first title.
                writeTitle(outputStream, titleWithoutSpeech);

                // Copy the null pointer to the next element and
                // the pointer to the program start with speech.
                outputStream.write(inputStream.readNBytes(4));

                // Skip the original second title.
                inputStream.skipNBytes(1 + MAX_TITLE_LENGTH);

                // Write a new second title.
                writeTitle(outputStream, title);

            }
            else
            {
                // Skip the original first element.
                inputStream.skipNBytes(4 + 1 + MAX_TITLE_LENGTH);

                // Copy the null pointer to the next element and
                // the pointer to the program start with speech.
                outputStream.write(inputStream.readNBytes(4));

                // Skip the original second title.
                inputStream.skipNBytes(1 + MAX_TITLE_LENGTH);

                // Write a new first title.
                writeTitle(outputStream, title);

                // Clear the second element.
                outputStream.write(new byte[4 + 1 + MAX_TITLE_LENGTH]);
            }

            // Copy the program code and the filler, up to the second
            // memory bank (discarding the demo video in it).
            outputStream.write(inputStream.readNBytes(MEMORY_BANK_SIZE - HEADER_SIZE));
        }

        // Copy the video data.
        try (BufferedInputStream inputStream =
                 new BufferedInputStream(
                 new FileInputStream(inputTmsFileName)))
        {
            // Copy all video data.
            outputStream.write(inputStream.readAllBytes());
        }
    }


    /**
     * Writes the length and bytes of the given title, truncated or padded.
     */
    private void writeTitle(OutputStream outputStream,
                            String       title)
    throws IOException
    {
        byte[] titleBytes =
            (title.length() > MAX_TITLE_LENGTH ?
                title.substring(0, MAX_TITLE_LENGTH) :
                title)
                .toUpperCase()
                .getBytes(StandardCharsets.US_ASCII);

        outputStream.write(titleBytes.length);
        outputStream.write(titleBytes);
        outputStream.write(new byte[MAX_TITLE_LENGTH - titleBytes.length]);
    }
}