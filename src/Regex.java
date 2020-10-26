
public class Regex {
	public static String classSignature = "(((|public|final|abstract|private|static|protected)(\\s+))?(class)(\\s+)(\\w+)(<.*>)?(\\s+extends\\s+\\w+)?(<.*>)?(\\s+implements\\s+)?(.*)?(<.*>)?(\\s*))\\{$";
	public static String methodSignature = "(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])";
	
	public static String reservedMethods = "(if|while|for|class|switch|try|catch|finally|return)";
	public static String reservedInstructions = "^\\b(do|else(?!\\s+if)|case|default|continue|break|switch)\\b.*";
	public static String nonExecutableLines = "^\\b(do|else(?!\\s+if)|default)\\b.*";
	
	// TODO do not capture escaped quotes
	public static String insideQuoteRestriction = "(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$).*";
}
