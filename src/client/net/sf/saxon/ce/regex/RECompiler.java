package client.net.sf.saxon.ce.regex;

import client.net.sf.saxon.ce.expr.z.*;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.logging.client.LogConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



/**
 * A regular expression compiler class.  This class compiles a pattern string into a
 * regular expression program interpretable by the RE evaluator class.  The 'recompile'
 * command line tool uses this compiler to pre-compile regular expressions for use
 * with RE.  For a description of the syntax accepted by RECompiler and what you can
 * do with regular expressions, see the documentation for the RE matcher class.
 *
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 * @author <a href="mailto:gholam@xtra.co.nz">Michael McCallum</a>
 * @version $Id: RECompiler.java 518156 2007-03-14 14:31:26Z vgritsenko $
 * @see REMatcher
 */

/*
 * Changes made for Saxon:
 *
 * - handle full Unicode repertoire (esp non-BMP characters) using UnicodeString class for
 *   both the source string and the regular expression
 * - added support for subtraction in a character class
 * - in a character range, changed the condition start < end to start <= end
 * - removed support for [:POSIX:] construct
 * - added support for \p{} and \P{} classes
 * - removed support for unsupported escapes: f, x, u, b, octal characters; added i and c
 * - changed the handling of hyphens within square brackets, and ^ appearing other than at the start
 * - changed the data structure used for the executable so that terms that match a character class
 *   now reference an IntPredicate that tests for membership of the character in a set
 * - added support for reluctant {n,m}? quantifiers
 * - allow a quantifier on a nullable expression [syntax permitted; semantics need more work]
 * - allow a quantifier on '$' or '^'
 * - some constructs (back-references, non-capturing groups, etc) are conditional on which XPath/XSD version
 *   is in use
 * - regular expression flags are now fixed at the time the RE is compiled, this can no longer be deferred
 *   until the RE is evaluated
 * - split() function includes a zero-length string at the end of the returned sequence if the last
 *   separator is at the end of the string
 * - added support for the 'q' and 'x' flags; improved support for the 'i' flag
 * - added a method to determine whether there is an anchored match (for XSD use)
 * - tests for newline (e.g in multiline mode) now match \n only, as required by the XPath specification
 * - reorganised the executable program to use Operation objects rather than integer opcodes
 * - introduced optimization for non-backtracking + and * operators (with simple operands)
 */
public class RECompiler {
    // The compiled program
    ArrayList<Operation> instructions = new ArrayList<Operation>(20);

    // Input state for compiling regular expression
    UnicodeString pattern;                                     // Input string
    int len;                                            // Length of the pattern string
    int idx;                                            // Current input index into ac
    int parens;                                         // Total number of paren pairs

    // Node flags
    static final int NODE_NORMAL = 0;                   // No flags (nothing special)
    static final int NODE_NULLABLE = 1;                 // True if node is potentially null
    static final int NODE_TOPLEVEL = 2;                 // True if top level expr

    // {m,n} stacks
    static final int bracketUnbounded = -1;             // Unbounded value
    int bracketMin;                                     // Minimum number of matches
    int bracketOpt;                                     // Additional optional matches

    boolean isXPath = true;
    boolean isXPath30 = true;
    IntHashSet captures = new IntHashSet();

    REFlags reFlags;

    List<String> warnings;

    /**
     * Constructor.  Creates (initially empty) storage for a regular expression program.
     */
    public RECompiler() {

    }

    /**
     * Set the regular expression flags to be used
     * @param flags the regular expression flags
     */

    public void setFlags(REFlags flags) {
        this.reFlags = flags;
        isXPath = flags.isAllowsXPath20Extensions();
        isXPath30 = flags.isAllowsXPath30Extensions();
    }


    private void insertNode(Operation node, int insertAt) {
        instructions.add(insertAt, node);
    }

    private void warning(String s) {
        if (warnings == null) {
            warnings = new ArrayList<String>(4);
        }
        warnings.add(s);
    }

    /**
     * On completion of compilation, get any warnings that were generated
     * @return the list of warning messages
     */

    public List<String> getWarnings() {
        if (warnings == null) {
            return Collections.emptyList();
        } else {
            return warnings;
        }
    }

    /**
     * Appends a node to the end of a node chain
     *
     * @param node    Start of node chain to traverse
     * @param pointTo Node to have the tail of the chain point to
     */
    void setNextOfEnd(int node, int pointTo) {
        //System.err.println("NEW nextOfEnd " + node + " " + pointTo);
        // Traverse the chain until the next offset is 0
        int next = instructions.get(node).next;
        // while the 'node' is not the last in the chain
        // and the 'node' is not the last in the program.
        while (next != 0 && node < instructions.size()) {
            // if the node we are supposed to point to is in the chain then
            // point to the end of the program instead.
            // Michael McCallum <gholam@xtra.co.nz>
            // FIXME: This is a _hack_ to stop infinite programs.
            // I believe that the implementation of the reluctant matches is wrong but
            // have not worked out a better way yet.
            if (node == pointTo) {
                pointTo = instructions.size();
            }
            node += next;
            next = instructions.get(node).next;
        }

        // if we have reached the end of the program then dont set the pointTo.
        // im not sure if this will break any thing but passes all the tests.
        if (node < instructions.size()) {
            int offset = pointTo - node;

            // Point the last node in the chain to pointTo.
            instructions.get(node).next = offset;
        }
    }

//    /**
//     * Adds a new node
//     *
//     * @param opcode Opcode for node
//     * @param opdata Opdata for node
//     * @return Index of new node in program
//     */
//    int node(int opcode, int opdata) {
//        // Make room for a new node
//        ensure(RE.nodeSize);
//
//        // Add new node at end
//        instruction[lenInstruction /* + RE.offsetOpcode */] = opcode;
//        instruction[lenInstruction + RE.offsetOpdata] = opdata;
//        instruction[lenInstruction + RE.offsetNext] = 0;
//        lenInstruction += RE.nodeSize;
//
//        // Return index of new node
//        return lenInstruction - RE.nodeSize;
//    }


