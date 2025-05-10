/**
 * CPSC 326, Spring 2025
 * 
 * Christian Carrington
 */

package cpsc326;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Generates MyPL VM code from an AST.
 */
public class CodeGenerator implements Visitor {

  /* vm to add frames to */
  private VM vm;

  /* current frame template being generated */
  private VMFrameTemplate currTemplate;

  /* variable -> index mappings with respect to environments */
  private VarTable varTable = new VarTable();

  /* struct defs for field names */
  private Map<String, StructDef> structs = new HashMap<>();

  /**
   * Create a new Code Generator given a virtual machine
   * 
   * @param vm the VM for storing generated frame templates
   */
  public CodeGenerator(VM vm) {
    this.vm = vm;
  }

  // ----------------------------------------------------------------------
  // Helper functions
  // ----------------------------------------------------------------------

  /**
   * Helper to add an instruction to the current frame.
   * 
   * @param instr the instruction to add
   */
  private void add(VMInstr instr) {
    currTemplate.add(instr);
  }

  /**
   * Helper to add an instruction to the current frame with a comment.
   * 
   * @param instr   the instruction to add
   * @param comment the comment to assign to the instruction
   */
  private void add(VMInstr instr, String comment) {
    instr.comment = comment;
    currTemplate.add(instr);
  }

  /**
   * Helper to execute body statements that cleans up the stack for
   * single function call statements (whose returned values aren't
   * used).
   */
  private void execBody(List<Stmt> stmts) {
    for (var stmt : stmts) {
      stmt.accept(this);
      if (stmt instanceof CallRValue callNode && !callNode.funName.lexeme.equals("print"))
        add(VMInstr.POP(), "clean up call rvalue statement");
    }
  }

  // ----------------------------------------------------------------------
  // Visitors for programs, functions, and structs
  // ----------------------------------------------------------------------

  /**
   * Generates the IR for the program
   */
  public void visit(Program node) {
    // record each struct definitions and check for duplicate names
    for (StructDef s : node.structs)
      s.accept(this);
    // generate each function
    for (FunDef f : node.functions)
      f.accept(this);
  }

  /**
   * Generates a function definition
   */
  public void visit(FunDef node) {
    // TODO: see lecture notes
    currTemplate = new VMFrameTemplate(node.funName.lexeme);
    varTable.pushEnvironment();

    // List<Integer> indexes = new ArrayList<>();
    for (VarDef param : node.params) {
      varTable.add(param.varName.lexeme); // adds to vartable
      int index = varTable.get(param.varName.lexeme); // gets vartable index
      add(VMInstr.STORE(index));
      // indexes.add(index);
    }

    // NOTE: to generate the body code (for this and other
    // statements), you can use: execBody(node.stmts)
    execBody(node.stmts);

    if (!currTemplate.instructions.isEmpty()) {
      VMInstr val = currTemplate.instructions.get(currTemplate.instructions.size() - 1); // gets the last instruction
      if (val.opcode != OpCode.RET) { // checks if last instruction was a return
        add(VMInstr.PUSH(VM.NULL), "ForStmt: 1");
        add(VMInstr.RET());
      }
    } else {
      add(VMInstr.PUSH(VM.NULL), "ForStmt: 2");
      add(VMInstr.RET());
    }

    varTable.popEnvironment();
    vm.add(currTemplate);
  }

  /**
   * Adds the struct def to the list of structs.
   */
  public void visit(StructDef node) {
    structs.put(node.structName.lexeme, node);
  }

  /**
   * The visitor function for a variable definition, but this visitor
   * function is not used in code generation.
   */
  public void visit(VarDef node) {
    // nothing to do here
  }

  /**
   * The visitor function for data types, but not used in code generation.
   */
  public void visit(DataType node) {
    // nothing to do here
  }

  // TODO: Finish the remaining visit functions ...

