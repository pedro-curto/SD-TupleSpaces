package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.domain.TupleSpacesImpl;

import java.io.IOException;

//import dns grpc
import pt.ulisboa.tecnico.nameserver.contract.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc.*;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;

import static java.lang.System.exit;

public class ServerMain {

    public static void main(String[] args) {
        System.out.println(ServerMain.class.getSimpleName());
        if (args.length < 4) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -D exec.args=\"<host> <port> <qualifier> <service> *-debug\"");
            System.err.println("* -debug argument is optional");
            return;
        }
        boolean debug = false;
        int i = 0;
        for (String arg : args) {
            i++;
            if (arg.equals("-debug")) {
                debug = true;
                System.err.println("Debug mode enabled");
            }
        }
        // server receives (localhost, 2001, A and TupleSpaces (and a default -debug flag)) from the pom.xml
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String qualifier = args[2];
        final String service = args[3];
        final ServerState state = new ServerState();
        final BindableService impl = new TupleSpacesImpl(state, debug);
        final String target = "localhost:5001";
        final String address = host + ":" + port;

        // when ctrl+c is hit the server will be unregistered from the name server
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


        // creates a new server to listen on port
        Server grpcServer = ServerBuilder.forPort(port).addService(impl).build();

        // starts grpc server
        try {
            grpcServer.start();
            if (debug) {
                System.err.println("Server started");
            }

            // registers server in the name server
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
                grpcServer.shutdownNow();
                System.exit(1);
            }

            if (debug) {
                System.err.println("Server registered");
            }

            // waits until server termination
            grpcServer.awaitTermination();
        } catch (IOException | InterruptedException | StatusRuntimeException e) {
            System.err.println("Error starting server: " + e.getMessage());
            exit(1);
        }
    }
}