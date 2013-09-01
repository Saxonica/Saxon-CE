package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.expr.instruct.Template;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.Rule;
import client.net.sf.saxon.ce.trans.StripSpaceRules;
import client.net.sf.saxon.ce.trans.XPathException;

/**
  * The RuleBasedStripper class performs whitespace stripping according to the rules of
  * the xsl:strip-space and xsl:preserve-space instructions.
  * It maintains details of which elements need to be stripped.
  * The code is written to act as a SAX-like filter to do the stripping.
  * @author Michael H. Kay
  */


public class RuleBasedStripper extends Stripper {

//    public static class StripRuleTarget implements RuleTarget {
//        public void explain(ExpressionPresenter presenter) {
//            // no-op
//        }
//    }
    public final static Template STRIP = new Template(){};
    public final static Template PRESERVE = new Template();

    private boolean preserveAll;              // true if all elements have whitespace preserved

    // stripStack is used to hold information used while stripping nodes. We avoid allocating
    // space on the tree itself to keep the size of nodes down. Each entry on the stack is two
    // booleans, one indicates the current value of xml-space is "preserve", the other indicates
    // that we are in a space-preserving element.

 	// We use a collection of rules to determine whether to strip spaces; a collection
	// of rules is known as a Mode. (We are reusing the code for template rule matching)

	private StripSpaceRules stripperMode;

    /**
    * Default constructor for use in subclasses
    */

    public RuleBasedStripper() {}

    /**
    * create a Stripper and initialise variables
    * @param stripperRules defines which elements have whitespace stripped. If
    * null, all whitespace is preserved.
    */

    public RuleBasedStripper(StripSpaceRules stripperRules) {
        stripperMode = stripperRules;
        preserveAll = (stripperRules==null);
    }

    /**
     * Get a clean copy of this stripper. The new copy shares the same PipelineConfiguration
     * as the original, but the underlying receiver (that is, the destination for post-stripping
     * events) is left uninitialized.
     */

    public RuleBasedStripper getAnother() {
        RuleBasedStripper clone = new RuleBasedStripper(stripperMode);
        clone.setPipelineConfiguration(getPipelineConfiguration());
        clone.preserveAll = preserveAll;
        return clone;
    }

    /**
     * Decide whether an element is in the set of white-space preserving element names
     * @param fingerprint Identifies the name of the element whose whitespace is to
     * be preserved
     * @return ALWAYS_PRESERVE if the element is in the set of white-space preserving
     *  element types, ALWAYS_STRIP if the element is to be stripped regardless of the
     * xml:space setting, and STRIP_DEFAULT otherwise
    */

    public byte isSpacePreserving(StructuredQName fingerprint) throws XPathException {
        if (preserveAll) {
            return ALWAYS_PRESERVE;
        }

        Rule rule = stripperMode.getRule(fingerprint);

        if (rule==null) {
            return ALWAYS_PRESERVE;
        }

        return (rule.getAction() == PRESERVE ? ALWAYS_PRESERVE : STRIP_DEFAULT);

    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
