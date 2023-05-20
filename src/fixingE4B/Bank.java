package fixingE4B;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Bank {

		byte[] bankBytes;
		int[] presetStartLocs;
		int numPresets;
		private ArrayList<Preset> presets = new ArrayList<>();
		
		public Bank(File bank) throws IOException {
			try {
				this.bankBytes = Files.readAllBytes(bank.toPath());
				this.loadPresets();
				
				//Create preset objects
				for(int i: this.presetStartLocs) {
					this.presets.add(new Preset(this, i));
				}
			} catch(IOException e) {
				throw new IOException("No bank file found at specified location.");
			}
		}
		
		
		private void loadPresets() {
			//Find where the E4P1 preset indicator locations.
			ArrayList<Integer> E4P1Ends = new ArrayList<>();
			for (int i = 0; i < this.bankBytes.length; i++) {
				if (this.bankBytes[i] == 49 && this.bankBytes[i-1] == 80 && this.bankBytes[i-2] == 52 && this.bankBytes[i-3] == 69) {
					E4P1Ends.add(i);
				}
			}
			
			this.numPresets = E4P1Ends.size()/2;
			
			//Remove the file header information from the E4P1 Ends
			for (int i = 0; i < this.numPresets; i++) {
				E4P1Ends.remove(0);
			}
			
			this.presetStartLocs = new int[E4P1Ends.size()];
			//Add all preset start locations to the array
			for (int i = 0; i< E4P1Ends.size(); i++) {
				this.presetStartLocs[i] = E4P1Ends.get(i);
			}
			
		}

		/**
		 * returns the byte at the specified location
		 * @param loc - byte number to be returned, relative to the start of the file
		 * @return byte, value stored at loc
		 */
		public byte get(int loc) {
			return this.bankBytes[loc];
		}
		
		/**
		 * Sets the value of the specified byte location in the file
		 * @param loc - byte number to be altered
		 * @param val - value to be stored in byte at loc
		 */
		void set(int loc, byte val) {
			this.bankBytes[loc] = val;
		}
		
		/**
		 * Returns the array of all Preset objects representing the presets in this bank.
		 * @return Preset[], array with all bank presets.
		 */
		public Preset[] getPresets(){
			Preset[] presetsArray = new Preset[this.numPresets];
			for (int i = 0; i < numPresets; i++) {
				presetsArray[i] = this.presets.get(i);
			}
			return presetsArray;
		}
		
		
//		/**
//		 * Write the data stored in this bank's byte array to an E4B File
//		 * @param outputFile - File object, where the bank data should be written to
//		 * @param sc - Scanner object, for user input.
//		 */
//		public void writeE4B(File outputFile, Scanner sc) {
//			//First, pull in all the data from the objects by calling their push methods.
//			for(Preset p: presets) {
//				p.push();
//			}
//			
//			//Next, move on to writing
//			boolean write = false;
//			try {
//				if (outputFile.createNewFile()) {
//					System.out.println("File created: " + outputFile.getName());
//					write = true;
//				} else {
//					System.out.println("File " + outputFile.getName() + " already exists, do you want to overwrite? (Y/N): ");
//					//System.in.read(new byte[System.in.available()]);
//					
//					String ans = "";
//					ans = sc.nextLine().toUpperCase().strip();
//					
//					if (ans.equals("Y")) {
//						write = true;
//					}
//				}
//			} catch (IOException e) {
//				System.out.println("Cannot create file! May be a priviledge issue?");
//				e.printStackTrace();
//			}
//			
//			//This writes the output stream to the new file
//		    if (write) {
//			    try (FileOutputStream outStream = new FileOutputStream(outputFile)){
//			    	outStream.write(bankBytes);
//			    	System.out.println("Bank successfully saved.");
//			    } catch (FileNotFoundException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//		    } else {
//		    	System.out.println("Bank write operation cancelled");
//		    }
//		}
		
		/**
		 * Write the data stored in this bank's byte array to an E4B File
		 * @param outputFile - File object, where the bank data should be written to
		 */
		public void writeE4B(File outputFile) throws IllegalStateException {
			//First, pull in all the data from the objects by calling their push methods.
			for(Preset p: presets) {
				p.push();
			}
			
			//Next, move on to writing
			boolean write = false;
			try {
				if (outputFile.createNewFile()) {
					E4BRepairUtils.parent.display("File created: " + outputFile.getName());
					write = true;
				} else {
					write = E4BRepairUtils.parent.promptTF("File " + outputFile.getName() + " already exists in the output folder, do you want to overwrite it?");
				}
			} catch (IOException e) {
				E4BRepairUtils.parent.display("Cannot create file! Might be a priviledge issue?");
				e.printStackTrace();
			}
			
			//This writes the output stream to the new file
		    if (write) {
			    try (FileOutputStream outStream = new FileOutputStream(outputFile)){
			    	outStream.write(bankBytes);
			    	System.out.println("Bank successfully saved.");
			    } catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    } else {
		    	System.out.println("Bank write operation cancelled");
		    }
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(bankBytes);
			result = prime * result + Arrays.hashCode(presetStartLocs);
			result = prime * result + Objects.hash(numPresets, presets);
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Bank))
				return false;
			Bank other = (Bank) obj;
			return Arrays.equals(bankBytes, other.bankBytes) && numPresets == other.numPresets
					&& Arrays.equals(presetStartLocs, other.presetStartLocs) && Objects.equals(presets, other.presets);
		}
}