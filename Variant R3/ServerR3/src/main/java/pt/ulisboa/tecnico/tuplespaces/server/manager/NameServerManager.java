package pt.ulisboa.tecnico.tuplespaces.server.manager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc.NameServerBlockingStub;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;

public class NameServerManager {

    public static void registerServer(String target, String service, String qualifier, String address) {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        NameServerBlockingStub stub = NameServerGrpc.newBlockingStub(channel);
        RegisterRequest registerRequest = RegisterRequest.newBuilder().setService(service).setQualifier(qualifier).setAddress(address).build();
        RegisterResponse registerResponse = stub.register(registerRequest);
        // shuts down the channel to the name server
        channel.shutdownNow();
    }

    public static void unregisterServer(String target, String service, String address) {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        NameServerBlockingStub stub = NameServerGrpc.newBlockingStub(channel);
        DeleteRequest deleteRequest = DeleteRequest.newBuilder().setService(service).setAddress(address).build();
        DeleteResponse deleteResponse = stub.delete(deleteRequest);
        if (deleteResponse.getResponse().isEmpty()) {
            channel.shutdownNow();
        } else {
            System.out.println("Server not deleted");
        }
    }
}
