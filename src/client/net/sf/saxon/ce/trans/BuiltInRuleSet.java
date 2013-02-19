package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.instruct.ParameterSet;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.tree.util.SourceLocator;


/**
 * Defines a set of built-in template rules (rules for use when no user-defined template
 * rules match a given node)
 */
public interface BuiltInRuleSet  {

    /**
     * Perform the built-in template action for a given node.
     *
     * @param node the node to be processed
     * @param parameters the parameters supplied to apply-templates
     * @param tunnelParams the tunnel parameters to be passed through
     * @param context the dynamic evaluation context
     * @param sourceLocator location of the instruction (apply-templates, apply-imports etc) that caused
     * the built-in template to be invoked
     * @exception XPathException if any dynamic error occurs
     */    

    public void process( NodeInfo node,
                         ParameterSet parameters,
                         ParameterSet tunnelParams,
                         XPathContext context,
                         SourceLocator sourceLocator) throws XPathException;

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
