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
package speech;

/**
 * This enum applies the quantization and encoding techniques
 * to the Linear Predictive Coding (LPC) parameters for the known
 * TMS52xx speech synthesizers.
 *
 * Derived from
 *     https://github.com/ptwz/python_wizard/blob/master/lpcplayer/tables.py
 *
 * @see http://www.unige.ch/medecine/nouspikel/ti99/speech.htm
 */
public enum LpcQuantization
{
    TMS5100(4,
            10,
            4,
            5,
            Constants.TI_K_BIT_COUNTS,
            Constants.TI_0280_PATENT_ENERGY,
            Constants.TI_0280_2801_PATENT_PITCH,
            Constants.TI_0280_PATENT_LPC,
            Constants.TI_0280_PATENT_CHIRP,
            Constants.TI_INTERPOLATION),

    TMS5110A(4,
             10,
             4,
             5,
             Constants.TI_K_BIT_COUNTS,
             Constants.TI_028X_LATER_ENERGY,
             Constants.TI_5110_PITCH,
             Constants.TI_5110_5220_LPC,
             Constants.TI_LATER_CHIRP,
             Constants.TI_INTERPOLATION),

    TMS5200(4,
            10,
            4,
            6,
            Constants.TI_K_BIT_COUNTS,
            Constants.TI_028X_LATER_ENERGY,
            Constants.TI_2501E_PITCH,
            Constants.TI_2801_2501E_LPC,
            Constants.TI_LATER_CHIRP,
            Constants.TI_INTERPOLATION),

    TMS5220(4,
            10,
            4,
            6,
            Constants.TI_K_BIT_COUNTS,
            Constants.TI_028X_LATER_ENERGY,
            Constants.TI_5220_PITCH,
            Constants.TI_5110_5220_LPC,
            Constants.TI_LATER_CHIRP,
            Constants.TI_INTERPOLATION);


    // The above enum constructors can only refer to constants in a separate class.
    private static class Constants
    {

    // K parameter bit counts.
    private static final int[] TI_K_BIT_COUNTS =
    {
        5, 5, 4, 4, 4, 4, 4, 3, 3, 3
    };

    // Energy tables.
    private static final int[] TI_0280_PATENT_ENERGY =
    {
        0, 0, 1, 1, 2, 3, 5, 7, 10, 15, 21, 30, 43, 61, 86, 0
    };

    private static final int[] TI_028X_LATER_ENERGY =
    {
        0, 1, 2, 3, 4, 6, 8, 11, 16, 23, 33, 47, 63, 85, 114, 0
    };

    // Pitch tables.
    private static final int[] TI_0280_2801_PATENT_PITCH =
    {
          0,  41,  43,  45,  47,  49,  51,  53,
         55,  58,  60,  63,  66,  70,  73,  76,
         79,  83,  87,  90,  94,  99, 103, 107,
        112, 118, 123, 129, 134, 140, 147, 153
    };

    private static final int[] TI_2802_PITCH =
    {
         0, 16,  18,  19,  21,  24,  26,  28,
        31, 35,  37,  42,  44,  47,  50,  53,
        56, 59,  63,  67,  71,  75,  79,  84,
        89, 94, 100, 106, 112, 126, 141, 150
    };

    private static final int[] TI_5110_PITCH =
    {
         0,  15,  16,  17,  19,  21,  22,  25,
        26,  29,  32,  36,  40,  42,  46,  50,
        55,  60,  64,  68,  72,  76,  80,  84,
        86,  93, 101, 110, 120, 132, 144, 159
    };

    private static final int[] TI_2501E_PITCH=
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

    private static final int[] TI_5220_PITCH =
    {
          0,  15,  16,  17,  18,  19,  20,  21,
         22,  23,  24,  25,  26,  27,  28,  29,
         30,  31,  32,  33,  34,  35,  36,  37,
         38,  39,  40,  41,  42,  44,  46,  48,
         50,  52,  53,  56,  58,  60,  62,  65,
         68,  70,  72,  76,  78,  80,  84,  86,
         91,  94,  98, 101, 105, 109, 114, 118,
        122, 127, 132, 137, 142, 148, 153, 159
    };

