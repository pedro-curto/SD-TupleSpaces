package pt.ulisboa.tecnico.tuplespaces.server.domain;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;

import java.util.List;

public class TupleSpacesImpl extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {

    private final ServerState state;
    private final boolean debug;

    public TupleSpacesImpl(ServerState state, boolean debug) {
        this.state = state;
        this.debug = debug;
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        if (debug) System.err.println("[TupleSpacesImpl] Read request received with pattern: " + request.getSearchPattern());
        String result = state.read(request.getSearchPattern());
        ReadResponse reply = ReadResponse.newBuilder().setResult(result).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        if (debug) System.err.println("[TupleSpacesImpl] Take request received with pattern: " + request.getSearchPattern() + " and seqNumber: " + request.getSeqNumber());
        String result = state.take(request.getSearchPattern(), request.getSeqNumber());
        TakeResponse reply = TakeResponse.newBuilder().setResult(result).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        if (debug) System.err.println("[TupleSpacesImpl] Put request received with tuple: " + request.getNewTuple() + " and seqNumber: " + request.getSeqNumber());
        state.put(request.getNewTuple(), request.getSeqNumber());
        PutResponse reply = PutResponse.newBuilder().build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        if (debug) System.err.println("[TupleSpacesImpl] GetTupleSpacesState request received");
        String[] result = state.getTupleSpacesState().toArray(new String[0]);
        getTupleSpacesStateResponse reply = getTupleSpacesStateResponse.newBuilder().addAllTuple(List.of(result)).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }


}
