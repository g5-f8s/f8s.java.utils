package org.g5.util;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.g5.util.CircularBuffer;
import org.junit.Test;

/**
 * 
 * Source code licensed under the GNU GPL v3.0 or later. *
 */
public class CircularBufferTest {

    @Test
    public void ensureCircularBehavior() {
        CircularBuffer<String> boundedCircularArray = new CircularBuffer<String>(5);
        assertThat(boundedCircularArray.size(), is(equalTo(0)));
        boundedCircularArray.add("1");
        assertThat(boundedCircularArray.size(), is(equalTo(1)));
        assertThat(boundedCircularArray.get(0), is(equalTo("1")));
        assertThat(boundedCircularArray, contains("1"));
        boundedCircularArray.add("2");
        assertThat(boundedCircularArray.size(), is(equalTo(2)));
        assertThat(boundedCircularArray.get(1), is(equalTo("2")));
        assertThat(boundedCircularArray, contains("1", "2"));
        boundedCircularArray.add("3");
        assertThat(boundedCircularArray.size(), is(equalTo(3)));
        assertThat(boundedCircularArray.get(2), is(equalTo("3")));
        assertThat(boundedCircularArray, contains("1", "2", "3"));
        boundedCircularArray.add("4");
        assertThat(boundedCircularArray.size(), is(equalTo(4)));
        assertThat(boundedCircularArray.get(3), is(equalTo("4")));
        assertThat(boundedCircularArray, contains("1", "2", "3", "4"));
        boundedCircularArray.add("5");
        assertThat(boundedCircularArray.size(), is(equalTo(5)));
        assertThat(boundedCircularArray.get(4), is(equalTo("5")));
        assertThat(boundedCircularArray, contains("1", "2", "3", "4", "5"));
        boundedCircularArray.add("6");
        assertThat(boundedCircularArray.size(), is(equalTo(5)));
        assertThat(boundedCircularArray.get(0), is(equalTo("6")));
        assertThat(boundedCircularArray, contains("2", "3", "4", "5", "6"));
        boundedCircularArray.add("7");
        assertThat(boundedCircularArray.size(), is(equalTo(5)));
        assertThat(boundedCircularArray.get(0), is(equalTo("6")));
        assertThat(boundedCircularArray.get(1), is(equalTo("7")));
        assertThat(boundedCircularArray.getLast(), is(equalTo("7")));
        assertThat(boundedCircularArray, contains("3", "4", "5", "6", "7"));
    }
    
    @Test(expected=IndexOutOfBoundsException.class)
    public void ensureInvalidIndicesAreHandledCorrectly() {
        CircularBuffer<String> boundedCircularArray = new CircularBuffer<String>(1);
        assertThat(boundedCircularArray.size(), is(equalTo(0)));
        boundedCircularArray.add("1");
        assertThat(boundedCircularArray.size(), is(equalTo(1)));
        assertThat(boundedCircularArray.get(0), is(equalTo("1")));
        boundedCircularArray.get(1);
    }
    
    @Test
    public void checkIteratorFailureConditions() {
        CircularBuffer<String> boundedCircularArray = new CircularBuffer<String>(5);
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
        assertThat(iterator.next(), is("1"));
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
        CircularBuffer<String> boundedCircularArray = new CircularBuffer<String>(3);
        assertThat(boundedCircularArray.size(), is(0));
        assertThat(boundedCircularArray.getLastInsertIndex(), is(equalTo(-1)));
        try {
            boundedCircularArray.getLast();
            assert false: "Should have thrown an exception!";
        } catch (IndexOutOfBoundsException e) {
            //expected
        }
        boundedCircularArray.add("1");
        assertThat(boundedCircularArray.size(), is(1));
        assertThat(boundedCircularArray.get(0), is("1"));
        assertThat(boundedCircularArray.getLast(), is("1"));
        boundedCircularArray.add("2");
        assertThat(boundedCircularArray.size(), is(2));
        assertThat(boundedCircularArray, contains("1", "2"));
        assertThat(boundedCircularArray.getLast(), is("2"));
        boundedCircularArray.add("3");
        assertThat(boundedCircularArray.size(), is(3));
        assertThat(boundedCircularArray.getLast(), is("3"));
        assertThat(boundedCircularArray, contains("1", "2", "3"));
        boundedCircularArray.add("4");
        assertThat(boundedCircularArray.size(), is(3));
        assertThat(boundedCircularArray, contains("2", "3", "4"));
        assertThat(boundedCircularArray.getLast(), is("4"));
        boundedCircularArray.add("5");
        assertThat(boundedCircularArray.size(), is(3));
        assertThat(boundedCircularArray, contains("3", "4", "5"));
        assertThat(boundedCircularArray.getLast(), is("5"));
        boundedCircularArray.add("6");
        assertThat(boundedCircularArray.size(), is(3));
        assertThat(boundedCircularArray, contains("4", "5", "6"));
        assertThat(boundedCircularArray.getLast(), is("6"));
    }
    
    @Test
    public void ensureLifoIterationOrder() {
        CircularBuffer<String> boundedCircularArray =
            new CircularBuffer<String>(3);
        boundedCircularArray.add("1");
        boundedCircularArray.add("2");
        boundedCircularArray.add("3");
        assertThat(boundedCircularArray.size(), is(3));
        assertThat(boundedCircularArray, contains("1", "2", "3"));
        boundedCircularArray.add("4");
        boundedCircularArray.add("5");
        //default iterator should be FIFO ordered
        assertThat(boundedCircularArray, contains("3", "4", "5"));
        final List<String> expectedList = Arrays.asList("4", "5", "3");
        //normal order of iteration should be a[0], a[1]...a[n]
        System.out.println("Natural ordering:");
        boundedCircularArray.forEach(it -> System.out.println(it));
        System.out.println("Internal ordering:");
        boundedCircularArray.indexSequenceIterator().forEachRemaining(it -> System.out.println(it));
        Iterator<String> iterator = boundedCircularArray.indexSequenceIterator();
        int i = 0;
        while (iterator.hasNext()) {
        	String next = iterator.next();
            assertThat("Expected item["+i+"]: "+expectedList.get(i)+", but was: "+next,
                next, is(expectedList.get(i)));
            i++;
            assertThat(i, lessThanOrEqualTo(expectedList.size()));
        }
    }

}
