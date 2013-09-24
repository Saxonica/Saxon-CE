package client.net.sf.saxon.ce.expr;


import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;

public class UnfailingItemMappingIterator extends ItemMappingIterator implements UnfailingIterator {

    public UnfailingItemMappingIterator(UnfailingIterator base, ItemMappingFunction action) {
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
    protected UnfailingIterator getBaseIterator() {
        return (UnfailingIterator)super.getBaseIterator();
    }

    @Override
    public UnfailingIterator getAnother() {
        try {
            UnfailingIterator newBase = getBaseIterator().getAnother();
            ItemMappingFunction action = getMappingFunction();
            ItemMappingFunction newAction = action instanceof StatefulMappingFunction ?
                    (ItemMappingFunction)((StatefulMappingFunction)action).getAnother(newBase) :
                    action;
            return new UnfailingItemMappingIterator(newBase, newAction);
        } catch (XPathException e) {
            throw new AssertionError(e);
        }
    }
}

