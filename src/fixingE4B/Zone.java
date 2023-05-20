package fixingE4B;

import java.util.Objects;

public class Zone {

	
	private Bank bank;
	private int start;
	private int zoneNum;
	
	//Parameter Variables:
	byte lowKey, lowKeyFade, highKey, highKeyFade, velocityLow, velocityLowFade, velocityHighFade, velocityHigh, sampleNum, fineTune, rootKey, volume, pan;
	
	//Constructor:
	public Zone(Bank b, int start) {
		this.start = start;
		this.bank = b;
		this.pull();
	}
	
	
	
	/**
	 * Pulls updated values from the master bank byte array, and sets all instance
	 * variables to the appropriate values.
	 */
	private void pull() {
		this.lowKey = this.bank.get(start);
		this.lowKeyFade = this.bank.get(start+1);
		this.highKeyFade = this.bank.get(start+2);
		this.highKey = this.bank.get(start+3);
		this.velocityLow =  this.bank.get(start+4);
		this.velocityLowFade = this.bank.get(start+5);
		this.velocityHighFade = this.bank.get(start+6);
		this.velocityHigh = this.bank.get(start+7);
		this.sampleNum = this.bank.get(start + 9);
		this.fineTune = this.bank.get(start+11);
		this.rootKey = this.bank.get(start+12);
		this.volume = this.bank.get(start+13);
		this.pan = this.bank.get(start+14);
	}
	
	
	/**
	 * Pushes the values stored in this instance's fields back 
	 * to the master byte array, usually to be written back to a file.
	 */
	public void push() {
		this.bank.set(start, lowKey);
		this.bank.set(start+1, lowKeyFade);
		this.bank.set(start+2, highKeyFade);
		this.bank.set(start+3, highKey);
		this.bank.set(start+4, velocityLow);
		this.bank.set(start+5, this.velocityLowFade);
		this.bank.set(start+6, this.velocityHighFade);
		this.bank.set(start+7, this.velocityHigh);
		this.bank.set(start+9, this.sampleNum);
		if (this.fineTune< (byte)0) {
			this.bank.set(start+10, (byte)255);
		}
		this.bank.set(start+11, this.fineTune);
		this.bank.set(start+12, this.rootKey);
		this.bank.set(start+13, this.volume);
		this.bank.set(start+14, this.pan);
	}
	
	/**
	 * String with typically useful information about the zone
	 * 
	 */
	public String toString() {
		return "Zone Number: " + this.zoneNum +  "  Sample Number: " + this.sampleNum + "  Root Key: " + this.rootKey;
	}



	@Override
	public int hashCode() {
		return Objects.hash(bank, fineTune, highKey, highKeyFade, lowKey, lowKeyFade, pan, rootKey, sampleNum, start,
				velocityHigh, velocityHighFade, velocityLow, velocityLowFade, volume, zoneNum);
	}


	/**
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Zone))
			return false;
		Zone other = (Zone) obj;
		return Objects.equals(bank, other.bank) && fineTune == other.fineTune && highKey == other.highKey
				&& highKeyFade == other.highKeyFade && lowKey == other.lowKey && lowKeyFade == other.lowKeyFade
				&& pan == other.pan && rootKey == other.rootKey && sampleNum == other.sampleNum && start == other.start
				&& velocityHigh == other.velocityHigh && velocityHighFade == other.velocityHighFade
				&& velocityLow == other.velocityLow && velocityLowFade == other.velocityLowFade
				&& volume == other.volume && zoneNum == other.zoneNum;
	}
}
