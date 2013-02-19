package client.net.sf.saxon.ce.regex;

import client.net.sf.saxon.ce.expr.z.IntPredicate;


/**
 * Represents an operation or instruction in the regular expression program. The class Operation
 * is abstract, and has concrete subclasses for each kind of operation/instruction
 */
public abstract class Operation {

    // Offset of the next instruction in the program (if branching). During code generation
    // this is a relative offset; when the array of operations is passed to the REProgram object
    // it is converted to an absolute offset.
    public int next;

    // Actions available after calling the exec() method
    public static final int ACTION_ADVANCE_TO_NEXT = 1;         // advance to next instruction
    public static final int ACTION_RETURN = 2;                  // return to caller
    public static final int ACTION_ADVANCE_TO_FOLLOWING = 3;    // proceed to collowing instruction
    public static final int ACTION_ADVANCE_TO_NEXT_NEXT = 4;    // advance to next instruction of the next instruction

    /**
     * Execute the operation
     * @param matcher the REMatcher
     * @param node the program node containing this operation
     * @param idx the current position in the input string
     * @return >=0: matching succeeded, returns new position in input string.
     * -1: matching failed: return to caller.
     */

    abstract int exec(REMatcher matcher, int node, int idx);

    /**
     * Determine the action to take after calling exec()
     * @param idx the value returned by exec()
     * @return one of the values ACTION_RETURN, ACTION_ADVANCE_TO_NEXT, ...
     */

    public int nextAction(int idx) {
        // Default action: return -1 on failure, continue on success
        if (idx == -1) {
            return ACTION_RETURN;
        } else {
            return ACTION_ADVANCE_TO_NEXT;
        }
    }

    /**
     * End of program
     */

