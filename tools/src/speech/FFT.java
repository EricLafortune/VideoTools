/*
 * Video tools for the TI-99/4A home computer.
 *
 *    Copyright (c) 2022-2024 Eric Lafortune
 *
 * FFT code derived from fft.c:
 *
 *     Copyright (c) 1992 Douglas L. Jones,
 *                        University of Illinois at Urbana-Champaign
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

import java.util.Arrays;

/**
 * This class computes Fast Fourier Transforms of complex numbers.
 */
public class FFT
{
    private static final double MIN_LOG     = -50.0;
    private static final double EXP_MIN_LOG = Math.exp(MIN_LOG);

    // Cached lookup tables.
    private double[] gaussianWindow;
    private double[] hammingWindow;
    private double[] blackmanWindow;
    private double[] gaussianFilter;
    private double[] valueBuffer;

    private int[]    melFrequencyIndices;

    private double[] cos;
    private double[] sin;


    /**
     * Shifts the given values so their average is 0, in-place.
     */
    public void normalizeMean(double[] values)
    {
        shift(values, -average(values));
    }


    /**
     * Normalizes the given values so their average is 1, in-place.
     */
    public void normalizeScale(double[] values)
    {
        double average = average(values);
        if (average != 0.0)
        {
            for (int i = 0; i < values.length; i++)
            {
                values[i] /= average;
            }
        }
    }


    /**
     * Scales the given values so their root mean squared value is 1,
     * in-place.
     */
    public void normalizeRms(double[] values)
    {
        double rms = rms(values);
        if (rms != 0.0)
        {
            scale(values, 1.0 / rms);
        }
    }


    /**
     * Applies a pre-emphasis to the given values, in-place.
     * It has the effect of reducing the lower frequencies,
     * emphasizing the higher frequencies.
     */
    public void applyPreemphasis(double[] values, double alpha)
    {
        if (alpha > 0.0)
        {
            for (int i = values.length-1; i >= 1; i--)
            {
                values[i] -= alpha * values[i-1];
            }
        }
    }


    /**
     * Applies a Gaussian window to the given values, in-place.
     */
    public void applyGaussianWindow(double[] values)
    {
        applyGaussianWindow(values, values.length);
    }


    /**
     * Applies a Gaussian window to the given values, in-place.
     * The window covers 2.5 sigma.
     */
    public void applyGaussianWindow(double[] values, int windowSize)
    {
        // Precompute a window.
        if (gaussianWindow == null ||
            gaussianWindow.length != windowSize)
        {
            gaussianWindow = new double[windowSize];

            final double a = 2.5;

            for (int i = 0; i < windowSize; i++)
            {
                // Range: a * [ -1 ... 1 )
                double e = a * (double)(windowSize - 2 * i) / (double)windowSize;

                gaussianWindow[i] = Math.exp(-0.5 * e * e);
            }
        }

        int i;
        for (i = 0; i < gaussianWindow.length; i++)
        {
            values[i] *= gaussianWindow[i];
        }

        while (i < values.length)
        {
            values[i++] = 0.0;
        }
    }


    /**
     * Applies a Hamming window to the given values, in-place.
     */
    public void applyHammingWindow(double[] values)
    {
        applyHammingWindow(values, values.length);
    }


    /**
     * Applies a Hamming window to the given values, in-place.
     */
    public void applyHammingWindow(double[] values, int windowSize)
    {
        // Precompute a window.
        if (hammingWindow == null ||
            hammingWindow.length != windowSize)
        {
            hammingWindow = new double[windowSize];

            for (int i = 0; i < windowSize; i++)
            {
                hammingWindow[i] = 0.54
                                 - 0.46 * Math.cos(2.0 * Math.PI * i / (windowSize - 1));
            }
        }

        int i;
        for (i = 0; i < hammingWindow.length; i++)
        {
            values[i] *= hammingWindow[i];
        }

        while (i < values.length)
        {
            values[i++] = 0.0;
        }
    }


    /**
     * Applies a Blackman window to the given values, in-place.
     */
    public void applyBlackmanWindow(double[] values)
    {
        applyBlackmanWindow(values, values.length);
    }


    /**
     * Applies a Blackman window to the given values, in-place.
     */
    public void applyBlackmanWindow(double[] values, int windowSize)
    {
        // Precompute a window.
        if (blackmanWindow == null ||
            blackmanWindow.length != windowSize)
        {
            blackmanWindow = new double[windowSize];

            for (int i = 0; i < windowSize; i++)
            {
                double theta = Math.PI * i / (windowSize - 1);

                blackmanWindow[i] = 0.42
                                  - 0.50 * Math.cos(2.0 * theta)
                                  + 0.08 * Math.cos(4.0 * theta);
            }
        }

        int i;
        for (i = 0; i < blackmanWindow.length; i++)
        {
            values[i] *= blackmanWindow[i];
        }

        while (i < values.length)
        {
            values[i++] = 0.0;
        }
    }


