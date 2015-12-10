import java.io.*;
import java.net.*;

public class FromNeighbor implements Runnable {
	private Socket clientSocket;

	public FromNeighbor(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try {
			/* getting input and output streams of the client socket */
			OutputStream outputStream = clientSocket.getOutputStream();
			InputStream inputStream = clientSocket.getInputStream();

			/* sending welcome message to the client */
			String message = "Welcome! You are connected.\n";
			outputStream.write(message.getBytes());
			outputStream.flush();

			/* getting the client reply */
			int character = inputStream.read();

			while (character != -1) {
				System.out.print((char) character);
				character = inputStream.read();
			}

			inputStream.close();
			outputStream.close();
		} catch (Exception ex) {
			System.out.println("Error: " + ex.getMessage());
		}
	}
}
