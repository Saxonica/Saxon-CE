package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.XPathContextMajor;
import client.net.sf.saxon.ce.expr.instruct.Template;
import client.net.sf.saxon.ce.expr.sort.GenericSorter;
import client.net.sf.saxon.ce.expr.sort.Sortable;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.*;
import client.net.sf.saxon.ce.style.StylesheetModule;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.core.client.JavaScriptObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A Mode is a collection of rules; the selection of a rule to apply to a given element
 * is determined by a Pattern.
 *
 * @author Michael H. Kay
 */

public class Mode  {

    public static final int UNNAMED_MODE = -1;
    public static final int NAMED_MODE = -3;

    public static final StructuredQName ALL_MODES =
            new StructuredQName("saxon", NamespaceConstant.SAXON, "_omniMode");
    public static final StructuredQName UNNAMED_MODE_NAME =
            new StructuredQName("saxon", NamespaceConstant.SAXON, "_defaultMode");

    private BuiltInRuleSet builtInRuleSet = StringifyRuleSet.getInstance();

    private Rule genericNodeRuleChain = null;
    private ArrayList<Rule> virtualRuleChain = null;
    private Rule documentRuleChain = null;
    private Rule textRuleChain = null;
    private Rule commentRuleChain = null;
    private Rule processingInstructionRuleChain = null;
    private Rule namespaceRuleChain = null;
    private Rule unnamedElementRuleChain = null;
    private Rule unnamedAttributeRuleChain = null;
    private HashMap<StructuredQName, Rule> namedElementRuleChains = new HashMap<StructuredQName, Rule>(32);
    private HashMap<StructuredQName, Rule> namedAttributeRuleChains = new HashMap<StructuredQName, Rule>(8);

    private Rule mostRecentRule;
    private int mostRecentModuleHash;
    private boolean isDefault;
    private boolean hasRules = false;
    private StructuredQName modeName;
    private int stackFrameSlotsNeeded = 0;

    /**
     * Default constructor - creates a Mode containing no rules
     * @param usage one of {@link #UNNAMED_MODE}, {@link #NAMED_MODE}
     * @param modeName the name of the mode
     */

    public Mode(int usage, StructuredQName modeName) {
        isDefault = (usage == UNNAMED_MODE);
        this.modeName = modeName;
    }

    /**
     * Construct a new Mode, copying the contents of an existing Mode
     *
     * @param omniMode the existing mode. May be null, in which case it is not copied
     * @param modeName the name of the new mode to be created
     */

    public Mode(Mode omniMode, StructuredQName modeName) {
        isDefault = false;
        this.modeName = modeName;
        if (omniMode != null) {
            documentRuleChain =
                    omniMode.documentRuleChain==null ? null : new Rule(omniMode.documentRuleChain);
            textRuleChain =
                    omniMode.textRuleChain==null ? null : new Rule(omniMode.textRuleChain);
            commentRuleChain =
                    omniMode.commentRuleChain==null ? null : new Rule(omniMode.commentRuleChain);
            processingInstructionRuleChain =
                    omniMode.processingInstructionRuleChain==null ? null : new Rule(omniMode.processingInstructionRuleChain);
            namespaceRuleChain =
                    omniMode.namespaceRuleChain==null ? null : new Rule(omniMode.namespaceRuleChain);
            unnamedElementRuleChain =
                    omniMode.unnamedElementRuleChain==null ? null : new Rule(omniMode.unnamedElementRuleChain);
            unnamedAttributeRuleChain =
                    omniMode.unnamedAttributeRuleChain==null ? null : new Rule(omniMode.unnamedAttributeRuleChain);

            namedElementRuleChains = new HashMap<StructuredQName, Rule>(omniMode.namedElementRuleChains.size());
            Iterator<StructuredQName> ii = omniMode.namedElementRuleChains.keySet().iterator();
            while (ii.hasNext()) {
                StructuredQName fp = ii.next();
                Rule r = omniMode.namedElementRuleChains.get(fp);
                namedElementRuleChains.put(fp, new Rule(r));
            }
            ii = omniMode.namedAttributeRuleChains.keySet().iterator();
            while (ii.hasNext()) {
                StructuredQName fp = ii.next();
                Rule r = omniMode.namedAttributeRuleChains.get(fp);
                namedAttributeRuleChains.put(fp, new Rule(r));
            }
            mostRecentRule = omniMode.mostRecentRule;
            mostRecentModuleHash = omniMode.mostRecentModuleHash;
        }
    }
    
