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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.JPFListener;
import gov.nasa.jpf.jvm.classfile.ClassFile;

/**
 * interface to register for callbacks by the VM
 * Observer role in equally named pattern
 * 
 * Note that we only have notifications for generic events, NOT for conditions that
 * are property specific, and especially nothing that is just triggered from an extension.
 * If listeners are used to implement high level properties, the notifications should be
 * used to implement properties, not to report some property violation that was detected
 * by JPF 
 */
public interface VMListener extends JPFListener {
  
  /**
   * VM got initialized (but search is not yet running). This can be used to
   * do type initialization in listeners, since the ClassLoader mechanism is now functional
   */
  void vmInitialized (VM vm);
  
  /**
   * VM is about to execute the next instruction
   */
  void executeInstruction (VM vm, ThreadInfo currentThread, Instruction instructionToExecute);
  
  /**
   * VM has executed the next instruction
   * (can be used to analyze branches, monitor PUTFIELD / GETFIELD and
   * INVOKExx / RETURN instructions)
   */
  void instructionExecuted (VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction);
  
  /**
   * new Thread entered run() method
   */
  void threadStarted (VM vm, ThreadInfo startedThread);
    
  /**
   * thread waits to acquire a lock
  // NOTE: vm.getLastThreadInfo() does NOT have to be the running thread, as this
  // notification can occur as a result of a lock operation in the current thread
   */
  void threadBlocked (VM vm, ThreadInfo blockedThread, ElementInfo lock);
  
  /**
   * thread is waiting for signal
   */
  void threadWaiting (VM vm, ThreadInfo waitingThread);

  /**
   * thread got notified
   */
  void threadNotified (VM vm, ThreadInfo notifiedThread);
    
  /**
   * thread got interrupted
   */
  void threadInterrupted (VM vm, ThreadInfo interruptedThread);
  
  /**
   * Thread exited run() method
   */
  void threadTerminated (VM vm, ThreadInfo terminatedThread);

  /**
   * new thread was scheduled by VM
   */
  void threadScheduled (VM vm, ThreadInfo scheduledThread); // this might go into the choice generator notifications

  /**
   * a new classfile is about to be parsed. This notification allows replacement
   * of the related classfile data via ClassFile.{get/set}Data() and can be
   * used to do on-the-fly classfile instrumentation with 3rd party libraries 
   */
  public void loadClass (VM vm, ClassFile cf);
  
  /**
   * new class was loaded. This is notified after the ClassInfo has been
   * instantiated, but before the class object is initialized, i.e. clinit
   * is called. The main use for this notification is to identify and 
   * store ClassInfos, MethodInfos, FieldInfos or Instructions that are
   * used by listeners etc. in order to enable efficient identify based filters
   * in the performance critical instruction notifications
   */
  void classLoaded (VM vm, ClassInfo loadedClass);
  
  /**
   * new object was created
   */
  void objectCreated (VM vm, ThreadInfo currentThread, ElementInfo newObject);
  
  /**
   * object was garbage collected (after potential finalization)
   */
  void objectReleased (VM vm, ThreadInfo currentThread, ElementInfo releasedObject);
  
  /**
   * notify if an object lock was taken (this includes automatic
   * surrender during a wait())
   */
  void objectLocked (VM vm, ThreadInfo currentThread, ElementInfo lockedObject);
  
  /**
   * notify if an object lock was released (this includes automatic
   * reacquisition after a notify())
   */
  void objectUnlocked (VM vm, ThreadInfo currentThread, ElementInfo unlockedObject);
  
  /**
   * notify if a wait() is executed
   */
  void objectWait (VM vm, ThreadInfo currentThread, ElementInfo waitingObject);
  
  /**
   * notify if an object notifies a single waiter
   */
  void objectNotify (VM vm, ThreadInfo currentThread, ElementInfo notifyingObject);

  /**
   * notify if an object notifies all waiters
   */
  void objectNotifyAll (VM vm, ThreadInfo currentThread, ElementInfo notifyingObject);
  
  void gcBegin (VM vm);
  
  void gcEnd (VM vm);
  
  /**
   * exception was thrown
   */
  void exceptionThrown (VM vm, ThreadInfo currentThread, ElementInfo thrownException);

  /**
   * exception causes top frame to be purged
   */
  void exceptionBailout (VM vm, ThreadInfo currentThread);

  /**
   * exception handled by current top frame
   */
  void exceptionHandled (VM vm, ThreadInfo currentThread);

  /**
   * next ChoiceGenerator was registered, which means this is the end of the current transition
   * 
   * the reason why we have this in addition to the choiceGeneratorSet is that listeners
   * can reset the registered CG and so force the current transition to continue (although the
   * listener in this case has to make sure the operand stack is in a consistent state for
   * continued execution because there might be a bottom half of an Instruction.execute() missing)
   */
  void choiceGeneratorRegistered (VM vm, ChoiceGenerator<?> nextCG, ThreadInfo currentThread, Instruction executedInstruction);

  /**
   * a new ChoiceGenerator was set, which means we are at the beginning of a new transition.
   *
   * NOTE - this notification happens before the KernelState is stored, i.e. listeners are NOT
   * allowed to alter the KernelState (e.g. by changing field values or thread states)
   */
  void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG);
  
  /**
   * the next choice was requested from a previously registered ChoiceGenerator
   *
   * NOTE - this notification happens before the KernelState is stored, i.e. listeners are NOT
   * allowed to alter the KernelState (e.g. by changing field values or thread states)
   */
  void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG);
  
  /**
   * a ChoiceGnerator has returned all his choices
   *
   * NOTE - this notification happens before the KernelState is stored, i.e. listeners are NOT
   * allowed to alter the KernelState (e.g. by changing field values or thread states)
   */
  void choiceGeneratorProcessed (VM vm, ChoiceGenerator<?> processedCG);

  /**
   * method body was entered. This is notified before the first instruction
   * is executed
   */
  void methodEntered (VM vm, ThreadInfo currentThread, MethodInfo enteredMethod);

  /**
   * method body was left. This is notified after the last instruction had
   * been executed
   * NOTE - this is also notified when a StackFrame is dropped due to unhandled exceptions
   */
  void methodExited (VM vm, ThreadInfo currentThread, MethodInfo exitedMethod);

}
