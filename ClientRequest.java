/**
Marcus Deng
mwd160230
CS 6378.001

This class is for sending a request message to a server. It allows for closing
of the connection if the request times out.
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ClientRequest implements Callable<Message> {
  private int offset;
  private int id;
  private Client q = null;
  private boolean closed = false;

  public ClientRequest(int id, int offset) {
    this.id = id;
    this.offset = offset;
  }

  public Message call() throws IOException, ClassNotFoundException {
    this.q = new Client(id, String.format(Client.SERVER_BASE_IP, offset + 1), Client.SERVERPORT + offset);
    Message recv = q.request();
    q.close();
    closed = true;
    return recv;
  }

  public void close() throws IOException {
    if (!closed && q != null) {
      System.out.println("request " + offset + " closing");
      q.close();
      closed = true;
    }
  }
}
