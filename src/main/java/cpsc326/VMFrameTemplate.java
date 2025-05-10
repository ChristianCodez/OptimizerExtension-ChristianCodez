/**
 * CPSC 326, Spring 2025
 * The information needed for creating a specific frame.
 */

package cpsc326;

import java.util.List;
import java.util.ArrayList;


/**
 * Class for holding the frame's function name and instructions.
 */
public class VMFrameTemplate {

  /* name of the function */
  public String functionName;

  /* list of instructions defining the function */
  public List<VMInstr> instructions = new ArrayList<>();

  /**
   * Create a new template
   */
  public VMFrameTemplate(String functionName) {
    this.functionName = functionName;
  }

  /**
   * Add an instruction
   * @param instr the instruction to add
   */
  public void add(VMInstr instr) {
    instructions.add(instr);
  }

  /**
   * Get the instruction at the given index
   * @param instrIndex the index of the instruction
   * @returns the instruction or null if the index is invalid
   */
  public VMInstr get(int instrIndex) {
    if (instrIndex < 0 || instrIndex >= instructions.size())
      return null;
    return instructions.get(instrIndex);
  }

}
