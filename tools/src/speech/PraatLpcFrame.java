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
package speech;

/**
 * This class represents a frame containing pitch values and prediction
 * coefficients from the Praat phonetics software.
 *
 * @see https://www.fon.hum.uva.nl/praat/
 */
public class PraatLpcFrame
{
    public double   intensity;
    public double   frequency;
    public double   gain;
    public double[] predictorCoefficients;


    public PraatLpcFrame(double   intensity,
                         double   frequency,
                         double   gain,
                         double[] predictorCoefficients)
    {
        this.intensity              = intensity;
        this.frequency              = frequency;
        this.gain                   = gain;
        this.predictorCoefficients = predictorCoefficients;
    }
}
