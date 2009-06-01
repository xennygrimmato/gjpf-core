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
package gov.nasa.jpf.mc;

import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;
import org.junit.runner.JUnitCore;

public class TestAnnotationJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.mc.TestAnnotation";

  public static void main (String[] args) {
    JUnitCore.main("gov.nasa.jpf.mc.TestAnnotationJPF");
  }


  /**************************** tests **********************************/

  @Test
  public void testLongElementAnnotation () {
    String[] args = { TEST_CLASS, "testLongElementAnnotation" };
    noPropertyViolation(args);
  }
}
