import java.io.File;
import java.util.*;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class Graph {
	private List<Node> nodes;		// Node list
	private List<Edge> edges;		// Edge list
	private List<String> sourceCode;
	private List<String> blockLines;
	private List<Map<Integer, List<Integer>>> lineMappings = new ArrayList<Map<Integer, List<Integer>>>();
	
	private boolean printDebug;
	
	public Graph() {
		
		sourceCode = new ArrayList<String>();
		blockLines = new ArrayList<String>();
		nodes = new ArrayList<Node>();
		edges = new ArrayList<Edge>();
	
		printDebug = false;
	}
	
	public void setDebug(boolean d){ printDebug = d; }
	
	// Add a line of source code
	public void AddSrcLine(String line){
		sourceCode.add(line);
	}
	
	// Add a node to the graph
	public void AddNode(Node _node) {
		nodes.add(_node);
	}
	
	// Add an edge to the graph
	public void AddEdge(Edge _edge) {
		edges.add(_edge);
	}
	
	// Get the first entry node
	public Node GetEntryNode() {
		Iterator<Node> iterator = nodes.iterator();
		Node node;
		while(iterator.hasNext()) {
			node = iterator.next();
			//System.out.println("GetEntryNode = " + node);
			if(node.isEntry() == true) {
				return node;
			}
		}
		return null;
	}
	
	// Get all the entry node list
	public List<Node> GetEntryNodeList() {
		List<Node> node_list = new LinkedList<Node>();
		Iterator<Node> iterator = nodes.iterator();
		Node node;
		while(iterator.hasNext()) {
			node = iterator.next();
			//System.out.println("GetEntryNode = " + node);
			if(node.isEntry() == true) {
				node_list.add(node);
			}
		}
		return node_list;
	}
	
	// Get the first exit node
	public Node GetExitNode() {
		Iterator<Node> iterator = nodes.iterator();
		Node node;
		while(iterator.hasNext()) {
			node = iterator.next();
			//System.out.println(node);
			if(node.isExit() == true) {
				return node;
			}
		}
		return null;
	}
	
	// Get all the exit node list
	public List<Node> GetExitNodeList() {
		List<Node> node_list = new LinkedList<Node>();
		Iterator<Node> iterator = nodes.iterator();
		Node node;
		while(iterator.hasNext()) {
			node = iterator.next();
			//System.out.println(node);
			if(node.isExit() == true) {
				node_list.add(node);
			}
		}
		return node_list;
	}
	
	// Get edge list that start from Node "_node"
	public List<Edge> GetEdgeStartFrom(Node _node) {
		List<Edge> chosenEdges = new LinkedList<Edge>();
		Iterator<Edge> iterator = edges.iterator();
		Edge edge;
		while(iterator.hasNext()) {
			edge = iterator.next();
			if(edge.GetStart() == _node.GetNodeNumber()) {
				chosenEdges.add(edge);
				//System.out.println("Node num = " + _node.GetNodeNumber() + ", Edge = " + edge);
			}
		}
		return chosenEdges;
	}
	
	// Get the node with a specified node number
	public Node GetNode(int _node_num) {
		Iterator<Node> iterator = nodes.iterator();
		Node node;
		while(iterator.hasNext()) {
			node = iterator.next();
			//System.out.println(node);
			if(node.GetNodeNumber() == _node_num) {
				return node;
			}
		}
		return null;
	}
	
	public void PrintNodes() {
		Iterator<Node> iterator = nodes.iterator();
		Node node;
		while(iterator.hasNext()) {
			node = iterator.next();
			System.out.println(node);
		}
	}
	
	public void PrintEdges() {
		Iterator<Edge> iterator = edges.iterator();
		Edge edge;
		while(iterator.hasNext()) {
			edge = iterator.next();
			System.out.println(edge);
		}
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
	
	public void build() {
		cleanup();
		addDummyNodes();
		buildFullMap();
		buildReverseFullMap();

		List<Pair<Integer, Integer>> classesBlocks = getClassesBlocks();
		//System.out.println(classesBlocks);
		
		for (int i=0; i < classesBlocks.size(); i++) {
			Pair<Integer, Integer> classBlockLimits = classesBlocks.get(i);
			int start = classBlockLimits.getLeft();
			int end = classBlockLimits.getRight();
			
			List<Pair<Integer, Integer>> methodBlocks = getMethodBlocks(start, end);

			for (int j=0; j < methodBlocks.size(); j++) {
				Pair<Integer, Integer> methodBlockLimits = methodBlocks.get(j);			
				// TODO: is it right to ignore method signature and last bracket?
				copyNextMethod(methodBlockLimits.getLeft()+1, methodBlockLimits.getRight()-1);
				getNodes();
				numberNodes();
				// TODO fix node numbering in next runs.
				combineNodes();
				fixNumbering();
				
				for(Node node : nodes) {
					node.SetSrcLineIdx(node.GetSrcLineIdx() + methodBlockLimits.getLeft() + 1);
				}
			}
			
			String className = getClassNameFromLineId(start);
			PrintNodeLineSrcs();

			writePng(className + ".png");
			nodes.clear();
			edges.clear();
		}
	}
	
	public void writePng(String path){
		
		String strDOT = generateDOT();
		
		if (printDebug) System.out.println("\n***** Generated DOT Code:\n\n"+strDOT+"\n\n");
				
		File out = new File(path);
		
		GraphViz gv = new GraphViz();
		gv.writeGraphToFile(gv.getGraph(strDOT, "png"), out);
		
	}
	
	public void PrintNodeLineSrcs() {
		Iterator<Node> iterator = nodes.iterator();
		Node node;
		while(iterator.hasNext()) {
			node = iterator.next();
			System.out.println("Printing source code present in node #" + node.GetNodeNumber() + "...");
			String unifiedLines = node.GetSrcLine();
			String[] lines = unifiedLines.split("\n");
			for(int i=0; i < lines.length ; i++) {
				String line = lines[i];
				line = line.replace("%forcenode%", "[FOR component] ");
				Integer curLineId = node.GetSrcLineIdx() + i;
				List<Integer> originalLines = lineMappings.get(lineMappings.size()-1).get(curLineId);
				for(int j=0; j<originalLines.size(); j++) {
					originalLines.set(j, originalLines.get(j)+1);
				}
				System.out.println(" - Code `" + line + "` originally present at line(s) " + 
						originalLines);
			}
			System.out.println("End of source code for node " + node.GetNodeNumber() + "\n");
		}
		System.out.println("=========\n");
	}
	
	/* TODO: Use thing method in all functions that look for specific characters or words */
	private boolean lineContainsReservedWord(String line, String word) {
		return line.matches(".*\\b"+word+"\\b(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$).*");
	}
	
	/* TODO: Support class inside classes */
	private List<Pair<Integer, Integer>> getClassesBlocks() {
		List<Pair<Integer, Integer>> classesBlocks = new ArrayList<Pair<Integer, Integer>>();
		for (int i=0; i<sourceCode.size(); i++) {
			if (lineContainsReservedWord(sourceCode.get(i), "class")) {
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
			if (lineContainsReservedWord(sourceCode.get(i), "(private|protected|public)")
					&& sourceCode.get(i).matches(".*\\(.*\\)[ \t]*\\{$")) {
				int start = i;
				int end = findEndOfBlock(i+1);
				methodBlocks.add(new ImmutablePair<Integer, Integer>(start, end));
			}
		}
		return methodBlocks;
	}
	
	
	private void copyNextMethod(int startLineId, int endLineId) {
		blockLines.clear();
		for (int i=startLineId; i<=endLineId; i++) {
			blockLines.add(sourceCode.get(i));
		}
	}
	
	private String getClassNameFromLineId(int lineId) {
		String line = sourceCode.get(lineId);
		int start = line.indexOf("class") + 6;
		int idx = start;
		int end = -1;
		
		while (idx < line.length() && end == -1) {
			if(line.charAt(idx) == '{') 
				end = idx;
			idx++;
		}
		
		if (end == -1){
			System.err.println("Invalid class name");
			System.err.println("When trying to get class name at line " + lineId);
			System.exit(2);
		}
		
		return line.substring(start, end-1);
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
			int idx = sourceCode.get(i).indexOf("{");
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
			
			int idx = sourceCode.get(i).indexOf("}"); 
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
			
			int idx = sourceCode.get(i).indexOf("}"); 
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
			
			while (!curLine.contains(";") 
					&& !curLine.contains("{") 
					&& !curLine.contains("}")
					&& !((curLine.contains("case") || curLine.contains("default"))
							&& curLine.contains(":"))
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
			
			if (sourceCode.get(i).matches("[case|default].*:.*")){
				int idx = sourceCode.get(i).indexOf(":");
				
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
				idx = sourceCode.get(i+2).indexOf(")");
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
				if (reverseMap.get(tgt) != null) {
					reverseMap.get(tgt).add(key);
				} else {
					reverseMap.put(tgt, new ArrayList<Integer>(Arrays.asList(key)));
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
		moveCodeAfterOpenedBrace(); // todo perform before openingBraces?
		moveClosingBraces();
		moveCodeAfterClosedBrace();
		trimLines();
		// At this point, all opening braces end a line and all closing braces are on their own line;

		separateLinesWithSemicolons();
		trimLines();
		combineMultiLineStatements();
		trimLines();
		// convertForEachToFor();
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
			String curLine = (useBlockLines ? blockLines.get(curLineId) : sourceCode.get(curLineId));
			if (curLine.contains("}")) {
				depth++;
			} else if (curLine.contains("{") && depth > 0) {
				depth--;
			} else if (curLine.contains("{")) {
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
			if (curLine.contains("{")) {
				depth++;
			} else if (curLine.contains("}") && depth > 0) {
				depth--;
			} else if (curLine.contains("}")) {
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
			
			if (line.matches("}")){
				//find the opening
				int openline = findStartOfBlock(i-1);
				
				if (sourceCode.get(openline).toLowerCase().matches("^(while|do).*")
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
	
	private void getNodes(){
		if (printDebug){
			String outlines="\n***** Processed Source Code:\n\n";
			for (int i=0; i<blockLines.size(); i++) outlines += i+": "+blockLines.get(i)+"\n";
			System.out.printf("%s\n", outlines);
		}
		
		int conditionalStartLine=0;
		List<Integer> edgeStartLines = new ArrayList<Integer>();
		
		for (int i=0; i<blockLines.size(); i++){
			
			String line = blockLines.get(i);
			
			//if we find a close brace, need to figure out where to go from here
			if (line.matches("}")){
				
				//find the opening
				int openline = findStartOfBlock(i-1, true);
				
				//for loops, add an edge back to the start
				if (blockLines.get(openline).toLowerCase().matches("^(for|while|do).*")){
					if (blockLines.get(openline).toLowerCase().matches("^(for|while).*")){
						addEdge(getPrevLine(i),openline);	
						addEdge(openline,getNextLine(i));
					}
					else{ //do
						addEdge(getPrevLine(openline), getNextLine(openline)); //entry edge that skips the "do" statement
						addEdge(getPrevLine(i),getNextLine(openline)); //looping edge
						addEdge(getPrevLine(i),getNextLine(i)); //loop exit edge
					}
				}
				
				//for conditionals, we won't add edges until after the block.  Then link all the close braces to the end of the block
				else if (blockLines.get(openline).toLowerCase().matches("^(if|else if).*")){
					if (blockLines.get(openline).toLowerCase().matches("^if.*")) conditionalStartLine = openline;
					addEdge(conditionalStartLine,openline+1);
					//if we're not done with the conditional block, save the start of this edge until we find the end of the block
					if (blockLines.size() > i+1 && blockLines.get(i+1).toLowerCase().matches("^else.*")){
						
						edgeStartLines.add(getPrevLine(i));						
					}
					else{
						for (Integer start: edgeStartLines){
							addEdge(start, i+1);
						}
						edgeStartLines.clear();
						
						addEdge(getPrevLine(i),getNextLine(i));
						addEdge(openline,getNextLine(i));
					}
				}
				else if (blockLines.get(openline).toLowerCase().substring(0,4).equals("else")){
					if (edgeStartLines.size() == 0){
						System.err.println("Else without If");
						System.exit(2);
					}
					edgeStartLines.add(i-1);
					addEdge(conditionalStartLine,openline+1);
					for (Integer start: edgeStartLines){
						addEdge(start, i+1);
					}
					edgeStartLines.clear();
				}
				else if (blockLines.get(openline).toLowerCase().matches("^switch.*")){

					//add edges to cases
					for (int k=openline; k<i; k++){ //iterate through the case statement
						if (blockLines.get(k).matches("^(case|default).*")){
							if (blockLines.get(k).matches(":$")) addEdge(openline,k);
							else addEdge(openline,getNextLine(k));  //didnt't split lines at : so could be the next line
						}
						if (blockLines.get(k).matches("^break;")) addEdge(k,getNextLine(i));
					}
				}
			}
			
			else{
				//TODO REMOVE AND CHECK IF IT CREATES SEPARATE NODES
				
				//we'll add a node and an edge unless these are not executable lines
				if (!blockLines.get(i).toLowerCase().matches("^(do|else|case|default).*")){
					addNode(line,i);
					if (i>0 && !blockLines.get(getPrevLine(i)).toLowerCase().matches("^(do|else|case|default).*") && !blockLines.get(i-1).equals("}")){
						addEdge(getPrevLine(i), i);
					}
					
				}
			}
						
		}
		
		// remove entry edges
		for (int i=0;i<edges.size();i++)
			if (edges.get(i).GetStart() < 0) edges.remove(i);

		// remove any duplicates.  this is very naughty but our list is relatively small
		for (int i=0; i<edges.size();i++){
			for (int j=i+1; j<edges.size(); j++){
				if (edges.get(j).GetStart() == edges.get(i).GetStart() && edges.get(j).GetEnd() == edges.get(i).GetEnd()) edges.remove(j);
			}
		}
		
		//fix any returns before the last line
		for (int i=0; i<nodes.size(); i++){
			
			if (nodes.get(i).GetSrcLine().contains("return")){
				
				//mark node as an exit node
				Node n = nodes.get(i);
				n.SetExit(true);
				nodes.set(i,n);
				
				//remove any lines coming from that node
				for (int j=0; j<edges.size(); j++){
					if (edges.get(j).GetStart() == n.GetSrcLineIdx()) edges.remove(j);
				}
				
			}
			
		}
		
		if (printDebug){
			System.out.print("\n***** Edges:\n   - numbers correspond to processed source code line numbers (above)\n   - basic block nodes not yet combined\n\n");
			for (Edge e: edges) System.out.println("("+e.GetStart()+","+e.GetEnd()+")");
		}
		
				
	}
	
	private void combineNodes(){
	

		//add entry edge temporarily to prevent combination of loop nodes
		addEdge(-1,0);
	
		//add dummy end nodes if needed
		for (Edge e: edges){
		boolean foundEnd=false;
			for (Node n: nodes){
				if (e.GetEnd() == n.GetNodeNumber()) foundEnd=true;				
			}
			if (!foundEnd){
				Node n = new Node();
				n.SetSrcLine("");
				n.SetNodeNumber(e.GetEnd());
				nodes.add(n);
			}
		}
		
		//figure out how many edges each node has (to and from the node)
		for (int i=0; i<nodes.size(); i++){
			for (Edge e: edges){
				if (e.GetStart() == nodes.get(i).GetNodeNumber()) nodes.get(i).IncEdgesFrom();
				if (e.GetEnd() == nodes.get(i).GetNodeNumber()) nodes.get(i).IncEdgesTo();
			}
		}
		
		// for any pair of consecutive nodes that have only 1 edge between, combine them
		
		for (int i=0; i<nodes.size(); i++){
			
			// if there's more than one edge (or no edges) leaving this node, we can't combine
			if (nodes.get(i).GetEdgesFrom() != 1 || nodes.get(i).GetSrcLine().contains("%forcenode%")) continue;
			
			// find the edge leaving this node
			int midEdge = 0;
			while (midEdge < edges.size() && edges.get(midEdge).GetStart() != nodes.get(i).GetNodeNumber()) midEdge++;
			int nextNode = 0;
			while (nodes.get(nextNode).GetNodeNumber() != edges.get(midEdge).GetEnd()) nextNode++;
			
			// if there's more than one edge entering the next node, we can't combine
			if (nodes.get(nextNode).GetEdgesTo() > 1 || nodes.get(nextNode).GetSrcLine().contains("%forcenode%")) continue;
			
			// if it's a self-loop we can't combine
                        if (nextNode == i) continue;	

			// If we got here we can combine the nodes
						
			//copy the sourceline (we'll delete nextNode)
			nodes.get(i).SetSrcLine(nodes.get(i).GetSrcLine()+"\n"+nodes.get(nextNode).GetSrcLine());
			nodes.get(i).GetSrcLinesIndex().addAll(nodes.get(nextNode).GetSrcLinesIndex());
			
			// get all the edges leaving the next node
			List<Integer> outEdges = new ArrayList<Integer>();
			for (int j=0; j<edges.size(); j++){
				if (edges.get(j).GetStart() == nodes.get(nextNode).GetNodeNumber()) outEdges.add(j);
			}
			nodes.get(i).ClearEdgesFrom();
			if (outEdges.size() > 0){ //if false, this is the last node
				// relink the outbound edges to start at the first node
				for (int idx: outEdges){
					edges.set(idx, new Edge(nodes.get(i).GetNodeNumber(), edges.get(idx).GetEnd()));
					nodes.get(i).IncEdgesFrom();
				}
			}
			
			// remove old middle edge and second node
			edges.remove(midEdge);
			nodes.remove(nextNode);
 
			//keep the current node as start until we can't combine any more
			i--;
			
		}

		//delete the temporary entry edge
		edges.remove(edges.size()-1);
		
	}

	private void numberNodes(){
		
		//save the oldedges and clear edges
		List<Edge> oldedges = new ArrayList<Edge>();
		for (Edge e: edges) oldedges.add(new Edge(e.GetStart(), e.GetEnd()));
		edges.clear();
		
		//number the nodes and add edges with new numbers
		
		//First assign node numbers
		for (int i=0; i<nodes.size(); i++){
			Node n = nodes.get(i);
			n.SetNodeNumber(i);
			nodes.set(i, n);
		}
		
		//add edges using node_numbers instead of source line index
		for (int i=0; i<oldedges.size(); i++){
			int newStart=0;
			int newEnd=0;
			
			while (newStart < nodes.size() && nodes.get(newStart).GetSrcLineIdx() != oldedges.get(i).GetStart()) newStart++;
			while (newEnd < nodes.size() && nodes.get(newEnd).GetSrcLineIdx() != oldedges.get(i).GetEnd()) newEnd++;
			
			addEdge(newStart,newEnd);
		}
		
	}

	private void fixNumbering(){
		
		//Renumber the nodes, and the edges accordingly
		for (int i=0; i<nodes.size(); i++){
			Node n = nodes.get(i);
			for (int j=0; j<edges.size(); j++){
				if (edges.get(j).GetStart() == n.GetNodeNumber()) edges.set(j, new Edge(i, edges.get(j).GetEnd()));
				if (edges.get(j).GetEnd() == n.GetNodeNumber()) edges.set(j, new Edge(edges.get(j).GetStart(), i));
			}
			n.SetNodeNumber(i);
			nodes.set(i, n);
		}
	
		nodes.get(0).SetEntry(true);
	
		// mark entry and exits
		for (int i=0;i<nodes.size(); i++){
			
			boolean exit = true;
			
			for (Edge e: edges){
				if (e.GetStart() == nodes.get(i).GetNodeNumber()) exit = false;
			}
			
			if (!nodes.get(i).isExit()) nodes.get(i).SetExit(exit); //make sure override stays for return nodes
			
		}
		
	}
	
	
	private String generateDOT(){
		
		String strDOT = "digraph cfg{\n";
		
		for (Node n: nodes){
			
			String line = "";
			
			//attributes
			if (n.isEntry()){
				line += "\tstart [style=invis];\n\tstart -> "+n.GetNodeNumber()+";\n"; // invisible entry node required to draw the entry arrow
				
			}
			if (n.isExit()){
				line += "\t"+n.GetNodeNumber()+" [penwidth=4];\n"; // make the exit node bold
			}
			
			if (n.GetSrcLine().contains("%forcelabel%")){
			//	line += "\t"+n.node_number+" [xlabel=\"" + removeTags(n.srcline).trim() + "\",labelloc=\"c\"]"; // label the node if forced
			}
			
			if (line.length() > 0) strDOT += line;
			
		}
		
		for (Edge e: edges){
			strDOT += "\t"+e.GetStart()+" -> "+e.GetEnd();
			
			// attributes
			
			strDOT += ";\n";
		}
		
		strDOT += "}";
		
		return strDOT;		
		
	}
	
	private void addNode(String line, int lineidx){
		
		Node node = new Node(0,line,false,false);
		node.SetSrcLineIdx(lineidx);
		nodes.add(node);
	
	}
	private void addEdge(int startidx, int endidx){
		
		edges.add(new Edge(startidx,endidx));
		
	}

	private int getPrevLine(int start){
		
		int prevEdge=start-1;
		
		while (prevEdge > -1 && blockLines.get(prevEdge).equals("}")) prevEdge--;
		
		return prevEdge;
		
	}
	
	private int getNextLine(int start){
		
		int nextEdge=start+1;
		
		while (nextEdge < blockLines.size() && blockLines.get(nextEdge).equals("}")) nextEdge++;
		
		return nextEdge;
	
	}
	
	private String removeTags(String line){
		
		line = line.replace("%forcenode%", "");
		line = line.replace("%forcelabel%", "");
		
		return line;
		
	}
	
	private <T> ArrayList<T> initArray(T firstElement) {
		return new ArrayList<T>(Arrays.asList(firstElement));
	}
	
	private <T> ArrayList<T> initArray(T[] elements) {
		return new ArrayList<T>(Arrays.asList(elements));
	}
		
}
