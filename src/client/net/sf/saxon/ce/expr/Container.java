package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.tree.util.SourceLocator;


/**
 * A Container is something other than an expression that can act as the container of an expression.
 * It is typically an object such as a function, a global variable, or in XSLT a template, or an attribute set.
 * When free-standing XPath expressions are compiled, the static context for the expression acts as its
 * container.
 */

public interface Container {

    /**
     * Get the Executable (representing a complete stylesheet or query) of which this Container forms part
     * @return the executable
     */

    public Executable getExecutable();

    /**
     * Get the SourceLocator allowing location identifiers to be resolved.
     * @return the SourceLocator
     */

    public SourceLocator getSourceLocator();

    /**
     * Get the granularity of the container. During successive phases of compilation, growing
     * expression trees are rooted in containers of increasing granularity. The granularity
     * of the container is used to avoid "repotting" a tree more frequently than is required,
     * as this requires a complete traversal of the tree which can take a measurable time.
     * @return 0 for a temporary container created during parsing; 1 for a container
     * that operates at the level of an XPath expression; 2 for a container at the level
     * of a global function or template
     */

    public int getContainerGranularity();
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
