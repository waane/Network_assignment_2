import java.io.Serializable;
import java.util.*;


public class VectorOnWire implements Serializable{
	public String _source="";
	public Map<String, Integer> _vectors = new Hashtable<String, Integer>();
	public int[] vecotrs = new int[DVImpl.NODE_COUNT];
	public VectorOnWire(String source, Map<String, Integer> vectors){		
		_source = source;
		_vectors = vectors;
		
	}
	
}
