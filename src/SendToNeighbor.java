import java.awt.image.DataBufferInt;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;

import javax.sql.ConnectionEvent;

public class SendToNeighbor implements Runnable {
	private final static int RETRY_LIMIT = 10;
	private Neighbor _neighbor = null;

	public SendToNeighbor(Neighbor neighbor) {
		_neighbor = new Neighbor(neighbor.routerName, neighbor.port, neighbor.weight);
	}// initialize sendtoNeighbor class with neighbors information.

	@Override
	public void run() {
		int retryCounter = 0;
		Socket toNeighborNode = null;
		boolean scanning = true;
		while (scanning) {
			try {
				toNeighborNode = new Socket("localhost", _neighbor.port);
				scanning = false;
				send(toNeighborNode);
			} catch (IOException e) {
				// System.out.println("Connect failed, waiting and trying again");
				try {
					if(retryCounter > RETRY_LIMIT){
						System.out.println("Connection to "+ _neighbor.routerName +" failed after "+ retryCounter +" trial"); 
						scanning = false;
						break;
					}
					Thread.sleep(2000);// 1 seconds
					retryCounter += 1;
					System.out.println("Retrying to connect : "
							+ _neighbor.routerName + " for " + retryCounter
							+ " times");

				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
		}// retry until neighbor router is connected or retryCount
	}// send distance vector to Neighbor

	private void send(Socket toNeighborNode) {
		try {
			ObjectOutputStream toNeighborStream = new ObjectOutputStream(toNeighborNode.getOutputStream());
			
			toNeighborStream.writeObject(new VectorOnWire(DVImpl.myName, DVImpl.myVector));
			toNeighborStream.close();
		} catch (IOException e) {

			System.out.println("sending vectors to neighbor failed : "
					+ e.toString());
		}
	}
}
