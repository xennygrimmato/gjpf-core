package gov.nasa.jpf.vm;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StackFrameTest {


  public static class Dup2_x1Test {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private StackFrame _frame;

    @Before
    public void setup() {
      _frame = new StackFrameForTest(0, 10);
    }

    @Test
    @Ignore(value = "Github issue #11")
    public void shouldDuplicateTopOperandValueAndInsertTwoValuesDown() {
      givenStackWithTwoValues();

      _frame.dup2_x1();

      assertThatStackHaveThreeValues();
      assertThreeValuesOnStackInRightOrder();
    }

    @Test
    public void shouldDuplicateTwoTopOperandValuesAndInsertThreeValuesDown() {
      // 1 2 3  => 2 3.1 2 3
      givenStackWithThreeValues();

      _frame.dup2_x1();

      assertThatStackHaveFiveValues();
      assertFiveValuesOnStackInRightOrder();
    }

    @Test
    public void shouldDuplicateTwoTopOperandValuesWithAttributesAndInsertThreeValuesDown() {
      // 1 2 3  => 2 3.1 2 3
      givenStackWithThreeValuesAndAttributes();

      _frame.dup2_x1();

      assertThatStackHaveFiveValues();
      assertThatFiveValuesOnStackWithAttributesInRightOrder();
    }

    private void assertFiveValuesOnStackInRightOrder() {
      assertGivenStackValues(new Object[][]
          {// position | value
              { 4, 2 },
              { 3, 3 },
              { 2, 1 },
              { 1, 2 },
              { 0, 3 }
          });
    }

    private void assertGivenStackValues(Object[][] values) {
      for (Object[] stackValues : values) {
        int position = (int)stackValues[0];
        int value = (int)stackValues[1];
        assertThatValueAtPositionIsEqual(position, value);
      }
    }

    private void assertGivenStackValuesWithAttributes(Object[][] values) {
      for (Object[] stackValues : values) {
        int position = (int)stackValues[0];
        int value = (int)stackValues[1];
        String attribute = (String)stackValues[2];
        assertThatValueAtPositionHave(position, value, attribute);
      }
    }

    private void assertThatFiveValuesOnStackWithAttributesInRightOrder() {
      assertGivenStackValuesWithAttributes(new Object[][]
          {// position | value | attribute
              { 4, 2, "2" },
              { 3, 3, "3" },
              { 2, 1, "1" },
              { 1, 2, "2" },
              { 0, 3, "3" }
          }
      );
    }

    private void assertThatStackHaveFiveValues() {
      softly.assertThat(_frame.getTopPos()).isEqualTo(4);
    }

    private void assertThatStackHaveThreeValues() {
      softly.assertThat(_frame.getTopPos()).isEqualTo(2);
    }

    private void assertThatValueAtPositionHave(int position, int value, String attribute) {
      softly.assertThat(_frame.peek(position)).isEqualTo(value);
      softly.assertThat(_frame.getOperandAttr(position)).isEqualTo(attribute); // same const pool string
    }

    private void assertThatValueAtPositionIsEqual(int position, int value) {
      softly.assertThat(_frame.peek(position)).isEqualTo(value);
    }

    private void assertThreeValuesOnStackInRightOrder() {
      assertThatValueAtPositionIsEqual(2, 1);
      assertThatValueAtPositionIsEqual(1, 2);
      assertThatValueAtPositionIsEqual(0, 1);
    }

    private void givenStackWithThreeValues() {
      _frame.push(1);
      _frame.push(2);
      _frame.push(3);
    }

    private void givenStackWithThreeValuesAndAttributes() {
      _frame.push(1);
      _frame.setOperandAttr("1");
      _frame.push(2);
      _frame.setOperandAttr("2");
      _frame.push(3);
      _frame.setOperandAttr("3");
    }

    private void givenStackWithTwoValues() {
      _frame.push(1);
      _frame.push(2);
    }
  }

  public static class Dup2_x2Test {

    private StackFrame _frame;

    @Before
    public void setup() {
      _frame = new StackFrameForTest(0, 10);
    }

    @Test
    public void testDup2_x2() {
      // 1 2 3 4  => 3 4.1 2 3 4
      givenStackWithFourValues();

      _frame.dup2_x2();

      assert _frame.getTopPos() == 5;
      assert _frame.peek(5) == 3;
      assert _frame.peek(4) == 4;
      assert _frame.peek(3) == 1;
      assert _frame.peek(2) == 2;
      assert _frame.peek(1) == 3;
      assert _frame.peek(0) == 4;
    }

    @Test
    public void testDup2_x2_Attrs() {
      // 1 2 3 4  => 3 4.1 2 3 4
      givenStackWithFourValuesAndAttributes();

      _frame.dup2_x2();
      _frame.printOperands(System.out);

      assert _frame.getTopPos() == 5;
      assert _frame.peek(5) == 3 && _frame.getOperandAttr(5) == "3";  // same const pool string
      assert _frame.peek(4) == 4 && _frame.getOperandAttr(4) == "4";
      assert _frame.peek(3) == 1 && _frame.getOperandAttr(3) == "1";
      assert _frame.peek(2) == 2 && _frame.getOperandAttr(2) == "2";
      assert _frame.peek(1) == 3 && _frame.getOperandAttr(1) == "3";
      assert _frame.peek(0) == 4 && _frame.getOperandAttr(0) == "4";
    }

    private void givenStackWithFourValuesAndAttributes() {
      _frame.push(1);
      _frame.setOperandAttr("1");
      _frame.push(2);
      _frame.setOperandAttr("2");
      _frame.push(3);
      _frame.setOperandAttr("3");
      _frame.push(4);
      _frame.setOperandAttr("4");
    }

    private void givenStackWithFourValues() {
      _frame.push(1);
      _frame.push(2);
      _frame.push(3);
      _frame.push(4);
    }
  }

  public static class PushPopDoubleTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private final double _value = Math.PI;

    private StackFrame _frame = new StackFrameForTest(2, 10);

    @Test
    public void shouldGetLocalValueObjectAfterPushDouble() {
      givenStackWithThreeLocalValues();
      _frame.pushDouble(_value);

      Object obj_Double = _frame.getLocalValueObject(new LocalVarInfo("testDouble", "D", "D", 0, 0, _frame.getTopPos() - 1));

      softly.assertThat(obj_Double).isInstanceOf(Double.class);
      softly.assertThat((Double)obj_Double).isEqualTo(_value);
    }

    @Test
    public void shouldPushAndPopTheSameDoubleValue() {
      // Push/Pop double value and also  JVMStackFrame.getLocalValueObject
      givenStackWithThreeLocalValues();
      _frame.pushDouble(_value);

      final double result_popLong = _frame.popDouble();

      softly.assertThat(result_popLong).isEqualTo(_value);
      assertRestOfStackIsTheSame();
    }

    private void assertRestOfStackIsTheSame() {
      softly.assertThat(_frame.peek(0)).isEqualTo(3);
      softly.assertThat(_frame.peek(1)).isEqualTo(2);
      softly.assertThat(_frame.peek(2)).isEqualTo(1);
    }

    private void givenStackWithThreeLocalValues() {
      _frame.push(1);
      _frame.push(2);
      _frame.push(3);
    }
  }

  public static class PushPopLongTest {
    private final long _long = 0x123456780ABCDEFL;

    private StackFrame _frame = new StackFrameForTest(0, 2);

    @Test
    public void shouldGetLocalValueObjectAfterPushLong() {
      _frame.pushLong(_long);

      Object obj_Long = _frame.getLocalValueObject(new LocalVarInfo("testLong", "J", "J", 0, 0, 0));

      assertThat(obj_Long).isInstanceOf(Long.class);
      assertThat((Long)obj_Long).isEqualTo(_long);
    }

    @Test
    public void shouldPushAndPopTheSameLongValue() {
      _frame.pushLong(_long);

      long result_popLong = _frame.popLong();

      assertThat(result_popLong).isEqualTo(_long);
    }
  }

  static final class StackFrameForTest
      extends StackFrame {

    StackFrameForTest(final int i, final int i1) {
      super(i, i1);
    }

    @Override
    public void setArgumentLocal(final int idx, final int value, final Object attr) {

    }

    @Override
    public void setLongArgumentLocal(final int idx, final long value, final Object attr) {

    }

    @Override
    public void setReferenceArgumentLocal(final int idx, final int ref, final Object attr) {

    }
  }
}