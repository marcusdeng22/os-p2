/**
Marcus Deng
mwd160230
CS 6378.001

This class is for defining a comparator for a DelayedMessage. It will compare
the timestamps and IDs of the DelayedMessages; if the timestamps are equal then
the ID is used. Earliest timestamp is given priority; for ID it follows String
comparison rules.
**/

import java.util.*;
import java.lang.*;

public class MessageComparator implements Comparator<DelayedMessage> {
	public int compare(DelayedMessage m1, DelayedMessage m2) {
		if (m1.getMessage().getTime() - m2.getMessage().getTime() == 0) {
			return m1.getMessage().getId().compareTo(m2.getMessage().getId());
		}
		return ((Long) m1.getMessage().getTime()).intValue() - ((Long) m2.getMessage().getTime()).intValue();
	}
}
