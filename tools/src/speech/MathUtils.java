/**
 * Copyright 2004-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
//package marytts.util.math;
package speech;

//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Vector;

//import marytts.util.string.StringUtils;

/**
 * @author Marc Schr&ouml;der, Oytun Tuerk
 *
 *
 *         An uninstantiable class, containing static utility methods in the Math domain.
 *
 */
public class MathUtils {
	protected static final double PASCAL = 2E-5;
	protected static final double PASCALSQUARE = 4E-10;
	protected static final double LOG10 = Math.log(10);

	public static final double TWOPI = 2 * Math.PI;

	public static boolean isPowerOfTwo(int N) {
		final int maxBits = 32;
		int n = 2;
		for (int i = 2; i <= maxBits; i++) {
			if (n == N)
				return true;
			n <<= 1;
		}
		return false;
	}

	public static int closestPowerOfTwoAbove(int N) {
		return 1 << (int) Math.ceil(Math.log(N) / Math.log(2));
	}
	public static double sumSquared(double[] data) {
		return sumSquared(data, 0.0);
	}

	// Computes sum_i=0^data.length-1 (data[i]+term)^2
	public static double sumSquared(double[] data, double term) {
		double sum = 0.0;
		for (int i = 0; i < data.length; i++) {
			if (Double.isNaN(data[i]))
				continue;
			sum += (data[i] + term) * (data[i] + term);
		}
		return sum;
	}

	public static double sumSquared(double[] data, int startInd, int endInd) {
		return sumSquared(data, startInd, endInd, 0.0);
	}

	// Computes sum_i=0^data.length-1 (data[i]+term)^2
	public static double sumSquared(double[] data, int startInd, int endInd, double term) {
		double sum = 0.0;
		for (int i = startInd; i <= endInd; i++)
			sum += (data[i] + term) * (data[i] + term);

		return sum;
	}

    /**
	 * Convert energy from linear scale to db scale.
	 *
	 * @param energy
	 *            in time or frequency domain, on a linear energy scale
	 * @return energy on a db scale, or NaN if energy is less than or equal to 0.
	 */
	public static double db(double energy) {
		if (energy <= 1e-80)
			return -200.0;
		else
			return 10 * log10(energy);
	}

	/**
	 * Build the sum of the squared difference of all elements with the same index numbers in the arrays. Any NaN values in either
	 * a or b are ignored in computing the error.
	 *
	 * @param a
	 *            a
	 * @param b
	 *            a
	 * @return sum
	 */
	public static double sumSquaredError(double[] a, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			double delta = a[i] - b[i];
			if (!Double.isNaN(delta)) {
				sum += delta * delta;
			}
		}
		return sum;
	}

    public static double log10(double x) {
		return Math.log(x) / LOG10;
	}

	public static double[] log(double[] a) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = Math.log(a[i]);
		}
		return c;
	}

	// A special log operation
	// The values smaller than or equal to minimumValue are set to fixedValue
	// The values greater than minimumValue are converted to log
	public static double[] log(double[] a, double minimumValue, double fixedValue) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			if (a[i] > minimumValue)
				c[i] = Math.log(a[i]);
			else
				c[i] = fixedValue;
		}
		return c;
	}

	public static double[] log10(double[] a) {
		double[] c = null;

		if (a != null) {
			c = new double[a.length];

			for (int i = 0; i < a.length; i++)
				c[i] = log10(a[i]);
		}

		return c;
	}
}
