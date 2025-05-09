/**
 * CPSC 326, Spring 2025
 * Basic symbol table implementation. 
 */


package cpsc326;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

/**
 * Class to represent variable environment type bindings for semantic
 * code analysis.
 */ 
public class SymbolTable {

  // a stack (as a deque) of environments (name -> data type mappings)
  private Deque<Map<String,DataType>> environments = new ArrayDeque<>();

  /**
   * Add an environment to the symbol table. 
   */
  public void pushEnvironment() {
    environments.push(new HashMap<>());
  }

  /**
   * Remove last added environment from the symbol table. 
   */
  public void popEnvironment() {
    if (environments.size() > 0)
      environments.pop();
  }

  /**
   * Check if a given variable name exists in the symbol table.
   * @param name the variable name to check
   * @return true if the name exists in the symbol table
   */ 
  public boolean exists(String name) {
    for (var env : environments)
      if (env.containsKey(name))
        return true;
    return false;
  }
  
  /**
   * Check if a given variable name exists in the last added
   * environment.
   * @param name the variable name to check   
   * @return true if the name is in the current environment
   */ 
  public boolean existsInCurrEnv(String name) {
    return environments.size() > 0 && environments.peek().containsKey(name);
  }
  
  /**
   * Add a variable binding to the current environment.
   * @param name the variable name to add
   * @param type the data type to bind to the variable name
   */
  public void add(String name, DataType type) {
    if (environments.size() > 0)
      environments.peek().put(name, type);
  }

  /**
   * Return the data type of the given variable name.
   * @param name the variable name 
   * @return the data type
   */
  public DataType get(String name) {
    for (var env : environments) {
      if (env.containsKey(name))
        return env.get(name);
    }
    return null;
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
   * @return a string representation of the symbol table
   */
  public String toString() {
    String s = "";
    for (var env : environments) {
      s += "environment: {"; 
      for (var e : env.entrySet()) {
        s += "\n " + e.getKey() + " -> " + e.getValue().type.lexeme;
        if (e.getValue().isArray)
          s += " (isArray = true)";
        else
          s += " (isArray = false)";
      }
      s += "\n}\n";
    }
    return s;
  }

}

