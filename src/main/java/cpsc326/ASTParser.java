/**
 * CPSC 326, Spring 2025
 * The AST Parser implementation.
 *
 * Christian Carrington
 */

package cpsc326;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Simple recursive descent parser for checking program syntax.
 */
public class ASTParser {

  private Lexer lexer; // the lexer
  private Token currToken; // the current token

  /**
   * Create a SimpleParser from the give lexer.
   * 
   * @param lexer The lexer for the program to parse.
   */
  public ASTParser(Lexer lexer) {
    this.lexer = lexer;
  }

  /**
   * Run the parser.
   */
  public Program parse() {
    advance();
    Program p = program();
    eat(TokenType.EOS, "expecting end of file");
    return p;
  }

  /**
   * Generate and throw a mypl parser exception.
   * 
   * @param msg The error message.
   */
  private void error(String msg) {
    String lexeme = currToken.lexeme;
    int line = currToken.line;
    int column = currToken.column;
    String s = "[%d,%d] %s found '%s'";
    MyPLException.parseError(String.format(s, line, column, msg, lexeme));
  }

  /**
   * Move to the next lexer token, skipping comments.
   */
  private void advance() {
    currToken = lexer.nextToken();
    while (match(TokenType.COMMENT))
      currToken = lexer.nextToken();
  }

  /**
   * Checks that the current token has the given token type.
   * 
   * @param targetTokenType The token type to check against.
   * @return True if the types match, false otherwise.
   */
  private boolean match(TokenType targetTokenType) {
    return currToken.tokenType == targetTokenType;
  }

  /**
   * Checks that the current token is contained in the given list of
   * token types.
   * 
   * @param targetTokenTypes The token types ot check against.
   * @return True if the current type is in the given list, false
   *         otherwise.
   */
  private boolean matchAny(List<TokenType> targetTokenTypes) {
    return targetTokenTypes.contains(currToken.tokenType);
  }

  /**
   * Advance to next token if current token matches the given token type.
   * 
   * @param targetType The token type to check against.
   */
  private void eat(TokenType targetTokenType, String msg) {
    if (!match(targetTokenType))
      error(msg);
    advance();
  }

  /**
   * Check if the current token is an allowed binary operator
   */
  private boolean isBinOp() {
    return matchAny(List.of(TokenType.PLUS, TokenType.MINUS, TokenType.TIMES,
        TokenType.DIVIDE, TokenType.AND, TokenType.OR,
        TokenType.EQUAL, TokenType.LESS, TokenType.GREATER,
        TokenType.LESS_EQ, TokenType.GREATER_EQ,
        TokenType.NOT_EQUAL));
  }

  /**
   * Check if the current token is a literal value
   */
  private boolean isLiteral() {
    return matchAny(List.of(TokenType.INT_VAL, TokenType.DOUBLE_VAL,
        TokenType.STRING_VAL, TokenType.BOOL_VAL,
        TokenType.NULL_VAL));
  }

