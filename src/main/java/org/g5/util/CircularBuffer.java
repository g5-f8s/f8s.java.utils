package org.g5.util;

import java.util.AbstractList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 
 * I am a simple, bounded circular buffer. When my upper-bound is reached,
 * I will insert the next element at my first position.<p/>
 * My default {@link Iterator iterator} is a {@link FifoIterator FIFO-iterator}
 * and will return items in FIFO order.<p/>
 * I am <b><i>not</i></b> thread-safe.
 * 
 * Source code licensed under the GNU GPL v3.0 or later.
 * 
 * @author gerard.fernandes@gmail.com
 *
 * @param <E>
 */
public class CircularBuffer<E> extends AbstractList<E> {

    private final int capacity;
    private int size = 0;
    private int lastInsertIndex = -1;
    private int nextInsertIndex = 0;
    private final E[] data;
    
    @SuppressWarnings("unchecked")
    public CircularBuffer(int capacity) {
        this.capacity = capacity;
        this.data = (E[]) new Object[this.capacity];
    }
    
    public boolean add(E data) {
        this.data[nextInsertIndex] = data;
        this.lastInsertIndex = this.nextInsertIndex;
        this.nextInsertIndex = this.nextInsertIndex < (this.capacity-1)?
                                this.nextInsertIndex+1:0;
        this.size = this.size < this.capacity? this.size + 1 : this.capacity;
        return true;
    }
    
    /**
     * @return the index at which an element was last inserted.
     */
    protected int getLastInsertIndex() {
        return this.lastInsertIndex;
    }
    
    /**
     * @return the last element inserted into this list.
     */
    public E getLast() {
        return get(getLastInsertIndex());
    }
    
    public boolean isFull() {
        return this.size == this.capacity;
    }
    
    private void checkBounds(int index) {
        if(index >=0 && index < capacity) {
            return;
        }
        throw new IndexOutOfBoundsException("Invalid index "+index+" - "+getClass().getSimpleName()+"[size: "+size+", capacity: "+capacity+"]");
    }
    
    @Override
    public Iterator<E> iterator() {
        return new FifoIterator();
    }
    
    public Iterator<E> indexSequenceIterator() {
        return super.iterator();
    }
    
    @Override
    public E get(int index) {
        checkBounds(index);
        return this.data[index];
    }

    @Override
    public int size() {
        return size;
    }
    
    /**
     * I am a FIFO order iterator over a {@link CircularBuffer}.
     * 
     * @author gerard.fernandes@gmail.com
     *
     */
    private class FifoIterator implements Iterator<E> {

        private final int expectedSize = size;
        private final int markerIndex;
        private int cursor;
        boolean completed = isEmpty();
        
        private FifoIterator() {
            if (isEmpty()) {
                this.markerIndex = this.cursor = 0;
            } else {
                int lastIndex = getLastInsertIndex();
                this.markerIndex =  (lastIndex < (capacity - 1))? lastIndex + 1: 0;
                this.cursor = (size < capacity)? 0: this.markerIndex;
            }
        }
        
        @Override
        public boolean hasNext() {
            return !this.completed;
        }

        @Override
        public E next() {
            checkForCoModification();
            if (hasNext()) {
                E next = data[this.cursor];
                this.cursor = (this.cursor < (capacity - 1))? this.cursor + 1 : 0;
                if (this.cursor == this.markerIndex) {
                    this.completed = true;
                }
                return next;
            }
            throw new NoSuchElementException();
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException("This is a read-only iterator! Can not remove items!");
        }
        
        private void checkForCoModification() {
            if (size != expectedSize) {
                throw new ConcurrentModificationException();
            }
        }
        
    }
    
}