    /**
     * Applies a low-pass window to the given values in the cepstral domain, in-place
     * (https://ccrma.stanford.edu/~jos/SpecEnv/Cepstral_Smoothing.html).
     */
    public void applyLowPassCepstralWindow(double[] values, int windowSize)
    {
        if (windowSize < values.length)
        {
            values[windowSize] *= 0.5;

            for (int i = windowSize+1; i < values.length; i++)
            {
                values[i] = 0;
            }
        }
    }


    /**
     * Applies a Gaussian filter to the given values, in-place.
     * The filter covers 2.5 sigma.
     */
    public void applyGaussianFilter(double[] values,
                                    int      filterSize,
                                    int      startIndex,
                                    int      endIndex)
    {
        // Precompute a filter.
        if (gaussianFilter == null ||
            gaussianFilter.length != filterSize)
        {
            gaussianFilter = new double[filterSize];

            final double a = 2.5;

            for (int i = 0; i < filterSize; i++)
            {
                // Make sure it is symmetric for odd filter sizes.
                // Range: a * [ -0.5 ... 0.5 ]
                double e = a * (double)(filterSize / 2 - i) / (double)filterSize;

                gaussianFilter[i] = Math.exp(-2.0 * e * e);
            }

            normalizeScale(gaussianFilter);
        }

        applyFilter(values,
                    gaussianFilter,
                    startIndex,
                    endIndex);
    }


    /**
     * Applies a given filter to the given values, in-place.
     */
    public void applyFilter(double[] values,
                            double[] filter,
                            int      startIndex,
                            int      endIndex)
    {
        int fullLength = filter.length;
        int halfLength = fullLength / 2;

        // Maintain a rolling buffer for values that are about to be
        // overwritten.
        if (valueBuffer == null ||
            valueBuffer.length != halfLength)
        {
            valueBuffer = new double[halfLength];
        }

        // Initialize the rolling buffer with just the first value.
        Arrays.fill(valueBuffer, values[startIndex]);

        for (int i = startIndex; i < endIndex; i++)
        {
            double newValue = 0.0;

            int j;

            // Compute the convolution with (earlier) values, from the rolling
            // buffer.
            for (j = 0; j < halfLength; j++)
            {
                newValue += filter[j] * valueBuffer[(i + j) % halfLength];
            }

            // Compute the convolution with (later) values, from the array.
            for (/* j = halfLength */; j < fullLength; j++)
            {
                newValue += filter[j] * values[Math.min(values.length - 1, i + j - halfLength)];
            }

            // Update the rolling buffer with the value that is about to be
            // overwritten.
            valueBuffer[i % halfLength] = values[i];

            values[i] = newValue;
        }
    }


    /**
     * Computes the inverse discrete Fourier transform of the given complex
     * values, in-place.
     * @param real the real parts of the values.
     * @param imag the imaginary parts of the values.
     */
    public void computeInverseFFT(double[] real, double[] imag)
    {
        computeInverseFFT(real, imag, real.length);
    }


    /**
     * Computes the inverse discrete Fourier transform of the given complex
     * values, in-place.
     * @param real the real parts of the values.
     * @param imag the imaginary parts of the values.
     * @param n    the window size of the FFT.
     */
    public void computeInverseFFT(double[] real, double[] imag, int n)
    {
        // Trick: swapping the real and imaginary parts of the input and the
        // output, and normalizing, yields the inverse FFT.
        // https://en.wikipedia.org/wiki/Discrete_Fourier_transform#Expressing_the_inverse_DFT_in_terms_of_the_DFT
        computeFFT(imag, real, n);

        // Traditionally, the FFT is normalized by 1, and the inverse FFT
        // is normalized by 1/N.
        for (int i = 0; i < n; i++)
        {
            real[i] /= n;
            imag[i] /= n;
        }
    }


    /**
     * Computes the discrete Fourier transform of the given complex values,
     * in-place.
     * @param real the real parts of the values.
     * @param imag the imaginary parts of the values.
     */
    public void computeFFT(double[] real, double[] imag)
    {
        computeFFT(real, imag, real.length);
    }


