import java.net.*;
import java.util.*;
import java.io.*;

import javax.swing.JTable.PrintMode;

public class DVImpl {
	private static final int INFINITE = 999;
	protected static final int 	READ_MAP_INTERVAL_IN_MILLIS = 10000;
	protected static final int NODE_COUNT = 5;
	private static final int ATOI_NUM = 65;
	private static ServerSocket routerSocket;
	private static int port = 0;
	
	private static Map<String, Integer> portInformation = new Hashtable<String, Integer>();
	private static List<String> names = new ArrayList<String>();
	
	private static List<Neighbor> neighbors = new ArrayList<Neighbor>();
	protected static int[][] forwardingTable = new int[NODE_COUNT][NODE_COUNT];
	
	protected static Map<String, Integer> myVector = new Hashtable<String, Integer>();
	protected static String myName = "";
	public static void main(String args[]) {
		
		if (args.length != 2) {
			System.out.println("input parameter가 잘못되었습니다.");
			return;
		}

		init(args[0], args[1]); // get input variables and Initial
		Socket client = null;

		// printMap();
		
		ReadNetMap read = new ReadNetMap(); // class 내부에서 my own vector 을 참조하여 전송한다. 
		Thread readMap = new Thread(read);
		readMap.start();
		
		
		sendToNeighborMethod(); //send my vectors to neighbours
		while (true) {
			try {
				client = routerSocket.accept(); // 접속 대기..neighbor에서 오는 tcp
				
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

	private static void handle(Socket fromClient) {
		try {
			ObjectInputStream input = new ObjectInputStream(
					fromClient.getInputStream());

			VectorOnWire vectorRecieved = (VectorOnWire) input.readObject();			
			printReceivedVector(vectorRecieved);
			calculateVector(vectorRecieved);
			// 여기서 최저 vector 에 대한 계산을 다시하고 계산전 결과와 차이가 있으면 neighbor 에게 다시 전
			input.close();
			
		} catch (Exception e) {
			System.out.println("Failed to get Message from Neighbor : " + e);
		}

	}// when Vector arrived from neighbours

	private static void printReceivedVector(VectorOnWire vectorRecieved){
		System.out.println("Vector From " + vectorRecieved._source + " : "
				+ vectorRecieved._vectors.toString());
	}//confirm vector sended
	
	
	private static void init(String fileName, String router) {
		// 5개로 한정되어있다라고 보고 programming 가능?

		
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
					portInformation.put(data[0], Integer.parseInt(data[1]));//save Port Information from conf file
					names.add(data[0]);
					
					if (data[0].equals(router)) {
						myName = router;	//set own routers name
						port = Integer.parseInt(data[1]); //set own routers port
						
						neighborIndex = 0;
						neighborName = "";
						distance = 0;
						for (int i = 2; i < data.length; i++) {
							
							if (i % 2 == 0) {
								neighborIndex = data[i].charAt(0) - ATOI_NUM;
								neighborName = data[i];
							} else {
								distance = Integer.parseInt(data[i]);
								forwardingTable[lineIndex][neighborIndex] = distance;
								
								myVector.put(neighborName, distance);
								//myVector.put(neighborName, distance);
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
			
			for (String name : names) {
				if(name.equals(myName)){
					myVector.put(myName, 0); //set myself as 0
				}
				else if (!myVector.containsKey(name)){
					myVector.put(name, INFINITE);
				}
			}// initialize MyVector
			myVector = new TreeMap<String, Integer>(myVector); //to sort Map by Alpha Order
			makeNeighbors(neighborList);
			openSocket();// server socket open 한다.

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
			SendToNeighbor send = new SendToNeighbor(neighbors.get(i)); // class 내부에서 my own vector 을 참조하여 전송한다. 
			Thread sendToThread = new Thread(send);
			sendToThread.start();
		}
	}


	private synchronized static void calculateVector(VectorOnWire vector) {
		boolean isChanged = false ;
		String vectorFrom="";
		//int currentMinCost = 0;
		int costToSource = 0; 
		int newCost = 0;
		vectorFrom = vector._source;
		costToSource = myVector.get(vectorFrom); // 여기를 myVector 가 아니라 연결된 직접 연결된 cost 로 한다면?
		
		for(Neighbor neigh: neighbors)
		{
			if(neigh.routerName.equals(vectorFrom))
				costToSource = neigh.weight;
		}
		
		for (Map.Entry<String, Integer> singleVector : vector._vectors.entrySet()){	
			forwardingTable[vectorFrom.toCharArray()[0] - ATOI_NUM][singleVector.getKey().charAt(0)- ATOI_NUM] = singleVector.getValue();
		}
		
		for (Map.Entry<String, Integer> mySingleVector : myVector.entrySet()){
			
			if(mySingleVector.getKey().equals(myName)){
				mySingleVector.setValue(0);//0으로 한번더 입력. 
			}//자기 자신에 대해서는 항상 0 으로 유지.
			else if(mySingleVector.getKey().equals(vectorFrom)){
			}// vector 을 보내온 node로부터 거리 역시 수정할 필요가 없음.
			else{			
				if( (newCost= costToSource + vector._vectors.get(mySingleVector.getKey())) < mySingleVector.getValue()){
					mySingleVector.setValue(newCost);
					isChanged = true;
					System.out.println("value changed");
				}//새로 받아온 vector 을 거쳐서 가는 것이 현재 저장되어있는 cost 보다 낮은 경우 무한대이면 어짜피 무한대로 유지.
			}
			forwardingTable[myName.charAt(0) - ATOI_NUM][mySingleVector.getKey().charAt(0) - ATOI_NUM] = mySingleVector.getValue();
		}// 나의 vector 들에 대해서
		
			
		if(isChanged){
			sendToNeighborMethod();
		}
			
		/*for (Map.Entry<String, Integer> singleVector : vector._vectors.entrySet()){
			eachNode = singleVector.getKey();
			
			forwardingTable[vectorFrom.toCharArray()[0] - ATOI_NUM][eachNode.charAt(0)- ATOI_NUM] = singleVector.getValue();
			//forwarding table 의 해당 neighbor 에서 온 값을 넣어준다. 
			
			if(vectorFrom.equals(myName)){
				 //neighbor에서 나에게 오는 cost 에 대해서는 무시한다. 
			}
			else{
				for (Map.Entry<String, Integer> mySingleVector : myVector.entrySet()){
					currentMinCost = mySingleVector.getValue();
					
					
					if(eachNode.equals(mySingleVector.getKey())){
						
						if(mySingleVector.getValue() == INFINITE){
							if(!eachNode.equals(vectorFrom))
								mySingleVector.setValue(singleVector.getValue());
							//현재 myVector 에 저장된 값이 INIFINITE 이라면 어떤 값이라도 교환가능.
						}
					}
				}
			}
		}*/
		System.out.println("my Vector After calc : " + myVector.toString());
		System.out.println("after message From neighbor " + vector._source);
		printMap(forwardingTable);
		//수정 완료하고 forwarding table을 새로 갱신 해준다. 
	};

	private static void makeNeighbors(Map<String, Integer> neighborList) {
		Neighbor tempNeighbor;

		for (Map.Entry<String, Integer> sigleNeighbor : neighborList.entrySet()) {
			for (Map.Entry<String, Integer> portMap : portInformation.entrySet()) {
				if (portMap.getKey().equals(sigleNeighbor.getKey())) {
					tempNeighbor = new Neighbor(sigleNeighbor.getKey(),
							portMap.getValue(), sigleNeighbor.getValue());
					neighbors.add(tempNeighbor);
				}// 이 router 의 neighbor 와 portMap 의 이름이 같은경우. 이웃의 포트번호를 가지고 있는 Map 에서 My Node 의 neighbor 을 찾아 그 포값을 저장.
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
	}//open Server Socket

	private static void printMap(int[][] vector) {
		for (int i = 0; i < NODE_COUNT; i++) {
			for (int j = 0; j < NODE_COUNT; j++)
				System.out.print(vector[i][j] + "\t");
			System.out.println("");
		}

	}// Print My Map

	private static void initCostMap() {
		for (int i = 0; i < NODE_COUNT; i++) {
			for (int j = 0; j < NODE_COUNT; j++) {
				if (i == j)
					forwardingTable[i][j] = 0;
				else
					forwardingTable[i][j] = INFINITE;
			}
		}
	}//Init cost Map with infinite and zeros

}
