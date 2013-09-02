package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Value;

import java.util.HashMap;

/**
 * This class contains static data tables defining the properties of standard functions. "Standard functions"
 * here means the XPath 2.0 functions, the XSLT 2.0 functions, and a few selected extension functions
 * which need special recognition.
 */

public abstract class StandardFunction {

    public static Value EMPTY = EmptySequence.getInstance();

    /**
     * Categories of functions, bit significant
     */

    public static final int CORE = 1;
    public static final int XSLT = 2;
    public static final int USE_WHEN = 4;

    /**
     * This class is never instantiated
     */

    private StandardFunction() {
    }

    /**
     * Register a system function in the table of function details.
     *
     * @param name          the function name
     * @param skeleton      instance of the class used to implement the function
     * @param type          the sequence type of the result of the function
     * @param argTypes      information about the argument types, expressed using a micro-syntax as follows.
     * There is a sequence of space-separated entries, one per argument. If the argument is optional, this
     * starts with "?". The rest of the string is either "*", meaning any type allowed, or an item type followed
     * by an occurrence indicator. The item type is for example i=integer, a=atomic, d=double, s=string, n=numeric,
     * N=node, E=element, I=item, dt=dateTime, dur=duration, tim=time, dtd=dayTimeDuration, q=qName.
     * @return the entry describing the function. The entry is incomplete, it does not yet contain information
     *         about the function arguments.
     */

    public static Entry register(String name,
                                 SystemFunction skeleton,
                                 SequenceType type,
                                 String argTypes) {
        Entry e = makeEntry(name, skeleton, type, CORE);
        functionTable.put(name, e);
        if (!argTypes.equals("")) {
            String[] args = argTypes.split("\\s");
            for (String arg : args) {
                if (arg.equals("*")) {
                    e.mandatoryArg(SequenceType.ANY_SEQUENCE);
                } else {
                    boolean optional = false;
                    String it;
                    if (arg.startsWith("?")) {
                        optional = true;
                        arg = arg.substring(1);
                    }
                    int card = StaticProperty.EXACTLY_ONE;
                    if (arg.endsWith("*")) {
                        card = StaticProperty.ALLOWS_ZERO_OR_MORE;
                        it = arg.substring(0, arg.length() - 1);
                    } else if (arg.endsWith("?")) {
                        card = StaticProperty.ALLOWS_ZERO_OR_ONE;
                        it = arg.substring(0, arg.length() - 1);
                    } else if (arg.endsWith("+")) {
                        card = StaticProperty.ALLOWS_ONE_OR_MORE;
                        it = arg.substring(0, arg.length() - 1);
                    } else {
                        it = arg;
                    }
                    ItemType t;
                    if (it.equals("I")) {
                        t = AnyItemType.getInstance();
                    } else if (it.equals("N")) {
                        t = AnyNodeTest.getInstance();
                    } else if (it.equals("E")) {
                        t = NodeKindTest.ELEMENT;
                    } else if (it.equals("a")) {
                        t = BuiltInAtomicType.ANY_ATOMIC;
                    } else if (it.equals("i")) {
                        t = BuiltInAtomicType.INTEGER;
                    } else if (it.equals("d")) {
                        t = BuiltInAtomicType.DOUBLE;
                    } else if (it.equals("n")) {
                        t = BuiltInAtomicType.NUMERIC;
                    } else if (it.equals("s")) {
                        t = BuiltInAtomicType.STRING;
                    } else if (it.equals("b")) {
                        t = BuiltInAtomicType.BOOLEAN;
                    } else if (it.equals("dt")) {
                        t = BuiltInAtomicType.DATE_TIME;
                    } else if (it.equals("dat")) {
                        t = BuiltInAtomicType.DATE;
                    } else if (it.equals("tim")) {
                        t = BuiltInAtomicType.TIME;
                    } else if (it.equals("dur")) {
                        t = BuiltInAtomicType.DURATION;
                    } else if (it.equals("dtd")) {
                        t = BuiltInAtomicType.DAY_TIME_DURATION;
                    } else if (it.equals("q")) {
                        t = BuiltInAtomicType.QNAME;
                    } else {
                        throw new IllegalArgumentException(it);
                    }
                    SequenceType st = SequenceType.makeSequenceType(t, card);
                    if (optional) {
                        e.optionalArg(st);
                    } else {
                        e.mandatoryArg(st);
                    }
                }
            }
        }
        return e;
    }

