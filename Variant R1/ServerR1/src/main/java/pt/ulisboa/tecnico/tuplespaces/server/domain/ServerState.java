package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

public class ServerState {

  private List<String> tuples;

  public ServerState() {
    this.tuples = new ArrayList<String>();

  }

  public synchronized void put(String tuple) {
    tuples.add(tuple);
    notifyAll();
  }

  private synchronized String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return "";
  }

  public synchronized String read(String pattern) {
    String tuple = getMatchingTuple(pattern);
    while (tuple.isEmpty()) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      tuple = getMatchingTuple(pattern);
    }
    return tuple;
  }

  public synchronized String take(String pattern) {
    String tuple = getMatchingTuple(pattern);
    while (tuple.isEmpty()) {
        try {
            wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        tuple = getMatchingTuple(pattern);
    }
    tuples.remove(tuple);
    return tuple;
  }

  public synchronized List<String> getTupleSpacesState() {
    return this.tuples;
  }
}