  // done
  public void visit(ForStmt node) {
    varTable.pushEnvironment();

    varTable.add(node.varName.lexeme);
    int loopVarIndex = varTable.get(node.varName.lexeme); // index of 'i' in for(int i = ...)
    if (loopVarIndex < 0) {
      throw new RuntimeException("ForStmt: Failed to allocate index for loop variable");
    }

    // evaluate from expr: i = from
    node.fromExpr.accept(this);
    add(VMInstr.STORE(loopVarIndex)); // start value (from) stored at index of 'i' in memory

    // Mark the start of the loop condition check
    int conditionPC = currTemplate.instructions.size();

    add(VMInstr.LOAD(loopVarIndex), "ForStmt: load loop var");

    // evaluate to expr: i < to
    node.toExpr.accept(this);

    // Compare loop var <= upper bound
    add(VMInstr.CMPLE(), "ForStmt: compare loop var <= upper bound"); // i <= n

    // Placeholder JMPF
    VMInstr jmpf = VMInstr.JMPF(0); // just a placeholder
    add(jmpf, "ForStmt: temporary jumpf offset");
    int jmpf_index = currTemplate.instructions.size() - 1; // Index where JMPF was added so we can go back and change

    // go through body
    execBody(node.stmts);

    // Increment loop variable: i++
    add(VMInstr.LOAD(loopVarIndex), "ForStmt: load loop var for increment");
    add(VMInstr.PUSH(1), "ForStmt: push 1");
    add(VMInstr.ADD(), "ForStmt: increment loop var");
    add(VMInstr.STORE(loopVarIndex), "ForStmt: store incremented loop var");

    // Jump back to start of the loop condition check
    add(VMInstr.JMP(conditionPC), "ForStmt: jmp to loop condition");

    // replace placeholder
    int final_pc = currTemplate.instructions.size();
    VMInstr final_jumpf = VMInstr.JMPF(final_pc);
    // replaces the placeholder at the index saved earlier with the correct jmpf
    currTemplate.instructions.set(jmpf_index, final_jumpf);

    varTable.popEnvironment();
  }

  // done
  public void visit(IfStmt node) {
    // List of JMP instructions that need to jump to the end of the if-elseif-else
    // stmts
    List<Integer> jumpsToEnd = new ArrayList<>();

    // IF block
    node.condition.accept(this); // evaluate the main if condition
    add(VMInstr.JMPF(0)); // jump to elseif/else stmts if condition is false -- placeholder for now
    int jmpfIndex = currTemplate.instructions.size() - 1; // index to go back to when you backpatch the placeholder

    execBody(node.ifStmts); // execute if-block

    // unconditional jump to end of all if/else blocks
    add(VMInstr.JMP(0));
    jumpsToEnd.add(currTemplate.instructions.size() - 1); // we will go back using this index to backpatch the jmp to
                                                          // the end

    // backpatch the initial JMPF
    int afterIfPC = currTemplate.instructions.size(); // pc for the end of the if stmt
    currTemplate.instructions.set(jmpfIndex, VMInstr.JMPF(afterIfPC)); // replace placeholder with afterIfPC for when if
                                                                       // stmt condition is false

    // ELSE-IF block
    IfStmt current = node;
    while (current.elseIf.isPresent()) {
      current = current.elseIf.get();

      current.condition.accept(this); // evaluate elseif condition

      add(VMInstr.JMPF(0));
      int elseifJmpfIndex = currTemplate.instructions.size() - 1; // we will go back using this index to change the jmpf
                                                                  // to the end of the else-if stmt

      execBody(current.ifStmts); // execute elseif block

      // unconditional jump to end
      add(VMInstr.JMP(0));
      jumpsToEnd.add(currTemplate.instructions.size() - 1); // we will go back using this index to backpatch the jmp to
                                                            // the end

      // backpatch the JMPF for this elseif
      int afterElseIfPC = currTemplate.instructions.size(); // the pc for the end of the elseif stmt
      currTemplate.instructions.set(elseifJmpfIndex, VMInstr.JMPF(afterElseIfPC)); // if condition was false then jmpf
                                                                                   // jumps to afterElseIfPC; not filler
    }

    // ELSE BLOCK
    if (current.elseStmts.isPresent()) {
      execBody(current.elseStmts.get()); // execute else block
    }

    // fix all JMP(0) to jump to the end
    int afterAllIfsPC = currTemplate.instructions.size(); // the correct pc to jump to the end
    for (int jmpIndex : jumpsToEnd) {
      currTemplate.instructions.set(jmpIndex, VMInstr.JMP(afterAllIfsPC)); // for every if, and else if statement we
                                                                           // change the jump after completed to go to
                                                                           // the end
    }
  }

  // done
  public void visit(VarStmt node) {
    varTable.add(node.varName.lexeme);
    int index = varTable.get(node.varName.lexeme);

    if (node.expr.isPresent()) {
      node.expr.get().accept(this);
    } else {
      add(VMInstr.PUSH(VM.NULL), "VarStmt"); // if there is no expression then just push null
    }

    add(VMInstr.STORE(index), "VarStmt"); // stores the index to get either the expression or null
  }

