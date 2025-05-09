/**
 * CPSC 326, Spring 2025
 * The available mypl token types. 
 */

package cpsc326;


public enum TokenType {
  // punctuation symbols
  DOT, COLON, COMMA, LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE, 
  // arithmetic operators
  PLUS, MINUS, TIMES, DIVIDE, 
  // assignment and comparator operators
  ASSIGN, EQUAL, NOT_EQUAL, LESS, LESS_EQ, GREATER, GREATER_EQ, 
  // primitive values and identifiers
  STRING_VAL, INT_VAL, DOUBLE_VAL, BOOL_VAL, NULL_VAL, ID, 
  // boolean operators
  AND, OR, NOT,
  // data types
  INT_TYPE, DOUBLE_TYPE, CHAR_TYPE, STRING_TYPE, BOOL_TYPE, VOID_TYPE,
  // reserved words
  STRUCT, VAR, WHILE, FOR, FROM, TO, IF, ELSE, NEW, RETURN, 
  // comment token and end-of-stream
  COMMENT, EOS
}
