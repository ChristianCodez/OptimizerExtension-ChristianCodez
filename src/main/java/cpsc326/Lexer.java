/**
 * CPSC 326, Spring 2025
 * MyPL Lexer Implementation.
 *
 * Christian Carrington
 */

package cpsc326;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import java.io.IOException;

/**
 * The Lexer class takes an input stream containing mypl source code
 * and transforms (tokenizes) it into a stream of tokens.
 */
public class Lexer {

  private BufferedReader buffer; // handle to the input stream
  private int line = 1; // current line number
  private int column = 0; // current column number

  /**
   * Creates a new Lexer object out of an input stream.
   */
  public Lexer(InputStream input) {
    buffer = new BufferedReader(new InputStreamReader(input));
  }

  /**
   * Helper function to read a single character from the input stream.
   * 
   * @return A single character
   */
  private char read() {
    try {
      ++column;
      return (char) buffer.read();
    } catch (IOException e) {
      error("read error", line, column + 1);
    }
    return (char) -1;
  }

  /**
   * Helper function to look ahead one character in the input stream.
   * 
   * @return A single character
   */
  private char peek() {
    int ch = -1;
    try {
      buffer.mark(1);
      ch = (char) buffer.read();
      buffer.reset();
      return (char) ch;
    } catch (IOException e) {
      error("read error", line, column + 1);
    }
    return (char) -1;
  }

  /**
   * Helper function to check if the given character is an end of line
   * symbol.
   * 
   * @return True if the character is an end of line character and
   *         false otherwise.
   */
  private boolean isEOL(char ch) {
    if (ch == '\n')
      return true;
    if (ch == '\r' && peek() == '\n') {
      read();
      return true;
    } else if (ch == '\r')
      return true;
    return false;
  }

  /**
   * Helper function to check if the given character is an end of file
   * symbol.
   * 
   * @return True if the character is an end of file character and
   *         false otherwise.
   */
  private boolean isEOF(char ch) {
    return ch == (char) -1;
  }

  /**
   * Print an error message and exit the program.
   */
  private void error(String msg, int line, int column) {
    String s = "[%d,%d] %s";
    MyPLException.lexerError(String.format(s, line, column, msg));
  }