    /**
     * Make a table entry describing the signature of a function, with a reference to the implementation class.
     *
     * @param name         the function name
     * @param skeleton     instance of the class used to implement the function
     * @param type         the sequence type of the result of the function
     * @param applicability the host languages to which the function is applicable
     * @return the entry describing the function. The entry is incomplete, it does not yet contain information
     *         about the function arguments.
     */
    public static Entry makeEntry(String name, SystemFunction skeleton,
                                  SequenceType type, int applicability) {
        Entry e = new Entry();
        int hash = name.indexOf('#');
        if (hash < 0) {
            e.name = name;
        } else {
            e.name = name.substring(0, hash);
        }
        e.skeleton = skeleton;
        e.minArguments = 0;
        e.maxArguments = 0;
        e.resultType = type;
        e.applicability = applicability;
        e.argumentTypes = new SequenceType[0];
        e.sameItemTypeAsFirstArgument = false;
        return e;
    }



    private static HashMap<String, Entry> functionTable = new HashMap<String, Entry>(200);

    static {
        Entry e;
        e = register("abs", new Rounding(Rounding.ABS), SequenceType.OPTIONAL_NUMERIC, "n?");
        e.sameItemTypeAsFirstArgument = true;
//        e.mandatoryArg(SequenceType.OPTIONAL_NUMERIC);

        register("adjust-date-to-timezone", new Adjust(), SequenceType.OPTIONAL_DATE, "dat? ?dtd?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE);
//        e.optionalArg(SequenceType.OPTIONAL_DAY_TIME_DURATION);

        register("adjust-dateTime-to-timezone", new Adjust(), SequenceType.OPTIONAL_DATE_TIME, "dt? ?dtd?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE_TIME);
//        e.optionalArg(SequenceType.OPTIONAL_DAY_TIME_DURATION);

        register("adjust-time-to-timezone", new Adjust(), SequenceType.OPTIONAL_TIME, "tim? ?dtd?");
//        e.mandatoryArg(SequenceType.OPTIONAL_TIME);
//        e.optionalArg(SequenceType.OPTIONAL_DAY_TIME_DURATION);

        register("avg", new Average(), SequenceType.OPTIONAL_ATOMIC, "a*");
        // can't say "same as first argument" because the avg of a set of integers is decimal
//        e.mandatoryArg(SequenceType.ATOMIC_SEQUENCE);

        register("base-uri", new BaseURI(), SequenceType.OPTIONAL_ANY_URI, "N?");
//        e.mandatoryArg(SequenceType.OPTIONAL_NODE);

        register("boolean", new BooleanFn(BooleanFn.BOOLEAN), SequenceType.SINGLE_BOOLEAN, "*");
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);

        e = register("ceiling", new Rounding(Rounding.CEILING), SequenceType.OPTIONAL_NUMERIC, "n?");
        e.sameItemTypeAsFirstArgument = true;
//        e.mandatoryArg(SequenceType.OPTIONAL_NUMERIC);

        register("codepoint-equal", new CodepointEqual(), SequenceType.OPTIONAL_BOOLEAN, "s? s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        register("codepoints-to-string", new CodepointsToString(), SequenceType.SINGLE_STRING, "i*");
        e.mandatoryArg(SequenceType.INTEGER_SEQUENCE);

        register("compare", new Compare(), SequenceType.OPTIONAL_INTEGER, "s? s? ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        e = register("concat", new Concat(), SequenceType.SINGLE_STRING, "a?");
//        e.mandatoryArg(SequenceType.OPTIONAL_ATOMIC);
        e.maxArguments = Integer.MAX_VALUE;
        // Note, this has a variable number of arguments so it is treated specially

        register("contains", new Contains(Contains.CONTAINS), SequenceType.SINGLE_BOOLEAN, "s? s? ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);


        register("count", new Count(), SequenceType.SINGLE_INTEGER, "*");
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);

        e = register("current", new Current(), SequenceType.SINGLE_ITEM, "");
        e.applicability = XSLT;

        register("current-date", new CurrentDateTime(), SequenceType.SINGLE_DATE, "");

        register("current-dateTime", new CurrentDateTime(), SequenceType.SINGLE_DATE_TIME, "");

        register("current-time", new CurrentDateTime(), SequenceType.SINGLE_TIME, "");

        register("current-group", new CurrentGroup(), SequenceType.ANY_SEQUENCE, "");
        e.applicability = XSLT;

        register("current-grouping-key", new CurrentGroupingKey(), SequenceType.OPTIONAL_ATOMIC, "");
        e.applicability = XSLT;

        register("dateTime", new DateTimeConstructor(), SequenceType.OPTIONAL_DATE_TIME, "dat? tim?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE);
//        e.mandatoryArg(SequenceType.OPTIONAL_TIME);

        register("day-from-date", new Component(Component.DAY), SequenceType.OPTIONAL_INTEGER, "dat?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE);

        register("day-from-dateTime", new Component(Component.DAY), SequenceType.OPTIONAL_INTEGER, "dt?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE_TIME);

        register("days-from-duration", new Component(Component.DAY), SequenceType.OPTIONAL_INTEGER, "dur?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DURATION);

        register("deep-equal", new DeepEqual(), SequenceType.SINGLE_BOOLEAN, "* * s?");
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("distinct-values", new DistinctValues(), SequenceType.ATOMIC_SEQUENCE, "a* ?s");
//        e.mandatoryArg(SequenceType.ATOMIC_SEQUENCE);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("doc", new Doc(), SequenceType.OPTIONAL_DOCUMENT, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        register("doc-available", new DocAvailable(), SequenceType.SINGLE_BOOLEAN, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        e = register("document", new DocumentFn(), SequenceType.NODE_SEQUENCE, "* ?N");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);
//        e.optionalArg(SequenceType.SINGLE_NODE);

        register("document-uri", new NamePart(NamePart.DOCUMENT_URI), SequenceType.OPTIONAL_ANY_URI, "N*");
//        e.mandatoryArg(SequenceType.NODE_SEQUENCE);

        register("empty", new Empty(), SequenceType.SINGLE_BOOLEAN, "*");
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);

        register("ends-with", new Contains(Contains.ENDS_WITH), SequenceType.SINGLE_BOOLEAN, "s? s? ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        e = register("element-available", new Available(Available.ELEMENT_AVAILABLE),
                SequenceType.SINGLE_BOOLEAN, "s");
        e.applicability = XSLT | USE_WHEN;
//        e.mandatoryArg(SequenceType.SINGLE_STRING);

        register("element-with-id", new Id(), SequenceType.ELEMENT_SEQUENCE, "s* ?N");
//        e.mandatoryArg(SequenceType.STRING_SEQUENCE);
//        e.optionalArg(SequenceType.SINGLE_NODE);

        register("encode-for-uri", new EscapeURI(EscapeURI.ENCODE_FOR_URI), SequenceType.SINGLE_STRING, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        register("escape-html-uri", new EscapeURI(EscapeURI.HTML_URI), SequenceType.SINGLE_STRING, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        register("error", new Error(), SequenceType.OPTIONAL_ITEM, "?q? ?s ?I");
        // The return type is chosen so that use of the error() function will never give a static type error,
        // on the basis that item()? overlaps every other type, and it's almost impossible to make any
        // unwarranted inferences from it, except perhaps count(error()) lt 2.
//        e.optionalArg(SequenceType.OPTIONAL_QNAME);
//        e.optionalArg(SequenceType.SINGLE_STRING);
//        e.optionalArg(SequenceType.ANY_SEQUENCE);

        register("exists", new Exists(), SequenceType.SINGLE_BOOLEAN, "*");
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);

        e = register("floor", new Rounding(Rounding.FLOOR), SequenceType.OPTIONAL_NUMERIC, "n?");
        e.sameItemTypeAsFirstArgument = true;
//        e.mandatoryArg(SequenceType.OPTIONAL_NUMERIC);

        e = register("format-date", new FormatDate(), SequenceType.SINGLE_STRING, "dat? s ?s? ?s? ?s?");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.optionalArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.OPTIONAL_STRING);

        e = register("format-dateTime", new FormatDate(), SequenceType.SINGLE_STRING, "dt? s ?s? ?s? ?s?");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE_TIME);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.optionalArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.OPTIONAL_STRING);

        e = register("format-number", new FormatNumber(), SequenceType.SINGLE_STRING, "n? s ?s");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.OPTIONAL_NUMERIC);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        e = register("format-time", new FormatDate(), SequenceType.SINGLE_STRING, "tim? s ?s? ?s? ?s?");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.OPTIONAL_TIME);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.optionalArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.OPTIONAL_STRING);

        e = register("function-available", new Available(Available.FUNCTION_AVAILABLE), SequenceType.SINGLE_BOOLEAN, "s ?i");
        e.applicability = XSLT | USE_WHEN;
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.optionalArg(SequenceType.SINGLE_INTEGER);

        e = register("generate-id", new NamePart(NamePart.GENERATE_ID), SequenceType.SINGLE_STRING, "N?");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.OPTIONAL_NODE);

        register("hours-from-dateTime", new Component(Component.HOURS),
                SequenceType.OPTIONAL_INTEGER, "dt?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE_TIME);

        register("hours-from-duration", new Component(Component.HOURS),
                SequenceType.OPTIONAL_INTEGER, "dur?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DURATION);

        register("hours-from-time", new Component(Component.HOURS),
                SequenceType.OPTIONAL_INTEGER, "tim?");
//        e.mandatoryArg(SequenceType.OPTIONAL_TIME);

        register("id", new Id(), SequenceType.ELEMENT_SEQUENCE, "s* ?N");
//        e.mandatoryArg(SequenceType.STRING_SEQUENCE);
//        e.optionalArg(SequenceType.SINGLE_NODE);

        register("implicit-timezone", new CurrentDateTime(), SequenceType.SINGLE_DAY_TIME_DURATION, "");

        register("in-scope-prefixes", new InScopePrefixes(), SequenceType.STRING_SEQUENCE, "E");
//        e.mandatoryArg(SequenceType.SINGLE_ELEMENT);

        register("index-of", new IndexOf(), SequenceType.INTEGER_SEQUENCE, "a* a ?s");
//        e.mandatoryArg(SequenceType.ATOMIC_SEQUENCE);
//        e.mandatoryArg(SequenceType.SINGLE_ATOMIC);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("insert-before", new Insert(), SequenceType.ANY_SEQUENCE, "* i *");
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);
//        e.mandatoryArg(SequenceType.SINGLE_INTEGER);
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);

        register("iri-to-uri", new EscapeURI(EscapeURI.IRI_TO_URI), SequenceType.SINGLE_STRING, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        e = register("key", new KeyFn(), SequenceType.NODE_SEQUENCE, "s a* ?N");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.mandatoryArg(SequenceType.ATOMIC_SEQUENCE);
//        e.optionalArg(SequenceType.SINGLE_NODE);

        register("lang", new Lang(), SequenceType.SINGLE_BOOLEAN, "s? ?N");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_NODE);

        register("last", new Last(), SequenceType.SINGLE_INTEGER, "");

        register("local-name", new NamePart(NamePart.LOCAL_NAME), SequenceType.SINGLE_STRING, "N?");
//        e.mandatoryArg(SequenceType.OPTIONAL_NODE);

        register("local-name-from-QName", new Component(Component.LOCALNAME),
                SequenceType.OPTIONAL_STRING, "q?");
//        e.mandatoryArg(SequenceType.OPTIONAL_QNAME);

        register("lower-case", new ForceCase(ForceCase.LOWERCASE), SequenceType.SINGLE_STRING, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        register("matches", new Matches(), SequenceType.SINGLE_BOOLEAN, "s? s ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("max", new Minimax(Minimax.MAX), SequenceType.OPTIONAL_ATOMIC, "a* ?s");
//        e.mandatoryArg(SequenceType.ATOMIC_SEQUENCE);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("min", new Minimax(Minimax.MIN), SequenceType.OPTIONAL_ATOMIC, "a* ?s");
//        e.mandatoryArg(SequenceType.ATOMIC_SEQUENCE);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("minutes-from-dateTime", new Component(Component.MINUTES),
                SequenceType.OPTIONAL_INTEGER, "dt?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE_TIME);

        register("minutes-from-duration", new Component(Component.MINUTES),
                SequenceType.OPTIONAL_INTEGER, "dur?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DURATION);

        register("minutes-from-time", new Component(Component.MINUTES),
                SequenceType.OPTIONAL_INTEGER, "tim?");
//        e.mandatoryArg(SequenceType.OPTIONAL_TIME);

        register("month-from-date", new Component(Component.MONTH),
                SequenceType.OPTIONAL_INTEGER, "dat?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE);

        register("month-from-dateTime", new Component(Component.MONTH),
                SequenceType.OPTIONAL_INTEGER, "dt?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE_TIME);

        register("months-from-duration", new Component(Component.MONTH),
                SequenceType.OPTIONAL_INTEGER, "dur?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DURATION);

        register("name", new NamePart(NamePart.NAME), SequenceType.SINGLE_STRING, "N?");
//        e.mandatoryArg(SequenceType.OPTIONAL_NODE);

        register("namespace-uri", new NamePart(NamePart.NAMESPACE_URI), SequenceType.SINGLE_ANY_URI, "N?");
//        e.mandatoryArg(SequenceType.OPTIONAL_NODE);

        register("namespace-uri-for-prefix", new NamespaceForPrefix(), SequenceType.OPTIONAL_ANY_URI, "s? E");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_ELEMENT);

        register("namespace-uri-from-QName", new Component(Component.NAMESPACE),
                SequenceType.OPTIONAL_ANY_URI, "q?");
//        e.mandatoryArg(SequenceType.OPTIONAL_QNAME);

        register("node-name", new NamePart(NamePart.NODE_NAME), SequenceType.OPTIONAL_QNAME, "N?");
//        e.mandatoryArg(SequenceType.OPTIONAL_NODE);

        register("not", new BooleanFn(BooleanFn.NOT), SequenceType.SINGLE_BOOLEAN, "*");
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);

        register("normalize-space", new NormalizeSpace(), SequenceType.SINGLE_STRING, "");
        register("normalize-space#0", new NormalizeSpace(), SequenceType.SINGLE_STRING, "");

        register("normalize-space#1", new NormalizeSpace(), SequenceType.SINGLE_STRING, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        register("normalize-unicode", new NormalizeUnicode(), SequenceType.SINGLE_STRING, "s? ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("number", new NumberFn(), SequenceType.SINGLE_DOUBLE, "a?");
//        e.mandatoryArg(SequenceType.OPTIONAL_ATOMIC);

        register("position", new Position(), SequenceType.SINGLE_INTEGER, "");

        register("prefix-from-QName", new Component(Component.PREFIX),
                SequenceType.OPTIONAL_STRING, "q?");
//        e.mandatoryArg(SequenceType.OPTIONAL_QNAME);

        register("QName", new QNameFn(), SequenceType.SINGLE_QNAME, "s? s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);

        e = register("regex-group", new RegexGroup(), SequenceType.SINGLE_STRING, "i");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.SINGLE_INTEGER);

        e = register("remove", new Remove(), SequenceType.ANY_SEQUENCE, "* i");
        e.sameItemTypeAsFirstArgument = true;
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);
//        e.mandatoryArg(SequenceType.SINGLE_INTEGER);

        register("replace", new Replace(), SequenceType.SINGLE_STRING, "s? s s ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("resolve-QName", new ResolveQName(), SequenceType.OPTIONAL_QNAME, "q? E");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_ELEMENT);

        register("resolve-uri", new ResolveURI(), SequenceType.OPTIONAL_ANY_URI, "s? ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("reverse", new Reverse(), SequenceType.ANY_SEQUENCE, "*");
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);

        register("root", new Root(), SequenceType.OPTIONAL_NODE, "?N?");
//        e.optionalArg(SequenceType.OPTIONAL_NODE);

        e = register("round", new Rounding(Rounding.ROUND), SequenceType.OPTIONAL_NUMERIC, "n?");
        e.sameItemTypeAsFirstArgument = true;
//        e.mandatoryArg(SequenceType.OPTIONAL_NUMERIC);

        e = register("round-half-to-even", new Rounding(Rounding.HALF_EVEN), SequenceType.OPTIONAL_NUMERIC, "n? i");
        e.sameItemTypeAsFirstArgument = true;
//        e.mandatoryArg(SequenceType.OPTIONAL_NUMERIC);
//        e.mandatoryArg(SequenceType.SINGLE_INTEGER);

        register("seconds-from-dateTime", new Component(Component.SECONDS),
                SequenceType.OPTIONAL_DECIMAL, "dt?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE_TIME);

        register("seconds-from-duration", new Component(Component.SECONDS),
                SequenceType.OPTIONAL_DECIMAL, "dur?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DURATION);

        register("seconds-from-time", new Component(Component.SECONDS),
                SequenceType.OPTIONAL_DECIMAL, "tim?");
//        e.mandatoryArg(SequenceType.OPTIONAL_TIME);

        register("starts-with", new Contains(Contains.STARTS_WITH), SequenceType.SINGLE_BOOLEAN, "s? s? ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("string", new StringFn(), SequenceType.SINGLE_STRING, "I?");
//        e.mandatoryArg(SequenceType.OPTIONAL_ITEM);

        register("string-length", new StringLength(), SequenceType.SINGLE_INTEGER, "");
        register("string-length#0", new StringLength(), SequenceType.SINGLE_INTEGER, "");

        register("string-length#1", new StringLength(), SequenceType.SINGLE_INTEGER, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        register("string-join", new StringJoin(), SequenceType.SINGLE_STRING, "s* s");
//        e.mandatoryArg(SequenceType.STRING_SEQUENCE);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);

        register("string-to-codepoints", new StringToCodepoints(), SequenceType.INTEGER_SEQUENCE, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        e = register("subsequence", new Subsequence(), SequenceType.ANY_SEQUENCE, "* d ?d");
        e.sameItemTypeAsFirstArgument = true;
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);
//        e.mandatoryArg(SequenceType.SINGLE_DOUBLE);
//        e.optionalArg(SequenceType.SINGLE_DOUBLE);

        register("substring", new Substring(), SequenceType.SINGLE_STRING, "s? d ?d");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_DOUBLE);
//        e.optionalArg(SequenceType.SINGLE_DOUBLE);

        register("substring-after", new SubstringAfter(), SequenceType.SINGLE_STRING, "s? s? ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("substring-before", new SubstringBefore(), SequenceType.SINGLE_STRING ,"s? s? ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        register("sum", new Sum(), SequenceType.OPTIONAL_ATOMIC, "a* a?");
//        e.mandatoryArg(SequenceType.ATOMIC_SEQUENCE);
//        e.optionalArg(SequenceType.OPTIONAL_ATOMIC);

        e = register("system-property", new SystemProperty(), SequenceType.SINGLE_STRING, "s");
        e.applicability = XSLT | USE_WHEN;
//        e.mandatoryArg(SequenceType.SINGLE_STRING);

        register("timezone-from-date", new Component(Component.TIMEZONE),
                SequenceType.OPTIONAL_DAY_TIME_DURATION, "dat?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE);

        register("timezone-from-dateTime", new Component(Component.TIMEZONE),
                SequenceType.OPTIONAL_DAY_TIME_DURATION, "dt?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE_TIME);

        register("timezone-from-time", new Component(Component.TIMEZONE),
                SequenceType.OPTIONAL_DAY_TIME_DURATION, "tim?");
//        e.mandatoryArg(SequenceType.OPTIONAL_TIME);

        register("translate", new Translate(), SequenceType.SINGLE_STRING, "s? s s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);

        register("tokenize", new Tokenize(), SequenceType.STRING_SEQUENCE, "s? s ?s");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);
        
        register("trace", new Trace(), SequenceType.ANY_SEQUENCE, "* s");
//        e.mandatoryArg(SequenceType.ANY_SEQUENCE);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
        
        e = register("type-available", new Available(Available.TYPE_AVAILABLE), SequenceType.SINGLE_BOOLEAN, "s");
        e.applicability = XSLT | USE_WHEN;
//        e.mandatoryArg(SequenceType.SINGLE_STRING);

        register("upper-case", new ForceCase(ForceCase.UPPERCASE), SequenceType.SINGLE_STRING, "s?");
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);

        e = register("unparsed-text", new UnparsedText(UnparsedText.UNPARSED_TEXT),
                SequenceType.OPTIONAL_STRING, "s? ?s");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.OPTIONAL_STRING);
//        e.optionalArg(SequenceType.SINGLE_STRING);

        e = register("unparsed-text-available", new UnparsedText(UnparsedText.UNPARSED_TEXT_AVAILABLE),
                SequenceType.SINGLE_BOOLEAN, "s s");
        e.applicability = XSLT;
//        e.mandatoryArg(SequenceType.SINGLE_STRING);
//        e.mandatoryArg(SequenceType.SINGLE_STRING);

        register("year-from-date", new Component(Component.YEAR),
                SequenceType.OPTIONAL_INTEGER, "dat?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE);

        register("year-from-dateTime", new Component(Component.YEAR),
                SequenceType.OPTIONAL_INTEGER, "dt?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DATE_TIME);

        register("years-from-duration", new Component(Component.YEAR),
                SequenceType.OPTIONAL_INTEGER, "dur?");
//        e.mandatoryArg(SequenceType.OPTIONAL_DURATION);

    }


    /**
     * Get the table entry for the function with a given name
     *
     * @param name the name of the function. This is an unprefixed local-name for functions in the
     *             system namespace
     * @param arity the number of arguments
     * @return if the function name is known, an Entry containing information about the function. Otherwise,
     *         null
     */

    public static Entry getFunction(String name, int arity) {
        // try first for an entry of the form name#arity
        Entry e = functionTable.get(name + '#' + arity);
        if (e != null) {
            return e;
        }
        // try for a generic entry
        return functionTable.get(name);
    }

    /**
     * An entry in the table describing the properties of a function
     */
    public static class Entry {
        /**
         * The name of the function: a local name in the case of functions in the standard library
         */
        public String name;
        /**
         * The class containing the implementation of this function (always a subclass of SystemFunction)
         */
        public SystemFunction skeleton;

        /**
         * The minimum number of arguments required
         */
        public int minArguments;
        /**
         * The maximum number of arguments permitted
         */
        public int maxArguments;
        /**
         * The  type of the result of the function
         */
        public SequenceType resultType;

        /**
         * Flags indicating which host languages the function is applicable to
         */
        public int applicability;
        /**
         * Flag indicating the function result is the same item type as the first argument
         */
        public boolean sameItemTypeAsFirstArgument;
        /**
         * An array holding the types of the arguments to the function
         */
        public SequenceType[] argumentTypes;

        
        /**
         * Add information to a function entry about the argument types of the function
         *
         * @param type          the sequence type of the argument
         */

        public void mandatoryArg(SequenceType type) {
            Entry e = this;
            e.minArguments++;
            e.maxArguments++;
            int argNr = e.argumentTypes.length;
            SequenceType[] st2 = new SequenceType[e.argumentTypes.length + 1];
            System.arraycopy(e.argumentTypes, 0, st2, 0, e.argumentTypes.length);
            e.argumentTypes = st2;
            e.argumentTypes[argNr] = type;
        }

        public void optionalArg(SequenceType type) {
            Entry e = this;
            e.maxArguments++;
            int argNr = e.argumentTypes.length;
            SequenceType[] st2 = new SequenceType[e.argumentTypes.length + 1];
            System.arraycopy(e.argumentTypes, 0, st2, 0, e.argumentTypes.length);
            e.argumentTypes = st2;
            e.argumentTypes[argNr] = type;
        }
        
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
