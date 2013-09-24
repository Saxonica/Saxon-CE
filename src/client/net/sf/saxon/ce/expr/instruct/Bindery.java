package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.DocumentPool;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.SequenceExtent;
import client.net.sf.saxon.ce.value.SequenceType;

import java.util.HashMap;


/**
* The Bindery class holds information about variables and their values. From Saxon 8.1, it is
* used only for global variables: local variables are now held in the XPathContext object.
*
* Variables are identified by a Binding object. Values will always be of class Value.
*/

public final class Bindery  {

    private Sequence[] globals;          // values of global variables and parameters
    private boolean[] busy;                         // set to true while variable is being evaluated
    private HashMap<StructuredQName, Sequence> globalParameters;    // supplied global parameters

    /**
     * Define how many slots are needed for global variables
     * @param numberOfGlobals number of slots needed for global variables.
    */

    public void allocateGlobals(int numberOfGlobals) {
        int n = numberOfGlobals+1;
        globals = new Sequence[n];
        busy = new boolean[n];
        for (int i=0; i<n; i++) {
            globals[i] = null;
            busy[i] = false;
        }
    }


    /**
    * Define global parameters
    * @param params The ParameterSet passed in by the user, eg. from the command line
    */

    public void defineGlobalParameters(HashMap<StructuredQName, Sequence> params) {
        globalParameters = params;
    }

    /**
     * Use global parameter. This is called when a global xsl:param element is processed.
     * If a parameter of the relevant name was supplied, it is bound to the xsl:param element.
     * Otherwise the method returns false, so the xsl:param default will be evaluated.
     * @param qName The name of the parameter
     * @param slot The slot number allocated to the parameter
     * @param requiredType The declared type of the parameter
     * @param context the XPath dynamic evaluation context
     * @return true if a parameter of this name was supplied, false if not
     */

    public boolean useGlobalParameter(StructuredQName qName, int slot, SequenceType requiredType, XPathContext context)
            throws XPathException {
        if (globals != null && globals[slot] != null) {
            return true;
        }

        if (globalParameters==null) {
            return false;
        }
        Object obj = globalParameters.get(qName);
        if (obj==null) {
            return false;
        }

        // If the supplied value is a document node, and the document node has a systemID that is an absolute
        // URI, and the absolute URI does not already exist in the document pool, then register it in the document
        // pool, so that the document-uri() function will find it there, and so that a call on doc() will not
        // reload it.

        if (obj instanceof DocumentInfo) {
            String systemId = ((DocumentInfo)obj).getSystemId();
            try {
                if (systemId != null && new URI(systemId, true).isAbsolute()) {
                    DocumentPool pool = context.getController().getDocumentPool();
                    if (pool.find(systemId) == null) {
                        pool.add(((DocumentInfo)obj), systemId);
                    }
                }
            } catch (URI.URISyntaxException err) {
                // ignore it
            }
        }   

        Sequence val = null;
        if (obj instanceof Sequence) {
            val = (Sequence)obj;
        }
        if (val==null) {
            val = EmptySequence.getInstance();
        }

        val = applyFunctionConversionRules(qName, val, requiredType, context);

        String err = TypeChecker.testConformance(val.iterate(), requiredType);
        if (err != null) {
            throw new XPathException(err, "XPTY0004");
        }

        globals[slot] = val;
        return true;
    }

    /**
     * Apply the function conversion rules to a value, given a required type.
     * @param value a value to be converted
     * @param requiredType the required type
     * @param context the conversion context
     * @return the converted value
     * @throws XPathException if the value cannot be converted to the required type
     */

    public static Sequence applyFunctionConversionRules(
            StructuredQName qName,
            Sequence value, SequenceType requiredType, final XPathContext context)
            throws XPathException {

        RoleLocator role = new RoleLocator(RoleLocator.PARAM, qName, 0);
        Expression e = TypeChecker.staticTypeCheck(new Literal(value), requiredType, false, role);
        return SequenceExtent.makeSequenceExtent(e.iterate(context));
    }



    /**
     * Set/Unset a flag to indicate that a particular global variable is currently being
     * evaluated. Note that this code is not synchronized, so there is no absolute guarantee that
     * two threads will not both evaluate the same global variable; however, apart from wasted time,
     * it is harmless if they do.
     * @param binding the global variable in question
     * @return true if evaluation of the variable should proceed; false if it is found that the variable has now been
     * evaluated in another thread.
     * @throws client.net.sf.saxon.ce.trans.XPathException If an attempt is made to set the flag when it is already set, this means
     * the definition of the variable is circular.
    */

    public boolean setExecuting(GlobalVariable binding)
    throws XPathException {
        int slot = binding.getSlotNumber();

        if (busy[slot]) {
            // The global variable is being evaluated in this thread. This shouldn't happen, because
            // we have already tested for circularities. If it does happen, however, we fail cleanly.
            throw new XPathException.Circularity("Circular definition of variable "
                    + binding.getVariableQName().getDisplayName());

        }
        busy[slot] = false;
        return true;
    }

    /**
     * Indicate that a global variable is not currently being evaluated
     * @param binding the global variable
     */

    public void setNotExecuting(GlobalVariable binding) {
        int slot = binding.getSlotNumber();
        busy[slot] = false;
    }


    /**
     * Save the value of a global variable, and mark evaluation as complete.
     * @param binding the global variable in question
     * @param value the value that this thread has obtained by evaluating the variable
     * @return the value actually given to the variable. Exceptionally this will be different from the supplied
     * value if another thread has evaluated the same global variable concurrently. The returned value should be
     * used in preference, to ensure that all threads agree on the value. They could be different if for example
     * the variable is initialized using the collection() function.
    */

    public synchronized Sequence saveGlobalVariableValue(GlobalVariable binding, Sequence value) {
        int slot = binding.getSlotNumber();
        if (globals[slot] != null) {
            // another thread has already evaluated the value
            return globals[slot];
        } else {
            busy[slot] = false;
            globals[slot] = value;
            return value;
        }
    }

    /**
    * Get the value of a global variable whose slot number is known
    * @param slot the slot number of the required variable
    * @return the Value of the variable if defined, null otherwise.
    */

    public Sequence getGlobalVariable(int slot) {
        return globals[slot];
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
