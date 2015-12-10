import java.net.InetAddress;

public class Neighbor {

	String routerName;
	int port;
	int weight;
	//int[][] costs = new int[DVImpl.NODE_COUNT][DVImpl.NODE_COUNT];

	public Neighbor(String name, int port, int weight) {
		this.routerName = name;
		this.port = port;
		this.weight = weight;
		//this.costs = costs.clone();

	}
}
