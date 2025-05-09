/**
 * CPSC 326, Spring 2025
 * MyPL Exception class for error handling.
 */

package cpsc326;


/**
 * A MyPLException is an unchecked exception since it won't generally
 * be a recoverable error.
 */
public class MyPLException extends RuntimeException {

  private enum ErrorType {
    LEXER_ERROR, PARSE_ERROR, STATIC_ERROR, VM_ERROR
  };

  private String message;       // the error message 
  private ErrorType type;       // the stage the error occurred in
  
  /**
   * Create a new exception of the given type.
   */
  public MyPLException(String message, ErrorType type) {
    this.message = message;
    this.type = type;
  }

  /**
   * Returns the exception message.
   */
  @Override
  public String getMessage() {
    return type.toString() + ": " + message;
  }

  /**
   * Helper to create a lexer error.
   */
  public static void lexerError(String message) {
    throw new MyPLException(message, ErrorType.LEXER_ERROR);
  }

  /**
   * Helper to create a parser error.
   */
  public static void parseError(String message) {
    throw new MyPLException(message, ErrorType.PARSE_ERROR);
  }

  /**
   * Helper to create a static analysis error.
   */
  public static void staticError(String message) {
    throw new MyPLException(message, ErrorType.STATIC_ERROR);
  }

  /**
   * Helper to create a virtual-machine runtime error.
   */
  public static void vmError(String message) {
    throw new MyPLException(message, ErrorType.VM_ERROR);
  }

}
