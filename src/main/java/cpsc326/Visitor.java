/**
 * CPSC 326, Spring 2025
 * Basic Visitor API.
 *
 */


package cpsc326;


public interface Visitor {
  // general AST classes
  public void visit(Program node);
  public void visit(FunDef node);
  public void visit(StructDef node);
  public void visit(DataType node);
  public void visit(VarDef node);
  // statements
  public void visit(ReturnStmt node);
  public void visit(VarStmt node);
  public void visit(AssignStmt node);
  public void visit(WhileStmt node);
  public void visit(ForStmt node);
  public void visit(IfStmt node);
  // expressions
  public void visit(BasicExpr node);
  public void visit(UnaryExpr node);
  public void visit(BinaryExpr node);
  public void visit(CallRValue node);
  public void visit(SimpleRValue node);
  public void visit(NewStructRValue node);
  public void visit(NewArrayRValue node);  
  public void visit(VarRValue node);
}