  /**
   * Parse the program
   * 
   * @return the corresponding Program AST object
   */
  private Program program() {
    // TODO: implement this function
    Program program = new Program();
    program.structs = new ArrayList<>();
    program.functions = new ArrayList<>();
    while (matchAny(List.of(TokenType.STRUCT, TokenType.VOID_TYPE, TokenType.ID, TokenType.LBRACKET, TokenType.INT_TYPE,
        TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
      if (match(TokenType.STRUCT)) {
        program.structs.add(structDef());
      } else if (matchAny(List.of(TokenType.VOID_TYPE, TokenType.ID, TokenType.LBRACKET, TokenType.INT_TYPE,
          TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
        program.functions.add(funDef());
      } else {
        error("Wrong token type found");
      }
    }
    return program;
  }

  /**
   * Parse a struct definition
   * 
   * @return the corresponding StructDef AST object
   */
  private StructDef structDef() {
    // TODO: implement this function
    StructDef structDef = new StructDef();
    structDef.fields = new ArrayList<>();
    eat(TokenType.STRUCT, "ate a STRUCT");
    structDef.structName = currToken;
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.LBRACE, "ate a LBRACE");
    structDef.fields = fields();
    eat(TokenType.RBRACE, "ate a RBRACE");
    return structDef;
  }

  /**
   * Parse a function definition
   * 
   * @return the corresponding FunDef AST object
   */
  private FunDef funDef() {
    // TODO: implement this function
    FunDef funDef = new FunDef();
    funDef.returnType = return_type();
    funDef.funName = currToken;
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.LPAREN, "ate a LPAREN");
    funDef.params = params();
    eat(TokenType.RPAREN, "ate a RPAREN");
    funDef.stmts = block();
    return funDef;
  }

  // ... and so on ...
  // TODO: implement the rest of the recursive descent functions
  /*
   * parse the fields of a struct def
   */
  private List<VarDef> fields() {
    List<VarDef> fields = new ArrayList<>();
    if (match(TokenType.ID)) {
      fields.add(field());
      while (match(TokenType.COMMA)) {
        advance();
        fields.add(field());
      }
    }
    return fields;
  }

  private VarDef field() {
    VarDef field = new VarDef();
    field.varName = currToken;
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.COLON, "ate a COLON");
    field.dataType = data_type();
    return field;
  }

  private List<Stmt> block() {
    List<Stmt> block = new ArrayList<>();
    eat(TokenType.LBRACE, "ate a LBRACE");
    while (!match(TokenType.RBRACE)) {
      block.add(stmt());
    }
    eat(TokenType.RBRACE, "ate a RBRACE");
    return block;
  }

  private DataType return_type() {
    DataType return_type = new DataType();
    if (matchAny(List.of(TokenType.ID, TokenType.LBRACKET, TokenType.INT_TYPE,
        TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
      return_type = data_type();
    } else if (match(TokenType.VOID_TYPE)) {
      return_type.type = currToken;
      return_type.isArray = false;
      advance();
    } else {
      error("Did not get expected type");
    }
    return return_type;
  }

  private List<VarDef> params() {
    List<VarDef> params = new ArrayList<>();
    if (match(TokenType.ID)) {
      params.add(param());
      while (match(TokenType.COMMA)) {
        advance();
        params.add(param());
      }
    }
    return params;
  }

  private VarDef param() {
    VarDef param = new VarDef();
    param.varName = currToken;
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.COLON, "ate a COLON");
    param.dataType = data_type();
    return param;
  }

  private DataType data_type() {
    DataType dataType = new DataType();
    dataType.isArray = false;
    if (matchAny(List.of(TokenType.INT_TYPE, TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
      dataType.type = base_type();
    } else if (match(TokenType.ID)) {
      dataType.type = currToken;
      advance();
    } else if (match(TokenType.LBRACKET)) {
      dataType.isArray = true;
      advance();
      if (matchAny(List.of(TokenType.INT_TYPE, TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
        dataType.type = base_type();
      } else if (match(TokenType.ID)) {
        dataType.type = currToken;
        advance();
      } else {
        error("Did not get expected type");
      }
      eat(TokenType.RBRACKET, "ate a RBRACKET");
    } else {
      error("Did not get expected type");
    }
    return dataType;
  }

  private Token base_type() {
    Token baseType = currToken;
    advance();
    return baseType;
  }

  private Stmt stmt() {
    if (match(TokenType.VAR)) {
      return var_stmt();
    } else if (match(TokenType.WHILE)) {
      return while_stmt();
    } else if (match(TokenType.IF)) {
      return if_stmt();
    } else if (match(TokenType.FOR)) {
      return for_stmt();
    } else if (match(TokenType.RETURN)) {
      return return_stmt();
    } else if (match(TokenType.ID)) {
      Token idToken = currToken;
      advance();
      if (match(TokenType.LPAREN)) {
        CallRValue funCall = fun_call(idToken);
        return funCall;
      } else {
        return assign_stmt(idToken);
      }
    } else {
      error("bad stuff");
      return var_stmt();
    }
  }

  private VarStmt var_stmt() {
    VarStmt varStmt = new VarStmt();
    eat(TokenType.VAR, "ate a VAR");
    varStmt.varName = currToken;
    eat(TokenType.ID, "ate a ID");
    if (match(TokenType.ASSIGN)) {
      varStmt.expr = Optional.of(var_init());
    } else if (match(TokenType.COLON)) {
      varStmt.dataType = Optional.of(var_type());
      if (match(TokenType.ASSIGN)) {
        varStmt.expr = Optional.of(var_init());
      }
    } else {
      error("Did not get expected type");
    }
    return varStmt;
  }

  private DataType var_type() {
    eat(TokenType.COLON, "ate a COLON");
    return data_type();
  }

  private Expr var_init() {
    eat(TokenType.ASSIGN, "ate a ASSIGN");
    return expr();
  }

  private WhileStmt while_stmt() {
    WhileStmt whileStmt = new WhileStmt();
    eat(TokenType.WHILE, "ate a WHILE");
    whileStmt.condition = expr();
    whileStmt.stmts = block();
    return whileStmt;
  }

  private IfStmt if_stmt() {
    IfStmt ifStmt = new IfStmt();
    eat(TokenType.IF, "ate a IF");
    ifStmt.condition = expr();
    ifStmt.ifStmts = block();
    if (match(TokenType.ELSE)) {
      advance();
      if (match(TokenType.IF)) {
        ifStmt.elseIf = Optional.of(if_stmt());
      } else if (match(TokenType.LBRACE)) {
        ifStmt.elseStmts = Optional.of(block());
      } else {
        error("Did not get expected type1");
      }
    }
    return ifStmt;
  }

  private ForStmt for_stmt() {
    ForStmt forStmt = new ForStmt();
    eat(TokenType.FOR, "ate a FOR");
    forStmt.varName = currToken;
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.FROM, "ate a FROM");
    forStmt.fromExpr = expr();
    eat(TokenType.TO, "ate a TO");
    forStmt.toExpr = expr();
    forStmt.stmts = block();
    return forStmt;
  }

  private ReturnStmt return_stmt() {
    ReturnStmt returnStmt = new ReturnStmt();
    eat(TokenType.RETURN, "ate a RETURN");
    returnStmt.expr = expr();
    return returnStmt;
  }

  private AssignStmt assign_stmt(Token idToken) {
    AssignStmt assignStmt = new AssignStmt();
    assignStmt.lvalue = lvalue(idToken);
    eat(TokenType.ASSIGN, "ate a ASSIGN");
    assignStmt.expr = expr();
    return assignStmt;
  }

  private List<VarRef> lvalue(Token idToken) {
    // Assuming ID was already advanced in stmt
    List<VarRef> nodes = new ArrayList<>();
    VarRef node = new VarRef();
    node.varName = idToken;
    if (match(TokenType.LBRACKET)) {
      advance();
      node.arrayExpr = Optional.of(expr());
      eat(TokenType.RBRACKET, "ate a RBRACKET");
    }
    nodes.add(node);
    while (match(TokenType.DOT)) {
      VarRef node2 = new VarRef();
      advance();
      node2.varName = currToken;
      eat(TokenType.ID, "ate a ID");
      if (match(TokenType.LBRACKET)) {
        advance();
        node2.arrayExpr = Optional.of(expr());
        eat(TokenType.RBRACKET, "ate a RBRACKET");
      }
      nodes.add(node2);
    }
    return nodes;
  }

  private CallRValue fun_call(Token idToken) {
    // assuming ID was already advanced in stmt
    CallRValue funCall = new CallRValue();
    funCall.funName = idToken;
    eat(TokenType.LPAREN, "ate a LPAREN");
    funCall.args = args();
    eat(TokenType.RPAREN, "ate a RPAREN");
    return funCall;
  }

  private List<Expr> args() {
    List<Expr> exprs = new ArrayList<>();
    if (isLiteral() || matchAny(List.of(TokenType.NEW, TokenType.ID, TokenType.NOT, TokenType.LPAREN))) {
      exprs.add(expr());
      while (match(TokenType.COMMA)) {
        advance();
        exprs.add(expr());
      }
    }
    return exprs;
  }

  // private Expr expr() {
  // Expr expr;
  // if (match(TokenType.LPAREN)) {
  // advance();
  // expr = expr();
  // eat(TokenType.RPAREN, "ate a RPAREN");
  // } else if (match(TokenType.NOT)) {
  // UnaryExpr unaryExpr = new UnaryExpr();
  // unaryExpr.unaryOp = currToken;
  // advance();
  // unaryExpr.expr = expr();
  // expr = unaryExpr;
  // } else {
  // BasicExpr basicExpr = new BasicExpr();
  // basicExpr.rvalue = rvalue();
  // expr = basicExpr;
  // }
  // while (isBinOp()) {
  // BinaryExpr binExpr = new BinaryExpr();
  // bin_op(binExpr);
  // binExpr.rhs = expr();
  // binExpr.lhs = expr;
  // expr = binExpr;
  // }
  // return expr;
  // }

  private Expr expr() {
    return parseExpr(0);
  }

  // helper

  private Expr primaryExpr() {
    Expr expr;
    if (match(TokenType.LPAREN)) {
      advance();
      expr = expr();
      eat(TokenType.RPAREN, "expected ')'");
    } else if (match(TokenType.NOT)) {
      UnaryExpr unaryExpr = new UnaryExpr();
      unaryExpr.unaryOp = currToken;
      advance();
      unaryExpr.expr = primaryExpr(); // recurse here too
      expr = unaryExpr;
    } else {
      BasicExpr basicExpr = new BasicExpr();
      basicExpr.rvalue = rvalue();
      expr = basicExpr;
    }
    return expr;
  }

  private int getPrecedence(TokenType op) {
    return switch (op) {
      case TIMES, DIVIDE -> 4;
      case PLUS, MINUS -> 3;
      case EQUAL, NOT_EQUAL,
          LESS, LESS_EQ,
          GREATER, GREATER_EQ ->
        2;
      case AND -> 1;
      case OR -> 0;
      default -> -100; // Not a binary operator
    };
  }

  private Expr parseExpr(int minPrecedence) {
    Expr left = primaryExpr();

    while (isBinOp() && getPrecedence(currToken.tokenType) >= minPrecedence) {
      Token op = currToken;
      int precedence = getPrecedence(op.tokenType);
      advance();

      Expr right = parseExpr(precedence + 1);

      BinaryExpr bin = new BinaryExpr();
      bin.lhs = left;
      bin.rhs = right;
      bin.binaryOp = op;
      left = bin;
    }

    return left;
  }

  private void bin_op(BinaryExpr expr) {
    expr.binaryOp = currToken;
    advance();
  }

  private RValue rvalue() {
    RValue value;
    if (isLiteral()) {
      SimpleRValue simpVal = new SimpleRValue();
      simpVal.literal = literal();
      value = simpVal;
    } else if (match(TokenType.NEW)) {
      value = new_rvalue();
    } else {
      Token token = currToken;
      advance();
      if (match(TokenType.LPAREN)) {
        // CallRValue
        value = fun_call(token);
      } else {
        value = var_rvalue(token);
      }
    }
    return value;
  }

  private NewRValue new_rvalue() {
    NewRValue val = null;
    eat(TokenType.NEW, "ate a NEW");
    if (matchAny(List.of(TokenType.INT_TYPE, TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
      NewArrayRValue arrVal = new NewArrayRValue();
      arrVal.type = base_type();
      eat(TokenType.LBRACKET, "ate a LBRACKET");
      arrVal.arrayExpr = expr();
      eat(TokenType.RBRACKET, "ate a RBRACKET");
      val = arrVal;
    } else {
      Token id = currToken;
      advance();
      if (match(TokenType.LPAREN)) {
        advance();
        NewStructRValue structVal = new NewStructRValue();
        structVal.structName = id;
        structVal.args = args();
        eat(TokenType.RPAREN, "ate a RPAREN");
        val = structVal;
      } else if (match(TokenType.LBRACKET)) {
        advance();
        NewArrayRValue arrVal = new NewArrayRValue();
        arrVal.type = id;
        arrVal.arrayExpr = expr();
        eat(TokenType.RBRACKET, "ate a RBRACKET");
        val = arrVal;
      } else {
        error("Expected different token type");
      }
    }
    return val;
  }

  private Token literal() {
    Token lit = currToken;
    advance();
    return lit;
  }

  private VarRValue var_rvalue(Token idToken) {
    VarRValue rValue = new VarRValue();
    VarRef node = new VarRef();
    node.varName = idToken;
    // Assuming ID was already advanced in stmt
    if (match(TokenType.LBRACKET)) {
      advance();
      node.arrayExpr = Optional.of(expr());
      eat(TokenType.RBRACKET, "ate a RBRACKET");
    }
    rValue.path.add(node);
    while (match(TokenType.DOT)) {
      VarRef node2 = new VarRef();
      advance();
      node2.varName = currToken;
      eat(TokenType.ID, "ate a ID");
      if (match(TokenType.LBRACKET)) {
        advance();
        node2.arrayExpr = Optional.of(expr());
        eat(TokenType.RBRACKET, "ate a RBRACKET");
      }
      rValue.path.add(node2);
    }
    return rValue;
  }
}