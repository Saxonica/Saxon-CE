package client.net.sf.saxon.ce.trans.update;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.trans.XPathException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * List of pending updates to the browser's HTML page
 */
public class PendingUpdateList {


    private List<PendingUpdateAction> list = new ArrayList<PendingUpdateAction>();

    private Logger logger = Logger.getLogger("PendingUpdateList");

    /**
     * Create a Pending Update List
     */

    public PendingUpdateList(Configuration config){
        //this.config = config;
    }

    /**
     * Add an action to the pending update list
     * @param action the Pending Update Action to be added to the list
     * @throws client.net.sf.saxon.ce.trans.XPathException if the pending update action conflicts with an action that is already on the list
     */

    public void add(PendingUpdateAction action) throws XPathException {
        list.add(action);
    }

    /**
     * Apply the pending updates
     * @param context the XPath dynamic evaluation context
     */

    public synchronized void apply(XPathContext context) throws XPathException {
    	String state = "";
    	try {
    	state = "delete";
        for (int i=0; i<list.size(); i++) {
            PendingUpdateAction action = list.get(i);
            if (action instanceof DeleteAction) {
                action.apply(context);
            }
        }
        state = "insert";
        for (int i=0; i<list.size(); i++) {
            PendingUpdateAction action = list.get(i);
            if (action instanceof InsertAction) {
                action.apply(context);
            }
        }
        state = "set-attribute";
        for (int i=0; i<list.size(); i++) {
            PendingUpdateAction action = list.get(i);
            if (action instanceof SetAttributeAction) {
                action.apply(context);
            }
        }
        state = "remove-attribute";
        for (int i=0; i<list.size(); i++) {
            PendingUpdateAction action = list.get(i);
            if (action instanceof RemoveAttributeAction) {
                action.apply(context);
            }
        }
        // empty list in case of further scheduled actions
        list = new ArrayList<PendingUpdateAction>();
    	} catch(Exception e) {
    		logger.severe("Error on DOM write action: " + state + " " + e.getMessage());
    		throw new XPathException(e);
    	}


    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

