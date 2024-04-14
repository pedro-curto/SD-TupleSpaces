package pt.ulisboa.tecnico.tuplespaces.server.domain;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of the server in a TupleSpace system.
 * The ServerState class manages the storage and retrieval of tuples,
 * as well as the locking and unlocking of tuples for concurrent access.
 * All methods are synchronized to handle concurrent access to the tuples.
 */
public class ServerState {
    private final List<Tuple> tuples;
    private final boolean debug;

    public ServerState(boolean debug) {
        this.debug = debug;
        this.tuples = new ArrayList<>();
    }

    public synchronized void put(String tuple) {
        Tuple newTuple = new Tuple(tuple);
        tuples.add(newTuple);
        notifyAll();
    }

    private synchronized String getMatchingTuple(String pattern) {
        // the requester returns the first matching tuple
        return tuples.stream()
                .map(Tuple::getFields)
                .filter(fields -> fields.matches(pattern))
                .findFirst()
                .orElse("");
    }

    public synchronized String read(String pattern) {
        String tuple = getMatchingTuple(pattern);
        while (tuple.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            tuple = getMatchingTuple(pattern);
        }
        return tuple;
    }

    public synchronized List<String> takePhase1(String pattern, int clientId) {
        List<String> reservedTuples = new ArrayList<>();
        if (debug) System.err.println("Now attempting to lock tuples");
        for (Tuple tuple : tuples) {
            if (tuple.getFields().matches(pattern)) {
                // this lock operation is idempotent, so the same client locking the same tuple
                // multiple times has the same effect as locking it once
                if (!tuple.lock(clientId)) {
                    if (debug) System.err.println("Not possible to lock tuple " + tuple.getFields() + " for client " + clientId);
                    throw new StatusRuntimeException(Status.UNAVAILABLE);
                }
                reservedTuples.add(tuple.getFields());
            }
        }
        if (debug) System.err.println("Reserved tuples: " + reservedTuples);
        return reservedTuples;
    }

    public synchronized void takePhase1Release(int clientId) {
        unlockTuples(clientId);
    }

    public synchronized void takePhase2(String tuple, int clientId) {
        tuples.stream()
                .filter(t -> t.getClientId() == clientId && t.getFields().equals(tuple))
                .findFirst()
                .ifPresent(t -> {
                    tuples.remove(t);
                    unlockTuples(clientId);
                });
    }

    public synchronized void unlockTuples(int clientId) {
        tuples.stream()
                .filter(t -> t.getClientId() == clientId)
                .forEach(t -> t.unlock(clientId));
    }

    public synchronized List<String> getTupleSpacesState() {
        return tuples.stream()
                .map(Tuple::getFields)
                .toList();
    }
}
