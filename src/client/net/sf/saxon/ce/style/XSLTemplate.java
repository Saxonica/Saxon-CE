package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.SlotManager;
import client.net.sf.saxon.ce.expr.instruct.Template;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NamespaceException;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.trans.Mode;
import client.net.sf.saxon.ce.trans.RuleManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.DecimalValue;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.logging.client.LogConfiguration;

import java.util.List;

/**
* An xsl:template element in the style sheet.
*/

public final class XSLTemplate extends StyleElement implements StylesheetProcedure {

    private String priorityAtt = null;

    private boolean prepared = false;
    private StructuredQName templateName;
    private StructuredQName[] modeNames;
    private Pattern match;
    private boolean prioritySpecified;
    private double priority;
    private SlotManager stackFrameMap;
    private Template compiledTemplate = new Template();
    private SequenceType requiredType = null;
    private boolean hasRequiredParams = false;
    private boolean ixslPreventDefault = false;
    private String ixslEventProperty = null;

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    protected boolean mayContainParam(String attName) {
        return true;
    }

    /**
     * Specify that xsl:param is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLParam);
    }

    /**
     * Return the name of this template. Note that this may
     * be called before prepareAttributes has been called.
     * @return the name of the template as a Structured QName.
     * Returns null for an unnamed template (unlike getObjectName(),
     * which returns an invented name)
    */

    public StructuredQName getTemplateName() {
        if (templateName == null && !prepared) {
            try {
                prepareAttributes();
            } catch (XPathException err) {
                //
            }
        }
        return templateName;
    }

    /**
     * Determine the type of item returned by this template
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        if (requiredType==null) {
            return getCommonChildItemType();
        } else {
            return requiredType.getPrimaryType();
        }
    }

    
    public void prepareAttributes() throws XPathException {
        if (prepared) {
            return;
        }
        prepared = true;
        templateName = (StructuredQName)checkAttribute("name", "q");
        if (templateName != null) {
            setObjectName(templateName);
        }
        String modeAtt = (String)checkAttribute("mode", "s");
        match = (Pattern)checkAttribute("match", "p");
        priorityAtt = (String)checkAttribute("priority", "w");
        requiredType = (SequenceType)checkAttribute("as", "z");
        checkForUnknownAttributes();

        String a = getAttributeValue(NamespaceConstant.IXSL, "prevent-default");
        ixslPreventDefault = "yes".equals(a);
        ixslEventProperty = getAttributeValue(NamespaceConstant.IXSL, "event-property");

        if (match == null) {
            if (templateName == null) {
                compileError("A template must have a name or match pattern (or both)", "XTSE0500");
            }
            if (modeAtt != null || priorityAtt != null) {
                compileError("A template with no match pattern must have no mode or priority", "XTSE0500");
            }
        }
        try {
            if (modeAtt!=null) {
                // mode is a space-separated list of mode names, or "#default", or "#all"

                List<String> tokens = Whitespace.tokenize(modeAtt);
                int count = 0;
                if (tokens.size()==0) {
                    compileError("The mode attribute must not be empty", "XTSE0550");
                }

                modeNames = new StructuredQName[tokens.size()];
                count = 0;
                boolean allModes = false;
                for (String s : tokens) {
                    StructuredQName mname;
                    if ("#default".equals(s)) {
                        mname = getContainingStylesheet().getDefaultMode();
                        if (mname == null) {
                            mname = Mode.UNNAMED_MODE_NAME;
                        }
                    } else if ("#all".equals(s)) {
                        allModes = true;
                        mname = Mode.ALL_MODES;
                    } else {
                        mname = makeQName(s);
                    }
                    for (int e=0; e < count; e++) {
                        if (modeNames[e].equals(mname)) {
                            compileError("In the list of modes, the value " + s + " is duplicated", "XTSE0550");
                        }
                    }
                    modeNames[count++] = mname;
                }
                if (allModes && (count>1)) {
                    compileError("mode='#all' cannot be combined with other modes", "XTSE0550");
                }
            } else {
                modeNames = new StructuredQName[]{Mode.UNNAMED_MODE_NAME};
            }
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
        } catch (XPathException err) {
            err.maybeSetErrorCode("XTSE0280");
            if (err.getErrorCodeLocalPart().equals("XTSE0020")) {
                err.setErrorCode("XTSE0550");
            }
            err.setIsStaticError(true);
            compileError(err);
        }

        prioritySpecified = (priorityAtt != null);
        if (prioritySpecified) {
            try {
                // it's got to be a valid decimal, but we want it as a double, so parse it twice
                if (!DecimalValue.castableAsDecimal(priorityAtt)) {
                    compileError("Invalid numeric value for priority (" + priority + ')', "XTSE0530");
                }
                priority = Double.parseDouble(priorityAtt);
            } catch (NumberFormatException err) {
                // shouldn't happen
                priority = -1e0;
            }
        }
	}

    public void validate(Declaration decl) throws XPathException {
        stackFrameMap = new SlotManager();
        checkTopLevel(null);

        match = typeCheck("match", match);

        // See if there are any required parameters.
        UnfailingIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo param = (NodeInfo)kids.next();
            if (param == null) {
                break;
            }
            if (param instanceof XSLParam && ((XSLParam)param).isRequiredParam()) {
                hasRequiredParams = true;
                break;
            }
        }

    }


    public void postValidate() throws XPathException {
        markTailCalls();
    }

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
        top.indexNamedTemplate(decl);
    }

    /**
    * Mark tail-recursive calls on templates and functions.
    */