    private static final int[][] TI_0280_PATENT_LPC =
    {
        /* K1  */
        { -501, -497, -493, -488, -480, -471, -460, -446,
          -427, -405, -378, -344, -305, -259, -206, -148,
           -86,  -21,   45,  110,  171,  227,  277,  320,
           357,  388,  413,  434,  451,  464,  474,  498 },
        /* K2  */
        { -349, -328, -305, -280, -252, -223, -192, -158,
          -124,  -88,  -51,  -14,   23,   60,   97,  133,
           167,  199,  230,  259,  286,  310,  333,  354,
           372,  389,  404,  417,  429,  439,  449,  506 },
        /* K3  */
        { -397, -365, -327, -282, -229, -170, -104,  -36,
            35,  104,  169,  228,  281,  326,  364,  396 },
        /* K4  */
        { -369, -334, -293, -245, -191, -131,  -67,   -1,
            64,  128,  188,  243,  291,  332,  367,  397 },
        /* K5  */
        { -319, -286, -250, -211, -168, -122,  -74,  -25,
            24,   73,  121,  167,  210,  249,  285,  318 },
        /* K6  */
        { -290, -252, -209, -163, -114,  -62,   -9,   44,
            97,  147,  194,  238,  278,  313,  344,  371 },
        /* K7  */
        { -291, -256, -216, -174, -128,  -80,  -31,   19,
            69,  117,  163,  206,  246,  283,  316,  345 },
        /* K8  */
        { -218, -133,  -38,   59,  152,  235,  305,  361 },
        /* K9  */
        { -226, -157,  -82,   -3,   76,  151,  220,  280 },
        /* K10 */
        { -179, -122,  -61,    1,   62,  123,  179,  231 }
    };

    private static final int[][] TI_2801_2501E_LPC =
    {
        /* K1  */
        { -501, -498, -495, -490, -485, -478, -469, -459,
          -446, -431, -412, -389, -362, -331, -295, -253,
          -207, -156, -102,  -45,   13,   70,  126,  179,
           228,  272,  311,  345,  374,  399,  420,  437 },
        /* K2  */
        { -376, -357, -335, -312, -286, -258, -227, -195,
          -161, -124,  -87,  -49,  -10,   29,   68,  106,
           143,  178,  212,  243,  272,  299,  324,  346,
           366,  384,  400,  414,  427,  438,  448,  506 },
        /* K3  */
        { -407, -381, -349, -311, -268, -218, -162, -102,
           -39,   25,   89,  149,  206,  257,  302,  341 },
        /* K4  */
        { -290, -252, -209, -163, -114,  -62,   -9,   44,
           97,   147,  194,  238,  278,  313,  344,  371 },
        /* K5  */
        { -318, -283, -245, -202, -156, -107,  -56,   -3,
            49,  101,  150,  196,  239,  278,  313,  344 },
        /* K6  */
        { -193, -152, -109,  -65,  -20,   26,   71,  115,
           158,  198,  235,  270,  301,  330,  355,  377 },
        /* K7  */
        { -254, -218, -180, -140,  -97,  -53,   -8,   36,
            81,  124,  165,  204,  240,  274,  304,  332 },
        /* K8  */
        { -205, -112,  -10,   92,  187,  269,  336,  387 },
        /* K9  */
        { -249, -183, -110,  -32,   48,  126,  198,  261 },
        /* on patents 4,403,965 and 4,946,391 the 4th entry is 0x3ED {-19}; which is a typo of the correct value of 0x3E0 {-32};*/
        /* K10 */
        { -190, -133, -73, -10, 53, 115, 173, 227}
    };


    private static final int[][] TI_5110_5220_LPC =
    {
        /* K1  */
        { -501, -498, -497, -495, -493, -491, -488, -482,
          -478, -474, -469, -464, -459, -452, -445, -437,
          -412, -380, -339, -288, -227, -158,  -81,   -1,
            80,  157,  226,  287,  337,  379,  411,  436 },
        /* K2  */
        { -328, -303, -274, -244, -211, -175, -138,  -99,
           -59,  -18,   24,   64,  105,  143,  180,  215,
           248,  278,  306,  331,  354,  374,  392,  408,
           422,  435,  445,  455,  463,  470,  476,  506 },
        /* K3  */
        { -441, -387, -333, -279, -225, -171, -117,  -63,
            -9,   45,   98,  152,  206,  260,  314,  368 },
        /* K4  */
        { -328, -273, -217, -161, -106,  -50,    5,   61,
           116,  172,  228,  283,  339,  394,  450,  506 },
        /* K5  */
        { -328, -282, -235, -189, -142,  -96,  -50,   -3,
            43,   90,  136,  182,  229,  275,  322,  368 },
        /* K6  */
        { -256, -212, -168, -123,  -79,  -35,   10,   54,
            98,  143,  187,  232,  276,  320,  365,  409 },
        /* K7  */
        { -308, -260, -212, -164, -117,  -69,  -21,   27,
            75,  122,  170,  218,  266,  314,  361,  409 },
        /* K8  */
        { -256, -161,  -66,   29,  124,  219,  314,  409 },
        /* K9  */
        { -256, -176,  -96,  -15,   65,  146,  226,  307 },
        /* K10 */
        { -205, -132,  -59,   14,   87,  160,  234,  307 }
    };

