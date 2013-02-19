package client.net.sf.saxon.ce.om;

import client.net.sf.saxon.ce.lib.NamespaceConstant;

import java.util.HashMap;


/**
 * Well-known names used in XSLT processing. These names must all have
 * fingerprints in the range 0-1023, to avoid clashing with codes allocated
 * in a NamePool. We use the top three bits for the namespace, and the bottom
 * seven bits for the local name.
 * <p/>
 * <p>Codes in the range 0-100 are used for standard node kinds such as ELEMENT,
 * DOCUMENT, etc, and for built-in types such as ITEM and EMPTY.</p>
 */

public abstract class StandardNames {


    private static final int DFLT_NS = 0;
    private static final int XSL_NS = 1;
    private static final int SAXON_NS = 2;
    private static final int XML_NS = 3;
    private static final int XS_NS = 4;
    private static final int XSI_NS = 5;
    private static final int IXSL_NS = 6;

    public static final int DFLT = 0;             //   0
    public static final int XSL = 128;            // 128
    public static final int SAXON = 128 * 2;      // 256
    public static final int XML = 128 * 3;        // 384
    public static final int XS = 128 * 4;         // 512
    public static final int XSI = 128 * 5;        // 640
    public static final int IXSL = 128 * 6;       // 768

    public static final int XSL_ANALYZE_STRING = XSL;
    public static final int XSL_APPLY_IMPORTS = XSL + 1;
    public static final int XSL_APPLY_TEMPLATES = XSL + 2;
    public static final int XSL_ATTRIBUTE = XSL + 3;
    public static final int XSL_ATTRIBUTE_SET = XSL + 4;
    public static final int XSL_BREAK = XSL + 5;
    public static final int XSL_CALL_TEMPLATE = XSL + 6;
    public static final int XSL_CATCH = XSL + 7;
    public static final int XSL_CHARACTER_MAP = XSL + 8;
    public static final int XSL_CHOOSE = XSL + 9;
    public static final int XSL_COMMENT = XSL + 10;
    public static final int XSL_COPY = XSL + 15;
    public static final int XSL_COPY_OF = XSL + 16;
    public static final int XSL_DECIMAL_FORMAT = XSL + 17;
    public static final int XSL_DOCUMENT = XSL + 18;
    public static final int XSL_ELEMENT = XSL + 19;
    public static final int XSL_EVALUATE = XSL + 20;
    public static final int XSL_FALLBACK = XSL + 22;
    public static final int XSL_FOR_EACH = XSL + 23;
    public static final int XSL_FORK = XSL + 24;
    public static final int XSL_FOR_EACH_GROUP = XSL + 26;
    public static final int XSL_FUNCTION = XSL + 27;
    public static final int XSL_IF = XSL + 28;
    public static final int XSL_IMPORT = XSL + 29;
    public static final int XSL_IMPORT_SCHEMA = XSL + 30;
    public static final int XSL_INCLUDE = XSL + 35;
    public static final int XSL_ITERATE = XSL + 36;
    public static final int XSL_KEY = XSL + 37;
    public static final int XSL_MATCHING_SUBSTRING = XSL + 38;
    public static final int XSL_MERGE = XSL + 39;
    public static final int XSL_MERGE_INPUT = XSL + 40;
    public static final int XSL_MERGE_SOURCE = XSL + 41;
    public static final int XSL_MESSAGE = XSL + 42;
    public static final int XSL_MODE = XSL + 46;
    public static final int XSL_NAMESPACE = XSL + 47;
    public static final int XSL_NAMESPACE_ALIAS = XSL + 48;
    public static final int XSL_NEXT_ITERATION = XSL + 49;
    public static final int XSL_NEXT_MATCH = XSL + 50;
    public static final int XSL_NON_MATCHING_SUBSTRING = XSL + 51;
    public static final int XSL_NUMBER = XSL + 52;
    public static final int XSL_OTHERWISE = XSL + 53;
    public static final int XSL_ON_COMPLETION = XSL + 54;
    public static final int XSL_OUTPUT = XSL + 55;
    public static final int XSL_OUTPUT_CHARACTER = XSL + 56;
    public static final int XSL_PARAM = XSL + 60;
    public static final int XSL_PERFORM_SORT = XSL + 61;
    public static final int XSL_PRESERVE_SPACE = XSL + 62;
    public static final int XSL_PROCESSING_INSTRUCTION = XSL + 63;
    public static final int XSL_RESULT_DOCUMENT = XSL + 64;
    public static final int XSL_SEQUENCE = XSL + 65;
    public static final int XSL_SORT = XSL + 66;
    public static final int XSL_STRIP_SPACE = XSL + 70;
    public static final int XSL_STYLESHEET = XSL + 71;
    public static final int XSL_TEMPLATE = XSL + 72;
    public static final int XSL_TEXT = XSL + 73;
    public static final int XSL_TRANSFORM = XSL + 74;
    public static final int XSL_VALUE_OF = XSL + 75;
    public static final int XSL_VARIABLE = XSL + 76;
    public static final int XSL_WHEN = XSL + 77;
    public static final int XSL_WITH_PARAM = XSL + 78;
    public static final int XSL_TRY = XSL + 79;
    
