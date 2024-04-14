package pt.ulisboa.tecnico.tuplespaces.client.manager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc.NameServerBlockingStub;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;

import java.util.ArrayList;
import java.util.List;

/**
 * The NameServerManager class is responsible for managing the client communication with the name server.
 * On startup, it looks up servers for qualifiers A, B and C and creates the channels and stubs for communication with them.
 * It also provides methods to call these stubs from the client and close the channels.
 */
public class NameServerManager {
	private static String _target;
	private static ManagedChannel[] _channels;
	private static TupleSpacesReplicaStub[] _stubs;

    public NameServerManager(int numServers, String target, String service) {
		_target = target;

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

	public TupleSpacesReplicaStub getStub(int index) {
		return _stubs[index];
	}

	public void closeChannels() {
		for (ManagedChannel channel : _channels){
			channel.shutdown();
		}
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
