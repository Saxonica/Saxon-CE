package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.StructuredQName;


/**
* Generic interface representing a variable declaration in the static context of an XPath expression.
* The declaration may be internal or external to the XPath expression itself. An external
* VariableDeclaration is identified (perhaps created) by the bindVariable() method in the StaticContext.
*/

public interface VariableDeclaration {

    /**
     * Method called by a BindingReference to register the variable reference for
     * subsequent fixup.
     * This method is called by the XPath parser when
     * each reference to the variable is encountered. At some time after parsing and before execution of the
     * expression, the VariableDeclaration is responsible for calling the two methods setStaticType()
     * and fixup() on each BindingReference that has been registered with it.<br>
     * @param ref the variable reference
    */

    public void registerReference(VariableReference ref);

    /**
     * Get the name of the variable as a structured QName
     * @return the variable name
    */

    public StructuredQName getVariableQName();

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.