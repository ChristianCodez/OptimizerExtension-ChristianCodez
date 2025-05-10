/**
 * CPSC 326, Spring 2025
 * VarTable implementation for HW-6
 */


package cpsc326;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;


public class VarTable {

  // a stack (as a deque) of environments (name -> data type mappings)
  private Deque<List<String>> environments = new ArrayDeque<>();
  // total number of variables that have been added
  private int totalVars = 0;

  
  /**
   */
  public void pushEnvironment() {
    environments.push(new ArrayList<>());
  }

  /**
   */
  public void popEnvironment() {
    if (environments.size() > 0) {
      totalVars -= environments.peek().size();
      environments.pop();
    }
  }

  /**
   */
  public void add(String varName) {
    if (environments.size() > 0) {
      environments.peek().add(varName);
      ++totalVars;
    }
  }

  /**
   * Return the index of name in the 
   */
  public int get(String name) {
    int numRemaining = totalVars;
    for (var env : environments) {
      numRemaining -= env.size();
      int index = env.indexOf(name);
      if (index >= 0) 
        return numRemaining + index;
    }
    return -1;
  }
  
  /**
   * Gives the number of environments in the symbol table
   * @return the number of environments
   */
  public int size() {
    return environments.size();
  }

  /**
   * Creates a string for pretty printing a symbol table to help with
   * debugging
   */
  public String toString() {
    String s = "";
    for (var env : environments) {
      s += "environment: {"; 
      for (int i = 0; i < env.size(); ++i) {
        s += env.get(i); 
        if (i < env.size() - 1)
          s += ", ";
      }
      s += "\n}\n";
    }
    return s;
  }

}

