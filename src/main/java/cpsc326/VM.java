/**
 * CPSC 326, Spring 2025
 * The virtual machine implementation.
 */

package cpsc326;

import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;

/**
 * MyPL virtual machine for running MyPL programs (as VM
 * instructions).
 */
public class VM {

  /* special NULL value */
  public static final Object NULL = new Object() {
    public String toString() {
      return "null";
    }
  };

  /* the array heap as an oid to list mapping */
  private Map<Integer, List<Object>> arrayHeap = new HashMap<>();

  /* the struct heap as an oid to object (field to value map) mapping */
  private Map<Integer, Map<String, Object>> structHeap = new HashMap<>();

  /* the operand stack */
  private Deque<Object> operandStack = new ArrayDeque<>();

  /* the function (frame) call stack */
  private Deque<VMFrame> callStack = new ArrayDeque<>();

  /* the set of program function definitions (frame templates) */
  private Map<String, VMFrameTemplate> templates = new HashMap<>();

  /* the next unused object id */
  private int nextObjectId = 2025;

  /* debug flag for output debug info during vm execution (run) */
  private boolean debug = false;

  private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

  // helper functions

  /**
   * Create and throw an error.
   * 
   * @param msg The error message.
   */
  private void error(String msg) {
    MyPLException.vmError(msg);
  }

  /**
   * Create and throw an error (for a specific frame).
   * 
   * @param msg   The error message.
   * @param frame The frame where the error occurred.
   */
  private void error(String msg, VMFrame frame) {
    String s = "%s in %s at %d: %s";
    String name = frame.template.functionName;
    int pc = frame.pc - 1;
    VMInstr instr = frame.template.instructions.get(pc);
    MyPLException.vmError(String.format(s, msg, name, pc, instr));
  }

  /**
   * Add a frame template to the VM.
   * 
   * @param template The template to add.
   */
  public void add(VMFrameTemplate template) {
    templates.put(template.functionName, template);
  }

  /**
   * For turning on debug mode to help with debugging the VM.
   * 
   * @param on Set to true to turn on debugging, false to turn it off.
   */
  public void debugMode(boolean on) {
    debug = on;
  }

  /**
   * Pretty-print the VM frames.
   */
  public String toString() {
    String s = "";
    for (var funName : templates.keySet()) {
      s += String.format("\nFrame '%s'\n", funName);
      VMFrameTemplate template = templates.get(funName);
      for (int i = 0; i < template.instructions.size(); ++i)
        s += String.format("  %d: %s\n", i, template.instructions.get(i));
    }
    return s;
  }

  // Additional helpers for implementing the VM instructions

  /**
   * Helper to ensure the given value isn't NULL
   * 
   * @param x     the value to check
   * @param frame the current stack frame
   */
  private void ensureNotNull(Object x, VMFrame frame) {
    if (x == NULL)
      error("null value error", frame);
  }

  /**
   * Helper to add two objects
   */
  private Object addHelper(Object x, Object y) {
    if (x instanceof Integer)
      return (int) x + (int) y;
    else if (x instanceof Double)
      return (double) x + (double) y;
    else
      return (String) x + (String) y;
  }

  /**
   * Helper to subtract two objects
   */
  private Object subHelper(Object x, Object y) {
    if (x instanceof Integer)
      return (int) x - (int) y;
    else
      return (double) x - (double) y;
  }

  /**
   * Helper to multiply two objects
   */
  private Object mulHelper(Object x, Object y) {
    if (x instanceof Integer)
      return (int) x * (int) y;
    else
      return (double) x * (double) y;
  }

  /**
   * Helper to divide two objects
   */
  private Object divHelper(Object x, Object y, VMFrame f) {
    if (x instanceof Integer && (int) y != 0)
      return (int) ((int) x / (int) y);
    else if (x instanceof Double && (double) y != 0.0)
      return (double) x / (double) y;
    else
      error("division by zero error", f);
    return null;
  }

  /**
   * Helper to compare if first object less than second
   */
  private Object cmpltHelper(Object x, Object y) {
    if (x instanceof Integer)
      return (int) x < (int) y;
    else if (x instanceof Double)
      return (double) x < (double) y;
    else
      return ((String) x).compareTo((String) y) < 0;
  }