    /**
     * Throws a new internal error exception
     *
     * @throws Error Thrown in the event of an internal error.
     */
    void internalError() throws Error {
        throw new Error("Internal error!");
    }

    /**
     * Throws a new syntax error exception
     * @param s the error message
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     */
    void syntaxError(String s) throws RESyntaxException {
    	if (LogConfiguration.loggingIsEnabled()) {
    		throw new RESyntaxException(s, idx);
    	} else {
    		throw new RESyntaxException("", idx);
    	}
    }

    /**
     * Match bracket {m,n} expression put results in bracket member variables
     *
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     */
    void bracket() throws RESyntaxException {
        // Current character must be a '{'
        if (idx >= len || pattern.charAt(idx++) != '{') {
            internalError();
        }

        // Next char must be a digit
        if (idx >= len || !isAsciiDigit(pattern.charAt(idx))) {
            syntaxError("Expected digit");
        }

        // Get min ('m' of {m,n}) number
        StringBuffer number = new StringBuffer();
        while (idx < len && isAsciiDigit(pattern.charAt(idx))) {
            number.append((char)pattern.charAt(idx++));
        }
        try {
            bracketMin = Integer.parseInt(number.toString());
        } catch (NumberFormatException e) {
            syntaxError("Expected valid number");
        }

        // If out of input, fail
        if (idx >= len) {
            syntaxError("Expected comma or right bracket");
        }

        // If end of expr, optional limit is 0
        if (pattern.charAt(idx) == '}') {
            idx++;
            bracketOpt = 0;
            return;
        }

        // Must have at least {m,} and maybe {m,n}.
        if (idx >= len || pattern.charAt(idx++) != ',') {
            syntaxError("Expected comma");
        }

        // If out of input, fail
        if (idx >= len) {
            syntaxError("Expected comma or right bracket");
        }

        // If {m,} max is unlimited
        if (pattern.charAt(idx) == '}') {
            idx++;
            bracketOpt = bracketUnbounded;
            return;
        }

        // Next char must be a digit
        if (idx >= len || !isAsciiDigit(pattern.charAt(idx))) {
            syntaxError("Expected digit");
        }

        // Get max number
        number.setLength(0);
        while (idx < len && isAsciiDigit(pattern.charAt(idx))) {
            number.append((char)pattern.charAt(idx++));
        }
        try {
            bracketOpt = Integer.parseInt(number.toString()) - bracketMin;
        } catch (NumberFormatException e) {
            syntaxError("Expected valid number");
        }

        // Optional repetitions must be >= 0
        if (bracketOpt < 0) {
            syntaxError("Bad range");
        }

        // Must have close brace
        if (idx >= len || pattern.charAt(idx++) != '}') {
            syntaxError("Missing close brace");
        }
    }

    /**
     * Test whether a character is an ASCII decimal digit
     * @param ch the character to be matched
     * @return true if the character is an ASCII digit (0-9)
     */

