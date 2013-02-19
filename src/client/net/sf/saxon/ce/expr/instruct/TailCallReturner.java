package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * This interface represents an expression that is capable of being processed leaving tail calls for the
 * calling instruction to deal with.
 */

public interface TailCallReturner {

    /**
    * ProcessLeavingTail: called to do the real work of this instruction. This method
    * must be implemented in each subclass. The results of the instruction are written
    * to the current Receiver, which can be obtained via the Controller.
    * @param context The dynamic context of the transformation, giving access to the current node,
    * the current variables, etc.
    * @return null if the instruction has completed execution; or a TailCall indicating
    * a function call or template call that is delegated to the caller, to be made after the stack has
    * been unwound so as to save stack space.
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException;
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
