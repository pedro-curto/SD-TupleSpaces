package pt.ulisboa.tecnico.tuplespaces.server.domain;

/**
 * Represents a tuple in the TupleSpaces server side.
 * Useful addition of the clientId and isLocked fields to the originally used Strings.
 */
public class Tuple {
	private final String fields;
	private int clientId;
	private boolean isLocked;

	public Tuple(String fields) {
		this.fields = fields;
		this.clientId = -1;
		this.isLocked = false;
	}

	public String getFields() {
		return fields;
	}

	public int getClientId() { return clientId; }

	public boolean isLocked() {
		return isLocked;
	}

	public boolean lock(int clientId) {
		if (isLocked && this.clientId != clientId) {
			return false;
		}
		this.clientId = clientId;
		isLocked = true;
		return true;
	}

	public void unlock(int clientId) {
		if (this.clientId != clientId) {
			return;
		}
		this.clientId = -1;
		isLocked = false;
	}
}