    // Chirp tables.
    private static final byte[] TI_0280_PATENT_CHIRP =
    {
        (byte)0x00, (byte)0x2a, (byte)0xd4, (byte)0x32, (byte)0xb2, (byte)0x12, (byte)0x25, (byte)0x14,
        (byte)0x02, (byte)0xe1, (byte)0xc5, (byte)0x02, (byte)0x5f, (byte)0x5a, (byte)0x05, (byte)0x0f,
        (byte)0x26, (byte)0xfc, (byte)0xa5, (byte)0xa5, (byte)0xd6, (byte)0xdd, (byte)0xdc, (byte)0xfc,
        (byte)0x25, (byte)0x2b, (byte)0x22, (byte)0x21, (byte)0x0f, (byte)0xff, (byte)0xf8, (byte)0xee,
        (byte)0xed, (byte)0xef, (byte)0xf7, (byte)0xf6, (byte)0xfa, (byte)0x00, (byte)0x03, (byte)0x02,
        (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    };

    private static final byte[] TI_LATER_CHIRP =
    {
        0x00, 0x03, 0x0f, 0x28, 0x4c, 0x6c, 0x71, 0x50,
        0x25, 0x26, 0x4c, 0x44, 0x1a, 0x32, 0x3b, 0x13,
        0x37, 0x1a, 0x25, 0x1f, 0x1d, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00
    };

    // Interpolation table.
    private static final int[] TI_INTERPOLATION =
    {
        0, 3, 3, 3, 2, 2, 1, 1
    };
    }

    private static final boolean DEBUG = false;


    public final int     unvoicedLpcCoefficientCount;
    public final int     voicedLpcCoefficientCount;
    public final int     energyBitCount;
    public final int     pitchBitCount;
    public final int[]   lpcCoefficientBitCounts;
    public final int[]   energyTable;
    public final int[]   pitchTable;
    public final int[][] lpcCoefficientTable;
    public final byte[]  chirpTable;
    public final int[]   interpolationTable;


    /**
     * Creates a new instance.
     */
    LpcQuantization(int     unvoicedLpcCoefficientCount,
                    int     voicedLpcCoefficientCount,
                    int     energyBitCount,
                    int     pitchBitCount,
                    int[]   lpcCoefficientBitCounts,
                    int[]   energyTable,
                    int[]   pitchTable,
                    int[][] lpcCoefficientTable,
                    byte[]  chirpTable,
                    int[]   interpolationTable)
    {
        this.unvoicedLpcCoefficientCount = unvoicedLpcCoefficientCount;
        this.voicedLpcCoefficientCount   = voicedLpcCoefficientCount;
        this.energyBitCount              = energyBitCount;
        this.pitchBitCount               = pitchBitCount;
        this.lpcCoefficientBitCounts     = lpcCoefficientBitCounts;
        this.energyTable                 = energyTable;
        this.pitchTable                  = pitchTable;
        this.lpcCoefficientTable         = lpcCoefficientTable;
        this.chirpTable                  = chirpTable;
        this.interpolationTable          = interpolationTable;
    }


    /**
     * Returns the encoded energy of silence.
     */
    public int silenceEnergy()
    {
        return 0;
    }


    /**
     * Returns the minimum encoded energy of a voiced or unvoiced frame.
     */
    public int minEncodedEnergy()
    {
        return 1;
    }


    /**
     * Returns the maximum encoded energy of a voiced or unvoiced frame.
     */
    public int maxEncodedEnergy()
    {
        return (1 << energyBitCount) - 2;
    }


    /**
     * Returns the  encoded energy of a stop frame.
     */
    public int stopEnergy()
    {
        return (1 << energyBitCount) - 1;
    }


    /**
     * Quantizes and encodes the given energy as an integer, which
     * can be used in an {@link LpcFrame}. Note that this must be an
     * {@link LpcSilenceFrame} for energy 0.
     * @param energy the energy as a value between 0 and 1.
     */
    public int encodeEnergy(double energy)
    {
        return nearestValueIndex(energyTable,
                                 0,
                                 energyTable.length - 2,
                                 intEnergy(energy));
    }


    /**
     * Returns the minimum encoded pitch of a voiced or unvoiced frame.
     */
    public int minEncodedPitch()
    {
        return 1;
    }


    /**
     * Returns the maximum encoded pitch of a voiced frame.
     */
    public int maxEncodedPitch()
    {
        return (1 << pitchBitCount) - 1;
    }


    /**
     * Quantizes and encodes the given frequency as an integer pitch,
     * which can be used in an {@link LpcFrame}.
     * @param frequency the frequency expressed in Hz.
     */
    public int encodePitch(double frequency)
    {
        return encodePitch(pitch(frequency));
    }


    /**
     * Quantizes and encodes the given pitch as an integer pitch that
     * can be used in an {@link LpcFrame}.
     * @param pitch the pitch expressed in samples.
     */
    public int encodePitch(int pitch)
    {
        return nearestValueIndex(pitchTable,
                                 1,
                                 pitchTable.length - 1,
                                 pitch);
    }


