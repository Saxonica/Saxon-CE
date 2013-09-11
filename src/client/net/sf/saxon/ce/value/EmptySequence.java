package client.net.sf.saxon.ce.value;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.om.GroundedValue;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.type.ItemType;

/**
* An EmptySequence object represents a sequence containing no members.
*/


public final class EmptySequence extends Value implements GroundedValue {

    // This class has a single instance
    private static EmptySequence THE_INSTANCE = new EmptySequence();


    /**
    * Private constructor: only the predefined instances of this class can be used
    */

    private EmptySequence() {}

    /**
    * Get the implicit instance of this class
    */

    public static EmptySequence getInstance() {
        return THE_INSTANCE;
    }

    /**
    * Return an iteration over the sequence
    */

    public SequenceIterator iterate() {
        return EmptyIterator.getInstance();
    }

    /**
     * Return the value in the form of an Item
     * @return the value in the form of an Item
     */

    public Item asItem() {
        return null;
    }
    
    public String toString() {
    	return "()";
    }

    /**
     * Determine the item type
     */

    public ItemType getItemType() {
        return EmptySequenceTest.getInstance();
    }

    /**
     * Get the length of the sequence
     * @return always 0 for an empty sequence
     */

    public final int getLength() {
        return 0;
    }
    /**
    * Is this expression the same as another expression?
    * @throws ClassCastException if the values are not comparable
    */

    public boolean equals(Object other) {
        if (!(other instanceof EmptySequence)) {
            throw new ClassCastException("Cannot compare " + other.getClass() + " to empty sequence");
        }
        return true;
    }

    public int hashCode() {
        return 42;
    }

    /**
    * Get the effective boolean value - always false
    */

    public boolean effectiveBooleanValue() {
        return false;
    }


    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     *
     * @param n position of the required item, counting from zero.
     * @return the n'th item in the sequence, where the first item in the sequence is
     *         numbered zero. If n is negative or >= the length of the sequence, returns null.
     */

    public Item itemAt(int n) {
        return null;
    }

    /**
     * Get a subsequence of the value
     *
     * @param min    the index of the first item to be included in the result, counting from zero.
     *               A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     *               sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     *               get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     *               is returned. If the value goes off the end of the sequence, the result returns items up to the end
     *               of the sequence
     * @return the required subsequence. If min is
     */

    public GroundedValue subsequence(int min, int length) {
        return this;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
