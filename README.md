# Emax II -> Emulator IV Repair Tool
Tool for repairing sound banks that were translated from the E-mu Emax II to the Emulator IV.

In the process of translating, many parameters in the banks are incorrectly handled. This software searches for such errors, and seeks to correct them.

Parameters fixed include: Pitch bend, VCA attack, VCA decay, VCF attack, VCF decay, delay, pan, pitch fine tune, LFO frequency (flagged for manual review), and chorus (flagged for manual review; automatically fixed on Emax I originating banks). 

Not all potential errors are yet known, so please reach out if you find that your bank is incorrectly repaired with or if you receive an error message.

# Background
The [Emax II](https://en.wikipedia.org/wiki/E-mu_Emax) was a electronic musical instrument released by E-mu Systems in 1989. Specifically, the Emax II is a "sampler," which allows any section of audio to be recorded into the instrument and then mapped to different portions of the keyboard. Along with the sample of audio, the Emax II also had a series of effects that could be applied to the sample to get the desired sound from the keyboard. You can see a demo of the sampler in this [YouTube video](https://www.youtube.com/watch?v=Tke-J_RmoRU).

In 1994, E-mu released the [Emulator IV](https://en.wikipedia.org/wiki/E-mu_Emulator#Emulator_IV_and_EOS), the final hardware sampler produced by the legendary manufacturer. One of its many features was the ability to convert sound banks (which contain sample audio, effects settings, and keyboard mappings) from the Emax II format to the new Emulator IV format. This process, however, was extremely half-baked and produced many errors in the final E4B (Emulator 4 Bank) file.

The purpose of the E4B Repair Tool is to restore these E4B files to a state as close to the original Emax II banks as possible. This was done by painstakingly analysing the binary of the E4B files, to map out how the data is stored. Then, correction tables were created and are used by the software to fix incorrect values.

# Support

If you have an issue with the program, whether it be in running it or in the output, please reach out! Please visit my website, [aselstyne.com](https://aselstyne.com), to find my contact methods.
