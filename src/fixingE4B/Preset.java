package fixingE4B;

import java.util.ArrayList;
import java.util.Objects;

public class Preset {

	private String presetName = "";
	private Bank bank;
	private int start;
	private int numVoices;
	private int presetLen;
	public byte  presetNum, transpose, volume, initA, initB, initC, initD;
	private ArrayList<Voice> voices = new ArrayList<>();
	
	protected Preset(Bank b, int start) {
		this.bank = b;
		this.start = start;

		this.pull();

		// Create voice array
		int currVoiceStart = this.start + 89;
		for (int v = 0; v < numVoices; v++) {
			voices.add(new Voice(this.bank, currVoiceStart, v+1));
			currVoiceStart += voices.get(v).voiceLen();
		}
	}
	
	/**
	 * Used to get all voice objects contained within this preset.
	 * @return Voice[], containing all voice objects.
	 */
	public Voice[] getVoices() {
		Voice[] voicesArray = new Voice[this.numVoices];
		for (int i = 0; i < numVoices; i++) {
			voicesArray[i] = this.voices.get(i);
		}
		return voicesArray;
	}
	
	/**
	 * Used to get the length of the preset, as stored in the E4B file.
	 * @return Length of this preset.
	 */
	public int getPresetLen() {
		return presetLen;
	}

	/**
	 * Pulls data from the bank into the preset fields.
	 */
	private void pull() {
		this.presetLen = (this.bank.get(this.start + 2) & 0xff) * 65536 + (this.bank.get(this.start + 3) & 0xff) * 256
				+ (this.bank.get(start + 4) & 0xff); // the length of the preset is stored across 3 bytes.
		this.presetNum = this.bank.get(this.start + 6);
		// Create the presetName, reading byte by byte
		for (int c = 0; c < 16; c++) {
			this.presetName += (char) this.bank.get(start + 7 + c);
		}
		//Use & 0xff to remove the sign from a byte value, but it also makes it an int.
		this.numVoices = (this.bank.get(this.start + 25) & 0xff) * 256 + (this.bank.get(this.start + 26) & 0xff);
		this.transpose = this.bank.get(start + 31);
		this.volume = this.bank.get(start + 32);
		this.initA = this.bank.get(start + 61);
		this.initB = this.bank.get(start + 62);
		this.initC = this.bank.get(start + 63);
		this.initD = this.bank.get(start + 64);
	}

	/**
	 * Pushes all data stored in it's variables up to the bank.
	 */
	public void push() {
		for (Voice v : voices) {
			v.push();
		}

		this.bank.set(this.start + 2, (byte) (this.presetLen / 65535));
		this.bank.set(this.start + 3, (byte) ((this.presetLen % 65535) / 256));
		this.bank.set(this.start + 4, (byte) ((this.presetLen % 65535) % 256));
		this.bank.set(this.start + 6, this.presetNum);
		// Create the presetName, reading byte by byte
		for (int c = 0; c < 16; c++) {
			this.bank.set(start + 7 + c, (byte)(this.presetName.charAt(c)));
		}
		this.bank.set(this.start + 25, (byte)(this.numVoices/256));
		this.bank.set(this.start + 26, (byte)(this.numVoices%256));
		this.bank.set(start + 31, this.transpose);
		this.bank.set(start + 32, this.volume);
		this.bank.set(start + 61, this.initA);
		this.bank.set(start + 62, this.initB);
		this.bank.set(start + 63, this.initC);
		this.bank.set(start + 64, this.initD);
		
	}
	
	@Override
	public String toString() {
		return "\nPreset Number: " + this.presetNum + "\nPreset Name: " + this.presetName
				+ "\nNumber of Voices: " + this.numVoices;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bank, initA, initB, initC, initD, numVoices, presetLen, presetName, presetNum, start,
				transpose, voices, volume);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Preset))
			return false;
		Preset other = (Preset) obj;
		return Objects.equals(bank, other.bank) && initA == other.initA && initB == other.initB && initC == other.initC
				&& initD == other.initD && numVoices == other.numVoices && presetLen == other.presetLen
				&& Objects.equals(presetName, other.presetName)
				&& transpose == other.transpose && Objects.equals(voices, other.voices) && volume == other.volume;
	}
}