    /*
     * Derived from fft.c
     *
     *   fft: in-place radix-2 DIT DFT of a complex input
     *
     * Douglas L. Jones
     * University of Illinois at Urbana-Champaign
     * January 19, 1992
     * https://cnx.org/contents/qAa9OhlP@2.44:zmcmahhR@7/Decimation-in-time-DIT-Radix-2-FFT
     * http://cnx.rice.edu/content/m12016/latest/
     *
     * Permission to copy and use this program is granted
     * under a Creative Commons "Attribution" license
     * http://creativecommons.org/licenses/by/1.0/
     */
    /**
     * Computes the discrete Fourier transform of the given complex values,
     * in-place.
     * @param real the real parts of the values.
     * @param imag the imaginary parts of the values.
     * @param n    the window size of the FFT.
     */
    public void computeFFT(double[] real, double[] imag, int n)
    {
        // Make sure the window size is a power of 2.
        if ((n & (n - 1)) != 0)
        {
            throw new IllegalArgumentException("The FFT window size ["+n+"] is not a power of 2");
        }

        int n2 = n / 2;
        int m  = Integer.numberOfTrailingZeros(n); // m = log2(n)

        // Cache cosine and sine values ("twiddle factors").
        if (cos == null ||
            cos.length != n2)
        {
            cos = new double[n2];
            sin = new double[n2];

            for (int i = 0; i < cos.length; i++)
            {
                double theta = -2.0 * Math.PI * i / n;

                cos[i] = Math.cos(theta);
                sin[i] = Math.sin(theta);
            }
        }

        // Bit-reverse the indices of the arrays.
        for (int i = 1; i < n - 1; i++)
        {
            int j = Integer.reverse(i) >>> 32-m;

            // Swap the values, avoiding to do it twice.
            if (i < j)
            {
                double t1 = real[i];
                real[i] = real[j];
                real[j] = t1;

                double t2 = imag[i];
                imag[i] = imag[j];
                imag[j] = t2;
            }
        }

        // Perform the actual FFT.
        n2 = 1;

        for (int i = 0; i < m; i++)
        {
            int n1 = n2;
            n2 *= 2;
            int ci = 0;

            for (int j = 0; j < n1; j++)
            {
                double c = cos[ci];
                double s = sin[ci];

                ci += 1 << (m - i - 1);

                for (int k = j; k < n; k += n2)
                {
                    int k1 = k + n1;

                    double t1 = c * real[k1] - s * imag[k1];
                    double t2 = s * real[k1] + c * imag[k1];

                    real[k1] = real[k] - t1;
                    imag[k1] = imag[k] - t2;

                    real[k] += t1;
                    imag[k] += t2;
                }
            }
        }
    }


    /**
     * Computes the power spectrum of the given FFT values, in-place.
     * @param real the real parts of the FFT values.
     *             The power spectrum on return.
     * @param imag the imaginary parts of the FFT values.
     */
    public void computePowerSpectrum(double[] real, double[] imag)
    {
        computePowerSpectrum(real, imag, real);
    }


    /**
     * Computes the power spectrum of the given FFT values.
     * @param real  the real parts of the FFT values.
     * @param imag  the imaginary parts of the FFT values.
     * @param power the power spectrum on return. The length of the
     *              array is typically the same as the real parts
     *              and the imaginary parts, or half the length if
     *              the input signal was real.
     */
    public void computePowerSpectrum(double[] real,
                                     double[] imag,
                                     double[] power)
    {
        power[0] = real[0] * real[0];

        for (int i = 1; i < power.length; i++)
        {
            double re = real[i];
            double im = imag[i];

            power[i] = re * re + im * im;
        }
    }


    /**
     * Converts the power spectrum from the physical frequency scale to the
     * perceptual mel scale. The conversion is m = 2595 log(1+f/700)
     * (https://en.wikipedia.org/wiki/Mel_scale).
     * @param frequencyPower the power spectrum on the frequency scale.
     * @param maxFrequency   the maximum frequency of the power spectrum.
     * @param melPower       the power spectrum on the mel scale.
     */
    public void convertPowerSpectrumToMel(double[] frequencyPower,
                                          double   maxFrequency,
                                          double[] melPower)
    {
        int frequencySize = frequencyPower.length;
        int melSize       = melPower.length;

        // Precompute a filter.
        if (melFrequencyIndices == null ||
            melFrequencyIndices.length != melSize)
        {
            melFrequencyIndices = new int[melSize];

            double frequencyDelta = maxFrequency / frequencySize;

            for (int frequencyIndex = 0; frequencyIndex < frequencySize; frequencyIndex++)
            {
                int melIndex = melIndex(frequencyDelta * frequencyIndex,
                                        frequencyDelta);

                melFrequencyIndices[melIndex] = frequencyIndex;
            }
        }

        int previousFrequencyIndex = 0;

        for (int melIndex = 0; melIndex < melSize; melIndex++)
        {
            int frequencyIndex = melFrequencyIndices[melIndex];

            melPower[melIndex] = average(frequencyPower,
                                         previousFrequencyIndex,
                                         frequencyIndex + 1);

            previousFrequencyIndex = frequencyIndex + 1;
        }
    }


