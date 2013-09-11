package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import com.google.gwt.logging.client.LogConfiguration;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.AxisExpression;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.RoleLocator;
import client.net.sf.saxon.ce.expr.TypeChecker;
import client.net.sf.saxon.ce.expr.instruct.ApplyTemplates;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.sort.SortExpression;
import client.net.sf.saxon.ce.expr.sort.SortKeyDefinition;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.Mode;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Whitespace;


/**
* An xsl:apply-templates element in the stylesheet
*/

public class XSLApplyTemplates extends StyleElement {

    private Expression select;
    private StructuredQName modeName;   // null if no name specified or if conventional values such as #current used
    private boolean useCurrentMode = false;
    private boolean useTailRecursion = false;
    private boolean defaultedSelectExpression = true;
    private Mode mode;
    private String modeAttribute;
    private String selectAtt = null;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }


    public void prepareAttributes() throws XPathException {

//		AttributeCollection atts = getAttributeList();
//
//		selectAtt = null;
//
//		for (int a=0; a<atts.getLength(); a++) {
//			StructuredQName qn = atts.getStructuredQName(a);
//            String f = qn.getClarkName();
//			if (f.equals("mode")) {
//        		modeAttribute = Whitespace.trim(atts.getValue(a));
//        	} else if (f.equals("select")) {
//        		selectAtt = atts.getValue(a);
//                defaultedSelectExpression = false;
//            } else {
//        		checkUnknownAttribute(qn);
//        	}
//        }

        select = (Expression)checkAttribute("select", "e");
        modeAttribute = (String)checkAttribute("mode", "w");
        checkForUnknownAttributes();


        if (modeAttribute!=null) {
            if (modeAttribute.equals("#current")) {
                useCurrentMode = true;
            } else if (modeAttribute.equals("#default")) {
                // do nothing;
            } else {
                try {
                    modeName = makeQName(modeAttribute);
                } catch (NamespaceException err) {
                    compileError(err.getMessage(), "XTSE0280");
                    modeName = null;
                } catch (XPathException err) {
                    compileError("Mode name " + Err.wrap(modeAttribute) + " is not a valid QName",
                            err.getErrorCodeQName());
                    modeName = null;
                }
            }
        }

        if (select != null) {
            defaultedSelectExpression = false;
        }
    }

    public void validate(Declaration decl) throws XPathException {

        //checkWithinTemplate();

        // get the Mode object
        if (!useCurrentMode) {
            if (modeName == null) {
                // XSLT 3.0 allows a default mode to be specified on the xsl:stylesheet element
                modeName = getContainingStylesheet().getDefaultMode();
            }
            mode = getPreparedStylesheet().getRuleManager().getMode(modeName, true);
        }

        // handle sorting if requested

        UnfailingIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLSort) {
                // no-op
            } else if (child instanceof XSLWithParam) {
                // usesParams = true;
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValue())) {
                    compileError("No character data is allowed within xsl:apply-templates", "XTSE0010");
                }
            } else {
                compileError("Invalid element within xsl:apply-templates", "XTSE0010");
            }
        }

        if (select==null) {
            select = new AxisExpression(Axis.CHILD, null);
            select.setSourceLocator(this);
        }

        select = typeCheck(select);
        try {
            RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:apply-templates/select", 0);
            role.setErrorCode("XTTE0520");
            select = TypeChecker.staticTypeCheck(select,
                                        SequenceType.NODE_SEQUENCE,
                                        false, role, makeExpressionVisitor());
        } catch (XPathException err) {
            compileError(err);
        }

    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
    */

    public boolean markTailCalls() {
        useTailRecursion = true;
        return true;
    }


    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        SortKeyDefinition[] sortKeys = makeSortKeys(decl);
        if (sortKeys != null) {
            useTailRecursion = false;
        }
        Expression sortedSequence = select;
        if (sortKeys != null) {
            sortedSequence = new SortExpression(select, sortKeys);
        }
        compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
        ApplyTemplates app = new ApplyTemplates(
                                    sortedSequence,
                                    useCurrentMode,
                                    useTailRecursion,
                                    defaultedSelectExpression,
                                    mode);
        app.setActualParameters(getWithParamInstructions(exec, decl, false, app),
                                 getWithParamInstructions(exec, decl, true, app));
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	if (selectAtt != null) {
        		app.AddTraceProperty("select", selectAtt);
        	}
        	if (modeAttribute != null) {
        		app.AddTraceProperty("mode", modeAttribute);
        	}
        }
        return app;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
