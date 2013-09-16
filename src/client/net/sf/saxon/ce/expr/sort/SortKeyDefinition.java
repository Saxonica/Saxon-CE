package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.HashMap;

/**
* A SortKeyDefinition defines one component of a sort key. <BR>
*
* Note that most attributes defining the sort key can be attribute value templates,
* and can therefore vary from one invocation to another. We hold them as expressions. As
* soon as they are all known (which in general is only at run-time), the SortKeyDefinition
* is replaced by a FixedSortKeyDefinition in which all these values are fixed.
*/

// TODO: optimise also for the case where the attributes depend only on global variables
// or parameters, in which case the same AtomicComparer can be used for the duration of a
// transformation.

// TODO: at present the SortKeyDefinition is evaluated to obtain a AtomicComparer, which can
// be used to compare two sort keys. It would be more efficient to use a Collator to
// obtain collation keys for all the items to be sorted, as these can be compared more
// efficiently.


public class SortKeyDefinition {

    private static StringLiteral defaultOrder = new StringLiteral("ascending");
    private static StringLiteral defaultCaseOrder = new StringLiteral("#default");
    private static StringLiteral defaultLanguage = new StringLiteral(StringValue.EMPTY_STRING);

    protected Expression sortKey;

    public static final int ORDER = 0;
    public static final int DATA_TYPE = 1;
    public static final int CASE_ORDER = 2;
    public static final int LANG = 3;
    public static final int COLLATION = 4;
    public static final int STABLE = 5;
    public static final int N = 6;

    protected Expression[] sortProperties = new Expression[N];
    protected StringCollator collation;
    protected String baseURI;           // needed in case collation URI is relative
    protected boolean backwardsCompatible = false;

    private AtomicComparer finalComparator = null;
    // Note, the "collation" defines the collating sequence for the sort key. The
    // "finalComparator" is what is actually used to do comparisons, after taking into account
    // ascending/descending, caseOrder, etc.

    public SortKeyDefinition() {
        sortProperties[ORDER] = defaultOrder;
        sortProperties[CASE_ORDER] = defaultCaseOrder;
        sortProperties[LANG] = defaultLanguage;
    }

    /**
     * Set the expression used as the sort key
     * @param exp the sort key select expression
    */

    public void setSortKey(Expression exp) {
        sortKey = exp;
    }

    /**
     * Get the expression used as the sort key
     * @return the sort key select expression
    */

    public Expression getSortKey() {
        return sortKey;
    }

    public void setSortProperty(int property, Expression value) {
        sortProperties[property] = value;
    }

    public Expression getSortProperty(int property) {
        return sortProperties[property];
    }

    /**
     * Set the collation to be used
     * @param collation A StringCollator, which encapsulates both the collation URI and the collating function
     */

    public void setCollation(StringCollator collation) {
        this.collation = collation;
    }

    /**
     * Get the collation to be used
     * @return A StringCollator, which encapsulates both the collation URI and the collating function
     */

    public StringCollator getCollation() {
        return collation;
    }

    /**
     * Set the base URI of the expression. This is needed to handle the case where a collation URI
     * evaluated at run-time turns out to be a relative URI.
     * @param baseURI the static base URI of the expression
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
     * Get the static base URI of the expression. This is needed to handle the case where a collation URI
     * evaluated at run-time turns out to be a relative URI.
     * @return the static base URI of the expression
     */

    public String getBaseURI() {
        return baseURI;
    }

    /**
     * Set whether this sort key is evaluated in XSLT 1.0 backwards compatibility mode
     * @param compatible true if backwards compatibility mode is selected
     */

    public void setBackwardsCompatible(boolean compatible) {
        backwardsCompatible = compatible;
    }

    /**
     * Ask whether this sort key is evaluated in XSLT 1.0 backwards compatibility mode
     * @return true if backwards compatibility mode was selected
     */

    public boolean isBackwardsCompatible() {
        return backwardsCompatible;
    }

     /**
     * Ask whether the sort key definition is fixed, that is, whether all the information needed
     * to create a Comparator is known statically
     * @return true if all information needed to create a Comparator is known statically
     */

