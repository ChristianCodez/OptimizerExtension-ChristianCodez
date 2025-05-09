package cpsc326;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.xml.crypto.Data;

import java.util.HashSet;

public class tmp implements Visitor {

    // for tracking function and struct definitions:
    private Map<String, FunDef> functions = new HashMap<>();
    private Map<String, StructDef> structs = new HashMap<>();
    // for tracking variable types:
    private SymbolTable symbolTable = new SymbolTable();
    // for holding the last inferred type:
    private DataType currType;
    // New field for tracking the current function return type
    private DataType currentFunctionReturnType = null; // Add this field

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

    /**
     * Converts a type name string to its corresponding TokenType.
     * 
     * @param typeName the name of the type as a string
     * @return the corresponding TokenType for the given type name
     */
    private TokenType getTokenTypeFromName(String typeName) {
        return switch (typeName) {
            case "int" -> TokenType.INT_TYPE;
            case "double" -> TokenType.DOUBLE_TYPE;
            case "bool" -> TokenType.BOOL_TYPE;
            case "string" -> TokenType.STRING_TYPE;
            case "void" -> TokenType.VOID_TYPE;
            case "null" -> TokenType.NULL_VAL;
            default -> TokenType.ID; // struct names
        };
    }

    // ----------------------------------------------------------------------
    // Visit Functions
    // ----------------------------------------------------------------------

    /**
     * Checks the program
     */
    public void visit(Program node) {
        // 1. record each struct definitions and check for duplicate names
        for (StructDef s : node.structs) {
            if (structs.containsKey(s.structName.lexeme)) {
                error("Duplicate struct name: " + s.structName.lexeme, s.structName);
            } else {
                structs.put(s.structName.lexeme, s);
            }
        }
        // 2. record each function definition and check for duplicate names
        for (FunDef f : node.functions) {
            if (isBuiltInFunction(f.funName.lexeme)) {
                error("Cannot redefine built-in function: " + f.funName.lexeme, f.funName);
            } else if (functions.containsKey(f.funName.lexeme)) {
                error("Duplicate function name: " + f.funName.lexeme, f.funName);
            } else {
                functions.put(f.funName.lexeme, f);
            }
        }
        // 3. check for a main function
        if (!functions.containsKey("main")) {
            error("No main function defined");
        }
        // 4. check each struct
        for (StructDef s : node.structs) {
            s.accept(this);
        }
        // check each function
        for (FunDef f : node.functions) {
            f.accept(this);
        }
    }

    /**
     * Checks a function definition signature and body ensuring valid
     * data types and no duplicate parameters
     */
    public void visit(FunDef node) {
        String funName = node.funName.lexeme;
        String returnType = node.returnType.type.lexeme;

        // 0. Check the return type
        if (!isBaseType(returnType) && !structs.containsKey(returnType) && !returnType.equals("void")) {
            error("Invalid return type: " + returnType, node.returnType.type);
        }

        currType = node.returnType;

        // 1. check signature if it is main
        if (funName.equals("main")) {
            if (node.params.size() > 0) {
                error("Main function cannot have parameters", node.funName);
            }
            if (!returnType.equals("void")) {
                error("Main function must return void", node.funName);
            }
        }

        // Store the function's return type specifically
        this.currentFunctionReturnType = node.returnType;

        // 2. add an environment for params
        symbolTable.pushEnvironment();
        // 3. check and add the params (no duplicate param var names)
        Set<String> paramNames = new HashSet<>();
        for (VarDef v : node.params) {
            String paramName = v.varName.lexeme;
            String paramType = v.dataType.type.lexeme;
            if (paramNames.contains(paramName)) {
                error("Duplicate parameter name: " + paramName, v.varName);
            } else {
                paramNames.add(paramName);
            }
            if (!isBaseType(paramType) && !structs.containsKey(paramType)) {
                error("Invalid parameter type: " + paramType, v.dataType.type);
            } else {
                // Create a new DataType instance to ensure it has a valid Token
                DataType dt = new DataType();
                dt.type = new Token(getTokenTypeFromName(paramType), paramType, v.dataType.type.line,
                        v.dataType.type.column);
                dt.isArray = v.dataType.isArray;
                symbolTable.add(paramName, dt);
            }
        }
        // 5. check the body statements
        for (Stmt s : node.stmts) {
            s.accept(this);
        }
        this.currentFunctionReturnType = null; // Reset after function body check
        // 6. Pop the environment
        symbolTable.popEnvironment();
    }

