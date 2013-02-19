package client.net.sf.saxon.ce.regex;

import client.net.sf.saxon.ce.expr.z.*;

import java.io.PrintStream;

/**
 * A class that holds compiled regular expressions.
 *
 * @see REMatcher
 * @see RECompiler
 *
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 * @version $Id: REProgram.java 518156 2007-03-14 14:31:26Z vgritsenko $
 */
public class REProgram
{
    static final int OPT_HASBACKREFS = 1;
    static final int OPT_HASBOL      = 2;

    Operation[] instructions;
    REFlags flags;
    UnicodeString prefix;              // Prefix string optimization
    int optimizationFlags;      // Optimization flags (REProgram.OPT_*)
    int maxParens = -1;
    boolean nullable = false;

    /**
     * Constructs a program object from a character array
     * @param parens Count of parens in the program
     * @param instructions Array with RE opcode instructions in it. The "next"
     * pointers within the operations must already have been converted to absolute
     * offsets.
     */
    public REProgram(Operation[] instructions, int parens, REFlags flags) {
        this.flags = flags;
        setInstructions(instructions);
        this.maxParens = parens;
    }



    /**
     * Sets a new regular expression program to run.  It is this method which
     * performs any special compile-time search optimizations.  Currently only
     * two optimizations are in place - one which checks for backreferences
     * (so that they can be lazily allocated) and another which attempts to
     * find an prefix anchor string so that substantial amounts of input can
     * potentially be skipped without running the actual program.
     * @param instructions Program instruction buffer
     */
    private void setInstructions(Operation[] instructions) {
        // Save reference to instruction array
        this.instructions = instructions;

        // Initialize other program-related variables
        this.optimizationFlags = 0;
        this.prefix = null;

        // Try various compile-time optimizations if there's a program
        if (instructions != null && instructions.length != 0) {
            if (instructions[0] instanceof Operation.OpAtom) {
                prefix = ((Operation.OpAtom)instructions[0]).atom;
            }
            // If the first node is a branch
            if (instructions[0] instanceof Operation.OpBranch) {
                // to the end node
                int next = instructions[0].next;
                if (instructions[next] instanceof Operation.OpEndProgram) {
                    final Operation nextOp = instructions[1];
                    // the branch starts with an atom
                    if (nextOp instanceof Operation.OpAtom) {
                        // then get that atom as an prefix because there's no other choice
                        this.prefix = ((Operation.OpAtom)nextOp).atom;
                    }
                    // the branch starts with a BOL
                    else if (nextOp instanceof Operation.OpBOL) {
                        // then set the flag indicating that BOL is present
                        this.optimizationFlags |= OPT_HASBOL;
                    }
                }
            }

            // Check for backreferences
            for (Operation op : instructions) {
                if (op instanceof Operation.OpBackReference) {
                    optimizationFlags |= OPT_HASBACKREFS;
                    break;
                }
            }

            // Check for deterministic quantifiers; the optimization causes constructs such as A* or [0-9]+ to
            // be evaluated using iteration rather than recursion if there is no ambiguity about the ending condition,
            // which means there will never be any need to backtrack.
            boolean caseBlind = flags.isCaseIndependent();
            for (int i=0; i<instructions.length; i++) {
                Operation op = instructions[i];
                if (op instanceof Operation.OpStar &&
                        op.next == i+2 &&
                        (instructions[i+1] instanceof Operation.OpAtom || instructions[i+1] instanceof Operation.OpCharClass)) {
                    if (noAmbiguity(instructions[i+1], instructions[op.next], caseBlind)) {
                        //System.err.println("Optimizing *");
                        instructions[i] = new Operation.OpConfidentStar();
                        instructions[i].next = op.next;
                    }
                } else if (op instanceof Operation.OpPlus &&
                        op.next == i-2 &&
                        (instructions[i-1] instanceof Operation.OpAtom || instructions[i-1] instanceof Operation.OpCharClass) &&
                        (instructions[i-2].next == i+1)) {
                    if (noAmbiguity(instructions[i-1], instructions[i+1], caseBlind)) {
                        //System.err.println("Optimizing +");
                        instructions[i] = new Operation.OpConfidentPlus();
                        instructions[i].next = i+1;
                    }
                }
            }
        }
    }

