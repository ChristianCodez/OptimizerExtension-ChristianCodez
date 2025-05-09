/**
 * CPSC 326, Spring 2025
 * MyPL AST Interface and Class Definitions
 * 
 */

package cpsc326;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;


/**
 * Base AST inteface class that all AST nodes must implement
 */
public interface AST {
  public void accept(Visitor v);
}


//----------------------------------------------------------------------
// top-level AST classes
//----------------------------------------------------------------------


/**
 * Represents a MyPL program consisting of function and struct
 * definitions.
 */
class Program implements AST {
  public List<StructDef> structs = new ArrayList<>();
  public List<FunDef> functions = new ArrayList<>();
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a struct definition
 */
class StructDef implements AST {
  public Token structName;
  public List<VarDef> fields;
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a function definition
 */
class FunDef implements AST {
  public DataType returnType;
  public Token funName;
  public List<VarDef> params = new ArrayList<>();
  public List<Stmt> stmts = new ArrayList<>();
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a variable name with a data type (for function params
 * and struct fields)
 */
class VarDef implements AST {
  public Token varName;
  public DataType dataType;
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a data type
 */
class DataType implements AST {
  public boolean isArray;
  public Token type;
  public void accept(Visitor v) {v.visit(this);}
}


//----------------------------------------------------------------------
// Values and Expressions
//----------------------------------------------------------------------

/**
 * Interface for all expressions
 */
interface Expr extends AST {
}

/**
 * Interface for all rvalues
 */
interface RValue extends AST {
}

/**
 * Represents an expression composed of a signle rvalue
 */
class BasicExpr implements Expr {
  public RValue rvalue;
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents an expression with a unary operator followed by an
 * expression (currently only "not" is supported)
 */
class UnaryExpr implements Expr {
  public Token unaryOp;
  public Expr expr;
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents an expression with a left-hand-side (lhs) expression, a binary
 * operator, and a righ-hand-side (rhs) expression
 */
class BinaryExpr implements Expr {
  public Expr lhs;
  public Token binaryOp;
  public Expr rhs; 
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a literal (constant) value
 */
class SimpleRValue implements RValue {
  public Token literal;
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a function call (which can be used as both a statement
 * and an rvalue)
 */
class CallRValue implements Stmt, RValue {
  public Token funName;
  public List<Expr> args = new ArrayList<>();
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Interface for all new expressions
 */
interface NewRValue extends RValue {
}

/**
 * Represents a new struct expression
 */
class NewStructRValue implements NewRValue {
  public Token structName;
  public List<Expr> args = new ArrayList<>();
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a new array expression
 */
class NewArrayRValue implements NewRValue {
  public Token type;
  public Expr arrayExpr;
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a reference to a variable (for rvalues and lvalues) that
 * consists of a variable and an optional array index expression
 */
class VarRef {
  public Token varName;
  public Optional<Expr> arrayExpr = Optional.empty();
}

/**
 * Represents an rvalue consisting of one or more variable references
 * (forming a path expression)
 */
class VarRValue implements RValue {
  public List<VarRef> path = new ArrayList<>();
  public void accept(Visitor v) {v.visit(this);}
}

//----------------------------------------------------------------------
// Statements
//----------------------------------------------------------------------

/**
 * Interface for all statements
 */
interface Stmt extends AST {
}

/**
 * Represents a return statement
 */
class ReturnStmt implements Stmt {
  public Expr expr;
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a variable declaration and (optionally) its definition
 * (value assignment)
 */
class VarStmt implements Stmt {
  public Token varName;
  public Optional<DataType> dataType = Optional.empty();
  public Optional<Expr> expr = Optional.empty();
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents an assignment statement
 */
class AssignStmt implements Stmt {
  public List<VarRef> lvalue = new ArrayList<>();
  public Expr expr;
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a while statement
 */
class WhileStmt implements Stmt {
  public Expr condition;
  public List<Stmt> stmts = new ArrayList<>();
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents a for statement
 */
class ForStmt implements Stmt {
  public Token varName;
  public Expr fromExpr;
  public Expr toExpr;
  public List<Stmt> stmts = new ArrayList<>();
  public void accept(Visitor v) {v.visit(this);}
}

/**
 * Represents an if statement
 */
class IfStmt implements Stmt {
  public Expr condition;
  public List<Stmt> ifStmts = new ArrayList<>();
  public Optional<IfStmt> elseIf = Optional.empty();
  public Optional<List<Stmt>> elseStmts = Optional.empty();
  public void accept(Visitor v) {v.visit(this);}
}

