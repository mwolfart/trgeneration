import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Node {
	private int node_number;	// Node number
	private String srcline;  	// the source code making up the node
	private boolean isEntry; 	// true if this is the entry node
	private boolean isExit;  	// true if this is an exit node
	
	private int srcLineIdx;
	private int edgesFrom;
	private int edgesTo;
	
	private List<Integer> srcLinesIndex = new ArrayList<Integer>();
	
	public Node(){}
	
	public Node(int _node_number, String _srcline, boolean _isEntry, boolean _isExit, List<Integer> _srcLinesIndex) {
		node_number = _node_number;
		srcline = _srcline;
		isEntry = _isEntry;
		isExit = _isExit;
		srcLinesIndex = _srcLinesIndex;
	}
	
	public Node(int _node_number, String _srcline, boolean _isEntry, boolean _isExit) {
		node_number = _node_number;
		srcline = _srcline;
		isEntry = _isEntry;
		isExit = _isExit;
		srcLinesIndex = new ArrayList<Integer>();
	}
	
	public void SetSrcLineIdx(int idx){	srcLineIdx=idx;	}
	public int GetSrcLineIdx(){	return srcLineIdx; }
		
	public int GetEdgesFrom(){ return edgesFrom; }
	public void ClearEdgesFrom(){	edgesFrom=0; }
	public void IncEdgesFrom(){	edgesFrom++; }
	
	public int GetEdgesTo(){ return edgesTo;	}
	public void IncEdgesTo(){ edgesTo++; }
	
	public int GetNodeNumber() { return node_number; }
	public void SetNodeNumber(int i){ node_number = i; }
	
	public String GetSrcLine() { return srcline; }
	public void SetSrcLine(String line) { srcline = line; }
	
	public List<Integer> GetSrcLinesIndex() { return srcLinesIndex; }
	public void SetSrcLinesIndex(List<Integer> linesIndex) { srcLinesIndex = linesIndex; }
	
	public boolean isEntry() { return isEntry;	}
	public void SetEntry(boolean e){	isEntry = e; }
	
	public boolean isExit() { return isExit; }
	public void SetExit(boolean e){ isExit = e; }
	
	public String toString() {
		return "Node" + node_number;
	}
	
	public boolean isSameNode(Node _node) {
		return node_number == _node.GetNodeNumber();
	}
	
	public int getNumLines() {
		return StringUtils.countMatches(srcline, '\n') + 1;
	}
	
	public int GetLastLineId() {
		return (srcLinesIndex.size() > 0 ? srcLinesIndex.get(srcLinesIndex.size() - 1) : srcLineIdx);
	}
	
	public void fixLinesIndex(List<Map<Integer, List<Integer>>> mappings) {
		for(int i = srcLineIdx; i < srcLineIdx+getNumLines(); i++) {
			List<Integer> tgtLines = mappings.get(mappings.size()-1).get(i);
			for(int tgt : tgtLines) {
				srcLinesIndex.add(tgt);
			}
		}
	}
}
