# ConvertWavToLpc

## Summary

Converts an audio file in WAV format to a speech file in our [binary LPC
format](LpcFileFormat.md), containing linear predictive coding coefficients.
Our LPC format is optimized for replay with the TMS5200/TMS5220 speech
synthesis chips.

## Technical features

I've explored and included (or sometimes rejected) a range of different
techniques:

* Traditional LeRoux-Gueguen algorithm to compute initial LPC coefficients.
* Unique iterative optimization of the LPC coefficients and energies for the
  TMS5200 or TMS5220 processors, in the discrete Fourier domain.
* Filtering transient high energy frames leading to pops and clicks.
* Filtering sequences of low energy frames as silent frames.
* Auto-correlation to distinguish between voiced and unvoiced speech frames.
* Filtering jitter between voiced and unvoiced speech frames.
* Auto-correlation to estimate pitches.
* Filtering outlier pitches.
* Pre-emphasis of high frequencies to get perceptually better results.
* Computation on log power spectra -- tunable, sometimes helps.
* Shifting sample windows to find better matches -- optional, sometimes helps.
* Computation in the Mel domain -- rejected, didn't produce better results for
  me. It doesn't seem to make sense to emphasize low frequencies when you've
 just pre-emphasized high frequencies.
* Computation in the cepstral domain -- rejected, didn't produce better results
  for me. Minimizing differences in power spectra gets thrown off when cepstral
  filters suppress parts of the spectra.

## Usage

    java ConvertWavToLpc [options...] input.wav output.lpc

where:

|              |                                                                            |
|--------------|----------------------------------------------------------------------------|
| _input.wav_  | The input sound file in WAV format, at 8000 Hz, mono.                      |
| _output.lpc_ | The output speech file containing the encoded binary stream of LPC frames. |

The basic options are:

|                           |                                                                                                                  |
|---------------------------|------------------------------------------------------------------------------------------------------------------|
| `-chip` _name_            | Target the specified speech synthesis chip: one of `TMS5100`, `TMS5110A`. `TMS5200` (the default), or `TMS5220`. |
| `-tms5200`                | Short for `-chip TMS5200` (the default).                                                                         |
| `-tms5220`                | Short for `-chip TMS5220`.                                                                                       |
| `-amplification` _factor_ | Amplify the input sound file by the given factor (default = 0.9373).                                             |
| `-minfrequency` _value_   | Set the minimum allowed frequency in the output LPC coefficients, expressed in Hz (default = 30.0).              |
| `-maxfrequency` _value_   | Set the maximum allowed frequency in the output LPC coefficients, expressed in Hz (default = 600.0).             |
| `-trimsilenceframes`      | Trim any LPC silence frames from the start and end of the output.                                                |
| `-addstopframe`           | Add a stop frame to the LPC frames in the output.                                                                |

Advanced options are:

