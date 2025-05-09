/**
 * CPSC 326, Spring 2025
 * The Simple Parser implementation.
 *
 * Christian Carrington
 */

package cpsc326;

import java.util.List;

/**
 * Simple recursive descent parser for checking program syntax.
 */
public class SimpleParser {

  private Lexer lexer; // the lexer
  private Token currToken; // the current token

  /**
   * Create a SimpleParser from the give lexer.
   * 
   * @param lexer The lexer for the program to parse.
   */
  public SimpleParser(Lexer lexer) {
    this.lexer = lexer;
  }

  /**
   * Run the parser.
   */
  public void parse() {
    advance();
    program();
    eat(TokenType.EOS, "expecting end of file");
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
   * @param targetTokenType The token type to check against.
   */
  private void eat(TokenType targetTokenType, String msg) {
    if (!match(targetTokenType))
      error(msg);
    advance();
  }

  /**
   * Helper to check that the current token is a binary operator.
   */
  private boolean isBinOp() {
    return matchAny(List.of(TokenType.PLUS, TokenType.MINUS, TokenType.TIMES,
        TokenType.DIVIDE, TokenType.AND, TokenType.OR,
        TokenType.EQUAL, TokenType.LESS, TokenType.GREATER,
        TokenType.LESS_EQ, TokenType.GREATER_EQ,
        TokenType.NOT_EQUAL));
  }

  /**
   * Helper to check that the current token is a literal value.
   */
  private boolean isLiteral() {
    return matchAny(List.of(TokenType.INT_VAL, TokenType.DOUBLE_VAL,
        TokenType.STRING_VAL, TokenType.BOOL_VAL,
        TokenType.NULL_VAL));
  }

  /**
   * Checks for a valid program.
   */
  private void program() {
    // TODO: implement this function
    while (matchAny(List.of(TokenType.STRUCT, TokenType.VOID_TYPE, TokenType.ID, TokenType.LBRACKET, TokenType.INT_TYPE,
        TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
      if (match(TokenType.STRUCT)) {
        structDef();
      } else if (matchAny(List.of(TokenType.VOID_TYPE, TokenType.ID, TokenType.LBRACKET, TokenType.INT_TYPE,
          TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
        funDef();
      } else {
        error("Wrong token type found");
      }
    }
  }

  /**
   * Checks for a valid struct definition.
   */
  private void structDef() {
    // TODO: implement this function
    eat(TokenType.STRUCT, "ate a STRUCT");
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.LBRACE, "ate a LBRACE");
    fields();
    eat(TokenType.RBRACE, "ate a RBRACE");
  }

  /**
   * checks for a valid function definition.
   */
  private void funDef() {
    // TODO: implement this function
    return_type();
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.LPAREN, "ate a LPAREN");
    params();
    eat(TokenType.RPAREN, "ate a RPAREN");
    block();
  }

  // ... and so on ...
  // TODO: implement the rest of the recursive descent functions

  private void fields() {
    if (match(TokenType.ID)) {
      field();
      while (match(TokenType.COMMA)) {
        advance();
        field();
      }
    }
  }

  private void field() {
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.COLON, "ate a COLON");
    data_type();
  }

  private void block() {
    eat(TokenType.LBRACE, "ate a LBRACE");
    while (matchAny(List.of(TokenType.VAR, TokenType.WHILE, TokenType.IF, TokenType.FOR,
        TokenType.RETURN, TokenType.ID))) {
      stmt();
    }
    eat(TokenType.RBRACE, "ate a RBRACE");
  }

  private void return_type() {
    if (matchAny(List.of(TokenType.ID, TokenType.LBRACKET, TokenType.INT_TYPE,
        TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
      data_type();
    } else if (match(TokenType.VOID_TYPE)) {
      advance();
    } else {
      error("Did not get expected type");
    }
  }

  private void params() {
    if (match(TokenType.ID)) {
      param();
      while (match(TokenType.COMMA)) {
        advance();
        param();
      }
    }
  }

  private void param() {
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.COLON, "ate a COLON");
    data_type();
  }

  private void data_type() {
    if (matchAny(List.of(TokenType.INT_TYPE, TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
      base_type();
    } else if (match(TokenType.ID)) {
      advance();
    } else if (match(TokenType.LBRACKET)) {
      advance();
      if (matchAny(List.of(TokenType.INT_TYPE, TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
        base_type();
      } else if (match(TokenType.ID)) {
        advance();
      } else {
        error("Did not get expected type");
      }
      eat(TokenType.RBRACKET, "ate a RBRACET");
    } else {
      error("Did not get expected type");
    }
  }

  private void base_type() {
    if (matchAny(List.of(TokenType.INT_TYPE, TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
      advance();
    } else {
      error("Did not get expected type");
    }
  }

  private void stmt() {
    if (match(TokenType.VAR)) {
      var_stmt();
    } else if (match(TokenType.WHILE)) {
      while_stmt();
    } else if (match(TokenType.IF)) {
      if_stmt();
    } else if (match(TokenType.FOR)) {
      for_stmt();
    } else if (match(TokenType.RETURN)) {
      return_stmt();
    } else if (match(TokenType.ID)) {
      advance();
      if (match(TokenType.LPAREN)) {
        fun_call();
      } else {
        assign_stmt();
      }
    }
  }

  private void var_stmt() {
    eat(TokenType.VAR, "ate a VAR");
    eat(TokenType.ID, "ate a ID");
    if (match(TokenType.ASSIGN)) {
      var_init();
    } else if (match(TokenType.COLON)) {
      var_type();
      if (match(TokenType.ASSIGN)) {
        var_init();
      }
    } else {
      error("Did not get expected type");
    }
  }

  private void var_type() {
    eat(TokenType.COLON, "ate a COLON");
    data_type();
  }

  private void var_init() {
    eat(TokenType.ASSIGN, "ate a ASSIGN");
    expr();
  }

  private void while_stmt() {
    eat(TokenType.WHILE, "ate a WHILE");
    expr();
    block();
  }

  private void if_stmt() {
    eat(TokenType.IF, "ate a IF");
    expr();
    block();
    if (match(TokenType.ELSE)) {
      advance();
      if (match(TokenType.IF)) {
        if_stmt();
      } else if (match(TokenType.LBRACE)) {
        block();
      } else {
        error("Did not get expected type1");
      }
    }
  }

  private void for_stmt() {
    eat(TokenType.FOR, "ate a FOR");
    eat(TokenType.ID, "ate a ID");
    eat(TokenType.FROM, "ate a FROM");
    expr();
    eat(TokenType.TO, "ate a TO");
    expr();
    block();
  }

  private void return_stmt() {
    eat(TokenType.RETURN, "ate a RETURN");
    expr();
  }

  private void assign_stmt() {
    lvalue();
    eat(TokenType.ASSIGN, "ate a ASSIGN");
    expr();
  }

  private void lvalue() {
    // Assuming ID was already advanced in stmt
    if (match(TokenType.LBRACKET)) {
      advance();
      expr();
      eat(TokenType.RBRACKET, "ate a RBRACKET");
    }
    while (match(TokenType.DOT)) {
      advance();
      eat(TokenType.ID, "ate a ID");
      if (match(TokenType.LBRACKET)) {
        advance();
        expr();
        eat(TokenType.RBRACKET, "ate a RBRACKET");
      }
    }
  }

  private void fun_call() {
    // assuming ID was already advanced in stmt
    eat(TokenType.LPAREN, "ate a LPAREN");
    args();
    eat(TokenType.RPAREN, "ate a RPAREN");
  }

  private void args() {
    if (isLiteral() || matchAny(List.of(TokenType.NEW, TokenType.ID, TokenType.NOT, TokenType.LPAREN))) {
      expr();
      while (match(TokenType.COMMA)) {
        advance();
        expr();
      }
    }
  }

  private void expr() {
    if (isLiteral() || matchAny(List.of(TokenType.NEW, TokenType.ID))) {
      rvalue();
    } else if (match(TokenType.NOT)) {
      advance();
      expr();
    } else if (match(TokenType.LPAREN)) {
      advance();
      expr();
      eat(TokenType.RPAREN, "ate a RPAREN");
    } else {
      error("Did not get expected token type");
    }
    if (isBinOp()) {
      bin_op();
      expr();
    }
  }

  private void bin_op() {
    if (isBinOp()) {
      advance();
    } else {
      error("Did not find expected token type");
    }
  }

  private void rvalue() {
    if (isLiteral()) {
      literal();
    } else if (match(TokenType.NEW)) {
      new_rvalue();
    } else if (match(TokenType.ID)) {
      advance();
      if (match(TokenType.LPAREN)) {
        fun_call();
      } else {
        var_rvalue();
      }
    }
  }

  private void new_rvalue() {
    eat(TokenType.NEW, "ate a NEW");
    if (matchAny(List.of(TokenType.INT_TYPE, TokenType.DOUBLE_TYPE, TokenType.STRING_TYPE, TokenType.BOOL_TYPE))) {
      base_type();
      eat(TokenType.LBRACKET, "ate a LBRACKET");
      expr();
      eat(TokenType.RBRACKET, "ate a RBRACKET");
    } else if (match(TokenType.ID)) {
      advance();
      if (match(TokenType.LPAREN)) {
        advance();
        args();
        eat(TokenType.RPAREN, "ate a RPAREN");
      } else if (match(TokenType.LBRACKET)) {
        advance();
        expr();
        eat(TokenType.RBRACKET, "ate a RBRACKET");
      } else {
        error("Expected different token type");
      }
    } else {
      error("Expected different token type");
    }
  }

  private void literal() {
    if (isLiteral()) {
      advance();
    } else {
      error("Expecting a different token type");
    }
  }

  private void var_rvalue() {
    // Assuming ID was already advanced in stmt
    if (match(TokenType.LBRACKET)) {
      advance();
      expr();
      eat(TokenType.RBRACKET, "ate a RBRACKET");
    }
    while (match(TokenType.DOT)) {
      advance();
      eat(TokenType.ID, "ate a ID");
      if (match(TokenType.LBRACKET)) {
        advance();
        expr();
        eat(TokenType.RBRACKET, "ate a RBRACKET");
      }
    }
  }
}
