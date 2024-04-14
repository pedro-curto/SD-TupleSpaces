class ServiceEntry:
    def __init__(self, name):
        self.name = name
        self.server_entries = []

    def add_server_entry(self, server_entry):
        self.server_entries.append(server_entry)

    def remove_server_entry(self, server_entry):
        self.server_entries.remove(server_entry)

    def get_all_addresses(self):
        return [server_entry.address for server_entry in self.server_entries]

    def lookup(self, qualifier):
        return [server_entry.address for server_entry in self.server_entries if server_entry.qualifier == qualifier]

    def delete(self, address):   #TODO da pra fazer com map ou algo do genero?
        for se in self.server_entries:
            if se.address == address:
                self.server_entries.remove(se)
                return
        raise Exception("Not possible to remove the server")