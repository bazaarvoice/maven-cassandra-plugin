package com.bazaarvoice.cca.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An Iterator that pages a variable-length buffer of objects at a time.
 */
public abstract class BufferedIterator<T> implements Iterator<T> {

    /** The total number batches (pages) to use. */
    private int _numBatches;

    /** The next batch (page) number to request. */
    private int _nextBatch;

    /** The objects that have already been loaded in the current batch (page). */
    private List<T> _buffer;

    /** The index of the next Object in _buffer to return. */
    private int _nextObjectIndex;

    /**
     * Determines the total number of batches (pages) that should used.
     * @return the total number of batchs, >= 1.
     */
    protected abstract int calculateBatchCount();

    /**
     * Loads the next batch of data.
     * @param zeroBasedBatchIndex the index of the batch (page) to be loaded.
     * @param numBatches the total number of batches (pages) that will be loaded.
     * @param buffer the collection that the batch should be added to
     */
    protected abstract void loadBatch(int zeroBasedBatchIndex, int numBatches, List<T> buffer);

    public boolean hasNext() {
        if(_buffer == null) {
            // first time through.  initialize the generator
            _numBatches = calculateBatchCount();
            _buffer = new ArrayList<T>();
        }
        // This needs to be a while loop because loadBatch is not guaranteed
        // to return a non-empty batch.
        while(_nextObjectIndex >= _buffer.size() && _nextBatch < _numBatches) {
            _buffer.clear();
            _nextObjectIndex = 0;
            // We cleanup after RuntimeExceptions here, since exceptions thrown
            // by subclass' loadBatch() will propagate, but could also leave
            // our buffer in an inconsistent state.
            try {
                loadBatch(_nextBatch, _numBatches, _buffer);
                _nextBatch++;
            }
            catch( RuntimeException re) {
                _buffer.clear();
                throw re;
            }
        }
        return _nextObjectIndex < _buffer.size();
    }

    public T next() {
        if(!hasNext()) {
            throw new NoSuchElementException();
        }
        return _buffer.get(_nextObjectIndex++);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    protected int calculateBatchCount(int estimatedNumItems, int batchSize) {
        return (estimatedNumItems + batchSize - 1) / batchSize;
    }
}
