package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.ItemType;

import java.util.Iterator;
import java.util.Stack;

/**
 *  The ExpressionVisitor supports the various phases of processing of an expression tree which require
 *  a recursive walk of the tree structure visiting each node in turn. In maintains a stack holding the
 *  ancestor nodes of the node currently being visited.
 */

public class ExpressionVisitor {

    private Stack<Expression> stack;
    private Executable executable;
    private StaticContext staticContext;
    private Configuration configuration;

    /**
     * Create an ExpressionVisitor
     */

    public ExpressionVisitor() {
        stack = new Stack<Expression>();
    }
    
    private SourceLocator getLastLocator() {
    	int stackSize = stack.size();
    	Expression[] expr = new Expression[stackSize];
    	stack.toArray(expr);
    	SourceLocator result = null;
    	for (int i = stackSize -1; i > -1; i--) {
    		if (expr[i].sourceLocator != null) {
    			return expr[i].sourceLocator;
    		}
    	}
    	return result;
    }
    
    public String getLocation() {
    	SourceLocator sl = getLastLocator();
    	String message = "";
    	if (sl != null) {
    		message = sl.getLocation();
    		int pos = message.indexOf(" in ");
    		if (pos > -1) {
    			message = message.substring(0, pos);
    		}
    	}
    	
    	return message;
    }

    /**
     * Get the Saxon configuration
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Set the Saxon configuration
     * @param configuration the Saxon configuration
     */


    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Get the Executable containing the expressions being visited
     * @return the Executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the Executable containing the expressions being visited
     * @param executable the Executable
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Get the static context for the expressions being visited. Note: this may not reflect all changes
     * in static context (e.g. namespace context, base URI) applying to nested expressions
     * @return the static context
     */

    public StaticContext getStaticContext() {
        return staticContext;
    }

    /**
     * Set the static context for the expressions being visited. Note: this may not reflect all changes
     * in static context (e.g. namespace context, base URI) applying to nested expressions
     * @param staticContext the static context
     */

    public void setStaticContext(StaticContext staticContext) {
        this.staticContext = staticContext;
    }

    /**
     * Factory method: make an expression visitor
     * @param env the static context
     * @return the new expression visitor
     */

    public static ExpressionVisitor make(StaticContext env, Executable exec) {
        ExpressionVisitor visitor = new ExpressionVisitor();
        visitor.setStaticContext(env);
        visitor.setExecutable(exec);
        visitor.setConfiguration(env.getConfiguration());
        return visitor;
    }

    /**
     * Simplify an expression, via the ExpressionVisitor
     * @param exp the expression to be simplified
     * @return the simplified expression
     * @throws XPathException
     */

    public Expression simplify(Expression exp) throws XPathException {
        if (exp != null) {
            stack.push(exp);
            Expression exp2 = exp.simplify(this);
            if (exp2 != exp) {
                ExpressionTool.copyLocationInfo(exp, exp2);
            }
            stack.pop();
            return exp2;
        } else {
            return null;
        }
    }

    /**
     * Type check an expression, via the ExpressionVisitor
     * @param exp the expression to be typechecked
     * @param contextItemType the static type of the context item for this expression
     * @return the expression that results from type checking (this may be wrapped in expressions that
     * perform dynamic checking of the item type or cardinality, or that perform atomization or numeric
     * promotion)
     * @throws XPathException if static type checking fails, that is, if the expression cannot possibly
     * deliver a value of the required type
     */

    public Expression typeCheck(Expression exp, ItemType contextItemType) throws XPathException {
        if (exp != null) {
            stack.push(exp);
            Expression exp2 = exp.typeCheck(this, contextItemType);
            if (exp2 != exp) {
                ExpressionTool.copyLocationInfo(exp, exp2);
            }
            stack.pop();
            return exp2;
        } else {
            return null;
        }
    }

   /**
     * Optimize an expression, via the ExpressionVisitor
     * @param exp the expression to be typechecked
     * @param contextItemType the static type of the context item for this expression
     * @return the rewritten expression
     * @throws XPathException
     */

    public Expression optimize(Expression exp, ItemType contextItemType) throws XPathException {
        if (exp != null) {
            stack.push(exp);
            Expression exp2 = exp.optimize(this, contextItemType);
            if (exp2 != exp) {
                ExpressionTool.copyLocationInfo(exp, exp2);
            }
            stack.pop();
            return exp2;
        } else {
            return null;
        }
    }

    /**
     * Return true if the current expression at the top of the visitor's stack is evaluated repeatedly
     * when a given ancestor expression is evaluated once
     * @param ancestor the ancestor expression. May be null, in which case the search goes all the way
     * to the base of the stack.
     * @return true if the current expression is evaluated repeatedly
     */

    public boolean isLoopingSubexpression(Expression ancestor) {
        int top = stack.size()-1;
        while (true) {
            if (top <= 0) {
                return false;
            }
            Expression parent = stack.get(top - 1);
            if (parent.hasLoopingSubexpression((stack.get(top)))) {
                return true;
            }
            if (parent == ancestor) {
                return false;
            }
            top--;
        }
    }

   /**
     * Reset the static properties for the current expression and for all its containing expressions.
     * This should be done whenever the expression is changed in a way that might
     * affect the properties. It causes the properties to be recomputed next time they are needed.
     */

    public final void resetStaticProperties() {
       Iterator<Expression> up = stack.iterator();
       while (up.hasNext()) {
           Expression exp = up.next();
           exp.resetLocalStaticProperties();
       }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
