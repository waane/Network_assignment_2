import java.io.IOException;
import java.net.Socket;


public class ReadNetMap implements Runnable {
	
	
	
	@Override
	public void run() {
		while(true){
			
			
			
			try {
				Thread.sleep(DVImpl.READ_MAP_INTERVAL_IN_MILLIS);//Read netMap File 
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}// send distance vector to Neighbor

	
}
