/**
Marcus Deng
mwd160230
CS 6378.001

This class defines the Server process. It will grant permission to a client if
it has not already granted another client permission; if it is locked, the
server will put the request on a priority queue, which will be served once the
client sends a release.

This algorithm provided in the project specification may deadlock.

Usage: java Server <id; 1-5>
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Server {
	private static final String FILESERVER_IP = "dc01.utdallas.edu";
	private static final int BASEPORT = 3000;
	private volatile boolean done = false;

	//statistics
	public static volatile AtomicInteger sentMsg = new AtomicInteger();
	public static volatile AtomicInteger recvMsg = new AtomicInteger();

	//main server class, starts a thread per connection
	public Server(int id, int port) throws IOException, ClassNotFoundException {
		ServerSocket server = new ServerSocket(port);
		System.out.println("Server started on port " + port);
		//start a thread to connect to file server
		ServerListener fileServerListener = new ServerListener(id, FILESERVER_IP, BASEPORT, done, server);
		fileServerListener.start();
		//fileServerListener will send and receive one message
		sentMsg.getAndIncrement();
		recvMsg.getAndIncrement();

		//started server and waits for a connection
		try {
			while (!done) {
				Socket socket = null;
				try {
					socket = server.accept();
					System.out.println("New client connected: " + socket);
					//start a new thread for a new client
					Thread t = new ClientHandler(id, socket);
					t.start();
				}
				catch (Exception e) {
					break;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			server.close();
		}

		recvMsg.getAndIncrement();	//fileServerListener will have received exit message
		fileServerListener.close();

		//print summary statistics
    System.out.println();
    System.out.println("***********************************************");
    System.out.println("Number of messages sent: " + sentMsg.get());
    System.out.println("Number of messages received: " + recvMsg.get());
	}

	public static void main(String args[]) throws IOException, ClassNotFoundException {
		if (args.length != 1) {
			System.out.println("please provide server id");
			return;
		}
		int id = 1;
		int port = BASEPORT;
		try {
			id = Integer.parseInt(args[0]);
			port += id;
			if (id < 1 || id > 7) {
				throw new IOException("invalid id");
			}
		}
		catch (Exception e) {
			System.out.println("please provide a valid server id 1-7");
			return;
		}
		Server server = new Server(id, port);
	}
}

//thread that handles communication with a client
class ClientHandler extends Thread {
	private static volatile boolean locked = false;
	private static volatile PriorityBlockingQueue<DelayedMessage> requestQueue = new PriorityBlockingQueue<DelayedMessage>(20, new MessageComparator());
	private String id;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Socket s;

	//define the communication streams
	public ClientHandler(int id, Socket s) throws IOException {
		this.id = "server" + id;
		this.s = s;
		this.out = new ObjectOutputStream(s.getOutputStream());
		this.in = new ObjectInputStream(s.getInputStream());
	}

	//grants permission
	@Override
	public void run() {
		Message recv;
		try {
			//test connection
			in.readObject();	//get the id msg, but don't care who it is yet
			Server.recvMsg.getAndIncrement();
			System.out.println("server hello to client test");
			out.writeObject(new Message(Message.TEST_MSG, id));
			Server.sentMsg.getAndIncrement();
			//wait for request
			while (true) {
				try {
					recv = (Message) in.readObject();
					Server.recvMsg.getAndIncrement();
					System.out.println("recv: " + recv);
					String flag = recv.getCmd();
					String reqId = recv.getId();
					boolean err = false;
					switch (flag) {
						//REQUEST is a request message from the client
						case Message.REQUEST_MSG:
							System.out.println("received request msg from " + reqId);
							//check queue
							if (!locked) {
								//grant request
								System.out.println("granted: " + reqId);
								out.writeObject(new Message(Message.GRANT_MSG, id));
								Server.sentMsg.getAndIncrement();
								locked = true;
							}
							else {
								//add request to queue
								System.out.println("delayed: " + reqId);
								requestQueue.add(new DelayedMessage(recv, out));
								System.out.println("queue: " + requestQueue);
								System.out.println();
							}
							break;
						//RELEASE will allow the server to send the next available message on the queue
						case Message.RELEASE_MSG:
							//grant what is at the top of the queue
							System.out.println("recevied release msg from " + reqId);
							DelayedMessage delayedMsg = requestQueue.poll();
							if (delayedMsg != null) {
								System.out.println("releasing to: " + delayedMsg.getMessage().getId());
								System.out.println();
								delayedMsg.getOut().writeObject(new Message(Message.GRANT_MSG, delayedMsg.getMessage().getId()));
								Server.sentMsg.getAndIncrement();
							}
							else {
								System.out.println("nothing on queue");
								locked = false;
							}
							out.writeObject(new Message(Message.DONE_MSG, id));
							Server.sentMsg.getAndIncrement();
							return;
						case Message.EXIT_MSG:
							//file server is done, so stop recording and exit
							System.out.println("exiting");
							return;
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
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			//cleanup
			try {
				in.close();
				out.close();
				s.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
