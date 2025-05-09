/**
 * CPSC 326, Spring 2025
 * The mypl driver program. 
 */

package cpsc326;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;


/**
 * The MyPL class serves as the main entry point to the
 * interpreter. 
 */
public class MyPL {

  /**
   * Print token information for the given mypl program.
   * @param input The mypl program as an input stream
   */
  private static void lexMode(InputStream input) {
    try {
      Lexer lexer = new Lexer(input);
      Token t = null;
      do {
        t = lexer.nextToken();
        System.out.println(t);
      } while (t.tokenType != TokenType.EOS);
    } catch(MyPLException e) {
      System.err.println(e.getMessage());
    }
  }

  
  /**
   * Parse the given mypl program and output the first error found, if
   * any, otherwise nothing is printed.
   * @param input The mypl program as an input stream
   */
  private static void parseMode(InputStream input) {
    try {
      Lexer lexer = new Lexer(input);
      SimpleParser parser = new SimpleParser(lexer);
      parser.parse();
    } catch(MyPLException e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Pretty print the given mypl program.
   * @param input The mypl program as an input stream
   */
  private static void printMode(InputStream input) {
    try {
      Lexer lexer = new Lexer(input);
      ASTParser parser = new ASTParser(lexer);
      Program p = parser.parse();
      PrintVisitor v = new PrintVisitor();
      p.accept(v);
    } catch(MyPLException e) {
      System.err.println(e.getMessage());
    }
  }
  
  /**
   * Perform a semantic analysis check of the given mypl program and
   * output first error found, if any, otherwise nothing is printed.
   * @param input The mypl program as an input stream
   */
  private static void checkMode(InputStream input) {
    try {
      Lexer lexer = new Lexer(input);
      ASTParser parser = new ASTParser(lexer);
      Program p = parser.parse();
      p.accept(new SemanticChecker());
    } catch(MyPLException e) {
      System.err.println(e.getMessage());
    }
    System.out.println("No static errors found");
  }
  
  /**
   * Output the intermediate representation of the given mypl
   * program. 
   * @param input The mypl program as an input stream
   */
  private static void irMode(InputStream input) {
    System.out.println("IR mode not yet supported");
  }

  /**
   * Run the given mypl program. 
   * @param input The mypl program as an input stream
   */
  private static void runMode(InputStream input) {
    System.out.println("RUN mode not yet supported");
  }

  /**
   * Run the given mypl program in debug mode.
   * @param input The mypl program as an input stream
   */
  private static void debugMode(InputStream input) {
    System.out.println("DEBUG mode not yet supported");
  }
  
  /**
   * Parse the command line options and run the given mypl program in
   * the corresponding mode (either lex, parse, print, check, ir, or
   * run). 
   */
  public static void main(String[] args) {
    InputStream input = System.in;
    // set up the command line (cmd) argument parser
    ArgumentParser cmdParser = ArgumentParsers.newFor("mypl").build()
      .defaultHelp(true)
      .description("MyPL interpreter.");
    cmdParser.addArgument("-m", "--mode")
      .choices("LEX", "PARSE", "PRINT", "CHECK", "IR", "RUN", "DEBUG")
      .setDefault("RUN")
      .help("specify execution mode");
    cmdParser.addArgument("file").nargs("?").help("mypl file to execute");
    // validate the command line arguments
    Namespace ns = null;
    try {
      ns = cmdParser.parseArgs(args);
    } catch (ArgumentParserException e) {
      cmdParser.handleError(e);
      System.exit(1);
    }
    // get the file if it is given
    if (ns.getString("file") != null) {
      String file = ns.getString("file");
      try {
        input = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        System.err.println("mypl: error: unable to open file '" + file + "'");
        System.exit(1);
      }
    }
    // call corresponding execution mode
    String mode = ns.getString("mode");
    if (mode == null || mode.equals("RUN"))
      runMode(input);
    else if (mode.equals("LEX"))
      lexMode(input);
    else if (mode.equals("PARSE"))
      parseMode(input);
    else if (mode.equals("PRINT"))
      printMode(input);
    else if (mode.equals("CHECK"))
      checkMode(input);
    else if (mode.equals("IR"))
      irMode(input);
    else if (mode.equals("DEBUG"))
      debugMode(input);
  }

}
