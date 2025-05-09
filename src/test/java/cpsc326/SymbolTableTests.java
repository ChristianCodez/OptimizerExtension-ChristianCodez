/**
 * CPSC 326, Spring 2025
 * Basic symbol table tests.
 */

package cpsc326;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the SymbolTable class.
 */
class SymbolTableTests {

  @Test
  void emptySymbolTable() {
    SymbolTable t = new SymbolTable();
    assertEquals(0, t.size());
  }

  @Test
  void symbolTablePushAndPop() {
    SymbolTable t = new SymbolTable();
    assertEquals(0, t.size());
    t.pushEnvironment();
    assertEquals(1, t.size());
    t.popEnvironment();
    assertEquals(0, t.size());
    t.pushEnvironment();
    t.pushEnvironment();
    assertEquals(2, t.size());
    t.popEnvironment();
    assertEquals(1, t.size());
    t.popEnvironment();
    assertEquals(0, t.size());
    t.popEnvironment();
    assertEquals(0, t.size());    
  }

  @Test
  void symbolTableSimpleAdd() {
    SymbolTable t = new SymbolTable();
    DataType d = new DataType();
    d.type = new Token(TokenType.INT_TYPE, "int", 0, 0);
    // no environment to add to
    t.add("x", d);
    assertFalse(t.exists("x"));
    // now add an environment
    t.pushEnvironment();
    t.add("x", d);
    assertTrue(t.exists("x"));      
    t.popEnvironment();
  }
  
  @Test
  void symbolTableMultipleAdds() {
    SymbolTable t = new SymbolTable();
    DataType d1 = new DataType();
    d1.type = new Token(TokenType.INT_TYPE, "int", 0, 0);
    DataType d2 = new DataType();
    d2.type = new Token(TokenType.STRING_TYPE, "string", 0, 0);
    t.pushEnvironment();
    t.add("x", d1);
    t.add("y", d2);
    assertTrue(t.exists("x"));
    assertEquals(d1, t.get("x"));
    assertTrue(t.exists("y"));
    assertEquals(d2, t.get("y"));
  }

  @Test
  void symbolTableMultipleEnvironments() {
    SymbolTable t = new SymbolTable();
    DataType d1 = new DataType();
    d1.type = new Token(TokenType.INT_TYPE, "int", 0, 0);
    DataType d2 = new DataType();
    d2.type = new Token(TokenType.STRING_TYPE, "string", 0, 0);
    t.pushEnvironment();
    t.add("x", d1);
    t.add("y", d2);
    t.pushEnvironment();
    t.add("x", d2);
    t.add("z", d1);
    t.pushEnvironment();
    t.add("u", d1);
    assertTrue(t.exists("x") && t.get("x") == d2);
    assertTrue(t.exists("y") && t.get("y") == d2);
    assertTrue(t.exists("z") && t.get("z") == d1);
    assertTrue(t.exists("u") && t.get("u") == d1);
    assertFalse(t.existsInCurrEnv("x"));
    assertFalse(t.existsInCurrEnv("y"));
    assertFalse(t.existsInCurrEnv("z"));
    assertTrue(t.existsInCurrEnv("u"));
    t.popEnvironment();
    assertTrue(t.exists("x") && t.get("x") == d2);
    assertTrue(t.exists("y") && t.get("y") == d2);
    assertTrue(t.exists("z") && t.get("z") == d1);
    assertFalse(t.exists("u"));    
    assertTrue(t.existsInCurrEnv("x"));
    assertFalse(t.existsInCurrEnv("y"));    
    assertTrue(t.existsInCurrEnv("z"));
    t.popEnvironment();
    assertTrue(t.exists("x") && t.get("x") == d1);
    assertTrue(t.exists("y") && t.get("y") == d2);
    assertFalse(t.exists("z"));
    t.popEnvironment();
  }
  
}
