package client.net.sf.saxon.ce.trace;


import java.util.EventListener;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.TraceListener;
import client.net.sf.saxon.ce.om.Item;

/**
 * A class which implements efficient and thread-safe multi-cast event
 * dispatching for the TraceListener evants.
 */
public class TraceEventMulticaster implements TraceListener {

    protected final EventListener a, b;

    /**
     * Creates an event multicaster instance which chains listener-a
     * with listener-b.
     *
     * @param a listener-a
     * @param b listener-b
     */
    protected TraceEventMulticaster(EventListener a, EventListener b) {
        this.a = a;
        this.b = b;
    }

//    public void setOutputDestination(PrintStream stream) {
//        ((TraceListener)a).setOutputDestination(stream);
//        ((TraceListener)b).setOutputDestination(stream);
//    }

    /**
     * Removes a listener from this multicaster and returns the
     * resulting multicast listener.
     *
     * @param oldl the listener to be removed
     */
    /*@Nullable*/ protected EventListener remove(EventListener oldl) {
        if (oldl == a) {
            return b;
        }
        if (oldl == b) {
            return a;
        }
        EventListener a2 = removeInternal(a, oldl);
        EventListener b2 = removeInternal(b, oldl);
        if (a2 == a && b2 == b) {
            return this;    // it's not here
        }
        return addInternal(a2, b2);
    }

    /**
     * Called at start
     */

    public void open() {
        ((TraceListener)a).open();
        ((TraceListener)b).open();
    }

    /**
     * Called at end
     */

    public void close() {
        ((TraceListener)a).close();
        ((TraceListener)b).close();
    }


    /**
     * Called when an element of the stylesheet gets processed
     */
    public void enter(InstructionInfo element, XPathContext context) {
        ((TraceListener)a).enter(element, context);
        ((TraceListener)b).enter(element, context);
    }

    /**
     * Called after an element of the stylesheet got processed
     */
    public void leave(InstructionInfo element) {
        ((TraceListener)a).leave(element);
        ((TraceListener)b).leave(element);
    }

    /**
     * Called when an item becomes current
     */
    public void startCurrentItem(Item item) {
        ((TraceListener)a).startCurrentItem(item);
        ((TraceListener)b).startCurrentItem(item);
    }

    /**
     * Called when an item ceases to be the current item
     */
    public void endCurrentItem(Item item) {
        ((TraceListener)a).endCurrentItem(item);
        ((TraceListener)b).endCurrentItem(item);
    }

    /**
     * Adds trace-listener-a with trace-listener-b and
     * returns the resulting multicast listener.
     *
     * @param a trace-listener-a
     * @param b trace-listener-b
     */
    public static TraceListener add(TraceListener a, TraceListener b) {
        return (TraceListener)addInternal(a, b);
    }

    /**
     * Removes the old trace-listener from trace-listener-l and
     * returns the resulting multicast listener.
     *
     * @param l    trace-listener-l
     * @param oldl the trace-listener being removed
     */
    public static TraceListener remove(TraceListener l, TraceListener oldl) {
        return (TraceListener)removeInternal(l, oldl);
    }

    /**
     * Returns the resulting multicast listener from adding listener-a
     * and listener-b together.
     * If listener-a is null, it returns listener-b;
     * If listener-b is null, it returns listener-a
     * If neither are null, then it creates and returns
     * a new EventMulticaster instance which chains a with b.
     *
     * @param a event listener-a
     * @param b event listener-b
     */
    protected static EventListener addInternal(EventListener a, EventListener b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return new TraceEventMulticaster(a, b);
    }

    /**
     * Returns the resulting multicast listener after removing the
     * old listener from listener-l.
     * If listener-l equals the old listener OR listener-l is null,
     * returns null.
     * Else if listener-l is an instance of SaxonEventMulticaster,
     * then it removes the old listener from it.
     * Else, returns listener l.
     *
     * @param l    the listener being removed from
     * @param oldl the listener being removed
     */

    protected static EventListener removeInternal(EventListener l, EventListener oldl) {
        if (l == oldl || l == null) {
            return null;
        } else if (l instanceof TraceEventMulticaster) {
            return ((TraceEventMulticaster)l).remove(oldl);
        } else {
            return l;        // it's not here
        }
    }

}

// Contributors: Edwin Glaser (edwin@pannenleiter.de); Michael Kay (Saxonica)

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

