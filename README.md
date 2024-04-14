# SD-TupleSpaces

## Description
Welcome! This is a project that I made in a group of 3, for the Distributed Systems (SD @IST) class in the year academic year 2023/2024.

The objective was to create a system called TupleSpace, which implements a distributed tuple space service. 
TupleSpace allows users to add, read, and remove tuples from a shared space. 
The project explores architectures and algorithms for implementing the TupleSpace service:
* Centralized server (R1)
* Replicated, with Xu-Liskov's implementation of take, read and put (R2)
* State machine replication (R3)

## Features
The features are similar in every delivery; what changes is the way they are implemented or the availability of the service. They are:
- put: Adds a tuple to the shared space.
- read: Reads a tuple matching a given description (with the possibility of using regular expressions).
- take: Removes and returns a tuple matching a given description (with or without a regular expression).
- getTupleSpacesState: Retrieves all tuples present in the server.
And additional commands, for debug purposes:
- setdelay: Adds a delay (in seconds) to every request sent to a server.
- sleep: Blocks the client for a specified amount of seconds.

## Deliverables
### R1 Variant
This variant implements a simple client-server architecture with a single server accepting requests on a fixed address/port.
#### Implementation Steps
Phase 1: Implement server with fixed address/port.
Phase 2: Implement dynamic server discovery using a Python-based name server.

### R2 Variant
This variant replicates the TupleSpace service across three servers (A, B, and C) following the Xu and Liskov algorithm. 
Clients discover server addresses dynamically through the name server.
#### Feature changes
Here, put, read and take operations follow the Xu-Liskov algorithm.
Namely, the take operation has a very interesting approach with two phases, and we recommend anyone interested to read the 
"put-read.png" and "take.png" files in the respective directory for detailed information on the functioning of the algorithm.

### R3 Variant
This variant employs a state machine replication approach (RME) for TupleSpace service replication. 
It offers an alternative to the Xu and Liskov algorithm; it works in total order, where the unique sequence number is provided
by a Sequencer service, registered in the name server.

### How to Run
Instructions for running each variant can be found in their respective directories.

## Have Fun!
This project was very fun to make, since it delves into various distributed systems concepts and provides valuable insights into trade-offs insights into designing fault-tolerant and efficient distributed services. 
Feel free to experiment with different configurations and algorithms!

