package fixingE4B;

import java.awt.*;
import java.io.File;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

public class UICreator implements GeneralUI {
	private static File folder = null;
	private static JTextArea txt;
	
	/**
	 * Triggers on "Open Folder" press.
	 * Brings up a dialogue to allow the user to select a folder to open.
	 */
	private void folderOpen() {
		JFileChooser f = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
		f.setDialogTitle("Choose Folder Containing E4B Files - All Files Will Be Imported");
        int returnVal = f.showOpenDialog(null);
        // Ensure the user didn't exit or cancel the dialogue
        if (returnVal == JFileChooser.APPROVE_OPTION) {
        	folder = f.getSelectedFile();
        	// Ensure a folder was actually selected
            if (folder != null) {
            	txt.append("\n\nWorking in " + folder.toString());
            }
        }
	}
	
	/**
	 * Starts execution of the E4B repairs, using the E4B Utils.
	 * Triggered on "Repair E4B Files" press.
	 */
	private void start() {
		try {
			// Only works if a folder was selected.
			if (folder != null) {
				E4BRepairUtils.execute(folder, this);
			}
		} catch (IllegalStateException e) {
			// Used to stop program execution.
			display(e.getMessage());
		}
	}
	
	/*
	 * Creates the visual aspects of the UI.
	 * An object is used for the UI, in order to pass it to the Utils class.
	 * 
	 */
	void generateGUI() {
		//Create UI
		JFrame window = new JFrame("E4B Repair Tool");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(500, 500);
		
		//Create Buttons
		JButton start = new JButton("Repair E4B Files");
		start.addActionListener(e -> start());
		JButton folder = new JButton("Open Folder");
		folder.addActionListener(e -> folderOpen());
		//Check boxes
		JCheckBox eb2 = new JCheckBox("Access to EB2?");
		eb2.addActionListener(e -> E4BRepairUtils.EB2 = eb2.isSelected() ? true : false);
		eb2.setToolTipText("Tick if you have access to the original EB2 files. Allows greater accuracy. Uncheck if unsure.");
		JCheckBox emaxI = new JCheckBox("Emax I banks");
		emaxI.addActionListener(e -> E4BRepairUtils.EmaxI = emaxI.isSelected() ? true : false);
		emaxI.setToolTipText("Tick if the banks come from the Emax I, uncheck if from Emax II. Changes chorus values. Uncheck if unsure.");
		//Create containers
		JPanel lowButtons = new JPanel();
		lowButtons.add(start);
		JPanel highButtons = new JPanel();
		highButtons.add(folder);
		highButtons.add(eb2);
		highButtons.add(emaxI);
		
		// Create text area
		txt = new JTextArea("Welcome! Hover over buttons and checkboxes in the UI for usage explanations.");
		txt.setEditable(false);
		JScrollPane scroll = new JScrollPane(txt);
		// Add to primary window
		window.getContentPane().add(BorderLayout.SOUTH, lowButtons);
		window.getContentPane().add(BorderLayout.NORTH, highButtons);
		window.getContentPane().add(BorderLayout.CENTER, scroll);
		window.setVisible(true);
	}
	
	/**
	 * Calls the create UI method, and creates an instance of this class.
	 * @param args
	 */
	public static void main(String[] args) {
		UICreator instance = new UICreator();
		instance.generateGUI();
	}
	
	@Override
	public String prompt(String question) throws IllegalStateException {
		String returnString = JOptionPane.showInputDialog(null, question, "", JOptionPane.QUESTION_MESSAGE);
		if (returnString == null){
			throw new IllegalStateException("Execution cancelled.");
		}
		return returnString;
	}

	@Override
	public int promptInt(String question) throws IllegalStateException {
		while (true) {
			String input = prompt(question);
			try {
				if (input != null) {
					int num = Integer.parseInt(input);
					return num;
				} else {
					throw new IllegalStateException("Execution cancelled.");
				}
			} catch(NumberFormatException e) {
				JOptionPane.showMessageDialog(null, "Only enter a number!", "", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	@Override
	public void alert(String text) {
		JOptionPane.showMessageDialog(null, text);
	}
	
	@Override
	public void display(String text) {
		txt.append("\n" + text);
	}
	
	@Override
	public boolean promptTF(String text) throws IllegalStateException {
		int selection = JOptionPane.showConfirmDialog(null, text, "Confirm/Deny", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (selection == 2) {
			throw new IllegalStateException("Execution cancelled.");
		} else if (selection == 0) return true;
		else return false;
	}
}
