//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;


/**
 * Branch always
 * No change
 *
 * <2do> store this as code insnIndex, not as bytecode position
 */
public class GOTO extends Instruction implements JVMInstruction {
  protected int targetPosition;
  Instruction target;

  public GOTO (int targetPosition){
    this.targetPosition = targetPosition;
  }

  public Instruction execute (ThreadInfo ti) {
    if (isBackJump() && ti.maxTransitionLengthExceeded()){
      // this is a rather simplistic attempt to terminate the search in
      // endless loops that do not change program state.
      // <2do> this does not handle nested loops yet
      if (ti.breakTransition("MAX_TRANSITION_LENGTH")){
        return this; // re-execute after giving state matching a chance to prune the search
      }
    }
    
    return getTarget();
  }

  public boolean isBackJump () {
    return (targetPosition <= position);
  }
  
  public Instruction getTarget() {
    if (target == null) {
      target = mi.getInstructionAt(targetPosition);
    }
    return target;
  }

  public int getLength() {
    return 3; // opcode, bb1, bb2
  }
  
  public int getByteCode () {
    return 0xA7;
  }
  
  public String toString () {
    return getMnemonic() + " " + targetPosition;
  }
  
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  @Override
  public Instruction typeSafeClone(MethodInfo mi) {
    GOTO clone = null;

    try {
      clone = (GOTO) super.clone();

      // reset the method that this insn belongs to
      clone.mi = mi;

      clone.target = null;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    return clone;
  }
}
