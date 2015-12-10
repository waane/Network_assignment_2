import java.net.*;
import java.util.*;
import java.io.*;

import javax.swing.JTable.PrintMode;

public class DVImpl {
	private static final int INFINITE = -1;
	protected static final int NODE_COUNT = 5;
	private static final int ATOI_NUM = 65;
	private static ServerSocket routerSocket;
	private static int port = 0;
	private static Map<String, Integer> neighborPort = new Hashtable<String, Integer>();
	private static List<Neighbor> neighbors = new ArrayList<Neighbor>();
	protected static int[][] distanceVector = new int[NODE_COUNT][NODE_COUNT];
	protected static Map<String, Integer> distance = new Hashtable<String, Integer>();
	public static void main(String args[]) {
		if (args.length != 2) {
			System.out.println("input parameter가 잘못되었습니다.");
			return;
		}

		init(args[0], args[1]); // get input variables and Initial
		Socket client = null;

		// printMap();
		sendToNeighborMethod();
		while (true) {
			try {
				client = routerSocket.accept(); // 접속 대기..neighbor에서 오는 tcp
												// connection
				// FromNeighbor clientHandler = new FromNeighbor(client);
				// Thread fromNeighbor = new Thread(clientHandler);
				// fromNeighbor.start();
				handle(client);
			} catch (IOException e) {
				System.out.println("Error reading request from client: " + e);

				continue;
			}
		}

	}

	private static void init(String fileName, String router) {
		// 5개로 한정되어있다라고 보고 programming 가능?
		// Map<String, Integer> neighborPort = new Hashtable<String, Integer>();
		Map<String, Integer> neighborList = new Hashtable<String, Integer>();
		int lineIndex = 0;
		String neighborName = "";
		int neighborIndex = 0;
		int distance = 0;
		try {
			initCostMap();
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String str;
			// 알파벳으로하지 않고 conf 파일의 순서대로 해도되는지.
			while ((str = in.readLine()) != null) {
				System.out.println(str);
				String[] data = str.split("\\s+");
				if (data.length > 0) {
					neighborPort.put(data[0], Integer.parseInt(data[1]));

					if (data[0].equals(router)) {
						port = Integer.parseInt(data[1]);
						neighborIndex = 0;
						neighborName = "";
						distance = 0;
						for (int i = 2; i < data.length; i++) {
							// neighborList.add(data[i]);
							if (i % 2 == 0) {
								neighborIndex = data[i].charAt(0) - ATOI_NUM;
								neighborName = data[i];
							} else {
								distance = Integer.parseInt(data[i]);
								distanceVector[lineIndex][neighborIndex] = distance;
								/* data[0].charAt(0) - ATOI_NUM */

								neighborList.put(neighborName, distance);
								neighborIndex = 0;
								distance = 0;
								neighborName = "";
							}
						}
					}// 실행시키는 라우터 이름에 대해서만 실행.
				}
				lineIndex++;
			}
			in.close();
			makeNeighbors(neighborList);
			openSocket();// server socket open 한다.
			// sendToNeighborMethod();

		} catch (IOException e) {
			System.out.println("read config file error : " + e);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Config 파일이 잘못되었습니다.");
		} catch (NumberFormatException e) {
			System.out.println("failed to Change alpha to int : " + e);
		}
	}

	private static void sendToNeighborMethod() {

		for (int i = 0; i < neighbors.size(); i++) {
			// neighbor 갯수만큼 thread 를 생성
			SendToNeighbor send = new SendToNeighbor(neighbors.get(i));
			Thread sendToThread = new Thread(send);
			sendToThread.start();
		}
	}

	private static void handle(Socket fromClient) {
		try {
			ObjectInputStream input = new ObjectInputStream(
					fromClient.getInputStream());
			int[][] vectorRecieved = (int[][]) input.readObject();
			calculateVector(vectorRecieved);
			// 여기서 최저 vector 에 대한 계산을 다시하고 계산전 결과와 차이가 있으면 neighbor 에게 다시 전
			input.close();
			printMap(vectorRecieved);
		} catch (Exception e) {
			System.out.println("Failed to get Message from Neighbor : " + e);
		}

	}

	private static void calculateVector(int [][] vector) {
		boolean isChanged = false ;
		
	};

	private static void makeNeighbors(Map<String, Integer> neighborList) {
		Neighbor tempNeighbor;

		for (Map.Entry<String, Integer> sigleNeighbor : neighborList.entrySet()) {
			for (Map.Entry<String, Integer> portMap : neighborPort.entrySet()) {
				if (portMap.getKey().equals(sigleNeighbor.getKey())) {
					tempNeighbor = new Neighbor(sigleNeighbor.getKey(),
							portMap.getValue(), sigleNeighbor.getValue());
					neighbors.add(tempNeighbor);
				}// 이 router 의 neighbor 와 portMap 의 이름이 같은경우.
			}
		}

	}

	private static void openSocket() {
		try {
			routerSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Error creating socket: " + e);
			System.exit(-1);
		}
	}

	private static void printMap(int[][] vector) {
		for (int i = 0; i < NODE_COUNT; i++) {
			for (int j = 0; j < NODE_COUNT; j++)
				System.out.print(vector[i][j] + "\t");
			System.out.println("");
		}

	}

	private static void initCostMap() {
		for (int i = 0; i < NODE_COUNT; i++) {
			for (int j = 0; j < NODE_COUNT; j++) {
				if (i == j)
					distanceVector[i][j] = 0;
				else
					distanceVector[i][j] = INFINITE;
			}
		}
	}

}
