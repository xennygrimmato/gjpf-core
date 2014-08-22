//
// Copyright (C) 2014 United States Government as represented by the
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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.SystemAttribute;
import gov.nasa.jpf.util.FieldSpecMatcher;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.MethodSpecMatcher;
import gov.nasa.jpf.util.TypeSpecMatcher;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;

/**
 * an abstract SharednessPolicy implementation that is independent of any
 * SyncPolicy and makes use of both shared field access CGs and exposure CGs.
 * 
 * This class is highly configurable, both in terms of using exposure CGs and filters.
 * The *never_break filters should be used with care to avoid missing defects, especially
 * the (transitive) method filters.
 * NOTE - the default settings from jpf-core/jpf.properties include several
 * java.util.concurrent* and java.lang.* fields/methods that can in fact contribute to
 * concurrency defects, esp. in SUTs that explicitly use Thread/ThreadGroup objects, in
 * which case they should be removed.
 * 
 * The *always_break field filter should only be used for white box SUT analysis if JPF
 * fails to detect sharedness (e.g. because no exposure is used). This should only
 * go into application property files
 */
public abstract class GenericSharednessPolicy implements SharednessPolicy, Attributor {
  
  //--- auxiliary types to configure filters
  static class NeverBreakIn implements SystemAttribute {
    static NeverBreakIn singleton = new NeverBreakIn();
  } 
  static class NeverBreakOn implements SystemAttribute {
    static NeverBreakOn singleton = new NeverBreakOn();
  } 
  static class AlwaysBreakOn implements SystemAttribute {
    static AlwaysBreakOn singleton = new AlwaysBreakOn();
  } 
  
  protected static JPFLogger logger = JPF.getLogger("shared");
  
  
  //--- options used for concurrent field access detection
  
  /**
   * never break or expose if a matching method is on the stack
   */
  protected MethodSpecMatcher neverBreakInMethods;
  
  /**
   * never break on matching fields 
   */  
  protected FieldSpecMatcher neverBreakOnFields;
    
  /**
   * always break matching fields, no matter if object is already shared or not
   */  
  protected FieldSpecMatcher alwaysBreakOnFields;
  

  /**
   * do we break on final field access 
   */
  protected boolean skipFinals;
  protected boolean skipConstructedFinals;
  protected boolean skipStaticFinals;
  
  /**
   * do we break inside of constructors
   * (note that 'this' references could leak from ctors, but
   * this is rather unusual)
   */
  protected boolean skipInits;

  /**
   * do we add CGs for objects that could become shared, e.g. when storing
   * a reference to a non-shared object in a shared object field.
   * NOTE: this is a conservative measure since we don't know yet at the
   * point of exposure if the object will ever be shared, which means it
   * can cause state explosion.
   */
  protected boolean breakOnExposure;
  
  /**
   * options to filter out lock protected field access, which is not
   * supposed to cause shared CGs
   * (this has no effect on exposure though)
   */
  protected boolean useSyncDetection;
  protected int lockThreshold;  
  
  protected VM vm;
  
  
  protected GenericSharednessPolicy (Config config){
    neverBreakInMethods = MethodSpecMatcher.create( config.getStringArray("vm.shared.never_break_methods"));
    neverBreakOnFields = FieldSpecMatcher.create( config.getStringArray("vm.shared.never_break_fields"));
    alwaysBreakOnFields = FieldSpecMatcher.create( config.getStringArray("vm.shared.always_break_fields"));
    
    skipFinals = config.getBoolean("vm.shared.skip_finals", true);
    skipConstructedFinals = config.getBoolean("vm.shared.skip_constructed_finals", false);
    skipStaticFinals = config.getBoolean("vm.shared.skip_static_finals", true);
    skipInits = config.getBoolean("vm.shared.skip_inits", true);
    
    breakOnExposure = config.getBoolean("vm.shared.break_on_exposure", true);
    
    useSyncDetection = config.getBoolean("vm.shared.sync_detection", true);
    lockThreshold = config.getInt("vm.shared.lockthreshold", 5);  
  }
  
  //--- internal methods (potentially overridden by subclass)
  
  
  //--- attribute management
  
