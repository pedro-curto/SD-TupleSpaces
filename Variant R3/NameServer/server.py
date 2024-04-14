import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from concurrent import futures
import logging
from NamingServerServiceImpl import NamingServerServiceImpl

# define the port
PORT = 5001

if __name__ == '__main__':
    try:
        # print received arguments
        debug = False
        print("Received arguments:")
        for i in range(1, len(sys.argv)):
            print("  " + sys.argv[i])
            if sys.argv[i] == "-debug":
                debug = True

        # start the server (based on route_guide_server.py)
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        pb2_grpc.add_NameServerServicer_to_server(NamingServerServiceImpl(debug), server)
        server.add_insecure_port('[::]:' + str(PORT))
        server.start()
        print("NameServer listening on port " + str(PORT))
        print("Press Ctrl+C to stop")

        server.wait_for_termination()

    except KeyboardInterrupt:
        print("NameServer stopped")
        exit(0)