  /**
   * Helper to compare if first object less than or equal second
   */
  private Object cmpleHelper(Object x, Object y) {
    if (x instanceof Integer)
      return (int) x <= (int) y;
    else if (x instanceof Double)
      return (double) x <= (double) y;
    else
      return ((String) x).compareTo((String) y) <= 0;
  }

  // the main run method

  /**
   * Execute the program
   */
  public void run() {
    // grab the main frame and "instantiate" it
    if (!templates.containsKey("main"))
      error("No 'main' function");
    VMFrame frame = new VMFrame(templates.get("main"));
    callStack.push(frame);

    // run loop until out of call frames or instructions in the frame
    while (!callStack.isEmpty() && frame.pc < frame.template.instructions.size()) {
      // get the next instruction
      VMInstr instr = frame.template.instructions.get(frame.pc);

      // for debugging:
      if (debug) {
        System.out.println();
        System.out.println("\t FRAME.........: " + frame.template.functionName);
        System.out.println("\t PC............: " + frame.pc);
        System.out.println("\t INSTRUCTION...: " + instr);
        Object val = operandStack.isEmpty() ? null : operandStack.peek();
        System.out.println("\t NEXT OPERAND..: " + val);
      }

      // increment the pc
      ++frame.pc;

      // ----------------------------------------------------------------------
      // Literals and Variables
      // ----------------------------------------------------------------------

      if (instr.opcode == OpCode.PUSH) {
        operandStack.push(instr.operand);
      }

      else if (instr.opcode == OpCode.POP) {
        operandStack.pop();
      }

      else if (instr.opcode == OpCode.LOAD) {
        operandStack.push(frame.memory.get((int) instr.operand));
      }

      // TODO: Implement the remaining instructions (except for DUP and NOP, see
      // below) ...
      // -- see lecture notes for hints and tips
      //
      // Additional Hints:
      // -- use ensureNotNull(v, frame) if operand can't be null
      // -- Deque supports pop(), peek(), isEmpty()
      // -- for WRITE, use System.out.print(...)
      // -- for READ, use: new BufferedReader(new InputStreamReader(System.in)) and
      // readLine()
      // -- for LEN, can check type via: if (value instanceof String) ...
      // -- for GETC, can use String charAt() function
      // -- for TOINT, can use intValue() on Double
      // -- for TOINT, can use Integer.parseInt(...) for String (in try-catch block)
      // -- similarly for TODBL (but with corresponding Double versions)
      // -- for TOSTR, can use String.valueOf(...)
      // -- in a number of places, can cast if type known, e.g., ((int)length)

      else if (instr.opcode == OpCode.STORE) {
        Object val = operandStack.pop();

        if (!(instr.operand instanceof Integer)) {
          error("invalid index");
        }
        if ((int) instr.operand > frame.memory.size() || (int) instr.operand < 0) {
          error("memory index out of bounds");
        }
        if ((int) instr.operand == frame.memory.size()) {
          frame.memory.add(val);
        } else {
          frame.memory.set((int) instr.operand, val);
        }
      }

      else if (instr.opcode == OpCode.ADD) {
        Object x = operandStack.pop();
        Object y = operandStack.pop();

        if (x.getClass() != y.getClass()) {
          error("To add they must have the same type");
        }
        operandStack.push(addHelper(y, x));
      }

      else if (instr.opcode == OpCode.SUB) {
        Object x = operandStack.pop();
        Object y = operandStack.pop();

        if (x.getClass() != y.getClass()) {
          error("To add they must have the same type");
        }

        operandStack.push(subHelper(y, x));
      }

      else if (instr.opcode == OpCode.MUL) {
        Object x = operandStack.pop();
        Object y = operandStack.pop();

        if (x.getClass() != y.getClass()) {
          error("To add they must have the same type");
        }

        operandStack.push(mulHelper(x, y));
      }

      else if (instr.opcode == OpCode.DIV) {
        Object x = operandStack.pop();
        Object y = operandStack.pop();

        if (x.getClass() != y.getClass()) {
          error("To add they must have the same type");
        }

        operandStack.push(divHelper(y, x, frame));
      }

      else if (instr.opcode == OpCode.AND) {
        Object boolX = operandStack.pop();
        Object boolY = operandStack.pop();

        if (!(boolX instanceof Boolean && boolY instanceof Boolean)) {
          error("operands must be booleans");
        }

        operandStack.push((boolean) boolX && (boolean) boolY);
      }

      else if (instr.opcode == OpCode.OR) {
        Object boolX = operandStack.pop();
        Object boolY = operandStack.pop();

        if (!(boolX instanceof Boolean && boolY instanceof Boolean)) {
          error("operands must be booleans");
        }

        operandStack.push((boolean) boolX || (boolean) boolY);
      }

      else if (instr.opcode == OpCode.NOT) {
        Object boolX = operandStack.pop();
        if (!(boolX instanceof Boolean)) {
          error("operands must be booleans");
        }

        operandStack.push(!((boolean) boolX));
      }

      else if (instr.opcode == OpCode.CMPLT) {
        Object x = operandStack.pop();
        Object y = operandStack.pop();

        if (x.getClass() != y.getClass()) {
          error("To add they must have the same type");
        }

        operandStack.push(cmpltHelper(y, x));
      }

      else if (instr.opcode == OpCode.CMPLE) {
        Object x = operandStack.pop();
        Object y = operandStack.pop();

        if (x.getClass() != y.getClass()) {
          error("To add they must have the same type");
        }

        operandStack.push(cmpleHelper(y, x));
      }

      else if (instr.opcode == OpCode.CMPEQ) {
        Object x = operandStack.pop();
        Object y = operandStack.pop();

        operandStack.push(x.equals(y));
      }

      else if (instr.opcode == OpCode.CMPNE) {
        Object x = operandStack.pop();
        Object y = operandStack.pop();

        operandStack.push(!(x.equals(y)));
      }

      else if (instr.opcode == OpCode.JMP) {
        if (!(instr.operand instanceof Integer))
          error("operand must be an Integer");
        else
          frame.pc = (int) instr.operand;
      }

      else if (instr.opcode == OpCode.JMPF) {
        Object val = operandStack.pop();

        if (!(val instanceof Boolean))
          error("Operand must be a boolean");

        if (!((boolean) val)) { // if val is false then continue to body -- jump
          if (!(instr.operand instanceof Integer)) {
            error("jump target must be an Integer");
          }
          frame.pc = (int) instr.operand;
        }
      }

      else if (instr.opcode == OpCode.CALL) {
        if (!(instr.operand instanceof String)) {
          error("Function name must be a string");
        }

        String name = (String) instr.operand;

        if (!templates.containsKey(name)) {
          error(name + " not recognized");
        }

        VMFrame newFrame = new VMFrame(templates.get(name));

        callStack.push(newFrame);
        newFrame.pc = 0;
        frame = newFrame;
      }

      else if (instr.opcode == OpCode.RET) {
        Object returnValue = operandStack.isEmpty() ? NULL : operandStack.pop(); // save return value

        callStack.pop(); // get rid of current function frame

        if (!callStack.isEmpty()) {
          frame = callStack.peek(); // get the previous frame
          operandStack.push(returnValue); // Push return value for the caller
        }
      }

      else if (instr.opcode == OpCode.ALLOCS) {
        int oid = nextObjectId;
        structHeap.put(oid, new HashMap<>());
        operandStack.push(oid);
        nextObjectId++;
      }

      else if (instr.opcode == OpCode.SETF) {
        Object val = operandStack.pop();
        Object oid = operandStack.pop();

        if (!(oid instanceof Integer) || !structHeap.containsKey((int) oid)) {
          error("Invalid struct object ID");
        }

        Map<String, Object> struct = structHeap.get((int) oid); // modify existing struct

        if (!(instr.operand instanceof String)) {
          error("Field name must be a string");
        }

        struct.put((String) instr.operand, val);
      }

      else if (instr.opcode == OpCode.GETF) {
        Object oid = operandStack.pop();

        if (!(oid instanceof Integer) || !structHeap.containsKey((int) oid)) {
          error("Invalid struct object ID");
        }

        if (!(instr.operand instanceof String)) {
          error("Field name must be a string");
        }

        Map<String, Object> struct = structHeap.get((int) oid);

        if (!(struct.containsKey((String) instr.operand))) {
          error((String) instr.operand + " does not exist");
        }

        operandStack.push(struct.get((String) instr.operand));
      }

      else if (instr.opcode == OpCode.ALLOCA) {
        Object x = operandStack.pop();

        if (!(x instanceof Integer) || (int) x < 0) {
          error("not a valid array size, must be a non-negative integer");
        }

        int oid = nextObjectId;

        List<Object> list = new ArrayList<>((int) x);

        for (int i = 0; i < ((int) x); i++) {
          list.add(VM.NULL);
        }

        arrayHeap.put(oid, list);
        operandStack.push(oid);
        nextObjectId++;
      }

      else if (instr.opcode == OpCode.SETI) {
        Object val = operandStack.pop();
        Object index = operandStack.pop();
        Object oid = operandStack.pop();

        if (!(oid instanceof Integer) || !arrayHeap.containsKey((int) oid)) {
          error("Invalid array object ID");
        }

        if (!(index instanceof Integer) || (int) index < 0) {
          error("not a valid array index, must be a non-negative integer");
        }

        List<Object> array = arrayHeap.get((int) oid);

        if ((int) index >= array.size()) {
          error("SETI: index out of bounds");
        }

        array.set((int) index, val);
      }

      else if (instr.opcode == OpCode.GETI) {
        Object index = operandStack.pop();
        Object oid = operandStack.pop();

        if (!(oid instanceof Integer) || !arrayHeap.containsKey((int) oid)) {
          error("Invalid array object ID");
        }

        if (!(index instanceof Integer) || (int) index < 0) {
          error("not a valid array index, must be a non-negative integer");
        }

        List<Object> array = arrayHeap.get((int) oid);

        if ((int) index >= array.size()) {
          error("GETI: index out of bounds");
        }

        operandStack.push(array.get((int) index));
      }

      else if (instr.opcode == OpCode.WRITE) {
        Object x = operandStack.pop();
        System.out.print(x);
      }

      else if (instr.opcode == OpCode.READ) {
        try {
          String input = reader.readLine();
          operandStack.push(input); // in lecture notes didn't seem like i push this, might have to change
        } catch (IOException e) {
          error("input error: " + e.getMessage());
        }
      }

      else if (instr.opcode == OpCode.LEN) {
        Object x = operandStack.pop(); // oid
        int len = 0;

        ensureNotNull(x, frame);

        if (x instanceof String) { // here we want the length of a string
          len = ((String) x).length();
        } else if (x instanceof Integer) {
          List<Object> array = arrayHeap.get((int) x);
          len = array.size();
        } else {
          error("invalid LEN call, must be on a String or Array");
        }

        operandStack.push(len);
      }

      else if (instr.opcode == OpCode.GETC) {
        Object index = operandStack.pop();
        Object string = operandStack.pop();

        if (!(string instanceof String)) {
          error("incorrect type, must be a string");
        }
        if (!(index instanceof Integer)) {
          error("invalid index; must be of type integer");
        }
        if ((int) index >= ((String) string).length() || (int) index < 0) {
          error("GETC: index out of bounds");
        }

        operandStack.push(Character.toString(((String) string).charAt((int) index)));
      }

      else if (instr.opcode == OpCode.TOINT) {
        Object val = operandStack.pop();

        ensureNotNull(val, frame);

        if (val instanceof Integer) {
          val = (int) val;
        } else if (val instanceof String) {
          try {
            val = Integer.valueOf((String) val);
          } catch (NumberFormatException e) {
            error("invalid string to call TOINT");
          }
        } else if (val instanceof Double) {
          val = (int) Math.floor((double) val);
        }

        operandStack.push(val);
      }

      else if (instr.opcode == OpCode.TODBL) {
        Object val = operandStack.pop();

        ensureNotNull(val, frame);

        if (val instanceof Double) {
          val = (double) val;
        } else if (val instanceof String) {
          try {
            val = Double.valueOf((String) val);
          } catch (NumberFormatException e) {
            error("invalid string to call TODBL");
          }
        } else if (val instanceof Integer) {
          val = (double) (int) val;
        }

        operandStack.push(val);
      }

      else if (instr.opcode == OpCode.TOSTR) {
        Object val = operandStack.pop();

        ensureNotNull(val, frame);

        if (val instanceof String) {
          val = (String) val;
        } else if (val instanceof Integer) {
          val = Integer.toString((int) val);
        } else if (val instanceof Double) {
          val = Double.toString((double) val);
        }

        operandStack.push((String) val);
      }

      // ----------------------------------------------------------------------
      // Special Instructions
      // ----------------------------------------------------------------------

      else if (instr.opcode == OpCode.DUP) {
        Object val = operandStack.pop();
        operandStack.push(val);
        operandStack.push(val);
      }

      else if (instr.opcode == OpCode.NOP) {
        // do nothing
      }

      else
        error("Unsupported operation: " + instr);
    }

  }

}
