package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.KeyDefinitionSet;
import client.net.sf.saxon.ce.trans.KeyManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.Configuration;

/**
 * A KeyPattern is a pattern of the form key(keyname, keyvalue)
 */

public final class KeyPattern extends NodeSetPattern {

    private StructuredQName keyName;     // the key name
    private KeyDefinitionSet keySet;     // the set of keys corresponding to the key name

    /**
     * Constructor
     *
     * @param keyName the name of the key
     * @param key     the value of the key: either a StringValue or a VariableReference
     */

    public KeyPattern(StructuredQName keyName, Expression key, Configuration config) {
        super(key, config);
        this.keyName = keyName;
    }

    /**
     * Type-check the pattern. This is needed for patterns that contain
     * variable references or function calls.
     *
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        expression = visitor.typeCheck(expression, contextItemType);
        RoleLocator role = new RoleLocator(RoleLocator.FUNCTION, "key", 2);
        expression = TypeChecker.staticTypeCheck(expression, SequenceType.ATOMIC_SEQUENCE, false, role);
        keySet = visitor.getExecutable().getKeyManager().getKeyDefinitionSet(keyName);
        if (keySet == null) {
            XPathException err = new XPathException("Unknown key name " + keyName.getClarkName() + " in pattern");
            err.setErrorCode("XTDE1260");
            err.setLocator(this);
            err.setIsStaticError(true);
            throw err;
        }
        return this;
    }

    /**
     * Determine whether this Pattern matches the given Node.
     *
     * @param e The NodeInfo representing the Element or other node to be tested against the Pattern
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matches(NodeInfo e, XPathContext context) throws XPathException {
        KeyDefinitionSet kds = keySet;
        if (kds == null) {
            // shouldn't happen
            kds = context.getController().getExecutable().getKeyManager().getKeyDefinitionSet(keyName);
        }
        DocumentInfo doc = e.getDocumentRoot();
        if (doc == null) {
            return false;
        }
        KeyManager km = context.getController().getExecutable().getKeyManager();
        SequenceIterator iter = expression.iterate(context);
        while (true) {
            Item it = iter.next();
            if (it == null) {
                return false;
            }
            SequenceIterator nodes = km.selectByKey(kds, doc, (AtomicValue)it, context);
            while (true) {
                NodeInfo n = (NodeInfo)nodes.next();
                if (n == null) {
                    break;
                }
                if (n.isSameNodeInfo(e)) {
                    return true;
                }
            }
        }
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public NodeTest getNodeTest() {
        return AnyNodeTest.getInstance();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