  // done
  public void visit(AssignStmt node) {

    // Handle single variable assignment: x = expr
    if (node.lvalue.size() == 1) {
      VarRef var = node.lvalue.get(0); // var holds x
      if (var.arrayExpr.isPresent()) { // checks if x is an array
        // Array assignment: x[i] = expr
        add(VMInstr.LOAD(varTable.get(var.varName.lexeme)), "AssignStmt: load array oid"); // pushes array object onto
                                                                                           // stack
        var.arrayExpr.get().accept(this); // evaluate index;; pushes index onto stack
        node.expr.accept(this); // value to assign is now on top of the operand stack
        add(VMInstr.SETI(), "AssignStmt: set array index");
      } else {
        // Simple variable assignment: x = expr
        node.expr.accept(this); // value to assign is now on top of the operand stack
        int index = varTable.get(var.varName.lexeme);
        add(VMInstr.STORE(index), "AssignStmt: store to local var"); // stores value into memory;; x -> val
      }
    } else {
      // Handle chained field assignment: a.b.c = expr
      // walk through the lvalue to get to the final struct and do a SETF

      // Evaluate base object: a
      VarRef base = node.lvalue.get(0);
      add(VMInstr.LOAD(varTable.get(base.varName.lexeme)), "AssignStmt: load struct oid");

      if (base.arrayExpr.isPresent()) { // if the field has an array expr then go into that
        base.arrayExpr.get().accept(this);
        add(VMInstr.GETI(), "gets index in array");
      }
      // Traverse intermediate fields: b
      for (int i = 1; i < node.lvalue.size() - 1; i++) {
        // add(VMInstr.DUP());
        VarRef fieldRef = node.lvalue.get(i);
        if (fieldRef.arrayExpr.isPresent()) { // if the field has an array expr then go into that
          add(VMInstr.GETF(fieldRef.varName.lexeme), "AssignStmt: get field (array)" + fieldRef.varName.lexeme);
          fieldRef.arrayExpr.get().accept(this);
          add(VMInstr.GETI(), "gets index in array");
        } else { // if no array expr then get the field as normal
          add(VMInstr.GETF(fieldRef.varName.lexeme), "AssignStmt: get field " + fieldRef.varName.lexeme);
        }
      }

      // Now set the final field: c
      VarRef finalRef = node.lvalue.get(node.lvalue.size() - 1);
      if (finalRef.arrayExpr.isPresent()) {
        add(VMInstr.GETF(finalRef.varName.lexeme), "AssignStmt: get field (array) " + finalRef.varName.lexeme);
        finalRef.arrayExpr.get().accept(this);
        node.expr.accept(this); // value to assign is now on top of the operand stack
        add(VMInstr.SETI(), "AssignStmt: sets value at index in array");
      } else {
        node.expr.accept(this); // value to assign is now on top of the operand stack
        add(VMInstr.SETF(finalRef.varName.lexeme), "AssignStmt: set field " + finalRef.varName.lexeme);
      }
    }
  }

  // done
  public void visit(ReturnStmt node) {
    node.expr.accept(this);
    add(VMInstr.RET(), "RetStmt");
  }

  // done
  public void visit(WhileStmt node) {
    int initial_pc = currTemplate.instructions.size(); // gets initial pc
    node.condition.accept(this);

    // Placeholder JMPF
    VMInstr jmpf = VMInstr.JMPF(0); // just a placeholder
    add(jmpf, "WhileStmt: temporary jumpf offset");
    int jmpf_index = currTemplate.instructions.size() - 1; // Index where JMPF was added so we can go back and change

    // go through body
    execBody(node.stmts);
    // jumps back to right before condition was checked
    add(VMInstr.JMP(initial_pc));

    // replace placeholder
    int final_pc = currTemplate.instructions.size();
    VMInstr final_jumpf = VMInstr.JMPF(final_pc);
    // replaces the placeholder at the index saved earlier with the correct jmpf
    currTemplate.instructions.set(jmpf_index, final_jumpf);
  }

  public void visit(BasicExpr node) {
    node.rvalue.accept(this);
  }

