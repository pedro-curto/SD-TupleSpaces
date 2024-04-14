import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from NamingServer import NamingServer
import grpc

# noinspection PyInterpreter
class NamingServerServiceImpl(pb2_grpc.NameServerServicer):

    def __init__(self, debug=False):
        self.namingServer = NamingServer()
        self.debug = debug

    def register(self, request, context):
        try:
            if self.debug:
                print("Registering service: " + request.service \
                      + " with qualifier: " + request.qualifier \
                      + " and address: " + request.address)
            self.namingServer.register_service(request.service, request.qualifier, request.address)
            if self.debug: print("Registered service")
        except Exception as e:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(str(e))
            raise grpc.RpcError('Exception: ' + str(e))
        return pb2.RegisterResponse()

    def lookup(self, request, context):
        if self.debug: print("Looking up service: " + request.service \
                             + " with qualifier: " + request.qualifier)
        address = self.namingServer.lookup_service(request.service, request.qualifier)
        if not address:
            return pb2.LookupResponse(address="")
        if self.debug: print("Service(s) found: " + str(address))
        return pb2.LookupResponse(address=address)

    def delete(self, request, context):
        try:
            if self.debug: print("Deleting service: " + request.service + " with address: " + request.address)
            self.namingServer.delete_service(request.service, request.address)
            if self.debug: print("Service deleted")
        except Exception as e:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(str(e))
            raise grpc.RpcError('Exception: ' + str(e))
        return pb2.DeleteResponse()