package client.net.sf.saxon.ce.expr;


import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;

public class UnfailingItemMappingIterator extends ItemMappingIterator implements UnfailingIterator {

    public UnfailingItemMappingIterator(SequenceIterator base, ItemMappingFunction action) {
        super(base, action);
    }

    @Override
    public Item next()  {
        try {
            return super.next();
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

    @Override
    public SequenceIterator getAnother() {
        try {
            return super.getAnother();
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }
}

