import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MethodGraph {
	private List<Node> nodes;
	private List<Edge> edges;
	private String methodName;
	private String className;
	private List<String> methodLines;
	private Boolean printDebug;
	
	public MethodGraph(
			String _methodName,
			String _className,
			List<String> _mL,
			Boolean _pD) {
		nodes = new ArrayList<Node>();
		edges = new ArrayList<Edge>();
		methodName = _methodName;
		className = _className;
		methodLines = _mL;
		printDebug = _pD;
	}
	
	public void SetNodes(List<Node> nodeList) {
		nodes = nodeList;
	}
	
	public void computeNodes() {
		getNodes();
		numberNodes();
		combineNodes();
		fixNumbering();
	}
	
	public String GetClassName() {
		return className;
	}
	
	public String GetMethodName() {
		return methodName;
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
	
	/*
	public void fixNodeNumbers(int nextNodeId) {
		for(int i=0; i<nodes.size(); i++) {
			nodes.get(i).SetNodeNumber(nodes.get(i).GetNodeNumber() + nextNodeId);
		}
		for(int i=0; i<edges.size(); i++) {
			edges.get(i).SetValues(edges.get(i).GetStart() + nextNodeId, edges.get(i).GetEnd() + nextNodeId);
		}
	}
	*/
	
	public void fixLineNumbers(int incAmount) {
		for(int i=0; i<nodes.size(); i++) {
			nodes.get(i).SetSrcLineIdx(nodes.get(i).GetSrcLineIdx()+incAmount);
		}
	}
	
	public int getAmountOfNodes() {
		return nodes.size();
	}
	
	public void writePng() {
		writePng(className + "__" + methodName + ".png");
	}
	
	public void writePng(String path) {
		String strDOT = generateDOT();
		
		if (printDebug) System.out.println("\n***** Generated DOT Code:\n\n"+strDOT+"\n\n");
				
		File out = new File(path);
		
		GraphViz gv = new GraphViz();
		gv.writeGraphToFile(gv.getGraph(strDOT, "png"), out);
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
	
	public void PrintGraphStructure(List<Map<Integer, List<Integer>>> mappings) {
		for (Node node : nodes) {
			System.out.println("Printing source code present in node #" + node.GetNodeNumber() + "...");
			String unifiedLines = node.GetSrcLine();
			String[] lines = unifiedLines.split("\n");
			
			for(int i=0; i < lines.length ; i++) {
				String line = lines[i];
				line.replace("%forcenode%", "[FOR component] ");
				Integer curLineId = node.GetSrcLineIdx() + i;
				List<Integer> originalLines = mappings.get(mappings.size()-1).get(curLineId);
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
	
	public void PrintLineFlow(List<Map<Integer, List<Integer>>> mappings) {
		System.out.println("Printing line flow for class " + className + " method " + methodName + "...");
		PrintNodeLineFlow(0, mappings, new ArrayList<Integer>());
		System.out.println("\n");
	}
	
	private void PrintNodeLineFlow(int nodeNumber, List<Map<Integer, List<Integer>>> mappings, List<Integer> visitedNodes) {			
		Node node = nodes.get(nodeNumber); // todo check if node id = node position in array
		String unifiedLines = node.GetSrcLine();
		String[] lines = unifiedLines.split("\n");
		for(int i=0; i < lines.length ; i++) {
			Integer curLineId = node.GetSrcLineIdx() + i;
			List<Integer> originalLines = mappings.get(mappings.size()-1).get(curLineId);
			
			// TODO why shouldn't I increment 1?
			System.out.print(originalLines);
			if (i != lines.length -1) {
				System.out.print(" -> ");
			}
		}
		
		if(visitedNodes.contains(nodeNumber)) {
			System.out.print("[loop]");
			return;
		}
		visitedNodes.add(nodeNumber);
		
		List<Integer> targetNodes = getTargetsOfNode(nodeNumber);
		if (targetNodes.size() > 1) {
			System.out.print(" -> (");
			
			for(int i=0; i < targetNodes.size(); i++) {
				PrintNodeLineFlow(targetNodes.get(i), mappings, visitedNodes);
				if (i < targetNodes.size()-1) 
					System.out.print(" ; ");
			}
			System.out.print(")");
		} else if (targetNodes.size() == 1) {
			System.out.print(" -> ");
			PrintNodeLineFlow(targetNodes.get(0), mappings, visitedNodes);
		} else {
			System.out.print("[end]");
		}
	}
	
	private List<Integer> getTargetsOfNode(int src) {
		List<Integer> targets = new ArrayList<Integer>();
		for (Edge edge : edges) {
			if (edge.GetStart() == src) {
				targets.add(edge.GetEnd());
			}
		}
		return targets;
	}
	
	private void getNodes(){
		if (printDebug){
			String outlines="\n***** Processed Source Code:\n\n";
			for (int i=0; i<methodLines.size(); i++) outlines += i+": "+methodLines.get(i)+"\n";
			System.out.printf("%s\n", outlines);
		}
		
		int conditionalStartLine=0;
		List<Integer> edgeStartLines = new ArrayList<Integer>();
		
		for (int i=0; i<methodLines.size(); i++){
			
			String line = methodLines.get(i);
			
			//if we find a close brace, need to figure out where to go from here
			if (line.matches("}")){
				
				//find the opening
				int openline = findStartOfBlock(i-1, true);
				
				//for loops, add an edge back to the start
				if (methodLines.get(openline).toLowerCase().matches("^(for|while|do).*")){
					if (methodLines.get(openline).toLowerCase().matches("^(for|while).*")){
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
				else if (methodLines.get(openline).toLowerCase().matches("^(if|else if).*")){
					if (methodLines.get(openline).toLowerCase().matches("^if.*")) conditionalStartLine = openline;
					addEdge(conditionalStartLine,openline+1);
					//if we're not done with the conditional block, save the start of this edge until we find the end of the block
					if (methodLines.size() > i+1 && methodLines.get(i+1).toLowerCase().matches("^else.*")){
						
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
				else if (methodLines.get(openline).toLowerCase().substring(0,4).equals("else")){
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
				else if (methodLines.get(openline).toLowerCase().matches("^switch.*")){

					//add edges to cases
					for (int k=openline; k<i; k++){ //iterate through the case statement
						if (methodLines.get(k).matches("^(case|default).*")){
							if (methodLines.get(k).matches(":$")) addEdge(openline,k);
							else addEdge(openline,getNextLine(k));  //didnt't split lines at : so could be the next line
						}
						if (methodLines.get(k).matches("^break;")) addEdge(k,getNextLine(i));
					}
				}
			}
			
			else{
				//TODO REMOVE AND CHECK IF IT CREATES SEPARATE NODES
				
				//we'll add a node and an edge unless these are not executable lines
				if (!methodLines.get(i).toLowerCase().matches("^(do|else|case|default).*")){
					addNode(line,i);
					if (i>0 && !methodLines.get(getPrevLine(i)).toLowerCase().matches("^(do|else|case|default).*") && !methodLines.get(i-1).equals("}")){
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
		for (int i=0; i<nodes.size(); i++) {
			if (nodes.get(i).GetSrcLine().contains("return")) {
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
			
			while (newStart < nodes.size() && nodes.get(newStart).GetSrcLineIdx() != oldedges.get(i).GetStart()) newStart++;
			while (newEnd < nodes.size() && nodes.get(newEnd).GetSrcLineIdx() != oldedges.get(i).GetEnd()) newEnd++;
			
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
	
	/*
	private void fixNodesAndEdgesNumbers(int nextNodeNumber, int startingLine) {
		for(int i=0; i<nodes.size(); i++) {
			nodes.get(i).SetNodeNumber(nextNodeNumber+i);
			nodes.get(i).SetSrcLineIdx(nodes.get(i).GetSrcLineIdx()+startingLine);
		}
		
		for(int i=0; i<edges.size(); i++) {
			edges.get(i).SetValues(edges.get(i).GetStart() + nextNodeNumber, edges.get(i).GetEnd() + nextNodeNumber);
		}
	}
	*/
	
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
	
	private void addNode(String line, int lineidx) {
		Node node = new Node(0,line,false,false);
		node.SetSrcLineIdx(lineidx);
		nodes.add(node);
	}
	
	private void addEdge(int startidx, int endidx) {
		edges.add(new Edge(startidx,endidx));
	}

	private int getPrevLine(int start) {
		int prevEdge=start-1;
		while (prevEdge > -1 && methodLines.get(prevEdge).equals("}")) prevEdge--;
		return prevEdge;
	}
	
	private int getNextLine(int start) {
		int nextEdge=start+1;
		while (nextEdge < methodLines.size() && methodLines.get(nextEdge).equals("}")) nextEdge++;
		return nextEdge;
	}
	
	private int findStartOfBlock(int startingLine, boolean useBlockLines) {
		int curLineId = startingLine;
		int openingLine = -1;
		int depth = 0;
		
		while (curLineId >= 0 && openingLine == -1) {
			String curLine = methodLines.get(curLineId);
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
}