    /**
     * Checks structs for duplicate fields and valid data types
     */
    public void visit(StructDef node) {
        // 1. check for duplicate field names
        Set<String> fieldNames = new HashSet<>();
        for (VarDef v : node.fields) {
            String fieldName = v.varName.lexeme;
            if (fieldNames.contains(fieldName)) {
                error("Duplicate field name: " + fieldName, v.varName);
            } else {
                fieldNames.add(fieldName);
            }
            // 2. check the field data type
            String fieldType = v.dataType.type.lexeme;
            if (!isBaseType(fieldType) && !structs.containsKey(fieldType)) {
                error("Invalid field type: " + fieldType, v.dataType.type);
            }
        }
    }

    /**
     * Checks a variable definition for valid data types
     */
    public void visit(VarDef node) {
        String varName = node.varName.lexeme;
        String varType = node.dataType.type.lexeme;
        if (!isBaseType(varType) && !structs.containsKey(varType)) {
            error("Invalid variable type: " + varType, node.dataType.type);
        } else {
            symbolTable.add(varName, node.dataType);
        }
    }

    /**
     * Checks a DataType node for a valid type and sets the currType
     */
    public void visit(DataType node) {
        String type = node.type.lexeme;
        if (!isBaseType(type) && !structs.containsKey(type)) {
            error("Invalid data type: " + type, node.type);
        }
        currType = node;
    }

    /**
     * Checks a ReturnStmt node for valid return types
     */
    public void visit(ReturnStmt node) {
        // Ensure we are inside a function
        if (this.currentFunctionReturnType == null) {
            error("Return statement must be inside a function");
            return;
        }

        // Store the expected function return type
        DataType expectedType = this.currentFunctionReturnType;

        // If there's a return expression, check its type
        if (node.expr != null) {
            node.expr.accept(this); // Visit expression to infer return type
            DataType returnType = currType;

            // Void functions should not return a value
            if (expectedType.type.lexeme.equals("void") && returnType.type.lexeme != ("null")) {
                error("Return statement in void function cannot have an expression");
            }
            // Allow returning null for nullable types
            else if (!returnType.type.lexeme.equals(expectedType.type.lexeme)
                    && !returnType.type.lexeme.equals("null")) {
                error("Return type mismatch: expected " + expectedType.type.lexeme + " but found "
                        + returnType.type.lexeme);
            }
        }
        // If no return expression is provided, function must be void
        else {
            if (!expectedType.type.lexeme.equals("void")) {
                error("Non-void function must return a value of type " + expectedType.type.lexeme);
            }
        }
    }

    /**
     * Checks an expression node for valid types (placeholder)
     */
    public void visit(Expr node) {
        // Check expression type and visit that expression
        if (node instanceof BasicExpr) {
            visit((BasicExpr) node);
        } else if (node instanceof UnaryExpr) {
            visit((UnaryExpr) node);
        } else if (node instanceof BinaryExpr) {
            visit((BinaryExpr) node);
        }
    }

    /**
     * Checks a BasicExpr statement for valid types
     */
    public void visit(BasicExpr node) {
        node.rvalue.accept(this);
    }

    /**
     * Checks a UnaryExpr statement for valid types
     */
    public void visit(UnaryExpr node) {
        // Check the unary operator and the expression
        if (!node.unaryOp.lexeme.equals("not")) {
            error("Invalid unary operator: " + node.unaryOp.lexeme, node.unaryOp);
        }
        node.expr.accept(this);
    }

