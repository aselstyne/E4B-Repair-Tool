package fixingE4B;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Voice {

	private Bank bank;
	private int start;
	private int voiceNum;
	private ArrayList<Zone> zones = new ArrayList<>();
	/**
	 * All byte-type fields are for storing and accessing parameters of the voice.
	 */
	byte groupNum, aux1Send, aux2Send, aux3Send, lowKey, lowKeyFade, highKey, highKeyFade, velocityLow, velocityLowFade,
			velocityHighFade, velocityHigh, realtimeLow, realtimeLowFade, realtimeHighFade, realtimeHigh, transpose,
			courseTune, fineTune, glideRate, fixedPitchToggle, keyMode, chorusWidth, chorusAmt, keyAssignGroup, itd,
			maxSampleOffset, latch, glideCurveType, volume, pan, ampEnvDynRange, filterType, filterQ;
	//The int parameters need values up to 255
	private int numZones, len, filterFreq;
	private byte[] vcaEnv = new byte[12];
	private byte[] vcfEnv = new byte[12];
	private byte[] auxEnv = new byte[12];
	private byte[] lfo1 = new byte[6];
	private byte[] lfo2 = new byte[6];
	private byte[][] cords = new byte[24][3];
	// Delay is spread over two bytes
	private int delayStart, delayEnd;

	/**
	 * Basic constructor for a Voice object
	 * @param b - Bank object, parent bank
	 * @param start - int, starting byte of this voice in the bank
	 * @param num - int, number representing this voice.
	 */
	Voice(Bank b, int start, int num) {
		this.bank = b;
		this.start = start;
		this.len = (this.bank.get(start)& 0xff)  * 256 + (this.bank.get(start + 1)& 0xff);
		this.voiceNum = num;

		this.pull();

		// Set up zones
		int zoneStart = start + 284;
		for (int i = 0; i < this.numZones; i++) {
			zones.add(new Zone(bank, zoneStart));
			zoneStart += 22;
		}
	}

	/**
	 * Returns the length of this voice, in number of bytes
	 * 
	 * @return The length of the voice
	 */
	public int voiceLen() {
		return this.len;
	}

	/*
	 * Returns number of zones in this voice. This cannot be altered in the current version of the E4B model.
	 * @returns int, number of zones in this voice
	 */
	public int numZones() {
		return this.numZones;
	}
	
	/**
	 * Returns the voice number. In a method as this shouldn't be altered
	 * @return int, voice number
	 */
	public int voiceNum() {
		return this.voiceNum;
	}
	
	public ArrayList<Byte> sampleNums() {
		ArrayList<Byte> samples = new ArrayList<>();
		for (Zone z: this.zones) {
			samples.add(z.sampleNum);
		}
		return samples;
	}

	/**
	 * Returns an array of Zone objects, one for each sample zone present in this
	 * voice.
	 * 
	 * @return Zone[] - array of zones in this voice
	 */
	public Zone[] getZones() {
		Zone[] zonesArray = new Zone[this.numZones];
		for (int i = 0; i < numZones; i++) {
			zonesArray[i] = this.zones.get(i);
		}
		return zonesArray;
	}

	/**
	 * Returns a byte array containing the parameters for the specified cord,
	 * starting with cord 1. Cord 0 does not exist. The returned byte array contains
	 * source, then destination, then amount %.
	 * 
	 * @param cordNum - Cord number to have information returned for, 1 <= cordNum
	 *                <= 24
	 * @return byte[] - cord information.
	 * @throws IllegalArgumentException - Invalid cord number
	 */
	public byte[] getCord(int cordNum) {
		if (cordNum < 1 || cordNum > 24) {
			throw new IllegalArgumentException("Cord number must be between 1 and 24, inclusive.");
		}
		return cords[cordNum - 1];
	}

	/**
	 * Returns a byte array containing the parameters for the specified cord,
	 * starting with cord 1. Cord 0 does not exist. The returned byte array contains
	 * source, then destination, then amount %.
	 * 
	 * @param cordNum - Cord number to have information returned for, 1 <= cordNum
	 *                <= 24
	 * @param param   - parameter number to be returned. 0 is source, 1 is
	 *                destination, and 2 is amount.
	 * @return byte - cord parameter information.
	 * @throws IllegalArgumentException - Invalid cord number or parameter number.
	 */
	public byte getCord(int cordNum, int param) {
		if (cordNum < 1 || cordNum > 24) {
			throw new IllegalArgumentException("Cord number must be between 1 and 24, inclusive.");
		} else if (param < 0 || param > 2) {
			throw new IllegalArgumentException("Cord parameter must be between 0 and 2");
		}

		return cords[cordNum - 1][param];
	}

	/**
	 * Sets the byte[] for the specified cord. Cord 0 does not exist.
	 * 
	 * @param cordNum - cord number to be altered, 1 <= cordNum <= 24
	 * @param cord    - cord information, stored as [source, destination, amount]
	 * @throws IllegalArgumentException - Invalid cord number or invalid byte[]
	 *                                  length.
	 */
	public void setCord(int cordNum, byte[] newCord) {
		if (cordNum < 1 || cordNum > 24) {
			throw new IllegalArgumentException("Cord number must be between 1 and 24, inclusive.");
		} else if (newCord.length != 3) {
			throw new IllegalArgumentException("Passed byte[] must be of length 3.");
		}

		this.cords[cordNum - 1] = newCord;
	}

	/**
	 * Sets the byte[] for the specified cord. Cord 0 does not exist.
	 * 
	 * @param cordNum - cord number to be altered, 1 <= cordNum <= 24
	 * @param param - parameter number to be changed. 0 is source, 1 is destination, 2 is amount.
	 * @param newVal - byte, new value to be stored in the specified parameter
	 * @throws IllegalArgumentException - Invalid cord number or an invalid cord
	 *                                  parameter number
	 */
	public void setCord(int cordNum, int param, byte newVal) {
		if (cordNum < 1 || cordNum > 24) {
			throw new IllegalArgumentException("Cord number must be between 1 and 24, inclusive.");
		} else if (param < 0 || param > 2) {
			throw new IllegalArgumentException("Cord parameter must be between 0 and 2");
		}

		this.cords[cordNum - 1][param] = newVal;
	}

	/**
	 * Returns the delay as an int, though it is actually stored as two bytes
	 * 
	 * @return int - delay value
	 */
	public int getDelay() {
		return this.delayStart * 256 + this.delayEnd;
	}

	/**
	 * Sets the delay value to the specified value, pass an int but it gets stored
	 * as two bytes
	 * 
	 * @param d - new decay value, int between 0 and 65535
	 * @throws IllegalArgumentException
	 */
	public void setDelay(int d) {
		if (d > 65535 || d < 0) {
			throw new IllegalArgumentException("Delay value passed is not a 2 byte value");
		}
		this.delayStart = (d / 256);
		this.delayEnd = (d % 256);
	}

	/**
	 * Returns the full byte array for the specified envelope. Index 0 is attack 1
	 * time, index 1 is attack 1 %, index 2 is attack 2 time, index 3 is attack 2 %,
	 * index 4 is decay 1 time, etc.
	 * 
	 * @param envNum - 1 is VCA envelope, 2 is VCF Env, and 3 in AUX Env.
	 * @return byte[] - full array for specified envelope
	 * @throws IllegalArgumentException when an invalid envelope number is passed in
	 */
	public byte[] getEnvelope(int envNum) {
		if (envNum == 1) {
			return this.vcaEnv;
		} else if (envNum == 2) {
			return this.vcfEnv;
		} else if (envNum == 3) {
			return this.auxEnv;
		} else {
			throw new IllegalArgumentException("Valid envelope numbers are 1, 2, and 3.");
		}
	}

	/**
	 * Sets the full byte array for the specified envelope. Index 0 is attack 1
	 * time, index 1 is attack 1 %, index 2 is attack 2 time, index 3 is attack 2 %,
	 * index 4 is decay 1 time, etc.
	 * 
	 * @param envNum - 1 is VCA envelope, 2 is VCF Env, and 3 in AUX Env.
	 * @param env    - byte[], new values to be stored in the specified envelope
	 * @throws IllegalArgumentException when an invalid envelope number is passed in
	 */
	public void setEnvelope(int envNum, byte[] env) {
		if (env.length != 12) {
			throw new IllegalArgumentException("Invalid byte[] length for envelope");
		}

		if (envNum == 1) {
			this.vcaEnv = env;
		} else if (envNum == 2) {
			this.vcfEnv = env;
		} else if (envNum == 3) {
			this.auxEnv = env;
		} else {
			throw new IllegalArgumentException("Valid envelope numbers are 1, 2, and 3.");
		}
	}

	/**
	 * Returns one parameter from the specified envelope. 0 is attack 1 time, 1 is
	 * attack 1 %, 2 is attack 2 time, 3 is attack 2 %, 4 is decay 1 time, 5 is
	 * decay 1 %, etc.
	 * 
	 * @param envNum - 1 is VCA envelope, 2 is VCF Env, and 3 in AUX Env.
	 * @param param  - int, the parameter you want to retrieve. See general JavaDoc
	 *               comment for legend.
	 * @return byte - value stored in the specified parameter
	 * @throws IllegalArgumentException when an invalid envelope number is passed
	 *                                  in, or when an illegal parameter number is
	 *                                  used
	 */
	public byte getEnvelope(int envNum, int param) {
		if (param < 0 || param > 11) {
			throw new IllegalArgumentException("Illegal parameter number");
		}

		if (envNum == 1) {
			return this.vcaEnv[param];
		} else if (envNum == 2) {
			return this.vcfEnv[param];
		} else if (envNum == 3) {
			return this.auxEnv[param];
		} else {
			throw new IllegalArgumentException("Valid envelope numbers are 1, 2, and 3.");
		}
	}

	/**
	 * Returns one parameter from the specified envelope. 0 is attack 1 time, 1 is
	 * attack 1 %, 2 is attack 2 time, 3 is attack 2 %, 4 is decay 1 time, 5 is
	 * decay 1 %, etc.
	 * 
	 * @param envNum - 1 is VCA envelope, 2 is VCF Env, and 3 in AUX Env.
	 * @param param  - int, the parameter you want to retrieve. See general JavaDoc
	 *               comment for legend.
	 * @param val    - value to be stored in the specified parameter
	 * @throws IllegalArgumentException when an invalid envelope number is passed
	 *                                  in, or when an illegal parameter number is
	 *                                  used
	 */
	public void setEnvelope(int envNum, int param, byte val) {
		if (param < 0 || param > 11) {
			throw new IllegalArgumentException("Illegal parameter number");
		}

		if (envNum == 1) {
			this.vcaEnv[param] = val;
		} else if (envNum == 2) {
			this.vcfEnv[param] = val;
		} else if (envNum == 3) {
			this.auxEnv[param] = val;
		} else {
			throw new IllegalArgumentException("Valid envelope numbers are 1, 2, and 3.");
		}
	}

	/**
	 * Returns the full byte array for the specified LFO. Index 0 is frequency,
	 * index 1 is shape, 2 is delay, 3 is variation, 4 is Key Sync, 5 is lag rate.
	 * 
	 * @param lfoNum - LFO number you want the byte array for
	 * @return byte[] - full array for specified LFO
	 * @throws IllegalArgumentException when an invalid LFO number is passed in
	 */
	public byte[] getLFO(int lfoNum) {
		if (lfoNum == 1) {
			return this.lfo1;
		} else if (lfoNum == 2) {
			return this.lfo2;
		} else {
			throw new IllegalArgumentException("Valid LFO numbers are 1 or 2.");
		}
	}

	/**
	 * Stores the full byte array for the specified LFO. Index 0 is frequency, index
	 * 1 is shape, 2 is delay, 3 is variation, 4 is Key Sync, 5 is lag rate.
	 * 
	 * @param lfoNum - LFO number you want the byte array for
	 * @param newLFO - byte[], full array to be stored in the specified LFO
	 * @throws IllegalArgumentException when an invalid LFO number is passed in
	 */
	public void setLFO(int lfoNum, byte[] newLFO) {
		if (newLFO.length != 6) {
			throw new IllegalArgumentException("Invalid byte[] length for LFO");
		}

		if (lfoNum == 1) {
			this.lfo1 = newLFO;
		} else if (lfoNum == 2) {
			this.lfo2 = newLFO;
		} else {
			throw new IllegalArgumentException("Valid LFO numbers are 1 or 2.");
		}
	}

	/**
	 * Returns the value of the specified parameter for the specified LFO. Parameter
	 * 0 is frequency, 1 is shape, 2 is delay, 3 is variation, 4 is Key Sync, 5 is
	 * lag rate.
	 * 
	 * @param lfoNum - LFO number you want the byte array for
	 * @param int,   parameter number to be retrieved.
	 * @return byte - specified parameter value
	 * @throws IllegalArgumentException when an invalid LFO number is passed in, or
	 *                                  a bad parameter number
	 */
	public byte getLFO(int envNum, int param) {
		if (param < 0 || param > 5) {
			throw new IllegalArgumentException("Illegal parameter number");
		}

		if (envNum == 1) {
			return this.lfo1[param];
		} else if (envNum == 2) {
			return this.lfo2[param];
		} else {
			throw new IllegalArgumentException("Valid LFO numbers are 1 or 2");
		}
	}

	/**
	 * Returns the value of the specified parameter for the specified LFO. Parameter
	 * 0 is frequency, 1 is shape, 2 is delay, 3 is variation, 4 is Key Sync, 5 is
	 * lag rate.
	 * 
	 * @param lfoNum - LFO number you want the byte array for
	 * @param int,   parameter number to be retrieved.
	 * @param val    - byte to be store in the specified parameter value
	 * @throws IllegalArgumentException when an invalid LFO number is passed in, or
	 *                                  a bad parameter number
	 */
	public void setLFO(int envNum, int param, byte val) {
		if (param < 0 || param > 5) {
			throw new IllegalArgumentException("Illegal parameter number");
		}

		if (envNum == 1) {
			this.lfo1[param] = val;
		} else if (envNum == 2) {
			this.lfo2[param] = val;
		} else {
			throw new IllegalArgumentException("Valid LFO numbers are 1 or 2");
		}
	}
	
	/**
	 * Give the filter frequency value. This is between 0-255, stored as an int.
	 * @return int, filter frequency value.
	 */
	public int getFilterFreq() {
		return this.filterFreq;
	}
	
	public void setFilterFreq(int newFreq) {
		if (newFreq < 0 || newFreq > 255) {
			throw new IllegalArgumentException("Filter frequency values must be between 0 and 255.");
		}
		this.filterFreq = newFreq;
	}

	/**
	 * Pulls data from the bank byte array into the fields.
	 */
	private void pull() {
		this.numZones = (this.bank.get(start + 2)& 0xff);
		this.groupNum = this.bank.get(start + 3);
		this.aux1Send = this.bank.get(start + 7);
		this.aux2Send = this.bank.get(start + 9);
		this.aux3Send = this.bank.get(start + 11);
		this.lowKey = this.bank.get(start + 12);
		this.lowKeyFade = this.bank.get(start + 13);
		this.highKeyFade = this.bank.get(start + 14);
		this.highKey = this.bank.get(start + 15);
		this.velocityLow = this.bank.get(start + 16);
		this.velocityLowFade = this.bank.get(start + 17);
		this.velocityHighFade = this.bank.get(start + 18);
		this.velocityHigh = this.bank.get(start + 19);
		this.realtimeLow = this.bank.get(start + 20);
		this.realtimeLowFade = this.bank.get(start + 21);
		this.realtimeHighFade = this.bank.get(start + 22);
		this.realtimeHigh = this.bank.get(start + 23);
		this.delayStart = (this.bank.get(start + 26)& 0xff);
		this.delayEnd = (this.bank.get(start + 27)& 0xff);
		this.transpose = this.bank.get(start + 32);
		this.courseTune = this.bank.get(start + 33);
		this.fineTune = this.bank.get(start + 34);
		this.glideRate = this.bank.get(start + 35);
		this.fixedPitchToggle = this.bank.get(start + 36);
		this.keyMode = this.bank.get(start + 37);
		this.chorusWidth = this.bank.get(start + 39);
		this.chorusAmt = this.bank.get(start + 40);
		this.keyAssignGroup = this.bank.get(start + 41);
		this.itd = this.bank.get(start + 42);
		this.maxSampleOffset = this.bank.get(start + 47);
		this.latch = this.bank.get(start + 48);
		this.glideCurveType = this.bank.get(start + 51);
		this.volume = this.bank.get(start + 52);
		this.pan = this.bank.get(start + 53);
		this.ampEnvDynRange = this.bank.get(start + 55);
		this.filterType = this.bank.get(start + 56);
		this.filterFreq = (this.bank.get(start + 58)& 0xff);
		this.filterQ = this.bank.get(start + 59);

		// Load envelope parameters
		for (int i = 0; i < 12; i++) {
			this.vcaEnv[i] = this.bank.get(start + 108 + i);
			this.vcfEnv[i] = this.bank.get(start + 122 + i);
			this.auxEnv[i] = this.bank.get(start + 136 + i);
		}

		// Load LFO parameters
		for (int i = 0; i < 5; i++) {
			this.lfo1[i] = this.bank.get(start + 150 + i);
			this.lfo2[i] = this.bank.get(start + 158 + i);
		}
		this.lfo1[5] = this.bank.get(start + 165);
		this.lfo2[5] = this.bank.get(start + 167);

		// Load cord info into 2D array
		for (int c = 0; c < 24; c++) {
			for (int p = 0; p < 3; p++) {
				this.cords[c][p] = this.bank.get(start + 188 + 4 * c + p);
			}
		}
	}

	/**
	 * Pushes data stored in the fields back to the bank's byte[]
	 */
	public void push() {
		// Push all zones
		for (Zone z : zones) {
			z.push();
		}

		// Push Parameters
		this.bank.set(start + 2, (byte)this.numZones);
		this.bank.set(start + 3, this.groupNum);
		this.bank.set(start + 7, this.aux1Send);
		this.bank.set(start + 9, this.aux2Send);
		this.bank.set(start + 11, this.aux3Send);
		this.bank.set(start + 12, this.lowKey);
		this.bank.set(start + 13, this.lowKeyFade);
		this.bank.set(start + 14, this.highKeyFade);
		this.bank.set(start + 15, this.highKey);
		this.bank.set(start + 16, this.velocityLow);
		this.bank.set(start + 17, this.velocityLowFade);
		this.bank.set(start + 18, this.velocityHighFade);
		this.bank.set(start + 19, this.velocityHigh);
		this.bank.set(start + 20, this.realtimeLow);
		this.bank.set(start + 21, this.realtimeLowFade);
		this.bank.set(start + 22, this.realtimeHighFade);
		this.bank.set(start + 23, this.realtimeHigh);
		this.bank.set(start + 26, (byte)this.delayStart);
		this.bank.set(start + 27, (byte)this.delayEnd);
		this.bank.set(start + 32, this.transpose);
		this.bank.set(start + 33, this.courseTune);
		this.bank.set(start + 34, this.fineTune);
		this.bank.set(start + 35, this.glideRate);
		this.bank.set(start + 36, this.fixedPitchToggle);
		this.bank.set(start + 37, this.keyMode);
		this.bank.set(start + 39, this.chorusWidth);
		this.bank.set(start + 40, this.chorusAmt);
		this.bank.set(start + 41, this.keyAssignGroup);
		this.bank.set(start + 42, this.itd);
		this.bank.set(start + 47, this.maxSampleOffset);
		this.bank.set(start + 48, this.latch);
		this.bank.set(start + 51, this.glideCurveType);
		this.bank.set(start + 52, this.volume);
		this.bank.set(start + 53, this.pan);
		this.bank.set(start + 55, this.ampEnvDynRange);
		this.bank.set(start + 56, this.filterType);
		this.bank.set(start + 58, (byte)this.filterFreq);
		this.bank.set(start + 59, this.filterQ);

		// Push envelope parameters
		for (int i = 0; i < 12; i++) {
			this.bank.set(start + 108 + i, this.vcaEnv[i]);
			this.bank.set(start + 122 + i, this.vcfEnv[i]);
			this.bank.set(start + 136 + i, this.auxEnv[i]);
		}

		// Push LFO parameters
		for (int i = 0; i < 5; i++) {
			this.bank.set(start + 150 + i, this.lfo1[i]);
			this.bank.set(start + 158 + i, this.lfo2[i]);
		}
		this.bank.set(start + 165, this.lfo1[5]);
		this.bank.set(start + 167, this.lfo2[5]);

		// Push cord info from 2D array
		for (int c = 0; c < 24; c++) {
			for (int p = 0; p < 3; p++) {
				this.bank.set(start + 188 + 4 * c + p, this.cords[c][p]);
			}
		}
	}

	@Override
	public String toString() {
		return "Voice Number: " + this.voiceNum + "  Voice Length: " + this.len + "  Num Zones: " + this.numZones;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(auxEnv);
		result = prime * result + Arrays.deepHashCode(cords);
		result = prime * result + Arrays.hashCode(lfo1);
		result = prime * result + Arrays.hashCode(lfo2);
		result = prime * result + Arrays.hashCode(vcaEnv);
		result = prime * result + Arrays.hashCode(vcfEnv);
		result = prime * result + Objects.hash(ampEnvDynRange, aux1Send, aux2Send, aux3Send, bank, chorusAmt,
				chorusWidth, courseTune, delayEnd, delayStart, filterFreq, filterQ, filterType, fineTune,
				fixedPitchToggle, glideCurveType, glideRate, groupNum, highKey, highKeyFade, itd, keyAssignGroup,
				keyMode, latch, len, lowKey, lowKeyFade, maxSampleOffset, numZones, pan, realtimeHigh, realtimeHighFade,
				realtimeLow, realtimeLowFade, start, transpose, velocityHigh, velocityHighFade, velocityLow,
				velocityLowFade, voiceNum, volume, zones);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Voice))
			return false;
		Voice other = (Voice) obj;
		return ampEnvDynRange == other.ampEnvDynRange && aux1Send == other.aux1Send && aux2Send == other.aux2Send
				&& aux3Send == other.aux3Send && Arrays.equals(auxEnv, other.auxEnv) && Objects.equals(bank, other.bank)
				&& chorusAmt == other.chorusAmt && chorusWidth == other.chorusWidth
				&& Arrays.deepEquals(cords, other.cords) && courseTune == other.courseTune && delayEnd == other.delayEnd
				&& delayStart == other.delayStart && filterFreq == other.filterFreq && filterQ == other.filterQ
				&& filterType == other.filterType && fineTune == other.fineTune
				&& fixedPitchToggle == other.fixedPitchToggle && glideCurveType == other.glideCurveType
				&& glideRate == other.glideRate && groupNum == other.groupNum && highKey == other.highKey
				&& highKeyFade == other.highKeyFade && itd == other.itd && keyAssignGroup == other.keyAssignGroup
				&& keyMode == other.keyMode && latch == other.latch && len == other.len
				&& Arrays.equals(lfo1, other.lfo1) && Arrays.equals(lfo2, other.lfo2) && lowKey == other.lowKey
				&& lowKeyFade == other.lowKeyFade && maxSampleOffset == other.maxSampleOffset
				&& numZones == other.numZones && pan == other.pan && realtimeHigh == other.realtimeHigh
				&& realtimeHighFade == other.realtimeHighFade && realtimeLow == other.realtimeLow
				&& realtimeLowFade == other.realtimeLowFade && start == other.start && transpose == other.transpose
				&& Arrays.equals(vcaEnv, other.vcaEnv) && Arrays.equals(vcfEnv, other.vcfEnv)
				&& velocityHigh == other.velocityHigh && velocityHighFade == other.velocityHighFade
				&& velocityLow == other.velocityLow && velocityLowFade == other.velocityLowFade
				&& volume == other.volume && Objects.equals(zones, other.zones);
	}

}
