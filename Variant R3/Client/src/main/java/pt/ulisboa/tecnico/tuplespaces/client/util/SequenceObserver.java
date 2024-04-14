package pt.ulisboa.tecnico.tuplespaces.client.util;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;

import java.util.List;

public class SequenceObserver<R> implements StreamObserver<R> {
	private final SeqResponseCollector collector;
	public SequenceObserver(SeqResponseCollector collector) { this.collector = collector; }

	@Override
	public void onNext(R value) { collector.addSeqNumber(value.toString()); }

	@Override
	public void onError(Throwable t) { }

	@Override
	public void onCompleted() { }
}
