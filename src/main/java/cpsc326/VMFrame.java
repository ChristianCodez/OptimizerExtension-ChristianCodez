/**
 * CPSC 326, Spring 2025
 * Represents a function call.
 */

package cpsc326;

import java.util.List;
import java.util.ArrayList;


/**
 * Class for representing function call information.
 */
public class VMFrame {

  /* the template this frame is an instance of */
  public VMFrameTemplate template;

  /* the memory associated with variables of the frame */
  public List<Object> memory = new ArrayList<>();

  /* the current instruction index (program counter) */
  public int pc = 0;

  
  /**
   * Create (instantiate) a frame with the given template
   */
  public VMFrame(VMFrameTemplate template) {
    this.template = template;
  }
  
}

