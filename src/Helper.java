import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {
	private static List<String> regexReservedChars = new ArrayList<String>(Arrays.asList(
			"{", "}", "\\", "\""
			));

	public static boolean lineContainsReservedChar(String line, String ch) {
		if (regexReservedChars.contains(ch)) {
			ch = "\\" + ch;
		}
		
		return line.matches(ch+"(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$).*");
	}
	
	public static boolean lineContainsReservedWord(String line, String word) {
		return line.matches(".*\\b"+word+"\\b(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$).*");
	}
	
	public static int getIndexOfReservedString(String text, String lookup) {
		if (regexReservedChars.contains(lookup)) {
			lookup = "\\" + lookup;
		}
		
		Pattern p = Pattern.compile(".*\\b"+lookup+"\\b(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$).*");
		Matcher m = p.matcher(text);
		return (m.find() ? m.start() : -1);
	}
}
