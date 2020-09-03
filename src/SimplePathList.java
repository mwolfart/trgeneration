import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class SimplePathList {
	protected List<SimplePath> spl;
	private boolean printAsLines = false;
	private boolean breakLines = false;	
	
	public SimplePathList() {
		spl = new LinkedList<SimplePath>();
	}
	
	public SimplePathList(List<Node> _nodes) {
		spl = new LinkedList<SimplePath>();
		AddInitialNodes(_nodes);
	}
	
	// Add simple paths with given node list
	public void AddInitialNodes(List<Node> _nodeList) {
		SimplePath sp;
		for(int i = 0; i < _nodeList.size(); i++) {
			Node node = _nodeList.get(i);
			sp = new SimplePath(node);
			spl.add(sp);
		}
	}
	
	public Iterator<SimplePath> iterator() {
		return spl.iterator();
	}
	
	public void add(SimplePath _sp) {
		spl.add(_sp);
	}
	
	public int size() {
		return spl.size();
	}
	
	public void setLineMode() {
		printAsLines = true;
	}

	public void allowLineBreaks() {
		breakLines = true;
	}
	
//	public String toString() {
//		SimplePath sp;
//		String res = "";
//		for(int i = 0; i < spl.size(); i++) {
//			sp = spl.get(i);
//			res += sp + " ";
//		}
//		return res;
//	}
	
	public String toString() {
		// Sort SimplePath list
		Collections.sort(spl, new Comparator<SimplePath>() {
			public int compare(SimplePath _s1, SimplePath _s2) {
				if(_s1.len() == _s2.len()) {		// If lengths are same
					int len = _s1.len();
					for(int i = 0; i < len; i++) {	// Compare node one by one
						if(_s1.GetNode(i).GetNodeNumber() != _s2.GetNode(i).GetNodeNumber()) {
							return(_s1.GetNode(i).GetNodeNumber() - _s2.GetNode(i).GetNodeNumber());
						}
					}
					return 0;
				}
				else								// If lengths are different, compare them
					return _s1.len() - _s2.len();
			}
		});
		
		SimplePath sp;
		String res = "";
		for(int i = 0; i < spl.size(); i++) {
			sp = spl.get(i);

			int lastLineListed = -1;
			if (printAsLines) {
				List<Node> nodeList = sp.GetNodes();
				
				if (nodeList.size() > 1) {
					lastLineListed = nodeList.get(0).GetLastLineId()+1;
					res += "[" + lastLineListed + ", ";
					for(int j = 1; j < nodeList.size()-2; j++) {
						for(Integer n : nodeList.get(j).GetSrcLinesIndex()) {
							if (n+1 != lastLineListed) {
								res += (n+1) + ", ";
								lastLineListed = n+1;
							}
						}
					}
					int lastLineId = (nodeList.get(nodeList.size()-1).GetSrcLinesIndex().get(0)+1);
					if (lastLineId != lastLineListed) {
						res += lastLineId;
					} else {
						res = res.substring(0, res.length()-2);
					}
					res += "]" + (breakLines ? "\n" : " ");
				} else {
					res += "[";
					for(Integer n : nodeList.get(0).GetSrcLinesIndex()) {
						if (n+1 != lastLineListed) {
							res += (n+1) + ", ";
						}
					}
					res = res.substring(0, res.length()-2);
					res += "]";
				}
			} else {
				res += sp + " ";
			}
		}
		
		return res;
	}	
}
