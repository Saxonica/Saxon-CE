package client.net.sf.saxon.ce.trans.update;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;

import java.util.*;
import java.util.logging.Logger;

import java_cup.action_part;

/**
 * List of pending updates to the browser's HTML page
 */
public class PendingUpdateList {


    private Configuration config;

    private List<PendingUpdateAction> list = new ArrayList<PendingUpdateAction>();
    //private Set<NodeInfo> affectedTrees = new HashSet<NodeInfo>();

    //private Set<NodeInfo> renamedNodes = new HashSet<NodeInfo>();
    private Set<NodeInfo> replacedNodes = new HashSet<NodeInfo>();
    //private Set<NodeInfo> deletedNodes = new HashSet<NodeInfo>();
    private Set<NodeInfo> replacedValueNodes = new HashSet<NodeInfo>();
    private Logger logger = Logger.getLogger("PendingUpdateList");


    // Actions to insert, delete, rename, or replace attributes are sorted according to the element that they affect.
    // These actions are processed after all other actions.
//    private Map<NodeInfo, List<PendingUpdateAction>> attributeActions =
//            new HashMap<NodeInfo, List<PendingUpdateAction>>();

    /**
     * Create a Pending Update List
     */

    public PendingUpdateList(Configuration config){
        this.config = config;
    }

    /**
     * Add an action to the pending update list
     * @param action the Pending Update Action to be added to the list
     * @throws client.net.sf.saxon.ce.trans.XPathException if the pending update action conflicts with an action that is already on the list
     */

    public void add(PendingUpdateAction action) throws XPathException {
        list.add(action);
    }

//    private void addAttributeAction(NodeInfo element, PendingUpdateAction action) {
//        List<PendingUpdateAction> actions = attributeActions.get(element);
//        if (actions == null) {
//            actions = new ArrayList<PendingUpdateAction>(2);
//            attributeActions.put(element, actions);
//        }
//        actions.add(action);
//    }


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

//        // Now process the attribute actions for each affected element
//        for (Map.Entry<NodeInfo, List<PendingUpdateAction>> e : attributeActions.entrySet()) {
//            MutableNodeInfo element = (MutableNodeInfo)e.getKey();
//            if (!element.isDeleted()) {
//                applyAttributeActions(element, e.getValue());
//                affectedTrees.add((MutableNodeInfo)element.getRoot());
//            }
//        }

//        // Do the put() actions
//        Collection<PendingUpdateAction> list = lists[PutAction.PHASE];
//        if (list != null) {
//            for (Iterator<PendingUpdateAction> iter= list.iterator(); iter.hasNext();) {
//                PendingUpdateAction action = iter.next();
//                action.apply(context, affectedTrees);
//            }
//        }

        // invalidate all document-level indexes
        //TODO: delete xsl:key indexes
//        for (Iterator<MutableNodeInfo> iter = affectedTrees.iterator(); iter.hasNext();) {
//            NodeInfo root = iter.next();
//            if (root instanceof MutableDocumentInfo) {
//                context.getController().getKeyManager().clearDocumentIndexes((DocumentInfo)root);
//                ((MutableDocumentInfo)root).resetIndexes();
//            }
//        }

    }

    /**
     * Check consistency of the attribute actions for an element.
     *
     * We have already checked namespace constraints; the check here is that after applying all
     * the deletes, renames, replaces, and inserts, there will not be two attributes with the
     * same name.
     *
     * Everything is checked before any updates are applied, to ensure atomicity: if there are
     * any errors, the data should be unchanged.
     */