    private static boolean isAsciiDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Match an escape sequence.  Handles quoted chars and octal escapes as well
     * as normal escape characters.  Always advances the input stream by the
     * right amount. This code "understands" the subtle difference between an
     * octal escape and a backref.  You can access the type of ESC_CLASS or
     * ESC_COMPLEX or ESC_BACKREF by looking at pattern[idx - 1].
     *
     * @return an IntPredicate that matches the character or characters represented
     * by this escape sequence. For a single-character escape this must be an IntValuePredicate
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     */
    IntPredicate escape(boolean inSquareBrackets) throws RESyntaxException {
        // "Shouldn't" happen
        if (pattern.charAt(idx) != '\\') {
            internalError();
        }

        // Escape shouldn't occur as last character in string!
        if (idx + 1 == len) {
            syntaxError("Escape terminates string");
        }

        // Switch on character after backslash
        idx += 2;
        int escapeChar = pattern.charAt(idx - 1);
        switch (escapeChar) {

            case 'n':
                return new IntValuePredicate('\n');
            case 'r':
                return new IntValuePredicate('\r');
            case 't':
                return new IntValuePredicate('\t');

            case '\\':
            case '|':
            case '.':
            case '-':
            case '^':
            case '?':
            case '*':
            case '+':
            case '{':
            case '}':
            case '(':
            case ')':
            case '[':
            case ']':
                return new IntValuePredicate(escapeChar);

            case '$':
                if (isXPath) {
                    return new IntValuePredicate(escapeChar);
                } else {
                    syntaxError("In XSD, '$' must not be escaped");
                }

            case 's':
                return MultiCharEscape.ESCAPE_s;

            case 'S':
                return MultiCharEscape.ESCAPE_S;

            case 'i':
                return MultiCharEscape.ESCAPE_i;

            case 'I':
                return MultiCharEscape.ESCAPE_I;

            case 'c':
                return MultiCharEscape.ESCAPE_c;

            case 'C':
                return MultiCharEscape.ESCAPE_C;

            case 'd':
                return MultiCharEscape.ESCAPE_d;

            case 'D':
                return MultiCharEscape.ESCAPE_D;

            case 'w':
                return MultiCharEscape.ESCAPE_w;

            case 'W':
                return MultiCharEscape.ESCAPE_W;


            case 'p':
            case 'P':

                if (idx == len) {
                    syntaxError("Expected '{' after \\" + escapeChar);
                }
                if (pattern.charAt(idx) != '{') {
                    syntaxError("Expected '{' after \\" + escapeChar);
                }
                int close = pattern.indexOf('}', idx++);
                if (close == -1) {
                    syntaxError("No closing '}' after \\" + escapeChar);
                }
                UnicodeString block = pattern.substring(idx, close);
                if (block.length() == 1 && block.charAt(0) < 256) {
                    IntPredicate primary = null;
                    try {
                        primary = MultiCharEscape.getCategoryCharClass((char)block.charAt(0));
                    } catch (IllegalArgumentException err) {
                        syntaxError(err.getMessage());
                    }
                    idx = close+1;
                    if (escapeChar == 'p') {
                        return primary;
                    } else {
                        return makeComplement(primary);
                    }
                } else if (block.length() == 2) {
                    IntPredicate primary = null;
                    try {
                        primary = new IntSetPredicate(MultiCharEscape.getSubCategoryCharClass(block.toString()));
                    } catch (IllegalArgumentException err) {
                        syntaxError(err.getMessage());
                    }
                    idx = close+1;
                    if (escapeChar == 'p') {
                        return primary;
                    } else {
                        return makeComplement(primary);
                    }
                } else if (block.toString().startsWith("Is")) {
                    String blockName = block.toString().substring(2);
                    IntSet uniBlock = UnicodeBlocks.getBlock(blockName);
                    if (uniBlock == null) {
                        // XSD 1.1 says this is not an error
                        warning("Unknown Unicode block: " + blockName);
                        idx = close+1;
                        return new IntSetPredicate(IntUniversalSet.getInstance());
                    }
                    idx = close+1;
                    IntPredicate primary = new IntSetPredicate(uniBlock);
                    if (escapeChar == 'p') {
                        return primary;
                    } else {
                        return makeComplement(primary);
                    }
                } else {
                    syntaxError("Unknown block: " + block);
                }

            case '0':
                syntaxError("Octal escapes not allowed");

            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':

                if (inSquareBrackets) {
                    syntaxError("Backreference not allowed within character class");
                } else if (isXPath) {
                    int backRef = (escapeChar - '0');
                    while (idx < len) {
                        int c1 = "0123456789".indexOf(pattern.charAt(idx));
                        if (c1 < 0) {
                            break;
                        } else {
                            int backRef2 = backRef * 10 + c1;
                            if (backRef2 > parens) {
                                break;
                            } else {
                                backRef = backRef2;
                                idx++;
                            }
                        }

                    }
                    if (!captures.contains(backRef)) {
                        String explanation = (backRef > parens ? "(no such group)" : "(group not yet closed)");
                        syntaxError("invalid backreference \\" + backRef + " " + explanation);
                    }
                    return new BackReference(backRef);
                } else {
                    syntaxError("digit not allowed after \\");
                }

            default:

                // Other characters not allowed in XSD regexes
                syntaxError("Escape character '" + (char)escapeChar + "' not allowed");
        }
        return null;
    }

    /**
     * For convenience a back-reference is treated as an IntPredicate, although this a fiction
     */

    class BackReference extends IntValuePredicate {
        public BackReference(int number) {
            super(number);
        }
    }


