package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;

public class ClientService {
    private final ManagedChannel channel;
    private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;
    private boolean debug;

    public ClientService(String host, String port, boolean debug) {
        this.channel = ManagedChannelBuilder.forAddress(host, Integer.parseInt(port)).usePlaintext().build();
        this.stub = TupleSpacesGrpc.newBlockingStub(channel);
        this.debug = debug;
    }

    public String read(String tuple) {
        if (debug) System.err.println("Read request received on clientService with pattern: " + tuple);
        ReadRequest request = ReadRequest.newBuilder().setSearchPattern(tuple).build();
        ReadResponse response = stub.read(request);
        return response.getResult();
    }

    public void shutdown() {
        channel.shutdown();
    }

    public void put(String tuple) {
        if (debug) System.err.println("Put request received on clientService with tuple: " + tuple);
        PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();
        stub.put(request);
    }


    public String take(String pattern) {
        if (debug) System.err.println("Take request received on clientService with pattern: " + pattern);
        TakeRequest request = TakeRequest.newBuilder().setSearchPattern(pattern).build();
        TakeResponse response = stub.take(request);
        return response.getResult();
    }

    public String[] getTupleSpacesState() {
        if (debug) System.err.println("GetTupleSpacesState request received on clientService");
        GetTupleSpacesStateRequest request = GetTupleSpacesStateRequest.newBuilder().build();
        GetTupleSpacesStateResponse response = stub.getTupleSpacesState(request);
        return response.getTupleList().toArray(new String[0]);
    }

}
