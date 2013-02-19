package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.expr.instruct.Template;
import client.net.sf.saxon.ce.pattern.*;
import client.net.sf.saxon.ce.style.StylesheetModule;
import client.net.sf.saxon.ce.type.Type;

import java.util.HashMap;


/**
 * The set of rules used to decide strip-space/preserve-space matching of element names in XSLT.
 *
 * @author Michael H. Kay
 */

public class StripSpaceRules  {

   // This is a cut-down version of the Mode class, which until 9.3 was used for the job, even though
   // it is over-engineered for the task.

    private Rule anyElementRule = null;
    private Rule unnamedElementRuleChain = null;
    private HashMap<Integer, Rule> namedElementRules = new HashMap<Integer, Rule>(32);
    private int sequence = 0;

    /**
     * Default constructor - creates a StripSpaceRules containing no rules
      */

    public StripSpaceRules() {
    }

    /**
     * Add a rule
     *
     * @param test          a NodeTest (*, *:local, prefix:*, or QName)
     * @param action           StripRuleTarget.STRIP or StripRuleTarget.PRESERVE
     * @param module the stylesheet module containing the rule
     */

    public void addRule(NodeTest test, Template action,
                        StylesheetModule module, int lineNumber) {

        // for fast lookup, we maintain one list for each element name for patterns that can only
        // match elements of a given name, one list for each node type for patterns that can only
        // match one kind of non-element node, and one generic list.
        // Each list is sorted in precedence/priority order so we find the highest-priority rule first

        int precedence = module.getPrecedence();
        int minImportPrecedence = module.getMinImportPrecedence();
        double priority = test.getDefaultPriority();
        Pattern pattern = new NodeTestPattern(test);
        pattern.setSystemId(module.getSourceElement().getSystemId());
        Rule newRule = new Rule(pattern, action, precedence, minImportPrecedence, priority, sequence++, false, null);
        newRule.setRank((precedence << 16) + sequence);
        if (test instanceof NodeKindTest) {
            newRule.setAlwaysMatches(true);
            anyElementRule = addRuleToList(newRule, anyElementRule, true);
        } else if (test instanceof NameTest) {
            newRule.setAlwaysMatches(true);
            int fp = test.getFingerprint();
            Rule chain = namedElementRules.get(fp);
            namedElementRules.put(fp, addRuleToList(newRule, chain, true));
        } else {
            unnamedElementRuleChain = addRuleToList(newRule, unnamedElementRuleChain, false);
        }

    }

    /**
     * Insert a new rule into this list before others of the same precedence
     * (we rely on the fact that all rules in a list have the same priority)
     * @param newRule the new rule to be added into the list
     * @param list the Rule at the head of the list, or null if the list is empty
     * @param dropRemainder if only one rule needs to be retained
     * @return the new head of the list (which might be the old head, or the new rule if it
     * was inserted at the start)
     */


    private Rule addRuleToList(Rule newRule, Rule list, boolean dropRemainder) {
        if (list == null) {
            return newRule;
        }
        int precedence = newRule.getPrecedence();
        Rule rule = list;
        Rule prev = null;
        while (rule != null) {
            if (rule.getPrecedence() <= precedence) {
                newRule.setNext(dropRemainder ? null : rule);
                if (prev == null) {
                    return newRule;
                } else {
                    prev.setNext(newRule);
                }
                break;
            } else {
                prev = rule;
                rule = rule.getNext();
            }
        }
        if (rule == null) {
            prev.setNext(newRule);
            newRule.setNext(null);
        }
        return list;
    }

    /**
     * Get the rule corresponding to a given element node, by finding the best pattern match.
     *
     * @param fingerprint the name of the element node to be matched
     * @return the best matching rule, if any (otherwise null).
     */

    public Rule getRule(int fingerprint) {

        // search the specific list for this node type / node name

        Rule bestRule = namedElementRules.get(fingerprint);

        // search the list for *:local and prefix:* node tests

        if (unnamedElementRuleChain != null) {
            bestRule = searchRuleChain(fingerprint, bestRule, unnamedElementRuleChain);
        }

        // See if there is a "*" rule matching all elements

        if (anyElementRule != null) {
            bestRule = searchRuleChain(fingerprint, bestRule, anyElementRule);
        }

        return bestRule;
    }

    /**
     * Search a chain of rules
     * @param fingerprint the name of the element node being matched
     * @param bestRule the best rule so far in terms of precedence and priority (may be null)
     * @param head the rule at the head of the chain to be searched
     * @return the best match rule found in the chain, or the previous best rule, or null
     * @throws client.net.sf.saxon.ce.trans.XPathException
     */

    private Rule searchRuleChain(int fingerprint, Rule bestRule, Rule head) {
        while (head != null) {
            if (bestRule != null) {
                int rank = head.compareRank(bestRule);
                if (rank < 0) {
                    // if we already have a match, and the precedence or priority of this
                    // rule is lower, quit the search
                    break;
                } else if (rank == 0) {
                    // this rule has the same precedence and priority as the matching rule already found
                    if (head.isAlwaysMatches() ||
                            head.getPattern().getNodeTest().matches(Type.ELEMENT, fingerprint, -1)) {
                        // reportAmbiguity(bestRule, head);
                        // We no longer report the recoverable error XTRE0270, we always
                        // take the recovery action.
                        // choose whichever one comes last (assuming the error wasn't fatal)
                        bestRule = head;
                        break;
                    } else {
                        // keep searching other rules of the same precedence and priority
                    }
                } else {
                    // this rule has higher rank than the matching rule already found
                    if (head.isAlwaysMatches() ||
                            head.getPattern().getNodeTest().matches(Type.ELEMENT, fingerprint, -1)) {
                        bestRule = head;
                    }
                }
            } else if (head.isAlwaysMatches() ||
                    head.getPattern().getNodeTest().matches(Type.ELEMENT, fingerprint, -1)) {
                bestRule = head;
                break;
            }
            head = head.getNext();
        }
        return bestRule;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
