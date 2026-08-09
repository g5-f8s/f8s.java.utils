package org.g5.util;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Source code licensed under the GNU GPL v3.0 or later. *
 */
class CircularBufferTest {

  @Test
  void ensureCircularBehavior() {
    CircularBuffer<String> boundedCircularArray = new CircularBuffer<>(5);
    assertThat(boundedCircularArray.size()).isEqualTo(0);;
    boundedCircularArray.add("1");
    assertThat(boundedCircularArray.size()).isEqualTo(1);;
    assertThat(boundedCircularArray.get(0)).isEqualTo("1");;
    assertThat(boundedCircularArray).contains("1");
    boundedCircularArray.add("2");
    assertThat(boundedCircularArray.size()).isEqualTo(2);;
    assertThat(boundedCircularArray.get(1)).isEqualTo("2");;
    assertThat(boundedCircularArray).contains("1", "2");
    boundedCircularArray.add("3");
    assertThat(boundedCircularArray.size()).isEqualTo(3);;
    assertThat(boundedCircularArray.get(2)).isEqualTo("3");;
    assertThat(boundedCircularArray).contains("1", "2", "3");
    boundedCircularArray.add("4");
    assertThat(boundedCircularArray.size()).isEqualTo(4);;
    assertThat(boundedCircularArray.get(3)).isEqualTo("4");;
    assertThat(boundedCircularArray).contains("1", "2", "3", "4");
    boundedCircularArray.add("5");
    assertThat(boundedCircularArray.size()).isEqualTo(5);;
    assertThat(boundedCircularArray.get(4)).isEqualTo("5");;
    assertThat(boundedCircularArray).contains("1", "2", "3", "4", "5");
    boundedCircularArray.add("6");
    assertThat(boundedCircularArray.size()).isEqualTo(5);;
    assertThat(boundedCircularArray.getFirst()).isEqualTo("6");;
    assertThat(boundedCircularArray).contains("2", "3", "4", "5", "6");
    boundedCircularArray.add("7");
    assertThat(boundedCircularArray.size()).isEqualTo(5);;
    assertThat(boundedCircularArray.getFirst()).isEqualTo("6");;
    assertThat(boundedCircularArray.get(1)).isEqualTo("7");;
    assertThat(boundedCircularArray.getLast()).isEqualTo("7");;
    assertThat(boundedCircularArray).contains("3", "4", "5", "6", "7");
  }

  @Test
  void ensureInvalidIndicesAreHandledCorrectly() {
    CircularBuffer<String> boundedCircularArray = new CircularBuffer<>(1);
    assertThat(boundedCircularArray.size()).isEqualTo(0);;
    boundedCircularArray.add("1");
    assertThat(boundedCircularArray.size()).isEqualTo(1);;
    assertThat(boundedCircularArray.get(0)).isEqualTo("1");;
    IndexOutOfBoundsException indexOutOfBoundsException = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> boundedCircularArray.get(1));
  }

  @Test
  void checkIteratorFailureConditions() {
    CircularBuffer<String> boundedCircularArray = new CircularBuffer<>(5);
    try {
      boundedCircularArray.iterator().next();
      assert false : "Should throw an exception!";
    } catch (NoSuchElementException e) {
      //expected
    }
    boundedCircularArray.add("1");
    Iterator<String> iterator = boundedCircularArray.iterator();
    iterator.next();
    try {
      iterator.next();
      assert false : "Should throw an exception!";
    } catch (NoSuchElementException e) {
      //expected
    }
    iterator = boundedCircularArray.iterator();
    assertThat(iterator.next()).isEqualTo("1");
    boundedCircularArray.add("2");
    try {
      iterator.next();
      assert false : "Should throw an exception!";
    } catch (ConcurrentModificationException e) {
      //expected
    }
  }

  @Test
  public void ensureLastItemCorrectlyReturned() {
    CircularBuffer<String> boundedCircularArray = new CircularBuffer<>(3);
    assertThat(boundedCircularArray.size()).isEqualTo(0);
    assertThat(boundedCircularArray.getLastInsertIndex()).isEqualTo(-1);;
    try {
      boundedCircularArray.getLast();
      assert false : "Should have thrown an exception!";
    } catch (IndexOutOfBoundsException e) {
      //expected
    }
    boundedCircularArray.add("1");
    assertThat(boundedCircularArray.size()).isEqualTo(1);
    assertThat(boundedCircularArray.get(0)).isEqualTo("1");
    assertThat(boundedCircularArray.getLast()).isEqualTo("1");
    boundedCircularArray.add("2");
    assertThat(boundedCircularArray.size()).isEqualTo(2);
    assertThat(boundedCircularArray).contains("1", "2");
    assertThat(boundedCircularArray.getLast()).isEqualTo("2");
    boundedCircularArray.add("3");
    assertThat(boundedCircularArray.size()).isEqualTo(3);
    assertThat(boundedCircularArray.getLast()).isEqualTo("3");
    assertThat(boundedCircularArray).contains("1", "2", "3");
    boundedCircularArray.add("4");
    assertThat(boundedCircularArray.size()).isEqualTo(3);
    assertThat(boundedCircularArray).contains("2", "3", "4");
    assertThat(boundedCircularArray.getLast()).isEqualTo("4");
    boundedCircularArray.add("5");
    assertThat(boundedCircularArray.size()).isEqualTo(3);
    assertThat(boundedCircularArray).contains("3", "4", "5");
    assertThat(boundedCircularArray.getLast()).isEqualTo("5");
    boundedCircularArray.add("6");
    assertThat(boundedCircularArray.size()).isEqualTo(3);
    assertThat(boundedCircularArray).contains("4", "5", "6");
    assertThat(boundedCircularArray.getLast()).isEqualTo("6");
  }

  @Test
  public void ensureLifoIterationOrder() {
    CircularBuffer<String> boundedCircularArray =
        new CircularBuffer<>(3);
    boundedCircularArray.add("1");
    boundedCircularArray.add("2");
    boundedCircularArray.add("3");
    assertThat(boundedCircularArray.size()).isEqualTo(3);
    assertThat(boundedCircularArray).contains("1", "2", "3");
    boundedCircularArray.add("4");
    boundedCircularArray.add("5");
    //default iterator should be FIFO ordered
    assertThat(boundedCircularArray).contains("3", "4", "5");
    final List<String> expectedList = Arrays.asList("4", "5", "3");
    //normal order of iteration should be a[0], a[1]...a[n]
    System.out.println("Natural ordering:");
    boundedCircularArray.forEach(System.out::println);
    System.out.println("Internal ordering:");
    boundedCircularArray.indexSequenceIterator().forEachRemaining(System.out::println);
    Iterator<String> iterator = boundedCircularArray.indexSequenceIterator();
    int i = 0;
    while (iterator.hasNext()) {
      String next = iterator.next();
      assertThat(next).isEqualTo(expectedList.get(i));
      i++;
      assertThat(i).isLessThanOrEqualTo(expectedList.size());
    }
  }

}
