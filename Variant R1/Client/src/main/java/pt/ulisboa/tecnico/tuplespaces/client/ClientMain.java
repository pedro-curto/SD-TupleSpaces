package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc.*;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import java.util.Random;

import java.util.List;

public class ClientMain {
    public static void main(String[] args) {

        // check arguments
        if (args.length < 3) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -D exec.args=<host> <port> <service> *-debug");
            System.err.println("* -debug argument is optional");
            return;
        }

        // get the host and the port
        final String host = args[0];
        final String port = args[1];
        final String service = args[2];
        final String target = host + ":" + port;
        boolean debug = false;

        int i = 0;
        for (String arg : args) {
            i++;
            if (arg.equals("-debug")) {
                debug = true;
                System.out.println("Debug mode enabled");
            }
        }

        // connects to the python NameSpace server to find a registered server that provides the service we want
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        NameServerBlockingStub nameServerStub = NameServerGrpc.newBlockingStub(channel);
        LookupRequest lookupRequest = LookupRequest.newBuilder().setService(service).setQualifier("").build();
        LookupResponse lookupResponse = nameServerStub.lookup(lookupRequest);

        List<String> addresses = lookupResponse.getAddressList();
        // simply select the first server that provides the service
        if (addresses.isEmpty()) {
            System.err.println("No server available");
            System.err.println("Client is now going to shutdown...");
            channel.shutdown();
            return;
        }
        String serverAddress = addresses.get(0);
        String[] serverAddressSplit = serverAddress.split(":");
        String serverHost = serverAddressSplit[0];
        String serverPort = serverAddressSplit[1];

        ClientService clientService = new ClientService(serverHost, serverPort, debug);
        CommandProcessor parser = new CommandProcessor(clientService);
        // the client stays in this loop until the user writes "exit"
        parser.parseInput();
        // when "exit" is inputted, the channel is shutdown
        channel.shutdown();

    }
}
