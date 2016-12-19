# account-service

This is an account service for [this test](https://docs.google.com/document/d/1079ot0t5EyihwMhlil0jkMMc2tl9kjoVXC78qKIwDbc/edit).

The service uses a MySQL database and RMI transport protocol.
Exceptions (RemoteException, SQLException, etc.) are not thrown, but caught and forwarded to the output.

### Server
A server runs the account service and receives commands from keyboard to write out stats
(the number of running requests or the total number of started requests) or to reset stats.

The arguments of the server are:
- a port to run RMI on
- a MySQL database name
- (*optionally*) a port to run MySQL on (if not present, default port 3306 is used)

The server can only be finished by an interruption signal.

### Client
A client runs some number of threads which make get requests to the server and the same number of threads which make
add requests. All threads are running concurrently.

The client takes as the arguments:
- an address where the server RMI is running on
- the number of threads calling getAmount()
- the number of threads calling addAmount()
- a value which should be added to an account during every addAmount() request
- the list of elements' ids which should be changed
- (*optionally*) the number of requests every thread will make (if not present, threads will be making requests
until an interruption signal)