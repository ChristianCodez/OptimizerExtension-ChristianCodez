/**
 * CPSC 326, Spring 2025
 * Basic semantic checker unit tests.
 */

package cpsc326;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Unit tests for the SemanticChecker implementation
 */
class OptimizerFoldingTests {

  /**
   * Helper to build an input string.
   */
  InputStream istream(String str) {
    try {
      return new ByteArrayInputStream(str.getBytes("UTF-8"));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private void assertLiteral(Expr expr, String expectedLexeme, TokenType expectedType) {
    assertTrue(expr instanceof BasicExpr, "Expected BasicExpr");
    BasicExpr basic = (BasicExpr) expr;

    assertTrue(basic.rvalue instanceof SimpleRValue, "Expected SimpleRValue");
    SimpleRValue val = (SimpleRValue) basic.rvalue;

    assertEquals(expectedLexeme, val.literal.lexeme, "Incorrect literal value");
    assertEquals(expectedType, val.literal.tokenType, "Incorrect literal type");
  }

  // ----------------------------------------------------------------------
  // Basic Function Definitions
  @Test
  void constantFoldingTest() {
    var p = """
          void main() {
            var x = 2 + 3 * 4
          }
        """;
    Program prog = new ASTParser(new Lexer(istream(p))).parse();

    // First check semantic correctness
    prog.accept(new SemanticChecker());

    // Run the optimizer
    prog.accept(new ASTOptimizer());

    // Now verify that the expression is folded
    FunDef mainFun = prog.functions.get(0);
    VarStmt varStmt = (VarStmt) mainFun.stmts.get(0);
    Expr folded = varStmt.expr.get();

    assertLiteral(folded, "14", TokenType.INT_VAL);
  }

  @Test
  void testIntFolding() {
    var p = "void main() { var x = 2 + 3 * 4 }";
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "14", TokenType.INT_VAL);
  }

  @Test
  void testBooleanFolding() {
    var p = "void main() { var b = not false }";
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "true", TokenType.BOOL_VAL);
  }

  @Test
  void testNestedFolding() {
    var p = "void main() { var z = (1 + 2) * (3 + 4) }";
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "21", TokenType.INT_VAL);
  }

  @Test
  void testDivisionFolding() {
    var p = "void main() { var y = 10 / 2 + 1 }";
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "6", TokenType.INT_VAL);
  }

  @Test
  void testDoubleFolding() {
    var p = "void main() { var d = 1.5 * 2.0 + 1.0 }";
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "4.0", TokenType.DOUBLE_VAL);
  }

  @Test
  void testBooleanEqualityTrue() {
    var p = "void main() { var b = true == true }";
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "true", TokenType.BOOL_VAL);
  }

  @Test
  void testBooleanEqualityFalse() {
    var p = "void main() { var b = true != true }";
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "false", TokenType.BOOL_VAL);
  }

  @Test
  void testIntEquality() {
    var p = "void main() { var b = 5 == 5 }";
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "true", TokenType.BOOL_VAL);
  }

  @Test
  void testDoubleInequality() {
    var p = "void main() { var b = 3.14 != 2.71 }";
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "true", TokenType.BOOL_VAL);
  }

}