  protected void setTypeAttribute (TypeSpecMatcher matcher, ClassInfo ci, Object attr){
    if (matcher != null){
      if (matcher.matches(ci)){
        ci.addAttr(attr);
      }
    }    
  }
  
  protected void setFieldAttributes (FieldSpecMatcher neverMatcher, FieldSpecMatcher alwaysMatcher, ClassInfo ci){
    for (FieldInfo fi : ci.getDeclaredInstanceFields()) {
      // invisible fields (created by compiler)
      if (fi.getName().startsWith("this$")) {
        fi.addAttr( NeverBreakOn.singleton);
        continue;
      }        

      // configuration
      if (neverMatcher != null && neverMatcher.matches(fi)) {
        fi.addAttr( NeverBreakOn.singleton);
      }
      if (alwaysMatcher != null && alwaysMatcher.matches(fi)) {
        fi.addAttr( AlwaysBreakOn.singleton);
      }
      
      // annotation
      if (fi.hasAnnotation("gov.nasa.jpf.annotation.NeverBreak")){
        fi.addAttr( NeverBreakOn.singleton);        
      }
    }

    for (FieldInfo fi : ci.getDeclaredStaticFields()) {
      // invisible fields (created by compiler)
      if ("$assertionsDisabled".equals(fi.getName())) {
        fi.addAttr( NeverBreakOn.singleton);
        continue;
      }

      // configuration
      if (neverMatcher != null && neverMatcher.matches(fi)) {
        fi.addAttr( NeverBreakOn.singleton);
      }
      if (alwaysMatcher != null && alwaysMatcher.matches(fi)) {
        fi.addAttr( AlwaysBreakOn.singleton);
      }
      
      // annotation
      if (fi.hasAnnotation("gov.nasa.jpf.annotation.NeverBreak")){
        fi.addAttr( NeverBreakOn.singleton);        
      }
    }
  }
  
  protected abstract boolean checkOtherRunnables (ThreadInfo ti);
  
  // this needs a three-way return value, hence Boolean
  protected Boolean canHaveSharednessCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    //--- thread
    if (ti.isFirstStepInsn()){ // no empty transitions
      return Boolean.FALSE;
    }
    
    if (!checkOtherRunnables(ti)){ // nothing to reschedule
      return Boolean.FALSE;
    }
    
    if (ti.hasAttr( NeverBreakIn.class)){
      return Boolean.FALSE;
    }
    
    //--- call site (method)
    for (StackFrame frame = ti.getTopFrame(); frame != null; frame=frame.getPrevious()){
      MethodInfo mi = frame.getMethodInfo();
      if (mi.hasAttr( NeverBreakIn.class)){
        return Boolean.FALSE;
      }
    }
    
    //--- field
    if (fi != null){
      if (fi.hasAttr(AlwaysBreakOn.class)) {
        return Boolean.TRUE;
      }
      if (fi.hasAttr(NeverBreakOn.class)) {
        return Boolean.FALSE;
      }
    }
    
