/**
 * CPSC 326, Spring 2025
 * Pretty print visitor.
 *
 * Christian Carrington
 */

package cpsc326;

public class PrintVisitor implements Visitor {

  private int indent = 0;

  /**
   * Prints message without ending newline
   */
  private void write(String s) {
    System.out.print(s);
  }

  /**
   * Increase the indent level by one
   */
  private void incIndent() {
    indent++;
  }

  /**
   * Decrease the indent level by one
   */
  private void decIndent() {
    indent--;
  }

  /**
   * Print an initial indent string
   */
  private String indent() {
    return "  ".repeat(indent);
  }

  /**
   * Prints a newline
   */
  private void newline() {
    System.out.println();
  }

  /**
   * Prints the program
   */
  public void visit(Program node) {
    // always one blank line at the "top"
    newline();
    for (StructDef s : node.structs)
      s.accept(this);
    for (FunDef f : node.functions)
      f.accept(this);
  }

  // TODO: Complete the rest of the visit functions.

  // Use the above helper functions to write, deal with indentation,
  // and print newlines as part of your visit functions.

  // general AST Classes
  public void visit(FunDef node) {
    incIndent();
    if (node.returnType.isArray) {
      write("list<");
      write(node.returnType.type.lexeme);
      write("> ");
    } else {
      write(node.returnType.type.lexeme + " ");
    }
    write(node.funName.lexeme + "(");
    int size = node.params.size();
    for (VarDef param : node.params) {
      param.accept(this);
      if (size > 1) {
        write(", ");
      }
      size--;
    }
    write(") {");
    newline();
    for (Stmt stmt : node.stmts) {
      write(indent());
      stmt.accept(this);
      newline();
    }
    decIndent();
    indent();
    write("}");
    newline();
    newline();
  }

  public void visit(StructDef node) {
    write("struct ");
    write(node.structName.lexeme + " {");
    newline();
    incIndent();
    int size = node.fields.size();
    for (VarDef field : node.fields) {
      write(indent());
      field.accept(this);
      if (size > 1) {
        write(",");
      }
      newline();
      size--;
    }
    decIndent();
    write("}");
    newline();
    newline();
  }

  public void visit(DataType node) {
    if (node.isArray) {
      write("[" + node.type.lexeme + "]");
    } else {
      write(node.type.lexeme);
    }
  }

  public void visit(VarDef node) {
    write(node.varName.lexeme + ": ");
    node.dataType.accept(this);
  }

  // statements
  public void visit(ReturnStmt node) {
    write("return ");
    node.expr.accept(this);
  }

  public void visit(VarStmt node) {
    write("var " + node.varName.lexeme + ": ");
    if (node.dataType.isPresent())
      node.dataType.get().accept(this);
    write(" = ");
    if (node.expr.isPresent())
      node.expr.get().accept(this);
  }

  public void visit(AssignStmt node) {
    boolean first = true;
    for (VarRef var : node.lvalue) {
      if (!first) {
        write(".");
      }
      write(var.varName.lexeme);
      if (var.arrayExpr.isPresent()) {
        write("[");
        var.arrayExpr.get().accept(this);
        write("]");
      }
      first = false;
    }
    write(" = ");
    node.expr.accept(this);
  }

  public void visit(WhileStmt node) {
    write("while ");
    node.condition.accept(this);
    write(" {");
    newline();
    incIndent();
    for (Stmt stmt : node.stmts) {
      write(indent());
      stmt.accept(this);
      newline();
    }
    decIndent();
    write(indent());
    write("}");
  }

  public void visit(ForStmt node) {
    write("for ");
    write(node.varName.lexeme + " from ");
    node.fromExpr.accept(this);
    write(" to ");
    node.toExpr.accept(this);
    write(" {");
    newline();
    incIndent();
    for (Stmt stmt : node.stmts) {
      write(indent());
      stmt.accept(this);
      newline();
    }
    decIndent();
    write(indent());
    write("}");
  }

  public void visit(IfStmt node) {
    write("if ");
    node.condition.accept(this);
    write(" {");
    newline();
    incIndent();
    for (Stmt stmt : node.ifStmts) {
      write(indent());
      stmt.accept(this);
      newline();
    }
    decIndent();
    write(indent());
    write("}");
    if (node.elseIf.isPresent()) {
      newline();
      write(indent());
      write("else ");
      node.elseIf.get().accept(this);
    }
    if (node.elseStmts.isPresent()) {
      newline();
      write(indent());
      write("else {");
      newline();
      incIndent();
      for (Stmt stmt : node.elseStmts.get()) {
        write(indent());
        stmt.accept(this);
        newline();
      }
      decIndent();
      write(indent());
      write("}");
    }
  }

  // expressions
  public void visit(BasicExpr node) {
    node.rvalue.accept(this);
  }

  public void visit(UnaryExpr node) {
    write(node.unaryOp.lexeme + " (");
    node.expr.accept(this);
    write(")");
  }

  public void visit(BinaryExpr node) {
    write("(");
    node.lhs.accept(this);
    write(" " + node.binaryOp.lexeme + " ");
    node.rhs.accept(this);
    write(")");
  }

  public void visit(CallRValue node) {
    write(node.funName.lexeme + "(");
    for (Expr arg : node.args) {
      arg.accept(this);
    }
    write(")");
  }

  public void visit(SimpleRValue node) {
    if (node.literal.tokenType == TokenType.STRING_VAL)
      write("\"");
    write(node.literal.lexeme);
    if (node.literal.tokenType == TokenType.STRING_VAL)
      write("\"");
  }

  public void visit(NewStructRValue node) {
    write("new ");
    write(node.structName.lexeme + "(");
    boolean first = true;
    for (Expr arg : node.args) {
      if (!first) {
        write(", ");
      }
      arg.accept(this);
      first = false;

    }
    write(")");

  }

  public void visit(NewArrayRValue node) {
    write(node.type.lexeme + "[");
    node.arrayExpr.accept(this);
    write("]");
  }

  public void visit(VarRValue node) {
    boolean first = true;
    for (VarRef var : node.path) {
      if (!first) {
        write(".");
      }
      write(var.varName.lexeme);
      if (var.arrayExpr.isPresent()) {
        write("[");
        var.arrayExpr.get().accept(this);
        write("]");
      }
      first = false;
    }
  }
}