  public void visit(BinaryExpr node) {
    // have to swap bc we have no > or >=
    if (node.binaryOp.tokenType == TokenType.GREATER || node.binaryOp.tokenType == TokenType.GREATER_EQ) {
      node.rhs.accept(this);
      node.lhs.accept(this);
    } else {
      node.lhs.accept(this);
      node.rhs.accept(this);
    }

    TokenType op = node.binaryOp.tokenType;
    switch (op) {
      case PLUS:
        add(VMInstr.ADD());
        break;
      case MINUS:
        add(VMInstr.SUB());
        break;
      case TIMES:
        add(VMInstr.MUL());
        break;
      case DIVIDE:
        add(VMInstr.DIV());
        break;
      case AND:
        add(VMInstr.AND());
        break;
      case OR:
        add(VMInstr.OR());
        break;
      case EQUAL:
        add(VMInstr.CMPEQ());
        break;
      case NOT_EQUAL:
        add(VMInstr.CMPNE());
        break;
      case LESS:
        add(VMInstr.CMPLT());
        break;
      case LESS_EQ:
        add(VMInstr.CMPLE());
        break;
      case GREATER:
        add(VMInstr.CMPLT());
        break;
      case GREATER_EQ:
        add(VMInstr.CMPLE());
        break;
      default:
        throw new RuntimeException("BinExpr: Unknown binary operator: " + op);
    }
  }

  public void visit(UnaryExpr node) {
    node.expr.accept(this);

    if (node.unaryOp.tokenType == TokenType.NOT) {
      add(VMInstr.NOT());
    } else {
      throw new RuntimeException("UnaryExpr: Unary Operator must be of type 'NOT'");
    }
  }

  public void visit(CallRValue node) {
    if (node.funName.lexeme.equals("print")) {
      if (node.args == null || node.args.size() != 1) {
        throw new RuntimeException(
            "CallRValue: One argument expected, found " + (node.args == null ? 0 : node.args.size()));
      }
      try {
        node.args.get(0).accept(this);
        add(VMInstr.WRITE());
        // add(VMInstr.PUSH(VM.NULL), "CallRValue");
      } catch (Exception e) {
        throw new RuntimeException("CallRValue: Null argument passed to 'print'");
      }
    } else if (node.funName.lexeme.equals("println")) {
      if (node.args == null || node.args.size() != 1) {
        throw new RuntimeException(
            "CallRValue: One argument expected, found " + (node.args == null ? 0 : node.args.size()));
      }
      try {
        node.args.get(0).accept(this);
        add(VMInstr.WRITE());
        add(VMInstr.PUSH("\n"));
        add(VMInstr.WRITE());
        add(VMInstr.PUSH(VM.NULL), "CallRValue");
      } catch (Exception e) {
        throw new RuntimeException("CallRValue: Null argument passed to 'str_val'");
      }
    } else if (node.funName.lexeme.equals("readln")) {
      if (node.args.size() != 0) {
        throw new RuntimeException(
            "CallRValue: No arguments expected, found " + (node.args == null ? 0 : node.args.size()));
      }
      try {
        add(VMInstr.READ());
        // add(VMInstr.PUSH(VM.NULL), "CallRValue");
      } catch (Exception e) {
        throw new RuntimeException("CallRValue: Null argument passed to 'str_val'");
      }
    } else if (node.funName.lexeme.equals("str_val")) { // TOSTR
      if (node.args == null || node.args.size() != 1) {
        throw new RuntimeException(
            "CallRValue: One argument expected, found " + (node.args == null ? 0 : node.args.size()));
      }
      try {
        node.args.get(0).accept(this);
        add(VMInstr.TOSTR());
        // add(VMInstr.PUSH(VM.NULL), "CallRValue");
      } catch (Exception e) {
        throw new RuntimeException("CallRValue: Null argument passed to 'str_val'");
      }
    } else if (node.funName.lexeme.equals("int_val")) { // TOINT
      if (node.args == null || node.args.size() != 1) {
        throw new RuntimeException(
            "CallRValue: One argument expected, found " + (node.args == null ? 0 : node.args.size()));
      }
      try {
        node.args.get(0).accept(this);
        add(VMInstr.TOINT());
        // add(VMInstr.PUSH(VM.NULL), "CallRValue");
      } catch (Exception e) {
        throw new RuntimeException("CallRValue: Null argument passed to 'int_val'");
      }
    } else if (node.funName.lexeme.equals("dbl_val")) { // TODBL
      if (node.args == null || node.args.size() != 1) {
        throw new RuntimeException(
            "CallRValue: One argument expected, found " + (node.args == null ? 0 : node.args.size()));
      }
      try {
        node.args.get(0).accept(this);
        add(VMInstr.TODBL());
        // add(VMInstr.PUSH(VM.NULL), "CallRValue");
      } catch (Exception e) {
        throw new RuntimeException("CallRValue: Null argument passed to 'dbl_val'");
      }
    } else if (node.funName.lexeme.equals("size")) { // LEN
      if (node.args == null || node.args.size() != 1) {
        throw new RuntimeException(
            "CallRValue: One argument expected, found " + (node.args == null ? 0 : node.args.size()));
      }
      try {
        node.args.get(0).accept(this);
        add(VMInstr.LEN());
        // add(VMInstr.PUSH(VM.NULL), "CallRValue");
      } catch (Exception e) {
        throw new RuntimeException("CallRValue: Null argument passed to 'size'");
      }
    } else if (node.funName.lexeme.equals("get")) { // GETC
      if (node.args == null || node.args.size() != 2) {
        throw new RuntimeException(
            "CallRValue: Two arguments expected, found " + (node.args == null ? 0 : node.args.size()));
      }
      try {
        node.args.get(1).accept(this);
        node.args.get(0).accept(this);
        add(VMInstr.GETC());
        // add(VMInstr.PUSH(VM.NULL), "CallRValue");
      } catch (Exception e) {
        throw new RuntimeException("CallRValue: Invalid argument passed to 'get'");
      }
    } else {
      if (node.args != null) {
        for (int i = node.args.size() - 1; i >= 0; i--) {
          node.args.get(i).accept(this);
        }
      }
      // Calls function
      add(VMInstr.CALL(node.funName.lexeme), "CallRValue: Call user function '" + node.funName.lexeme + "'");
    }
  }

