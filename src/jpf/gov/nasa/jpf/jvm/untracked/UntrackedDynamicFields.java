//
// Copyright (C) 2008 United States Government as represented by the
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
package gov.nasa.jpf.jvm.untracked;

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicFields;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;

/**
 * DynamicFields that support the @UntrackedField annotation
 *
 * @author Milos Gligoric (milos.gligoric@gmail.com)
 * @author Tihomir Gvero (tihomir.gvero@gmail.com)
 */
public class UntrackedDynamicFields extends DynamicFields {

  //--- the (unfortunately redundant) UntrackedFields interface implementation (see comment there)
  protected int untracked; // counts the number of incoming references marked as untracked

  public void setUntracked (int untracked) {
    this.untracked = untracked;
  }
  public int getUntracked () {
    return untracked;
  }
  public boolean isUntracked () {
    return untracked > 0;
  }
  public void incUntracked () {
    untracked++;
  }
  public void decUntracked () {
    untracked--;
  }


  //--- own stuff
  public UntrackedDynamicFields (String t, ClassInfo ci) {
    super(t, ci);
  }

  private boolean isFieldUntracked (int storageOffset) {
    FieldInfo fi = findFieldInfo(storageOffset);
    if (fi != null) {
      return (fi.getAnnotation("gov.nasa.jpf.jvm.untracked.UntrackedField") != null);
    } else {
      return false;
    }
  }


  //--- overridden base methods
  public void setReferenceValue (ElementInfo ei, int index, int newValue) {

    // If the reference field of the current object is "untracked" or the
    // entire object has already been marked as "untracked", the traversal
    // for old and new value of the field should be started.
    UntrackedManager manager = null;
    if (UntrackedManager.getProperty() &&
        ((untracked > 0) || isFieldUntracked(index))) {
      manager = UntrackedManager.getInstance();
      manager.oldObjectsTraversal(values[index]);
    }

    values[index] = newValue;

    if (manager != null) {
      manager.newObjectsTraversal(newValue);
    }
  }

}
