package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.client.manager.NameServerManager;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.client.util.ClientObserver;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.client.util.ResponseCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Random;

public class ClientService {
    private final String service;
    private boolean debug;
    OrderedDelayer delayer;
    private NameServerManager manager;
    private int numServers;
    private final String target = "localhost:5001";
    private final int clientId;
    private static final int MAX_ATTEMPTS = 4; // avoid huge punishments, max is about 16s

    public ClientService(int numServers, boolean debug, String service, int id) {
        this.debug = debug;
        this.service = service;
        this.numServers = numServers;
        this.clientId = id;
        delayer = new OrderedDelayer(numServers);
        manager = new NameServerManager(numServers, target, service);
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
        if (debug) System.err.println("Put request received on clientService with tuple: " + tuple);
        ResponseCollector collector = new ResponseCollector(debug);
        for (Integer i : delayer) {
            if (debug) { System.err.println("Sending request to stub[" + i + "]"); }
            ClientObserver<PutResponse> observer = new ClientObserver<PutResponse>(collector);
            PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();
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
        String tupleToRemove = takePhase1(pattern, 0);
        return takePhase2(tupleToRemove);
    }

    /**
     * Sends a TakePhase1Request with the specified pattern, attempting to lock 
     * all tuples in all servers that match the pattern.
     *
     * @param pattern the search pattern for the take request
     * @return a randomly selected string from the intersection of the tuple lists
     */
    private String takePhase1(String pattern, int attempt) {
        if (debug) System.err.println("Take request received on clientService with pattern: " + pattern);
        TakePhase1Request request = TakePhase1Request.newBuilder()
                .setSearchPattern(pattern)
                .setClientId(clientId)
                .build();
        ResponseCollector collectorPhase1 = new ResponseCollector(debug);
        for (Integer i : delayer) {
            if (debug) System.err.println("Sending request to stub[" + i + "]");
            ClientObserver<TakePhase1Response> observer = new ClientObserver<>(collectorPhase1);
            manager.getStub(i).takePhase1(request, observer);
        }
        // waits for all responses; retries and doesn't unlock if it was able to lock a majority,
        // and retries but unlocks all locks if it wasn't able to lock a majority
        try {
            collectorPhase1.waitForTake(numServers);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.FAILED_PRECONDITION) {
                // able to lock majority case
                if (debug) System.err.println(e.getMessage());
                backoff(attempt);
                return takePhase1(pattern, attempt + 1);
            }
            if (e.getStatus().getCode() == Status.Code.CANCELLED) {
                // only locked minority case
                if (debug) System.err.println("Now releasing locks. Error message: " + e.getMessage());
                takePhase1Release();
                backoff(attempt);
                return takePhase1(pattern, attempt + 1);
            }
        }
        // from this point, we have all servers with locked tuples
        List<List<String>> replicaAnswers = collectorPhase1.getTupleLists();
        List<String> intersection = new ArrayList<>(replicaAnswers.get(0));
        for (int i = 1; i < numServers; i++) {
            intersection.retainAll(replicaAnswers.get(i));
        }
        if (debug) System.out.println(intersection);
        // backs off and retries if the intersection is empty, but doesn't unlock the tuples
        if (intersection.isEmpty()) {
            if (debug) System.err.println("No intersection found, repeating phase 1");
            backoff(attempt);
            return takePhase1(pattern, attempt + 1);
        }
        if (debug) System.out.println("TakePhase1 completed.");
        return intersection.get(new Random().nextInt(intersection.size()));
    }

    /**
     * Effectively removes the selected tuple, sending a TakePhase2Request to all servers.
     *
     * @param tupleToRemove the randomly selected tuple to be removed from the TupleSpace
     * @return the tuple that was removed from the TupleSpace
     */
    private String takePhase2(String tupleToRemove) {
        ResponseCollector phase2Collector = new ResponseCollector(debug);
        TakePhase2Request phase2Request = TakePhase2Request.newBuilder()
                .setTuple(tupleToRemove)
                .setClientId(clientId)
                .build();
        for (Integer i : delayer) {
            if (debug) System.err.println("Sending phase 2 request to stub[" + i + "]");
            ClientObserver<TakePhase2Response> phase2Observer = new ClientObserver<>(phase2Collector);
            manager.getStub(i).takePhase2(phase2Request, phase2Observer);
        }
        phase2Collector.waitForResponses(numServers);
        return tupleToRemove;
    }

    /**
     * Sends TakePhase1ReleaseRequest to every server, asking for every tuple 
     * locked by this client to be unlocked.
     */
    private void takePhase1Release() {
        if (debug) System.err.println("TakePhase1Release request received on clientService with clientId: " + clientId);
        TakePhase1ReleaseRequest request = TakePhase1ReleaseRequest.newBuilder().setClientId(clientId).build();
        for (Integer i : delayer) {
            if (debug) System.err.println("Sending request to stub[" + i + "]");
            ClientObserver<TakePhase1ReleaseResponse> observer = new ClientObserver<>(new ResponseCollector(debug));
            manager.getStub(i).takePhase1Release(request, observer);
        }
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
        ClientObserver<GetTupleSpacesStateResponse> observer = new ClientObserver<GetTupleSpacesStateResponse>(collector);
        GetTupleSpacesStateRequest request = GetTupleSpacesStateRequest.newBuilder().build();
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

    /**
     * Implements a backoff mechanism by sleeping for a random delay, which
     * we specify to be between 0 and 5 seconds, to avoid contention.
     */
    private static void backoff(int attempt) {
        attempt = Math.min(attempt, MAX_ATTEMPTS);
        Random random = new Random();
        // exp. backoff formula: 2^attempt * 1000ms
        int delay = (int) Math.pow(2, attempt) * 1000;
        delay += random.nextInt(1000);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