    public boolean markTailCalls() {
        StyleElement last = getLastChildInstruction();
        return last != null && last.markTailCalls();
    }

    /**
    * Compile: creates the executable form of the template
    */

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        Expression block = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
        if (block == null) {
            block = Literal.makeEmptySequence();
        }

        compiledTemplate.setMatchPattern(match);
        compiledTemplate.setBody(block);
        compiledTemplate.setStackFrameMap(stackFrameMap);
        compiledTemplate.setExecutable(getExecutable());
        compiledTemplate.setSourceLocator(this);
        compiledTemplate.setHasRequiredParams(hasRequiredParams);
        compiledTemplate.setRequiredType(requiredType);


        Expression exp = null;
        try {
            exp = makeExpressionVisitor().simplify(block);
        } catch (XPathException e) {
            compileError(e);
        }

        try {
            if (requiredType != null) {
                RoleLocator role =
                        new RoleLocator(RoleLocator.TEMPLATE_RESULT, getDiagnosticId(), 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                role.setErrorCode("XTTE0505");
                exp = TypeChecker.staticTypeCheck(exp, requiredType, false, role, makeExpressionVisitor());
            }
        } catch (XPathException err) {
            compileError(err);
        }

        compiledTemplate.setBody(exp);
        compiledTemplate.setTemplateName(getObjectName());
        
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	exp = makeTraceInstruction(this, exp);
        	if (exp instanceof TraceExpression) {
                ((TraceExpression)exp).setProperty("match", match.toString());
                ((TraceExpression)exp).setProperty("mode", getAttributeValue("", "mode"));
        	}
        	compiledTemplate.setBody(exp);
        }

        return null;
    }

    /**
     * Returns a string that identifies the template for diagnostics
     * @return an identifying string
     */

    public String getDiagnosticId() {
        if (templateName != null) {
            return templateName.getDisplayName();
        } else {
            return match.toString();
        }
    }

    /**
     * Registers the template rule with each Mode that it belongs to.
     * @param declaration Associates this template with a stylesheet module (in principle an xsl:template
     * element can be in a document that is imported more than once; these are separate declarations)
     * @throws XPathException
     */

    public void register(Declaration declaration) throws XPathException {
        if (match != null) {
            StylesheetModule module = declaration.getModule();
            int slots = match.allocateSlots(getStaticContext(), getSlotManager(), 0);
            RuleManager mgr = getPreparedStylesheet().getRuleManager();
            for (StructuredQName nc : modeNames) {
                Mode mode = mgr.getMode(nc, true);
                if (prioritySpecified) {
                    mgr.setTemplateRule(match, compiledTemplate, mode,
                            module, priority, ixslPreventDefault, ixslEventProperty);
                } else {
                    mgr.setTemplateRule(match, compiledTemplate, mode,
                            module, Double.NaN, ixslPreventDefault, ixslEventProperty);
                }
                mode.allocatePatternSlots(slots);
            }

            allocatePatternSlots(slots);
        }
    }


    /**
     * This method is a bit of a misnomer, because it does more than invoke optimization of the template body.
     * In particular, it also registers the template rule with each Mode that it belongs to.
     * @throws XPathException
     * @param declaration Associates this template with a stylesheet module (in principle an xsl:template
     * element can be in a document that is imported more than once; these are separate declarations)
     */

    public void optimize(Declaration declaration) throws XPathException {
        ItemType contextItemType = Type.ITEM_TYPE;
        if (getObjectName() == null) {
            // the template can't be called by name, so the context item must match the match pattern
            contextItemType = match.getNodeTest();
        }

        Expression exp = compiledTemplate.getBody();
        ExpressionVisitor visitor = makeExpressionVisitor();
        try {
            // We've already done the typecheck of each XPath expression, but it's worth doing again at this
            // level because we have more information now.
            Expression exp2 = visitor.typeCheck(exp, contextItemType);
            exp2 = visitor.optimize(exp2, contextItemType);
            if (exp != exp2) {
                compiledTemplate.setBody(exp2);
                exp = exp2;
            }
        } catch (XPathException e) {
            compileError(e);
        }

        allocateSlots(exp);
    }


    /**
    * Get associated Procedure (for details of stack frame)
    */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }



    /**
     * Get the compiled template
     * @return the compiled template
    */

    public Template getCompiledTemplate() {
        return compiledTemplate;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
