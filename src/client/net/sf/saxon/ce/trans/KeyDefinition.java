package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Procedure;
import client.net.sf.saxon.ce.expr.instruct.SlotManager;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;

/**
  * Corresponds to a single xsl:key declaration.<P>
  * @author Michael H. Kay
  */

public class KeyDefinition extends Procedure {

    private Pattern match;                // the match pattern
    private Expression use;
    private BuiltInAtomicType useType;    // the type of the values returned by the atomized use expression
    private StringCollator collation;     // the collating sequence, when type=string
    private String collationName;         // the collation URI
    private boolean backwardsCompatible = false;
    private boolean convertUntypedToOther = false;

    /**
    * Constructor to create a key definition
     * @param match the pattern in the xsl:key match attribute
     * @param use the expression in the xsl:key use attribute, or the expression that results from compiling
     * the xsl:key contained instructions. Note that a KeyDefinition constructed by the XSLT or XQuery parser will
     * always use an Expression here; however, a KeyDefinition constructed at run-time by a compiled stylesheet
     * or XQuery might use a simple ExpressionEvaluator that lacks all the compile-time information associated
     * with an Expression
     * @param collationName the name of the collation being used
     * @param collation the actual collation. This must be one that supports generation of collation keys.
    */

    public KeyDefinition(Pattern match, Expression use, String collationName, StringCollator collation) {
        setHostLanguage(Configuration.XSLT);
        this.match = match;
        this.use = use;
        setBody(use);
        this.collation = collation;
        this.collationName = collationName;
    }

    /**
     * Set the primitive item type of the values returned by the use expression
     * @param itemType the primitive type of the indexed values
     */

    public void setIndexedItemType(BuiltInAtomicType itemType) {
        useType = itemType;
    }

    /**
     * Get the primitive item type of the values returned by the use expression
     * @return the primitive item type of the indexed values
     */

    public BuiltInAtomicType getIndexedItemType() {
        if (useType == null) {
            return BuiltInAtomicType.ANY_ATOMIC;
        } else {
            return useType;
        }
    }

    /**
     * Set backwards compatibility mode. The key definition is backwards compatible if ANY of the xsl:key
     * declarations has version="1.0" in scope.
     * @param bc set to true if running in XSLT 2.0 backwards compatibility mode
     */

    public void setBackwardsCompatible(boolean bc) {
        backwardsCompatible = bc;
    }

    /**
     * Test backwards compatibility mode
     * @return true if running in XSLT backwards compatibility mode
     */

    public boolean isBackwardsCompatible() {
        return backwardsCompatible;
    }


    /**
     * Indicate that untypedAtomic values should be converted to the type of the other operand,
     * rather than to strings. This is used for indexes constructed internally by Saxon-EE to
     * support filter expressions that use the "=" operator, as distinct from "eq".
     * @param convertToOther true if comparisons follow the semantics of the "=" operator rather than
     * the "eq" operator
     */

    public void setConvertUntypedToOther(boolean convertToOther) {
        convertUntypedToOther = convertToOther;
    }

    /**
     * Determine whether untypedAtomic values are converted to the type of the other operand.
     * @return true if comparisons follow the semantics of the "=" operator rather than
     * the "eq" operator
     */

    public boolean isConvertUntypedToOther() {
        return convertUntypedToOther;
    }

    /**
     * Set the map of local variables needed while evaluating the "use" expression
     */

    public void setStackFrameMap(SlotManager map) {
        if (map != null && map.getNumberOfVariables() > 0) {
            super.setStackFrameMap(map);
        }
    }

    /**
    * Get the match pattern for the key definition
     * @return the pattern specified in the "match" attribute of the xsl:key declaration
    */

    public Pattern getMatch() {
        return match;
    }

    /**
     * Set the body of the key (the use expression). This is held redundantly as an Expression and
     * as a SequenceIterable (not sure why!)
     * @param body the use expression of the key
     */

    public void setBody(Expression body) {
        super.setBody(body);
        use = body;
    }

    /**
    * Get the use expression for the key definition
     * @return the expression specified in the "use" attribute of the xsl:key declaration
    */

    public Expression getUse() {
        return use;
    }

    /**
    * Get the collation name for this key definition.
    * @return the collation name (the collation URI)
    */

    public String getCollationName() {
        return collationName;
    }

    /**
    * Get the collation.
     * @return the collation
    */

    public StringCollator getCollation() {
        return collation;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     *
     */

    public StructuredQName getObjectName() {
        return null; 
    }

	@Override
	public int getConstructType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getSystemId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLineNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getProperty(String name) {
		// TODO Auto-generated method stub
		return null;
	}
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.