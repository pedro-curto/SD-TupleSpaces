package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import pt.ulisboa.tecnico.tuplespaces.client.manager.NameServerManager;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.client.util.ClientObserver;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.client.util.ResponseCollector;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientService {
    private final String service;
    private boolean debug;
    OrderedDelayer delayer;
    private NameServerManager manager;
    private final int numServers;

    public ClientService(int numServers, boolean debug, String service) {
        this.debug = debug;
        this.service = service;
        this.numServers = numServers;
        delayer = new OrderedDelayer(numServers);
        manager = new NameServerManager(numServers, service, debug);
    }

    /**
     * Reads a tuple from the tuple space that matches the specified pattern.
     *
     * @param tuple the pattern to match against tuples in the tuple space
     * @return the first tuple that matches the pattern, as a string
     */
    public String read(String tuple) {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        if (debug) System.err.println("Read request received on clientService with pattern: " + tuple);
        ResponseCollector collector = new ResponseCollector(debug);
        executorService.submit(() -> {
            for (Integer i : delayer) {
                if (debug) System.err.println("Sending request to stub[" + i + "]");
                ClientObserver<ReadResponse> observer = new ClientObserver<ReadResponse>(collector);
                ReadRequest request = ReadRequest.newBuilder().setSearchPattern(tuple).build();
                manager.getStub(i).read(request, observer);
            }
        });
        collector.waitForResponses(1);
        executorService.shutdownNow();
        List<String> res = collector.getResponses();
        return res.get(0);
	}

    /**
     * Shuts down the client service by closing all channels managed by its NameServerManager.
     */
    public void shutdown() {
        manager.closeChannels();
    }

    /**
     * Sends a put request to the server with the specified tuple.
     * 
     * @param tuple the tuple to be put in the server
     */
    public void put(String tuple) {
        int seqNumber = manager.getRequestSeqNumber();
        if (debug) System.err.println("Put request received on clientService with tuple: " + tuple + " and seqNumber: " + seqNumber);
        ResponseCollector collector = new ResponseCollector(debug);
        for (Integer i : delayer) {
            if (debug) { System.err.println("Sending put request to stub[" + i + "]"); }
            ClientObserver<PutResponse> observer = new ClientObserver<PutResponse>(collector);
            PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).setSeqNumber(seqNumber).build();
            manager.getStub(i).put(request, observer);
        }
        collector.waitForResponses(numServers);
        if (debug) { System.err.println("Put request completed."); }
    }

    /**
     * Takes a tuple from the tuple space that matches the given pattern.
     * This method performs the take operation in two phases: takePhase1 and takePhase2.
     * 
     * @param pattern the pattern to match the tuple
     * @return the tuple that was removed from the tuple space
     */
    public String take(String pattern) {
        int seqNumber = manager.getRequestSeqNumber();
        TakeRequest request = TakeRequest.newBuilder()
                .setSearchPattern(pattern)
                .setSeqNumber(seqNumber)
                .build();
        ResponseCollector collector = new ResponseCollector(debug);
        for (Integer i : delayer) {
            if (debug) System.err.println("Sending request to stub[" + i + "]");
            ClientObserver<TakeResponse> observer = new ClientObserver<>(collector);
            manager.getStub(i).take(request, observer);
        }
        collector.waitForResponses(numServers);
        List<String> responses = collector.getResponses();
        if (responses.isEmpty()) {
            return "";
        }
        return responses.get(0);
    }

    /**
     * Retrieves the state of tuple spaces for a given replica, identified by its qualifier.
     * This request is immediate and does not go through the delayer, since its main
     * purpose is to assist in debugging.
     *
     * @param qualifier The qualifier of the tuple spaces.
     * @return A list of strings representing the tuples present in the tuple spaces.
     */
    public List<String> getTupleSpacesState(String qualifier) {
        if (debug) System.err.println("GetTupleSpacesState request received on clientService for qualifier:" + qualifier + " and service: " + service);
        ResponseCollector collector = new ResponseCollector(debug);
        ClientObserver<getTupleSpacesStateResponse> observer = new ClientObserver<getTupleSpacesStateResponse>(collector);
        getTupleSpacesStateRequest request = getTupleSpacesStateRequest.newBuilder().build();
        int index = manager.indexOfServerQualifier(qualifier);
        if (index == -1) return null;
        manager.getStub(index).getTupleSpacesState(request, observer);
        collector.waitForResponses(1);
        return collector.getResponses();
    }

    /**
     * Sets the delay for a specific server.
     * 
     * @param id    the ID of the server
     * @param delay the delay in seconds
     */
    public void setDelay(int id, int delay) {
        delayer.setDelay(id, delay);
        if (debug) { System.err.println("Setting delay for server[" + id + "] to " + delay + " seconds."); }
    }
}