    /**
     * Ask whether the regular expression matches a zero length string
     * @return true if the regex matches a zero length string
     */

    public boolean isNullable() {
        return nullable;
    }

    /**
     * Say whether the regular expression matches a zero length string
     * @param nullable true if the regex matches a zero length string
     */

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }


    /**
     * Returns a copy of the prefix of current regular expression program
     * in a character array.  If there is no prefix, or there is no program
     * compiled yet, <code>getPrefix</code> will return null.
     * @return A copy of the prefix of current compiled RE program
     */
    public UnicodeString getPrefix() {
        return prefix;
    }

    /**
     * Output a human-readable printout of the program
     */

    public void display(PrintStream out) {
        for (int i=0; i<instructions.length; i++) {
            int nextOffset = instructions[i].next;
            out.println(i + ". " + instructions[i].toString() +
                    (nextOffset==-1 ? "" : ", next = " + (nextOffset)));
        }
    }

    /**
     * Determine that there is no ambiguity between two branches, that is, if one of them matches then the
     * other cannot possibly match. (This is for optimization, so it does not have to detect all cases; but
     * if it returns true, then the result must be dependable.)
     * @return true if it can be established that there is no input sequence that will match both instructions
     */

    boolean noAmbiguity(Operation op0, Operation op1, boolean caseBlind) {
        // op0 will always be either an Atom or a CharClass. op1 may be anything.
        if (op1 instanceof Operation.OpClose || op1 instanceof Operation.OpCloseCluster) {
            op1 = instructions[op1.next];
        }
        if (op1 instanceof Operation.OpEndProgram || op1 instanceof Operation.OpBOL || op1 instanceof Operation.OpEOL) {
            return true;
        }
        IntSet set0;
        if (op0 instanceof Operation.OpAtom) {
            set0 = getInitialChars((Operation.OpAtom) op0, caseBlind);
        } else {
            IntPredicate ip0 = ((Operation.OpCharClass)op0).predicate;
            if (ip0 instanceof IntSetPredicate) {
                set0 = ((IntSetPredicate)ip0).getIntSet();
            } else if (ip0 instanceof IntValuePredicate) {
                set0 = new IntSingletonSet(((IntValuePredicate)ip0).getTarget());
            } else {
                return false;
            }
        }

        IntSet set1;
        if (op1 instanceof Operation.OpAtom) {
            set1 = getInitialChars((Operation.OpAtom) op1, caseBlind);
        } else if (op1 instanceof Operation.OpCharClass) {
            IntPredicate ip1 = ((Operation.OpCharClass)op1).predicate;
            if (ip1 instanceof IntSetPredicate) {
                set1 = ((IntSetPredicate)ip1).getIntSet();
            } else if (ip1 instanceof IntValuePredicate) {
                set1 = new IntSingletonSet(((IntValuePredicate)ip1).getTarget());
            } else {
                return false;
            }
        } else {
            return false;
        }

        return isDisjoint(set0, set1);
    }

    private IntSet getInitialChars(Operation.OpAtom op, boolean caseBlind) {
        IntSet set;
        int ch = op.atom.charAt(0);
        set = new IntSingletonSet(ch);
        if (caseBlind) {
            set = new IntHashSet(10);
            set.add(ch);
            for (int v : CaseVariants.getCaseVariants(ch)) {
                set.add(v);
            }
        }
        return set;
    }

    boolean isDisjoint(IntSet set0, IntSet set1) {
        try {
            IntSet intersection = set0.intersect(set1);
            return intersection.isEmpty();
        } catch (Throwable e) {
            return false;
        }
    }
}

// This class is derived from the Apache Jakarta project, with substantial
// modifications by Saxonica to make the regular expression dialect conform
// with XPath 2.0 specifications.

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