  /**
   * Obtains and returns the next token in the stream.
   * 
   * @return The next token in the stream.
   */
  public Token nextToken() {
    // read the initial character
    char ch = read();

    // TODO: Finish this method
    while (Character.isWhitespace(ch)) {
      if (isEOL(ch)) {
        ++line;
        column = 0;
      }
      ch = read();
    }
    if (isEOF(ch)) {
      return new Token(TokenType.EOS, "end-of-stream", line, column);
    } else if (ch == '.') {
      return new Token(TokenType.DOT, ".", line, column);
    } else if (ch == ',') {
      return new Token(TokenType.COMMA, ",", line, column);
    } else if (ch == '(') {
      return new Token(TokenType.LPAREN, "(", line, column);
    } else if (ch == ')') {
      return new Token(TokenType.RPAREN, ")", line, column);
    } else if (ch == '[') {
      return new Token(TokenType.LBRACKET, "[", line, column);
    } else if (ch == ']') {
      return new Token(TokenType.RBRACKET, "]", line, column);
    } else if (ch == '{') {
      return new Token(TokenType.LBRACE, "{", line, column);
    } else if (ch == '}') {
      return new Token(TokenType.RBRACE, "}", line, column);
    } else if (ch == ':') {
      return new Token(TokenType.COLON, ":", line, column);
    } else if (ch == '+') {
      return new Token(TokenType.PLUS, "+", line, column);
    } else if (ch == '-') {
      return new Token(TokenType.MINUS, "-", line, column);
    } else if (ch == '*') {
      return new Token(TokenType.TIMES, "*", line, column);
    } else if (ch == '/') {
      return new Token(TokenType.DIVIDE, "/", line, column);
    } else if (ch == '=') {
      if (peek() == '=') {
        ch = read();
        return new Token(TokenType.EQUAL, "==", line, column - 1);
      }
      return new Token(TokenType.ASSIGN, "=", line, column);
    } else if (ch == '!') {
      if (peek() == '=') {
        ch = read();
        return new Token(TokenType.NOT_EQUAL, "!=", line, column - 1);
      }
      error("expecting !=", line, column);
    } else if (ch == '>') {
      if (peek() == '=') {
        ch = read();
        return new Token(TokenType.GREATER_EQ, ">=", line, column - 1);
      }
      return new Token(TokenType.GREATER, ">", line, column);
    } else if (ch == '<') {
      if (peek() == '=') {
        ch = read();
        return new Token(TokenType.LESS_EQ, "<=", line, column - 1);
      }
      return new Token(TokenType.LESS, "<", line, column);
    } else if (ch == '#') { // CHECKING FOR COMMENTS
      StringBuilder sb = new StringBuilder("");
      int initialColumn = column;
      ch = read();
      while (!isEOL(ch)) {
        if (isEOF(peek())) {
          sb.append(ch);
          return new Token(TokenType.COMMENT, sb.toString(), line, initialColumn);
        }
        sb.append(ch);
        ch = read();
      }
      ++line;
      column = 0;
      return new Token(TokenType.COMMENT, sb.toString(), line - 1, initialColumn);
    } else if (ch == '"') { // CHECKING FOR STRINGS
      int initialColumn = column;
      StringBuilder sb = new StringBuilder("");
      while (peek() != '"') {
        if (isEOF(ch)) {
          error("Expected a closing '\"'", line, initialColumn);
        } else if (isEOL(ch)) {
          error("non-terminated string", line, column);
        } else { // add the characters to a string to act as the lexeme
          sb.append(peek());
          ch = read();
        }
      }
      ch = read();
      return new Token(TokenType.STRING_VAL, sb.toString(), line, initialColumn);
    } else if (Character.isDigit(ch)) { // CHECKING FOR INTEGER AND DOUBLE VALUES
      if (ch == '0' && Character.isDigit(peek())) {
        error("leading zero in number", line, column);
      } else {
        StringBuilder sb = new StringBuilder("");
        boolean isDouble = false;
        int initialColumn = column;
        while (Character.isDigit(ch) || ch == '.') { // checking if next character is a digit and adding it if it is
          if (ch == '.' && !(Character.isDigit(peek()))) {
            error("missing digit after decimal", line, column + 1);
          } else {
            if (isDouble && peek() == '.') { // CHECKING FOR MULTIPLE '.'
              sb.append(ch);
              return new Token(TokenType.DOUBLE_VAL, sb.toString(), line, initialColumn);
            } else if (ch == '.') {
              isDouble = true;
            }
            sb.append(ch);
            if (Character.isDigit(peek()) || peek() == '.') {
              ch = read();
            } else {
              if (!isDouble) {
                return new Token(TokenType.INT_VAL, sb.toString(), line, initialColumn);
              } else {
                return new Token(TokenType.DOUBLE_VAL, sb.toString(), line, initialColumn);
              }
            }
          }
        }
      }
    } else if (Character.isLetter(ch)) { // CHECK FOR RESERVED WORDS AND IDs
      String[] reservedWords = { "and", "or", "not", "struct", "var", "if", "else", "while", "for", "from", "to", "new",
          "true", "false", "null", "void", "int", "double", "bool", "string", "return" };
      StringBuilder sb = new StringBuilder("");
      int initialColumn = column;
      while (Character.isLetterOrDigit(ch) || ch == '_') {
        sb.append(ch);
        if (Character.isLetterOrDigit(peek()) || peek() == '_') {
          ch = read();
        } else {
          break;
        }
      }
      if (Arrays.asList(reservedWords).contains(sb.toString())) {
        String word = sb.toString();
        switch (word) {
          case "and":
            return new Token(TokenType.AND, sb.toString(), line, initialColumn);
          case "or":
            return new Token(TokenType.OR, sb.toString(), line, initialColumn);
          case "not":
            return new Token(TokenType.NOT, sb.toString(), line, initialColumn);
          case "struct":
            return new Token(TokenType.STRUCT, sb.toString(), line, initialColumn);
          case "var":
            return new Token(TokenType.VAR, sb.toString(), line, initialColumn);
          case "if":
            return new Token(TokenType.IF, sb.toString(), line, initialColumn);
          case "else":
            return new Token(TokenType.ELSE, sb.toString(), line, initialColumn);
          case "while":
            return new Token(TokenType.WHILE, sb.toString(), line, initialColumn);
          case "for":
            return new Token(TokenType.FOR, sb.toString(), line, initialColumn);
          case "from":
            return new Token(TokenType.FROM, sb.toString(), line, initialColumn);
          case "to":
            return new Token(TokenType.TO, sb.toString(), line, initialColumn);
          case "new":
            return new Token(TokenType.NEW, sb.toString(), line, initialColumn);
          case "true":
            return new Token(TokenType.BOOL_VAL, sb.toString(), line, initialColumn);
          case "false":
            return new Token(TokenType.BOOL_VAL, sb.toString(), line, initialColumn);
          case "null":
            return new Token(TokenType.NULL_VAL, sb.toString(), line, initialColumn);
          case "void":
            return new Token(TokenType.VOID_TYPE, sb.toString(), line, initialColumn);
          case "int":
            return new Token(TokenType.INT_TYPE, sb.toString(), line, initialColumn);
          case "double":
            return new Token(TokenType.DOUBLE_TYPE, sb.toString(), line, initialColumn);
          case "bool":
            return new Token(TokenType.BOOL_TYPE, sb.toString(), line, initialColumn);
          case "string":
            return new Token(TokenType.STRING_TYPE, sb.toString(), line, initialColumn);
          case "return":
            return new Token(TokenType.RETURN, sb.toString(), line, initialColumn);
        }
      } else {
        return new Token(TokenType.ID, sb.toString(), line, initialColumn);
      }
    } else {
      error("unrecognized symbol '" + ch + "'", line, column);
    }
    return null;
  }

}
