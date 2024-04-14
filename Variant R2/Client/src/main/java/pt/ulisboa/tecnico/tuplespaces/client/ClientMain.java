package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import pt.ulisboa.tecnico.tuplespaces.client.manager.NameServerManager;
import java.util.Random;

import java.util.List;

public class ClientMain {
    static final int numServers = 3;
    public static void main(String[] args) {
        // check arguments
        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -D exec.args=<id> <service> *-debug");
            System.err.println("* -debug argument is optional");
            return;
        }
        final String service = args[0];
        final int id = Integer.parseInt(args[1]);
        boolean debug = false;

        // checks for debug flag in arg[2]
        if (args.length > 2) {
            if (args[2].equals("-debug")) {
                debug = true;
                System.out.println("Debug mode enabled");
            }
        }
        // connects to the python NameSpace server to find a registered server that provides the service we want
        CommandProcessor parser = new CommandProcessor(new ClientService(ClientMain.numServers, debug, service, id));
        // the client stays in this loop until the user writes "exit"
        parser.parseInput();
    }
}
