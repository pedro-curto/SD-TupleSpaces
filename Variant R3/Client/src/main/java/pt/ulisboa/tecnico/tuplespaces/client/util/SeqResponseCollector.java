package pt.ulisboa.tecnico.tuplespaces.client.util;

public class SeqResponseCollector {
    private int _seqNumber;
    private boolean _debug;

    public SeqResponseCollector(boolean debug) {
        _debug = debug;
        _seqNumber = -1;
    }

    public synchronized void addSeqNumber(String response) {
        if (_debug) {
            System.err.println("[SeqResponseCollector] Response received: " + response);
            System.err.println("[SeqResponseCollector] Sequence number received: " + Integer.parseInt(response.substring(11, response.length() - 1)));
        }
        // we receive seqNumber: 2 as a response so we only want the number
        _seqNumber = Integer.parseInt(response.substring(11, response.length() - 1));
        System.out.println("Sequence number received: " + _seqNumber);
        notifyAll();
    }

    public synchronized int getSeqNumber() {
        return _seqNumber;
    }

    public synchronized void waitForSeqResponse() {
        while (_seqNumber == -1) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("Interrupted while waiting for sequence number response");
            }
        }
    }
}
