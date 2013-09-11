package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;

import java.util.HashMap;


/**
* The Bindery class holds information about variables and their values. From Saxon 8.1, it is
* used only for global variables: local variables are now held in the XPathContext object.
*
* Variables are identified by a Binding object. Values will always be of class Value.
*/

public final class Bindery  {

    private ValueRepresentation[] globals;          // values of global variables and parameters
    private boolean[] busy;                         // set to true while variable is being evaluated
    private HashMap<StructuredQName, ValueRepresentation> globalParameters;    // supplied global parameters

    /**
     * Define how many slots are needed for global variables
     * @param map the SlotManager that keeps track of slot allocation for global variables.
    */

    public void allocateGlobals(SlotManager map) {
        int n = map.getNumberOfVariables()+1;
        globals = new ValueRepresentation[n];
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

    public void defineGlobalParameters(HashMap<StructuredQName, ValueRepresentation> params) {
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

        ValueRepresentation val = null;
        if (obj instanceof ValueRepresentation) {
            val = (ValueRepresentation)obj;
        }
        if (val==null) {
            val = EmptySequence.getInstance();
        }

        val = applyFunctionConversionRules(val, requiredType, context);

        XPathException err = TypeChecker.testConformance(val, requiredType, context);
        if (err != null) {
            throw err;
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

    public static Value applyFunctionConversionRules(
            ValueRepresentation value, SequenceType requiredType, final XPathContext context)
            throws XPathException {
        final TypeHierarchy th = TypeHierarchy.getInstance();
        final ItemType requiredItemType = requiredType.getPrimaryType();
        ItemType suppliedItemType = (value instanceof NodeInfo
                ? new NameTest(((NodeInfo)value))
                : ((Value)value).getItemType());

        SequenceIterator iterator = Value.asIterator(value);

        if (requiredItemType.isAtomicType()) {

            // step 1: apply atomization if necessary

            if (!suppliedItemType.isAtomicType()) {
                iterator = Atomizer.getAtomizingIterator(iterator);
                suppliedItemType = suppliedItemType.getAtomizedItemType();
            }

            // step 2: convert untyped atomic values to target item type

            if (th.relationship(suppliedItemType, BuiltInAtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT) {
                ItemMappingFunction converter = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if (item instanceof UntypedAtomicValue) {
                            return ((UntypedAtomicValue)item).convert(
                                    (BuiltInAtomicType)requiredItemType, true).asAtomic();
                        } else {
                            return item;
                        }
                    }
                };
                iterator = new ItemMappingIterator(iterator, converter, true);
            }

            // step 3: apply numeric promotion

            if (requiredItemType.equals(BuiltInAtomicType.DOUBLE)) {
                ItemMappingFunction promoter = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if (item instanceof NumericValue) {
                            return ((AtomicValue)item).convert(
                                BuiltInAtomicType.DOUBLE, true).asAtomic();
                        } else {
                            throw new XPathException(
                                    "Cannot promote non-numeric value to xs:double", "XPTY0004");
                        }
                    }
                };
                iterator = new ItemMappingIterator(iterator, promoter, true);
            } else if (requiredItemType.equals(BuiltInAtomicType.FLOAT)) {
                ItemMappingFunction promoter = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if (item instanceof DoubleValue) {
                            throw new XPathException(
                                    "Cannot promote xs:double value to xs:float", "XPTY0004");
                        } else if (item instanceof NumericValue) {
                            return ((AtomicValue)item).convert(
                                BuiltInAtomicType.FLOAT, true).asAtomic();
                        } else {
                            throw new XPathException(
                                    "Cannot promote non-numeric value to xs:float", "XPTY0004");
                        }
                    }
                };
                iterator = new ItemMappingIterator(iterator, promoter, true);
            }

            // step 4: apply URI-to-string promotion

            if (requiredItemType.equals(BuiltInAtomicType.STRING) &&
                    th.relationship(suppliedItemType, BuiltInAtomicType.ANY_URI) != TypeHierarchy.DISJOINT) {
                ItemMappingFunction promoter = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if (item instanceof AnyURIValue) {
                            return new StringValue(item.getStringValue());
                        } else {
                            return item;
                        }
                    }
                };
                iterator = new ItemMappingIterator(iterator, promoter, true);
            }
        }

        return Value.asValue(SequenceExtent.makeSequenceExtent(iterator));
    }

    /**
    * Provide a value for a global variable
    * @param binding identifies the variable
    * @param value the value of the variable
    */

    public void defineGlobalVariable(GlobalVariable binding, ValueRepresentation value) {
        globals[binding.getSlotNumber()] = value;
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

    public synchronized ValueRepresentation saveGlobalVariableValue(GlobalVariable binding, ValueRepresentation value) {
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
    * Get the value of a global variable
    * @param binding the Binding that establishes the unique instance of the variable
    * @return the Value of the variable if defined, null otherwise.
    */

    public ValueRepresentation getGlobalVariableValue(GlobalVariable binding) {
        return globals[binding.getSlotNumber()];
    }

    /**
    * Get the value of a global variable whose slot number is known
    * @param slot the slot number of the required variable
    * @return the Value of the variable if defined, null otherwise.
    */

    public ValueRepresentation getGlobalVariable(int slot) {
        return globals[slot];
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
