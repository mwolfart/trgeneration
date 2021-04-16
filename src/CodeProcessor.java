import java.util.*;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class CodeProcessor {
	
	// Processed source code file path
	private String filePath;
	// Processed source code file directory
	private String fileDir;
	// Processed source code
	private List<String> processedCode;
	// Code cleaner
	private CodeCleaner cleaner;
	// Graphs for each class and method of the code
	private List<Graph> graphs;
	private boolean processClean;
	private boolean outputPNG;
	// Debug flag
	private boolean debug;
	
	public CodeProcessor(String fPath, boolean processClean, boolean outputPNG) {
		filePath = fPath;
		int lastSlashId = filePath.lastIndexOf("\\");
		fileDir = (lastSlashId != -1 ? filePath.substring(0, lastSlashId+1) : "");

		debug = false;
		processedCode = new ArrayList<String>();
		cleaner = new CodeCleaner(debug, processClean);
		graphs = new ArrayList<Graph>();
		this.processClean = processClean;
		this.outputPNG = outputPNG;
	}
		
	public void setDebug(boolean d) { 
		debug = d; 
		cleaner.setDebug(d);
	}
	
	public void addSourceCodeLine(String line) {
		processedCode.add(line);
	}
	
	public void clear() {
		processedCode.clear();
		graphs.clear(); // TODO correct cleanup?
		cleaner.clear();
	}
	
	public void build() {
		cleaner.cleanupCode(processedCode);
		if (debug)
			cleaner.dumpCode();
		processClasses();
	}
	
	public void writeGraphStructures() {
		for(Graph graph : graphs) {
			String className = graph.getClassName();
			String methodSignature = graph.getMethodSignature();
			Map<Integer, List<Integer>> lineMap = cleaner.getCleanToOriginalCodeMapping();
			
			String output = "Test Requirements for class " + className
					+ " method " + methodSignature + ":\n\n";
			output += graph.listGraphStructure(lineMap);
			
			String filePath = fileDir + className + "/" + methodSignature + "/graphStructure.txt";
			Helper.writeFile(filePath, output);
		}
	}
	
	public void writePPCandECrequirements() {
		TestRequirements tr = new TestRequirements();
		if (debug) tr.allowDebug();
		
		for(Graph graph : graphs) {
			tr.ReadGraph(graph);
			tr.useLineMode();
			
			String className = graph.getClassName();
			String methodSignature = graph.getMethodSignature();

			tr.allowLineBreaksBetweenSets();
			
			String output = "";
			output += tr.PrintEdgeCoverage();
			String filePath = fileDir + className + "/" + methodSignature + "/EC.txt";
			Helper.writeFile(filePath, output);
			
			output = tr.PrintPrimePathCoverage();
			output += "\n";
			filePath = fileDir + className + "/" + methodSignature + "/PPC.txt";
			Helper.writeFile(filePath, output);
		}
	}
	
	public void writeTestRequirements() {
		TestRequirements tr = new TestRequirements();
		if (debug) tr.allowDebug();
		
		for (Graph graph : graphs) {
			tr.ReadGraph(graph);
			tr.useLineMode();

			String className = graph.getClassName();
			String methodSignature = graph.getMethodSignature();

			String output = "Test Requirements for class " + className
					+ " method " + methodSignature + ":\n\n";
			output += "TR for Node coverage: " + tr.PrintNodeCoverage();
			output += "TR for Edge coverage: " + tr.PrintEdgeCoverage();
			output += "TR for Edge-Pair coverage: " + tr.PrintEdgePairCoverage();
			output += "TR for Prime Path coverage: " + tr.PrintPrimePathCoverage();
			output += "\n";
			
			String filePath = fileDir + className + "/" + methodSignature + "/testRequirements.txt";
			Helper.writeFile(filePath, output);
		}
	}
	
	public void writeLineEdges() {
		for (Graph graph : graphs) {
			String className = graph.getClassName();
			String methodSignature = graph.getMethodSignature();
			Map<Integer, List<Integer>> lineMap = cleaner.getCleanToOriginalCodeMapping();
			
			String output = "Printing line edges for class " 
					+ className + " method " + methodSignature + "...\n";
			output += graph.listLineEdges(lineMap);
			
			String filePath = fileDir + className + "/" + methodSignature + "/lineEdges.txt";
			Helper.writeFile(filePath, output);
		}
	}
	
	private void processClasses() {
		if (debug) System.out.println("Processing classes in file " + filePath);

		List<Pair<Integer, Integer>> classesBlocks = getClassesBlocks();
		
		for (int i=0; i < classesBlocks.size(); i++) {
			Pair<Integer, Integer> classBlockLimits = classesBlocks.get(i);
			int start = classBlockLimits.getLeft();
			int end = classBlockLimits.getRight();
			String className = getClassNameFromLineId(start);
			Helper.createDir(fileDir + className);
			processMethods(start, end, className);
		}
	}
	
	private void processMethods(int startLine, int endLine, String className) {
		if (debug) System.out.println("Processing methods for class " + className);
		
		List<Pair<Integer, Integer>> methodBlocks = getMethodBlocks(startLine, endLine);

		for (int j=0; j < methodBlocks.size(); j++) {
			int methodStartLine = methodBlocks.get(j).getLeft();
			int methodEndLine = methodBlocks.get(j).getRight();
			
			// TODO: is it right to ignore method signature and last bracket?
			String methodName = getMethodNameFromLineId(methodStartLine);
			String methodParams = getMethodParamsFromLineId(methodStartLine);
			String methodSignature = methodName + methodParams;
			
			if (methodEndLine - methodStartLine < 3) {
				if (debug) {
					System.out.println("Method " + methodSignature + " is too short and will be ignored.");
				}
				continue;
			}
			
			if (debug) {
				System.out.println("Processing method " + methodSignature + "...");
			}
			
			Helper.createDir(fileDir + className + "/" + methodSignature);
			
			List<String> methodBody = copyCodeBlock(methodStartLine+1, methodEndLine-1);
			Graph methodGraph = new Graph(methodName, methodSignature, className, methodBody, debug);
			Map<Integer, List<Integer>> lineMap = processClean ? null : cleaner.getCleanToOriginalCodeMapping();
			
			methodGraph.buildNodes();
			if (outputPNG) {
				methodGraph.writePng();
			}
			methodGraph.simplifyDummyEdges();
			methodGraph.adjustLineNumbers(methodStartLine+1, lineMap);
			graphs.add(methodGraph);	
		}
	}
	
	/* TODO: Support class inside classes */
	private List<Pair<Integer, Integer>> getClassesBlocks() {
		List<Pair<Integer, Integer>> classesBlocks = new ArrayList<Pair<Integer, Integer>>();
		
		for (int i=0; i<processedCode.size(); i++) {
			if (processedCode.get(i).matches(Regex.classSignature)) {
				int start = i;
				int end = Helper.findEndOfBlock(processedCode, i+1);
				classesBlocks.add(new ImmutablePair<Integer, Integer>(start, end));
			}
		}
		
		if (debug) {
			for (Pair<Integer, Integer> p : classesBlocks) {
				System.out.println("Found class body between lines " + p.getLeft() + " and " + p.getRight());
			}
		}
		
		return classesBlocks;
	}
	
	/* TODO: Support methods/functions inside methods */
	private List<Pair<Integer, Integer>> getMethodBlocks(int classStartLineId, int classEndLineId) {
		List<Pair<Integer, Integer>> methodBlocks = new ArrayList<Pair<Integer, Integer>>();

		for (int i=classStartLineId; i<classEndLineId; i++) {
			if (processedCode.get(i).matches(Regex.methodSignature) 
					&& !Helper.lineContainsReservedWord(processedCode.get(i), Regex.reservedMethods)) {
				int start = i;
				int end = Helper.findEndOfBlock(processedCode, i+1);
				methodBlocks.add(new ImmutablePair<Integer, Integer>(start, end));
			}
		}
		
		if (debug) {
			for (Pair<Integer, Integer> p : methodBlocks) {
				System.out.println("Found method body between lines " + p.getLeft() + " and " + p.getRight());
			}
		}
		
		return methodBlocks;
	}

	private List<String> copyCodeBlock(int startLineId, int endLineId) {
		List<String> codeBlock = new ArrayList<String>();
		for (int i=startLineId; i<=endLineId; i++) {
			codeBlock.add(processedCode.get(i));
		}
		return codeBlock;
	}
	
	private String getClassNameFromLineId(int lineId) {
		String line = processedCode.get(lineId);
		int start = line.indexOf("class") + 6;
		int idx = start;
		int end = -1;
		
		while (idx < line.length() && end == -1) {
			if(!Character.isDigit(line.charAt(idx)) && !Character.isLetter(line.charAt(idx))) 
				end = idx-1;
			idx++;
		}
		
		if (end == -1) {
			System.err.println("Invalid class name");
			System.err.println("When trying to get class name at line " + lineId);
			System.exit(2);
		}
		
		return line.substring(start, end+1);
	}
	
	private String getMethodNameFromLineId(int lineId) {
		String line = processedCode.get(lineId);
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
		
		if (start == -1) {
			System.err.println("Invalid method name");
			System.err.println("When trying to get method name at line " + line);
			System.exit(2);
		}
		
		return line.substring(start, end);
	}
	
	private String getMethodParamsFromLineId(int lineId) {
		String line = processedCode.get(lineId);
		int start = line.indexOf("(");
		int end = line.lastIndexOf(")");		
		return line.substring(start, end+1);
	}
}
