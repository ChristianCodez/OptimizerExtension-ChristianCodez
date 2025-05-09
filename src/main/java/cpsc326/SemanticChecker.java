/**
* CPSC 326, Spring 2025
* The Semantic Checker implementation.
* 
* Christian Carrington
*/

package cpsc326;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class SemanticChecker implements Visitor {

  // for tracking function and struct definitions:
  private Map<String, FunDef> functions = new HashMap<>();
  private Map<String, StructDef> structs = new HashMap<>();
  // for tracking variable types:
  private SymbolTable symbolTable = new SymbolTable();
  // for holding the last inferred type:
  private DataType currType;

  // keeps track of the current func return type
  private DataType functionRet = null;

  // ----------------------------------------------------------------------
  // Helper functions
  // ----------------------------------------------------------------------

  /**
   */
  private boolean isBaseType(String type) {
    return List.of("int", "double", "bool", "string").contains(type);
  }

  /**
   */
  private boolean isBuiltInFunction(String name) {
    return List.of("print", "println", "readln", "size", "get", "int_val",
        "dbl_val", "str_val").contains(name);
  }

  /**
   * Create an error message
   */
  private void error(String msg) {
    MyPLException.staticError(msg);
  }

  /**
   * Creates an error
   */
  private void error(String msg, Token token) {
    String s = "[%d,%d] %s";
    MyPLException.staticError(String.format(s, token.line, token.column, msg));
  }

  /**
   * Checks if the field name is a field in the struct
   * definition. This is a helper method for checking and inferring
   * assignment statement lvalues and var rvalue paths.
   * 
   * @param fieldName the field name to check for
   * @param structDef the struct definition to check
   * @returns true if a match and false otherwise
   */
  private boolean isStructField(String fieldName, StructDef structDef) {
    for (var field : structDef.fields)
      if (field.varName.lexeme.equals(fieldName))
        return true;
    return false;
  }

  /**
   * Obtains the data type for the field name in the struct
   * definition. This is a helper method for checking and inferring
   * assignment statement lvalues and var rvalue paths.
   * 
   * @param fieldName the field name
   * @param structDef the struct definition
   * @returns the corresponding data type or null if no such field exists
   */
  private DataType getStructFieldType(String fieldName, StructDef structDef) {
    for (var field : structDef.fields)
      if (field.varName.lexeme.equals(fieldName))
        return field.dataType;
    return null;
  }

  // ----------------------------------------------------------------------
  // Visit Functions
  // ----------------------------------------------------------------------

  // checks program
  public void visit(Program node) {
    for (StructDef def : node.structs) {
      String id = def.structName.lexeme;
      if (structs.containsKey(id)) {
        error("Struct name already used: " + id, def.structName);
      }
      structs.put(id, def);
    }

    for (FunDef fn : node.functions) {
      String id = fn.funName.lexeme;
      if (isBuiltInFunction(id)) {
        error("Cannot use the name of a built-in: " + id, fn.funName);
      }
      if (functions.containsKey(id)) {
        error("Function name already used: " + id, fn.funName);
      }
      functions.put(id, fn);
    }

    if (!functions.containsKey("main")) {
      error("No main function defined");
    }

    for (StructDef struct : node.structs) {
      struct.accept(this);
    }
    // check each function
    for (FunDef func : node.functions) {
      func.accept(this);
    }
  }

  // Checks the function signature and body -- shadowing
  public void visit(FunDef node) {
    // Validate return type (must be base, struct, or void)
    if (!isBaseType(node.returnType.type.lexeme) &&
        !structs.containsKey(node.returnType.type.lexeme) &&
        !node.returnType.type.lexeme.equals("void")) {
      error("Return type is invalid: " + node.returnType.type.lexeme, node.returnType.type);
    }

    currType = node.returnType;

    // Special rules for main function
    if (node.funName.lexeme.equals("main")) {
      if (!node.returnType.type.lexeme.equals("void")) {
        error("Main function should return void", node.funName);
      }
      if (!node.params.isEmpty()) {
        error("Main function can't have parameters", node.funName);
      }
    }

    // Store return type of the function
    functionRet = node.returnType;

    // Begin a new scope for parameters
    symbolTable.pushEnvironment();

    Set<String> names = new HashSet<>();
    for (VarDef v : node.params) {
      // Validate parameter type
      if (!isBaseType(v.dataType.type.lexeme) && !structs.containsKey(v.dataType.type.lexeme)) {
        error("Parameter type invalid: " + v.dataType.type.lexeme, v.dataType.type);
      }
      // Check for duplicate parameter names
      if (!names.add(v.varName.lexeme)) {
        error("Parameter name already used: " + v.varName.lexeme, v.varName);
      }
      // Register parameter in symbol table
      DataType dataType = new DataType();
      dataType.type = new Token(TokenType.ID, v.dataType.type.lexeme, v.dataType.type.line, v.dataType.type.column);
      dataType.isArray = v.dataType.isArray;
      symbolTable.add(v.varName.lexeme, dataType);
    }

    // Check all statements in function body
    for (Stmt s : node.stmts) {
      s.accept(this);
    }

    // Clear current return type and exit scope
    functionRet = null;
    symbolTable.popEnvironment();
  }

  // Validates struct fields for duplicate names and proper types
  public void visit(StructDef node) {
    Set<String> seen = new HashSet<>();
    for (VarDef field : node.fields) {
      // check for duplicate field names
      if (seen.contains(field.varName.lexeme)) {
        error("Field name has been used: " + field.varName.lexeme, field.varName);
      } else {
        seen.add(field.varName.lexeme);
      }
      // validate that field type is either a base type or a known struct
      if (!isBaseType(field.dataType.type.lexeme) && !structs.containsKey(field.dataType.type.lexeme)) {
        error("Field type not usable: " + field.dataType.type.lexeme, field.dataType.type);
      }
    }
  }

  // Visits a variable definition node
  public void visit(VarDef node) {
    // Get the variable name directly from the node
    if (!isBaseType(node.dataType.type.lexeme) && !structs.containsKey(node.dataType.type.lexeme)) {
      // Report error if type is not valid
      error("Invalid variable type: " + node.dataType.type.lexeme, node.dataType.type);
    } else {
      // Add the variable to the symbol table
      symbolTable.add(node.varName.lexeme, node.dataType);
    }
  }

  // Visits a data type node
  public void visit(DataType node) {
    // Check if the type is valid (either base type or defined in structs)
    if (!isBaseType(node.type.lexeme) && !structs.containsKey(node.type.lexeme)) {
      // Report error if type is invalid
      error("Data type invalid: " + node.type.lexeme, node.type);
    }
    // Set the current type
    currType = node;
  }

  // check the return statement
  public void visit(ReturnStmt node) {
    // Disallow return statements outside of functions
    if (functionRet == null) {
      error("Return statement must be inside a function");
      return;
    }

    // Handle return with an expression
    if (node.expr != null) {
      node.expr.accept(this); // Type-check the return expression

      // Disallow returning a value from a void function
      if (functionRet.type.lexeme.equals("void") && !currType.type.lexeme.equals("null")) {
        error("Void functions cannot return a value");
      }
      // Enforce return type consistency, allow 'null' for nullable returns
      else if (!currType.type.lexeme.equals(functionRet.type.lexeme) && !currType.type.lexeme.equals("null")) {
        error("Return type mismatch: expected " + functionRet.type.lexeme +
            ", found " + currType.type.lexeme);
      }
    }
    // Handle return without an expression
    else {
      // Non-void functions must return a value
      if (!functionRet.type.lexeme.equals("void")) {
        error("Non-void function must return a value of type " + functionRet.type.lexeme);
      }
    }
  }

  // check expr types
  public void visit(Expr node) {
    // Check the type of expression and then visit it
    if (node instanceof BasicExpr) {
      visit((BasicExpr) node);
    } else if (node instanceof UnaryExpr) {
      visit((UnaryExpr) node);
    } else if (node instanceof BinaryExpr) {
      visit((BinaryExpr) node);
    }
  }

  // handles a basic expression
  public void visit(BasicExpr node) {
    node.rvalue.accept(this);
  }

  // unary expr (!)
  public void visit(UnaryExpr node) {
    // only valid unary expression is ! for myPl
    if (!node.unaryOp.lexeme.equals("not")) {
      error("only accepted unary expression is '!': " + node.unaryOp.lexeme, node.unaryOp);
    }
    node.expr.accept(this);
  }

  // makes sure the binary expression is using valid types
  public void visit(BinaryExpr node) {
    // Type-check the left-hand side expression
    node.lhs.accept(this);
    DataType lhs = currType;

    // Type-check the right-hand side expression
    node.rhs.accept(this);
    DataType rhs = currType;

    // Handle logical operators: and, or
    if (node.binaryOp.lexeme.equals("and") || node.binaryOp.lexeme.equals("or")) {
      if (!lhs.type.lexeme.equals("bool") || !rhs.type.lexeme.equals("bool")) {
        error("Logical operator '" + node.binaryOp.lexeme + "' requires boolean operands");
      }
      currType = new DataType();
      currType.type = new Token(TokenType.BOOL_TYPE, "bool", node.binaryOp.line, node.binaryOp.column);
      return;
    }

    // Handle equality operators: ==, !=
    if (node.binaryOp.lexeme.equals("==") || node.binaryOp.lexeme.equals("!=")) {
      if (!lhs.type.lexeme.equals(rhs.type.lexeme)
          && !lhs.type.lexeme.equals("null")
          && !rhs.type.lexeme.equals("null")) {
        error("Equality operator '" + node.binaryOp.lexeme + "' requires matching types or null");
      }
      currType = new DataType();
      currType.type = new Token(TokenType.BOOL_TYPE, "bool", node.binaryOp.line, node.binaryOp.column);
      return;
    }

    // Handle relational operators: <, <=, >, >=
    if (node.binaryOp.lexeme.equals("<") || node.binaryOp.lexeme.equals("<=") ||
        node.binaryOp.lexeme.equals(">") || node.binaryOp.lexeme.equals(">=")) {

      if (lhs.type.lexeme.equals("bool") || rhs.type.lexeme.equals("bool")) {
        error("Relational operator '" + node.binaryOp.lexeme + "' cannot be applied to boolean types");
      }
      if (lhs.isArray || rhs.isArray) {
        error("Relational operator '" + node.binaryOp.lexeme + "' cannot be applied to arrays");
      }

      currType = new DataType();
      currType.type = new Token(TokenType.BOOL_TYPE, "bool", node.binaryOp.line, node.binaryOp.column);
      return;
    }

    // Handle arithmetic/string operators: +, -, *, /
    if (node.binaryOp.lexeme.equals("+") || node.binaryOp.lexeme.equals("-") ||
        node.binaryOp.lexeme.equals("*") || node.binaryOp.lexeme.equals("/")) {

      if (lhs.type.lexeme.equals("bool") || rhs.type.lexeme.equals("bool")) {
        error("Arithmetic operator '" + node.binaryOp.lexeme + "' cannot be applied to boolean types");
      }
      if (lhs.isArray || rhs.isArray) {
        error("Arithmetic operator '" + node.binaryOp.lexeme + "' cannot be applied to arrays");
      }

      if (!lhs.type.lexeme.equals(rhs.type.lexeme)) {
        error("Type mismatch for operator '" + node.binaryOp.lexeme + "': " +
            lhs.type.lexeme + " vs " + rhs.type.lexeme);
      }

      currType = new DataType();

      // Determine result type based on operand type
      switch (lhs.type.lexeme) {
        case "int":
          currType.type = new Token(TokenType.INT_TYPE, "int", node.binaryOp.line, node.binaryOp.column);
          break;
        case "double":
          currType.type = new Token(TokenType.DOUBLE_TYPE, "double", node.binaryOp.line, node.binaryOp.column);
          break;
        case "string":
          if (node.binaryOp.lexeme.equals("+")) {
            currType.type = new Token(TokenType.STRING_TYPE, "string", node.binaryOp.line, node.binaryOp.column);
          } else {
            error("Operator '" + node.binaryOp.lexeme + "' is not valid for strings");
          }
          break;
        default:
          error("Unsupported operand type '" + lhs.type.lexeme + "' for operator '" + node.binaryOp.lexeme + "'");
      }
    }
  }

  // RValue check
  public void visit(RValue node) {
    // Check the type of RValue and go there
    if (node instanceof CallRValue) {
      visit((CallRValue) node);
    } else if (node instanceof SimpleRValue) {
      visit((SimpleRValue) node);
    } else if (node instanceof NewStructRValue) {
      visit((NewStructRValue) node);
    } else if (node instanceof NewArrayRValue) {
      visit((NewArrayRValue) node);
    } else if (node instanceof VarRValue) {
      visit((VarRValue) node);
    }
  }

  // checks callRValue, checks for built ins
  public void visit(CallRValue node) {
    String fname = node.funName.lexeme;

    // Handle built-in functions
    if (isBuiltInFunction(fname)) {

      // --- print / println ---
      if (fname.equals("print") || fname.equals("println")) {
        if (node.args.size() != 1)
          error("Function '" + fname + "' expects one argument", node.funName);
        node.args.get(0).accept(this);
        if (structs.containsKey(currType.type.lexeme) || currType.isArray)
          error("Cannot print struct or array types", node.funName);
        currType = new DataType();
        currType.type = new Token(TokenType.NULL_VAL, "null", node.funName.line, node.funName.column);
        return;
      }

      // --- readln ---
      if (fname.equals("readln")) {
        if (!node.args.isEmpty())
          error("Function 'readln' takes no arguments", node.funName);
        currType = new DataType();
        currType.type = new Token(TokenType.STRING_VAL, "string", node.funName.line, node.funName.column);
        return;
      }

      // --- str_val ---
      if (fname.equals("str_val")) {
        if (node.args.size() != 1)
          error("Function 'str_val' expects one argument", node.funName);
        node.args.get(0).accept(this);
        if (currType.type.lexeme.equals("string"))
          error("str_val argument must not be string", node.funName);
        currType = new DataType();
        currType.type = new Token(TokenType.STRING_TYPE, "string", node.funName.line, node.funName.column);
        return;
      }

      // --- int_val ---
      if (fname.equals("int_val")) {
        if (node.args.size() != 1)
          error("Function 'int_val' expects one argument", node.funName);
        node.args.get(0).accept(this);
        if (currType.type.lexeme.equals("int"))
          error("int_val argument must not be int", node.funName);
        currType = new DataType();
        currType.type = new Token(TokenType.INT_TYPE, "int", node.funName.line, node.funName.column);
        return;
      }

      // --- dbl_val ---
      if (fname.equals("dbl_val")) {
        if (node.args.size() != 1)
          error("Function 'dbl_val' expects one argument", node.funName);
        node.args.get(0).accept(this);
        if (currType.type.lexeme.equals("double"))
          error("dbl_val argument must not be double", node.funName);
        currType = new DataType();
        currType.type = new Token(TokenType.DOUBLE_TYPE, "double", node.funName.line, node.funName.column);
        return;
      }

      // --- size ---
      if (fname.equals("size")) {
        if (node.args.size() != 1)
          error("Function 'size' expects one argument", node.funName);
        node.args.get(0).accept(this);
        if (!currType.isArray && !currType.type.lexeme.equals("string"))
          error("size argument must be a string or an array", node.funName);
        currType = new DataType();
        currType.type = new Token(TokenType.INT_TYPE, "int", node.funName.line, node.funName.column);
        return;
      }

      // --- get ---
      if (fname.equals("get")) {
        if (node.args.size() != 2)
          error("Function 'get' expects two arguments", node.funName);

        // First arg must be int
        node.args.get(0).accept(this);
        if (!currType.type.lexeme.equals("int"))
          error("First argument to 'get' must be an int", node.funName);

        // Second arg must be array or string
        node.args.get(1).accept(this);
        if (!currType.isArray && !currType.type.lexeme.equals("string"))
          error("Second argument to 'get' must be a string or array", node.funName);

        // Set return type accordingly
        if (currType.type.lexeme.equals("string")) {
          currType = new DataType();
          currType.type = new Token(TokenType.STRING_TYPE, "string", node.funName.line, node.funName.column);
        } else {
          // Array element type is same as the array's base type
          Token baseType = currType.type;
          currType = new DataType();
          currType.type = new Token(baseType.tokenType, baseType.lexeme, node.funName.line, node.funName.column);
        }
        return;
      }

      // If built-in function matched none of the above
      error("Unknown built-in function: " + fname, node.funName);
      return;
    }

    // --- Handle user-defined functions ---

    // Ensure the function exists
    if (!functions.containsKey(fname))
      error("Undefined function: " + fname, node.funName);

    FunDef def = functions.get(fname);

    // Ensure argument count matches
    if (node.args.size() != def.params.size())
      error("Function '" + fname + "' expects " + def.params.size()
          + " arguments, got " + node.args.size(), node.funName);

    // Type-check each argument
    for (int i = 0; i < node.args.size(); i++) {
      node.args.get(i).accept(this);
      DataType expected = def.params.get(i).dataType;
      boolean typeMatches = currType.type.lexeme.equals(expected.type.lexeme);
      boolean arrayMatches = currType.isArray == expected.isArray;
      boolean isNull = currType.type.lexeme.equals("null");
      if (!(typeMatches && arrayMatches) && !isNull)
        error("Function '" + fname + "' argument " + (i + 1) + " type mismatch", node.funName);
    }

    // Set return type of function
    currType = new DataType();
    if (def.returnType.type.lexeme.equals("void")) {
      currType.type = new Token(TokenType.NULL_VAL, "null", node.funName.line, node.funName.column);
    } else {
      currType.type = def.returnType.type;
      currType.isArray = def.returnType.isArray;
    }
  }

  // checks SimpleRValue
  public void visit(SimpleRValue node) {
    String type = node.literal.tokenType.toString();

    if (type.equals("INT_VAL")) {
      currType = new DataType();
      currType.type = new Token(TokenType.INT_TYPE, "int", node.literal.line, node.literal.column);
    } else if (type.equals("DOUBLE_VAL")) {
      currType = new DataType();
      currType.type = new Token(TokenType.DOUBLE_TYPE, "double", node.literal.line, node.literal.column);
    } else if (type.equals("STRING_VAL")) {
      currType = new DataType();
      currType.type = new Token(TokenType.STRING_TYPE, "string", node.literal.line, node.literal.column);
    } else if (type.equals("BOOL_VAL")) {
      currType = new DataType();
      currType.type = new Token(TokenType.BOOL_TYPE, "bool", node.literal.line, node.literal.column);
    } else if (type.equals("NULL_VAL")) {
      currType = new DataType();
      currType.type = new Token(TokenType.NULL_VAL, "null", node.literal.line, node.literal.column);
    } else {
      error("Unknown literal type: " + type, node.literal);
    }
  }

  // Checks a NewStructRValue node for a valid type
  public void visit(NewStructRValue node) {
    // Check if struct is defined
    if (!structs.containsKey(node.structName.lexeme)) {
      error("Struct not found: " + node.structName.lexeme, node.structName);
      return;
    }

    // Retrieve struct definition directly from the map
    StructDef structDef = structs.get(node.structName.lexeme);

    // Check if number of arguments matches number of struct fields
    if (node.args.size() != structDef.fields.size()) {
      error("Struct constructor parameter mismatch: expected " +
          structDef.fields.size() + " but found " + node.args.size(), node.structName);
      return;
    }

    // Check each argument's type against the corresponding field type
    for (int i = 0; i < node.args.size(); i++) {
      node.args.get(i).accept(this);
      if (!currType.type.lexeme.equals(structDef.fields.get(i).dataType.type.lexeme) &&
          !currType.type.lexeme.equals("null")) {
        error("Struct constructor parameter type mismatch: expected " +
            structDef.fields.get(i).dataType.type.lexeme + " but found " +
            currType.type.lexeme, node.structName);
      }
    }

    // Set the resulting type as the struct
    currType = new DataType();
    currType.type = new Token(TokenType.ID, node.structName.lexeme, node.structName.line, node.structName.column);
  }

  // visit NewArrayRValue
  public void visit(NewArrayRValue node) {
    // if the type isn't a base type or a struct then error
    if (!(isBaseType(node.type.lexeme) || structs.containsKey(node.type.lexeme))) {
      error("Invalid array element type: " + node.type.lexeme);
    }

    // visit arrayExpr
    node.arrayExpr.accept(this);

    // make sure the current type isnt null
    if (currType == null || currType.type == null) {
      error("currType cannot be null", node.type);
      return;
    }

    // check that the array size (... = new int[_]) is an int
    if (!currType.type.lexeme.equals("int")) {
      error("Array size must be an int");
    }

    // reset currType
    currType = new DataType();
    currType.type = new Token(TokenType.INT_TYPE, node.type.lexeme, node.type.line, node.type.column);
    currType.isArray = true;
  }

  // watch VarRValue for a valid type
  public void visit(VarRValue node) {
    // Get the first variable in the path
    String varName = node.path.get(0).varName.lexeme;

    // Check if the variable exists in the symbol table
    if (!symbolTable.exists(node.path.get(0).varName.lexeme)) {
      error("Variable does not exist: " + node.path.get(0).varName.lexeme);
    }

    // Retrieve the variables type
    currType = symbolTable.get(varName);

    // Check if the first part of the path involves array indexing
    if (node.path.get(0).arrayExpr.isPresent()) {
      if (!currType.isArray) {
        error("Variable is not an array " + varName);
      }

      // Prepare to handle the element type of the array
      DataType elemType = new DataType();
      elemType.type = currType.type;
      elemType.isArray = false;

      // accept the array index
      node.path.get(0).arrayExpr.get().accept(this);

      // check that the array index is an int
      if (!currType.type.lexeme.equals("int") || currType.isArray) {
        error("Array index must be an integer", node.path.get(0).varName);
      }

      // Set the current type
      currType = elemType;
    }

    // Process further elements in the path (i.e., struct field accesses)
    for (int i = 1; i < node.path.size(); i++) {
      VarRef ref = node.path.get(i);

      // Handle array indexing on an array type
      if (currType.isArray) {
        if (!ref.arrayExpr.isPresent()) {
          error("Cannot access field on array type", ref.varName);
        }

        // Prepare to handle the element type of the array
        DataType elemType = new DataType();
        elemType.type = currType.type;
        elemType.isArray = false;

        // Process the array index for the field
        ref.arrayExpr.get().accept(this);

        // Ensure the array index is of type 'int'
        if (!currType.type.lexeme.equals("int") || currType.isArray) {
          error("Array index must be an integer", ref.varName);
        }

        // Set the current type to the element type
        currType = elemType;
      }

      // Ensure the current type is a struct for field access
      if (!structs.containsKey(currType.type.lexeme)) {
        error("Must be a struct to access fields: " + ref.varName.lexeme, ref.varName);
      }

      // Retrieve the struct definition
      StructDef def = structs.get(currType.type.lexeme);

      // Check if the field exists within the struct
      if (!isStructField(ref.varName.lexeme, def)) {
        error("Field does not exist: " + ref.varName.lexeme, ref.varName);
      }

      // Set the current type to the field type
      currType = getStructFieldType(ref.varName.lexeme, def);
    }
  }

  // VarStmt visit
  public void visit(VarStmt node) {
    // Check if the variable name is already declared in the current environment
    if (symbolTable.existsInCurrEnv(node.varName.lexeme)) {
      error("Illegal variable name (already exists): " + node.varName.lexeme);
    }

    // If the declaration includes an explicit type
    if (node.dataType.isPresent()) {
      // Check if the type is either a base type or a user-defined struct
      if (!isBaseType(node.dataType.get().type.lexeme) && !structs.containsKey(node.dataType.get().type.lexeme)) {
        error("Variable type not supported here: " + node.dataType.get().type.lexeme, node.dataType.get().type);
      } else {
        // If theres also an expression being assigned to the variable
        if (node.expr.isPresent()) {
          // Visit the expression to compute its type
          node.expr.get().accept(this);

          // Check that the expressions type matches the declared type, or is null
          if (!currType.type.lexeme.equals(node.dataType.get().type.lexeme) && !currType.type.lexeme.equals("null")) {
            error("Variable type mismatch: expected " + node.dataType.get().type.lexeme + " but found "
                + currType.type.lexeme);
          }

          // If the declared type is an array, but the expression is not, and not null
          if (node.dataType.get().isArray && !currType.isArray && !currType.type.lexeme.equals("null")) {
            error("Array type mismatch: expected array but found " + currType.type.lexeme);
          }

          // If the expression is an array but the declared type is not
          if (currType.isArray && !node.dataType.get().isArray && !node.dataType.get().type.lexeme.equals("null")) {
            error("Array type mismatch: expected non-array but found array");
          }
        }

        // Add the variable with its declared type to the symbol table
        symbolTable.add(node.varName.lexeme, node.dataType.get());
      }
    } else {
      // If no type is declared an expression must be provided
      if (!node.expr.isPresent()) {
        error("Variable declaration is incomplete without a type or expression");
      } else {
        // Visit the expression to infer its type
        node.expr.get().accept(this);

        // Cannot infer type from a null expression
        if (currType.type.lexeme.equals("null")) {
          error("Null expression was found; does not support type inference");
        }

        // Add the variable with the inferred type to the symbol table
        symbolTable.add(node.varName.lexeme, currType);
      }
    }
  }

  // Visit Assign Stmt
  public void visit(AssignStmt node) {
    // Get the name of the first variable in the lvalue chain
    if (!symbolTable.exists(node.lvalue.get(0).varName.lexeme)) {
      // If the variable hasn't been declared, it's an error
      error("Undefined variable: " + node.lvalue.get(0).varName.lexeme);
      return;
    }

    // Start by setting currType to the type of the variable being assigned to
    currType = symbolTable.get(node.lvalue.get(0).varName.lexeme);

    // Handle array indexing if the first variable has an array access
    if (node.lvalue.get(0).arrayExpr.isPresent()) {
      if (!currType.isArray)
        error("Variable is not an array: " + node.lvalue.get(0).varName.lexeme);

      // Create a new DataType representing the element type of the array
      DataType elemType = new DataType();
      elemType.type = currType.type;
      elemType.isArray = false;

      // Check the index expression
      node.lvalue.get(0).arrayExpr.get().accept(this);

      // The index must be of type int and not an array
      if (!currType.type.lexeme.equals("int") || currType.isArray)
        error("Array index must be an integer");

      // Update currType to the element type
      currType = elemType;
    }

    // Process the rest of the lvalue chain (for field and/or array accesses)
    for (int i = 1; i < node.lvalue.size(); i++) {
      // If this part of the chain is an array access
      if (node.lvalue.get(i).arrayExpr.isPresent()) {
        if (!currType.isArray)
          error("Trying to index non-array type", node.lvalue.get(i).varName);

        // Check the index expression
        node.lvalue.get(i).arrayExpr.get().accept(this);

        // Index must be int and not an array
        if (!currType.type.lexeme.equals("int") || currType.isArray)
          error("Array index must be an integer", node.lvalue.get(i).varName);

        // Step into the element type of the array
        DataType elemType = new DataType();
        elemType.type = currType.type;
        elemType.isArray = false;
        currType = elemType;
      }

      // After array handling, we might be accessing a struct field
      if (currType.isArray)
        error("Cannot access field on array type", node.lvalue.get(i).varName);

      // Make sure the current type is a struct
      if (!structs.containsKey(currType.type.lexeme))
        error("Invalid field reference: " + node.lvalue.get(i).varName.lexeme, node.lvalue.get(i).varName);

      // Make sure the field exists in the struct
      if (!isStructField(node.lvalue.get(i).varName.lexeme, structs.get(currType.type.lexeme)))
        error("Undefined field: " + node.lvalue.get(i).varName.lexeme, node.lvalue.get(i).varName);

      // Update currType to the type of the accessed field
      currType = getStructFieldType(node.lvalue.get(i).varName.lexeme, structs.get(currType.type.lexeme));
    }

    // Save the final type of the left-hand side of the assignment
    DataType lhsType = currType;

    // Visit the right-hand side expression to determine its type
    node.expr.accept(this);
    DataType rhsType = currType;

    // Check type compatibility between lhs and rhs
    if (!lhsType.type.lexeme.equals(rhsType.type.lexeme) && !rhsType.type.lexeme.equals("null"))
      error("Type mismatch: expected " + lhsType.type.lexeme + " but found " + rhsType.type.lexeme);

    // Check array/non-array mismatch
    if (lhsType.isArray != rhsType.isArray && !rhsType.type.lexeme.equals("null"))
      error("Type mismatch: base type to array type");
  }

  // Visit while statement node
  public void visit(WhileStmt node) {
    // Check the condition expression
    node.condition.accept(this);

    // Ensure the condition is of type bool
    if (!currType.type.lexeme.equals("bool")) {
      error("Conditions must be of type bool: " + currType.type.lexeme);
    }

    // Ensure the condition is not an array
    if (currType.isArray) {
      error("The condition must not be an array " + currType.type.lexeme);
    }

    // Enter a new environment for the while block
    symbolTable.pushEnvironment();

    // Visit all statements inside the while loop
    for (Stmt stmt : node.stmts) {
      stmt.accept(this);
    }

    // Exit the while block environment
    symbolTable.popEnvironment();
  }

  // Visit for statement node
  public void visit(ForStmt node) {

    // Enter a new environment for the for loop
    symbolTable.pushEnvironment();

    // Create and add a new variable of type int for the loop variable
    DataType symbolType = new DataType();
    symbolType.type = new Token(TokenType.INT_TYPE, "int", node.varName.line, node.varName.column);
    symbolTable.add(node.varName.lexeme, symbolType);

    // Visit and type check the start expression
    node.fromExpr.accept(this);
    if (!currType.type.lexeme.equals("int")) {
      error("Start expression must be an int but found " + currType.type.lexeme);
    }

    // Visit and type check the end expression
    node.toExpr.accept(this);
    if (!currType.type.lexeme.equals("int")) {
      error("End expression must be an int but found " + currType.type.lexeme);
    }

    // Visit each statement in the for loop body
    for (Stmt stmt : node.stmts) {
      stmt.accept(this);
    }

    // Exit the for loop environment
    symbolTable.popEnvironment();
  }

  // Visit an if statement node
  public void visit(IfStmt node) {
    // Visit and check the condition
    node.condition.accept(this);

    // Condition must be bool
    if (!currType.type.lexeme.equals("bool")) {
      error("Expected a boolean, found: " + currType.type.lexeme);
    }

    // Condition must not be an array
    if (currType.isArray) {
      error("Conditions cannot be arrays" + currType.type.lexeme);
    }

    // Visit the "if" block with a new environment
    symbolTable.pushEnvironment();
    for (Stmt stmt : node.ifStmts) {
      stmt.accept(this);
    }
    symbolTable.popEnvironment();

    // Visit the optional else-if branch if present
    if (node.elseIf.isPresent()) {
      node.elseIf.get().accept(this);
    }

    // Visit the optional else block if present
    if (node.elseStmts.isPresent()) {
      symbolTable.pushEnvironment();
      for (Stmt stmt : node.elseStmts.get()) {
        stmt.accept(this);
      }
      symbolTable.popEnvironment();
    }
  }

}