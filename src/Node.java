import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Node {
	private int nodeNumber;	// Node number
	private String sourceCode;  	// the source code making up the node
	private boolean isEntry; 	// true if this is the entry node
	private boolean isExit;  	// true if this is an exit node
	
	private int startingLineId;
	private int edgesFrom;
	private int edgesTo;
	
	private List<Integer> sourceCodeLineIds = new ArrayList<Integer>();
	
	public Node(){}
	
	public Node(int _nodeNumber, String _sourceCode, boolean _isEntry, boolean _isExit, List<Integer> _sourceCodeLineIds) {
		nodeNumber = _nodeNumber;
		sourceCode = _sourceCode;
		isEntry = _isEntry;
		isExit = _isExit;
		sourceCodeLineIds = _sourceCodeLineIds;
	}
	
	public Node(int _nodeNumber, String _sourceCode, boolean _isEntry, boolean _isExit) {
		nodeNumber = _nodeNumber;
		sourceCode = _sourceCode;
		isEntry = _isEntry;
		isExit = _isExit;
		sourceCodeLineIds = new ArrayList<Integer>();
	}
	
	public void SetStartingLineId(int idx){	startingLineId=idx;	}
	public int GetStartingLineId(){	return startingLineId; }
		
	public int GetEdgesFrom(){ return edgesFrom; }
	public void ClearEdgesFrom(){	edgesFrom=0; }
	public void IncEdgesFrom(){	edgesFrom++; }
	
	public int GetEdgesTo(){ return edgesTo;	}
	public void IncEdgesTo(){ edgesTo++; }
	
	public int GetNodeNumber() { return nodeNumber; }
	public void SetNodeNumber(int i){ nodeNumber = i; }
	
	public String GetSourceCode() { return sourceCode; }
	public void SetSourceCode(String line) { sourceCode = line; }
	
	public List<Integer> GetSourceCodeLineIds() { return sourceCodeLineIds; }
	public void SetSourceCodeLineIds(List<Integer> linesIndex) { sourceCodeLineIds = linesIndex; }
	
	public boolean isEntry() { return isEntry;	}
	public void SetEntry(boolean e){	isEntry = e; }
	
	public boolean isExit() { return isExit; }
	public void SetExit(boolean e){ isExit = e; }
	
	public String toString() {
		return "Node" + nodeNumber;
	}
	
	public boolean isSameNode(Node _node) {
		return nodeNumber == _node.GetNodeNumber();
	}
	
	public int getNumLines() {
		return StringUtils.countMatches(sourceCode, '\n') + 1;
	}
	
	public int getLastLineId() {
		return (sourceCodeLineIds.size() > 0 ? sourceCodeLineIds.get(sourceCodeLineIds.size() - 1) : startingLineId);
	}
	
	public void applyLineMapping(Map<Integer, List<Integer>> mapping) {
		for(int i = startingLineId; i < startingLineId+getNumLines(); i++) {
			List<Integer> tgtLines = mapping.get(i);
			for(int tgt : tgtLines) {
				sourceCodeLineIds.add(tgt + 1);
			}
		}
		startingLineId = mapping.get(startingLineId).get(0) + 1;
	}
}
