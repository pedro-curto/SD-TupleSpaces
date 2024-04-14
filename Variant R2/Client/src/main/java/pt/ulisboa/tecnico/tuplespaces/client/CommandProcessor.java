package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

import java.util.List;
import java.util.Scanner;

public class CommandProcessor {

    private static final String SPACE = " ";
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";
    private static final String PUT = "put";
    private static final String READ = "read";
    private static final String TAKE = "take";
    private static final String SLEEP = "sleep";
    private static final String SET_DELAY = "setdelay";
    private static final String EXIT = "exit";
    private static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";

    private final ClientService clientService;

    public CommandProcessor(ClientService clientService) { this.clientService = clientService; }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);
            switch (split[0]) {
                case PUT:
                    this.put(split);
                    break;

                case READ:
                    this.read(split);
                    break;

                case TAKE:
                    this.take(split);
                    break;

                case GET_TUPLE_SPACES_STATE:
                    this.getTupleSpacesState(split);
                    break;

                case SLEEP:
                    this.sleep(split);
                    break;

                case SET_DELAY:
                    this.setdelay(split);
                    break;

                case EXIT:
                    exit = true;
                    break;

                default:
                    this.printUsage();
                    break;
            }
        }
        scanner.close();
        clientService.shutdown();
    }

    private void put(String[] split) {
        // check if input is valid
        if (!this.inputIsValid(split)) {
            return;
        }
        // get the tuple
        String tuple = split[1];
        // puts the tuple
        clientService.put(tuple);
        System.out.println("OK\n");
    }

    private void read(String[] split){
        // check if input is valid
        if (!this.inputIsValid(split)) {
            return;
        }
        // get the tuple
        String tuple = split[1];
        // read the tuple and prints with the required format from the tests
        String message = clientService.read(tuple);
        // pre-processes tuples to remove start (result: "content")
        // and I just want content
        message = message.substring(9, message.length() - 2);
        System.out.println("OK");
        System.out.println(message + "\n");
    }


    private void take(String[] split){
        // check if input is valid
        if (!this.inputIsValid(split)) {
            return;
        }
        // gets the tuple
        String tuple = split[1];
        // takes the tuple from the tuple space and prints with the required format from the tests
        String takenTuple = clientService.take(tuple);
        System.out.println("OK");
        System.out.println(takenTuple + "\n");
    }

    private void getTupleSpacesState(String[] split) {
        if (split.length != 2) {
            this.printUsage();
            return;
        }
        String qualifier = split[1];
        // get the tuple spaces state
        List<String> tupleSpacesState = clientService.getTupleSpacesState(qualifier);
        if (tupleSpacesState == null) {
            System.out.println("Invalid server qualifier");
            return;
        }
        // prints with the required format from the tests
        System.out.println("OK");
        if (tupleSpacesState.get(0).equals("")) {
            System.out.println("[]\n");
            return;
        }
        // pre-processes tuples to remove start (tuple: "content")
        // and I just want content
        String tuples = tupleSpacesState.get(0)
                        // adds a bracket at the start
                        .replaceFirst("tuple: ", "[")
                        .replaceAll("tuple: ", "")
                        .replaceAll("\"", "")
                        .replaceAll("\n", ", ")
                        // removes the last comma and end of line and adds a bracket at the end
                        .replaceAll(", $", "]");
        System.out.println(tuples + "\n");
    }

    private void sleep(String[] split) {
        if (split.length != 2){
            this.printUsage();
            return;
        }
        Integer time;
        // checks if input String can be parsed as an Integer
        try {
            time = Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            this.printUsage();
            return;
        }

        try {
            Thread.sleep(time*1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void setdelay(String[] split) {
        if (split.length != 3){
            this.printUsage();
            return;
        }
        int qualifier = indexOfServerQualifier(split[1]);
        if (qualifier == -1) {
            System.out.println("Invalid server qualifier");
        }
        Integer time;

        // checks if input String can be parsed as an Integer
        try {
            time = Integer.parseInt(split[2]);
        } catch (NumberFormatException e) {
            this.printUsage();
            return;
        }

        // register delay <time> for when calling server <qualifier>
        clientService.setDelay(qualifier, time);
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- put <element[,more_elements]>\n" +
                "- read <element[,more_elements]>\n" +
                "- take <element[,more_elements]>\n" +
                "- getTupleSpacesState <server>\n" +
                "- sleep <integer>\n" +
                "- setdelay <server> <integer>\n" +
                "- exit\n");
    }

    private int indexOfServerQualifier(String qualifier) {
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

    private boolean inputIsValid(String[] input){
        if (input.length < 2
                ||
                !input[1].substring(0, 1).equals(BGN_TUPLE)
                ||
                !input[1].endsWith(END_TUPLE)
                ||
                input.length > 2
        ) {
            this.printUsage();
            return false;
        }
        else {
            return true;
        }
    }
}
