/**
Marcus Deng
mwd160230
CS 6378.001

This class defines a thread for the Server to communicate with the file server.
Upon receiving an exit message from the file server, this will cause the main
server thread to stop.
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerListener extends Thread {
  private String id;
  private Socket s;
  private ObjectInputStream in;
  private ObjectOutputStream out;
  private volatile boolean done;
  private ServerSocket server;

  public ServerListener(int id, String address, int port, boolean done, ServerSocket server) throws IOException, ClassNotFoundException {
    this.id = "server" + id;
    s = new Socket(address, port);
    this.out = new ObjectOutputStream(s.getOutputStream());
		this.in = new ObjectInputStream(s.getInputStream());
    this.done = done;
    this.server = server;
    //test connection
    out.writeObject(new Message(Message.SERVER_MSG, this.id));  //identify self as a server
    Message recv = (Message) in.readObject();
    if (!recv.getCmd().equals(Message.TEST_MSG)) {
      this.done = true;
    }
    System.out.println("connected to file server");
  }

  //block and read from the file server until an exit message arrives
  @Override
  public void run() {
    try {
      Message recv = (Message) in.readObject();
      System.out.println("got message from file server: " + recv);
      System.out.println();
      if (recv.getCmd().equals(Message.EXIT_MSG)) {
        done = false;
        close();
      }
    } catch (Exception e) {}
  }

  //cleanup
  public void close() {
    try {
      in.close();
      out.close();
      s.close();
      server.close();
    }
    catch (Exception e) {}
  }
}
