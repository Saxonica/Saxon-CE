package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.NumericValue;

/**
* The XPath 2.0 insert-before() function
*/


public class Insert extends SystemFunction {

    public Insert newInstance() {
        return new Insert();
    }

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue n0 = (AtomicValue)argument[1].evaluateItem(context);
        NumericValue n = (NumericValue)n0;
        int pos = (int)n.intValue();
        SequenceIterator ins = argument[2].iterate(context);
        return new InsertIterator(seq, ins, pos);
    }

    public static class InsertIterator implements SequenceIterator {

        private SequenceIterator base;
        private SequenceIterator insert;
        private int insertPosition;
        private int position = 0;
        private Item current = null;
        private boolean inserting = false;

        public InsertIterator(SequenceIterator base, SequenceIterator insert, int insertPosition) {
            this.base = base;
            this.insert = insert;
            this.insertPosition = (insertPosition<1 ? 1 : insertPosition);
            this.inserting = (insertPosition==1);
        }


        public Item next() throws XPathException {
            Item nextItem;
            if (inserting) {
                nextItem = insert.next();
                if (nextItem == null) {
                    inserting = false;
                    nextItem = base.next();
                }
            } else {
                if (position == insertPosition-1) {
                    nextItem = insert.next();
                    if (nextItem == null) {
                        nextItem = base.next();
                    } else {
                        inserting = true;
                    }
                } else {
                    nextItem = base.next();
                    if (nextItem==null && position < insertPosition-1) {
                        inserting = true;
                        nextItem = insert.next();
                    }
                }
            }
            if (nextItem == null) {
                current = null;
                position = -1;
                return null;
            } else {
                current = nextItem;
                position++;
                return current;
            }
        }

        public SequenceIterator getAnother() throws XPathException {
            return new InsertIterator(  base.getAnother(),
                                        insert.getAnother(),
                                        insertPosition);
        }

    }

}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.