    public boolean isFixed() {
        for (int i=0; i<N; i++) {
            if (sortProperties[i] != null && !(sortProperties[i] instanceof Literal)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Simplify this sort key definition
     * @param visitor the expression visitor
     * @return the simplified sort key definition
     * @throws XPathException if any failure occurs
     */

    public SortKeyDefinition simplify(ExpressionVisitor visitor) throws XPathException {
        sortKey = visitor.simplify(sortKey);
        for (int i=0; i<N; i++) {
            sortProperties[i] = visitor.simplify(sortProperties[i]);
        }
        return this;
    }


    /**
     * Type-check this sort key definition (all properties other than the sort key
     * select expression, which has a different dynamic context)
     * @param visitor the expression visitor
     * @param contextItemType the type of the context item
     * @throws XPathException if any failure occurs
     */

    public void typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        for (int i=0; i<N; i++) {
            sortProperties[i] = visitor.typeCheck(sortProperties[i], contextItemType);
        }

        Expression language = sortProperties[LANG];
        if (language instanceof StringLiteral && ((StringLiteral)language).getStringValue().length() != 0) {
            if (!StringValue.isValidLanguageCode(((StringLiteral)language).getStringValue())) {
                throw new XPathException("The lang attribute of xsl:sort must be a valid language code", "XTDE0030");
            }
        }
    }    

    /**
     * Allocate an AtomicComparer to perform the comparisons described by this sort key component. This method
     * is called at run-time. The AtomicComparer takes into account not only the collation, but also parameters
     * such as order=descending and handling of empty sequence and NaN (the result of the compare()
     * method of the comparator is +1 if the second item is to sort after the first item)
     * @param context the dynamic evaluation context
     * @return an AtomicComparer suitable for making the sort comparisons
    */

    public AtomicComparer makeComparator(XPathContext context) throws XPathException {

        String orderX = sortProperties[ORDER].evaluateAsString(context).toString();

        final Configuration config = context.getConfiguration();
        final TypeHierarchy th = TypeHierarchy.getInstance();

        AtomicComparer atomicComparer;
        StringCollator stringCollator;
        if (collation != null) {
            stringCollator = collation;
        } else if (sortProperties[COLLATION] != null) {
            String cname = sortProperties[COLLATION].evaluateAsString(context).toString();
            URI collationURI;
            try {
                collationURI = new URI(cname, true);
                if (!collationURI.isAbsolute()) {
                    if (baseURI == null) {
                        throw new XPathException("Collation URI is relative, and base URI is unknown");
                    } else {
                        URI base = new URI(baseURI);
                        collationURI = base.resolve(collationURI.toString());
                    }
                }
            } catch (URI.URISyntaxException err) {
                throw new XPathException("Collation name " + cname + " is not a valid URI: " + err);
            }
            stringCollator = context.getConfiguration().getNamedCollation(collationURI.toString());
            if (stringCollator == null) {
                throw new XPathException("Unknown collation " + collationURI.toString(), "XTDE1035");
            }
        } else {
            String caseOrderX = sortProperties[CASE_ORDER].evaluateAsString(context).toString();
            String languageX = sortProperties[LANG].evaluateAsString(context).toString();
            HashMap props = new HashMap();
            if (languageX.length() != 0 && !(sortProperties[LANG] instanceof StringLiteral)) {
                if (!StringValue.isValidLanguageCode(((StringLiteral)sortProperties[LANG]).getStringValue())) {
                    throw new XPathException("The lang attribute of xsl:sort must be a valid language code", "XTDE0030");
                }
                props.put("lang", languageX);
            }
            if (!caseOrderX.equals("#default")) {
                props.put("case-order", caseOrderX);
            }
            //stringCollator = Configuration.getPlatform().makeCollation(config, props, "");
            stringCollator = null;
        }



        if (sortProperties[DATA_TYPE]==null) {
            atomicComparer = AtomicSortComparer.makeSortComparer(stringCollator,
                sortKey.getItemType().getAtomizedItemType(), context.getImplicitTimezone());
        } else {
            String dataType = sortProperties[DATA_TYPE].evaluateAsString(context).toString();
            if (dataType.equals("text")) {
                atomicComparer = AtomicSortComparer.makeSortComparer(stringCollator,
                    AtomicType.STRING, context.getImplicitTimezone());
                atomicComparer = new TextComparer(atomicComparer);
            } else if (dataType.equals("number")) {
                atomicComparer = NumericComparer.getInstance();
            } else {
                XPathException err = new XPathException("data-type on xsl:sort must be 'text' or 'number'");
                err.setErrorCode("XTDE0030");
                throw err;
            }
        }

        if (sortProperties[STABLE] != null) {
            StringValue stableVal = (StringValue)sortProperties[STABLE].evaluateItem(context);
            String s = Whitespace.trim(stableVal.getStringValue());
            if (s.equals("yes") || s.equals("no")) {
                // no action
            } else {
                XPathException err = new XPathException("Value of 'stable' on xsl:sort must be 'yes' or 'no'");
                err.setErrorCode("XTDE0030");
                throw err;
            }
        }

        if (orderX.equals("ascending")) {
            return atomicComparer;
        } else if (orderX.equals("descending")) {
            return new DescendingComparer(atomicComparer);
        } else {
            XPathException err1 = new XPathException("order must be 'ascending' or 'descending'");
            err1.setErrorCode("XTDE0030");
            throw err1;
        }
    }

    /**
     * Set the comparator which is used to compare two values according to this sort key. The comparator makes the final
     * decision whether one value sorts before or after another: this takes into account the data type, the collation,
     * whether empty comes first or last, whether the sort order is ascending or descending.
     *
     * <p>This method is called at compile time if all these factors are known at compile time.
     * It must not be called at run-time, except to reconstitute a finalComparator that has been
     * lost by virtue of serialization .</p>
     * @param comp the Atomic Comparer to be used
    */

    public void setFinalComparator(AtomicComparer comp) {
        finalComparator = comp;
    }

    /**
     * Get the comparator which is used to compare two values according to this sort key. This method
     * may be called either at compile time or at run-time. If no comparator has been allocated,
     * it returns null. It is then necessary to allocate a comparator using the {@link #makeComparator}
     * method.
     * @return the Atomic Comparer to be used
    */

    public AtomicComparer getFinalComparator() {
        return finalComparator;
    }



}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