    public ArrayList<Rule> getVirtualRuleSet() {
    	return virtualRuleChain;
    }

     /**
     * Get the built-in template rules to be used with this Mode in the case where there is no
     * explicit template rule
     * @return the built-in rule set, defaulting to the StringifyRuleSet if no other rule set has
     * been supplied
     */

    public BuiltInRuleSet getBuiltInRuleSet() {
        return this.builtInRuleSet;
    }

    /**
     * Determine if this is the default mode
     * @return true if this is the default (unnamed) mode
     */

    public boolean isDefaultMode() {
        return isDefault;
    }

    /**
     * Get the name of the mode (for diagnostics only)
     * @return the mode name. Null for the default (unnamed) mode
     */

    public StructuredQName getModeName() {
        return modeName;
    }

    /**
     * Ask whether there are any template rules in this mode
     * (a mode could exist merely because it is referenced in apply-templates)
     * @return true if no template rules exist in this mode
     */

    public boolean isEmpty() {
        return !hasRules;
    }

    /**
     * Add a rule to the Mode.
     *
     * @param pattern          a Pattern
     * @param action     the Object to return from getRule() when the supplied node matches this Pattern
     * @param module the stylesheet module containing the rule
     * @param explicitMode  true if adding a template rule for a specific (default or named) mode;
     *      false if adding a rule because it applies to all modes
     */

    public void addRule(Pattern pattern, Template action,
                        StylesheetModule module, double priority, boolean explicitMode, boolean ixslPreventDefault, String ixslEventProperty) {

        if (explicitMode) {
            hasRules = true;
        }

        // Ignore a pattern that will never match, e.g. "@comment"

        if (pattern.getNodeTest() instanceof EmptySequenceTest) {
            return;
        }

        // for fast lookup, we maintain one list for each element name for patterns that can only
        // match elements of a given name, one list for each node type for patterns that can only
        // match one kind of non-element node, and one generic list.
        // Each list is sorted in precedence/priority order so we find the highest-priority rule first

        // This logic is designed to ensure that when a UnionPattern contains multiple branches
        // with the same priority, next-match doesn't select the same template twice (override20_047)
        int moduleHash = module.hashCode();
        int sequence;
        if (mostRecentRule == null) {
            sequence = 0;
        } else if (action == mostRecentRule.getAction() && moduleHash == mostRecentModuleHash) {
            sequence = mostRecentRule.getSequence();
        } else {
            sequence = mostRecentRule.getSequence() + 1;
        }
        int precedence = module.getPrecedence();
        int minImportPrecedence = module.getMinImportPrecedence();
        Rule newRule = new Rule(pattern, action, precedence, minImportPrecedence, priority, sequence, ixslPreventDefault, ixslEventProperty);
        if (pattern instanceof NodeTestPattern) {
            NodeTest test = pattern.getNodeTest();
            if (test instanceof AnyNodeTest) {
                newRule.setAlwaysMatches(true);
            } else if (test instanceof NodeKindTest) {
                newRule.setAlwaysMatches(true);
            } else if (test instanceof NameTest) {
                int kind = test.getRequiredNodeKind();
                if (kind == Type.ELEMENT || kind == Type.ATTRIBUTE) {
                    newRule.setAlwaysMatches(true);
                }
            }

        }
        mostRecentRule = newRule;
        mostRecentModuleHash = moduleHash;

        int kind = pattern.getNodeKind();
        switch (kind) {
            case Type.ELEMENT: {
                StructuredQName fp = pattern.getNodeTest().getRequiredNodeName();
                if (fp == null) {
                    unnamedElementRuleChain = addRuleToList(newRule, unnamedElementRuleChain);
                } else {
                    Rule chain = namedElementRuleChains.get(fp);
                    namedElementRuleChains.put(fp, addRuleToList(newRule, chain));
                }
                break;
            }
            case Type.ATTRIBUTE: {
                StructuredQName fp = pattern.getNodeTest().getRequiredNodeName();
                if (fp == null) {
                    unnamedAttributeRuleChain = addRuleToList(newRule, unnamedAttributeRuleChain);
                } else {
                    Rule chain = namedAttributeRuleChains.get(fp);
                    namedAttributeRuleChains.put(fp, addRuleToList(newRule, chain));
                }
                break;
            }
            case Type.NODE:
            	genericNodeRuleChain = addRuleToList(newRule, genericNodeRuleChain);
                break;
            case Type.DOCUMENT:
                documentRuleChain = addRuleToList(newRule, documentRuleChain);
                break;
            case Type.TEXT:
                textRuleChain = addRuleToList(newRule, textRuleChain);
                break;
            case Type.COMMENT:
                commentRuleChain = addRuleToList(newRule, commentRuleChain);
                break;
            case Type.PROCESSING_INSTRUCTION:
                processingInstructionRuleChain = addRuleToList(newRule, processingInstructionRuleChain);
                break;
            case Type.NAMESPACE:
                namespaceRuleChain = addRuleToList(newRule, namespaceRuleChain);
                break;
            case Type.EMPTY:
            	if (pattern instanceof JSObjectPattern) {
            		if (virtualRuleChain == null) {
            			virtualRuleChain = new ArrayList<Rule>();
            		}
            		newRule.setIsVirtual();
            		virtualRuleChain.add(newRule);
            	}
            	break;
        }

    }