  public void visit(VarRValue node) {
    add(VMInstr.LOAD(varTable.get(node.path.get(0).varName.lexeme)), "VarRValue"); // loads oid

    if (node.path.get(0).arrayExpr.isPresent()) { // checks if it is x[]
      node.path.get(0).arrayExpr.get().accept(this); // loads index
      add(VMInstr.GETI());
    }

    for (int i = 1; i < node.path.size(); i++) {
      add(VMInstr.GETF(node.path.get(i).varName.lexeme), "VarRValue");

      if (node.path.get(i).arrayExpr.isPresent()) { // checks if it is x[]
        node.path.get(i).arrayExpr.get().accept(this); // loads index
        add(VMInstr.GETI());
      }
    }
  }

  public void visit(SimpleRValue node) {
    String val = node.literal.lexeme;
    switch (node.literal.tokenType) {
      case TokenType.INT_VAL:
        int intVal = Integer.parseInt(val);
        add(VMInstr.PUSH(intVal), "SimpleRValue: intVal");
        break;
      case TokenType.DOUBLE_VAL:
        double doubleVal = Double.parseDouble(val);
        add(VMInstr.PUSH(doubleVal), "SimpleRValue: doubleVal");
        break;
      case TokenType.STRING_VAL:
        val = val.replace("\\n", "\n");
        val = val.replace("\\t", "\t");
        val = val.replace("\\r", "\r");
        add(VMInstr.PUSH(val), "SimpleRValue: stringVal");
        break;
      case TokenType.BOOL_VAL:
        boolean boolVal = Boolean.parseBoolean(val);
        add(VMInstr.PUSH(boolVal), "SimpleRValue: boolVal");
        break;
      case TokenType.NULL_VAL:
        add(VMInstr.PUSH(VM.NULL), "SimpleRValue");
        break;
      default:
        throw new RuntimeException("ERROR: Unknown literal type " + val);
    }
  }

  public void visit(NewStructRValue node) {

    add(VMInstr.ALLOCS());

    StructDef struct = structs.get(node.structName.lexeme);

    if (struct == null) {
      throw new RuntimeException("NewStructRValue: Undefined struct type");
    }

    /*
     * if (node.args.size() != struct.fields.size()) {
     * throw new RuntimeException("NewStructRValue: Incorrect number of arguments");
     * }
     */

    for (int i = 0; i < node.args.size(); i++) {
      add(VMInstr.DUP(), "NewStructRValue");
      node.args.get(i).accept(this);
      add(VMInstr.SETF(struct.fields.get(i).varName.lexeme));
    }
  }

  public void visit(NewArrayRValue node) {
    node.arrayExpr.accept(this);
    add(VMInstr.ALLOCA());
  }

}