    public static class OpEndProgram extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            // An anchored match is successful only if we are at the end of the string.
            // Otherwise, match has succeeded unconditionally
            if (matcher.anchoredMatch) {
                return (matcher.search.isEnd(idx) ? idx : -1);
            } else {
                matcher.setParenEnd(0, idx);
                return idx;
            }
        }

        public int nextAction(int idx) {
            return ACTION_RETURN;
        }

        public String toString() {
            return "END";
        }

    }

    /**
     * Beginning of Line (^)
     */

    public static class OpBOL extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            // Fail if we're not at the start of the string
            if (idx != 0) {
                // If we're multiline matching, we could still be at the start of a line
                if (matcher.program.flags.isMultiLine()) {
                    // Continue if at the start of a line
                    if (matcher.isNewline(idx - 1)) {
                        return idx;
                    }
                }
                return -1;
            }
            return idx;
        }

        public String toString() {
            return "BOL";
        }

    }

    /**
     * End of Line ($)
     */

    public static class OpEOL extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            // If we're not at the end of string

            UnicodeString search = matcher.search;
            if (matcher.program.flags.isMultiLine()) {
                if (search.isEnd(0) || search.isEnd(idx) || matcher.isNewline(idx)) {
                    return idx; //match successful
                } else {
                    return -1;
                }
            } else {
                // TODO: Spec issue. De facto rule (and XSLT test regex02) assume $ matches a final \n
                if (search.isEnd(0) || search.isEnd(idx) || (matcher.isNewline(idx) && search.isEnd(idx+1))) {
                    return idx;
                } else {
                    return -1;
                }
            }

        }

        public String toString() {
            return "EOL";
        }

    }

    /**
     * Choice (|)
     */

    public static class OpBranch extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            // Try all available branches
            int idxNew;
            do {
                // Try matching the branch against the string
                if ((idxNew = matcher.matchNodes(node + 1, idx)) != -1) {
                    return idxNew;
                }

                // Go to next branch (if any)
                node = matcher.instructions[node].next;
            }
            while (node != -1 && (matcher.program.instructions[node] instanceof Operation.OpBranch));

            // Failed to match any branch!
            return -1;
        }

        public int nextAction(int idx) {
            return ACTION_RETURN;
        }

        public String toString() {
            return "BRANCH";
        }

    }

    /**
     * Atom
     */

    public static class OpAtom extends Operation {
        public UnicodeString atom;

        public int exec(REMatcher matcher, int node, int idx) {
            // Match an atom value
            UnicodeString search = matcher.search;
            if (search.isEnd(idx)) {
                return -1;
            }


            // Give up if not enough input remains to have a match
            if (search.isEnd(atom.length() + idx - 1)) {
                return -1;
            }

            // Match atom differently depending on casefolding flag
            if (matcher.program.flags.isCaseIndependent()) {
                for (int i = 0; i < atom.length(); i++) {
                    if (!matcher.equalCaseBlind(search.charAt(idx++), atom.charAt(i))) {
                        return -1;
                    }
                }
            } else {
                for (int i = 0; i < atom.length(); i++) {
                    if (search.charAt(idx++) != atom.charAt(i)) {
                        return -1;
                    }
                }
            }
            return idx;
        }

        public String toString() {
            return "ATOM \"" + atom.toString() + "\"";
        }
    }

    /**
     * Star quantifier
     */

    public static class OpStar extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            // Note: same as OpMaybe
            // If we've been here before, then don't try again; we won't make any progress.
            if (matcher.beenHereBefore(idx, node)) {
                return -1;
            }

            // Try to match the following subexpr. If it matches:
            //   MAYBE:  Continues matching rest of the expression
            //    STAR:  Points back here to repeat subexpr matching
            return matcher.matchNodes(node + 1, idx);
        }

        public int nextAction(int idx) {
            if (idx == -1) {
                return ACTION_ADVANCE_TO_NEXT;
            } else {
                return ACTION_RETURN;
            }
        }

        public String toString() {
            return "STAR";
        }

    }

    /**
     * "Confident Star" quantifier: used when there is no ambiguity about the ending condition,
     * and therefore no need to backtrack. This means we can use iteration rather than recursion,
     * eliminating the risk of stack overflow.
     */

    public static class OpConfidentStar extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {

            // If we've been here before, then don't try again; we won't make any progress.
            if (matcher.beenHereBefore(idx, node)) {
                return -1;
            }

            int newIdx;
            Operation term = matcher.instructions[node+1];
            while (true) {
                newIdx = term.exec(matcher, node+1, idx);
                if (newIdx == -1) {
                    return idx;
                } else {
                    idx = newIdx;
                }
            }
        }

        public int nextAction(int idx) {
            return ACTION_ADVANCE_TO_NEXT;
        }

        public String toString() {
            return "CONFIDENT_STAR";
        }

    }


    /**
     * Plus quantifier
     */

    public static class OpPlus extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            return matcher.matchNodes(next, idx);
        }

        public int nextAction(int idx) {
            if (idx == -1) {
                return ACTION_ADVANCE_TO_NEXT_NEXT;
            } else {
                return ACTION_RETURN;
            }
        }

        public String toString() {
            return "PLUS";
        }

    }

    /**
     * "Confident Plus" quantifier: used when there is no ambiguity about the ending condition,
     * and therefore no need to backtrack. This means we can use iteration rather than recursion,
     * eliminating the risk of stack overflow.
     */

    public static class OpConfidentPlus extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {

            // If we've been here before, then don't try again; we won't make any progress.
            if (matcher.beenHereBefore(idx, node)) {
                return -1;
            }

            int newIdx;
            Operation term = matcher.instructions[node-1];
            while (true) {
                newIdx = term.exec(matcher, node-1, idx);
                if (newIdx == -1) {
                    return idx;
                } else {
                    idx = newIdx;
                }
            }
        }

        public int nextAction(int idx) {
            return ACTION_ADVANCE_TO_NEXT;
        }

        public String toString() {
            return "CONFIDENT_PLUS";
        }

    }


    /**
     * Maybe (question-mark) quantifier
     */

    public static class OpMaybe extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            // Note: same as OpStar
            // If we've been here before, then don't try again; we won't make any progress.
            if (matcher.beenHereBefore(idx, node)) {
                return -1;
            }

            // Try to match the following subexpr. If it matches:
            //   MAYBE:  Continues matching rest of the expression
            //    STAR:  Points back here to repeat subexpr matching
            return matcher.matchNodes(node + 1, idx);
        }

        public int nextAction(int idx) {
            if (idx == -1) {
                return ACTION_ADVANCE_TO_NEXT;
            } else {
                return ACTION_RETURN;
            }
        }

        public String toString() {
            return "MAYBE";
        }

    }

    /**
     * Open paren (captured group)
     */

    public static class OpOpen extends Operation {

        public int groupNr;

        public OpOpen(int group) {
            this.groupNr = group;
        }

        public int exec(REMatcher matcher, int node, int idx) {
            if ((matcher.program.optimizationFlags & REProgram.OPT_HASBACKREFS) != 0) {
                matcher.startBackref[groupNr] = idx;
            }
            int idxNew = matcher.matchNodes(next, idx);
            if (idxNew != -1) {
                // Increase valid paren count
                if (groupNr >= matcher.parenCount) {
                    matcher.parenCount = groupNr + 1;
                }

                // Don't set paren if already set later on
                if (matcher.getParenStart(groupNr) == -1) {
                    matcher.setParenStart(groupNr, idx);
                }
            }
            return idxNew;
        }

        public int nextAction(int idx) {
            return ACTION_RETURN;
        }

        public String toString() {
            return "OPEN_GROUP " + groupNr;
        }

    }

    /**
     * Open non-capturing paren
     */

    public static class OpOpenCluster extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            return idx;
        }

        public int nextAction(int idx) {
            return ACTION_ADVANCE_TO_NEXT;
        }

        public String toString() {
            return "OPEN_CLUSTER";
        }

    }

    /**
     * Close paren (captured group)
     */

    public static class OpClose extends Operation {

        public int groupNr;

        public OpClose(int groupNr) {
            this.groupNr = groupNr;
        }

        public int exec(REMatcher matcher, int node, int idx) {
            // Done matching subexpression
            if ((matcher.program.optimizationFlags & REProgram.OPT_HASBACKREFS) != 0) {
                matcher.endBackref[groupNr] = idx;
            }
            int idxNew = matcher.matchNodes(next, idx);
            if (idxNew != -1) {
                // Increase valid paren count
                if (groupNr >= matcher.parenCount) {
                    matcher.parenCount = groupNr + 1;
                }

                // Don't set paren if already set later on
                if (matcher.getParenEnd(groupNr) == -1) {
                    matcher.setParenEnd(groupNr, idx);
                }
            }
            return idxNew;
        }

        public int nextAction(int idx) {
            return ACTION_RETURN;
        }

        public String toString() {
            return "CLOSE_GROUP " + groupNr;
        }
    }

    /**
     * Close non-capturing group
     */

    public static class OpCloseCluster extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            return idx;
        }

        public int nextAction(int idx) {
            return ACTION_ADVANCE_TO_NEXT;
        }

        public String toString() {
            return "CLOSE_CLUSTER";
        }
    }

    /**
     * Back-reference
     */

    public static class OpBackReference extends Operation {

        public int groupNr;

        public int exec(REMatcher matcher, int node, int idx) {
            // Get the start and end of the backref
            int s = matcher.startBackref[groupNr];
            int e = matcher.endBackref[groupNr];

            // We don't know the backref yet
            if (s == -1 || e == -1) {
                return -1;
            }

            // The backref is empty size
            if (s == e) {
                return idx;
            }

            // Get the length of the backref
            int l = e - s;

            // If there's not enough input left, give up.
            UnicodeString search = matcher.search;
            if (search.isEnd(idx + l - 1)) {
                return -1;
            }

            // Case fold the backref?
            if (matcher.program.flags.isCaseIndependent()) {
                // Compare backref to input
                for (int i = 0; i < l; i++) {
                    if (!matcher.equalCaseBlind(search.charAt(idx++), search.charAt(s + i))) {
                        return -1;
                    }
                }
            } else {
                // Compare backref to input
                for (int i = 0; i < l; i++) {
                    if (search.charAt(idx++) != search.charAt(s + i)) {
                        return -1;
                    }
                }
            }
            return idx;
        }

        public String toString() {
            return "BACKREF " + groupNr;
        }
    }

    /**
     * Goto specified instruction
     */

    public static class OpGoTo extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            return idx;
        }

        public int nextAction(int idx) {
            return ACTION_ADVANCE_TO_NEXT;
        }

        public String toString() {
            return "GOTO";
        }

    }

    /**
     * Match empty string
     */

    public static class OpNothing extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            return idx;
        }

        public int nextAction(int idx) {
            return ACTION_ADVANCE_TO_NEXT;
        }

        public String toString() {
            return "NOTHING";
        }
    }

    /**
     * Continue to the following instruction (ignore 'next')
     */

    public static class OpContinue extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            return idx;
        }

        public int nextAction(int idx) {
            return ACTION_ADVANCE_TO_FOLLOWING;
        }

        public String toString() {
            return "CONTINUE";
        }
    }

    /**
     * Reluctant star operator
     */

    public static class OpReluctantStar extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            // Don't go round in circles...
            if (matcher.beenHereBefore(idx, node)) {
                return -1;
            }
            // Try to match the rest without using the reluctant subexpr

            int idxNew = matcher.matchNodes(next, idx);
            if (idxNew != -1) {
                return idxNew;
            }

            // Try reluctant subexpr. If it matches:
            //   RELUCTANTMAYBE: Continues matching rest of the expression
            //    RELUCTANTSTAR: Points back here to repeat reluctant star matching
            return matcher.matchNodes(node + 1, next, idx);
        }

        public int nextAction(int idx) {
            return ACTION_RETURN;
        }

        public String toString() {
            return "RELUCTANT_STAR";
        }
    }

    /**
     * Reluctant plus operator
     */

    public static class OpReluctantPlus extends Operation {

        public int exec(REMatcher matcher, int node, int idx) {
            return matcher.matchNodes(matcher.instructions[next].next, idx);
        }

        public int nextAction(int idx) {
            if (idx == -1) {
                return ACTION_ADVANCE_TO_NEXT;
            } else {
                return ACTION_RETURN;
            }
        }

        public String toString() {
            return "RELUCTANT_PLUS";
        }
    }

    /**
     * Reluctant maybe operator
     */

    public static class OpReluctantMaybe extends Operation {

        // Note: same as ReluctantStar

        public int exec(REMatcher matcher, int node, int idx) {
            // Don't go round in circles...
            if (matcher.beenHereBefore(idx, node)) {
                return -1;
            }
            // Try to match the rest without using the reluctant subexpr

            int idxNew = matcher.matchNodes(next, idx);
            if (idxNew != -1) {
                return idxNew;
            }

            // Try reluctant subexpr. If it matches:
            //   RELUCTANTMAYBE: Continues matching rest of the expression
            //    RELUCTANTSTAR: Points back here to repeat reluctant star matching
            return matcher.matchNodes(node + 1, next, idx);
        }

        public int nextAction(int idx) {
            return ACTION_RETURN;
        }

        public String toString() {
            return "RELUCTANT_MAYBE";
        }
    }

    /**
     * Character class: match any one of a set of characters
     */

    public static class OpCharClass extends Operation {
        IntPredicate predicate;

        public int exec(REMatcher matcher, int node, int idx) {
            // Out of input?
            UnicodeString search = matcher.search;
            if (search.isEnd(idx)) {
                return -1;
            }

            if (!predicate.matches(search.charAt(idx))) {
                return -1;
            }

            // Matched.
            return idx+1;
        }

        public String toString() {
            return "CHAR_CLASS (" + predicate.getClass() + ") ";
        }
    }



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
