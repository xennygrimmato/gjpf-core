/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package java8;

import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.Verify;
import org.junit.Test;

/**
 * regression test for Java 8 default methods
 */
public class DefaultMethodTest extends TestJPF {
  
  //------------------------------------------ non-ambiguous recursive lookup
  
  interface A1 {
    default int getValue(){
      return 42;
    }
  } 
  
  interface B1 extends A1 {
    // nothing
  }
  
  static class C1 implements A1 {
    // nothing
  }
  
  static class D1 extends C1 {
    // nothing
  } 
  
  @Test
  public void testSingleMethod (){
    if (verifyNoPropertyViolation()){
      D1 o = new D1();
      int result = o.getValue();
      System.out.println(result);
      assertTrue (result == 42);
    }
  }
  
  //------------------------------------------ ambiguity resolution
  
  interface B2 {
    default int getValue(){
      return 3;
    }
  }
  
  static class D2 implements A1, B2 {
    @Override
    public int getValue(){
      return A1.super.getValue() + B2.super.getValue();
    }
  }
  
  @Test
  public void testExplicitDelegation (){
    if (verifyNoPropertyViolation()){
      D2 o = new D2();
      int result = o.getValue();
      System.out.println(result);
      assertTrue (result == 45);
    }    
  }
  
  
  // <2do> how to test IncompatibleClassChangeError without explicit classfile restore?
}