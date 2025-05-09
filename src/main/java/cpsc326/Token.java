/**
 * CPSC 326, Spring 2025
 * Basic class for representing tokens.
 */

package cpsc326;

/**
 * A Token represents a basic meaningful unit of a program, and
 * consists of a type, a lexeme, a line, and a column.
 */
public class Token {

  public TokenType tokenType;  // the token's type
  public String lexeme;        // the token's string representation
  public int line;             // the line the token appears on
  public int column;           // the column the token appears on

  /**
   * Creates a new token object.
   */
  public Token(TokenType tokenType, String lexeme, int line, int column) {
    this.tokenType = tokenType;
    this.lexeme = lexeme;
    this.line = line;
    this.column = column;
  }

  /**
   * Pretty prints a token object.
   */
  @Override
  public String toString() {
    return tokenType + " \"" + lexeme + "\" line " + line + " column " + column;
  }
    
}
