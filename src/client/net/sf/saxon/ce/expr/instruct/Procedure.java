package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.Container;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionTool;
import client.net.sf.saxon.ce.trace.InstructionInfo;
import client.net.sf.saxon.ce.tree.util.SourceLocator;

import java.util.Collections;
import java.util.Iterator;

/**
 * This object represents the compiled form of a user-written function, template, attribute-set, etc
 * (the source can be either an XSLT stylesheet function or an XQuery function).
 *
 * <p>It is assumed that type-checking, of both the arguments and the results,
 * has been handled at compile time. That is, the expression supplied as the body
 * of the function must be wrapped in code to check or convert the result to the
 * required type, and calls on the function must be wrapped at compile time to check or
 * convert the supplied arguments.
 */

public abstract class Procedure implements Container, InstructionInfo {

    private SourceLocator sourceLocator;
    protected Expression body;
    private Executable executable;
    private int numberOfSlots;

    public Procedure() {}

    /**
     * Get the granularity of the container.
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 2;
    }

    public void setSourceLocator(SourceLocator locator) {
        this.sourceLocator = locator;
    }

    public SourceLocator getSourceLocator() {
        return sourceLocator;
    }

    public void setBody(Expression body) {
        this.body = body;
        body.setContainer(this);
    }

    public final Expression getBody() {
        return body;
    }

    public void allocateSlots(int reserved) {
        numberOfSlots = ExpressionTool.allocateSlots(body, reserved);
    }
//    public void setStackFrameMap(SlotManager map) {
//        numberOfSlots = map.getNumberOfVariables();
//    }

    public int getNumberOfSlots() {
        return numberOfSlots;
    }

    public final Executable getExecutable() {
        return executable;
    }

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }
    // added for Trace features
    public Object getProperty(String name) {
        return null;
    }
    
    public int getLineNumber() {
        return -1;
    }
    
    public String getSystemId() {
        return "";
    }

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property. The iterator may return properties whose
     * value is null.
     */

    public Iterator getProperties() {
        return Collections.EMPTY_LIST.iterator();
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.