    /**
     * Insert a new rule into this list before others of the same precedence/priority
     * @param newRule the new rule to be added into the list
     * @param list the Rule at the head of the list, or null if the list is empty
     * @return the new head of the list (which might be the old head, or the new rule if it
     * was inserted at the start)
     */


    private Rule addRuleToList(Rule newRule, Rule list) {
        if (list == null) {
            return newRule;
        }
        int precedence = newRule.getPrecedence();
        double priority = newRule.getPriority();
        Rule rule = list;
        Rule prev = null;
        while (rule != null) {
            if ((rule.getPrecedence() < precedence) ||
                    (rule.getPrecedence() == precedence && rule.getPriority() <= priority)) {
                newRule.setNext(rule);
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
     * Specify how many slots for local variables are required by a particular pattern
     * @param slots the number of slots needed
     */

    public void allocatePatternSlots(int slots) {
        stackFrameSlotsNeeded = Math.max(stackFrameSlotsNeeded, slots);
    }

    /**
     * Make a new XPath context for evaluating patterns if there is any possibility that the
     * pattern uses local variables
     *
     * @param context The existing XPath context
     * @return a new XPath context (or the existing context if no new context was created)
     */

    private XPathContext makeNewContext(XPathContext context) {
        context = context.newContext();
        ((XPathContextMajor)context).openStackFrame(stackFrameSlotsNeeded);
        return context;
    }
    
    public Rule getVirtualRule(XPathContext context){
    	if (virtualRuleChain == null) {
    		return null;
    	}
    	JavaScriptObject eventObject = (JavaScriptObject)context.getController().getUserData("Saxon-CE", "current-object");
    	for (Rule r : virtualRuleChain) {
    		JSObjectPattern jso = (JSObjectPattern)r.getPattern();
    		if (jso.matchesObject(eventObject)) {
    			return r;
    		}
    	}
    	return null;
    }

    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @param context the XPath dynamic evaluation context
     * @return the best matching rule, if any (otherwise null).
     */

    public Rule getRule(NodeInfo node, XPathContext context) throws XPathException {

        // If there are match patterns in the stylesheet that use local variables, we need to allocate
        // a new stack frame for evaluating the match patterns. We base this on the match pattern with
        // the highest number of range variables, so we can reuse the same stack frame for all rules
        // that we test against. If no patterns use range variables, we don't bother allocating a new
        // stack frame.

        // Note, this method isn't functionally necessary; we could call the 3-argument version
        // with a filter that always returns true. But this is the common path for apply-templates,
        // and we want to squeeze every drop of performance from it.

        if (stackFrameSlotsNeeded > 0) {
            context = makeNewContext(context);
        }

        // search the specific list for this node type / node name

        Rule unnamedNodeChain;
        Rule bestRule = null;

        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                unnamedNodeChain = documentRuleChain;
                break;

            case Type.ELEMENT: {
                unnamedNodeChain = unnamedElementRuleChain;
                Rule namedNodeChain = namedElementRuleChains.get(node.getNodeName());
                if (namedNodeChain != null) {
                    bestRule = searchRuleChain(node, context, null, namedNodeChain);
                }
                break;
            }
            case Type.ATTRIBUTE: {
                unnamedNodeChain = unnamedAttributeRuleChain;
                Rule namedNodeChain = namedAttributeRuleChains.get(node.getNodeName());
                if (namedNodeChain != null) {
                    bestRule = searchRuleChain(node, context, null, namedNodeChain);
                }
                break;
            }
            case Type.TEXT:
                unnamedNodeChain = textRuleChain;
                break;
            case Type.COMMENT:
                unnamedNodeChain = commentRuleChain;
                break;
            case Type.PROCESSING_INSTRUCTION:
                unnamedNodeChain = processingInstructionRuleChain;
                break;
            case Type.NAMESPACE:
                unnamedNodeChain = namespaceRuleChain;
                break;
            default:
                throw new AssertionError("Unknown node kind");
        }

        // search the list for unnamed nodes of a particular kind

        if (unnamedNodeChain != null) {
            bestRule = searchRuleChain(node, context, bestRule, unnamedNodeChain);
        }

        // Search the list for rules for nodes of unknown node kind

        if (genericNodeRuleChain != null) {
            bestRule = searchRuleChain(node, context, bestRule, genericNodeRuleChain);
        }

        return bestRule;
    }

    /**
     * Search a chain of rules
     * @param node the node being matched
     * @param context XPath dynamic context
     * @param bestRule the best rule so far in terms of precedence and priority (may be null)
     * @param head the rule at the head of the chain to be searched
     * @return the best match rule found in the chain, or the previous best rule, or null
     * @throws XPathException
     */

    private Rule searchRuleChain(NodeInfo node, XPathContext context, Rule bestRule, Rule head) throws XPathException {
        while (head != null) {
            if (bestRule != null) {
                int rank = head.compareRank(bestRule);
                if (rank < 0) {
                    // if we already have a match, and the precedence or priority of this
                    // rule is lower, quit the search
                    break;
                } else if (rank == 0) {
                    // this rule has the same precedence and priority as the matching rule already found
                    if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                        // reportAmbiguity(node, bestRule, head, context);
                        // choose whichever one comes last (assuming the error wasn't fatal)
                        bestRule = (bestRule.getSequence() > head.getSequence() ? bestRule : head);
                        break;
                    } else {
                        // keep searching other rules of the same precedence and priority
                    }
                } else {
                    // this rule has higher rank than the matching rule already found
                    if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                        bestRule = head;
                    }
                }
            } else if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                bestRule = head;
                break;   // choose the first match; rules within a chain are in order of rank
            }
            head = head.getNext();
        }
        return bestRule;
    }

    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @param context the XPath dynamic evaluation context
     * @return the best matching rule, if any (otherwise null).
     */

    public Rule getRule(NodeInfo node, XPathContext context, RuleFilter filter) throws XPathException {

        // If there are match patterns in the stylesheet that use local variables, we need to allocate
        // a new stack frame for evaluating the match patterns. We base this on the match pattern with
        // the highest number of range variables, so we can reuse the same stack frame for all rules
        // that we test against. If no patterns use range variables, we don't bother allocating a new
        // stack frame.

        if (stackFrameSlotsNeeded > 0) {
            context = makeNewContext(context);
        }

        // search the specific list for this node type / node name

        Rule bestRule = null;
        Rule unnamedNodeChain;

        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                unnamedNodeChain = documentRuleChain;
                break;
            case Type.ELEMENT: {
                unnamedNodeChain = unnamedElementRuleChain;
                Rule namedNodeChain = namedElementRuleChains.get(node.getNodeName());
                bestRule = searchRuleChain(node, context, null, namedNodeChain, filter);
                break;
            }
            case Type.ATTRIBUTE: {
                unnamedNodeChain = unnamedAttributeRuleChain;
                Rule namedNodeChain = namedAttributeRuleChains.get(node.getNodeName());
                bestRule = searchRuleChain(node, context, null, namedNodeChain, filter);
                break;
            }
            case Type.TEXT:
                unnamedNodeChain = textRuleChain;
                break;
            case Type.COMMENT:
                unnamedNodeChain = commentRuleChain;
                break;
            case Type.PROCESSING_INSTRUCTION:
                unnamedNodeChain = processingInstructionRuleChain;
                break;
            case Type.NAMESPACE:
                unnamedNodeChain = namespaceRuleChain;
                break;
            default:
                throw new AssertionError("Unknown node kind");
        }

        // Search the list for unnamed nodes of a particular kind

        bestRule = searchRuleChain(node, context, bestRule, unnamedNodeChain, filter);

        // Search the list for rules for nodes of unknown node kind

        return searchRuleChain(node, context, bestRule, genericNodeRuleChain, filter);
    }

    /**
     * Search a chain of rules
     * @param node the node being matched
     * @param context XPath dynamic context
     * @param bestRule the best rule so far in terms of precedence and priority (may be null)
     * @param head the rule at the head of the chain to be searched
     * @return the best match rule found in the chain, or the previous best rule, or null
     * @throws XPathException
     */

    private Rule searchRuleChain(NodeInfo node, XPathContext context,
                                 Rule bestRule, Rule head, RuleFilter filter) throws XPathException {
        while (head != null) {
            if (filter.testRule(head)) {
                if (bestRule != null) {
                    int rank = head.compareRank(bestRule);
                    if (rank < 0) {
                        // if we already have a match, and the precedence or priority of this
                        // rule is lower, quit the search
                        break;
                    } else if (rank == 0) {
                        // this rule has the same precedence and priority as the matching rule already found
                        if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                            // reportAmbiguity(node, bestRule, head, context);
                            // choose whichever one comes last (assuming the error wasn't fatal)
                            bestRule = (bestRule.getSequence() > head.getSequence() ? bestRule : head);
                            break;
                        } else {
                            // keep searching other rules of the same precedence and priority
                        }
                    } else {
                        // this rule has higher rank than the matching rule already found
                        if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                            bestRule = head;
                        }
                    }
                } else if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                    bestRule = head;
                    break;   // choose the first match; rules within a chain are in order of rank
                }
            }
            head = head.getNext();
        }
        return bestRule;
    }


    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match, subject to a minimum
     * and maximum precedence. (This supports xsl:apply-imports)
     *
     * @param node the NodeInfo referring to the node to be matched
     * @param min the minimum import precedence
     * @param max the maximum import precedence
     * @param context the XPath dynamic evaluation context
     * @return the Rule registered for that node, if any (otherwise null).
     */

    public Rule getRule(NodeInfo node, final int min, final int max, XPathContext context) throws XPathException {
        RuleFilter filter = new RuleFilter() {
            public boolean testRule(Rule r) {
                int p = r.getPrecedence();
                return p >= min && p <= max;
            }
        };
        return getRule(node, context, filter);
    }

    /**
     * Get the rule corresponding to a given Node, by finding the next-best Pattern match
     * after the specified object.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @param currentRule the current rule; we are looking for the next match after the current rule
     * @param context the XPath dynamic evaluation context
     * @return the object (e.g. a NodeHandler) registered for that element, if any (otherwise null).
     */

    public Rule getNextMatchRule(NodeInfo node, final Rule currentRule, XPathContext context) throws XPathException {
        RuleFilter filter = new RuleFilter() {
            public boolean testRule(Rule r) {
                int comp = r.compareRank(currentRule);
                return comp < 0 || (comp == 0 && r.getSequence() < currentRule.getSequence());
            }
        };
        return getRule(node, context, filter);
    }

