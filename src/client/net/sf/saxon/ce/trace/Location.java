package client.net.sf.saxon.ce.trace;

import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;

/**
 * This class holds constants identifying different kinds of location in a source stylesheet or query.
 * These constants are used in the getConstructType() method of class InstructionInfo. Some of these
 * locations represent points where the dynamic context changes, and they are therefore recorded as
 * such on the context stack. Some of the locations represent points in the evaluation of a stylesheet
 * (or query or XPath expression) that are notified to the trace listener. Some fulfil both roles.
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
    public static final StructuredQName LITERAL_RESULT_ELEMENT =
            new StructuredQName("xsl", NamespaceConstant.XSLT, "LiteralResultElement");

    /**
     * An attribute of an XSLT literal result element or of an XQuery direct element constructor.
     * Usage: QT
     */
    public static final StructuredQName LITERAL_RESULT_ATTRIBUTE =
            new StructuredQName("xsl", NamespaceConstant.XSLT, "LiteralResultAttribute");;

   /**
     * An XSLT user-written template rule or named template. Usage: TC
     */
    public static final StructuredQName TEMPLATE =
           new StructuredQName("xsl", NamespaceConstant.XSLT, "template");


    /**
     * An XQuery "let" clause, or an XSLT local variable (which compiles into a LET clause).
     * Usage: Q,T
     */

    public static final StructuredQName LET_EXPRESSION =
            new StructuredQName("", NamespaceConstant.SAXON, "let-expression");



    /**
     * An explicit call of the fn:trace() function. Usage: QT
     */

    public static final StructuredQName TRACE_CALL = new StructuredQName("fn", NamespaceConstant.FN, "trace");

    /**
     * An XPath expression constructed dynamically using saxon:evaluate (or saxon:expression).
     * Usage: QTC
     */


    /**
     * A function declaration in XSLT or XQuery
     */

    public static final StructuredQName FUNCTION = new StructuredQName("xsl", NamespaceConstant.XSLT, "function");

    /**
     * XPath expression, otherwise unclassified. The "expression" property references the actual expression,
     * of class ComputedExpression. Used in fallback cases only.
     */
    public static final StructuredQName XPATH_EXPRESSION =
            new StructuredQName("", NamespaceConstant.SAXON, "xpath-expression");



    private Location() {
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.