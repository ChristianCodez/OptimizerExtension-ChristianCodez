/**
 * CPSC 326, Spring 2025
 * Optimizer constant folding tests including bit-shift optimizations.
 */

package cpsc326;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

class OptimizerBitShiftFoldingTests {

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
  // Power-of-2 Multiplication Optimizations
  // ----------------------------------------------------------------------

  @Test
  void testMultiplyBy2Folding() {
    var p = "void main() { var x = 5 * 2 }"; // Should become 5 << 1 → 10
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "10", TokenType.INT_VAL);
  }

  @Test
  void testMultiplyBy8Folding() {
    var p = "void main() { var x = 3 * 8 }"; // Should become 3 << 3 → 24
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "24", TokenType.INT_VAL);
  }

  @Test
  void testMultiplyByLargePowerOf2() {
    var p = "void main() { var x = 7 * 1024 }"; // 1024=2^10 → 7 << 10 → 7168
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "7168", TokenType.INT_VAL);
  }

  // ----------------------------------------------------------------------
  // Power-of-2 Division Optimizations
  // ----------------------------------------------------------------------

  @Test
  void testDivideBy2Folding() {
    var p = "void main() { var x = 20 / 2 }"; // Should become 20 >> 1 → 10
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "10", TokenType.INT_VAL);
  }

  @Test
  void testDivideBy16Folding() {
    var p = "void main() { var x = 256 / 16 }"; // 16=2^4 → 256 >> 4 → 16
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "16", TokenType.INT_VAL);
  }

  // ----------------------------------------------------------------------
  // Non-Power-of-2 Cases (No Optimization)
  // ----------------------------------------------------------------------

  @Test
  void testMultiplyByNonPowerOf2() {
    var p = "void main() { var x = 5 * 3 }"; // 3 isn't power of 2 → no shift
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "15", TokenType.INT_VAL); // Still folded normally
  }

  @Test
  void testDivideByNonPowerOf2() {
    var p = "void main() { var x = 10 / 3 }"; // 3 isn't power of 2 → no shift
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "3", TokenType.INT_VAL); // Still folded normally
  }

  // ----------------------------------------------------------------------
  // Edge Cases
  // ----------------------------------------------------------------------

  @Test
  void testMultiplyByZero() {
    var p = "void main() { var x = 42 * 0 }"; // 0 isn't a valid shift amount
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "0", TokenType.INT_VAL); // Still folded normally
  }

  @Test
  void testDivideByOne() {
    var p = "void main() { var x = 100 / 1 }"; // 1=2^0 → 100 >> 0 → 100
    Program prog = new ASTParser(new Lexer(istream(p))).parse();
    prog.accept(new SemanticChecker());
    prog.accept(new ASTOptimizer());

    VarStmt stmt = (VarStmt) prog.functions.get(0).stmts.get(0);
    assertLiteral(stmt.expr.get(), "100", TokenType.INT_VAL);
  }
}