    return null;    
  }

  //--- FieldLockInfo management
  
  /**
   * factory method called during object creation 
   */
  protected abstract FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi);

  
  /**
   * generic version of FieldLockInfo update, which relies on FieldLockInfo implementation to determine
   * if ElementInfo needs to be cloned
   */  
  protected ElementInfo updateFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    FieldLockInfo fli = ei.getFieldLockInfo(fi);
    if (fli == null){
      fli = createFieldLockInfo(ti, ei, fi);
      ei = ei.getModifiableInstance();
      ei.setFieldLockInfo(fi, fli);
      
    } else {
      FieldLockInfo newFli = fli.checkProtection(ti, ei, fi);
      if (newFli != fli) {
        ei = ei.getModifiableInstance();
        ei.setFieldLockInfo(fi,newFli);
      }
    }
    
    return ei;
  }
  
  
  //--- runnable computation & CG creation

  // NOTE - we don't schedule threads outside this process since field access if process local
  
  protected ThreadInfo[] getRunnables (ApplicationContext appCtx){
    return vm.getThreadList().getProcessTimeoutRunnables(appCtx);
  }
  
  protected ChoiceGenerator<ThreadInfo> getRunnableCG (String id, ThreadInfo tiCurrent){
    if (vm.getSystemState().isAtomic()){ // no CG if we are in a atomic section
      return null;
    }
    
    ThreadInfo[] choices = getRunnables(tiCurrent.getApplicationContext());
    if (choices.length <= 1){ // field access doesn't block, i.e. the current thread is always runnable
      return null;
    }
    
    return new ThreadChoiceFromSet( id, choices, true);
  }
  
  protected boolean setNextChoiceGenerator (ChoiceGenerator<ThreadInfo> cg){
    if (cg != null){
      return vm.getSystemState().setNextChoiceGenerator(cg); // listeners could still remove CGs
    }
    
    return false;
  }
  
  
  //--- sharedness and exposure
  
  protected ElementInfo updateSharedness (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    ThreadInfoSet tis = ei.getReferencingThreads();
    ThreadInfoSet newTis = tis.add(ti);
    
    if (tis != newTis){
      ei = ei.getModifiableInstance();
      ei.setReferencingThreads(newTis);
    }
      
    // we only change from non-shared to shared
    if (newTis.isShared(ti, ei) && !ei.isShared() && !ei.isSharednessFrozen()) {
      ei = ei.getModifiableInstance();
      ei.setShared(ti, true);
    }

    if (ei.isShared() && fi != null){
      ei = updateFieldLockInfo(ti,ei,fi);
    }
    
    return ei;
  }

  protected boolean setsExposureCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, ElementInfo eiExposed){
    if (breakOnExposure){
      if (eiFieldOwner.isExposedOrShared()){
        // don't check against the 'old' field value because this might get called after the field was already updated
        // we should solely depend on different object sharedness
        if (isFirstExposure(eiFieldOwner, eiExposed)) {
          eiExposed = eiExposed.getExposedInstance(ti, eiFieldOwner);
          logger.info("exposure CG setting field ", fi, " to ", eiExposed);
          return setNextChoiceGenerator(getRunnableCG("EXPOSE", ti));
        }
      }
    }
    
    return false;
  }

  protected boolean isFirstExposure (ElementInfo eiFieldOwner, ElementInfo eiExposed){
    if (!eiExposed.isImmutable()){
      if (!eiExposed.isExposedOrShared()) {
         return (eiFieldOwner.isExposedOrShared());
      }
    }
    
    return false;  
  }

  
  //------------------------------------------------ Attributor interface
    
  /**
   * this can be used to initializeSharednessPolicy per-application mechanisms such as ClassInfo attribution
   */
  @Override
  public void initializeSharednessPolicy (VM vm, ApplicationContext appCtx){
    this.vm = vm;
    
    SystemClassLoaderInfo sysCl = appCtx.getSystemClassLoader();
    sysCl.addAttributor(this);
  }
  
  
  @Override
  public void setAttributes (ClassInfo ci){    
    setFieldAttributes( neverBreakOnFields, alwaysBreakOnFields, ci);
    
    // this one is more expensive to iterate over and should be avoided
    if (neverBreakInMethods != null){
      for (MethodInfo mi : ci.getDeclaredMethods().values()){
        if (neverBreakInMethods.matches(mi)){
          mi.setAttr( NeverBreakIn.singleton);
        }
      }
    }
    
  }
    
  //------------------------------------------------ SharednessPolicy interface
  
  @Override
  public ElementInfo updateObjectSharedness (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    return updateSharedness(ti, ei, fi);
  }
  @Override
  public ElementInfo updateClassSharedness (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    return updateSharedness(ti, ei, fi);
  }
  @Override
  public ElementInfo updateArraySharedness (ThreadInfo ti, ElementInfo ei, int idx){
    // NOTE - we don't support per-element FieldLockInfos (yet)
    return updateSharedness(ti, ei, null);
  }

  
  /**
   * check to determine if call site, object/class attributes and thread execution state
   * could cause CGs. This is called before sharedness is updated, i.e. can be used to
   * filter objects/classes that should not be sharedness tracked
   */
  @Override
  public boolean canHaveSharedObjectCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    Boolean ret = canHaveSharednessCG( ti, insn, eiFieldOwner, fi);
    if (ret != null){
      return ret;
    }
    
    if  (eiFieldOwner.isImmutable()){
      return false;
    }
    
    if (skipFinals && fi.isFinal()){
      return false;
    }
            
    //--- mixed (dynamic) attributes
    if (skipConstructedFinals && fi.isFinal() && eiFieldOwner.isConstructed()){
      return false;
    }
    
    if (skipInits && insn.getMethodInfo().isInit()){
      return false;
    }
    
    return true;
  }
  
  @Override
  public boolean canHaveSharedClassCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    Boolean ret = canHaveSharednessCG( ti, insn, eiFieldOwner, fi);
    if (ret != null){
      return ret;
    }

    if  (eiFieldOwner.isImmutable()){
      return false;
    }
    
    if (skipStaticFinals && fi.isFinal()){
      return false;
    }

    // call site. This could be transitive, in which case it has to be dynamic and can't be moved to isRelevant..()
    MethodInfo mi = insn.getMethodInfo();
    if (mi.isClinit() && (fi.getClassInfo() == mi.getClassInfo())) {
      // clinits are all synchronized, so they are lock protected per se
      return false;
    }
    
    return true;
  }
  
  @Override
  public boolean canHaveSharedArrayCG (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int idx){
    Boolean ret = canHaveSharednessCG( ti, insn, eiArray, null);
    if (ret != null){
      return ret;
    }

    // more array specific checks here
    
    return true;
  }
  
  
  /**
   * those are the public interfaces towards the FieldInstructions, which have to be aware of
   * that the field owning ElementInfo (instance or static) will change if it becomes shared
   */
  @Override
  public boolean setsSharedObjectCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    if (eiFieldOwner.isShared() && !eiFieldOwner.isLockProtected(fi)) {
      logger.info("CG accessing shared instance field ", fi);
      return setNextChoiceGenerator( getRunnableCG("SHARED_OBJECT", ti));
    }
    
    return false;
  }
  
  @Override
  public boolean setsSharedClassCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    if (eiFieldOwner.isShared() && !eiFieldOwner.isLockProtected(fi)) {
      logger.info("CG accessing shared static field ", fi);
      return setNextChoiceGenerator( getRunnableCG("SHARED_CLASS", ti));
    }
    
    return false;
  }
  
  @Override
  public boolean setsSharedArrayCG (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int index){
    if (eiArray.isShared()){
      // <2do> we should check lock protection for the whole array here
      logger.info("CG accessing shared array ", eiArray);
      return setNextChoiceGenerator( getRunnableCG("SHARED_ARRAY", ti));
    }
    
    return false;
  }
  
  
  //--- internal policy methods that can be overridden by subclasses
    
  protected boolean isRelevantStaticFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo ei, FieldInfo fi){
    if (!ei.isShared()){
      return false;
    }
    
    if  (ei.isImmutable()){
      return false;
    }
    
    if (skipStaticFinals && fi.isFinal()){
      return false;
    }    
    
    if (!ti.hasOtherRunnables()){ // nothing to break for
      return false;
    }

    // call site. This could be transitive, in which case it has to be dynamic and can't be moved to isRelevant..()
    MethodInfo mi = insn.getMethodInfo();
    if (mi.isClinit() && (fi.getClassInfo() == mi.getClassInfo())) {
      // clinits are all synchronized, so they are lock protected per se
      return false;
    }
    
    return true;
  }

  
  protected boolean isRelevantArrayAccess (ThreadInfo ti, Instruction insn, ElementInfo ei, int index){
    // <2do> this is too simplistic, we should support filters for array objects
    
    if (!ti.hasOtherRunnables()){
      return false;
    }
    
    if (!ei.isShared()){
      return false;
    }
    
    if (ti.isFirstStepInsn()){ // we already did break
      return false;
    }

    return true;
  }
  
  //--- object exposure 

  // <2do> explain why not transitive
  
  @Override
  public boolean setsSharedObjectExposureCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, ElementInfo eiExposed){
    return setsExposureCG(ti,insn,eiFieldOwner,fi,eiExposed);
  }

  @Override
  public boolean setsSharedClassExposureCG (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, ElementInfo eiExposed){
    return setsExposureCG(ti,insn,eiFieldOwner,fi,eiExposed);
  }  

  // since exposure is about the object being exposed (the element), there is no separate setsSharedArrayExposureCG
  
  
  @Override
  public void cleanupThreadTermination(ThreadInfo ti) {
    // default action is to do nothing
  }

}
