package client.net.sf.saxon.ce.tree.iter;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;

/**
 * A general-purpose iterator built over a function that steps from one item in a sequence to the next
 */

public class SteppingIterator implements UnfailingIterator {

    private Item origin;
    private boolean includeSelf;
    private Item next;
	private SteppingFunction function;

	/**
	 * Create an axis iterator, given an origin node and a means of getting from one node to the next
     * @param origin the node from which the axis originates
     * @param function methods to step to the next item, and to test if the next item is
     * to be included in the sequence
     * @param includeSelf true of the origin node is a candidate for inclusion in the result
	*/

	public SteppingIterator(Item origin, SteppingFunction function, boolean includeSelf) {
	    this.origin = origin;
        this.includeSelf = includeSelf;
	    this.function = function;
        this.next = origin;
        if (!includeSelf || !function.conforms(origin)) {
            advance();
        }
	}

	/**
	* Advance along the axis until a node is found that matches the required criteria
	*/

	protected final void advance() {
	    do {
	        next = function.step(next);
	    } while (next != null && !function.conforms(next));
	}

    /**
	* Return the next node in the iteration
	*/

	public final Item next() {
        if (next==null) {
            return null;
        } else {
            Item curr = next;
            advance();
            return curr;
        }
	}

    /**
     * Get another iterator over the same sequence of items, positioned at the
     * start of the sequence. It must be possible to call this method at any time, whether
     * none, some, or all of the items in the original iterator have been read. The method
     * is non-destructive: it does not change the state of the original iterator.
     * @return a new iterator over the same sequence
     */

    public SequenceIterator getAnother() {
        return new SteppingIterator(origin, function, includeSelf);
    }

    /**
     * Interface defining the function that steps from one item to the next.
     * Note that this function must not be stateful, and it must not throw
     * any errors.
     */

    public interface SteppingFunction {
        /**
         * Step from one item to the next
         * @param current the current item
         * @return the next item, or null if there are no more items in the sequence
         */
        public Item step(Item current);

        /**
         * Ask whether an item is to be included in the sequence, or skipped
         * @param current the item to be tested
         * @return true if the item is to be included in the sequence, false if it is to be skipped
         */
        public boolean conforms(Item current);
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
