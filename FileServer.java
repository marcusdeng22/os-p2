/**
Marcus Deng
mwd160230
CS 6378.001

This class defines the FileServer process. It will append to a file the ID and
timestamp of a request; this is the "critical section" of the algorithm. It also
starts the termination of all quorum servers.

The algorithm provided in the project specification may deadlock.

Usage: java FileServer
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.file.*;

public class FileServer {
	public static final int BASEPORT = 3000;
	public static final int NUMCLIENTS = 5;
	public static final int NUMSERVERS = 7;
	public static volatile AtomicInteger finishCount = new AtomicInteger();	//counts the number of exit requests

	//statistics
	public static volatile AtomicInteger sentMsg = new AtomicInteger();
	public static volatile AtomicInteger recvMsg = new AtomicInteger();

	//main file server class, starts a thread per connection
	public FileServer(int port) throws IOException {
		ServerSocket server = new ServerSocket(port);
		System.out.println("File server started on port " + port);
		// started server and waits for a connection
		ArrayList<Thread> threadList = new ArrayList<>();
		try {
			// finishCount = 0;
			int numConn = 0;
			while (numConn < NUMCLIENTS + NUMSERVERS) {	//after we are connected to all clients, wait until all exit
				Socket socket = null;
				try {
					socket = server.accept();
					System.out.println("New client connected: " + socket);
					System.out.println("finishCount = " + finishCount);
					//start a new thread for a new client
					// FileClientHandler t = new FileClientHandler(socket, finishCount);
					FileClientHandler t = new FileClientHandler(socket);
					t.start();
					threadList.add(t);
					numConn ++;
				}
				catch (Exception e) {
					//unknown exception, so close the clients
					socket.close();
					e.printStackTrace();
					return;
				}
			}
			//we now have everyone connected, so wait for client exit messages
			System.out.println("all connected to file server");
			for (Thread t : threadList) {
				t.join();
			}
			System.out.println("done");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			server.close();
		}
	}

	public static void main(String args[]) throws IOException {
		FileServer server = new FileServer(BASEPORT);
		//print summary statistics
    System.out.println();
    System.out.println("***********************************************");
    System.out.println("Number of messages sent: " + FileServer.sentMsg.get());
    System.out.println("Number of messages received: " + FileServer.recvMsg.get());
	}
}

//thread that handles communication with a client or server
class FileClientHandler extends Thread {
	private String id;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Socket s;

	//define the communication streams
	public FileClientHandler(Socket s) throws IOException {
		this.id = "fileServer";
		this.s = s;
		this.out = new ObjectOutputStream(s.getOutputStream());
		this.in = new ObjectInputStream(s.getInputStream());
	}

	//write to file if request received, or send exit messages if exit received
	@Override
	public void run() {
		Message recv;
		try {
			//test connection
			Message idMsg = (Message) in.readObject();
			FileServer.recvMsg.getAndIncrement();
			if (idMsg.getCmd().equals(Message.CLIENT_MSG)) {	//logic for a client
				System.out.println("file server hello to client test");
				out.writeObject(new Message(Message.TEST_MSG, id));
				FileServer.sentMsg.getAndIncrement();
				//wait for request
				while (true) {
					try {
						recv = (Message) in.readObject();
						FileServer.recvMsg.getAndIncrement();
						System.out.println("recv: " + recv);
						String flag = recv.getCmd();
						String reqId = recv.getId();
						boolean err = false;
						switch (flag) {
							//REQUEST is a request message from the client, saying that it has been granted permission to write
							case Message.REQUEST_MSG:
								System.out.println("received request msg from " + reqId);
								System.out.println();
								//open file and write
								synchronized (this) {
									PrintWriter writer = new PrintWriter(new FileWriter("file.txt", true));
									writer.println("request from " + reqId + " " + recv.getTime());
									writer.close();
								}
								//send response notifying write complete
								out.writeObject(new Message(Message.DONE_MSG, id));
								FileServer.sentMsg.getAndIncrement();
								break;
							case Message.EXIT_MSG:
								System.out.println("received exit msg from " + reqId);
								//client is done, so stop recording and exit
								synchronized (this) {
									FileServer.finishCount.getAndIncrement();
									System.out.println("finishCount=" + FileServer.finishCount);
									System.out.println();
									return;
								}
						}
					}
					catch (SocketException e) {
						System.out.println("remote unreachable, closing self");
						break;
					}
					catch (EOFException e) {
						System.out.println("remote closed, closing self");
						break;
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else if (idMsg.getCmd().equals(Message.SERVER_MSG)) {	//logic for a server
				System.out.println("file server hello to server test");
				out.writeObject(new Message(Message.TEST_MSG, id));
				FileServer.sentMsg.getAndIncrement();
				//keep connection alive until all clients have submitted an exit message
				while (FileServer.finishCount.get() < FileServer.NUMCLIENTS) {
					try {
						Thread.sleep(500);
					}
					catch (Exception e) {}
				}
				System.out.println("closing all servers");
				out.writeObject(new Message(Message.EXIT_MSG, id));	//send exit message to server
				FileServer.sentMsg.getAndIncrement();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			System.out.println("closing");
			close();
		}
	}

	public void close() {
		try {
			in.close();
			out.close();
			s.close();
		}
		catch (Exception e) {}
	}
}
