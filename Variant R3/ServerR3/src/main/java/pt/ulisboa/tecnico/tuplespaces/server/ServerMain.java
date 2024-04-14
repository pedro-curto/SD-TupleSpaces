package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.domain.TupleSpacesImpl;
import pt.ulisboa.tecnico.tuplespaces.server.manager.NameServerManager;

import java.io.IOException;

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
        final ServerState state = new ServerState(debug);
        final BindableService impl = new TupleSpacesImpl(state, debug);
        final String target = "localhost:5001"; // name server address
        final String address = host + ":" + port;

        // when ctrl+c is hit the server will be unregistered from the name server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("\nShutting down server...");
                NameServerManager.unregisterServer(target, service, address);
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
                NameServerManager.registerServer(target, service, qualifier, address);
            } catch (StatusRuntimeException e) {
                System.err.println("Error registering server in name server: " + e.getMessage());
                // shuts down the server
                grpcServer.shutdownNow();
                System.exit(1);
            }

            if (debug) {
                System.err.println("Server registered");
                System.err.println("Listening on " + address + " with service " + service + " and qualifier " + qualifier);
            }

            // waits until server termination
            grpcServer.awaitTermination();
        } catch (IOException | InterruptedException | StatusRuntimeException e) {
            System.err.println("Error starting server: " + e.getMessage());
            exit(1);
        }
    }
}
