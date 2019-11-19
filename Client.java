/**
Marcus Deng
mwd160230
CS 6378.001

This class defines the Client. It will create a quorum recursively, and ask the
quorum for permission to enter its critical section. Upon gaining all the
permissions needed, it will send a request to the file server to write to the
file; after a confirmation of success, it will then send a release message to
the quorum. This repeats 20 times.

This algorithm provided in the project specification may deadlock.

Usage: java Client <id> (<minDelay1> <maxDelay1> <minDelay2> <maxDelay2>)
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.Math;

public class Client {
  private String id;
  private Socket socket = null;
  private ObjectInputStream in = null;
  private ObjectOutputStream out = null;

  private boolean valid = false;  //test validity of connection

  //constants defined for the program
  public static final int NUMCLIENTS = 5;        //number of clients
	public static final int NUMSERVERS = 7;				//number of servers
	public static final int SERVERPORT = 3000;			//base server port; +0 = file server
	public static final int NUMREQ = 20;						//make 20 requests

	public static final String FILESERVER_IP = "dc01.utdallas.edu";
  public static final String SERVER_BASE_IP = "dc%02d.utdallas.edu";

  //statistics
  public static int sentMsg = 0;
  public static int recvMsg = 0;

  public Client(int id, String address, int port) throws IOException, ClassNotFoundException {
    this.id = "client" + id;
    // establish a connection to server
    try {
      socket = new Socket(address, port);

      out = new ObjectOutputStream(socket.getOutputStream());
      in = new ObjectInputStream(socket.getInputStream());
    }
    catch (Exception e) {
			System.out.println("failed to setup connection to server");
			return;
		}

    //send a message to the server, identifying self as a client
    out.writeObject(new Message(Message.CLIENT_MSG, this.id));
    sentMsg ++;

    //get the test message from server
    String recv = ((Message) in.readObject()).getCmd();
    recvMsg ++;
    if (!recv.equals(Message.TEST_MSG)) {
      System.out.println("bad connection; quitting");
      return;
    }
    System.out.println("Connected to " + address + ":" + port);
    this.valid = true;
  }

  //cleanup connection with server
  public void close() throws IOException {
    System.out.println(this.id + " closing");
    if (in != null) {
      in.close();
    }
    if (out != null) {
      out.close();
    }
    if (socket != null) {
      socket.close();
    }
    this.valid = false;
  }

  //returns if the connection is valid
  public boolean isValid() {
    return this.valid;
  }

  //sends a request message
	public Message request() throws IOException, ClassNotFoundException {
		if (!this.valid) {
			return null;
		}
		out.writeObject(new Message(Message.REQUEST_MSG, id));
    sentMsg ++;
		Message recv = (Message) in.readObject();
    recvMsg ++;
		return recv;
	}

  //sends a release message
  public Message release() throws IOException, ClassNotFoundException {
    if (!this.valid) {
      return null;
    }
    out.writeObject(new Message(Message.RELEASE_MSG, id));
    sentMsg ++;
    Message recv = (Message) in.readObject();
    recvMsg ++;
    return recv;
  }

  //sends an exit message
  private void exit() throws IOException, ClassNotFoundException {
    if (!this.valid) {
      return;
    }
    System.out.println("sending exit message");
    out.writeObject(new Message(Message.EXIT_MSG, id));
    sentMsg ++;
  }

  //recursively picks a quorum
	private static ArrayList<Integer> pickQuorum(int tree[], int i) {
		ArrayList<Integer> ret = new ArrayList<>();
		if (2*i >= tree.length) {
			ret.add(tree[i]);
			return ret;
		}
		Random r = new Random();
		int qChoice = r.nextInt(3);
		switch (qChoice) {
			case 0:
				//pick root and left subtree
				ret.add(tree[i]);
				ret.addAll(pickQuorum(tree, 2*i));
				break;
			case 1:
        //pick root and right subtree
				ret.add(tree[i]);
				ret.addAll(pickQuorum(tree, 2*i + 1));
				break;
			case 2:
        //pick left and right subtrees
				ret.addAll(pickQuorum(tree, 2*i));
				ret.addAll(pickQuorum(tree, 2*i + 1));
				break;
		}
		return ret;
	}

  //sleep for min/max milliseconds
  private static void sleep(int min, int max) {
    Random sleepDelay = new Random();
    try {
      Thread.sleep(sleepDelay.nextInt(max - min) + min);
    }
    catch (Exception e) {}
  }

  public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException {
    if (args.length < 1) {
      System.out.println("please provide client id");
      return;
    }

    final int id;
    try {
			id = Integer.parseInt(args[0]);
			if (id < 1 || id > NUMCLIENTS) {
				throw new IOException("invalid id");
			}
		}
		catch (Exception e) {
			System.out.println("please provide a valid client id 1-5");
			return;
		}

    //default sleep durations
    int minDelay1 = 2000;
    int maxDelay1 = 5000;
    int minDelay2 = 1000;
    int maxDelay2 = 3000;

    //user selected sleep durations
    for (int x = 1; x < args.length; x ++) {
      int t;
      try {
        t = Integer.parseInt(args[x]);
        if (t < 0) {
          throw new IOException("invalid time");
        }
      }
      catch (Exception e) {
        continue;
      }
      switch (x) {
        case 1:
          minDelay1 = t;
          break;
        case 2:
          maxDelay1 = t;
          break;
        case 3:
          minDelay2 = t;
          break;
        case 4:
          maxDelay2 = t;
          break;
      }
    }
    int TIMEOUT = Math.max(maxDelay1, maxDelay2) * 3; //timeout after three times the maximum delay
    if (TIMEOUT < 10 * 1000) {
      TIMEOUT = 10 * 1000;  //minimum timeout is 10 seconds
    }
    System.out.println("TIMEOUT: " + TIMEOUT + " ms");

		Client fileServerConn = new Client(id, FILESERVER_IP, SERVERPORT);
		if (!fileServerConn.isValid()) {
			System.out.println("failed to establish connection to file server");
			return;
		}

		//initialize the quorum server list
		int serverList[] = new int[NUMSERVERS + 1];
		for (int x = 1; x < serverList.length; x ++) {
			serverList[x] = x;
		}

    boolean status = true;  //keep track if deadlocked
    ArrayList<Long> latencyHistory = new ArrayList<>();
		for (int x = 0; x < NUMREQ; x ++) { //loop for 20 times (make 20 requests)
      //keep track of statistics between critical sections
      int localMsgSent = 0;
      int localMsgRecv = 0;
      long startTime;
      long latency;
			//sleep 2 to 5 seconds
			sleep(minDelay1, maxDelay1);
			//pick quorum
			ArrayList<Integer> quorum = Client.pickQuorum(serverList, 1);
			System.out.println(quorum);
			//for each server in quorum, start a thread to request
      startTime = System.currentTimeMillis();
      ExecutorService ex = Executors.newSingleThreadExecutor();
      try {
        ArrayList<ClientRequest> requestList = new ArrayList<>();
        ArrayList<Future<Message>> requestResults = new ArrayList<>();
        for (Integer s : quorum) {
          ClientRequest cr = new ClientRequest(id, s);
          requestList.add(cr);
          requestResults.add(ex.submit(cr));  //submit all requests
        }
        //attempt to get requests; if timeout then quit
        for (int a = 0; a < requestResults.size(); a ++) {
          Future<Message> r = requestResults.get(a);
          localMsgSent ++;
          try {
            Message recv = r.get(TIMEOUT, TimeUnit.MILLISECONDS);
            localMsgRecv ++;
          }
          catch (Exception e) {
            System.out.println("DEADLOCKED at server " + quorum.get(a));
            for (int b = 0; b < requestResults.size(); b ++) {  //close all connections for this request
              requestList.get(b).close();
              requestResults.get(b).cancel(true);
            }
            status = false;
            break;
          }
        }
      }
      finally {
        ex.shutdownNow();
      }

      if (!status) {
        System.out.println("shutting down");
        break;
      }

      System.out.println("quorum granted request");
      latency = System.currentTimeMillis() - startTime;
      latencyHistory.add(latency);
			//send request to file server
      fileServerConn.request();
      localMsgRecv ++;
      localMsgSent ++;
      System.out.println("file server ok");
      //sleep 1 to 3 seconds
      sleep(minDelay2, maxDelay2);
			//after response from file server, send release
      ArrayList<Thread> serverRequest = new ArrayList<>();
      serverRequest.clear();
      for (Integer s : quorum) {
        Thread t = new Thread() {
          @Override
          public void run() {
            try {
              Client q = new Client(id, String.format(SERVER_BASE_IP, s + 1), SERVERPORT + s);
              q.release();
              q.close();
            } catch (Exception e) {}
          }
        };
        t.start();
        serverRequest.add(t);
      }
      for (Thread t : serverRequest) {
        t.join();
        localMsgRecv ++;
        localMsgSent ++;
      }
      System.out.println("sent releases");
      System.out.println("*********************************************");
      System.out.println("number of local messages sent: " + localMsgSent);
      System.out.println("number of local messages received: " + localMsgRecv);
      System.out.println("latency: " + latency);
      System.out.println();
		}
		//send completion message to file server, and exit
    fileServerConn.exit();
    fileServerConn.close();

    //calculate latency statistics
    long minLat = Long.MAX_VALUE;
    long maxLat = Long.MIN_VALUE;
    double sumLat = 0;
    for (Long l : latencyHistory) {
      if (l < minLat) {
        minLat = l;
      }
      if (l > maxLat) {
        maxLat = l;
      }
      sumLat += l;
    }

    //print summary statistics
    System.out.println();
    System.out.println("***********************************************");
    System.out.println("Summary statistics:");
    System.out.println("Deadlocked: " + !status);
    System.out.println("Max latency: " + maxLat);
    System.out.println("Min latency: " + minLat);
    System.out.println("Average latency: " + sumLat / latencyHistory.size());
    System.out.println("Number of messages sent: " + Client.sentMsg);
    System.out.println("Number of messages received: " + Client.recvMsg);
    System.exit(0);
  }
}
