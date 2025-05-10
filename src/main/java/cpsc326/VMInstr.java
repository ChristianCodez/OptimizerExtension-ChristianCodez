/**
 * CPSC 326, Spring 2025
 * Class for representing a VM instruction.
 */

package cpsc326;

import java.util.List;
import java.util.ArrayList;


/**
 * Represents a VM instruction.
 */
public class VMInstr {
  public OpCode opcode;
  public Object operand;
  public String comment = "";

  /**
   * Create an instruction
   * @param opcode The instruction opcode.
   */
  public VMInstr(OpCode opcode) {
    this.opcode = opcode;
  }
  
  /**
   * Create an instruction
   * @param opcode The instruction opcode.
   * @param operand The operand value.
   */
  public VMInstr(OpCode opcode, Object operand) {
    this.opcode = opcode;
    this.operand = operand;
  }

  /**
   * Pretty print an instruction.
   */
  public String toString() {
    String s = opcode + "(";
    if (operand != null)
      s += operand;
    s += ")";
    if (!comment.equals(""))
      s += "  // " + comment;
    return s;
  }

  // Helpers to create instructions (use these to create actual instructions)
  
  public static VMInstr PUSH(Object value) {
    return new VMInstr(OpCode.PUSH, value);
  }

  public static VMInstr POP() {
    return new VMInstr(OpCode.POP);
  }

  public static VMInstr LOAD(int memoryAddress) {
    return new VMInstr(OpCode.LOAD, memoryAddress);
  }

  public static VMInstr STORE(int memoryAddress) {
    return new VMInstr(OpCode.STORE, memoryAddress);
  }

  public static VMInstr ADD() {
    return new VMInstr(OpCode.ADD);
  }
  
  public static VMInstr SUB() {
    return new VMInstr(OpCode.SUB);
  }

  public static VMInstr MUL() {
    return new VMInstr(OpCode.MUL);
  }

  public static VMInstr DIV() {
    return new VMInstr(OpCode.DIV);
  }

  public static VMInstr CMPLT() {
    return new VMInstr(OpCode.CMPLT);
  }
  
  public static VMInstr CMPLE() {
    return new VMInstr(OpCode.CMPLE);
  }

  public static VMInstr CMPEQ() {
    return new VMInstr(OpCode.CMPEQ);
  }

  public static VMInstr CMPNE() {
    return new VMInstr(OpCode.CMPNE);
  }

  public static VMInstr AND() {
    return new VMInstr(OpCode.AND);
  }

  public static VMInstr OR() {
    return new VMInstr(OpCode.OR);
  }

  public static VMInstr NOT() {
    return new VMInstr(OpCode.NOT);
  }
  
  public static VMInstr JMP(int offset) {
    return new VMInstr(OpCode.JMP, offset);
  }

  public static VMInstr JMPF(int offset) {
    return new VMInstr(OpCode.JMPF, offset);
  }

  public static VMInstr CALL(String functionName) {
    return new VMInstr(OpCode.CALL, functionName);
  }

  public static VMInstr RET() {
    return new VMInstr(OpCode.RET);
  }

  public static VMInstr WRITE() {
    return new VMInstr(OpCode.WRITE);
  }

  public static VMInstr READ() {
    return new VMInstr(OpCode.READ);
  }

  public static VMInstr LEN() {
    return new VMInstr(OpCode.LEN);
  }

  public static VMInstr GETC() {
    return new VMInstr(OpCode.GETC);
  }

  public static VMInstr TOINT() {
    return new VMInstr(OpCode.TOINT);
  }

  public static VMInstr TODBL() {
    return new VMInstr(OpCode.TODBL);
  }

  public static VMInstr TOSTR() {
    return new VMInstr(OpCode.TOSTR);
  }

  public static VMInstr ALLOCS() {
    return new VMInstr(OpCode.ALLOCS);
  }

  public static VMInstr SETF(String field) {
    return new VMInstr(OpCode.SETF, field);
  }

  public static VMInstr GETF(String field) {
    return new VMInstr(OpCode.GETF, field);
  }

  public static VMInstr ALLOCA() {
    return new VMInstr(OpCode.ALLOCA);
  }

  public static VMInstr SETI() {
    return new VMInstr(OpCode.SETI);
  }

  public static VMInstr GETI() {
    return new VMInstr(OpCode.GETI);
  }

  public static VMInstr DUP() {
    return new VMInstr(OpCode.DUP);
  }

  public static VMInstr NOP() {
    return new VMInstr(OpCode.NOP);
  }
  
}