    /**
     * Compile a character class (in square brackets)
     *
     * @return an IntPredicate that tests whether a character matches this character class
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     */
    IntPredicate parseCharacterClass() throws RESyntaxException {
        // Check for bad calling or empty class
        if (pattern.charAt(idx) != '[') {
            internalError();
        }

        // Check for unterminated or empty class
        if ((idx + 1) >= len || pattern.charAt(++idx) == ']') {
            syntaxError("Missing ']'");
        }

        // Parse class declaration
        int simpleChar;
        boolean positive = true;
        boolean definingRange = false;
        int rangeStart = -1;
        int rangeEnd;
        IntRangeSet range = new IntRangeSet();
        IntPredicate addend = null;
        IntPredicate subtrahend = null;
        if (thereFollows("^")) {
            if (thereFollows("^-[")) {
                syntaxError("Nothing before subtraction operator");
            } else if (thereFollows("^]")) {
                syntaxError("Empty negative character group");
            } else {
                positive = false;
                idx++;
            }
        } else if (thereFollows("-[")) {
            syntaxError("Nothing before subtraction operator");
        }
        while (idx < len && pattern.charAt(idx) != ']') {
            int ch = pattern.charAt(idx);
            simpleChar = -1;
            switch (ch) {
                case '[':
                    syntaxError("Unescaped '[' within square brackets");
                    break;
                case '\\': {
                    // Escape always advances the stream
                    IntPredicate cc = escape(true);
                    if (cc instanceof IntValuePredicate) {
                        simpleChar = ((IntValuePredicate) cc).getTarget();
                        break;
                    } else {
                        if (definingRange) {
                            syntaxError("Multi-character escape cannot follow '-'");
                        } else if (addend == null) {
                            addend = cc;
                        } else {
                            addend = makeUnion(addend, cc);
                        }
                        continue;
                    }
                }
                case '-':
                    if (thereFollows("-[")) {
                        idx++;
                        subtrahend = parseCharacterClass();
                        if (!thereFollows("]")) {
                            syntaxError("Expected closing ']' after subtraction");
                        }
                    } else if (thereFollows("-]")) {
                        simpleChar = '-';
                        idx++;
                    } else if (rangeStart >= 0) {
                        definingRange = true;
                        idx++;
                        continue;
                    } else if (definingRange) {
                        syntaxError("Bad range");
                    } else if (thereFollows("--") && !thereFollows("--[")) {
                        syntaxError("Unescaped hyphen as start of range");
                    } else {
                        simpleChar = '-';
                        idx++;
                    }
                    break;

                default:
                    simpleChar = ch;
                    idx++;
                    break;
            }

            // Handle simple character simpleChar
            if (definingRange) {
                // if we are defining a range make it now
                rangeEnd = simpleChar;

                // Actually create a range if the range is ok
                if (rangeStart > rangeEnd) {
                    syntaxError("Bad character range: start > end");
                    // TODO: not an error in XSD, merely a no-op?
                }
                range.addRange(rangeStart, rangeEnd);
                if (reFlags.isCaseIndependent()) {
                    // Special-case A-Z and a-z
                    if (rangeStart == 'a' && rangeEnd == 'z') {
                        range.addRange('A', 'Z');
                        for (int v=0; v<CaseVariants.ROMAN_VARIANTS.length; v++) {
                            range.add(CaseVariants.ROMAN_VARIANTS[v]);
                        }
                    } else if (rangeStart == 'A' && rangeEnd == 'Z') {
                        range.addRange('a', 'z');
                        for (int v=0; v<CaseVariants.ROMAN_VARIANTS.length; v++) {
                            range.add(CaseVariants.ROMAN_VARIANTS[v]);
                        }
                    } else {
                        for (int k = rangeStart; k <= rangeEnd; k++) {
                            int[] variants = CaseVariants.getCaseVariants(k);
                            for (int variant : variants) {
                                range.add(variant);
                            }
                        }
                    }
                }

                // We are done defining the range
                definingRange = false;
                rangeStart = -1;
            } else {
                // If simple character and not start of range, include it (see XSD 1.1 rules)
                if (thereFollows("-")) {
                    if (thereFollows("-[")) {
                        range.add(simpleChar);
                    } else if (thereFollows("-]")) {
                        range.add(simpleChar);
                    } else if (thereFollows("--[")) {
                        range.add(simpleChar);
                    } else if (thereFollows("--")) {
                        syntaxError("Unescaped hyphen cannot act as end of range");
                    } else {
                        rangeStart = simpleChar;
                    }
                } else {
                    range.add(simpleChar);
                    if (reFlags.isCaseIndependent()) {
                        int[] variants = CaseVariants.getCaseVariants(simpleChar);
                        for (int variant : variants) {
                            range.add(variant);
                        }
                    }
                }
            }
        }

        // Shouldn't be out of input
        if (idx == len) {
            syntaxError("Unterminated character class");
        }

        // Absorb the ']' end of class marker
        idx++;
        IntPredicate result = new IntSetPredicate(range);
        if (addend != null) {
            result = makeUnion(result, addend);
        }
        if (!positive) {
            result = makeComplement(result);
        }
        if (subtrahend != null) {
            result = makeDifference(result, subtrahend);
        }
        return result;
    }

    /**
     * Test whether the string starting at the current position is equal to some specified string
     * @param s the string being tested
     * @return true if the specified string is present
     */

    private boolean thereFollows(String s) {
        return idx + s.length() <= len &&
                (pattern.substring(idx, idx + s.length()).toString().equals(s));
    }

    /**
     * Make the union of two IntPredicates (matches if p1 matches or p2 matches)
     * @param p1 the first
     * @param p2 the second
     * @return the result
     */

    private IntPredicate makeUnion(IntPredicate p1, IntPredicate p2) {
        if (p1 instanceof IntSetPredicate && ((IntSetPredicate)p1).getIntSet(). isEmpty()) {
            return p2;
        }
        if (p2 instanceof IntSetPredicate && ((IntSetPredicate)p2).getIntSet(). isEmpty()) {
            return p1;
        }
        return new IntUnionPredicate(p1, p2);
    }

    /**
     * Make the difference of two IntPredicates (matches if p1 matches and p2 does not match)
     * @param p1 the first
     * @param p2 the second
     * @return the result
     */

    private IntPredicate makeDifference(IntPredicate p1, IntPredicate p2) {
        return new IntExceptPredicate(p1, p2);
    }

    /**
     * Make the complement of an IntPredicate (matches if p1 does not match)
     * @param p1 the operand
     * @return the result
     */

    private IntPredicate makeComplement(IntPredicate p1) {
        if (p1 instanceof IntComplementPredicate) {
            return ((IntComplementPredicate)p1).getOperand();
        } else {
            return new IntComplementPredicate(p1);
        }
    }

    private int emitCharacterClass(IntPredicate range) {
        Operation.OpCharClass node = new Operation.OpCharClass();
        node.predicate = range;
        return appendNode(node);
    }

    /**
     * Absorb an atomic character string.  This method is a little tricky because
     * it can un-include the last character of string if a quantifier operator follows.
     * This is correct because *+? have higher precedence than concatentation (thus
     * ABC* means AB(C*) and NOT (ABC)*).
     *
     * @return Index of new atom node
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     */
    int atom() throws RESyntaxException {
        // Create a string node
        Operation.OpAtom node = new Operation.OpAtom();

        // Length of atom
        int lenAtom = 0;

        // Loop while we've got input

        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);

        atomLoop:

