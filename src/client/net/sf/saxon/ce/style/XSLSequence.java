package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.ItemType;
import com.google.gwt.logging.client.LogConfiguration;


/**
 * An xsl:sequence element in the stylesheet. <br>
 * The xsl:sequence element takes attributes:<ul>
 * <li>a mandatory attribute select="expression".</li>
 * </ul>
 */

public final class XSLSequence extends StyleElement {

    private Expression select;
    private String selectAttTrace = "";

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return select.getItemType();
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return false;
    }

    /**
    * Determine whether this type of element is allowed to contain an xsl:fallback
    * instruction
    */

    public boolean mayContainFallback() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
        select = (Expression)checkAttribute("select", "e1");
        checkForUnknownAttributes();
        
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	selectAttTrace = getAttributeValue("", "select");
        }
    }

    public void validate(Declaration decl) throws XPathException {
        UnfailingIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) break;
            if (!(child instanceof XSLFallback)) {
                compileError("The only child node allowed for xsl:sequence is an xsl:fallback instruction", "XTSE0010");
                break;
            }
        }
        select = typeCheck(select);
    }

    public Expression compile(Executable exec, Declaration decl) {
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	select.AddTraceProperty("select", selectAttTrace);
        }
        return select;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
