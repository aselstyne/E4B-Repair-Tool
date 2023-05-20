package fixingE4B;

public interface GeneralUI {
	public String prompt(String question) throws IllegalStateException ;
	public int promptInt(String question) throws IllegalStateException ;
	public boolean promptTF(String question) throws IllegalStateException ;
	public void display(String text);
	public void alert(String text);
}
