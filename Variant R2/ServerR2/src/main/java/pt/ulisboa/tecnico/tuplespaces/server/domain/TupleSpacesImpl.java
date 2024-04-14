package pt.ulisboa.tecnico.tuplespaces.server.domain;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.GetTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.GetTupleSpacesStateResponse;

import java.util.List;

/**
 * Represents the implementation of the TupleSpaces replica.
 * Provides the implementation for the gRPC service methods read, put,
 * take (phase1, phase1release and phase2) and getTupleSpacesState.
 */
public class TupleSpacesImpl extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {

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
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        if (debug) System.err.println("Put request received with tuple: " + request.getNewTuple());
        state.put(request.getNewTuple());
        PutResponse reply = PutResponse.newBuilder().build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void takePhase1(TakePhase1Request request, StreamObserver<TakePhase1Response> responseObserver){
        if (debug) System.err.println("TakePhase1 request received with pattern: " + request.getSearchPattern() + " and clientId: " + request.getClientId());
        try {
            List<String> reservedTuples = state.takePhase1(request.getSearchPattern(), request.getClientId());
            TakePhase1Response reply = TakePhase1Response.newBuilder().addAllReservedTuples(reservedTuples).build();
            System.out.println("Reserved tuples: " + reservedTuples);
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void takePhase1Release(TakePhase1ReleaseRequest request, StreamObserver<TakePhase1ReleaseResponse> responseObserver) {
        if (debug) System.err.println("TakePhase1Release request received with clientId: " + request.getClientId());
        state.takePhase1Release(request.getClientId());
        TakePhase1ReleaseResponse reply = TakePhase1ReleaseResponse.newBuilder().build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void takePhase2(TakePhase2Request request, StreamObserver<TakePhase2Response> responseObserver) {
        if (debug) System.err.println("TakePhase2 request received with tuple: " + request.getTuple() + " and clientId: " + request.getClientId());
        state.takePhase2(request.getTuple(), request.getClientId());
        TakePhase2Response reply = TakePhase2Response.newBuilder().build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(GetTupleSpacesStateRequest request, StreamObserver<GetTupleSpacesStateResponse> responseObserver) {
        if (debug) System.err.println("GetTupleSpacesState request received");
        List<String> result = state.getTupleSpacesState();
        GetTupleSpacesStateResponse reply = GetTupleSpacesStateResponse.newBuilder().addAllTuple(result).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
