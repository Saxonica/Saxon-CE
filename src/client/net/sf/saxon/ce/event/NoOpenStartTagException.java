package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;

/**
* Exception indicating that an attribute or namespace node has been written when
* there is no open element to write it to
*/

public class NoOpenStartTagException extends XPathException {

    /**
     * Static factory method to create the exception
     * @param nodeKind the kind of node being created (attribute or namespace)
     * @param name the name of the node being created
     * @param parentIsDocument true if the nodes are being added to a document node (rather than an element)
     * @return the constructed exception object
     */

    public static NoOpenStartTagException makeNoOpenStartTagException(
            int nodeKind, String name, boolean parentIsDocument) {
        String message;
        String errorCode;
        if (parentIsDocument) {
            String kind = (nodeKind == Type.ATTRIBUTE ? "an attribute" : "a namespace");
            message = "Cannot create " + kind + " node (" + name + ") whose parent is a document node";
            errorCode = "XTDE0420";
        } else {
            String kind = (nodeKind == Type.ATTRIBUTE ? "An attribute" : "A namespace");
            message = kind + " node (" + name + ") cannot be created after the children of the containing element";
            errorCode = "XTDE0410";
        }
        NoOpenStartTagException err = new NoOpenStartTagException(message);
        err.setErrorCode(errorCode);
        return err;
    }

    public NoOpenStartTagException(String message) {
        super(message);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.