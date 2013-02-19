package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.SequenceOutputter;
import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.event.TeeOutputter;
import client.net.sf.saxon.ce.expr.LastPositionFinder;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.tree.iter.ArrayIterator;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.GroundedIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.trans.XPathException;

import java.util.List;

/**
 * A MemoClosure represents a value that has not yet been evaluated: the value is represented
 * by an expression, together with saved values of all the context variables that the
 * expression depends on.
 * <p/>
 * <p>The MemoClosure is designed for use when the value is only read several times. The
 * value is saved on the first evaluation and remembered for later use.</p>
 * <p/>
 * <p>The MemoClosure maintains a reservoir containing those items in the value that have
 * already been read. When a new iterator is requested to read the value, this iterator
 * first examines and returns any items already placed in the reservoir by previous
 * users of the MemoClosure. When the reservoir is exhausted, it then uses an underlying
 * Input Iterator to read further values of the underlying expression. If the value is
 * not read to completion (for example, if the first user did exists($expr), then the
 * Input Iterator is left positioned where this user abandoned it. The next user will read
 * any values left in the reservoir by the first user, and then pick up iterating the
 * base expression where the first user left off. Eventually, all the values of the
 * expression will find their way into the reservoir, and future users simply iterate
 * over the reservoir contents. Alternatively, of course, the values may be left unread.</p>
 * <p/>
 * <p>Delayed evaluation is used only for expressions with a static type that allows
 * more than one item, so the evaluateItem() method will not normally be used, but it is
 * supported for completeness.</p>
 * <p/>
 * <p>The expression may depend on local variables and on the context item; these values
 * are held in the saved XPathContext object that is kept as part of the Closure, and they
 * will always be read from that object. The expression may also depend on global variables;
 * these are unchanging, so they can be read from the Bindery in the normal way. Expressions
 * that depend on other contextual information, for example the values of position(), last(),
 * current(), current-group(), should not be evaluated using this mechanism: they should
 * always be evaluated eagerly. This means that the Closure does not need to keep a copy
 * of these context variables.</p>
 * <p/>
 * <p>In Saxon-EE, a for-each loop can be multithreaded. If a variable declared outside
 * the loop is evaluated as a MemoClosure, then a reference to the variable within the
 * loop can result in concurrent attempts to evaluate the variable incrementally. This
 * is prevented by synchronizing the evaluation methods.</p>
 */

public class MemoClosure extends Closure {

    transient private Item[] reservoir = null;
    private int used;
    protected int state;

    // State in which no items have yet been read
    private static final int UNREAD = 0;

    // State in which zero or more items are in the reservoir and it is not known
    // whether more items exist
    private static final int MAYBE_MORE = 1;

    // State in which all the items are in the reservoir
    private static final int ALL_READ = 3;

    // State in which we are getting the base iterator. If the closure is called in this state,
    // it indicates a recursive entry, which is only possible on an error path
    private static final int BUSY = 4;

    // State in which we know that the value is an empty sequence
    protected static final int EMPTY = 5;

    /**
     * Constructor should not be called directly, instances should be made using the make() method.
     */

    public MemoClosure() {
        //System.err.println("Creating MemoClosure");
    }

    /**
     * Evaluate the expression in a given context to return an iterator over a sequence
     *
     */

    public synchronized SequenceIterator iterate() throws XPathException {

        switch (state) {
        case UNREAD:
            state = BUSY;
            inputIterator = expression.iterate(savedXPathContext);
            if (inputIterator instanceof EmptyIterator) {
                state = EMPTY;
                return inputIterator;
            }
            // TODO: following optimization looks OK, but it throws func20 into an infinite loop
//                if (inputIterator instanceof GroundedIterator) {
//                    state = UNREAD;
//                    return inputIterator.getAnother();
//                }
            reservoir = new Item[50];
            used = 0;
            state = MAYBE_MORE;
            return new ProgressiveIterator();

        case MAYBE_MORE:
            return new ProgressiveIterator();

        case ALL_READ:
            switch (used) {
            case 0:
                state = EMPTY;
                return EmptyIterator.getInstance();
            case 1:
                return SingletonIterator.makeIterator(reservoir[0]);
            default:
                return new ArrayIterator(reservoir, 0, used);
            }

        case BUSY:
            // recursive entry: can happen if there is a circularity involving variable and function definitions
            // Can also happen if variable evaluation is attempted in a debugger, hence the cautious message
            XPathException de = new XPathException("Attempt to access a variable while it is being evaluated");
            de.setErrorCode("XTDE0640");
            //de.setXPathContext(context);
            throw de;

        case EMPTY:
            return EmptyIterator.getInstance();

        default:
            throw new IllegalStateException("Unknown iterator state");

        }
    }