        while (idx < len) {
            // Is there a next char?
            if ((idx + 1) < len) {
                int c = pattern.charAt(idx + 1);

                // If the next 'char' is an escape, look past the whole escape
                if (pattern.charAt(idx) == '\\') {
                    int idxEscape = idx;
                    escape(false);
                    if (idx < len) {
                        c = pattern.charAt(idx);
                    }
                    idx = idxEscape;
                }

                // Switch on next char
                switch (c) {
                    case '{':
                    case '?':
                    case '*':
                    case '+':

                        // If the next character is a quantifier operator and our atom is non-empty, the
                        // current character should bind to the quantifier operator rather than the atom
                        if (lenAtom != 0) {
                            break atomLoop;
                        }
                }
            }

            // Switch on current char
            switch (pattern.charAt(idx)) {
                case ']':
                case '.':
                case '[':
                case '(':
                case ')':
                case '|':
                    break atomLoop;

                case '{':
                case '?':
                case '*':
                case '+':

                    // We should have an atom by now
                    if (lenAtom == 0) {
                        // No atom before quantifier
                        syntaxError("No expression before quantifier");
                    }
                    break atomLoop;

                case '\\': {
                    // Get the escaped character (advances input automatically)
                    int idxBeforeEscape = idx;
                    IntPredicate charClass = escape(false);

                    // Check if it's a simple escape (as opposed to, say, a backreference)
                    if (charClass instanceof BackReference || !(charClass instanceof IntValuePredicate)) {
                        // Not a simple escape, so backup to where we were before the escape.
                        idx = idxBeforeEscape;
                        break atomLoop;
                    }

                    // Add escaped char to atom
                    fsb.appendWideChar(((IntValuePredicate) charClass).getTarget());
                    lenAtom++;
                    break;
                }

                case '^':
                case '$':
                    if (isXPath) {
                        break atomLoop;
                    }
                    // else fall through ($ is not a metacharacter in XSD)

                default:

                    // Add normal character to atom
                    fsb.appendWideChar(pattern.charAt(idx++));
                    lenAtom++;
                    break;
            }
        }

        // This shouldn't happen
        if (fsb.length() == 0) {
            internalError();
        }

