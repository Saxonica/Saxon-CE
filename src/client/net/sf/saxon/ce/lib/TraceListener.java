package client.net.sf.saxon.ce.lib;


import java.util.EventListener;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.trace.InstructionInfo;

/**
 * This interface defines methods that are called by Saxon during the execution of
 * a stylesheet, if tracing is switched on. Tracing can be switched on by nominating
 * an implementation of this class using the TRACE_LISTENER feature of the TransformerFactory,
 * or using the addTraceListener() method of the Controller, which is Saxon's implementation
 * of tyhe JAXP javax.xml.transform.Transformer interface.
 */

public interface TraceListener extends EventListener {

    /**
     * Method called to supply the destination for output
     * @param stream a PrintStream to which any output produced by the TraceListener should be written
     */

    //public void setOutputDestination(PrintStream stream);

    /**
     * Method called at the start of execution, that is, when the run-time transformation starts
     * @param controller identifies the transformation controller, and provides the listener with
     * access to context and configuration information
     */

    public void open();

    /**
     * Method called at the end of execution, that is, when the run-time execution ends
     */

    public void close();

    /**
     * Method that is called when an instruction in the stylesheet gets processed.
     *
     * @param instruction gives information about the instruction being
     *                    executed, and about the context in which it is executed. This object is mutable,
     *                    so if information from the InstructionInfo is to be retained, it must be copied.
     */

    public void enter(InstructionInfo instruction, XPathContext context);

    /**
     * Method that is called after processing an instruction of the stylesheet,
     * that is, after any child instructions have been processed.
     *
     * @param instruction gives the same information that was supplied to the
     *                    enter method, though it is not necessarily the same object. Note that the
     *                    line number of the instruction is that of the start tag in the source stylesheet,
     *                    not the line number of the end tag.
     */

    public void leave(InstructionInfo instruction);

    /**
     * Method that is called by an instruction that changes the current item
     * in the source document: that is, xsl:for-each, xsl:apply-templates, xsl:for-each-group.
     * The method is called after the enter method for the relevant instruction, and is called
     * once for each item processed.
     *
     * @param currentItem the new current item. Item objects are not mutable; it is safe to retain
     *                    a reference to the Item for later use.
     */

    public void startCurrentItem(Item currentItem);

    /**
     * Method that is called when an instruction has finished processing a new current item
     * and is ready to select a new current item or revert to the previous current item.
     * The method will be called before the leave() method for the instruction that made this
     * item current.
     *
     * @param currentItem the item that was current, whose processing is now complete. This will represent
     *                    the same underlying item as the corresponding startCurrentItem() call, though it will
     *                    not necessarily be the same actual object.
     */

    public void endCurrentItem(Item currentItem);

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Edwin Glaser (edwin@pannenleiter.de)
//
// Portions created by Saxonica Limited are Copyright (C) Saxonica Limited 2011. All Rights Reserved.
//
// Contributor(s): Heavily modified by Saxonica Limited 
//
