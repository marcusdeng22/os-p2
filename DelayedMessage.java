/**
Marcus Deng
mwd160230
CS 6378.001

This class is for defining a DelayedMessage. A DelayedMessage contains one
Message and the corresponding output stream to send the Message with. Used in
the priority queue of a Server.
**/

import java.io.*;

class DelayedMessage {
	private Message m;
	private ObjectOutputStream out;

	public DelayedMessage(Message m, ObjectOutputStream out) {
		this.m = m;
		this.out = out;
	}

	public Message getMessage() {
		return m;
	}

	public ObjectOutputStream getOut() {
		return out;
	}

	public String toString() {
		return getMessage().toString();
	}
}
