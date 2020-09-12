import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeCleaner {

	// Processed source code
	private List<String> processedCode;
	// (line mode only) Mappings containing information on processed code x original code
	private List<Map<Integer, List<Integer>>> lineMappings;
	// (line mode only) List containing lines that originally were empty or only contained comments
	private List<Integer> emptyLines;
	
	// TODO make line mode optional in order to avoid unnecessary tasks
	
	public CodeCleaner() {
		lineMappings = new ArrayList<Map<Integer, List<Integer>>>();
		emptyLines = new ArrayList<Integer>();
	}
	
	public void clear() {
		emptyLines.clear();
		lineMappings.clear();
	}
	
	public void cleanupCode(List<String> codeToCleanup) {
		processedCode = codeToCleanup;
		cleanup();
		addDummyNodes();
		buildFullMap();
		buildReverseFullMap();
	}
	
	public Map<Integer, List<Integer>> getCleanToOriginalCodeMapping() {
		// returns the last map in the list, which will be the mapping
		//  that contains the clean line id -> original line id map
		//  if the method is called after cleanupCode
		return lineMappings.get(lineMappings.size()-1);
	}
	
	// CURRENT FORMAT CONSTRAINTS:
	// TODO must use surrounding braces for all loops and conditionals
	// TODO continue not supported
	private int cleanup() {
		eliminateComments();
		trimLines();
		removeBlankLines();
		// convertTernaries();
		
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

	// Trim all lines (remove indents and other leading/trailing whitespace)
	private void trimLines() {
		for (int i=0; i<processedCode.size(); i++) {
			processedCode.set(i, processedCode.get(i).trim());
		}
	}
	
	private void eliminateComments() {		
		for (int i=0; i<processedCode.size(); i++) {
			int idxSingle = processedCode.get(i).indexOf("//"); 
			int idxMulti = processedCode.get(i).indexOf("/*");
			int idx = (idxSingle >= 0 && idxMulti >= 0) ? 
					Math.min(idxSingle, idxMulti) : Math.max(idxSingle, idxMulti);
			
			if (idx == -1) {
				continue;
			} else if (idx == idxSingle) {
				processedCode.set(i, processedCode.get(i).substring(0, idx)); 
			} else {
				i = eraseMultiLineComment(i, idx);
			}
		}
	}
		
	private int eraseMultiLineComment(int startLine, int idxStart) {
		String preceding = processedCode.get(startLine).substring(0, idxStart);
		
		boolean closingBlockFound = false;
		int i = startLine, idxClosing = 0;
		
		while(!closingBlockFound) {
			idxClosing = processedCode.get(i).indexOf("*/");
			if (idxClosing != -1) {
				closingBlockFound = true;
			} else if (i != startLine) {
				processedCode.set(i, "");
			}
			i++;
		}
		int endLine = i-1;
		String trailing = processedCode.get(endLine).substring(idxClosing+2);
		
		if (endLine == startLine) {
			processedCode.set(startLine, preceding + trailing);
		} else {
			processedCode.set(startLine, preceding);
			processedCode.set(endLine, trailing);
		}
		
		return endLine;
	}
	
	private void removeBlankLines() {		
		int blankLines = 0;
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		
		for(int i = 0; i < processedCode.size(); i++) {
			if (processedCode.get(i).equals("")) {
				blankLines++;
				emptyLines.add(i);
			}
			if (processedCode.get(i).equals("{")) {
				emptyLines.add(i);
			}
			// if first line of file is blank, point to 0.
			int targetLineId = Math.max(i-blankLines, 0);
			mapping.put(i, Helper.initArray(targetLineId));
		}
		
		lineMappings.add(mapping);
		processedCode.removeAll(Collections.singleton(""));
	}
	
	// convert ternary operations into ifs
	// ifs are inserted in the same lines, so no mapping needs to be done
	
	// TODO since this is a hard operation to make, I'm leaving it for later
	/*
	private void convertTernaries() {
		int a = 1 > 2 ? 3 : 4;
		for(int i = 0; i < sourceCode.size(); i++) {
			String curLine = sourceCode.get(i);
			if (Helper.lineContainsReservedWord(curLine, "(.*?[^\\{\\}]*:.*)"));
		}
	}
	*/
	
	// Move opening braces on their own line to the previous line
	// Note: curly bracket must be alone
	private void moveOpeningBraces() {
		int numRemovedLines = 0;
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();

		for (int i=0; i<processedCode.size(); i++) {
			int oldLineId = i+numRemovedLines;
			
			if (processedCode.get(i).equals("{")) {
				processedCode.set(i-1, processedCode.get(i-1) + "{");
				
				mapping.put(oldLineId, Helper.initArray(i-1)); 
				numRemovedLines++;
				
				processedCode.remove(i);
				i--;
			} else {
				mapping.put(oldLineId, Helper.initArray(i));
			}
		}

		lineMappings.add(mapping);
	}

	// Move any code after an opening brace to the next line
	private void moveCodeAfterOpenedBrace() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<processedCode.size(); i++) {
			int oldLineId = i-numAddedLines;

			// add current line to targets
			List<Integer> targetLinesIds = (mapping.containsKey(oldLineId) ? 
					mapping.get(oldLineId) : new ArrayList<Integer>());
			targetLinesIds.add(i);
			
			// find brace and check if there is code after
			int idx = Helper.getIndexOfReservedChar(processedCode.get(i), "{");
			boolean hasCodeAfterBrace = (idx > -1 
					&& idx < processedCode.get(i).length()-1);
			
			if (hasCodeAfterBrace) { 
				String preceding = processedCode.get(i).substring(0, idx+1);
				String trailing = processedCode.get(i).substring(idx+1);
				processedCode.add(i+1, trailing); //insert the text right of the { as the next line
				processedCode.set(i, preceding); //remove the text right of the { on the current line
				
				mapping.put(oldLineId, targetLinesIds);
				numAddedLines++;
			} else {
				mapping.put(oldLineId, targetLinesIds);
			}
		}
		
		lineMappings.add(mapping);
	}
	
	// Move closing braces NOT starting a line to the next line
	private void moveClosingBraces() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<processedCode.size(); i++) {
			int oldLineId = i-numAddedLines;

			// add current line to targets
			List<Integer> targetLinesIds = (mapping.containsKey(oldLineId) ? 
					mapping.get(oldLineId) : new ArrayList<Integer>());
			targetLinesIds.add(i);
			
			int idx = Helper.getIndexOfReservedChar(processedCode.get(i), "}"); 
			if (idx > 1) { // this means the } is not starting a line
				String trailing = processedCode.get(i).substring(idx);
				String preceding = processedCode.get(i).substring(0, idx);
				processedCode.add(i+1, trailing); // insert the text starting with the } as the next line
				processedCode.set(i, preceding); // remove the text starting with the } on the current line
				
				mapping.put(oldLineId, targetLinesIds);
				numAddedLines++;
			} else {
				mapping.put(oldLineId, targetLinesIds);
			}
		}

		lineMappings.add(mapping);
	}
	
	// Move any code after a closing brace to the next line
	private void moveCodeAfterClosedBrace() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<processedCode.size(); i++) {
			int oldLineId = i-numAddedLines;

			// add current line to targets
			List<Integer> targetLinesIds = (mapping.containsKey(oldLineId) ? 
					mapping.get(oldLineId) : new ArrayList<Integer>());
			targetLinesIds.add(i);
			
			int idx = Helper.getIndexOfReservedChar(processedCode.get(i), "}"); 
			if (idx > -1 && processedCode.get(i).length() > 1) { // this means there is text after the {
				String trailing = processedCode.get(i).substring(idx+1);
				String preceding = processedCode.get(i).substring(0, idx+1);

				processedCode.add(i+1, trailing); // insert the text right of the { as the next line
				processedCode.set(i, preceding); // remove the text right of the { on the current line
				
				mapping.put(oldLineId, targetLinesIds);
				numAddedLines++;
			} else {
				mapping.put(oldLineId, targetLinesIds);
			}
		}
		
		lineMappings.add(mapping);
	}

	// Separate sourceCode with containing semicolons except at the end
	private void separateLinesWithSemicolons() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i < processedCode.size(); i++) {
			int oldLineId = i-numAddedLines;
			List<Integer> targetLinesIds = new ArrayList<Integer>();
			
			List<String> statements = Helper.initArray(processedCode.get(i).split(";"));
			
			targetLinesIds.add(i);
			if (statements.size() > 1) {
				boolean lineEndsWithSemicolon = processedCode.get(i).matches("^.*;$");
				processedCode.set(i, statements.get(0) + ";");
				
				for (int j=1; j < statements.size(); j++) {
					String pause = (j == statements.size()-1 && !lineEndsWithSemicolon ? "" : ";");
					processedCode.add(i+j, statements.get(j) + pause);
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

		for (int i=0; i < processedCode.size(); i++) {
			mapping.put(i+removedLines, Helper.initArray(i));
			String curLine = processedCode.get(i);
			
			while (!Helper.lineContainsReservedChar(curLine, ";")
					&& !Helper.lineContainsReservedChar(curLine, "{") 
					&& !Helper.lineContainsReservedChar(curLine, "}")
					&& !((Helper.lineContainsReservedWord(curLine, "case") || Helper.lineContainsReservedWord(curLine, "default"))
							&& Helper.lineContainsReservedChar(curLine, ":"))
					) {
				String separator = (curLine.charAt(curLine.length()-1) != ' '
									&& processedCode.get(i+1).charAt(0) != ' ' ? " " : "");
				processedCode.set(i, curLine + separator + processedCode.get(i+1));
				processedCode.remove(i+1);

				removedLines++;
				mapping.put(i+removedLines, Helper.initArray(i));
				curLine = processedCode.get(i);
			}
		}
		
		lineMappings.add(mapping);
	}

	// Separate case statements with next line
	private void separateCaseStatements() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<processedCode.size(); i++) {
			int oldLineId = i-numAddedLines;

			// add current line to targets
			List<Integer> targetLinesIds = (mapping.containsKey(oldLineId) ? 
					mapping.get(oldLineId) : new ArrayList<Integer>());
			targetLinesIds.add(i);
			mapping.put(oldLineId, targetLinesIds);
			
			if (processedCode.get(i).matches("^\\b(case|default)\\b.*:.*")) {
				int idx = Helper.getIndexOfReservedChar(processedCode.get(i), ":"); // TODO test if it works in all situations
				
				if (processedCode.get(i).substring(idx+1).matches("[ \t]*\\{[ \t]*")) {
					continue;
				}
				
				if (idx < processedCode.get(i).length()-1) {
					processedCode.add(i+1, processedCode.get(i).substring(idx+1));
					processedCode.set(i, processedCode.get(i).substring(0, idx+1));
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
		
		for (int i=0; i<processedCode.size(); i++) {			
			if (processedCode.get(i).matches("^for.+$")) {
				int depth = loopsClosingLines.size();
				int closingLine = Helper.findEndOfBlock(processedCode, i+3);
				
				// Move the initialization before the loop
				mapping.put(i+depth, Helper.initArray(i));
				int idx = processedCode.get(i).indexOf("(");
				processedCode.add(i, "%forcenode%" + processedCode.get(i).substring(idx+1));
				i++; //adjust for insertion
				
				// Move the iterator to just before the close
				mapping.put(i+1+depth, Helper.initArray(closingLine-1));
				idx = processedCode.get(i+2).lastIndexOf(")");
				processedCode.add(closingLine+1, "%forcenode%" + processedCode.get(i+2).substring(0, idx) + ";");
				processedCode.remove(i+2); //remove old line
				
				// Replace for initialization with while
				mapping.put(i+depth, Helper.initArray(i));
				String testStatement = processedCode.get(i+1).substring(0, processedCode.get(i+1).length()-1).trim();
				processedCode.set(i, "while (" + testStatement + ") {");
				processedCode.remove(i+1); // Remove old (test) line

				loopsClosingLines.add(closingLine);
			} else {
				int depth = loopsClosingLines.size();
				if (depth > 0 && i == loopsClosingLines.get(depth-1) - 1) {
					loopsClosingLines.remove(loopsClosingLines.size()-1);
				} else {
					mapping.put(i+depth, Helper.initArray(i));
				}
			}
		}
		
		lineMappings.add(mapping);
	}
	
	private void convertForEachToFor() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int addedLines = 0;
		
		for (int i=0; i<processedCode.size(); i++) {			
			if (processedCode.get(i).matches("^for.+$")
					&& Helper.lineContainsReservedChar(processedCode.get(i), ":")) {
				List<String> forEachInformation = extractForEachInfo(processedCode.get(i));
				String type = forEachInformation.get(0);
				String varName = forEachInformation.get(1);
				String setName = forEachInformation.get(2);
				
				mapping.put(i - addedLines, new ArrayList<>(Arrays.asList(i, i+1)));
				processedCode.set(i, "for (Iterator<" + type + "> it = " + setName + ".iterator(); it.hasNext(); ) {");
				processedCode.add(i+1, type + " " + varName + " = it.next();");
				addedLines++;
				i++;
			} else {
				mapping.put(i - addedLines, Helper.initArray(i));
			}
		}
		
		lineMappings.add(mapping);
	}
	
	// Given a line containing a for each statement, collect the necessary info
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

	private void addDummyNodes() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		int numAddedLines = 0;
		
		for (int i=0; i<processedCode.size(); i++) {
			String line = processedCode.get(i);
			
			if (Helper.lineContainsReservedChar(line, "}")) {
				//find the opening
				int openline = Helper.findStartOfBlock(processedCode, i-1);

				if (processedCode.get(openline).toLowerCase().matches("^\\b(while|do|if|else)\\b.*")
						&& processedCode.get(i-1).equals("}")) {
					processedCode.add(i, "dummy_node;");
					numAddedLines++;
					// i--; //adjust i due to insertion // I don't think this is needed.
				}
			}
			
			int oldLineId = i-numAddedLines;
			mapping.put(oldLineId, Helper.initArray(i));
		}
		
		lineMappings.add(mapping);
	}
	
	// Build mapping original -> final state
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
	
	// Build mapping final -> original state
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
					reverseMap.put(tgt, Helper.initArray(key));
				}
			}
		}
		
		lineMappings.add(reverseMap);
	}
	
	// Given line id and depth, find its last id in final source code
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

	public void dumpInfo() {
		for(int n=0; n<lineMappings.size(); n++) {
			System.out.println("Mapping #" + n);
			for(int key : lineMappings.get(n).keySet()) {				
				System.out.println(" | " + key + " -> " + lineMappings.get(n).get(key));
			}
		}
		dumpCode();
	}
	
	public void dumpCode() {
		for(int n=0; n<processedCode.size(); n++) {
			System.out.println(processedCode.get(n));
		}
	}
	
	/*
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
	*/
}
