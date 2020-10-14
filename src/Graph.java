import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Graph {
	private List<Node> nodes;
	private List<Edge> edges;
	private String methodName;
	private String methodSignature;
	private String className;
	private List<String> methodCode;
	private Boolean debug;
	
	public Graph(
			String _methodName,
			String _methodSignature,
			String _className,
			List<String> _mC,
			Boolean _d) {
		nodes = new ArrayList<Node>();
		edges = new ArrayList<Edge>();
		methodName = _methodName;
		methodSignature = _methodSignature;
		className = _className;
		methodCode = _mC;
		debug = _d;
	}
	
	public Graph(
			String _methodName,
			String _methodSignature,
			String _className,
			Boolean _d) {
		nodes = new ArrayList<Node>();
		edges = new ArrayList<Edge>();
		methodName = _methodName;
		methodSignature = _methodSignature;
		className = _className;
		debug = _d;
	}
	
	public void buildNodes() {
		getNodes();
		numberNodes();
		combineNodes();
		fixNumbering();
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public String getMethodSignature() {
		return methodSignature;
	}
	
	public List<Edge> getEdges() {
		return edges;
	}
	
	// Get the first entry node
	public Node getEntryNode() {
		for (Node n : nodes) {
			if (n.isEntry() == true) {
				return n;
			}
		}
		return null;
	}
	
	// Get list of all the entry nodes 
	public List<Node> getEntryNodeList() {
		List<Node> nodeList = new LinkedList<Node>();
		for (Node n : nodes) {
			if (n.isEntry() == true) {
				nodeList.add(n);
			}
		}
		return nodeList;
	}
	
	// Get the first exit node
	public Node getExitNode() {
		for (Node n : nodes) {
			if (n.isExit() == true) {
				return n;
			}
		}
		return null;
	}
	
	// Get all the exit node list
	public List<Node> getExitNodeList() {
		List<Node> nodeList = new LinkedList<Node>();
		for (Node n : nodes) {
			if (n.isExit() == true) {
				nodeList.add(n);
			}
		}
		return nodeList;
	}
	
	// Get edge list that start from node
	public List<Edge> getEdgesStartingFrom(Node node) {
		List<Edge> chosenEdges = new LinkedList<Edge>();
		for (Edge e : edges) {
			if (e.GetStart() == node.GetNodeNumber()) {
				chosenEdges.add(e);
			}
		}
		return chosenEdges;
	}
	
	public List<Integer> getLastLineIdsFromNodes(List<Node> nodeList) {
		List<Integer> lineIds = new ArrayList<Integer>();
		for (Node n : nodeList) {
			lineIds.add(n.getLastLineId());
		}
		return lineIds;
	}
	
	public Node getNode(int nodeNumber) {
		for (Node n : nodes) {
			if(n.GetNodeNumber() == nodeNumber) {
				return n;
			}
		}
		return null;
		// TODO this may be O(1) if order is always guaranteed (not sure for line graphs)
		// return nodeNumber < nodes.size() ? nodes.get(nodeNumber) : null;
	}
	
	public void setEntryNode(int nodeId) {
		getNode(nodeId).SetEntry(true);
	}
	
	public void setExitNodes(List<Integer> nodeIds) {
		for (int id : nodeIds) {
			Node n = getNode(id);
			if (n != null) {
				n.SetExit(true);
			}
		}
	}

	public String listNodes() {
		String output = "";
		for (Node n : nodes) {
			output += " " + n;
		}
		return output;
	}
	
	public String listEdges() {
		String output = "";
		for (Edge e : edges) {
			output += " " + e;
		}
		return output;
	}
	
	public String listGraphStructure(Map<Integer, List<Integer>> mapping) {
		String output = "";
		for (Node node : nodes) {
			output += "Printing source code present in node #" + node.GetNodeNumber() + "...\n";
			
			String sourceCode = node.GetSourceCode();
			String[] lines = sourceCode.split("\n");
			
			for(int i=0; i < lines.length ; i++) {
				String line = lines[i];
				line.replace("%forcenode%", "[FOR component] ");
				Integer curLineId = node.GetStartingLineId() + i;
				//List<Integer> originalLines = Helper.incOneToAll(mapping.get(curLineId));
				List<Integer> originalLines = mapping.get(curLineId);
				output += " - Code `" + line + "` originally present at line(s) " + 
						originalLines + "\n";
			}
			output += "End of source code for node " + node.GetNodeNumber() + "\n\n";		
		}
		output += "=========\n\n";
		return output;
	}
	
	public String listLineEdges(Map<Integer, List<Integer>> mapping) {
		String output = "";
		output += "Entry line: " + getEntryNodeList().get(0).GetStartingLineId() + "\n";
		output += "Exit lines: " + getLastLineIdsFromNodes(getExitNodeList()) + "\n";
		
		List<Edge> lineEdges = getLineEdges();
		for (Edge e : lineEdges) {
			output += (e.GetStart()) + " -> " + (e.GetEnd()) + "\n";
		}
		return output;
	}

	public void buildFromEdges(List<Edge> _edges) {
		if (debug) {
			System.out.println("* Building line graph from edges: " + _edges);
		}
		List<Integer> addedNodes = new ArrayList<Integer>();
		for(Edge e : _edges) {
			int start = e.GetStart();
			int end = e.GetEnd();
			if (!addedNodes.contains(start)) {
				nodes.add(new Node(start, "", false, false));
			}
			if (!addedNodes.contains(end)) {
				nodes.add(new Node(end, "", false, false));
			}
		}
		edges = _edges;
	}
	
	public void adjustLineNumbers(int blockStartingLine, Map<Integer, List<Integer>> mapping) {
		for (Node n : nodes) {
			n.SetStartingLineId(n.GetStartingLineId() + blockStartingLine);
			n.applyLineMapping(mapping);
		}
	}

	public List<Edge> getLineEdges() {
		List<Edge> lineEdgeList = new ArrayList<Edge>();
		
		for (Node n : nodes) {
			List<Integer> lines = n.GetSourceCodeLineIds();
			for (int i = 1; i < lines.size(); i++) {
				lineEdgeList.add(new Edge(lines.get(i-1), lines.get(i)));
			}
		}
		
		for (Edge e : edges) {
			Node src = nodes.get(e.GetStart());
			Node tgt = nodes.get(e.GetEnd());
			Integer srcLine = src.getLastLineId();
			Integer tgtLine = tgt.GetStartingLineId();
			if (srcLine != tgtLine) {
				lineEdgeList.add(new Edge(srcLine, tgtLine));
			}
		}
		
		if (lineEdgeList.size() == 0) {
			int lineId = getEntryNode().GetStartingLineId();
			lineEdgeList.add(new Edge(lineId, lineId));
		}
		
		return lineEdgeList;
	}
	
	public void simplifyDummyEdges() {
		// first simplify edges
		List<Node> nodesToUnsetExit = new ArrayList<>();
		
		for (int edgeId=0; edgeId < edges.size(); edgeId++) {
			Edge curEdge = edges.get(edgeId);
			Node tgtNode = getNode(curEdge.GetEnd());
			if (tgtNode.isDummy()) {
				int startNodeId = curEdge.GetStart();
				if (tgtNode.isExit()) {
					getNode(startNodeId).SetExit(true);
					nodesToUnsetExit.add(tgtNode);
				} else {
					List<Edge> tgtNodeEdges = getEdgesStartingFrom(tgtNode);
					for (Edge tgtEdge : tgtNodeEdges) {
						int newTarget = tgtEdge.GetEnd();
						edges.add(new Edge(startNodeId, newTarget));
					}
				}
				edges.remove(edgeId);
				edgeId--;
			}
		}
		
		// remove remaining edges leaving from dummy nodes
		for (int edgeId=0; edgeId < edges.size(); edgeId++) {
			Edge curEdge = edges.get(edgeId);
			Node srcNode = getNode(curEdge.GetStart());
			if (srcNode.isDummy()) {
				edges.remove(edgeId);
				edgeId--;
			}
		}
		
		for (Node dummyNode : nodesToUnsetExit) {
			dummyNode.SetExit(false);
		}
	}
	
	public void writePng() {
		Helper.writePng(className + "/" + methodSignature + ".png", generateDOT());
	}

	private void addNode(String sourceCode, int startingLineId) {
		if (debug) 
			System.out.println("* Added node for code " + sourceCode + " starting at line " + startingLineId);
		Node node = new Node(0, sourceCode, false, false);
		node.SetStartingLineId(startingLineId);
		nodes.add(node);
	}
	
	private void addEdge(int srcNodeId, int tgtNodeId) {
		if (debug) 
			System.out.println("* Added edge from " + srcNodeId + " to " + tgtNodeId);
		edges.add(new Edge(srcNodeId, tgtNodeId));
	}

	private int getPreviousInstructionLineId(int startingLineId) {
		int prevLineId = startingLineId-1;
		while (prevLineId > -1 && methodCode.get(prevLineId).equals("}"))
			prevLineId--;
		return prevLineId;
	}
	
	private int getNextInstructionLineId(int startingLineId) {
		int nextLineId = startingLineId+1;
		while (nextLineId < methodCode.size() && methodCode.get(nextLineId).equals("}")) 
			nextLineId++;
		return nextLineId;
	}
	
	private void getNodes() {
		if (debug) {
			dumpCode();
		}
		
		List<Integer> conditionalStartLines = new ArrayList<Integer>();
		List<List<Integer>> edgeStartLinesList = new ArrayList<List<Integer>>();
		
		for (int i=0; i<methodCode.size(); i++) {
			String line = methodCode.get(i);
			
			//if we find a close brace, need to figure out where to go from here
			if (line.matches("}")) {
				//find the opening
				int openline = Helper.findStartOfBlock(methodCode, i-1, true);
				
				//for loops, add an edge back to the start
				if (methodCode.get(openline).toLowerCase().matches("^\\b(for|while|do)\\b.*")) {
					if (methodCode.get(openline).toLowerCase().matches("^\\b(for|while)\\b.*")) {
						addEdge(getPreviousInstructionLineId(i),openline);	
						addEdge(openline,getNextInstructionLineId(i));
					}
					else if (methodCode.get(i+1).toLowerCase().matches("^\\b(while)\\b.*")) { //do
						addEdge(getPreviousInstructionLineId(openline), getNextInstructionLineId(openline)); //entry edge that skips the "do" statement
						addEdge(getPreviousInstructionLineId(i),i+1); //into loop test
						addEdge(i+1,getNextInstructionLineId(openline)); //looping edge
						addEdge(i+1,getNextInstructionLineId(i+1)); //loop exit edge
					} else {
						System.err.println("Do without while");
						System.exit(2);
					}
				}
				//for conditionals, we won't add edges until after the block.  Then link all the close braces to the end of the block
				else if (methodCode.get(openline).toLowerCase().matches("^\\b(if|else if)\\b.*")) {
					if (methodCode.get(openline).toLowerCase().matches("^\\bif\\b.*")) {
						edgeStartLinesList.add(new ArrayList<Integer>());
						
						conditionalStartLines.add(openline);
						addEdge(openline,openline+1);
					} else { // else if
						int conditionalStartLine = conditionalStartLines.get(conditionalStartLines.size()-1);
						addEdge(conditionalStartLine,openline);
						addEdge(openline,openline+1);
						conditionalStartLines.set(conditionalStartLines.size()-1, openline);
					}
					
					//if we're not done with the conditional block, save the start of this edge until we find the end of the block
					if (methodCode.size() > i+1 && methodCode.get(i+1).toLowerCase().matches("^\\belse\\b.*")) {
						edgeStartLinesList.get(edgeStartLinesList.size()-1).add(getPreviousInstructionLineId(i));
					}
					else {
						for (Integer start: edgeStartLinesList.get(edgeStartLinesList.size()-1)) {
							if (!methodCode.get(start).matches("^\\bcontinue\\b.*")) {
								addEdge(start, i+1);
							}
						}
						edgeStartLinesList.get(edgeStartLinesList.size()-1).clear();
						edgeStartLinesList.remove(edgeStartLinesList.size()-1);
						conditionalStartLines.remove(conditionalStartLines.size()-1);
						
						int prevInstrId = getPreviousInstructionLineId(i);
						if (!methodCode.get(prevInstrId).matches("^\\bcontinue\\b.*")) {
							addEdge(prevInstrId,getNextInstructionLineId(i));
						}
						addEdge(openline,getNextInstructionLineId(i));
					}
				}
				else if (methodCode.get(openline).toLowerCase().substring(0,4).equals("else")) {
					if (edgeStartLinesList.size() == 0 
							|| edgeStartLinesList.get(edgeStartLinesList.size()-1).size() == 0){
						System.err.println("Else without if block");
						System.exit(2);
					}
					edgeStartLinesList.get(edgeStartLinesList.size()-1).add(i-1);
					
					int conditionalStartLine = conditionalStartLines.get(conditionalStartLines.size()-1);
					addEdge(conditionalStartLine,openline+1);
					
					for (Integer start: edgeStartLinesList.get(edgeStartLinesList.size()-1)){
						addEdge(start, i+1);
					}
					
					edgeStartLinesList.get(edgeStartLinesList.size()-1).clear();
					edgeStartLinesList.remove(edgeStartLinesList.size()-1);
					conditionalStartLines.remove(conditionalStartLines.size()-1);
				}
				else if (methodCode.get(openline).toLowerCase().matches("^\\bswitch\\b.*")){
					addEdgesForSwitch(openline, i);
				}
			}
			else {
				//TODO REMOVE AND CHECK IF IT CREATES SEPARATE NODES
				
				//we'll add a node and an edge unless these are not executable lines
				if (!methodCode.get(i).matches("^\\b(do|else(?!\\s+if)|default)\\b.*")){
					addNode(line,i);
					if (methodCode.get(i).matches("^\\bcontinue\\b.*")) {
						int loopStart = Helper.findConditionalLine(methodCode, i);
						addEdge(i, loopStart);
					}
					if (i > 0) {
						String previousInstruction = methodCode.get(getPreviousInstructionLineId(i));
						if (!previousInstruction.matches("^\\b(do|else(?!\\s+if)|case|default|continue|break|switch)\\b.*") 
								&& !methodCode.get(i-1).equals("}")){
							addEdge(getPreviousInstructionLineId(i), i);
						}
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
		for (int i=0; i<nodes.size(); i++) {
			if (Helper.lineContainsReservedWord(nodes.get(i).GetSourceCode(), "(return|throw)")) {
				//mark node as an exit node
				Node n = nodes.get(i);
				n.SetExit(true);
				nodes.set(i,n);
				
				//remove any lines coming from that node
				for (int j=0; j<edges.size(); j++){
					if (edges.get(j).GetStart() == n.GetStartingLineId()) edges.remove(j);
				}
			}
		}
		
		if (debug){
			dumpEdges();
		}
	}
	
	private void addEdgesForSwitch(int switchStartLineId, int switchEndLineId) {
		for (int i = switchStartLineId; i < switchEndLineId; i++){ //iterate through the case statement
			if (methodCode.get(i).matches("^\\bcase\\b.*")) {
				addEdge(switchStartLineId, i);
				int nextInstrId = getNextInstructionLineId(i);
				if (methodCode.get(nextInstrId).matches("^\\bdefault\\b.*")) {
					addEdge(i, getNextInstructionLineId(nextInstrId));
				} else {
					addEdge(i, nextInstrId);
				}
			}
			else if (methodCode.get(i).matches("^\\bdefault\\b.*")) {
				int nextInstrId = getNextInstructionLineId(i);
				addEdge(switchStartLineId, nextInstrId);
			}
			else if (methodCode.get(i).matches("^break;")) {
				addEdge(i, getNextInstructionLineId(i));
			}
		}
	}
	
	private void combineNodes() {
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
				n.SetSourceCode("");
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
			if (nodes.get(i).GetEdgesFrom() != 1 || nodes.get(i).GetSourceCode().contains("%forcenode%")) continue;
			
			// find the edge leaving this node
			int midEdge = 0;
			while (midEdge < edges.size() && edges.get(midEdge).GetStart() != nodes.get(i).GetNodeNumber()) midEdge++;
			int nextNode = 0;
			while (nodes.get(nextNode).GetNodeNumber() != edges.get(midEdge).GetEnd()) nextNode++;
			
			// if there's more than one edge entering the next node, we can't combine
			if (nodes.get(nextNode).GetEdgesTo() > 1 || nodes.get(nextNode).GetSourceCode().contains("%forcenode%")) continue;
			
			// if it's a self-loop we can't combine
                        if (nextNode == i) continue;	

			// If we got here we can combine the nodes
						
			//copy the sourceline (we'll delete nextNode)
			nodes.get(i).SetSourceCode(nodes.get(i).GetSourceCode()+"\n"+nodes.get(nextNode).GetSourceCode());
			nodes.get(i).GetSourceCodeLineIds().addAll(nodes.get(nextNode).GetSourceCodeLineIds());
			
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

	private void numberNodes() {
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
			
			while (newStart < nodes.size() && nodes.get(newStart).GetStartingLineId() != oldedges.get(i).GetStart()) newStart++;
			while (newEnd < nodes.size() && nodes.get(newEnd).GetStartingLineId() != oldedges.get(i).GetEnd()) newEnd++;
			
			addEdge(newStart,newEnd);
		}
		
	}

	private void fixNumbering() {
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
	
	private String generateDOT() {
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
			
			if (n.GetSourceCode().contains("%forcelabel%")){
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
	
	private void dumpCode() {
		String outlines = "\n***** Processed Source Code:\n\n";
		for (int i=0; i<methodCode.size(); i++) 
			outlines += i + ": " + methodCode.get(i) + "\n";
		System.out.printf("%s\n", outlines);
	}
	
	private void dumpEdges() {
		System.out.print("\n***** Edges:\n   "
				+ "- numbers correspond to processed source code line numbers (above)\n   "
				+ "- basic block nodes not yet combined\n\n");
		for (Edge e: edges) 
			System.out.println("("+e.GetStart()+","+e.GetEnd()+")");
		System.out.println();
	}
}
