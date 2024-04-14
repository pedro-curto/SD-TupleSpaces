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

### Installation and running

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

After running the NamingServer, run the ServerR1:

```s
cd ServerR1
mvn compile exec:java
```

Finally, to run the Client:

```s
cd Client
mvn compile exec:java
```

Then, start running commands as you please! Example:

```s
> put <a>
> put <b>
> read <[ab]>
> read <b>
> getTupleSpacesState A
> take <b>
...
```

### Testing (optional)

To ensure that everything is working as expected, run the tests located on the tests directory. 

Start the NamingServer and then the ServerR1 as described previously, and then run the tests:

```s
cd tests
./run_tests.sh
```

All tests should pass with TEST PASSED.

### Quick notes on the client and server arguments

By default, the server arguments are set to "localhost 2001 A TupleSpaces".

To output debug information, you can run the server with the optional -debug flag, either by uncommenting it on the pom.xml file or by using the command below.

Similarly, you can run the server with different arguments by changing them on the pom.xml file or by running either of the commands (with or without the debug flag):

```s
mvn compile exec:java -Dexec.args="<host> <port> <qualifier> <service>"
mvn compile exec:java -Dexec.args="<host> <port> <qualifier> <service> -debug"
```

On the client side, the arguments are set to "localhost 5001 TupleSpaces" by default. 

The debug flag is commented on the pom.xml to pass the tests, but it can be uncommented. 

You can change these arguments on the pom.xml file or by running either of the commands (with or without the debug flag):

```s
mvn compile exec:java -Dexec.args="<host> <port> <service>"
mvn compile exec:java -Dexec.args="<host> <port> <service> -debug"
```

Finally, in a similar way for the NameServer, you can run it with a debug flag to output debug information:

```s
python server.py -debug
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