        // Emit the instruction into the program
        node.atom = GeneralUnicodeString.makeUnicodeString(fsb.condense());
        return appendNode(node);
    }

    private int appendNode(Operation node) {
        instructions.add(node);
        return instructions.size()-1;
    }


    /**
     * Match a terminal node.
     *
     * @param flags Flags
     * @return Index of terminal node (closeable)
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     */
    int terminal(int[] flags) throws RESyntaxException {
        switch (pattern.charAt(idx)) {
            case '$':
                if (isXPath) {
                    idx++;
                    Operation.OpEOL eol = new Operation.OpEOL();
                    return appendNode(eol);
                }
                break;

            case '^':
                if (isXPath) {
                    idx++;
                    Operation.OpBOL bol = new Operation.OpBOL();
                    return appendNode(bol);
                }
                break;

            case '.':
                idx++;
                IntPredicate predicate;
                if (reFlags.isSingleLine()) {
                    // in XPath with the 's' flag, '.' matches everything
                    predicate = new IntPredicate() {
                        public boolean matches(int value) {
                            return true;
                        }
                    };
                } else {
                    // in XSD, "." matches everything except \n and \r. See also bug 15594.
                    predicate = new IntPredicate() {
                        public boolean matches(int value) {
                            return (value != '\n' && value != '\r');
                        }
                    };
                }
                Operation.OpCharClass dot = new Operation.OpCharClass();
                dot.predicate = predicate;
                return appendNode(dot);

            case '[':
                IntPredicate range = parseCharacterClass();
                Operation.OpCharClass cc = new Operation.OpCharClass();
                cc.predicate = range;
                return appendNode(cc);

            case '(':
                return expr(flags);

            case ')':
                syntaxError("Unexpected close paren");

            case '|':
                internalError();

            case ']':
                syntaxError("Mismatched class");

            case 0:
                syntaxError("Unexpected end of input");

            case '?':
            case '+':
            case '{':
            case '*':
                syntaxError("No expression before quantifier");

            case '\\': {
                // Don't forget, escape() advances the input stream!
                int idxBeforeEscape = idx;

                IntPredicate esc = escape(false);

                if (esc instanceof BackReference) {
                    int backreference = ((BackReference)esc).getTarget();
                    if (parens <= backreference) {
                        syntaxError("Bad backreference");
                    }
                    flags[0] |= NODE_NULLABLE;
                    Operation.OpBackReference back = new Operation.OpBackReference();
                    back.groupNr = backreference;
                    return appendNode(back);

                } else if (esc instanceof IntSingletonSet) {
                    // We had a simple escape and we want to have it end up in
                    // an atom, so we back up and fall though to the default handling
                    idx = idxBeforeEscape;
                    flags[0] &= ~NODE_NULLABLE;

                } else {

                    flags[0] &= ~NODE_NULLABLE;
                    return emitCharacterClass(esc);
                    //return node(RE.OP_ESCAPE, pattern.charAt(idx - 1));
                }

            }
        }

        // Everything above either fails or returns.
        // If it wasn't one of the above, it must be the start of an atom.
        flags[0] &= ~NODE_NULLABLE;
        return atom();
    }

    /**
     * Compile a piece consisting of an atom and optional quantifier
     *
     * @param flags Flags passed by reference
     * @return Index of resulting instruction
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     */
    int piece(int[] flags) throws RESyntaxException {
        // Before terminal
        int idxBeforeTerminal = idx;

        // Values to pass by reference to terminal()
        int[] terminalFlags = {NODE_NORMAL};

        // Get terminal symbol
        int ret = terminal(terminalFlags);

        // Or in flags from terminal symbol
        flags[0] |= terminalFlags[0];

        // Advance input, set NODE_NULLABLE flag and do sanity checks
        if (idx >= len) {
            return ret;
        }

        boolean greedy = true;
        int quantifierType = pattern.charAt(idx);
        switch (quantifierType) {
            case '?':
            case '*':

                // The current node can be null
                flags[0] |= NODE_NULLABLE;

                // Drop through

            case '+':

                // Eat quantifier character
                idx++;

                // Drop through

            case '{':

                if (quantifierType == '{') {
                    bracket();
                }

                Operation op = instructions.get(ret);
                if (op instanceof Operation.OpBOL || op instanceof Operation.OpEOL) {
                    // Pretty meaningless, but legal. If the quantifier allows zero occurrences, ignore the instruction.
                    // Otherwise, ignore the quantifier
                    if (quantifierType == '?' || quantifierType == '*' ||
                            (quantifierType == '{' && bracketMin == 0)) {
                        instructions.set(ret, new Operation.OpNothing());
                    } else {
                        quantifierType = 0;
                    }
                }
                if ((terminalFlags[0] & NODE_NULLABLE) != 0) {
                    if (quantifierType == '?') {
                        // can ignore the quantifier
                        quantifierType = 0;
                    } else if (quantifierType == '+') {
                        // '*' and '+' are equivalent
                        quantifierType = '*';
                    } else if (quantifierType == '{') {
                        // bounds are meaningless
                        quantifierType = '*';
                    }
                }

        }

        // If the next character is a '?', make the quantifier non-greedy (reluctant)
        if (idx < len && pattern.charAt(idx) == '?') {
            if (!isXPath) {
                syntaxError("Reluctant quantifiers are not allowed in XSD");
            }
            idx++;
            greedy = false;
        }

        if (greedy) {
            // Actually do the quantifier now
            switch (quantifierType) {
                case '{': {
                    //bracket();
                    int bracketEnd = idx;
                    int bracketMin = this.bracketMin;
                    int bracketOpt = this.bracketOpt;

                    // Pointer to the last terminal
                    int pos = ret;

                    // Process min first
                    for (int c = 0; c < bracketMin; c++) {
                        // Rewind stream and run it through again - more matchers coming
                        idx = idxBeforeTerminal;
                        setNextOfEnd(pos, pos = terminal(terminalFlags));
                    }

                    // Do the right thing for maximum ({m,})
                    if (bracketOpt == bracketUnbounded) {
                        // Drop through now and quantifier expression.
                        // We are done with the {m,} expr, so skip rest
                        idx = bracketEnd;
                        Operation.OpStar op = new Operation.OpStar();
                        insertNode(op, pos);
                        setNextOfEnd(pos + 1, pos);
                        break;
                    } else if (bracketOpt > 0) {
                        int opt[] = new int[bracketOpt + 1];
                        // Surround first optional terminal with MAYBE
                        Operation.OpMaybe op = new Operation.OpMaybe();
                        insertNode(op, pos);
                        opt[0] = pos;

                        // Add all the rest optional terminals with preceding MAYBEs
                        for (int c = 1; c < bracketOpt; c++) {
                            op = new Operation.OpMaybe();
                            opt[c] = appendNode(op);
                            // Rewind stream and run it through again - more matchers coming
                            idx = idxBeforeTerminal;
                            terminal(terminalFlags);
                        }

                        // Tie ends together
                        int end = opt[bracketOpt] = appendNode(new Operation.OpNothing());
                        for (int c = 0; c < bracketOpt; c++) {
                            setNextOfEnd(opt[c], end);
                            setNextOfEnd(opt[c] + 1, opt[c + 1]);
                        }
                    } else {
                        // Rollback terminal - no opt matchers present
                        //lenInstruction = pos;
                        while (instructions.size() > pos) {
                            instructions.remove(instructions.size()-1);
                        }
                        Operation.OpNothing nothing = new Operation.OpNothing();
                        appendNode(nothing);
                    }

                    // We are done. skip the reminder of {m,n} expr
                    idx = bracketEnd;
                    break;
                }

                case '?': {
                    Operation.OpMaybe maybe = new Operation.OpMaybe();
                    insertNode(maybe, ret);
                    Operation.OpNothing nothing = new Operation.OpNothing();
                    int n = appendNode(nothing);
                    setNextOfEnd(ret, n);
                    setNextOfEnd(ret + 1, n);
                    break;
                }

                case '*': {
                    Operation.OpStar star = new Operation.OpStar();
                    insertNode(star, ret);
                    setNextOfEnd(ret + 1, ret);
                    break;
                }

                case '+': {
                    Operation.OpContinue continu = new Operation.OpContinue();
                    insertNode(continu, ret);
                    Operation.OpPlus plus = new Operation.OpPlus();
                    int n = appendNode(plus);
                    setNextOfEnd(ret + 1, n);
                    setNextOfEnd(n, ret);
                    break;
                }
            }
        } else {
            // Not greedy (reluctant): Actually do the quantifier now
            switch (quantifierType) {
                case '?': {
                    Operation.OpReluctantMaybe reluctantMaybe = new Operation.OpReluctantMaybe();
                    insertNode(reluctantMaybe, ret);
                    //nodeInsert(RE.OP_RELUCTANTMAYBE, 0, ret);
                    int n = appendNode(new Operation.OpNothing());
                    //int n = node(RE.OP_NOTHING, 0);
                    setNextOfEnd(ret, n);
                    setNextOfEnd(ret + 1, n);
                    break;
                }

                case '*': {
                    Operation.OpReluctantStar reluctantStar = new Operation.OpReluctantStar();
                    insertNode(reluctantStar, ret);
                    setNextOfEnd(ret + 1, ret);
                    break;
                }

                case '+': {
                    insertNode(new Operation.OpContinue(), ret);
                    //nodeInsert(RE.OP_CONTINUE, 0, ret);
                    int n = appendNode(new Operation.OpReluctantPlus());
                    //int n = node(RE.OP_RELUCTANTPLUS, 0);
                    setNextOfEnd(n, ret);
                    setNextOfEnd(ret + 1, n);
                    break;
                }

                case '{': {
                    // reluctant {..}? - added by MHK
                    //bracket();
                    int bracketEnd = idx;
                    int bracketMin = this.bracketMin;
                    int bracketOpt = this.bracketOpt;

                    // Pointer to the last terminal
                    int pos = ret;

                    // Process min first
                    for (int c = 0; c < bracketMin; c++) {
                        // Rewind stream and run it through again - more matchers coming
                        idx = idxBeforeTerminal;
                        setNextOfEnd(pos, pos = terminal(terminalFlags));
                    }

                    // Do the right thing for maximum ({m,})
                    if (bracketOpt == bracketUnbounded) {
                        // Drop through now and quantifier expression.
                        // We are done with the {m,} expr, so skip rest
                        idx = bracketEnd;
                        insertNode(new Operation.OpReluctantStar(), pos);
                        //nodeInsert(RE.OP_RELUCTANTSTAR, 0, pos);
                        setNextOfEnd(pos + 1, pos);
                        break;
                    } else if (bracketOpt > 0) {
                        int opt[] = new int[bracketOpt + 1];
                        // Surround first optional terminal with MAYBE
                        insertNode(new Operation.OpReluctantMaybe(), pos);
                        //nodeInsert(RE.OP_RELUCTANTMAYBE, 0, pos);
                        opt[0] = pos;

                        // Add all the rest optional terminals with preceeding MAYBEs
                        for (int c = 1; c < bracketOpt; c++) {
                            opt[c] = appendNode(new Operation.OpReluctantMaybe());
                            //opt[c] = node(RE.OP_RELUCTANTMAYBE, 0);
                            // Rewind stream and run it through again - more matchers coming
                            idx = idxBeforeTerminal;
                            terminal(terminalFlags);
                        }

                        // Tie ends together
                        int end = opt[bracketOpt] = appendNode(new Operation.OpNothing());
                        for (int c = 0; c < bracketOpt; c++) {
                            setNextOfEnd(opt[c], end);
                            setNextOfEnd(opt[c] + 1, opt[c + 1]);
                        }
                    } else {
                        // Rollback terminal - no opt matchers present
                        while (instructions.size() > pos) {
                            instructions.remove(instructions.size() - 1);
                        }
                        appendNode(new Operation.OpNothing());
                    }

                    // We are done. skip the reminder of {m,n} expr
                    idx = bracketEnd;
                    break;
                }
            }
        }

        return ret;
    }

    /**
     * Compile body of one branch of an or operator (implements concatenation)
     *
     * @param compilerFlags Flags passed by reference
     * @return Pointer to first node in the branch
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     */
    int branch(int[] compilerFlags) throws RESyntaxException {
        // Get each possibly qnatified piece and concat
        int node;
        int ret = -1;
        int chain = -1;
        int[] quantifierFlags = new int[1];
        boolean nullable = true;
        while (idx < len && pattern.charAt(idx) != '|' && pattern.charAt(idx) != ')') {
            // Get new node
            quantifierFlags[0] = NODE_NORMAL;
            node = piece(quantifierFlags);
            if (quantifierFlags[0] == NODE_NORMAL) {
                nullable = false;
            }

            // If there's a chain, append to the end
            if (chain != -1) {
                setNextOfEnd(chain, node);
            }

            // Chain starts at current
            chain = node;
            if (ret == -1) {
                ret = node;
            }
        }

        // If we don't run loop, make a nothing node
        if (ret == -1) {
            Operation nothing = new Operation.OpNothing();
            ret = appendNode(nothing);
        }

        // Set nullable flag for this branch
        if (nullable) {
            compilerFlags[0] |= NODE_NULLABLE;
        }

        return ret;
    }

    /**
     * Compile an expression with possible parens around it.  Paren matching
     * is done at this level so we can tie the branch tails together.
     *
     * @param compilerFlags Flag value passed by reference
     * @return Node index of expression in instruction array
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     */
    int expr(int[] compilerFlags) throws RESyntaxException {
        // Create open paren node unless we were called from the top level (which has no parens)
        int paren = -1;
        int ret = -1;
        int closeParens = parens;
        if ((compilerFlags[0] & NODE_TOPLEVEL) == 0 && pattern.charAt(idx) == '(') {
            // if its a cluster ( rather than a proper subexpression ie with backrefs )
            if (idx + 2 < len && pattern.charAt(idx + 1) == '?' && pattern.charAt(idx + 2) == ':') {
                if (!isXPath30) {
                    syntaxError("Non-capturing groups allowed only in XPath3.0");
                }
                paren = 2;
                idx += 3;
                ret = appendNode(new Operation.OpOpenCluster());
            } else {
                paren = 1;
                idx++;
                ret = appendNode(new Operation.OpOpen(parens++));
            }
        }
        compilerFlags[0] &= ~NODE_TOPLEVEL;

        // Process contents of first branch node
        boolean open = false;
        int branch = branch(compilerFlags);
        if (ret == -1) {
            ret = branch;
        } else {
            setNextOfEnd(ret, branch);
        }

        // Loop through branches
        while (idx < len && pattern.charAt(idx) == '|') {
            // Now open the first branch since there are more than one
            if (!open) {
                Operation.OpBranch op = new Operation.OpBranch();
                insertNode(op, branch);
                open = true;
            }

            idx++;
            setNextOfEnd(branch, branch = appendNode(new Operation.OpBranch()));
            branch(compilerFlags);
        }

        // Create an ending node (either a close paren or an OP_END)
        int end;
        if (paren > 0) {
            if (idx < len && pattern.charAt(idx) == ')') {
                idx++;
            } else {
                syntaxError("Missing close paren");
            }
            if (paren == 1) {
                end = appendNode(new Operation.OpClose(closeParens));
                captures.add(closeParens);
            } else {
                end = appendNode(new Operation.OpCloseCluster());
            }
        } else {
            end = appendNode(new Operation.OpEndProgram());
        }

        // Append the ending node to the ret nodelist
        setNextOfEnd(ret, end);

        // Hook the ends of each branch to the end node
        int currentNode = ret;
        int nextNodeOffset = instructions.get(currentNode).next;
        // while the next node o
        while (nextNodeOffset != 0 && currentNode < instructions.size()) {
            // If branch, make the end of the branch's operand chain point to the end node.
            if (instructions.get(currentNode) instanceof Operation.OpBranch) {
                setNextOfEnd(currentNode + 1, end);
            }
            nextNodeOffset = instructions.get(currentNode).next;
            currentNode += nextNodeOffset;
        }

        // Return the node list
        return ret;
    }

    /**
     * Compiles a regular expression pattern into a program runnable by the pattern
     * matcher class 'RE'.
     *
     * @param pattern Regular expression pattern to compile (see RECompiler class
     *                for details).
     * @return A compiled regular expression program.
     * @throws RESyntaxException Thrown if the regular expression has invalid syntax.
     * @see RECompiler
     * @see REMatcher
     */
    public REProgram compile(UnicodeString pattern) throws RESyntaxException {
        // Initialize variables for compilation
        this.pattern = pattern;                         // Save pattern in instance variable
        len = pattern.length();                         // Precompute pattern length for speed
        idx = 0;                                        // Set parsing index to the first character
        parens = 1;                                     // Set paren level to 1 (the implicit outer parens)
        boolean nullable = false;

        if (reFlags.isLiteral()) {

            // 'q' flag is set
            int ret = literalAtom();
            Operation.OpEndProgram endNode = new Operation.OpEndProgram();
            int end = appendNode(endNode);
            setNextOfEnd(ret, end);

        } else {

            if (reFlags.isAllowWhitespace()) {
                // 'x' flag is set. Preprocess the expression to strip whitespace, other than between
                // square brackets
                FastStringBuffer sb = new FastStringBuffer(pattern.length());
                int nesting = 0;
                boolean astral = false;
                boolean escaped = false;
                for (int i=0; i<pattern.length(); i++) {
                    int ch = pattern.charAt(i);
                    if (ch > 65535) {
                        astral = true;
                    }
                    if (ch == '\\' && !escaped) {
                        escaped = true;
                        sb.appendWideChar(ch);
                    } else if (ch == '[' && !escaped) {
                        nesting++;
                        escaped = false;
                        sb.appendWideChar(ch);
                    } else if (ch == ']' && !escaped) {
                        nesting--;
                        escaped = false;
                        sb.appendWideChar(ch);
                    } else if (nesting==0 && Whitespace.isWhitespace(ch)) {
                        // no action
                    } else {
                        escaped = false;
                        sb.appendWideChar(ch);
                    }
                }
                if (astral) {
                    pattern = new GeneralUnicodeString(sb);
                } else {
                    pattern = new BMPString(sb);
                }
                this.pattern = pattern;
                this.len = pattern.length();
            }

            // Initialize pass by reference flags value
            int[] compilerFlags = {NODE_TOPLEVEL};

            // Parse expression
            expr(compilerFlags);

            nullable = (compilerFlags[0] & NODE_NULLABLE) != 0;

            // Should be at end of input
            if (idx != len) {
                if (pattern.charAt(idx) == ')') {
                    syntaxError("Unmatched close paren");
                }
                syntaxError("Unexpected input remains");
            }

        }

        // Return the result
        Operation[] ops = new Operation[instructions.size()];
        for (int i=0; i<instructions.size(); i++) {
            // convert relative offsets in "next" pointer to absolute offsets (with -1 meaning null)
            Operation op = instructions.get(i);
            if (op.next == 0) {
                op.next = -1;
            } else {
                op.next += i;
            }
            ops[i] = op;
        }
        REProgram program = new REProgram(ops, parens, reFlags);

        if (reFlags.isDebug()) {
            program.display(System.err);
            //throw new AssertionError("terminated by request");
        }

        program.setNullable(nullable);

        return program;
    }

    /**
     * Process a "regular expression" with the q flag set. This is simply handled as an atom, where
     * no characters are treated as special (i.e. all are treated as if escaped)
     *
     * @return Index of new atom node
     */
    int literalAtom() {
        // Create a string node
        Operation.OpAtom node = new Operation.OpAtom();
        node.atom = pattern;
        return appendNode(node);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // DIAGNOSTIC CODE
    ///////////////////////////////////////////////////////////////////////////////////////////////



    /**
     * Return a string describing a (possibly unprintable) character.
     *
     * @param c Character to convert to a printable representation
     * @return String representation of character
     */
    String charToString(char c) {
        // If it's unprintable, convert to '\###'
        if (c < ' ' || c > 127) {
            return "\\" + (int) c;
        }

        // Return the character as a string
        return String.valueOf(c);
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

