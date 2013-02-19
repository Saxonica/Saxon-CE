package client.net.sf.saxon.ce.trace;

/**
 * This class holds constants identifying different kinds of location in a source stylesheet or query.
 * These constants are used in the getConstructType() method of class InstructionInfo. Some of these
 * locations represent points where the dynamic context changes, and they are therefore recorded as
 * such on the context stack. Some of the locations represent points in the evaluation of a stylesheet
 * (or query or XPath expression) that are notified to the trace listener. Some fulfil both roles.
 *
 * <p>Any constant used in {@link net.sf.saxon.om.StandardNames} can be used as a Location. Such
 * names are generally used to identify XSLT instructions. They are also used for equivalent constructs
 * in XQuery, for example XSL_ELEMENT is used for a computed element constructor in XQuery. The constants
 * in StandardNames are all in the range 0-1023, so constants defined in this class are outside this
 * range.</p>
 *
 * <p>The constants in this file are annotated with Q to indicate they can appear in XQuery trace output,
 * T to indicate they can appear in XSLT trace output, and/or C to indicate that they can appear on the
 * dynamic context stack.</p>
 */
public class Location {

    /**
     * The outer system environment, identified as the caller of a user query or stylesheet.
     * Usage:C
     */
    public static final int CONTROLLER = 2000;


    /**
     * An XSLT instruction. The name of the instruction (which may be an extension instruction) can
     * be obtained using the fingerprint property. Usage:T
     */
    public static final int EXTENSION_INSTRUCTION = 2005;

    /**
     * An XSLT literal result element, or an XQuery direct element constructor. Usage:QT
     */
    public static final int LITERAL_RESULT_ELEMENT = 2006;

    /**
     * An attribute of an XSLT literal result element or of an XQuery direct element constructor.
     * Usage: QT
     */
    public static final int LITERAL_RESULT_ATTRIBUTE = 2007;

   /**
     * An XSLT user-written template rule or named template. Usage: TC
     */
    public static final int TEMPLATE = 2008;

    /**
     * An XPath function call to a user-defined function.
     * The "expression" property references the actual expression, of class ComputedExpression.
     * The "target" property references the function being called, of class UserFunction.
     * Usage: QTC
     */
    public static final int FUNCTION_CALL = 2009;

    /**
     * An XSLT built-in template rule. Usage: TC
     */
    public static final int BUILT_IN_TEMPLATE = 2010;

    /**
     * Entry point to a top-level XPath expression within an XSLT stylesheet.
     * Usage: TC
     */

    public static final int XPATH_IN_XSLT = 2011;

    /**
     * An XPath or XQuery "for" clause. Usage: Q
     */

    public static final int FOR_EXPRESSION = 2012;

    /**
     * An XQuery "let" clause, or an XSLT local variable (which compiles into a LET clause).
     * Usage: Q,T
     */

    public static final int LET_EXPRESSION = 2013;

    /**
     * An XPath or XQuery "return" clause. Usage: Q
     */

    public static final int RETURN_EXPRESSION = 2014;


    /**
     * An XPath or XQuery "if" expression. Usage: Q
     */

    public static final int IF_EXPRESSION = 2015;

    /**
     * An XPath or XQuery "then" clause. Usage: Q
     */

    public static final int THEN_EXPRESSION = 2016;

    /**
     * An XPath or XQuery "else" clause. Usage: Q
     */

    public static final int ELSE_EXPRESSION = 2017;

    /**
     * A WHERE clause in a FLWOR expression. Usage: Q
     */

    public static final int WHERE_CLAUSE = 2018;

    /**
     * An order-by clause in a FLWOR expression. Usage: Q
     */

    public static final int ORDER_BY_CLAUSE = 2019;

    /**
     * An XPath or XQuery "typeswitch" expression. Usage: Q
     */

    public static final int TYPESWITCH_EXPRESSION = 2020;

    /**
     * CASE clause within "typeswitch". Usage: Q
     */

    public static final int CASE_EXPRESSION = 2021;

    /**
     * DEFAULT clause within "typeswitch". Usage: Q
     */

    public static final int DEFAULT_EXPRESSION = 2022;

    /**
     * An XPath or XQuery "validate" expression. Usage: Q
     */

    public static final int VALIDATE_EXPRESSION = 2023;

     /**
     * An XPath or XQuery path expression. Usage: C
     */

    public static final int PATH_EXPRESSION = 2025;

    /**
     * An XPath or XQuery "switch" expression. Usage: Q
     */

    public static final int SWITCH_EXPRESSION = 2026;

    /**
     * An XQuery Update copy-modify (transform) expression. Usage: Q
     */

    public static final int COPY_MODIFY_EXPRESSION = 2027;

    /**
     * An XQuery Update insert expression. Usage: Q
     */

    public static final int INSERT_EXPRESSION = 2028;

    /**
     * An XPath or XQuery replace expression. Usage: Q
     */

    public static final int REPLACE_EXPRESSION = 2029;

    /**
     * An XPath or XQuery delete expression. Usage: Q
     */

    public static final int DELETE_EXPRESSION = 2030;

   /**
     * An XPath or XQuery rename expression. Usage: Q
     */

    public static final int RENAME_EXPRESSION = 2031;


    /**
     * An explicit call of the fn:trace() function. Usage: QT
     */

    public static final int TRACE_CALL = 2041;

    /**
     * An XPath expression constructed dynamically using saxon:evaluate (or saxon:expression).
     * Usage: QTC
     */

    public static final int SAXON_EVALUATE = 2051;

    /**
     * Lazy evaluation of an expression (this code is used to identify a context created as a saved
     * copy of an existing context, to be stored in a Closure). Usage: C
     */

    public static final int LAZY_EVALUATION = 2063;

    /**
     * A function declaration in XSLT or XQuery
     */

    public static final int FUNCTION = 2065;

    /**
     * XPath expression, otherwise unclassified. The "expression" property references the actual expression,
     * of class ComputedExpression. Used in fallback cases only.
     */
    public static final int XPATH_EXPRESSION = 2098;

    /**
     * Unclassified location. Used in fallback cases only.
     */
    public static final int UNCLASSIFIED = 2099;

    /**
     * Values of the form CLAUSE_BASE + n represent FLWOR clauses
     */
    public static final int CLAUSE_BASE = 3000;

    private Location() {
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.