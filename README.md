## Writeup
This implements the algorithm specified in the project specification. It may
deadlock, as shown in the example below:

Let process A ask the quorum servers 1, 4, and 5 for permission, and process B
ask the quorum servers 1, 4, and 5 also. Process A makes the requests at the
following times: 1, 5, and 6. Process B makes the requests at the following
times: 2, 3, and 4. Now, server 1 has granted A permission, but servers 5 and 6
have granted B permission; we now have deadlock.

Deadlock is detected via timeout. The minimum timeout is 10 seconds, but if a
delay is specified then it takes the max delay and multiplies it by 3. If
finding a deadlock is uncommon, try increasing the constant factor in line 34
of `Message.java`.

Below are my test results for latency with the corresponding time delays:
```
0 0 0 0
max 5895
min 593
avg 2700

1000 3000 0 0
max 3366
min 430
avg 1200

2000 5000 0 0
max 1481
min 442
avg 700

0 0 1000 3000
max 9258
min 508
avg NA
deadlocked

2000 5000 1000 3000		(default)
max 15840
min 485
avg 6200

2000 5000 2000 5000
max 16972
min 2918
avg NA
deadlocked
```
It appears that increasing the delay between critical sections helps avoid
deadlock, but increasing the time in the critical section causes more deadlocks
to occur.

## Resetting
Run `python3 reset.py` to reset the `file.txt` to its original state.
This script will also remove all .class files, if you want to recompile.

## Compilation
Run:
```
javac Client.java
javac Server.java
javac FileServer.java
```
This will build all the required files.

## Running
First start the file server, then all the servers, and then the clients.

To run the file server, run `java FileServer`.
This should be on dc01.utdallas.edu

On each machine running a server, run `java Server <id; 1-7>`
where id is a number 1 to 7, inclusive.
This should be on dc02.utdallas.edu to dc08.utdallas.edu

For each machine running a client, run
`java Client <id; 1-5> (<minDelay1> <maxDelay1> <minDelay2> <maxDelay2>)`
where id is a number 1 to 5, inclusive. The delays are optional; they are by
default 3000, 5000, 1000, and 3000 respectively. Provide delays in milliseconds.
This can be run on any machine.

## Debugging
There are constants defined in Server.java and Client.java that may help you
debug.

BASEPORT, SERVERPORT, and PORT define the starting port ranges for each
machine. Each server/client uses a different port.

Changing NUMCLIENTS will reduce the number of clients in the system.