    public static final int XSL_DEFAULT_COLLATION = XSL + 100;
    public static final int XSL_EXCLUDE_RESULT_PREFIXES = XSL + 101;
    public static final int XSL_EXTENSION_ELEMENT_PREFIXES = XSL + 102;
    public static final int XSL_INHERIT_NAMESPACES = XSL + 103;
    public static final int XSL_TYPE = XSL + 104;
    public static final int XSL_USE_ATTRIBUTE_SETS = XSL + 105;
    public static final int XSL_USE_WHEN = XSL + 106;
    public static final int XSL_VALIDATION = XSL + 107;
    public static final int XSL_VERSION = XSL + 108;
    public static final int XSL_XPATH_DEFAULT_NAMESPACE = XSL + 109;

    public static final int IXSL_REMOVE_ATTRIBUTE = IXSL;
    public static final int IXSL_SCHEDULE_ACTION = IXSL + 1;
    public static final int IXSL_SET_ATTRIBUTE = IXSL + 2;
    public static final int IXSL_TRANSFORM = IXSL + 3;
    public static final int IXSL_SET_PROPERTY = IXSL + 4;

    public static final int XML_BASE = XML + 1;
    public static final int XML_SPACE = XML + 2;
    public static final int XML_LANG = XML + 3;
    public static final int XML_ID = XML + 4;
    public static final int XML_LANG_TYPE = XML + 5;