//    /**
//     * Report an ambiguity, that is, the situation where two rules of the same
//     * precedence and priority match the same node
//     *
//     * @param node The node that matches two or more rules
//     * @param r1   The first rule that the node matches
//     * @param r2   The second rule that the node matches
//     * @param c    The controller for the transformation
//     */

//    private void reportAmbiguity(NodeInfo node, Rule r1, Rule r2, XPathContext c)
//            throws XPathException {
//        // don't report an error if the conflict is between two branches of the same Union pattern
//        if (r1.getAction() == r2.getAction() && r1.getSequence() == r2.getSequence()) {
//            return;
//        }
//        String path;
//        String errorCode = "XTRE0540";
//
//        if (isStripper) {
//            // don't report an error if the conflict is between strip-space and strip-space, or
//            // preserve-space and preserve-space
//            if (r1.getAction().equals(r2.getAction())) {
//                return;
//            }
//            errorCode = "XTRE0270";
//            path = "xsl:strip-space";
//        } else {
//            path = Navigator.getPath(node);
//        }
//
//        Pattern pat1 = r1.getPattern();
//        Pattern pat2 = r2.getPattern();
//
//        String message;
//        if (r1.getAction() == r2.getAction()) {
//            message = "Ambiguous rule match for " + path + ". " +
//                "Matches \"" + showPattern(pat1) + "\" in " + pat1.getSystemId() +
//                ", a rule which appears in the stylesheet more than once, because the containing module was included more than once";
//        } else {
//            message = "Ambiguous rule match for " + path + '\n' +
//                "Matches both \"" + showPattern(pat1) +
//                "\nand \"" + showPattern(pat2);
//        }
//
//        XPathException err = new XPathException(message, errorCode);
//        c.getController().recoverableError(err);
//    }

    private static String showPattern(Pattern p) {
        // Complex patterns can be laid out with lots of whitespace, which looks messy in the error message
        return Whitespace.collapseWhitespace(p.toString()).toString();
    }

    /**
     * Walk over all the rules, applying a specified action to each one.
     * @param action an action that is to be applied to all the rules in this Mode
     */

    public void processRules(RuleAction action) throws XPathException {
        processRuleChain(documentRuleChain, action);
        processRuleChain(unnamedElementRuleChain, action);
        Iterator<StructuredQName> ii = namedElementRuleChains.keySet().iterator();
        while (ii.hasNext()) {
            Rule r = namedElementRuleChains.get(ii.next());
            processRuleChain(r, action);
        }
        processRuleChain(unnamedAttributeRuleChain, action);
        ii = namedAttributeRuleChains.keySet().iterator();
        while (ii.hasNext()) {
            Rule r = namedAttributeRuleChains.get(ii.next());
            processRuleChain(r, action);
        }
        processRuleChain(textRuleChain, action);
        processRuleChain(commentRuleChain, action);
        processRuleChain(processingInstructionRuleChain, action);
        processRuleChain(namespaceRuleChain, action);
        processRuleChain(genericNodeRuleChain, action);
    }

    private void processRuleChain(Rule r, RuleAction action) throws XPathException {
        while (r != null) {
            action.processRule(r);
            r = r.getNext();
        }
    }


    /**
     * Compute a rank for each rule, as a combination of the precedence and priority, to allow
     * rapid comparison.
     */

    public void computeRankings() throws XPathException {
        final RuleSorter sorter = new RuleSorter();
        RuleAction addToSorter = new RuleAction() {
            public void processRule(Rule r) {
                sorter.addRule(r);
            }
        };
        // add all the rules in this Mode to the sorter
        processRules(addToSorter);
        // now allocate ranks to all the modes
        sorter.allocateRanks();
    }

    /**
     * Supporting class used at compile time to sort all the rules into precedence/priority
     * order and allocate a rank to each one, so that at run-time, comparing two rules to see
     * which has higher precedence/priority is a simple integer subtraction.
     */

    private static class RuleSorter implements Sortable {
        public ArrayList<Rule> rules = new ArrayList<Rule>(100);
        public void addRule(Rule rule) {
            rules.add(rule);
        }
    
        public int compare(int a, int b) {
            return rules.get(a).compareComputedRank(rules.get(b));
        }

        public void swap(int a, int b) {
            Rule temp = rules.get(a);
            rules.set(a, rules.get(b));
            rules.set(b, temp);
        }

        public void allocateRanks() {
            GenericSorter.quickSort(0, rules.size(), this);
            int rank = 0;
            for (int i=0; i<rules.size(); i++) {
                if ( i> 0 && rules.get(i-1).compareComputedRank(rules.get(i)) != 0) {
                    rank++;
                }
                rules.get(i).setRank(rank);
            }
        }
    }

    /**
     * Interface for helper classes used to filter a chain of rules
     */

    private static interface RuleFilter {
        /**
         * Test a rule to see whether it should be included
         * @param r the rule to be tested
         * @return true if the rule qualifies
         */
        public boolean testRule(Rule r);
    }

    /**
     * Interface for helper classes used to process all the rules in the Mode
     */

    private static interface RuleAction {
        /**
         * Process a given rule
         * @param r the rule to be processed
         */
        public void processRule(Rule r) throws XPathException;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