    /**
     * Decodes the given LPC coefficient indices from a single long integer,
     * from an {@link LpcFrame}, to an array.
     * @param k      the quantized LPC reflection coefficients encoded as a
     *               single long integer.
     * @param voiced a flag that indicates whether the coefficients are
     *               for a voiced frame or an unvoiced frame.
     * @return       the quantized LPC reflection coefficients, typically up
     *               to 4 (unvoiced) or up to 10 (voiced), with values between
     *               0 and 2^5, 2^4, or 2^3.
     */
    public int[] decodeLpcCoefficients(long k, boolean voiced)
    {
        int count = voiced ?
            voicedLpcCoefficientCount :
            unvoicedLpcCoefficientCount;

        int[] result = new int[count];

        for (int index = count - 1; index >= 0; index--)
        {
            int bitCount = lpcCoefficientBitCounts[index];

            result[index] = (int)(k & (2 << bitCount-1)-1);
            k >>>= bitCount;
        }

        return result;
    }


    /**
     * Encodes the given LPC coefficient indices into a single long integer,
     * which can be used in an {@link LpcFrame}.
     * @param k      the quantized LPC reflection coefficients, typically up
     *               to 4 (unvoiced) or up to 10 (voiced), with values between
     *               0 and 2^5, 2^4, or 2^3.
     * @param voiced a flag that indicates whether the coefficients are
     *               for a voiced frame or an unvoiced frame.
     * @return       the quantized LPC reflection coefficients encoded as a
     *               single long integer.
     */
    public long encodeLpcCoefficients(int[] k, boolean voiced)
    {
        long result = 0L;

        int count = voiced ?
            voicedLpcCoefficientCount :
            unvoicedLpcCoefficientCount;

        for (int index = 0; index < count; index++)
        {
            result <<= lpcCoefficientBitCounts[index];
            result |= k[index];
        }

        return result;
    }


    /**
     * Quantizes and encodes the given LPC coefficients into a single long
     * integer, which can be used in an {@link LpcFrame}.
     * @param k      the LPC reflection coefficients, typically up to 4
     *               (unvoiced) or up to 10 (voiced), with values between
     *               -1 and 1.
     * @param voiced a flag that indicates whether the coefficients are
     *               for a voiced frame or an unvoiced frame.
     * @return       the quantized LPC reflection coefficients encoded as a
     *               single long integer.
     */
    public long encodeLpcCoefficients(double[] k, boolean voiced)
    {
        long result = 0L;

        int count = voiced ?
            voicedLpcCoefficientCount :
            unvoicedLpcCoefficientCount;

        for (int index = 0; index < count; index++)
        {
            int lpcCoefficient = index < k.length ?
                intLpcCoefficient(k[index]) :
                0;

            result <<= lpcCoefficientBitCounts[index];
            result |= nearestValueIndex(lpcCoefficientTable[index],
                                        lpcCoefficient);
        }

        return result;
    }


    // Small utility methods.

    private int intEnergy(double energy)
    {
        return (int)(energy * 128.0);
    }


    public int pitch(double frequency)
    {
        return (int)Math.round(8000.0 / frequency - 1.0);
    }


    public double frequency(int pitch)
    {
        return 8000.0 / (pitch + 1);
    }


    private int intLpcCoefficient(double lpcCoefficient)
    {
        return (int)Math.round(512.0 * lpcCoefficient);
    }


    private double doubleLpcCoefficient(int lpcCoefficient)
    {
        return (double)lpcCoefficient / 512.0;
    }


    private int nearestValueIndex(int[] values, int value)
    {
        return nearestValueIndex(values, 0, values.length-1, value);
    }


    private int nearestValueIndex(int[] values,
                                  int   minIndex,
                                  int   maxIndex,
                                  int   value)
    {
        // Perform a binary search.
        while (minIndex+1 < maxIndex)
        {
            int middleIndex = (minIndex + maxIndex) / 2;
            if (values[middleIndex] < value)
            {
                minIndex = middleIndex;
            }
            else
            {
                maxIndex = middleIndex;
            }
        }

        // Pick the closest of the two surrounding values.
        int nearestIndex = 2 * value < values[minIndex] + values[maxIndex] ?
            minIndex :
            maxIndex;

        if (DEBUG)
        {
            System.out.println("Value "+value+" in (0..."+values.length+") ["+values[0]+"..."+values[values.length-1]+"] -> ("+minIndex+","+maxIndex+") ["+values[minIndex]+","+values[maxIndex]+"] -> ("+nearestIndex+") ["+values[nearestIndex]+"]"+(value < values[0] || value > values[values.length-1] ? " (clipped)":""));
        }

        return nearestIndex;
    }
}
