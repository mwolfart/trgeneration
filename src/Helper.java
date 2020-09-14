import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	
	public static void createDir(String dirPath) {
		dirPath = dirPath.replace("<", "{").replace(">", "}");
		File dir = new File(dirPath);
		if (dir.exists()) return;
		if (!dir.mkdir()) {
			System.err.println("Could not create directory " + dirPath);
			System.exit(2);
		}
	}
	
	public static void writeFile(String filePath, String content) {
		filePath = filePath.replace("<", "{").replace(">", "}");
		try {
			File file = new File(filePath);
			FileWriter fr = new FileWriter(file, true);
			fr.write(content);
			fr.close();
		} catch (IOException e) {
			System.err.println("Error in writing file " + filePath);
		}
	}
	
	public static void writePng(String path, String dot) {
		path = path.replace("<", "{").replace(">", "}");
		File out = new File(path);
		GraphViz gv = new GraphViz();
		gv.writeGraphToFile(gv.getGraph(dot, "png"), out);
	}
	
	public static <T> ArrayList<T> initArray(T firstElement) {
		return new ArrayList<T>(Arrays.asList(firstElement));
	}
	
	public static <T> ArrayList<T> initArray(T[] elements) {
		return new ArrayList<T>(Arrays.asList(elements));
	}
	
	/* TODO: maybe refactor the three next functions */
	public static int findStartOfBlock(List<String> sourceCode, int startingLine) {
		return findStartOfBlock(sourceCode, startingLine, false);
	}

	/* TODO: Maybe reenginer this */
	public static int findStartOfBlock(List<String> sourceCode, int startingLine, boolean useBlockLines) {
		int curLineId = startingLine;
		int openingLine = -1;
		int depth = 0;
		
		while (curLineId >= 0 && openingLine == -1) {
			String curLine = sourceCode.get(curLineId);
			if (Helper.lineContainsReservedChar(curLine, "}")) {
				depth++;
			} else if (Helper.lineContainsReservedChar(curLine, "{") && depth > 0) {
				depth--;
			} else if (Helper.lineContainsReservedChar(curLine, "{")) {
				openingLine = curLineId;
			}
			curLineId--;
		}

		if (openingLine == -1) {
			System.err.println("Braces are not balanced");
			System.err.println("When trying to find start of block ending at line " + (startingLine+1));			
			System.exit(2);
		}
		
		return openingLine;
	}
	
	public static int findEndOfBlock(List<String> sourceCode, int startingLine) {
		int curLineId = startingLine;
		int closingLine = -1;
		int depth = 0;
		
		while (curLineId < sourceCode.size() && closingLine == -1) {
			String curLine = sourceCode.get(curLineId);
			if (Helper.lineContainsReservedChar(curLine, "{")) {
				depth++;
			} else if (Helper.lineContainsReservedChar(curLine, "}") && depth > 0) {
				depth--;
			} else if (Helper.lineContainsReservedChar(curLine, "}")) {
				closingLine = curLineId;
			}
			curLineId++;
		}

		if (closingLine == -1) {
			System.err.println("Braces are not balanced");
			System.err.println("When trying to find end of block starting at line " + (startingLine+1));
			System.err.println("Line content: " + sourceCode.get(startingLine));
			System.exit(2);
		}
		
		return closingLine;
	}
}
