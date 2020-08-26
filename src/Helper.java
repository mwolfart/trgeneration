import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {
	private static List<String> regexReservedChars = new ArrayList<String>(Arrays.asList(
			"{", "}", "\\", "\"", "(", ")"
			));

	public static boolean lineContainsReservedChar(String line, String ch) {
		if (regexReservedChars.contains(ch)) {
			ch = "\\" + ch;
		}
		return line.matches(".*"+ch+"(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$).*");
	}
	
	public static boolean lineContainsReservedWord(String line, String word) {
		return line.matches(".*\\b"+word+"\\b(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$).*");
	}
	
	public static int getIndexOfReservedString(String text, String lookup) {
		Pattern p = Pattern.compile("\\b"+lookup+"\\b(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$).*");
		Matcher m = p.matcher(text);
		int idx = (m.find() ? m.start() : -1);
		return idx;
	}
	
	public static int getIndexOfReservedChar(String text, String lookup) {
		if (regexReservedChars.contains(lookup)) {
			lookup = "\\" + lookup;
		}
		
		Pattern p = Pattern.compile(lookup+"(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$).*");
		Matcher m = p.matcher(text);
		int match = (m.find() ? m.start() : -1);
		return match;
	}
	
	public static int parseInt(String strInt) {
		int result = -1;
		try {
			Integer.parseInt(strInt);
		} catch(NumberFormatException e) {
		}
		return result;
	}
}
