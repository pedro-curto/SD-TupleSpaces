# TupleSpaces

Distributed Systems Project 2024

## Getting Started

The overall system is made up of several modules. The different types of servers are located in _ServerX_ (where X denotes stage 1, 2 or 3). 
The clients is in _Client_.
The definition of messages and services is in _Contract_. The future naming server
is in _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/TupleSpaces) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Disclaimer:
In order for the program to work properly, you should have three servers running 
and with different qualifiers (one A, one B and one C), as explained below.

### Detailed installation and running

All cd's (change directory) are relative to the root of the project.

Start by compiling and installing all modules (run this from the root directory of the project):

```s
mvn clean install
```

Then, compile and install the contract module:
    
```s
cd Contract
mvn compile exec:exec
```
Then, run the NamingServer (written in Python):

```s
cd NamingServer
python server.py
```

After running the NamingServer, run three instances of ServerR2 with distinct qualifiers (one A, one B and one C). Example:

```s
cd ServerR2
mvn compile exec:java -Dexec.args="localhost 2001 A TupleSpaces"
mvn compile exec:java -Dexec.args="localhost 2002 B TupleSpaces"
mvn compile exec:java -Dexec.args="localhost 2003 C TupleSpaces"
```

Finally, to run the Client, for example:

```s
cd Client
mvn compile exec:java -Dexec.args="TupleSpaces 1"
```

### An example

Launch two clients. Client 1:
```s
cd Client
mvn compile exec:java -Dexec.args="TupleSpaces 1"
```

And Client 2:
```s
cd Client
mvn compile exec:java -Dexec.args="TupleSpaces 2"
```

In Client 1:

```s
> put <a>
> put <b>
> put <c>
> getTupleSpacesState A // Should get [<a>, <b>, <c>]
> setdelay A 7
> setdelay B 7
> take <a>
```

Immediately after the `take <a>` from Client 1, in Client 2:

```s
> take <[abc]>
```

The expected outcome is the client 2 concluding the take first, because it can acquire the majority of the locks;
client 1 will release its minority, and eventually client 2 succeeds.
If the take client 2 executes isn't of the tuple \<a>, client 1 will also conclude the take succesfully in sucession.

### Testing (optional)

To ensure that everything is working as expected, run the tests located on the tests directory. 

Start the NamingServer and then the ServerR1 as described previously, and then run the tests:

```s
cd tests
./run_tests.sh
```

All tests should pass with TEST PASSED.

### Notes on the client and server arguments

By default, the server arguments are set to "localhost 2001 A TupleSpaces".

To output debug information, you can run the server with the optional -debug flag, either by uncommenting it on the pom.xml file or by using the command below.

Similarly, you can run the server with different arguments by changing them on the pom.xml file or by running either of the commands (with or without the debug flag):

```s
mvn compile exec:java -Dexec.args="<host> <port> <qualifier> <service>"
mvn compile exec:java -Dexec.args="<host> <port> <qualifier> <service> -debug"
```

On the client side, the arguments are set to "TupleSpaces 1" by default. 

The debug flag is commented on the pom.xml to pass the tests, but it can be uncommented. 

You can change these arguments on the pom.xml file or by running either of the commands (with or without the debug flag):

```s
mvn compile exec:java -Dexec.args="<service> <clientId>"
mvn compile exec:java -Dexec.args="<service> <clientId> -debug"
```

Finally, in a similar way for the NameServer, you can run it with a debug flag to output debug information:

```s
python server.py -debug
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
