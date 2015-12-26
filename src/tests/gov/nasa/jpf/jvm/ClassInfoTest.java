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

package gov.nasa.jpf.jvm;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassParseException;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.MethodInfo;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

/**
 * unit test for ClassInfo initialization
 */
public class ClassInfoTest {

  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

  private final String CLASS_NAME = "gov.nasa.jpf.jvm.ClassInfoTest$MyClass";

  @Test
  public void shouldInitializeExampleClassProperly()
      throws ClassParseException {
    File exampleClass = givenExampleClass();

    ClassInfo classInfo = whenInitializeClass(exampleClass);

    softly.assertThat(classInfo.getName()).isEqualTo(CLASS_NAME);
    assertDeclaredInstanceFields(classInfo);
    assertDeclaredStaticFields(classInfo);
    assertDelcaredMethods(classInfo);
  }

  private void assertDelcaredMethods(final ClassInfo classInfo) {
    softly.assertThat(classInfo.getDeclaredMethodInfos()).extracting("uniqueName").containsExactly(
        "<init>(Ljava/lang/String;)V",
        "whatIsIt()I",
        "isItTheAnswer(ZILjava/lang/String;)Z",
        "foo()V",
        "getString()Ljava/lang/String;"
    );
  }

  private void assertDeclaredStaticFields(final ClassInfo classInfo) {
    softly.assertThat(classInfo.getDeclaredStaticFields()).extracting("type").containsExactly("int");
    softly.assertThat(classInfo.getDeclaredStaticFields()).extracting("name").containsExactly("D");
  }

  private void assertDeclaredInstanceFields(final ClassInfo classInfo) {
    softly.assertThat(classInfo.getDeclaredInstanceFields()).extracting("type").containsExactly("java.lang.String");
    softly.assertThat(classInfo.getDeclaredInstanceFields()).extracting("name").containsExactly("s");
  }

  private File givenExampleClass()
      throws ClassParseException {
    return new File("build/tests/gov/nasa/jpf/jvm/ClassInfoTest$MyClass.class");
  }

  private ClassInfo whenInitializeClass(File file)
      throws ClassParseException {
    return new NonResolvedClassInfo("gov.nasa.jpf.jvm.ClassInfoTest$MyClass", file);
  }

  @interface X {
    String value() default "nothing";
  }

  @interface Y {
    int[] value();
  }

  @X
  public static class MyClass
      implements Cloneable {
    public static final int D = 42;

    @X("data")
    String s;

    public MyClass(String s) {
      this.s = s;
      foo();
    }

    public static int whatIsIt() {
      int d = D;
      switch (d) {
        case 41:
          d = -1;
          break;
        case 42:
          d = 0;
          break;
        case 43:
          d = 1;
          break;
        default:
          d = 2;
          break;
      }
      return d;
    }

    public boolean isItTheAnswer(boolean b, @X @Y({ 1, 2, 3 }) int d, String s) {
      switch (d) {
        case 42:
          return true;
        default:
          return false;
      }
    }

    protected void foo()
        throws IndexOutOfBoundsException {
      @X int d = D;

      Object[] a = new Object[2];
      String s = "blah";
      a[0] = s;

      String x = (String)a[0];
      Object o = a;
      if (o instanceof Object[]) {
        o = x;
      }
      if (o instanceof String) {
        o = null;
      }

      Object[][] aa = new Object[2][2];

      try {
        char c = s.charAt(d);
      } catch (IndexOutOfBoundsException ioobx) {
        System.out.println("too big");
        throw ioobx;
      }
    }

    @X
    String getString() {
      return s;
    }
  }

}