    /**
     * Process the expression by writing the value to the current Receiver
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public synchronized void process(XPathContext context) throws XPathException {
        // To evaluate the closure in push mode, we need to use the original context of the
        // expression for everything except the current output destination, which is taken from the
        // context supplied at evaluation time
        if (state == EMPTY) {
            return;     // we know there is nothing to do
        } else if (state == BUSY) {
            // recursive entry: can happen if there is a circularity involving variable and function definitions
            XPathException de = new XPathException("Attempt to access a variable while it is being evaluated");
            de.setErrorCode("XTDE0640");
            de.setXPathContext(context);
            throw de;
        }
        if (reservoir != null) {
            SequenceIterator iter = iterate();
            SequenceReceiver out = context.getReceiver();
            while (true) {
                Item it = iter.next();
                if (it==null) break;
                out.append(it, NodeInfo.ALL_NAMESPACES);
            }
        } else {
            state = BUSY;
            Controller controller = context.getController();
            XPathContext c2 = savedXPathContext.newMinorContext();
            //c2.setOrigin(this);
            // Fork the output: one copy goes to a SequenceOutputter which remembers the contents for
            // use next time the variable is referenced; another copy goes to the current output destination.
            SequenceOutputter seq = controller.allocateSequenceOutputter(20);
            seq.setPipelineConfiguration(controller.makePipelineConfiguration());
            seq.open();
            TeeOutputter tee = new TeeOutputter(context.getReceiver(), seq);
            tee.setPipelineConfiguration(controller.makePipelineConfiguration());
            c2.setTemporaryReceiver(tee);

            expression.process(c2);

            seq.close();
            List list = seq.getList();
            if (list.isEmpty()) {
                state = EMPTY;
            } else {
                reservoir = new Item[list.size()];
                reservoir = (Item[])list.toArray(reservoir);
                used = list.size();
                state = ALL_READ;
            }
            // give unwanted stuff to the garbage collector
            savedXPathContext = null;
            seq.reset();
        }

    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
     */

    public synchronized Item itemAt(int n) throws XPathException {
        if (n < 0) {
            return null;
        }
        if (reservoir != null && n < used) {
            return reservoir[n];
        }
        if (state == ALL_READ || state == EMPTY) {
            return null;
        }
        if (state == UNREAD) {
            return super.itemAt(n);
            // this will read from the start of the sequence
        }
        // We have read some items from the input sequence but not enough. Read as many more as are needed.
        int diff = n - used + 1;
        while (diff-- > 0) {
            Item i = inputIterator.next();
            if (i == null) {
                state = ALL_READ;
                condense();
                return itemAt(n);
            }
            append(i);
            state = MAYBE_MORE;
        }
        //noinspection ConstantConditions
        return reservoir[n];
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() throws XPathException {
        if (state == ALL_READ) {
            return used;
        } else if (state == EMPTY) {
            return 0;
        } else {
            return super.getLength();
        }
    }

    /**
     * Append an item to the reservoir
     * @param item the item to be added
     */

    private void append(Item item) {
        if (used >= reservoir.length) {
            Item[] r2 = new Item[used*2];
            System.arraycopy(reservoir, 0, r2, 0, used);
            reservoir = r2;
        }
        reservoir[used++] = item;
    }

    /**
     * Release unused space in the reservoir (provided the amount of unused space is worth reclaiming)
     */

    private void condense() {
        if (reservoir.length - used > 30) {
            Item[] r2 = new Item[used];
            System.arraycopy(reservoir, 0, r2, 0, used);
            reservoir = r2;
        }
        // give unwanted stuff to the garbage collector
        savedXPathContext = null;
//        inputIterator = null;
//        expression = null;
    }

    /**
     * Determine whether the contents of the MemoClosure have been fully read
     * @return true if the contents have been fully read
     */

    public boolean isFullyRead() {
        return state==EMPTY || state==ALL_READ;
    }

    /**
     * Return a value containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding value
     */

    public Value materialize() throws XPathException {
        if (state == ALL_READ) {
            return new SequenceExtent(reservoir, 0, used);
        } else if (state == EMPTY) {
            return EmptySequence.getInstance();
        }
        return new SequenceExtent(iterate());
    }

    /**
     * A ProgressiveIterator starts by reading any items already held in the reservoir;
     * when the reservoir is exhausted, it reads further items from the inputIterator,
     * copying them into the reservoir as they are read.
     */

    public final class ProgressiveIterator implements SequenceIterator, LastPositionFinder, GroundedIterator {

        int position = -1;  // zero-based position in the reservoir of the
        // item most recently read

        /**
         * Create a ProgressiveIterator
         */

        public ProgressiveIterator() {
        }

        public Item next() throws XPathException {
            if (position == -2) {   // means we've already returned null once, keep doing so if called again.
                return null;
            }
            if (++position < used) {
                return reservoir[position];
            } else if (state == ALL_READ) {
                // someone else has read the input to completion in the meantime
                position = -2;
                return null;
            } else {
                Item i = inputIterator.next();
                if (i == null) {
                    state = ALL_READ;
                    condense();
                    position = -2;
                    return null;
                }
                position = used;
                append(i);
                state = MAYBE_MORE;
                return i;
            }
        }

        public Item current() {
            if (position < 0) {
                return null;
            }
            return reservoir[position];
        }

        public int position() {
            return position + 1;    // return one-based position
        }

        public SequenceIterator getAnother() {
            return new ProgressiveIterator();
        }

        /**
         * Get the last position (that is, the number of items in the sequence)
         */

        public int getLastPosition() throws XPathException {
            if (state == ALL_READ) {
                return used;
            } else if (state == EMPTY) {
                return 0;
            } else {
                // save the current position
                int savePos = position;
                // fill the reservoir
                while (true) {
                    Item item = next();
                    if (item == null) {
                        break;
                    }
                }
                // reset the current position
                position = savePos;
                // return the total number of items
                return used;
            }
        }

        /**
         * Return a value containing all the items in the sequence returned by this
         * SequenceIterator
         *
         * @return the corresponding value
         */

        public GroundedValue materialize() throws XPathException {
            if (state == ALL_READ) {
                return new SequenceExtent(reservoir);
            } else if (state == EMPTY) {
                return EmptySequence.getInstance();
            }
            return new SequenceExtent(iterate());

        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED} and {@link #LAST_POSITION_FINDER}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         */

        public int getProperties() {
            if (state == EMPTY || state == ALL_READ) {
                return GROUNDED | LAST_POSITION_FINDER;
            } else {
                return 0;
            }
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