//    private void checkAttributeActions(MutableNodeInfo element, List<PendingUpdateAction> actions)
//    throws XPathException {
//
//        // TODO: the rules have been changed so that nodes in deleted or detached tree fragments
//        // are checked. This means we could do most of the checks much earlier.
//
//        IntToIntMap names = new IntToIntHashMap(); // map from fingerprints to the number of times
//                                                   // they will appear in the result
//        names.setDefaultValue(0);
//        // Get the existing attribute names
//        AxisIterator attributes = element.iterateAxis(Axis.ATTRIBUTE);
//        while (true) {
//            NodeInfo att = (NodeInfo)attributes.next();
//            if (att == null) {
//                break;
//            }
//            names.put(att.getFingerprint(), 1);
//        }
//        // Adjust for the names that will be created or removed using insert, replace, or rename
//        for (PendingUpdateAction action : actions) {
//            if (action instanceof DeleteAction) {
//                int fp = action.getTargetNode().getFingerprint();
//                int count = names.get(fp);
//                if (count > 0) {
//                    names.put(fp, count-1);
//                }
//            } else if (action instanceof RenameAction) {
//                int fp = action.getTargetNode().getFingerprint();
//                int count = names.get(fp);
//                if (count > 0) {
//                    names.put(fp, count-1);
//                }
//                fp = ((RenameAction)action).getNewNameCode() & NamePool.FP_MASK;
//                count = names.get(fp);
//                names.put(fp, count+1);
//            } else if (action instanceof ReplaceAttributeAction) {
//                NodeInfo target = ((ReplaceAttributeAction)action).getOldAttribute();
//                int fp = target.getFingerprint();
//                int count = names.get(fp);
//                if (count > 0) {
//                    names.put(fp, count-1);
//                }
//                AttributeCollection newNodes = ((ReplaceAttributeAction)action).getNewAttributes();
//                for (int i=0; i<newNodes.getLength(); i++) {
//                    fp = newNodes.getNameCode(i) & NamePool.FP_MASK;
//                    count = names.get(fp);
//                    names.put(fp, count+1);
//                }
//            } else if (action instanceof InsertAttributeAction) {
//                int fp = ((InsertAttributeAction)action).getNewNameCode() & NamePool.FP_MASK;
//                int count = names.get(fp);
//                names.put(fp, count+1);
//            }
//        }
//        // Finally, see whether any name will appear more than once
//        IntIterator iter = names.keyIterator();
//        while (iter.hasNext()) {
//            int fp = iter.next();
//            int count = names.get(fp);
//            if (count > 1) {
//                throw new XPathException("After applying all updates, the element at " +
//                        Navigator.getPath(element) + " would have " +
//                        (count==2 ? "two" : count) + " attributes named " +
//                        element.getNamePool().getClarkName(fp), "XUDY0021");
//            }
//        }
//
//    }

    /**
     * Apply attribute actions to an element. At this stage sufficient checking has been done
     * to ensure that the updates must succeed.
     */

//    private void applyAttributeActions(NodeInfo element, List<PendingUpdateAction> actions) {
//        // First do any delete actions
//        for (PendingUpdateAction action : actions) {
//            if (action instanceof DeleteAction) {
//                ((NodeInfo)action.getTargetNode()).delete();
//            }
//        }
//        // Delete any attributes that are being replaced
//        for (PendingUpdateAction action : actions) {
//            if (action instanceof ReplaceAttributeAction) {
//                NodeInfo target = ((ReplaceAttributeAction)action).getOldAttribute();
//                ((NodeInfo)target).delete();
//            }
//        }
//        // Rename any attributes that are being renamed, using a temporary name in the event of a failure
//        Map<NodeInfo, Integer> pendingRenames = null;
//        for (PendingUpdateAction action : actions) {
//            if (action instanceof RenameAction) {
//                int nc = ((RenameAction)action).getNewNameCode();
//                if (element.getAttributeValue(nc&NamePool.FP_MASK) != null) {
//                    // Name already exists. Need to allocate a temporary name. To conserve namepool space,
//                    // choose a system name (1 - 1024) that isn't already in use on the element
//                    int temp = -1;
//                    for (int n=1; n<1024; n++) {
//                         if (element.getAttributeValue(n) == null) {
//                             temp = n;
//                             break;
//                         }
//                    }
//                    if (temp == -1) {
//                        throw new NamePool.NamePoolLimitException(
//                                "Element has too many attributes: circular rename limit reached");
//                    }
//                    if (pendingRenames == null) {
//                        pendingRenames = new HashMap<NodeInfo, Integer>();
//                    }
//                    pendingRenames.put((NodeInfo)action.getTargetNode(), nc);
//                    nc = temp;
//                }
//                ((NodeInfo)action.getTargetNode()).rename(nc);
//            }
//        }
//        // Apply any pending renames
//        if (pendingRenames != null) {
//            for (Map.Entry<NodeInfo, Integer> e : pendingRenames.entrySet()) {
//                 e.getKey().rename(e.getValue());
//            }
//        }
//        // Create any attributes that result from replacements
//        for (PendingUpdateAction action : actions) {
//            if (action instanceof ReplaceAttributeAction) {
//                AttributeCollection content = ((ReplaceAttributeAction)action).getNewAttributes();
//                for (int i=0; i<content.getLength(); i++) {
//                    element.addAttribute(
//                            content.getNameCode(i),
//                            content.getTypeAnnotation(i),
//                            content.getValue(i),
//                            0);
//                }
//            }
//        }
//        // Create any inserted attributes
//        // Create any attributes that result from replacements
//        for (PendingUpdateAction action : actions) {
//            if (action instanceof InsertAttributeAction) {
//                InsertAttributeAction a = (InsertAttributeAction)action;
//                element.addAttribute(
//                            a.getNewNameCode(),
//                            a.getNewTypeCode(),
//                            a.getNewStringValue(),
//                            0);
//            }
//        }
//        element.removeTypeAnnotation();
//
//    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

