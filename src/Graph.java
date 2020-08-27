import java.util.*;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class Graph {
	//private List<Node> nodes;		// Node list
	//private List<Edge> edges;		// Edge list
	private List<String> sourceCode;
	private List<MethodGraph> methodGraphs;
	private List<Integer> emptyLines;
	
	private List<Map<Integer, List<Integer>>> lineMappings = new ArrayList<Map<Integer, List<Integer>>>();
	
	private boolean printDebug;
	
	public Graph() {
		sourceCode = new ArrayList<String>();	
		methodGraphs = new ArrayList<MethodGraph>();
		emptyLines = new ArrayList<Integer>();
		printDebug = false;
	}
	
	public void setDebug(boolean d){ printDebug = d; }
	
	// Add a line of source code
	public void AddSrcLine(String line){
		sourceCode.add(line);
	}
		
	public void dump() {
		for(int n=0; n<lineMappings.size(); n++) {
			System.out.println("Mapping #" + n);
			for(int key : lineMappings.get(n).keySet()) {				
				System.out.println(" | " + key + " -> " + lineMappings.get(n).get(key));
			}
		}
		dumpsrc();
	}
	
	public void dumpsrc() {
		for(int n=0; n<sourceCode.size(); n++) {
			System.out.println(sourceCode.get(n));
		}
	}
	
	public void dumpblockids(List<Pair<Integer, Integer>> blockList) {
		for(Pair<Integer, Integer> blocklimits : blockList) {
			int start = blocklimits.getLeft();
			int end = blocklimits.getRight();
			List<Integer> startO = lineMappings.get(lineMappings.size()-1).get(start);
			List<Integer> endO = lineMappings.get(lineMappings.size()-1).get(end);
			for(int i=0; i<startO.size(); i++) {
				startO.set(i, startO.get(i)+1);
			}
			for(int i=0; i<endO.size(); i++) {
				endO.set(i, endO.get(i)+1);
			}
			System.out.println(startO + " " + endO);
		}
	}
	
	public void build() {
		cleanup();
		addDummyNodes();
		buildFullMap();
		buildReverseFullMap();
		
		List<Pair<Integer, Integer>> classesBlocks = getClassesBlocks();
		
		for (int i=0; i < classesBlocks.size(); i++) {
			Pair<Integer, Integer> classBlockLimits = classesBlocks.get(i);
			int start = classBlockLimits.getLeft();
			int end = classBlockLimits.getRight();
			String className = getClassNameFromLineId(start);
			
			List<Pair<Integer, Integer>> methodBlocks = getMethodBlocks(start, end);

			for (int j=0; j < methodBlocks.size(); j++) {
				// TODO parse method parameters
				int methodBlockStart = methodBlocks.get(j).getLeft();
				int methodBlockEnd = methodBlocks.get(j).getRight();		
				// TODO: is it right to ignore method signature and last bracket?
				String methodName = getMethodNameFromLineId(methodBlockStart);
				
				List<String> methodBody = getNextMethodBody(methodBlockStart+1, methodBlockEnd-1);
				MethodGraph methodGraph = new MethodGraph(methodName, className, methodBody, printDebug);
				
				methodGraph.computeNodes();
				methodGraph.fixLineNumbers(methodBlockStart+1, lineMappings);
				methodGraph.writePng();
				methodGraphs.add(methodGraph);	
			}
		}
	}
	
	public void PrintGraphStructures() {
		for(MethodGraph graph : methodGraphs) {
			String className = graph.GetClassName();
			String methodName = graph.GetMethodName();
			
			if (className.equals(methodName)) {
				System.out.println("Graph structure for class " + className
						+ " constructor:\n");
			} else {
				System.out.println("Graph structure for class " + className
						+ " method " + methodName + ":\n");
			}
			
			graph.PrintGraphStructure(lineMappings);
		}
	}
	
	public void PrintTestRequirements() {
		TestRequirements tr = new TestRequirements();
		
		for(MethodGraph graph : methodGraphs) {
			tr.ReadGraph(graph);
			
			String className = graph.GetClassName();
			String methodName = graph.GetMethodName();
			
			if (className.equals(methodName)) {
				System.out.println("Test Requirements for class " + className
						+ " constructor:\n");
			} else {
				System.out.println("Test Requirements for class " + className
						+ " method " + methodName + ":\n");
			}
		
			tr.PrintNodeCoverage();
			tr.PrintEdgeCoverage();
			tr.PrintEdgePairCoverage();
			tr.PrintPrimePathCoverage();
			
			System.out.println("\n");
		}
	}
	
	public void PrintLineFlows() {
		for(MethodGraph graph : methodGraphs) {
			graph.PrintLineFlow(lineMappings);
		}
	}
	
	/* TODO: Support class inside classes */
	private List<Pair<Integer, Integer>> getClassesBlocks() {
		List<Pair<Integer, Integer>> classesBlocks = new ArrayList<Pair<Integer, Integer>>();
		for (int i=0; i<sourceCode.size(); i++) {
			// FIXME
			if (sourceCode.get(i).matches("^[ \\t]*((public|private|protected)\\s+)?(static\\s+)?(final\\s+)?class\\s.*")) {
				int start = i;
				int end = findEndOfBlock(i+1); // TODO might be problem if { } is in same line
				classesBlocks.add(new ImmutablePair<Integer, Integer>(start, end));
			}
		}
		return classesBlocks;
	}
	
	/* TODO: Support methods/functions inside methods */
	/* TODO: maybe refactor with previous function */
	private List<Pair<Integer, Integer>> getMethodBlocks(int classStartLineId, int classEndLineId) {
		List<Pair<Integer, Integer>> methodBlocks = new ArrayList<Pair<Integer, Integer>>();
		for (int i=classStartLineId; i<classEndLineId; i++) {
			// TODO not all methods contain a scope, default is protected
			if (Helper.lineContainsReservedWord(sourceCode.get(i), "(private|protected|public)")
					&& sourceCode.get(i).matches(".*[a-zA-Z][a-zA-Z0-9]*[ \t]*\\(.*\\)[ \t]*\\{$")) {
				int start = i;
				int end = findEndOfBlock(i+1);
				methodBlocks.add(new ImmutablePair<Integer, Integer>(start, end));
			}
		}
		return methodBlocks;
	}

	private List<String> getNextMethodBody(int startLineId, int endLineId) {
		List<String> methodBody = new ArrayList<String>();
		for (int i=startLineId; i<=endLineId; i++) {
			methodBody.add(sourceCode.get(i));
		}
		return methodBody;
	}
	
	private String getClassNameFromLineId(int lineId) {
		String line = sourceCode.get(lineId);
		int start = line.indexOf("class") + 6;
		int idx = start;
		int end = -1;
		
		while (idx < line.length() && end == -1) {
			if(!Character.isDigit(line.charAt(idx)) && !Character.isLetter(line.charAt(idx))) 
				end = idx-1;
			idx++;
		}
		
		if (end == -1){
			System.err.println("Invalid class name");
			System.err.println("When trying to get class name at line " + lineId);
			System.exit(2);
		}
		
		return line.substring(start, end+1);
	}
	
	private String getMethodNameFromLineId(int lineId) {
		String line = sourceCode.get(lineId);
		int end = line.indexOf("(");
		int idx = end;
		int start = -1;
		boolean foundName = false;
		
		while (idx > 0 && start == -1) {
			if (!foundName && line.charAt(idx) != ' ') {
				foundName = true;
				end = idx;
			}
			if (foundName && line.charAt(idx) == ' ') 
				start = idx+1;
			idx--;
		}
		
		if (start == -1){
			System.err.println("Invalid method name");
			System.err.println("When trying to get method name at line " + lineId);
			System.exit(2);
		}
		
		return line.substring(start, end);
	}
	
	//trim all lines (remove indents and other leading/trailing whitespace)
	// #n -> #n
	private void trimLines() {
		for (int i=0; i<sourceCode.size(); i++){
			sourceCode.set(i, sourceCode.get(i).trim());
		}
	}
	
	// #n -> #n
	private void eliminateComments() {		
		for (int i=0; i<sourceCode.size(); i++) {
			int idxSingle = sourceCode.get(i).indexOf("//"); 
			int idxMulti = sourceCode.get(i).indexOf("/*");
			int idx = (idxSingle >= 0 && idxMulti >= 0) ? 
					Math.min(idxSingle, idxMulti) : Math.max(idxSingle, idxMulti);
			
			if (idx == -1) {
				continue;
			} else if (idx == idxSingle) {
				sourceCode.set(i, sourceCode.get(i).substring(0, idx)); 
			} else {
				i = eraseMultiLineComment(i, idx);
			}
		}
	}
		
	private int eraseMultiLineComment(int startLine, int idxStart) {
		String preceding = sourceCode.get(startLine).substring(0, idxStart);
		
		boolean closingBlockFound = false;
		int i = startLine, idxClosing = 0;
		
		while(!closingBlockFound) {
			idxClosing = sourceCode.get(i).indexOf("*/");
			if (idxClosing != -1) {
				closingBlockFound = true;
			} else if (i != startLine) {
				sourceCode.set(i, "");
			}
			i++;
		}
		int endLine = i-1;
		String trailing = sourceCode.get(endLine).substring(idxClosing+2);
		
		if (endLine == startLine) {
			sourceCode.set(startLine, preceding + trailing);
		} else {
			sourceCode.set(startLine, preceding);
			sourceCode.set(endLine, trailing);
		}
		
		return endLine;
	}
	
	private void removeBlankLines() {		
		int blankLines = 0;
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		
		for(int i = 0; i < sourceCode.size(); i++) {
			if (sourceCode.get(i).equals("")) {
				blankLines++;
				emptyLines.add(i);
			}
			if (sourceCode.get(i).equals("{")) {
				emptyLines.add(i);
			}
			// if first line of file is blank, point to 0.
			int targetLineId = Math.max(i-blankLines, 0);
			mapping.put(i, initArray(targetLineId));
		}
		
		lineMappings.add(mapping);
		sourceCode.removeAll(Collections.singleton(""));
	}
	
	//move opening braces on their own line to the previous line
	// Note: curly bracket must be alone
	private void moveOpeningBraces() {
		int numRemovedLines = 0;
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();

		for (int i=0; i<sourceCode.size(); i++){
			int oldLineId = i+numRemovedLines;
			
			if (sourceCode.get(i).equals("{")){
				sourceCode.set(i-1, sourceCode.get(i-1) + "{");
				
				mapping.put(oldLineId, initArray(i-1)); 
				numRemovedLines++;
				
				sourceCode.remove(i);
				i--;
			} else {
				mapping.put(oldLineId, initArray(i));
			}
		}

		lineMappings.add(mapping);
	}

	//move any code after an opening brace to the next line
	private void moveCodeAfterOpenedBrace() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<sourceCode.size(); i++) {
			int oldLineId = i-numAddedLines;

			// add current line to targets
			List<Integer> targetLinesIds = (mapping.containsKey(oldLineId) ? 
					mapping.get(oldLineId) : new ArrayList<Integer>());
			targetLinesIds.add(i);
			
			// find brace and check if there is code after
			int idx = Helper.getIndexOfReservedChar(sourceCode.get(i), "{");
			boolean hasCodeAfterBrace = (idx > -1 
					&& idx < sourceCode.get(i).length()-1);
			
			if (hasCodeAfterBrace){ 
				String preceding = sourceCode.get(i).substring(0, idx+1);
				String trailing = sourceCode.get(i).substring(idx+1);
				sourceCode.add(i+1, trailing); //insert the text right of the { as the next line
				sourceCode.set(i, preceding); //remove the text right of the { on the current line
				
				mapping.put(oldLineId, targetLinesIds);
				numAddedLines++;
			} else {
				mapping.put(oldLineId, targetLinesIds);
			}
		}
		
		lineMappings.add(mapping);
	}
	
	//move closing braces NOT starting a line to the next line
	private void moveClosingBraces() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<sourceCode.size(); i++){
			int oldLineId = i-numAddedLines;

			// add current line to targets
			List<Integer> targetLinesIds = (mapping.containsKey(oldLineId) ? 
					mapping.get(oldLineId) : new ArrayList<Integer>());
			targetLinesIds.add(i);
			
			int idx = Helper.getIndexOfReservedChar(sourceCode.get(i), "}"); 
			if (idx > 1) { //this means the } is not starting a line
				String trailing = sourceCode.get(i).substring(idx);
				String preceding = sourceCode.get(i).substring(0, idx);
				sourceCode.add(i+1, trailing); //insert the text starting with the } as the next line
				sourceCode.set(i, preceding); //remove the text starting with the } on the current line
				
				mapping.put(oldLineId, targetLinesIds);
				numAddedLines++;
			} else {
				mapping.put(oldLineId, targetLinesIds);
			}
		}

		lineMappings.add(mapping);
	}
	
	//move any code after a closing brace to the next line
	private void moveCodeAfterClosedBrace() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<sourceCode.size(); i++){
			int oldLineId = i-numAddedLines;

			// add current line to targets
			List<Integer> targetLinesIds = (mapping.containsKey(oldLineId) ? 
					mapping.get(oldLineId) : new ArrayList<Integer>());
			targetLinesIds.add(i);
			
			int idx = Helper.getIndexOfReservedChar(sourceCode.get(i), "}"); 
			if (idx > -1 && sourceCode.get(i).length() > 1) { //this means there is text after the {
				String trailing = sourceCode.get(i).substring(idx+1);
				String preceding = sourceCode.get(i).substring(0, idx+1);

				sourceCode.add(i+1, trailing); //insert the text right of the { as the next line
				sourceCode.set(i, preceding); //remove the text right of the { on the current line
				
				mapping.put(oldLineId, targetLinesIds);
				numAddedLines++;
			} else {
				mapping.put(oldLineId, targetLinesIds);
			}
		}
		
		lineMappings.add(mapping);
	}

	//Separate sourceCode with containing semicolons except at the end
	private void separateLinesWithSemicolons() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i < sourceCode.size(); i++){
			int oldLineId = i-numAddedLines;
			List<Integer> targetLinesIds = new ArrayList<Integer>();
			
			List<String> statements = initArray(sourceCode.get(i).split(";"));
			
			targetLinesIds.add(i);
			if (statements.size() > 1) {
				boolean lineEndsWithSemicolon = sourceCode.get(i).matches("^.*;$");
				sourceCode.set(i, statements.get(0) + ";");
				
				for (int j=1; j < statements.size(); j++){
					String pause = (j == statements.size()-1 && !lineEndsWithSemicolon ? "" : ";");
					sourceCode.add(i+j, statements.get(j) + pause);
					targetLinesIds.add(i+j);
				}
				
				mapping.put(oldLineId, targetLinesIds);
				numAddedLines += statements.size()-1;
				i += statements.size()-1;	// can skip what we altered already
			} else {
				mapping.put(oldLineId, targetLinesIds);
			}			
		}
		
		lineMappings.add(mapping);
	}
	
	private void combineMultiLineStatements() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int removedLines = 0;

		for (int i=0; i < sourceCode.size(); i++) {
			mapping.put(i+removedLines, initArray(i));
			String curLine = sourceCode.get(i);
			
			while (!Helper.lineContainsReservedChar(curLine, ";")
					&& !Helper.lineContainsReservedChar(curLine, "{") 
					&& !Helper.lineContainsReservedChar(curLine, "}")
					&& !((Helper.lineContainsReservedWord(curLine, "case") || Helper.lineContainsReservedWord(curLine, "default"))
							&& Helper.lineContainsReservedChar(curLine, ":"))
					){
				String separator = (curLine.charAt(curLine.length()-1) != ' '
									&& sourceCode.get(i+1).charAt(0) != ' ' ? " " : "");
				sourceCode.set(i, curLine + separator + sourceCode.get(i+1));
				sourceCode.remove(i+1);

				removedLines++;
				mapping.put(i+removedLines, initArray(i));
				curLine = sourceCode.get(i);
			}
		}
		
		lineMappings.add(mapping);
	}

	//separate case statements with next line
	private void separateCaseStatements() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<sourceCode.size(); i++){
			int oldLineId = i-numAddedLines;

			// add current line to targets
			List<Integer> targetLinesIds = (mapping.containsKey(oldLineId) ? 
					mapping.get(oldLineId) : new ArrayList<Integer>());
			targetLinesIds.add(i);
			mapping.put(oldLineId, targetLinesIds);
			
			if (sourceCode.get(i).matches("^\\b(case|default)\\b.*:.*")){
				int idx = Helper.getIndexOfReservedChar(sourceCode.get(i), ":"); // TODO test if it works in all situations
				
				if (sourceCode.get(i).substring(idx+1).matches("[ \t]*\\{[ \t]*")) {
					continue;
				}
				
				if (idx < sourceCode.get(i).length()-1){
					sourceCode.add(i+1, sourceCode.get(i).substring(idx+1));
					sourceCode.set(i, sourceCode.get(i).substring(0, idx+1));
					numAddedLines++;
				}
			}
		}
		lineMappings.add(mapping);
	}
	
	//turn for loops into while loops
	private void convertForToWhile() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		List<Integer> loopsClosingLines = new ArrayList<Integer>();
		
		for (int i=0; i<sourceCode.size(); i++){			
			if (sourceCode.get(i).matches("^for.+$")){
				int depth = loopsClosingLines.size();
				int closingLine = findEndOfBlock(i+3);
				
				//move the initialization before the loop
				mapping.put(i+depth, initArray(i));
				int idx = sourceCode.get(i).indexOf("(");
				sourceCode.add(i, "%forcenode%" + sourceCode.get(i).substring(idx+1));
				i++; //adjust for insertion
				
				//move the iterator to just before the close
				mapping.put(i+1+depth, initArray(closingLine-1));
				idx = sourceCode.get(i+2).lastIndexOf(")");
				sourceCode.add(closingLine+1, "%forcenode%" + sourceCode.get(i+2).substring(0, idx) + ";");
				sourceCode.remove(i+2); //remove old line
				
				//replace for initialization with while
				mapping.put(i+depth, initArray(i));
				String testStatement = sourceCode.get(i+1).substring(0, sourceCode.get(i+1).length()-1).trim();
				sourceCode.set(i, "while (" + testStatement + "){");
				sourceCode.remove(i+1); //remove old (test) line

				loopsClosingLines.add(closingLine);
			} else {
				int depth = loopsClosingLines.size();
				if (depth > 0 && i == loopsClosingLines.get(depth-1) - 1) {
					loopsClosingLines.remove(loopsClosingLines.size()-1);
				} else {
					mapping.put(i+depth, initArray(i));
				}
			}
		}
		
		lineMappings.add(mapping);
	}
	
	private void convertForEachToFor() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int addedLines = 0;
		
		for (int i=0; i<sourceCode.size(); i++){			
			if (sourceCode.get(i).matches("^for.+$")
					&& Helper.lineContainsReservedChar(sourceCode.get(i), ":")) {
				List<String> forEachInformation = extractForEachInfo(sourceCode.get(i));
				String type = forEachInformation.get(0);
				String varName = forEachInformation.get(1);
				String setName = forEachInformation.get(2);
				
				mapping.put(i - addedLines, new ArrayList<>(Arrays.asList(i, i+1)));
				sourceCode.set(i, "for (Iterator<" + type + "> it = " + setName + ".iterator(); it.hasNext(); ){");
				sourceCode.add(i+1, type + " " + varName + " = it.next();");
				addedLines++;
				i++;
			} else {
				mapping.put(i - addedLines, initArray(i));
			}
		}
		
		lineMappings.add(mapping);
	}
	
	// given a line containing a for each statement, collect the necessary info
	private List<String> extractForEachInfo(String line) {
		String buffer = "";
		List<String> info = new ArrayList<>();
		
		int i;
		for(i = 3; i < line.length() && info.size() < 2; i++) {
			if (line.charAt(i) != '(' && line.charAt(i) != ' ' && line.charAt(i) != '\t') {
				buffer += line.charAt(i);
			} else if ((line.charAt(i) == ' ' || line.charAt(i) == '\t') && buffer.length() > 0) {
				info.add(buffer);
				buffer = "";
			}
		}
		
		while (line.charAt(i) == ':' || line.charAt(i) == ' ' || line.charAt(i) == '\t') i++;
		int start = i;
		int end = line.length() - 1;
		while (line.charAt(end) == '{' || line.charAt(end) == ' ' || line.charAt(end) == '\t') end--;
		info.add(line.substring(start, end));
		
		return info;
	}
	
	//given line id and depth, find its last id in final source code
	private List<Integer> getFinalTargetLineId(int id, int depth) {
		Map<Integer, List<Integer>> currentMap = lineMappings.get(depth);
		List<Integer> endLines = new ArrayList<Integer>(); 
		
		if (depth == lineMappings.size() - 1) {
			endLines.addAll(currentMap.get(id));
		} else {
			List<Integer> directTargets = currentMap.get(id);
			for (Integer target : directTargets) {
				endLines.addAll(getFinalTargetLineId(target, depth + 1));
			}
		}
		
		return endLines;
	}
	
	//build mapping original -> final state
	private void buildFullMap() {
		Map<Integer, List<Integer>> firstMap = lineMappings.get(0);
		Map<Integer, List<Integer>> lastMap = new HashMap<Integer, List<Integer>>();
		
		for (Integer lineId : firstMap.keySet()) {
			List<Integer> targets = new ArrayList<Integer>();
			targets.addAll(getFinalTargetLineId(lineId, 0));
			lastMap.put(lineId, targets);
		}
		
		lineMappings.add(lastMap);
	}
	
	//build mapping final -> original state
	private void buildReverseFullMap() {
		Map<Integer, List<Integer>> lastMap = lineMappings.get(lineMappings.size()-1);
		Map<Integer, List<Integer>> reverseMap = new HashMap<Integer, List<Integer>>();
		
		for(Integer key : lastMap.keySet()) {
			for (Integer tgt : lastMap.get(key)) {
				if (emptyLines.contains(key)) {
					continue;
				} else if (reverseMap.get(tgt) != null) {
					reverseMap.get(tgt).add(key);
				} else {
					reverseMap.put(tgt, initArray(key));
				}
			}
		}
		
		lineMappings.add(reverseMap);
	}
	
	//CURRENT FORMAT CONSTRAINTS:
	//  Must use surrounding braces for all loops and conditionals
	//  do,for,while loop supported / do-while loops not supported
	private int cleanup() {
		eliminateComments();
		trimLines();
		removeBlankLines();
		
		// TODO trim lines each step? There are some spaces causing extra lines.
		//  actually, in the functions, do not split if its only spaces and tabs
		moveOpeningBraces();
		moveCodeAfterOpenedBrace(); // TODO perform before openingBraces?
		moveClosingBraces();
		moveCodeAfterClosedBrace();
		trimLines();
		// At this point, all opening braces end a line and all closing braces are on their own line;

		convertForEachToFor();
		trimLines();
		separateLinesWithSemicolons();
		trimLines();
		combineMultiLineStatements();
		trimLines();
		convertForToWhile();
		trimLines();
		separateCaseStatements();
		trimLines();
		
		return Defs.success;
	}
	
	/* TODO: maybe refactor the three next functions */
	private int findStartOfBlock(int startingLine) {
		return findStartOfBlock(startingLine, false);
	}

	/* TODO: Maybe reenginer this */
	private int findStartOfBlock(int startingLine, boolean useBlockLines) {
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

		if (openingLine == -1){
			System.err.println("Braces are not balanced");
			System.err.println("When trying to find start of block ending at line " + (startingLine+1));			
			System.exit(2);
		}
		
		return openingLine;
	}
	
	private int findEndOfBlock(int startingLine) {
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

		if (closingLine == -1){
			System.err.println("Braces are not balanced");
			System.err.println("When trying to find end of block starting at line " + (startingLine+1));
			System.err.println("Line content: " + sourceCode.get(startingLine));
			System.exit(2);
		}
		
		return closingLine;
	}
	
	private void addDummyNodes(){
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<sourceCode.size(); i++){
			String line = sourceCode.get(i);
			
			if (Helper.lineContainsReservedChar(line, "}")){
				//find the opening
				int openline = findStartOfBlock(i-1);

				if (sourceCode.get(openline).toLowerCase().matches("^\\b(while|do|if|else)\\b.*")
						&& sourceCode.get(i-1).equals("}")) {
					sourceCode.add(i, "dummy_node;");
					numAddedLines++;
					// i--; //adjust i due to insertion // I don't think this is needed.
				}
			}
			
			int oldLineId = i-numAddedLines;
			mapping.put(oldLineId, initArray(i));
		}
		
		lineMappings.add(mapping);
	}
	
	/*
	private String removeTags(String line){
		line = line.replace("%forcenode%", "");
		line = line.replace("%forcelabel%", "");
		
		return line;
	}
	*/
	
	private <T> ArrayList<T> initArray(T firstElement) {
		return new ArrayList<T>(Arrays.asList(firstElement));
	}
	
	private <T> ArrayList<T> initArray(T[] elements) {
		return new ArrayList<T>(Arrays.asList(elements));
	}
		
}
