
public class Regex {
	
	// TODO [methodSignature] not all methods contain a scope, default is protected
	
	public static String classSignature = "^[ \\t]*((public|private|protected)\\s+)?(static\\s+)?(final\\s+)?class\\s.*";
	public static String methodSignature = ".*[a-zA-Z][a-zA-Z0-9]*\\s*\\(.*\\)\\s*\\{$";
	public static String reservedMethods = "(if|while|for|class|switch)";
}
