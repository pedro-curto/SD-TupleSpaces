package pt.ulisboa.tecnico.tuplespaces.client.manager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc.NameServerBlockingStub;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.client.util.SeqResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.SequenceObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc.*;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The NameServerManager class is responsible for managing the client communication with the name server.
 * On startup, it looks up servers for qualifiers A, B and C and creates the channels and stubs for communication with them.
 * It also provides methods to call these stubs from the client and close the channels.
 */
public class NameServerManager {
	private static String _target = "localhost:5001";
	private static ManagedChannel[] _channels;
	private static TupleSpacesReplicaStub[] _stubs;
	private static String _sequencerAddress;
	private static ManagedChannel _sequencerChannel;
	private static SequencerStub _sequencerStub;
	private boolean _debug;

    public NameServerManager(int numServers, String service, boolean debug) {
		_debug = debug;
		_sequencerAddress = genericLookup("Sequencer").get(0);
		_sequencerChannel = ManagedChannelBuilder.forTarget(_sequencerAddress).usePlaintext().build();
		_sequencerStub = SequencerGrpc.newStub(_sequencerChannel);

        List<String> addresses = new ArrayList<>();
		addresses.add(lookupForQualifier(service, "A"));
		addresses.add(lookupForQualifier(service, "B"));
		addresses.add(lookupForQualifier(service, "C"));

		_channels = new ManagedChannel[numServers];
		_stubs = new TupleSpacesReplicaStub[addresses.size()];

		for (int i = 0; i < numServers; i++) {
			_channels[i] = ManagedChannelBuilder.forTarget(addresses.get(i)).usePlaintext().build();
			_stubs[i] = TupleSpacesReplicaGrpc.newStub(_channels[i]);
		}
	}

	public String lookupForQualifier(String service, String qualifier) {
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(_target).usePlaintext().build();
		LookupRequest lookupRequest = LookupRequest.newBuilder().setService(service).setQualifier(qualifier).build();
		LookupResponse lookupResponse = NameServerGrpc.newBlockingStub(channel).lookup(lookupRequest);
		channel.shutdownNow();
		return lookupResponse.getAddress(0);
	}

	public List<String> genericLookup(String service) {
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(_target).usePlaintext().build();
		LookupRequest lookupRequest = LookupRequest.newBuilder().setService(service).setQualifier("").build();
		LookupResponse lookupResponse = NameServerGrpc.newBlockingStub(channel).lookup(lookupRequest);
		channel.shutdownNow();
		return lookupResponse.getAddressList();
	}

	public TupleSpacesReplicaStub getStub(int index) {
		return _stubs[index];
	}

	public int getRequestSeqNumber() {
		SeqResponseCollector collector = new SeqResponseCollector(_debug);
		SequenceObserver<GetSeqNumberResponse> observer = new SequenceObserver<GetSeqNumberResponse>(collector);
		GetSeqNumberRequest request = GetSeqNumberRequest.newBuilder().build();
		_sequencerStub.getSeqNumber(request, observer);
		collector.waitForSeqResponse();
		return collector.getSeqNumber();
	}

	public void closeChannels() {
		for (ManagedChannel channel : _channels){
			channel.shutdown();
		}
		_sequencerChannel.shutdown();
	}

	public int indexOfServerQualifier(String qualifier) {
		switch (qualifier) {
			case "A":
				return 0;
			case "B":
				return 1;
			case "C":
				return 2;
			default:
				return -1;
		}
	}
}
