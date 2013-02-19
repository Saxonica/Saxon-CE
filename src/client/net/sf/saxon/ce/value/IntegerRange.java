package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.expr.RangeIterator;
import client.net.sf.saxon.ce.om.GroundedValue;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;

/**
 * This class represents a sequence of consecutive ascending integers, for example 1 to 50.
 * The integers must be within the range of a Java int.
 */

public class IntegerRange extends Value implements GroundedValue {

    public int start;
    public int end;

    /**
     * Construct an integer range expression
     * @param start the first integer in the sequence (inclusive)
     * @param end the last integer in the sequence (inclusive). Must be >= start
     */

    public IntegerRange(int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end < start in IntegerRange");
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Get the first integer in the sequence (inclusive)
     * @return the first integer in the sequence (inclusive)
     */

    public long getStart() {
        return start;
    }

   /**
     * Get the last integer in the sequence (inclusive)
     * @return the last integer in the sequence (inclusive)
     */

    public long getEnd() {
        return end;
    }
    
    public String toString() {
    	return "(" + start + " to " + end + ")";
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate() throws XPathException {
        return new RangeIterator(start, end);
    }

    /**
     * Determine the data type of the items in the expression, if possible
     *
     * @return AnyItemType (not known)
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.INTEGER;
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     */

    public Item itemAt(int n) {
        if (n < 0 || n > (end-start)) {
            return null;
        }
        return IntegerValue.makeIntegerValue(start + n);
    }


    /**
     * Get a subsequence of the value
     *
     * @param start  the index of the first item to be included in the result, counting from zero.
     *               A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     *               sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     *               get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     *               is returned. If the value goes off the end of the sequence, the result returns items up to the end
     *               of the sequence
     * @return the required subsequence. 
     */

    public GroundedValue subsequence(int start, int length) {
        if (length <= 0) {
            return EmptySequence.getInstance();
        }
        int newStart = this.start + (start > 0 ? start : 0);
        int newEnd = newStart + length - 1;
        if (newEnd > end) {
            newEnd = end;
        }
        return new IntegerRange(newStart, newEnd);
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() throws XPathException {
        return end - start + 1;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