    public int melIndex(double frequency, double frequencyDelta)
    {
        return (int)Math.round(mel(frequency) / mel(frequencyDelta));
    }


    public double mel(double frequency)
    {
        return 2595.0 * Math.log(1.0 + frequency / 700.0);
    }


    /**
     * Estimates the pitch of the given samples.
     * @param samples  the samples of the signal.
     * @param minPitch the minimum pitch, expresses a number of samples.
     * @param maxPitch the maximum pitch, expresses a number of samples.
     * @return the pitch between subsequent cycles of the samples, expressed
     *         as a number of samples.
     */
    public int estimatePitch(double[] samples,
                             int      minPitch,
                             int      maxPitch)
    {
        // There are many possible techniques for pitch detection:
        // - in the time domain: zero crossings, autocorrelation, maximum
        //   likelihood, adaptive filter, super resolution;
        // - in the frequency domain: harmonic product soectrum,
        //   cepstrum, maximum likelihood;
        // - perceptual.
        // https://ccrma.stanford.edu/~pdelac/154/m154paper.htm
        // We use the maximum autocorrelation.
        double maxCorrelation = 0.0;
        int    bestPitch      = minPitch;

        for (int pitch = minPitch; pitch <= maxPitch; pitch++)
        {
            double correlation = autocorrelation(samples, pitch);
            if (correlation > maxCorrelation)
            {
                maxCorrelation = correlation;
                bestPitch      = pitch;
            }
        }

        return bestPitch;
    }


    /**
     * Estimates LPC reflection coefficients with the algorithm by Le Roux
     * and Gueguen.
     * @param r the autocorrelation values.
     * @param k the reflection coefficients on return.
     */
    public void estimateReflectionCoefficientsLeRouxGueguen(double[] r,
                                                            double[] k)
    {
        // Compute the predictor coefficients together with the reflection coefficients.
        // We don't store the first values 1 and 0, respectively.
        double[] a = new double[k.length+1];
        double[] b = new double[k.length]; // Temporary array.

        // We don't unroll the first iteration of the loop.
        a[0] = r[0];

        for (int i = 0; i < k.length; i++)
        {
            double sum = b[0] = r[i+1];

            for (int j = 0; j < i; j++)
            {
                b[j+1] = a[j] + k[j] * sum;
                sum   += k[j] * a[j];
                a[j]   = b[j];
            }

            k[i]   = a[i] == 0.0 ?  0.0 : -sum / a[i];
            a[i+1] = a[i] + k[i] * sum;
            a[i]   = b[i];
        }
    }


    /**
     * Estimates LPC reflection coefficients with the algorithm by Levinson
     * and Durbin.
     * @param r the autocorrelation values.
     * @param k the reflection coefficients on return.
     */
    public void estimateReflectionCoefficientsLevinsonDurbin(double[] r,
                                                             double[] k)
    {
        // Compute the predictor coefficients together with the reflection coefficients.
        // We don't store the first values 1 and 0, respectively.
        double[] a = new double[k.length];
        double[] b = new double[k.length]; // Temporary array.

        // We don't unroll the first iteration of the loop.
        double product = r[0];

        for (int i = 0; i < k.length; i++)
        {
            double sum = r[i+1];

            for (int j = 0; j < i; j++)
            {
                sum += a[j] * r[i-j];
            }

            a[i] = k[i] = product == 0.0 ? 0.0 : -sum / product;

            product *= 1.0 - k[i] * k[i];

            // Make a temporary copy of the predictor coefficients.
            System.arraycopy(a, 0, b, 0, i);

            for (int j = 0; j < i; j++)
            {
                a[j] = b[j] + k[i] * b[i-j];
            }
        }
    }


    /**
     * Returns the average of the given values.
     */
    public double average(double[] values)
    {
        return average(values, 0, values.length);
    }


    /**
     * Returns the average of the given values.
     */
    public double average(double[] values,
                          int      startIndex,
                          int      endIndex)
    {
        double sum = 0.0;

        for (int i = startIndex; i < endIndex; i++)
        {
            sum += values[i];
        }

        return sum / (endIndex - startIndex);
    }


