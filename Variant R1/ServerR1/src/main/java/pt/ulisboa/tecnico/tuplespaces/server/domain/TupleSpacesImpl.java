package pt.ulisboa.tecnico.tuplespaces.server.domain;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;

import java.util.List;


public class TupleSpacesImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private ServerState state;
    private boolean debug;

    public TupleSpacesImpl(ServerState state, boolean debug) {
        this.state = state;
        this.debug = debug;
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        if (debug) System.err.println("Read request received with pattern: " + request.getSearchPattern());
        String result = state.read(request.getSearchPattern());
        ReadResponse reply = ReadResponse.newBuilder().setResult(result).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        if (debug) System.err.println("Take request received with pattern: " + request.getSearchPattern());
        String result = state.take(request.getSearchPattern());
        TakeResponse reply = TakeResponse.newBuilder().setResult(result).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        if (debug) System.err.println("Put request received with tuple: " + request.getNewTuple());
        state.put(request.getNewTuple());
        PutResponse reply = PutResponse.newBuilder().build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(GetTupleSpacesStateRequest request, StreamObserver<GetTupleSpacesStateResponse> responseObserver) {
        if (debug) System.err.println("GetTupleSpacesState request received");
        String[] result = state.getTupleSpacesState().toArray(new String[0]);
        GetTupleSpacesStateResponse reply = GetTupleSpacesStateResponse.newBuilder().addAllTuple(List.of(result)).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }


}
