package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

public class ServerState {

    private final List<String> tuples;
    private final boolean debug;
    private final List<TakeRequest> queue = new ArrayList<TakeRequest>();
    private int seqNumber;
    private int takeQueueNumber;


    public ServerState(boolean debug) {
        this.tuples = new ArrayList<String>();
        this.debug = debug;
        this.seqNumber = 1;
        this.takeQueueNumber = -1;
    }

    public void put(String tuple, int seqNumber) {
        waitForTurn(seqNumber);
        tuples.add(tuple);
        int index = checkAnyMatchInQueue(tuple);
        if (index != -1) {
            this.takeQueueNumber = queue.get(index).seqNumber;
            queue.get(index).notifyMe();
            queue.remove(index);
        } else {
            incrementSeqNumber();
        }
        if (debug) System.err.println("[ServerState] Tuple added: " + tuple);
    }

    private String getMatchingTuple(String pattern) {
        for (String tuple : this.tuples) {
            if (tuple.matches(pattern)) {
                return tuple;
            }
        }
        return "";
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

    public String take(String pattern, int seqNumber) {
        waitForTurn(seqNumber);
        String tuple = getMatchingTuple(pattern);
        if (tuple.isEmpty()) {
            addToQueue(seqNumber, pattern);
            afterWaitInQueue(pattern);
        } else {
            tuples.remove(tuple);
            incrementSeqNumber();
        }
        return tuple;
    }

    public List<String> getTupleSpacesState() {
        return this.tuples.stream().toList();
    }

    private void waitForTurn(int seqNumber) {
        while (this.seqNumber != seqNumber) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void incrementSeqNumber() {
        this.seqNumber++;
        synchronized (this) {
            notifyAll();
        }
    }

    private void addToQueue(int seqNumber, String pattern) {
        TakeRequest request = new TakeRequest(seqNumber, pattern);
        queue.add(request);
        incrementSeqNumber();
        request.waitInQueue();
    }

    private int checkAnyMatchInQueue(String tuple) {
        for (int i = 0; i < queue.size(); i++) {
            if (tuple.matches(queue.get(i).pattern)) {
                return i;
            }
        }
        return -1;
    }

    private void afterWaitInQueue(String pattern) {
        this.tuples.remove(getMatchingTuple(pattern));
        this.takeQueueNumber = -1;
        incrementSeqNumber();
    }

    private static class TakeRequest {
        public int seqNumber;
        public String pattern;

        public TakeRequest(int seqNumber, String pattern) {
            this.seqNumber = seqNumber;
            this.pattern = pattern;
        }

        public synchronized void waitInQueue() {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public synchronized void notifyMe() {
            notify();
        }
    }
}