    /**
     * Returns the slope of the given values.
     */
    public double slope(double[] values)
    {
        return slope(values, 0, values.length);
    }


    /**
     * Returns the slope of the given values.
     */
    public double slope(double[] values,
                        int      startIndex,
                        int      endIndex)
    {
        int    n = endIndex - startIndex;
        double m = 0.5 * (n - 1);
        double f = 12.0 / (n * n * n - n);

        double sum = 0.0;

        for (int i = startIndex; i < endIndex; i++)
        {
            sum += values[i] * f * (i - m);
        }

        return sum;
    }


    /**
     * Returns the root mean squared value of the given values.
     */
    public double rms(double[] values)
    {
        return rms(values, 0, values.length);
    }


    /**
     * Returns the root mean squared value of the given values.
     */
    public double rms(double[] values,
                      int      startIndex,
                      int      endIndex)
    {
        double sum = 0.0;

        for (int i = startIndex; i < endIndex; i++)
        {
            double value = values[i];

            sum += value * value;
        }

        return Math.sqrt(sum / (endIndex - startIndex));
    }


    /**
     * Computes the autocorrelation values of the given values, with shifts
     * of 0 to autocorrelation.length-1.
     */
    public void computeAutocorrelation(double[] values,
                                       double[] autocorrelation)
    {
        for (int i = 0; i < autocorrelation.length; i++)
        {
            autocorrelation[i] = autocorrelation(values, i);
        }
    }


    /**
     * Returns the autocorrelation value of the given values with the given lag.
     */
    public double autocorrelation(double[] values, int lag)
    {
        double sum = 0.0;

        for (int i = 0; i < values.length-lag; i++)
        {
            sum += values[i] * values[i+lag];
        }

        // TODO: We should normalize based on the actual number of summed values,
        // but that differs from QWizard and seems to produce worse results.
        // Maybe the window function already accounts for it.
        //return sum / (values.length - lag);
        return sum / values.length;
    }


    /**
     * Adds a constant to the given values, in-place.
     */
    public void shift(double[] values, double constant)
    {
        if (constant != 0.0)
        {
            for (int i = 0; i < values.length; i++)
            {
                values[i] += constant;
            }
        }
    }


    /**
     * Multiplies the given values by a constant, in-place.
     */
    public void scale(double[] values, double constant)
    {
        if (constant != 1.0)
        {
            for (int i = 0; i < values.length; i++)
            {
                values[i] *= constant;
            }
        }
    }


    /**
     * Computes the natural logarithms of the given values, in-place.
     */
    public void computeLog(double[] values)
    {
        for (int i = 0; i < values.length; i++)
        {
            double value = values[i];
            values[i] = value < EXP_MIN_LOG ? MIN_LOG :
                Math.log(value);
        }
    }


    /**
     * Computes the sum of the squared differences of the given series of
     * values.
     */
    public double difference(double[] values1,
                             double[] values2,
                             int      startIndex,
                             int      endIndex)
    {
        double difference = 0.0;

        for (int i = startIndex; i < endIndex; i++)
        {
            double d = values1[i] - values2[i];

            difference += d * d;
        }

        return difference / (endIndex - startIndex);
    }


    /**
     * Tests the FFT code.
     */
    public static void main(String[] args)
    {
        FFT fft = new FFT();

        // https://en.wikipedia.org/wiki/Discrete_Fourier_transform#Example
        // double[] real = { 1,  2,  0, -1 };
        // double[] imag = { 0, -1, -1,  2 };
        // FFT:
        //   { 2, -2,  0, 4 }
        //   { 0, -2, -2, 4 }

        int n = 64;

        double[] real = new double[n];
        double[] imag = new double[n];

        // A simple sine function.
        for (int i = 0; i < n; i++)
        {
            real[i] = Math.sin(2.0  * Math.PI * i * 10 / n);
        }

        double[] samplesReal = real.clone();
        double[] samplesImag = imag.clone();

        fft.computeFFT(real, imag);

        double[] fftReal = real.clone();
        double[] fftImag = imag.clone();

        fft.computeInverseFFT(real, imag);

        // Print the samples, FFT, and subsequent inverse FFT (= original samples).
        for (int i = 0; i < n; i++)
        {
            System.out.printf("%3d: %8.3f %8.3f   %8.3f %8.3f   %8.3f %8.3f%n",
                              i,
                              samplesReal[i], samplesImag[i],
                              fftReal[i], fftImag[i],
                              real[i], imag[i]);
        }
    }
}
