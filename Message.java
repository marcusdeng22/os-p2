/**
Marcus Deng
mwd160230
CS 6378.001

This class is for defining a message. It defines the message types, as well a
way to mark the timestamp of a message. Messages consist of the command, time,
and ID of the sender.
**/

import java.io.Serializable;
import java.lang.System;

public class Message implements Serializable {
  //command types
  public static final String TEST_MSG = "TEST";
  public static final String EXIT_MSG = "EXIT";
  public static final String DONE_MSG = "DONE";
	public static final String ERR_MSG = "ERROR";
  public static final String REQUEST_MSG = "REQUEST";
  public static final String GRANT_MSG = "GRANT";
	public static final String RELEASE_MSG = "RELEASE";
  public static final String CLIENT_MSG = "CLIENT";
  public static final String SERVER_MSG = "SERVER";

  //fields of a message
  private final String cmd;
  private final long timestamp;
  private final String id;

  public Message (String cmd, String id) {
    this.cmd = cmd;
    this.id = id;
    this.timestamp = System.currentTimeMillis() / 100;
  }

  public String getCmd() {
    return cmd;
  }

  public long getTime() {
    return timestamp;
  }

  public String getId() {
    return id;
  }

  public String toString() {
    return id + " " + cmd + " " + Long.toString(timestamp);
  }
}
