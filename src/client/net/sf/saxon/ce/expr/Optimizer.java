package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.sort.DocumentSorter;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.TypeHierarchy;

/**
 * This class performs optimizations that vary between different versions of the Saxon product.
 * The optimizer is obtained from the Saxon Configuration. This class is the version used in Saxon-B,
 * which in most cases does no optimization at all: the methods are provided so that they can be
 * overridden in Saxon-EE.
 */
public class Optimizer  {

    public static final int NO_OPTIMIZATION = 0;
    public static final int FULL_OPTIMIZATION = 10;

    protected Configuration config;
    private int optimizationLevel = FULL_OPTIMIZATION;

    /**
     * Create an Optimizer.
     * @param config the Saxon configuration
     */

    public Optimizer(Configuration config) {
        this.config = config;
    }

    /**
     * Get the Saxon configuration object
     * @return the configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the optimization level
     * @param level the optimization level, between 0 (no optimization) and 10 (full optimization).
     * Currently all values greater than zero have the same effect as full optimization
     */

    public void setOptimizationLevel(int level) {
        if (level<NO_OPTIMIZATION || level>FULL_OPTIMIZATION) {
            throw new IllegalArgumentException("Optimization level");
        }
        optimizationLevel = level;
    }

    /**
     * Get the optimization level
     * @return the optimization level, between 0 (no optimization) and 10 (full optimization).
     * Currently all values greater than zero have the same effect as full optimization
     */

    public int getOptimizationLevel() {
        return optimizationLevel;
    }

    /**
     * Simplify a GeneralComparison expression
     * @param gc the GeneralComparison to be simplified
     * @param backwardsCompatible true if in 1.0 compatibility mode
     * @return the simplified expression
     */

    public BinaryExpression simplifyGeneralComparison(GeneralComparison gc, boolean backwardsCompatible) {
        if (backwardsCompatible) {
            Expression[] operands = gc.getOperands();
            GeneralComparison10 gc10 = new GeneralComparison10(operands[0], gc.getOperator(), operands[1]);
            gc10.setAtomicComparer(gc.getAtomicComparer());
            return gc10;
        } else {
            Expression[] operands = gc.getOperands();
            GeneralComparison20 gc20 = new GeneralComparison20(operands[0], gc.getOperator(), operands[1]);
            gc20.setAtomicComparer(gc.getAtomicComparer());
            return gc20;
        }
    }


    /**
     * Convert a path expression such as a/b/c[predicate] into a filter expression
     * of the form (a/b/c)[predicate]. This is possible whenever the predicate is non-positional.
     * The conversion is useful in the case where the path expression appears inside a loop,
     * where the predicate depends on the loop variable but a/b/c does not.
     * @param pathExp the path expression to be converted
     * @param th the type hierarchy cache
     * @return the resulting filterexpression if conversion is possible, or null if not
     */

    public FilterExpression convertToFilterExpression(PathExpression pathExp, TypeHierarchy th)
    throws XPathException {
        return null;
    }

    /**
     * Make a conditional document sorter. This optimization is attempted
     * when a DocumentSorter is wrapped around a path expression
     * @param sorter the document sorter
     * @param path the path expression
     * @return the original sorter unchanged when no optimization is possible, which is always the
     * case in Saxon-B
     */

    public Expression makeConditionalDocumentSorter(DocumentSorter sorter, PathExpression path) {
        return sorter;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

