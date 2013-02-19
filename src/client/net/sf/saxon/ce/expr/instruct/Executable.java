package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.functions.FunctionLibraryList;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.KeyManager;
import client.net.sf.saxon.ce.trans.StripSpaceRules;
import client.net.sf.saxon.ce.trans.XPathException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A compiled stylesheet or a query in executable form.
 * Note that the original stylesheet tree is not retained.
 */

public class Executable {

    // the Configuration options
    private transient Configuration config;

    // definitions of strip/preserve space action
    private StripSpaceRules stripperRules;

    // definitions of keys, including keys created by the optimizer
    private KeyManager keyManager;

    // the map of slots used for global variables and params
    private SlotManager globalVariableMap;

    // list of functions available in the static context
    private FunctionLibraryList functionLibrary;

    // a list of required parameters, identified by the structured QName of their names
    private HashSet<StructuredQName> requiredParams = null;

    // a boolean, true if the executable represents a stylesheet that uses xsl:result-document
    private boolean createsSecondaryResult = false;


    /**
     * Create a new Executable (a collection of stylesheet modules and/or query modules)
     * @param config the Saxon Configuration
     */

    public Executable(Configuration config) {
        setConfiguration(config);
    }

    /**
     * Set the configuration
     * @param config the Configuration
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the configuration
     * @return the Configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the library containing all the in-scope functions in the static context
     *
     * @return the function libary
     */

    public FunctionLibraryList getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Set the library containing all the in-scope functions in the static context
     *
     * @param functionLibrary the function libary
     */

    public void setFunctionLibrary(FunctionLibraryList functionLibrary) {
        //System.err.println("***" + this + " setFunctionLib to " + functionLibrary);
        this.functionLibrary = functionLibrary;
    }

     /**
     * Set the rules determining which nodes are to be stripped from the tree
     *
     * @param rules a Mode object containing the whitespace stripping rules. A Mode
     *              is generally a collection of template rules, but it is reused here to represent
     *              a collection of stripping rules.
     */

    public void setStripperRules(StripSpaceRules rules) {
        stripperRules = rules;
    }

    /**
     * Get the rules determining which nodes are to be stripped from the tree
     *
     * @return a Mode object containing the whitespace stripping rules. A Mode
     *         is generally a collection of template rules, but it is reused here to represent
     *         a collection of stripping rules.
     */

    public StripSpaceRules getStripperRules() {
        return stripperRules;
    }

    /**
     * Get the KeyManager which handles key definitions
     *
     * @return the KeyManager containing the xsl:key definitions
     */

    public KeyManager getKeyManager() {
        if (keyManager == null) {
            keyManager = new KeyManager();
        }
        return keyManager;
    }

    /**
     * Get the global variable map
     *
     * @return the SlotManager defining the allocation of slots to global variables
     */

    public SlotManager getGlobalVariableMap() {
        if (globalVariableMap == null) {
            globalVariableMap = new SlotManager();
        }
        return globalVariableMap;
    }

    /**
     * Allocate space in bindery for all the variables needed
     * @param bindery The bindery to be initialized
     */

    public void initializeBindery(Bindery bindery) {
        bindery.allocateGlobals(getGlobalVariableMap());
    }

    /**
     * Add a required parameter. Used in XSLT only.
     * @param qName the name of the required parameter
     */

    public void addRequiredParam(StructuredQName qName) {
        if (requiredParams == null) {
            requiredParams = new HashSet<StructuredQName>(5);
        }
        requiredParams.add(qName);
    }

    /**
     * Check that all required parameters have been supplied. Used in XSLT only.
     * @param params the set of parameters that have been supplied
     * @throws XPathException if there is a required parameter for which no value has been supplied
     */

    public void checkAllRequiredParamsArePresent(HashMap<StructuredQName, ValueRepresentation> params) throws XPathException {
        if (requiredParams == null) {
            return;
        }
        Iterator<StructuredQName> iter = requiredParams.iterator();
        while (iter.hasNext()) {
            StructuredQName req = iter.next();
            if (params == null || !params.containsKey(req)) {
 //           if (params == null || params.get(req) == null) {
                XPathException err = new XPathException("No value supplied for required parameter " +
                        req.getDisplayName());
                err.setErrorCode("XTDE0050");
                throw err;
            }
        }
    }


    /**
     * Set whether this executable represents a stylesheet that uses xsl:result-document
     * to create secondary output documents
     * @param flag true if the executable uses xsl:result-document
     */

    public void setCreatesSecondaryResult(boolean flag) {
        createsSecondaryResult = flag;
    }

    /**
     * Ask whether this executable represents a stylesheet that uses xsl:result-document
     * to create secondary output documents
     * @return true if the executable uses xsl:result-document
     */

    public boolean createsSecondaryResult() {
        return createsSecondaryResult;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