    /**
     * Checks a BinaryExpr statement for valid types
     */
    public void visit(BinaryExpr node) {
        // Check the left-hand side, the binary operator, and the right-hand side
        node.lhs.accept(this);
        DataType lhsType = currType;
        node.rhs.accept(this);
        DataType rhsType = currType;
        String op = node.binaryOp.lexeme;
        if (op.equals("and") || op.equals("or")) {
            if (!lhsType.type.lexeme.equals("bool") || !rhsType.type.lexeme.equals("bool")) {
                error("Invalid types for binary operator: " + op);
            }
            currType = new DataType();
            currType.type = new Token(TokenType.BOOL_TYPE, "bool", node.binaryOp.line, node.binaryOp.column);
            return;
        }
        if (op.equals("==") || op.equals("!=")) {
            if (!lhsType.type.lexeme.equals(rhsType.type.lexeme) && !lhsType.type.lexeme.equals("null")
                    && !rhsType.type.lexeme.equals("null")) {
                error("Invalid types for binary operator: " + op);
            }
            currType = new DataType();
            currType.type = new Token(TokenType.BOOL_TYPE, "bool", node.binaryOp.line, node.binaryOp.column);
            return;
        }
        if (op.equals("<") || op.equals("<=") || op.equals(">")
                || op.equals(">=")) {
            if (lhsType.type.lexeme.equals("bool") || rhsType.type.lexeme.equals("bool")) {
                error("Invalid types for binary operator: " + op);
            }
            if (lhsType.isArray || rhsType.isArray) {
                error("Invalid types for binary operator: " + op);
            }
            currType = new DataType();
            currType.type = new Token(TokenType.BOOL_TYPE, "bool", node.binaryOp.line, node.binaryOp.column);
            return;
        }

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
            if (lhsType.type.lexeme.equals("bool") || rhsType.type.lexeme.equals("bool")) {
                error("Invalid types for binary operator: " + op);
            }
            if (lhsType.isArray || rhsType.isArray) {
                error("Invalid types for binary operator: " + op);
            }

            // Ensure both operands have the same type
            if (!lhsType.type.lexeme.equals(rhsType.type.lexeme)) {
                error("Mismatched types for binary operator: " + lhsType.type.lexeme + " and " + rhsType.type.lexeme);
            }

            currType = new DataType();
            switch (lhsType.type.lexeme) {
                case "int":
                    currType.type = new Token(TokenType.INT_TYPE, "int", node.binaryOp.line, node.binaryOp.column);
                    break;
                case "double":
                    currType.type = new Token(TokenType.DOUBLE_TYPE, "double", node.binaryOp.line,
                            node.binaryOp.column);
                    break;
                case "string":
                    if (op.equals("+")) {
                        currType.type = new Token(TokenType.STRING_TYPE, "string", node.binaryOp.line,
                                node.binaryOp.column);
                    } else {
                        error("Invalid operator '" + op + "' for strings");
                    }
                    break;
                default:
                    error("Invalid types for binary operator: " + op);
            }
        }

    }

    /**
     * Checks a Rvalue node for valid types (Placeholder)
     */
    public void visit(RValue node) {
        // Check the RValue type and visit that RValue
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

    /**
     * Checks a CallRValue node for valid types
     */
    public void visit(CallRValue node) {
        String funName = node.funName.lexeme;
        if (isBuiltInFunction(funName)) {

            // !Checks for all built-in functions!

            if (funName.equals("print") || funName.equals("println")) {
                for (Expr arg : node.args) {
                    arg.accept(this);
                    // If it's a struct type or array type, throw an error
                    if (structs.containsKey(currType.type.lexeme) || currType.isArray) {
                        error("Cannot pass struct type to print function '" + funName + "'", node.funName);
                    }
                }
                if (node.args.size() != 1) {
                    error("Print functions accept exactly one argument");
                }
                currType = new DataType();
                currType.type = new Token(TokenType.NULL_VAL, "null", node.funName.line, node.funName.column);
                return;
            } else if (funName.equals("readln")) {
                if (node.args.size() != 0) {
                    error("Read line function does not accept arguments");
                }
                currType = new DataType();
                currType.type = new Token(TokenType.STRING_VAL, "string", node.funName.line, node.funName.column);
                return;
            } else if (funName.equals("str_val")) {
                if (node.args.size() != 1)
                    error("str_val expects one argument", node.funName);
                node.args.get(0).accept(this);
                if (currType.type.lexeme.equals("string"))
                    error("str_val argument must not be of type string", node.funName);
                currType = new DataType();
                currType.type = new Token(TokenType.STRING_TYPE, "string", node.funName.line, node.funName.column);
                return;

            } else if (funName.equals("int_val")) {
                if (node.args.size() != 1)
                    error("int_val expects one argument", node.funName);
                node.args.get(0).accept(this);
                if (currType.type.lexeme.equals("int"))
                    error("int_val argument must not be of type int", node.funName);
                currType = new DataType();
                currType.type = new Token(TokenType.INT_TYPE, "int", node.funName.line, node.funName.column);
                return;

            } else if (funName.equals("dbl_val")) {
                if (node.args.size() != 1)
                    error("dbl_val expects one argument", node.funName);
                node.args.get(0).accept(this);
                if (currType.type.lexeme.equals("double"))
                    error("dbl_val argument must not be of type double", node.funName);
                currType = new DataType();
                currType.type = new Token(TokenType.DOUBLE_TYPE, "double", node.funName.line, node.funName.column);
                return;

            } else if (funName.equals("size")) {
                if (node.args.size() != 1)
                    error("size expects one argument", node.funName);
                node.args.get(0).accept(this);
                if (!(currType.type.lexeme.equals("string") || currType.isArray))
                    error("size argument must be a string or an array", node.funName);
                currType = new DataType();
                currType.type = new Token(TokenType.INT_TYPE, "int", node.funName.line, node.funName.column);
                return;

            } else if (funName.equals("get")) {
                if (node.args.size() != 2)
                    error("get expects two arguments", node.funName);

                // First argument: must be int
                node.args.get(0).accept(this);
                DataType indexType = currType;
                if (!indexType.type.lexeme.equals("int"))
                    error("first argument of get must be an int", node.funName);

                // Second argument: must be string or array
                node.args.get(1).accept(this);
                DataType targetType = currType;

                // Null checks before access
                if (targetType == null || targetType.type == null) {
                    error("Second argument to get must be a string or an array", node.funName);
                }

                boolean isValidTarget = targetType.type.lexeme.equals("string") || targetType.isArray;

                if (!isValidTarget)
                    error("second argument of get must be a string or an array", node.funName);

                // Return type depends on second argument
                currType = new DataType();
                if (targetType.type.lexeme.equals("string")) {
                    currType.type = new Token(TokenType.STRING_TYPE, "string", node.funName.line, node.funName.column);
                } else {
                    // Array: return its element type
                    currType.type = new Token(targetType.type.tokenType, targetType.type.lexeme, node.funName.line,
                            node.funName.column);
                    currType.isArray = false;
                }
                return;
            }

        } else if (!functions.containsKey(funName)) {
            error("Undefined function: " + funName, node.funName);
        } else {
            FunDef f = functions.get(funName);
            if (f.params.size() != node.args.size()) {
                error("Function call argument mismatch: expected " + f.params.size() + " but found " + node.args.size(),
                        node.funName);
            } else {
                for (int i = 0; i < f.params.size(); i++) {
                    node.args.get(i).accept(this);
                    DataType expected = f.params.get(i).dataType;
                    if ((!currType.type.lexeme.equals(expected.type.lexeme) ||
                            currType.isArray != expected.isArray) && !currType.type.lexeme.equals("null")) {
                        error("Function call argument type mismatch: expected array");
                    }
                }
            }
            // Set return type as currType
            currType = new DataType();
            if (f.returnType.type.lexeme.equals("void")) {
                // Treat void as returning null
                currType.type = new Token(TokenType.NULL_VAL, "null", node.funName.line, node.funName.column);
            } else {
                currType.type = f.returnType.type;
                currType.isArray = f.returnType.isArray;
            }
        }
    }

    /**
     * Checks a SimpleRValue node for a valid type
     */
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

    /**
     * Checks a NewStructRValue node for a valid type
     */
    public void visit(NewStructRValue node) {
        // Check the struct type
        String structName = node.structName.lexeme;
        if (!structs.containsKey(structName)) {
            error("Undefined struct: " + structName, node.structName);
        } else {
            StructDef s = structs.get(structName);
            if (s.fields.size() != node.args.size()) {
                error("Struct constructor argument mismatch: expected " + s.fields.size() + " but found "
                        + node.args.size(),
                        node.structName);
            } else {
                for (int i = 0; i < s.fields.size(); i++) {
                    node.args.get(i).accept(this);
                    if (!currType.type.lexeme.equals(s.fields.get(i).dataType.type.lexeme)
                            && !currType.type.lexeme.equals("null")) {
                        error("Struct constructor argument type mismatch: expected "
                                + s.fields.get(i).dataType.type.lexeme
                                + " but found " + currType.type.lexeme, node.structName);
                    }
                }
            }
            currType = new DataType();
            currType.type = new Token(TokenType.ID, structName, node.structName.line, node.structName.column);
        }
    }

    /**
     * Checks a NewArrayRValue node for a valid type
     */
    public void visit(NewArrayRValue node) {

        // 1. Get array element type from node
        String elementType = node.type.lexeme;
        // 2. Check if the element type is valid (base type or struct)
        // a. If not valid, report an error
        if (!(isBaseType(elementType) || structs.containsKey(elementType))) {
            error("Invalid array element type: " + elementType);
        }
        // 3. Visit the size expression and check its type
        node.arrayExpr.accept(this);

        // 4. Ensure `currType` is not null before checking its type
        if (currType == null || currType.type == null) {
            error("Expression evaluation resulted in an unknown type", node.type);
            return;
        }

        // 5. Ensure array size is an integer
        if (!currType.type.lexeme.equals("int")) {
            error("Array size must be an integer");
        }

        // 6. Set currType to an array of the given type
        currType = new DataType();
        currType.type = new Token(TokenType.INT_TYPE, elementType, node.type.line, node.type.column);
        currType.isArray = true;

    }

    /**
     * Checks a VarRValue node for a valid type
     */
    public void visit(VarRValue node) {
        String varName = node.path.get(0).varName.lexeme;
        if (!symbolTable.exists(varName))
            error("Undefined variable: " + varName);
        currType = symbolTable.get(varName);
        if (node.path.get(0).arrayExpr.isPresent()) {
            if (!currType.isArray)
                error("Variable is not an array: " + varName);
            DataType elemType = new DataType();
            elemType.type = currType.type;
            elemType.isArray = false;
            node.path.get(0).arrayExpr.get().accept(this);
            if (!currType.type.lexeme.equals("int") || currType.isArray)
                error("Array index must be an integer", node.path.get(0).varName);
            currType = elemType;
        }
        for (int i = 1; i < node.path.size(); i++) {
            VarRef ref = node.path.get(i);
            if (currType.isArray) {
                if (!ref.arrayExpr.isPresent())
                    error("Cannot access field on array type", ref.varName);
                DataType elemType = new DataType();
                elemType.type = currType.type;
                elemType.isArray = false;
                ref.arrayExpr.get().accept(this);
                if (!currType.type.lexeme.equals("int") || currType.isArray)
                    error("Array index must be an integer", ref.varName);
                currType = elemType;
            }
            if (!structs.containsKey(currType.type.lexeme))
                error("Invalid field reference: " + ref.varName.lexeme, ref.varName);
            StructDef def = structs.get(currType.type.lexeme);
            if (!isStructField(ref.varName.lexeme, def))
                error("Undefined field: " + ref.varName.lexeme, ref.varName);
            currType = getStructFieldType(ref.varName.lexeme, def);
        }
    }

    /*******************************
     * Statement node visit methods
     ****************************/

    /**
     * Checks a VarStmt node for a valid type
     */
    public void visit(VarStmt node) {
        String varName = node.varName.lexeme;
        if (symbolTable.existsInCurrEnv(varName)) {
            error("Duplicate variable name: " + varName);
        }
        if (node.dataType.isPresent()) {
            String varType = node.dataType.get().type.lexeme; // Unwrapping Optional
            Boolean isArray = node.dataType.get().isArray;
            if (!isBaseType(varType) && !structs.containsKey(varType)) {
                error("Invalid variable type: " + varType, node.dataType.get().type);
            } else {
                if (node.expr.isPresent()) {
                    node.expr.get().accept(this);
                    if (!currType.type.lexeme.equals(varType) && !currType.type.lexeme.equals("null")) {
                        error("Variable type mismatch: expected " + varType + " but found " + currType.type.lexeme);
                    }
                    if (isArray && !currType.isArray && !currType.type.lexeme.equals("null")) {
                        error("Array type mismatch: expected array but found " + currType.type.lexeme);
                    }
                    if (currType.isArray && !isArray && !varType.equals("null")) {
                        error("Array type mismatch: expected array but found " + varType);
                    }
                }
                symbolTable.add(varName, node.dataType.get());
            }
        } else {
            if (!node.expr.isPresent()) {
                error("Variable declaration must have a type or an expression");
            } else {
                node.expr.get().accept(this);
                if (currType.type.lexeme.equals("null")) {
                    error("Variable type cannot be inferred from null expression");
                }
                symbolTable.add(varName, currType);
            }
        }
    }

    /**
     * Checks a AssignStmt node for a valid type
     */
    public void visit(AssignStmt node) {
        String varName = node.lvalue.get(0).varName.lexeme;
        if (!symbolTable.exists(varName)) {
            error("Undefined variable: " + varName);
            return;
        }
        currType = symbolTable.get(varName);
        if (node.lvalue.get(0).arrayExpr.isPresent()) {
            if (!currType.isArray)
                error("Variable is not an array: " + varName);
            DataType elemType = new DataType();
            elemType.type = currType.type;
            elemType.isArray = false;
            node.lvalue.get(0).arrayExpr.get().accept(this);
            if (!currType.type.lexeme.equals("int") || currType.isArray)
                error("Array index must be an integer");
            currType = elemType;
        }
        for (int i = 1; i < node.lvalue.size(); i++) {
            VarRef ref = node.lvalue.get(i);
            if (currType.isArray) {
                if (!ref.arrayExpr.isPresent())
                    error("Cannot access field on array type", ref.varName);
                DataType elemType = new DataType();
                elemType.type = currType.type;
                elemType.isArray = false;
                ref.arrayExpr.get().accept(this);
                if (!currType.type.lexeme.equals("int") || currType.isArray)
                    error("Array index must be an integer", ref.varName);
                currType = elemType;
            }
            if (!structs.containsKey(currType.type.lexeme))
                error("Invalid field reference: " + ref.varName.lexeme, ref.varName);
            StructDef def = structs.get(currType.type.lexeme);
            if (!isStructField(ref.varName.lexeme, def))
                error("Undefined field: " + ref.varName.lexeme, ref.varName);
            currType = getStructFieldType(ref.varName.lexeme, def);
        }
        DataType lhsType = currType;
        node.expr.accept(this);
        DataType rhsType = currType;
        if (!lhsType.type.lexeme.equals(rhsType.type.lexeme) && !rhsType.type.lexeme.equals("null"))
            error("Assignment type mismatch: expected " + lhsType.type.lexeme + " but found " + rhsType.type.lexeme);
        if (lhsType.isArray != rhsType.isArray && !rhsType.type.lexeme.equals("null"))
            error("Assignment type mismatch, base type to array type");
    }

    /**
     * Checks a BlockStmt node for a valid type
     */
    public void visit(WhileStmt node) {
        node.condition.accept(this);
        if (!currType.type.lexeme.equals("bool")) {
            error("Invalid while condition type: expected bool but found " + currType.type.lexeme);
        }
        if (currType.isArray) {
            error("Invalid while condition type: expected bool but found an array value " + currType.type.lexeme);
        }
        symbolTable.pushEnvironment();
        for (Stmt s : node.stmts) {
            s.accept(this);
        }
        symbolTable.popEnvironment();
    }

    /**
     * Checks a ForStmt node for a valid type
     */
    public void visit(ForStmt node) {
        String varName = node.varName.lexeme;
        symbolTable.pushEnvironment();

        DataType symbolType = new DataType();
        symbolType.type = new Token(TokenType.INT_TYPE, "int", node.varName.line, node.varName.column);
        symbolTable.add(varName, symbolType);

        node.fromExpr.accept(this);
        if (!currType.type.lexeme.equals("int")) {
            error("Invalid for loop start expression type: expected int but found " + currType.type.lexeme);
        }
        node.toExpr.accept(this);
        if (!currType.type.lexeme.equals("int")) {
            error("Invalid for loop end expression type: expected int but found " + currType.type.lexeme);
        }
        for (Stmt s : node.stmts) {
            s.accept(this);
        }
        symbolTable.popEnvironment();
    }

    /**
     * Checks a IfStmt node for a valid type
     */
    public void visit(IfStmt node) {
        node.condition.accept(this);
        if (!currType.type.lexeme.equals("bool")) {
            error("Invalid if condition type: expected bool but found " + currType.type.lexeme);
        }
        if (currType.isArray) {
            error("Invalid if condition type: expected bool but found an array value " + currType.type.lexeme);
        }
        symbolTable.pushEnvironment();
        for (Stmt s : node.ifStmts) {
            s.accept(this);
        }
        symbolTable.popEnvironment();
        if (node.elseIf.isPresent()) {
            node.elseIf.get().accept(this);
        }
        if (node.elseStmts.isPresent()) {
            symbolTable.pushEnvironment();
            for (Stmt s : node.elseStmts.get()) {
                s.accept(this);
            }
            symbolTable.popEnvironment();
        }
    }

}