    public static final String AS = "as";
    public static final String BYTE_ORDER_MARK = "byte-order-mark";
    public static final String CASE_ORDER = "case-order";
    public static final String CDATA_SECTION_ELEMENTS = "cdata-section-elements";
    public static final String CHARACTER = "character";
    public static final String COLLATION = "collation";
    public static final String COPY_NAMESPACES = "copy-namespaces";
    public static final String COUNT = "count";
    public static final String DATA_TYPE = "data-type";
    public static final String DECIMAL_SEPARATOR = "decimal-separator";
    public static final String DEFAULT_COLLATION = "default-collation";
    public static final String DEFAULT_MODE = "default-mode";
    public static final String DEFAULT_VALIDATION = "default-validation";
    public static final String DIGIT = "digit";
    public static final String DISABLE_OUTPUT_ESCAPING = "disable-output-escaping";
    public static final String DOCTYPE_PUBLIC = "doctype-public";
    public static final String DOCTYPE_SYSTEM = "doctype-system";
    public static final String ELEMENTS = "elements";
    public static final String ESCAPE_URI_ATTRIBUTES = "escape-uri-attributes";
    public static final String ENCODING = "encoding";
    public static final String EXCLUDE_RESULT_PREFIXES = "exclude-result-prefixes";
    public static final String EXTENSION_ELEMENT_PREFIXES = "extension-element-prefixes";
    public static final String FLAGS = "flags";
    public static final String FORMAT = "format";
    public static final String FROM = "from";
    public static final String GROUP_ADJACENT = "group-adjacent";
    public static final String GROUP_BY = "group-by";
    public static final String GROUP_ENDING_WITH = "group-ending-with";
    public static final String GROUP_STARTING_WITH = "group-starting-with";
    public static final String GROUPING_SEPARATOR = "grouping-separator";
    public static final String GROUPING_SIZE = "grouping-size";
    public static final String HREF = "href";
    public static final String ID = "id";
    public static final String INCLUDE_CONTENT_TYPE = "include-content-type";
    public static final String INDENT = "indent";
    public static final String INFINITY = "infinity";
    public static final String INHERIT_NAMESPACES = "inherit-namespaces";
    public static final String INPUT_TYPE_ANNOTATIONS = "input-type-annotations";
    public static final String LANG = "lang";
    public static final String LETTER_VALUE = "letter-value";
    public static final String LEVEL = "level";
    public static final String MATCH = "match";
    public static final String MEDIA_TYPE = "media-type";
    public static final String METHOD = "method";
    public static final String MINUS_SIGN = "minus-sign";
    public static final String MODE = "mode";
    public static final String NAME = "name";
    public static final String NAMESPACE = "namespace";
    public static final String NAN = "NaN";
    public static final String NORMALIZATION_FORM = "normalization-form";
    public static final String OMIT_XML_DECLARATION = "omit-xml-declaration";
    public static final String ORDER = "order";
    public static final String ORDINAL = "ordinal";
    public static final String OUTPUT_VERSION = "output-version";
    public static final String OVERRIDE = "override";
    public static final String PATTERN_SEPARATOR = "pattern-separator";
    public static final String PERCENT = "percent";
    public static final String PER_MILLE = "per-mille";
    public static final String IXSL_PREVENT_DEFAULT = "{" + NamespaceConstant.IXSL + "}" + "prevent-default";
    public static final String IXSL_EVENT_PROPERTY = "{" + NamespaceConstant.IXSL + "}" + "event-property";
    public static final String PRIORITY = "priority";
    public static final String REGEX = "regex";
    public static final String REQUIRED = "required";
    public static final String RESULT_PREFIX = "result-prefix";
    public static final String SCHEMA_LOCATION = "schema-location";
    public static final String SELECT = "select";
    public static final String SEPARATOR = "separator";
    public static final String STABLE = "stable";
    public static final String STANDALONE = "standalone";
    public static final String STRING = "string";
    public static final String STYLESHEET_PREFIX = "stylesheet-prefix";
    public static final String TERMINATE = "terminate";
    public static final String TEST = "test";
    public static final String TUNNEL = "tunnel";
    public static final String TYPE = "type";
    public static final String UNDECLARE_PREFIXES = "undeclare-prefixes";
    public static final String USE = "use";
    public static final String USE_ATTRIBUTE_SETS = "use-attribute-sets";
    public static final String USE_CHARACTER_MAPS = "use-character-maps";
    public static final String USE_WHEN = "use-when";
    public static final String VALIDATION = "validation";
    public static final String VALUE = "value";
    public static final String VERSION = "version";
    public static final String XPATH_DEFAULT_NAMESPACE = "xpath-default-namespace";
    public static final String ZERO_DIGIT = "zero-digit";

    public static final int XS_STRING = XS + 1;
    public static final int XS_BOOLEAN = XS + 2;
    public static final int XS_DECIMAL = XS + 3;
    public static final int XS_FLOAT = XS + 4;
    public static final int XS_DOUBLE = XS + 5;
    public static final int XS_DURATION = XS + 6;
    public static final int XS_DATE_TIME = XS + 7;
    public static final int XS_TIME = XS + 8;
    public static final int XS_DATE = XS + 9;
    public static final int XS_G_YEAR_MONTH = XS + 10;
    public static final int XS_G_YEAR = XS + 11;
    public static final int XS_G_MONTH_DAY = XS + 12;
    public static final int XS_G_DAY = XS + 13;
    public static final int XS_G_MONTH = XS + 14;
    public static final int XS_HEX_BINARY = XS + 15;
    public static final int XS_BASE64_BINARY = XS + 16;
    public static final int XS_ANY_URI = XS + 17;
    public static final int XS_QNAME = XS + 18;
    public static final int XS_INTEGER = XS + 21;

    // Note that any type code <= XS_INTEGER is considered to represent a
    // primitive type: see Type.isPrimitiveType()



    public static final int XS_ID = XS + 48;
    public static final int XS_IDREF = XS + 49;


    public static final int XS_ANY_TYPE = XS + 60;
    public static final int XS_ANY_SIMPLE_TYPE = XS + 61;

    public static final int XS_INVALID_NAME = XS + 62;


