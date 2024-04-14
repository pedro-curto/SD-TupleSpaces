package pt.ulisboa.tecnico.tuplespaces.sequencer;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc.*;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;
import io.grpc.*;

import java.io.IOException;

//import dns grpc
import pt.ulisboa.tecnico.nameserver.contract.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc.*;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;

import static java.lang.System.exit;

public class SequencerServer {

	/** Server host port. */
	private static int port;

	public static void main(String[] args) throws Exception {
		System.out.println(SequencerServer.class.getSimpleName());

		// Print received arguments.
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// Check arguments.
		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", Server.class.getName());
			return;
		}

		port = Integer.valueOf(args[0]);
		final BindableService impl = new SequencerServiceImpl();
		final String target = "localhost:5001";
		final String service = "Sequencer";
		final String qualifier = "S";
		final String address = "localhost:" + port;

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				System.out.println("\nShutting down server...");
				final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
				NameServerBlockingStub stub = NameServerGrpc.newBlockingStub(channel);
				DeleteRequest deleteRequest = DeleteRequest.newBuilder().setService(service).setAddress(address).build();
				DeleteResponse deleteResponse = stub.delete(deleteRequest);
				if (deleteResponse.getResponse().isEmpty()) {
					channel.shutdownNow();
				} else {
					System.out.println("Server not deleted");
				}
			} catch (StatusRuntimeException e) {
				System.err.println("Error deleting server from name server");
			}
		}));

		// Create a new server to listen on port.
		Server server = ServerBuilder.forPort(port).addService(impl).build();
		// Start the server.
		server.start();
		// Server threads are running in the background.
		System.out.println("Sequencer server started");

		try {
			final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
			NameServerBlockingStub stub = NameServerGrpc.newBlockingStub(channel);
			RegisterRequest registerRequest = RegisterRequest.newBuilder().setService(service).setQualifier(qualifier).setAddress(address).build();
			RegisterResponse registerResponse = stub.register(registerRequest);
			// shuts down the channel to the name server
			channel.shutdownNow();
		} catch (StatusRuntimeException e) {
			System.err.println("Error registering server in name server: " + e.getMessage());
			// shuts down the server
			server.shutdownNow();
			//grpcServer.shutdownNow();
			System.exit(1);
		}

		// Do not exit the main thread. Wait until server is terminated.
		server.awaitTermination();

		// Server is terminated.
		System.out.println("Sequencer server stopped");
		server.shutdown();
	}

}
