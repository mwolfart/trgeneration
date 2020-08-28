

	/*
	public void PrintLineFlow(List<Map<Integer, List<Integer>>> mappings) {
		System.out.println("Printing line flow for class " + className + " method " + methodName + "...");
		String lineFlow = getNodeLineFlow(0, mappings, new ArrayList<Integer>());
		String lineFlowClean = cleanLineFlow(lineFlow);
		System.out.println(lineFlowClean);
		System.out.println("\n");
	}
	
	List<Integer> todoNodes = new ArrayList<>();
	private String getNodeLineFlow(int nodeNumber, List<Map<Integer, List<Integer>>> mappings, List<Integer> visitedNodes) {			
		Node node = nodes.get(nodeNumber); // todo check if node id = node position in array
		String unifiedLines = node.GetSrcLine();
		String[] lines = unifiedLines.split("\n");
		String result = "";

		// fact: node i will always point to node i+1 in non-disjunct trees
		// this can be used to perform some interesting operations below
		
		if (todoNodes.contains(nodeNumber) && !visitedNodes.contains(nodeNumber)) {
			result += ") -> ";
			visitedNodes.add(nodeNumber);
		} else if (visitedNodes.contains(nodeNumber)) {
			result += nodeNumber + " <loop>)";
			return result;
		} else if (!visitedNodes.contains(nodeNumber-1) && nodeNumber > 0) {
			todoNodes.add(nodeNumber);
			return result;
		} else {
			visitedNodes.add(nodeNumber);
		}
		
		for(int i=0; i < lines.length ; i++) {
			Integer curLineId = node.GetSrcLineIdx() + i;
			List<Integer> originalLines = Helper.incOneToAll(mappings.get(mappings.size()-1).get(curLineId));
			
			result += originalLines;
			if (i != lines.length -1) {
				result += " -> ";
			}
		}
		
		List<Integer> targetNodes = getTargetsOfNode(nodeNumber);
		if (targetNodes.size() > 1) {
			result += " -> (";
			
			for(int i=0; i < targetNodes.size(); i++) {
				result += getNodeLineFlow(targetNodes.get(i), mappings, visitedNodes);
				if (i < targetNodes.size()-1 && result.charAt(result.length()-1) != ')')
					result += " ; ";
				else if (i < targetNodes.size()-1)
					result += " -> ";
			}
			//result += ")";
		} else if (targetNodes.size() == 1) {
			String newResult = getNodeLineFlow(targetNodes.get(0), mappings, visitedNodes);
			result += (newResult != "" && !newResult.startsWith(")")) ? " -> " + newResult : "";
		} else {
			result += " <end>";
		}
		
		return result;
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
	
	private String cleanLineFlow(String lineFlow) {
		lineFlow = lineFlow.replace(",", " ->");
		lineFlow = lineFlow.replace("[", "");
		lineFlow = lineFlow.replace("]", "");
		String[] items = lineFlow.split(" -> ");
		
		for(int i = 0; i < items.length - 1; i++) {
			if (items[i].equals(items[i+1])) {
				items[i] = "";
			}
		}
		
		lineFlow = String.join(" -> ", items);
		lineFlow = lineFlow.replace("->  ->", "->");
		
		return lineFlow;
	}
	*/