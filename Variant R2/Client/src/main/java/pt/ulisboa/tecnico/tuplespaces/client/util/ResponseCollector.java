package pt.ulisboa.tecnico.tuplespaces.client.util;

import com.google.rpc.context.AttributeContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;
public class ResponseCollector {

	private List<String> _responses = new ArrayList<String>();
	private List<List<String>> _tupleLists = new ArrayList<>();
	private boolean _debug;
	private int _rejectedResponses;

	public ResponseCollector(boolean debug) {
		_debug = debug;
		_rejectedResponses = 0;
	}

	public synchronized void incrementRejectedResponses() {
		if (_debug) System.out.println("Rejected response received, current rejected responses: " + _rejectedResponses);
		_rejectedResponses++;
		notifyAll();
	}

	public synchronized void addResponse(String response) {
		if (_debug) System.err.println("Response received: " + response);
		_responses.add(response);
		notifyAll();
	}

	public synchronized void addTupleList(List<String> response) {
		if (_debug) System.err.println("Tuple received in ResponseCollector: " + response);
		_tupleLists.add(response);
		notifyAll();
	}

	public synchronized List<String> getResponses() { return _responses; }

	public synchronized List<List<String>> getTupleLists() { return _tupleLists; }

	public synchronized void waitForResponses(int numResponses) {
		while (_responses.size() < numResponses) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("Interrupted while waiting for responses");
			}
		}
	}

	/**
	 * Only used for the take operation; waits until the specified number of responses
	 * have been collected, either accepted or rejected.
	 * Informs the client if there are rejected responses, and how many
	 * for it to decide to keep or release the locks.
	 *
	 * @param numResponses the number of responses to wait for
	 */
	public synchronized void waitForTake(int numResponses) {
		while ((_tupleLists.size() + _rejectedResponses) < numResponses) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("Interrupted while waiting for responses");
			}
		}
		if (_rejectedResponses >= (int) Math.ceil(numResponses / 2.0)) {
			throw new StatusRuntimeException(Status.CANCELLED.withDescription("Too many rejected responses"));
		} else if (_rejectedResponses > 0) {
			throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Some responses were rejected"));
		}
		if (_debug) System.out.println("All responses received");
	}
}
