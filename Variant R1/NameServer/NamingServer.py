from ServerEntry import ServerEntry
from ServiceEntry import ServiceEntry

class NamingServer:
    def __init__(self):
        self.services = {}
        
    def register_service(self, service, qualifier, address):
        try:
            if service not in self.services:
                self.services[service] = ServiceEntry(service)
            self.services[service].add_server_entry(ServerEntry(address, qualifier))
        except Exception as e:
            raise Exception("Not possible to register the server")

    def lookup_service(self, service, qualifier):
        if service not in self.services:
            return []

        if qualifier is None or qualifier == "":
            return [address for x in self.services if x == service for address in self.services[x].get_all_addresses()]

        return self.services[service].lookup(qualifier)

    def delete_service(self, service, address):
        try:
            self.services[service].delete(address)
            if len(self.services[service].server_entries) == 0:
                self.services.pop(service)
        except Exception as e:
            raise Exception(str(e))