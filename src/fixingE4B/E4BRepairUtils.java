package fixingE4B;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class E4BRepairUtils {

	private static HashMap<Integer, Integer> pb = new HashMap<>();
	private static HashMap<Integer, Integer> delay = new HashMap<>();
	private static HashMap<Integer, Integer> tuning = new HashMap<>();
	private static HashMap<Integer, Integer> pan = new HashMap<>();
	static GeneralUI parent;
	private static String folderName;
	public static boolean EB2 = false; // True if you have access to the original EB2 File
	public static boolean EmaxI = false; // True if this is an EmaxI bank, important for chorusing

	private static HashMap<Integer, Integer> loadCSV(String FileName) throws FileNotFoundException {
		File CSV = new File(".\\CSV\\" + FileName);
		Scanner sc = new Scanner(CSV);
		HashMap<Integer, Integer> m = new HashMap<>();
		int key, val = 0;
		while (sc.hasNext()) {
			String currLine = sc.next();
			key = Integer.parseInt(currLine.substring(0, currLine.indexOf(',')));
			val = Integer.parseInt(currLine.substring(currLine.indexOf(',') + 1, currLine.length()));
			m.put(key, val);
		}
		sc.close();
		return m;
	}

	/**
	 * Writes a log file using the specified list of log lines, where the ArrayList
	 * is organized so that all the lines relating to one bank are stored in one
	 * index in the higher order of the list.
	 * 
	 * @param logList
	 */
	private static void writeLog(ArrayList<ArrayList<String>> logList, File[] banks) {

		// Create the log file, with parameters that the user needs to fix.
		String logName = folderName.replace("\\", "_") + "_log.txt";
		File logFile = new File(".\\output\\" + folderName + "\\" + logName);
		FileWriter logWriter;
		try {
			logWriter = new FileWriter(logFile);
			for (int i = 0; i < logList.size(); i++) {
				logWriter.append(banks[i].getName());
				logWriter.append("\n\n");
				// iterate through each line in the log files
				for (String line : logList.get(i)) {
					logWriter.append("\t" + line + "\n");
				}
				logWriter.append("\n\n");
			}
			logWriter.close();

			parent.display("\n\nLog file \"" + logFile.getName() + "\" written, "
					+ "with further instructions for correcting banks. "
					+ "If file is blank, no further steps.\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			parent.alert("Log file could not be created, bank files may still have been corrected and written though.");
		}

	}

	/**
	 * Fixes the specified parameter using a CSV lookup table
	 * 
	 * @param param - fixingE4B.Param enum, parameter to be fixed
	 * @param p     - Preset object, used for error names
	 * @param v     - voice object, used for corrections and errors.
	 */
	private static void fixParam(Param param, Preset p, Voice v) throws IllegalStateException{
		int valueError = 1000;
		if (param == Param.PitchBend) {
			// Pitch Bend is stored in cord 2
			int currPB = v.getCord(2, 2);
			Integer destPB = pb.get(currPB);
			if (destPB != null)
				v.setCord(2, 2, (byte) (int) destPB);
			else
				valueError = currPB;
		} else if (param == Param.Delay) {
			int currDelay = v.getDelay();
			Integer destDelay = delay.get(currDelay);
			if (destDelay != null)
				v.setDelay(destDelay);
			else
				valueError = currDelay;
		} else if (param == Param.Tuning) {
			int currTune = v.fineTune;
			Integer destTune = tuning.get(currTune);
			if (destTune != null)
				v.fineTune = (byte) (int) destTune;
			else
				valueError = currTune;
		} else if (param == Param.Pan) {
			int currPan = v.pan;
			Integer destPan = pan.get(currPan);
			if (destPan != null)
				v.pan = (byte) (int) destPan;
			else
				valueError = currPan;

		} else
			throw new IllegalArgumentException(param + " is not a fixable parameter.");

		// Check for unknown values found in the preset
		if (valueError != 1000)
			throw new IllegalStateException("Unknown " + param + " value in preset " + p.presetNum + ", voice "
					+ v.voiceNum() + " : " + valueError);
	}

	/**
	 * Fixes the specified parameter in a Sample Zone using a CSV lookup table
	 * 
	 * @param param fixingE4B.Param enum, parameter to be fixed
	 * @param p     Preset object, used for error names
	 * @param v     voice object, used for corrections and errors.
	 * @param z     Zone object that needs to be corrected
	 */
	private static void fixParam(Param param, Preset p, Voice v, Zone z) throws IllegalStateException{
		int valueError = 1000;
		if (param == Param.Tuning) {
			int currTune = z.fineTune;
			Integer destTune = tuning.get(currTune);
			if (destTune != null)
				z.fineTune = (byte) (int) destTune;
			else
				valueError = currTune;
		} else if (param == Param.Pan) {
			int currPan = z.pan;
			Integer destPan = pan.get(currPan);
			if (destPan != null)
				z.pan = (byte) (int) destPan;
			else
				valueError = currPan;
		} else
			throw new IllegalArgumentException(param + " is not a fixable parameter.");

		// Check for unknown values found in the preset
		if (valueError != 1000)
			throw new IllegalStateException("Unknown " + param + " value in preset " + p.presetNum + ", voice "
					+ v.voiceNum() + ", sample " + z.sampleNum + " : " + valueError);
	}

	/**
	 * Contains the fixing algorithm. Calls other methods to fix a variety of
	 * parameters
	 * 
	 * @param bankFile - File object, E4B file that needs to be fixed
	 * @return
	 * @throws IOException 
	 */
	private static ArrayList<String> processBank(File bankFile) throws IOException, IllegalStateException {
		Bank bank = new Bank(bankFile);
		ArrayList<String> log = new ArrayList<>();

		parent.display("\n\nBank name: " + bankFile.getName());

		// Load all CSV Files
		try {
			tuning = loadCSV("tuning.csv");
			pb = loadCSV("Cord2PB.csv");
			delay = loadCSV("delay.csv");
			pan = loadCSV("pan.csv");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		// Check to make sure that all voice processing segments begin the same distance
		// from the 1
		for (Preset p : bank.getPresets()) {
			parent.display(p.toString());
			for (Voice v : p.getVoices()) {
				// General Parameters:
				v.setLFO(1, 1, (byte) 0); // LFO 1 shape
				v.setLFO(2, 1, (byte) 0);// LFO 2 shape
				v.chorusWidth = (byte) 128; // Chorusing stereo width

				// VCA Attack Time
				double x = v.getEnvelope(1, 2);
				double y = 1.3 * (Math.pow(2, 0.1 * (x - 59)));
				byte newAttack = (byte) ((Math.log((y / 2) / 1.3) / Math.log(2)) / 0.1 + 59);
				if (newAttack <0 && x > 0)
					//Fixes wrap around error.
					newAttack = (byte)x;
				v.setEnvelope(1, 2, newAttack);
				// VCA Release Time
				x = v.getEnvelope(1, 8);
				y = 1.3 * (Math.pow(2, 0.1 * (x - 59)));
				byte newRelease = (byte) ((Math.log((y / 2) / 1.3) / Math.log(2)) / 0.1 + 59);
				if (newRelease <0 && x > 0)
					//Fixes wrap around error.
					newRelease = (byte)x;
				v.setEnvelope(1, 8, newRelease);

				// VCF Release Time
				v.setEnvelope(2, 8, (byte) (v.getEnvelope(2, 8) - 10));

				// Fix odd decay 1 % 99.2 in VCA and VCF
				for (int e = 1; e <= 2; e++) {
					if (v.getEnvelope(e, 5) == (byte) 126)
						v.setEnvelope(e, 5, (byte) 127);
				}

				// Fix VCA decay 2 66.577
				if (v.getEnvelope(1, 6) == (byte) 118) {
					v.setEnvelope(1, 6, (byte) 0);
					v.setEnvelope(1, 7, (byte) 127);
				}

				// Fix VCF decay 2 time 18.822
				if (v.getEnvelope(2, 6) == (byte) 99) {
					v.setEnvelope(2, 6, (byte) 0);
					v.setEnvelope(2, 7, (byte) 127);
				}

				// VCF Attack 1 78.
				if (v.getEnvelope(2, 1) == (byte) 99)
					v.setEnvelope(2, 1, (byte) 127);

				// Filter Envelope Amount, on cord 6.
				byte oldFilterEnvAmt = v.getCord(6, 2);
				double newFilterEnvAmt = 1.27 * oldFilterEnvAmt;
				v.setCord(6, 2, (byte) newFilterEnvAmt);

				// Pitch bend
				E4BRepairUtils.fixParam(Param.PitchBend, p, v);
				E4BRepairUtils.fixParam(Param.Delay, p, v);
				E4BRepairUtils.fixParam(Param.Tuning, p, v);
				E4BRepairUtils.fixParam(Param.Pan, p, v);

				// Fix Zone parameters:
				if (v.numZones() > 1) {
					for (Zone z : v.getZones()) {
						E4BRepairUtils.fixParam(Param.Tuning, p, v, z);
						E4BRepairUtils.fixParam(Param.Pan, p, v, z);
					}
				}

				// This is all about the Chorus %
				if (EmaxI) {
					if (v.chorusAmt != 0)
						v.chorusAmt = (byte) ((5.0 / 20.0) * 127.0); // Emax I had a fixed chorus amount.
				} else {
					if (EB2) {
						if (v.chorusAmt != 0) {
							double EB2Chorus = parent.promptInt("Voice " + v.voiceNum() + " in preset " + p.presetNum + " containing sample(s) " + v.sampleNums()
							+ " in bank " + bankFile.getName() + " has chorusing.\nWhat is the EB2 chorus depth?");
							v.chorusAmt = (byte) ((EB2Chorus / 20.0) * 77.0); // Maximum chorus amount is 60% 
						}
					} else {
						// Chorus must be fixed by ear otherwise.
						if (v.chorusAmt != 0) {
							parent.display("\tVoice " + v.voiceNum() + " containing sample(s) " + v.sampleNums()
									+ " in bank " + bankFile.getName() + " has chorusing.\n\tPlease correct this by ear.");
							log.add("Correct chorus in preset " + p.presetNum + ", voice " + v.voiceNum()
									+ " containing sample(s) " + v.sampleNums());
						}
					}
				}

				// This looks at LFO rate
				for (int i = 1; i <= 24; i++) {
					// A cord source of 96 corresponds to LFO
					if (v.getCord(i, 0) == 96) {
						if (v.getCord(i, 2) != 0) {
							parent.display("\tVoice " + v.voiceNum() + " containing sample(s) " + v.sampleNums()
									+ " in bank " + bankFile.getName() + " has an active LFO 1.\n\tPlease correct this by ear.");
							log.add("Correct LFO 1 frequency in preset " + p.presetNum + ", voice " + v.voiceNum()
									+ " containing sample(s) " + v.sampleNums());
						}
					}
				}
			}
		}
		// Create the file first, so that the parent folder can be created first.
		File outputFileObj = new File(".\\output\\" + folderName + "\\" + bankFile.getName());
		outputFileObj.getParentFile().mkdirs();

		bank.writeE4B(outputFileObj);
		return log;
	}
	
	public static void execute(File folder, GeneralUI p) {
		// if (true) throw new IllegalStateException("test");
		parent = p;
		File[] folderContents = folder.listFiles();
		folderName = folder.toString();
		folderName = folderName.substring(folderName.lastIndexOf("\\") + 1, folderName.length());
		
		// Remove all non-E4B banks:
		ArrayList<File> bankList = new ArrayList<>();
		for (int i = 0; i < folderContents.length; i++) {
			String bankName = folderContents[i].getName().toUpperCase();
			if (bankName.endsWith(".E4B"))
				// Add a file to the list if it's an E4B
				bankList.add(folderContents[i]);
		}
		//Check if any banks were actually found - stop execution if none.
		if (bankList.size() == 0) throw new IllegalStateException("No banks found in the selected folder.");
		
		// Create a regular array from the ArrayList
		File[] banks = Arrays.copyOf(bankList.toArray(), bankList.size(), File[].class);
		parent.display(banks.length + " banks found in the specified folder.");
		// 2D ArrayList used for writing the log when the program exits
		ArrayList<ArrayList<String>> finalLog = new ArrayList<>();

		try {
			for (File b : banks) {
				finalLog.add(processBank(b));
			}
			writeLog(finalLog, banks);
		} catch (IOException e) {
			e.printStackTrace();
			writeLog(finalLog, banks);
		} catch (Exception e) {
			// For every exception, the log should still be written
			writeLog(finalLog, banks);
			IllegalStateException e2 = new IllegalStateException(e.getMessage() + " Log still written.");
			//String oldMessage = e.getMessage();
			//Exception newException = new e.getClass() 
			//throw new e.getClass()(oldMessage + " Log still written.");
			throw e2;
		} 
	}

//	public static void main(String[] args) throws IOException {
//		//Create UI
////		JFrame window = new JFrame("E4B Repair Tool");
////		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
////		window.setSize(500,500);
////		JButton start = new JButton("Click here to start");
////		JButton folder = new JButton("Open Folder");
////		JPanel buttons = new JPanel();
////		buttons.add(start);
////		buttons.add(folder);
////		window.getContentPane().add(buttons);
////		window.setVisible(true);
////		
////		JFileChooser f = new JFileChooser();
////        f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
////        f.showSaveDialog(null);
////
////        System.out.println(f.getCurrentDirectory());
////        System.out.println(f.getSelectedFile());
//		// String bankName = "B.000-FLYS ALAN.E4B";
//		// String dir = "C:\\Users\\Alexa\\Music\\" + folderName + "\\" + bankName;
//		File[] folderContents = new File("C:\\Users\\Alexa\\Music\\" + folderName).listFiles();
//		// Remove all non-E4B banks:
//		ArrayList<File> bankList = new ArrayList<>();
//		for (int i = 0; i < folderContents.length; i++) {
//			String bankName = folderContents[i].getName().toUpperCase();
//			if (bankName.endsWith(".E4B"))
//				// Add a file to the list if it's an E4B
//				bankList.add(folderContents[i]);
//		}
//		// Create a regular array from the ArrayList
//		File[] banks = Arrays.copyOf(bankList.toArray(), bankList.size(), File[].class);
//		System.out.println(banks.length + " banks found in the specified folder.");
//		// 2D ArrayList used for writing the log when the program exits
//		ArrayList<ArrayList<String>> finalLog = new ArrayList<>();
//
//		try {
//			for (File b : banks) {
//				finalLog.add(processBank(b));
//			}
//			writeLog(finalLog, banks);
//		} catch (Exception e) {
//			// For every exception, the log should still be written
//			e.printStackTrace();
//			writeLog(finalLog, banks);
//		}
//	}
}