    public static final int XS_UNTYPED = XS + 118;
    public static final int XS_UNTYPED_ATOMIC = XS + 119;
    public static final int XS_ANY_ATOMIC_TYPE = XS + 120;
    public static final int XS_YEAR_MONTH_DURATION = XS + 121;
    public static final int XS_DAY_TIME_DURATION = XS + 122;
    public static final int XS_NUMERIC = XS + 123;

    private static String[] localNames = new String[1023];
    private static HashMap<String, Integer> lookup = new HashMap<String, Integer>(1023);
    // key is an expanded QName in Clark notation
    // value is a fingerprint, as a java.lang.Integer

    private StandardNames() {
        //pool = namePool;
    }

    private static void bindXSLTName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.XSLT + '}' + localName, Integer.valueOf(constant));
    }
    
    private static void bindIXSLName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.IXSL + '}' + localName, Integer.valueOf(constant));
    }

    private static void bindXMLName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.XML + '}' + localName, Integer.valueOf(constant));
    }

    private static void bindXSName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.SCHEMA + '}' + localName, Integer.valueOf(constant));
    }

    private static void bindXSIName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.SCHEMA_INSTANCE + '}' + localName, Integer.valueOf(constant));
    }

    static {

        bindXSLTName(XSL_ANALYZE_STRING, "analyze-string");
        bindXSLTName(XSL_APPLY_IMPORTS, "apply-imports");
        bindXSLTName(XSL_APPLY_TEMPLATES, "apply-templates");
        bindXSLTName(XSL_ATTRIBUTE, "attribute");
        bindXSLTName(XSL_ATTRIBUTE_SET, "attribute-set");
        bindXSLTName(XSL_BREAK, "break");
        bindXSLTName(XSL_CALL_TEMPLATE, "call-template");
        bindXSLTName(XSL_CATCH, "catch");
        bindXSLTName(XSL_CHARACTER_MAP, "character-map");
        bindXSLTName(XSL_CHOOSE, "choose");
        bindXSLTName(XSL_COMMENT, "comment");
        bindXSLTName(XSL_COPY, "copy");
        bindXSLTName(XSL_COPY_OF, "copy-of");
        bindXSLTName(XSL_DECIMAL_FORMAT, "decimal-format");
        bindXSLTName(XSL_DOCUMENT, "document");
        bindXSLTName(XSL_ELEMENT, "element");
        bindXSLTName(XSL_EVALUATE, "evaluate");
        bindXSLTName(XSL_FALLBACK, "fallback");
        bindXSLTName(XSL_FOR_EACH, "for-each");
        bindXSLTName(XSL_FOR_EACH_GROUP, "for-each-group");
        bindXSLTName(XSL_FORK, "fork");
        bindXSLTName(XSL_FUNCTION, "function");
        bindXSLTName(XSL_IF, "if");
        bindXSLTName(XSL_IMPORT, "import");
        bindXSLTName(XSL_IMPORT_SCHEMA, "import-schema");
        bindXSLTName(XSL_INCLUDE, "include");
        bindXSLTName(XSL_ITERATE, "iterate");
        bindXSLTName(XSL_KEY, "key");
        bindXSLTName(XSL_MATCHING_SUBSTRING, "matching-substring");
        bindXSLTName(XSL_MERGE, "merge");
        bindXSLTName(XSL_MERGE_INPUT, "merge-input");
        bindXSLTName(XSL_MERGE_SOURCE, "merge-source");
        bindXSLTName(XSL_MESSAGE, "message");
        bindXSLTName(XSL_MODE, "mode");
        bindXSLTName(XSL_NEXT_MATCH, "next-match");
        bindXSLTName(XSL_NUMBER, "number");
        bindXSLTName(XSL_NAMESPACE, "namespace");
        bindXSLTName(XSL_NAMESPACE_ALIAS, "namespace-alias");
        bindXSLTName(XSL_NEXT_ITERATION, "next-iteration");
        bindXSLTName(XSL_NON_MATCHING_SUBSTRING, "non-matching-substring");
        bindXSLTName(XSL_ON_COMPLETION, "on-completion");
        bindXSLTName(XSL_OTHERWISE, "otherwise");
        bindXSLTName(XSL_OUTPUT, "output");
        bindXSLTName(XSL_OUTPUT_CHARACTER, "output-character");
        bindXSLTName(XSL_PARAM, "param");
        bindXSLTName(XSL_PERFORM_SORT, "perform-sort");
        bindXSLTName(XSL_PRESERVE_SPACE, "preserve-space");
        bindXSLTName(XSL_PROCESSING_INSTRUCTION, "processing-instruction");
        bindXSLTName(XSL_RESULT_DOCUMENT, "result-document");
        bindXSLTName(XSL_SEQUENCE, "sequence");
        bindXSLTName(XSL_SORT, "sort");
        bindXSLTName(XSL_STRIP_SPACE, "strip-space");
        bindXSLTName(XSL_STYLESHEET, "stylesheet");
        bindXSLTName(XSL_TEMPLATE, "template");
        bindXSLTName(XSL_TEXT, "text");
        bindXSLTName(XSL_TRANSFORM, "transform");
        bindXSLTName(XSL_TRY, "try");
        bindXSLTName(XSL_VALUE_OF, "value-of");
        bindXSLTName(XSL_VARIABLE, "variable");
        bindXSLTName(XSL_WITH_PARAM, "with-param");
        bindXSLTName(XSL_WHEN, "when");

        bindXSLTName(XSL_DEFAULT_COLLATION, "default-collation");
        bindXSLTName(XSL_XPATH_DEFAULT_NAMESPACE, "xpath-default-namespace");
        bindXSLTName(XSL_EXCLUDE_RESULT_PREFIXES, "exclude-result-prefixes");
        bindXSLTName(XSL_EXTENSION_ELEMENT_PREFIXES, "extension-element-prefixes");
        bindXSLTName(XSL_INHERIT_NAMESPACES, "inherit-namespaces");
        bindXSLTName(XSL_TYPE, "type");
        bindXSLTName(XSL_USE_ATTRIBUTE_SETS, "use-attribute-sets");
        bindXSLTName(XSL_USE_WHEN, "use-when");
        bindXSLTName(XSL_VALIDATION, "validation");
        bindXSLTName(XSL_VERSION, "version");
        
        bindXMLName(XML_BASE, "base");
        bindXMLName(XML_SPACE, "space");
        bindXMLName(XML_LANG, "lang");
        bindXMLName(XML_ID, "id");
        bindXMLName(XML_LANG_TYPE, "_langType");

        bindXSName(XS_STRING, "string");
        bindXSName(XS_BOOLEAN, "boolean");
        bindXSName(XS_DECIMAL, "decimal");
        bindXSName(XS_FLOAT, "float");
        bindXSName(XS_DOUBLE, "double");
        bindXSName(XS_DURATION, "duration");
        bindXSName(XS_DATE_TIME, "dateTime");
        bindXSName(XS_TIME, "time");
        bindXSName(XS_DATE, "date");
        bindXSName(XS_G_YEAR_MONTH, "gYearMonth");
        bindXSName(XS_G_YEAR, "gYear");
        bindXSName(XS_G_MONTH_DAY, "gMonthDay");
        bindXSName(XS_G_DAY, "gDay");
        bindXSName(XS_G_MONTH, "gMonth");
        bindXSName(XS_HEX_BINARY, "hexBinary");
        bindXSName(XS_BASE64_BINARY, "base64Binary");
        bindXSName(XS_ANY_URI, "anyURI");
        bindXSName(XS_QNAME, "QName");
        bindXSName(XS_INTEGER, "integer");

        bindXSName(XS_ANY_TYPE, "anyType");
        bindXSName(XS_ANY_SIMPLE_TYPE, "anySimpleType");
        bindXSName(XS_INVALID_NAME, "invalidName");
        bindXSName(XS_UNTYPED, "untyped");
        bindXSName(XS_UNTYPED_ATOMIC, "untypedAtomic");
        bindXSName(XS_ANY_ATOMIC_TYPE, "anyAtomicType");
        bindXSName(XS_YEAR_MONTH_DURATION, "yearMonthDuration");
        bindXSName(XS_DAY_TIME_DURATION, "dayTimeDuration");
        bindXSName(XS_NUMERIC, "_numeric_");
     
        bindIXSLName(IXSL_REMOVE_ATTRIBUTE,"remove-attribute");
        bindIXSLName(IXSL_SCHEDULE_ACTION,"schedule-action");
        bindIXSLName(IXSL_SET_ATTRIBUTE,"set-attribute");
        bindIXSLName(IXSL_SET_PROPERTY,"set-property");
        bindIXSLName(IXSL_TRANSFORM,"transform");
    }

    /**
     * Get the fingerprint of a system-defined name, from its URI and local name
     *
     * @param uri       the namespace URI
     * @param localName the local part of the name
     * @return the standard fingerprint, or -1 if this is not a built-in name
     */

    public static int getFingerprint(String uri, String localName) {
        Integer fp = lookup.get('{' + uri + '}' + localName);
        if (fp == null) {
            return -1;
        } else {
            return fp.intValue();
        }
    }

    /**
     * Get the local part of a system-defined name
     * @param fingerprint the fingerprint of the name
     * @return the local part of the name
     */

    public static String getLocalName(int fingerprint) {
        return localNames[fingerprint];
    }

    /**
     * Get the namespace URI part of a system-defined name
     * @param fingerprint the fingerprint of the name
     * @return the namespace URI part of the name
     */

    public static String getURI(int fingerprint) {
        int c = fingerprint >> 7;
        switch (c) {
        case DFLT_NS:
            return "";
        case XSL_NS:
            return NamespaceConstant.XSLT;
        case IXSL_NS:
        	return NamespaceConstant.IXSL;
        case SAXON_NS:
            return NamespaceConstant.SAXON;
        case XML_NS:
            return NamespaceConstant.XML;
        case XS_NS:
            return NamespaceConstant.SCHEMA;
        case XSI_NS:
            return NamespaceConstant.SCHEMA_INSTANCE;
         default:
            return null;
        }
    }

    /**
     * Get the namespace URI part of a system-defined name as a URI code
     * @param fingerprint the fingerprint of the name
     * @return the namespace URI part of the name, as a URI code
     */

    public static short getURICode(int fingerprint) {
        int c = fingerprint >> 7;
        switch (c) {
        case DFLT_NS:
            return 0;
        case XSL_NS:
            return NamespaceConstant.XSLT_CODE;
        case SAXON_NS:
            return NamespaceConstant.SAXON_CODE;
        case XML_NS:
            return NamespaceConstant.XML_CODE;
        case XS_NS:
            return NamespaceConstant.SCHEMA_CODE;
        case XSI_NS:
            return NamespaceConstant.XSI_CODE;
        case IXSL_NS:
        	return NamespaceConstant.IXSL_CODE;
        default:
            return -1;
        }
    }

    /**
     * Get the Clark form of a system-defined name, given its name code or fingerprint
     * @param fingerprint the fingerprint of the name
     * @return the local name if the name is in the null namespace, or "{uri}local"
     *         otherwise. The name is always interned.
     */

    public static String getClarkName(int fingerprint) {
        String uri = getURI(fingerprint);
        if (uri.length() == 0) {
            return getLocalName(fingerprint);
        } else {
            return '{' + uri + '}' + getLocalName(fingerprint);
        }
    }

    /**
     * Get the conventional prefix of a system-defined name
     * @param fingerprint the fingerprint of the name
     * @return the conventional prefix of the name
     */

    public static String getPrefix(int fingerprint) {
        int c = fingerprint >> 7;
        switch (c) {
        case DFLT_NS:
            return "";
        case XSL_NS:
            return "xsl";
        case SAXON_NS:
            return "claxon";
        case XML_NS:
            return "xml";
        case XS_NS:
            return "xs";
        case XSI_NS:
            return "xsi";
        case IXSL_NS:
        	return "ixsl";
        default:
            return null;
        }
    }

    /**
     * Get the lexical display form of a system-defined name
     * @param fingerprint the fingerprint of the name
     * @return the lexical display form of the name, using a conventional prefix
     */

    public static String getDisplayName(int fingerprint) {
        if (fingerprint == -1) {
            return "(anonymous type)";
        }
        if (fingerprint > 1023) {
            return "(" + fingerprint + ')';
        }
        if ((fingerprint >> 7) == DFLT) {
            return getLocalName(fingerprint);
        }
        return getPrefix(fingerprint) + ':' + getLocalName(fingerprint);
    }

    /**
     * Get a StructuredQName representing a system-defined name
     * @param fingerprint the fingerprint of the name
     * @return a StructuredQName representing the system-defined name
     */

    public static StructuredQName getStructuredQName(int fingerprint) {
        return new StructuredQName(getPrefix(fingerprint), getURI(fingerprint), getLocalName(fingerprint));
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.