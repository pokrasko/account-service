# account-service

This is an account service for [this test](https://docs.google.com/document/d/1079ot0t5EyihwMhlil0jkMMc2tl9kjoVXC78qKIwDbc/edit).

The service uses a MySQL database and RMI transport protocol. Exceptions (RemoteException, SQLException, etc.) are not thrown, but caught and forwarded to the output. Both the server and the client can only be finished by sending an interruption signal.

A server runs the service and receives commands from keyboard to write out stats (the number of running requests or the total number of started requests) or to reset stats. The arguments of the server are a port to run RMI on, a MySQL database name and (optionally) a port to run MySQL on (3306 is default).

A client takes as the arguments an address where the server RMI is running on, the number of threads calling getAmount() and addAmount(), the second argument of every addAmount() call (a value which is added every time) and the list of changing elements' ids.