|                                   |                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-preemphasis` _value_            | Apply a the given pre-emphasis for high frequencies of the input sound file (default = 0.9373).                                                                                                                                                                                                                                                                                                                                 |
| `-voicedthreshold` _value_        | Set the autocorrelation threshold above which frames are considered voiced (default = 0.25).                                                                                                                                                                                                                                                                                                                                    |
| `-dontfixpitchoutliers`           | Disable the processing step that finds and fixes outliers of the pitch. Such outliers introduce shrieking artifacts, especially with the TMS52xx speech synthesizer interpolating between the pitches of subsequent frames.                                                                                                                                                                                                     |
| `-dontfixvoicedjittering`         | Disable the processing step that finds and fixes jittering between voiced and unvoiced frames. Notably spurious unvoiced frames sound like loud pops.                                                                                                                                                                                                                                                                           |
| `-lpcwindowsize` _value_          | Set the sliding window size over which to compute the initial LPC coefficients for a frame of 200 samples, expressed as a number of samples (default = 512). Smaller values typically result in slightly crispier but also more noisy speech. Larger values typically result in smoother but also more slurred speech.                                                                                                          |
| `-frameoversampling` _count_      | Set the number of smaller steps for computing LPC coefficients for each frame of 200 samples, eventually picking the best matches (default = 1, other values could be 2, 3,...) In theory, trying to match a few slightly shifted sliding frames should lead to better coefficients, especially when the sound switches between voiced and unvoiced. In practice, our implementation doesn't seem to make much of a difference. |
| `-dontoptmizeframes`              | Disable the processing step that optimizes the initial frames found with a traditional LPC algorithm. This step refines the LPC parameters with a smaller window size, based on a simulation of the TMS52xx speech synthesis chip.                                                                                                                                                                                              |
| `-optimizationwindowsize` _value_ | Set the window size over which to optimize the initial LPC coefficients for each frame of 200 samples, expressed as a number of samples (default = 256). Again, smaller values typically result in slightly crispier but also more noisy speech. Larger values typically result in smoother but also more slurred speech.                                                                                                       |
| `-linearpowershift` _value_       | Set the term added to power spectra before taking their logarithms (default = 0.1). A larger shift makes the optimization algorithms more linear than logarithmic. In theory, a logarithic scale is more perceptual. In our experience, a purely logarithmic approach more easily leads to upward outliers that may manifest themselves as loud pops.                                                                           |
| `-dontfixenergytransitions`       | Disable the processing step that fixes transitions from high-energy unvoiced frames to voiced frames. The TMS52xx speech synthesizer may play such transitions as loud pops, due to its simplified internal interpolation.                                                                                                                                                                                                      |
| `-dontfixclampedsamples`          | Disable the processing step that fixes frames with samples that overflow and get clamped in the TMS52xx speech synthesizer. Clamped samples often sound as pops in the output.                                                                                                                                                                                                                                                  |

## Tuning and troubleshooting

If you get unwanted **swings in the pitch** of the resulting speech (like
yodling), you should set a suitable frequency range for the speech fragment.
Typical ranges are 50 to 250 Hz for male voices, 120 to 500 Hz for female
voices, and preferably even narrower ranges than those.

If the resulting speech sounds **too sharp**, you can try decreasing the
pre-emphasis of the high frequencies.

If the resulting speech is **poorly articulated**, you can try increasing the
threshold for voiced frames (aaa, eee, ooo,...) in favor of unvoiced frames
(k, p, t,...) Depending on the quality of the input, this may increase the
number of loud clicks and pops in the output.

Conversely, if the resulting speech contains **too many clicks and pops**, you
can try reducing the threshold for voiced frames, since voiced frames sound
smoother.

If the resulting speech contains **too many clicks and pops** because the input
is noisy, you could also try increasing the LPC window size and the
optimization window size, to smoothen out any artifacts. Conversely, should
the speech sound too slurred, you could try decreasing the window sizes.

## Example workflow

Convert a sound file with male speech to a file with LPC coefficients for the
TMS5200 speech synthesizer:

    java ConvertWavToLpc -tms5200 -minfrequency 50 -maxfrequency 250 speech.wav speech.lpc

You can convert the LPC coefficients to a WAV sound file, in order to quickly
check the quality of the speech in any audio player:

    java ConvertLpcToWav -tms5200 -analog -precise speech.lpc check.wav

We're choosing the common analog dynamic range, but with a full 16-bit
precision, so we can identify any artifacts more easily.

If you're happy with the results, you can embed the LPC speech in an executable
cartridge for the TI-99/4A. First create a TMS video file, in this example only
containing the speech:

    java ComposeVideo -ntsc speech.lpc speech.tms

Then package the video file with a small default player program in a cartridge:

    java PackageVideoInCartridge -title 'MY SONG' speech.tms speech.rpk

Instead of an RPK file `speech.rpk` for the Mame emulator, you can also create
just a raw ROM file like `romc.bin`, for other emulators.

You can then run the application from a RAM cartridge on the computer or in an
emulator like Mame:

    mame ti99_4a -ioport peb -ioport:peb:slot3 speech -cart1 song.rpk

## Related tools

* [ConvertPraatToLpc](ConvertPraatToLpc.md)
* [ConvertLpcToWav](ConvertLpcToWav.md)
* [TuneLpcFile](TuneLpcFile.md)
* [CutLpcFile](CutLpcFile.md)
* [ConvertLpcToText](ConvertLpcToText.md)
* [ComposeVideo](ComposeVideo.md)
* [PackageVideoInCartridge](PackageVideoInCartridge.md)
* [VideoTools](../README.md)

## Alternatives

* [QBox](http://ftp.whtech.com/pc%20utilities/qboxpro.zip) Pro (Windows 3.1,
  TMS5220)
* [BlueWizard](https://github.com/patrick99e99/BlueWizard) (Mac OS X, TMS5220)
* [python_wizard](https://github.com/ptwz/python_wizard) (supports TMS5200)
* [TMS-Express](https://github.com/tornupnegatives/TMS-Express) (TMS5220)
* [Praat](https://www.fon.hum.uva.nl/praat/) with our own
  [ConvertPraatToLpc](ConvertPraatToLpc.md)

## Interesting references

* [Digital Speech Processing, Lecture 13](https://web.ece.ucsb.edu/Faculty/Rabiner/ece259/speech%20course.html),
  Larry Rabiner, Department of Electrical and Computer Engineering, University of California at Santa Barbara.
* [Linear Prediction Coding](http://cs.haifa.ac.il/~nimrod/Compression/Speech/S4LinearPredictionCoding2009.pdf),
  Nimrod Peleg, Department of Computer Science, University of Haifa.
* [LPC Vocoder and Spectral Analysis, course project, report](https://www.clear.rice.edu/elec532/PROJECTS00/vocode/),
  Gambiroza et al, Rice University.
* [Spectral Envelope Extraction](https://ccrma.stanford.edu/~jos/sasp/Spectral_Envelope_Extraction.html),
  Julius O. Smith III, Center for Computer Research in Music and Acoustics (CCRMA), Stanford University.
* [Cepstral Smoothing](https://ccrma.stanford.edu/~jos/SpecEnv/Cepstral_Smoothing.html),
  Julius O. Smith III, Center for Computer Research in Music and Acoustics, Stanford University.
* [Mel scale](https://en.wikipedia.org/wiki/Mel_scale),
  Wikipedia.
* [Discrete Fourier Transform](https://en.wikipedia.org/wiki/Discrete_Fourier_transform),
  Wikipedia.
* [The Speech Synthesizer Module](https://www.unige.ch/medecine/nouspikel/ti99/speech.htm),
  Thierry Nouspikel.
* [TMS 5220 Voice Synthesis Processor Data Manual](https://www.99er.net/download2/index.php?act=view&id=144),
  Texas Instruments.
* {BlueWizard: Reflector.m](https://github.com/patrick99e99/BlueWizard/blob/master/BlueWizard/Reflector.m),
  Patrick Collins.
* [python_wizard: Reflector.py](https://github.com/ptwz/python_wizard/blob/master/pywizard/Reflector.py),
  ptwz.
* [Praat: Sound_and_LPC.cpp](https://github.com/praat/praat/blob/master/LPC/Sound_and_LPC.cpp),
  David Weenink, Phonetic Sciences, University of Amsterdam.
