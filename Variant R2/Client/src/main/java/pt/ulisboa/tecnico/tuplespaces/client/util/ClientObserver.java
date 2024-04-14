package pt.ulisboa.tecnico.tuplespaces.client.util;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;

import java.util.List;

public class ClientObserver<R> implements StreamObserver<R> {
	private final ResponseCollector collector;
	public ClientObserver(ResponseCollector collector) { this.collector = collector; }

	@Override
	public void onNext(R value) {
		if (value instanceof TakePhase1Response) {
			collector.addTupleList((((TakePhase1Response) value).getReservedTuplesList()));
		} else {
			collector.addResponse(value.toString());
		}
	}

	@Override
	public void onError(Throwable t) { collector.incrementRejectedResponses(); }

	@Override
	public void onCompleted() {
	}
}
