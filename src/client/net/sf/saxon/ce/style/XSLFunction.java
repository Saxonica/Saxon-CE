package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.SlotManager;
import client.net.sf.saxon.ce.expr.instruct.UserFunction;
import client.net.sf.saxon.ce.expr.instruct.UserFunctionParameter;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.logging.client.LogConfiguration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Handler for xsl:function elements in stylesheet (XSLT 2.0). <BR>
* Attributes: <br>
* name gives the name of the function
*/

public class XSLFunction extends StyleElement implements StylesheetProcedure {

    private String nameAtt = null;
    private String asAtt = null;
    private String overrideAtt = null;
    private SequenceType resultType;
    private String functionName;
    private SlotManager stackFrameMap;
    private boolean override = true;
    private int numberOfArguments = -1;  // -1 means not yet known
    private UserFunction compiledFunction;

    // List of UserFunctionCall objects that reference this XSLFunction
    List references = new ArrayList(10);

    /**
     * Method called by UserFunctionCall to register the function call for
     * subsequent fixup.
     * @param ref the UserFunctionCall to be registered
    */

    public void registerReference(UserFunctionCall ref) {
        references.add(ref);
    }

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();
        overrideAtt = "yes";
    	for (int a=0; a<atts.getLength(); a++) {
            StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
            if (f.equals(StandardNames.NAME)) {
				nameAtt = Whitespace.trim(atts.getValue(a));
				if (nameAtt.indexOf(':')<0) {
					compileError("Function name must have a namespace prefix", "XTSE0740");
				}
				try {
				    setObjectName(makeQName(nameAtt));
        		} catch (NamespaceException err) {
        		    compileError(err.getMessage(), "XTSE0280");
        		} catch (XPathException err) {
                    compileError(err);
                }
        	} else if (f.equals(StandardNames.AS)) {
        		asAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.OVERRIDE)) {
                overrideAtt = Whitespace.trim(atts.getValue(a));
                if (overrideAtt.equals("yes")) {
                    override = true;
                } else if (overrideAtt.equals("no")) {
                    override = false;
                } else {
                    override = true;
                    compileError("override must be 'yes' or 'no'", "XTSE0020");
                }
        	} else {
        		checkUnknownAttribute(qn);
        	}
        }

        if (nameAtt == null) {
            reportAbsence("name");
            nameAtt="xsl:unnamed-function";
        }

        if (asAtt == null) {
            resultType = SequenceType.ANY_SEQUENCE;
        } else {
            resultType = makeSequenceType(asAtt);
        }

        functionName = nameAtt;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be -1.
     */

    public StructuredQName getObjectName() {
        StructuredQName qn = super.getObjectName();
        if (qn == null) {
            nameAtt = Whitespace.trim(getAttributeValue("", "name"));
            if (nameAtt == null) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-function");
            }
            try {
                qn = makeQName(nameAtt);
                setObjectName(qn);
            } catch (NamespaceException err) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-function");
            } catch (XPathException err) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-function");
            }
        }
        return qn;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body.
    * @return true: yes, it may contain a general template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    protected boolean mayContainParam(String attName) {
        return !"required".equals(attName);
    }

    /**
     * Specify that xsl:param is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLParam);
    }
    /**
    * Is override="yes"?.
    * @return true if override="yes" was specified, otherwise false
    */

    public boolean isOverriding() {
        if (overrideAtt == null) {
            // this is a forwards reference
            try {
                prepareAttributes();
            } catch (XPathException e) {
                // no action: error will be caught later
            }
        }
        return override;
    }

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
        top.indexFunction(decl);
    }

    /**
    * Notify all references to this function of the data type.
     * @throws XPathException
    */

    public void fixupReferences() throws XPathException {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            ((UserFunctionCall)iter.next()).setStaticType(resultType);
        }
        super.fixupReferences();
    }

    public void validate(Declaration decl) throws XPathException {

        stackFrameMap = new SlotManager();

        // check the element is at the top level of the stylesheet

        checkTopLevel(null);
        getNumberOfArguments();

    }


    /**
     * Compile the function definition to create an executable representation
     * @return an Instruction, or null. The instruction returned is actually
     * rather irrelevant; the compile() method has the side-effect of binding
     * all references to the function to the executable representation
     * (a UserFunction object)
     * @throws XPathException
     */

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        compileAsExpression(exec, decl);
        return null;
    }

    /**
     * Compile the function into a UserFunction object, which treats the function
     * body as a single XPath expression. This involves recursively translating
     * xsl:variable declarations into let expressions, withe the action part of the
     * let expression containing the rest of the function body.
     * The UserFunction that is created will be linked from all calls to
     * this function, so nothing else needs to be done with the result. If there are
     * no calls to it, the compiled function will be garbage-collected away.
     * @param exec the Executable
     * @param decl
     * @throws XPathException
     */

    private void compileAsExpression(Executable exec, Declaration decl) throws XPathException {
        Expression exp = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
        if (exp == null) {
            exp = Literal.makeEmptySequence();
        } else {
            ExpressionVisitor visitor = makeExpressionVisitor();
            exp = exp.simplify(visitor);
        }
        
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) { 
            TraceExpression trace = new TraceExpression(exp);
            trace.setConstructType(getNodeName());
            trace.setObjectName(getObjectName());
            exp = trace;        	
        }

        UserFunction fn = new UserFunction();
        fn.setHostLanguage(Configuration.XSLT);
        fn.setBody(exp);
        fn.setFunctionName(getObjectName());
        setParameterDefinitions(fn);
        fn.setResultType(getResultType());
        fn.setSourceLocator(this);
        fn.setStackFrameMap(stackFrameMap);
        fn.setExecutable(exec);
        compiledFunction = fn;
        fixupInstruction(fn);

    }

    public void typeCheckBody() throws XPathException {
        Expression exp = compiledFunction.getBody();
        Expression exp2 = exp;
        ExpressionVisitor visitor = makeExpressionVisitor();
        try {
            // We've already done the typecheck of each XPath expression, but it's worth doing again at this
            // level because we have more information now.

            exp2 = visitor.typeCheck(exp, null);
            if (resultType != null) {
                RoleLocator role =
                        new RoleLocator(RoleLocator.FUNCTION_RESULT, functionName, 0);
                role.setErrorCode("XTTE0780");
                exp2 = TypeChecker.staticTypeCheck(exp2, resultType, false, role, visitor);
            }
        } catch (XPathException err) {
            err.maybeSetLocation(this);
            compileError(err);
        }
        if (exp2 != exp) {
            compiledFunction.setBody(exp2);
        }
    }

    public void optimize(Declaration declaration) throws XPathException {
        Expression exp = compiledFunction.getBody();
        ExpressionVisitor visitor = makeExpressionVisitor();
        Expression exp2 = exp;
        Optimizer opt = getConfiguration().getOptimizer();
        try {
            if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
                exp2 = exp.optimize(visitor, null);
            }

        } catch (XPathException err) {
            err.maybeSetLocation(this);
            compileError(err);
        }

        allocateSlots(exp2);
        if (exp2 != exp) {
            compiledFunction.setBody(exp2);
        }

        int tailCalls = ExpressionTool.markTailFunctionCalls(exp2, getObjectName(), getNumberOfArguments());
        if (tailCalls != 0) {
            compiledFunction.setTailRecursive(tailCalls > 0, tailCalls > 1);
            compiledFunction.setBody(new TailCallLoop(compiledFunction));
        }

        compiledFunction.computeEvaluationMode();

    }

    /**
    * Fixup all function references.
     * @param compiledFunction the Instruction representing this function in the compiled code
     * @throws XPathException if an error occurs.
    */

    private void fixupInstruction(UserFunction compiledFunction)
    throws XPathException {
        ExpressionVisitor visitor = makeExpressionVisitor();
        try {
            Iterator iter = references.iterator();
            while (iter.hasNext()) {
                UserFunctionCall call = ((UserFunctionCall)iter.next());
                call.setFunction(compiledFunction);
                call.checkFunctionCall(compiledFunction, visitor);
                call.computeArgumentEvaluationModes();
            }
        } catch (XPathException err) {
            compileError(err);
        }
    }

    /**
     * Get associated Procedure (for details of stack frame).
     * @return the associated Procedure object
     */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }

    /**
     * Get the type of value returned by this function
     * @return the declared result type, or the inferred result type
     * if this is more precise
     */
    public SequenceType getResultType() {
        return resultType;
    }

    /**
     * Get the number of arguments declared by this function (that is, its arity).
     * @return the arity of the function
     */

    public int getNumberOfArguments() {
        if (numberOfArguments == -1) {
            numberOfArguments = 0;
            AxisIterator kids = iterateAxis(Axis.CHILD);
            while (true) {
                Item child = kids.next();
                if (child instanceof XSLParam) {
                    numberOfArguments++;
                } else {
                    return numberOfArguments;
                }
            }
        }
        return numberOfArguments;
    }

    /**
     * Set the definitions of the parameters in the compiled function, as an array.
     * @param fn the compiled object representing the user-written function
     */

    public void setParameterDefinitions(UserFunction fn) {
        UserFunctionParameter[] params = new UserFunctionParameter[getNumberOfArguments()];
        fn.setParameterDefinitions(params);
        int count = 0;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo node = (NodeInfo)kids.next();
            if (node == null) {
                return;
            }
            if (node instanceof XSLParam) {
                UserFunctionParameter param = new UserFunctionParameter();
                params[count++] = param;
                param.setRequiredType(((XSLParam)node).getRequiredType());
                param.setVariableQName(((XSLParam)node).getVariableQName());
                param.setSlotNumber(((XSLParam)node).getSlotNumber());
                ((XSLParam)node).fixupBinding(param);
                int refs = ExpressionTool.getReferenceCount(fn.getBody(), param, false);
                param.setReferenceCount(refs);
            }
        }
    }

    /**
     * Get the compiled function
     * @return the object representing the compiled user-written function
     */

    public UserFunction getCompiledFunction() {
        return compiledFunction;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
