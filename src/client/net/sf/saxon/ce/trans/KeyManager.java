package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.SlotManager;
import client.net.sf.saxon.ce.expr.sort.LocalOrderComparer;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.ListIterator;
import client.net.sf.saxon.ce.tree.iter.SingleNodeIterator;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.BuiltInType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.DoubleValue;
import client.net.sf.saxon.ce.value.NumericValue;

import java.util.*;

/**
  * KeyManager manages the set of key definitions in a stylesheet, and the indexes
  * associated with these key definitions. It handles xsl:sort-key as well as xsl:key
  * definitions.
  *
  * <p>The memory management in this class is subtle, with extensive use of weak references.
  * The idea is that an index should continue to exist in memory so long as both the compiled
  * stylesheet and the source document exist in memory: if either is removed, the index should
  * go too. The document itself holds no reference to the index. The compiled stylesheet (which
  * owns the KeyManager) holds a weak reference to the index. The index, of course, holds strong
  * references to the nodes in the document. The Controller holds a strong reference to the
  * list of indexes used for each document, so that indexes remain in memory for the duration
  * of a transformation even if the documents themselves are garbage collected.</p>
  *
  * <p>Potentially there is a need for more than one index for a given key name, depending
  * on the primitive type of the value provided to the key() function. An index is built
  * corresponding to the type of the requested value; if subsequently the key() function is
  * called with the same name and a different type of value, then a new index is built.</p>
  *
  * <p>For XSLT-defined keys, equality matching follows the rules of the eq operator, which means
  * that untypedAtomic values are treated as strings. In backwards compatibility mode, <i>all</i>
  * values are converted to strings.</p>
 *
 * <p>This class is also used for internal indexes constructed (a) to support the idref() function,
 * and (b) (in Saxon-EE only) to support filter expressions of the form /a/b/c[d=e], where the
 * path expression being filtered must be a single-document context-free path rooted at a document node,
 * where exactly one of d and e must be dependent on the focus, and where certain other conditions apply
 * such as the filter predicate not being positional. The operator in this case may be either "=" or "eq".
 * If it is "eq", then the semantics are very similar to xsl:key indexes, except that use of non-comparable
 * types gives an error rather than a non-match. If the operator is "=", however, then the rules for
 * handling untypedAtomic values are different: these must be converted to the type of the other operand.
 * In this situation the following rules apply. Assume that the predicate is [use=value], where use is
 * dependent on the focus (the indexed value), and value is the sought value.</p>
 *
 * <ul>
 * <li>If value is a type other than untypedAtomic, say T, then we build an index for type T, in which any
 * untypedAtomic values that arise in evaluating "use" are converted to type T. A conversion failure results
 * in an error. A value of a type that is not comparable to T also results in an error.</li>
 * <li>If value is untypedAtomic, then we build an index for every type actually encountered in evaluating
 * the use expression (treating untypedAtomic as string), and then search each of these indexes. (Note that
 * it is not an error if the use expression returns a mixture of say numbers and dates, provided that the
 * sought value is untypedAtomic).</li>
 * </ul>
  *
  * @author Michael H. Kay
  */

public class KeyManager {

    private HashMap<StructuredQName, KeyDefinitionSet> keyMap;
                                     // one entry for each named key; the entry contains
                                     // a KeyDefinitionSet holding the key definitions with that name
    private transient HashMap<DocumentInfo, HashMap<Long, Object>> docIndexes;
                                     // one entry for each document that is in memory;
                                     // the entry contains a HashMap mapping the fingerprint of
                                     // the key name plus the primitive item type
                                     // to the HashMap that is the actual index
                                     // of key/value pairs.

    /**
     * Create a KeyManager and initialise variables
     */

    public KeyManager() {
        keyMap = new HashMap<StructuredQName, KeyDefinitionSet>(10);
        docIndexes = new HashMap<DocumentInfo, HashMap<Long, Object>>(10);
    }

    /**
     * Pre-register a key definition. This simply registers that a key with a given name exists,
     * without providing any details.
     * @param keyName the name of the key to be pre-registered
     */

    public void preRegisterKeyDefinition(StructuredQName keyName) {
        KeyDefinitionSet keySet = keyMap.get(keyName);
        if (keySet==null) {
            keySet = new KeyDefinitionSet(keyName, keyMap.size());
            keyMap.put(keyName, keySet);
        }
    }

    /**
     * Register a key definition. Note that multiple key definitions with the same name are
     * allowed
     * @param keyName Structured QName representing the name of the key
     * @param keydef The details of the key's definition
     * @param config The configuration
     * @throws XPathException if this key definition is inconsistent with existing key definitions having the same name
     */

    public void addKeyDefinition(StructuredQName keyName, KeyDefinition keydef, Configuration config) throws XPathException {
        KeyDefinitionSet keySet = keyMap.get(keyName);
        if (keySet==null) {
            keySet = new KeyDefinitionSet(keyName, keyMap.size());
            keyMap.put(keyName, keySet);
        }
        keySet.addKeyDefinition(keydef);

        boolean backwardsCompatible = keySet.isBackwardsCompatible();

        if (backwardsCompatible) {
            // In backwards compatibility mode, convert all the use-expression results to sequences of strings
            List v = keySet.getKeyDefinitions();
            for (int i=0; i<v.size(); i++) {
                KeyDefinition kd = (KeyDefinition)v.get(i);
                kd.setBackwardsCompatible(true);
                if (!kd.getBody().getItemType(config.getTypeHierarchy()).equals(BuiltInAtomicType.STRING)) {
                    Expression exp = new AtomicSequenceConverter(kd.getBody(), BuiltInAtomicType.STRING);
                    kd.setBody(exp);
                }
            }
        }

    }

    /**
    * Get all the key definitions that match a particular name
    * @param qName The name of the required key
    * @return The set of key definitions of the named key if there are any, or null otherwise.
    */

    public KeyDefinitionSet getKeyDefinitionSet(StructuredQName qName) {
        return keyMap.get(qName);
    }

    /**
     * Build the index for a particular document for a named key
     * @param keySet The set of key definitions with this name
     * @param itemType the type of the values to be indexed.
     * @param foundItemTypes Optional (may be null). If supplied, a set that is to be populated with
     * the set of primitive types actually found among the "use" values.
     * @param doc The source document in question
     * @param context The dynamic context
     * @return the index in question, as a HashMap mapping a key value onto a ArrayList of nodes
    */

    private synchronized HashMap buildIndex(KeyDefinitionSet keySet,
                                            BuiltInAtomicType itemType,
                                            Set<BuiltInAtomicType> foundItemTypes,
                                            DocumentInfo doc,
                                            XPathContext context) throws XPathException {

        List<KeyDefinition> definitions = keySet.getKeyDefinitions();
        HashMap<Object, List<NodeInfo>> index = new HashMap<Object, List<NodeInfo>>(100);

        // There may be multiple xsl:key definitions with the same name. Index them all.
        for (int k=0; k<definitions.size(); k++) {
            constructIndex( doc, index, definitions.get(k), itemType, foundItemTypes, context, k == 0);
        }

        return index;

    }

    /**
     * Process one key definition to add entries to an index
     * @param doc the document to be indexed
     * @param index the index to be built
     * @param keydef the key definition used to build the index
     * @param soughtItemType the primitive type of the value that the user is searching for on the call
     * to the key() function that triggered this index to be built
     * @param foundItemTypes Optional (may be null): if supplied, a Set to be populated with the set of
     * primitive types actually found for the use expression
     * @param context the XPath dynamic evaluation context
     * @param isFirst true if this is the first index to be built for this key
     */

    private void constructIndex(    DocumentInfo doc,
                                    HashMap<Object, List<NodeInfo>> index,
                                    KeyDefinition keydef,
                                    BuiltInAtomicType soughtItemType,
                                    Set<BuiltInAtomicType> foundItemTypes,
                                    XPathContext context,
                                    boolean isFirst) throws XPathException {
        //System.err.println("build index for doc " + doc.getDocumentNumber());
        Pattern match = keydef.getMatch();

        //NodeInfo curr;
        XPathContextMajor xc = context.newContext();

        // The use expression (or sequence constructor) may contain local variables.
        SlotManager map = keydef.getStackFrameMap();
        if (map != null) {
            xc.openStackFrame(map);
        }

        SequenceIterator iter = match.selectNodes(doc, xc);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            processKeyNode((NodeInfo)item, soughtItemType, foundItemTypes, keydef, index, xc, isFirst);
        }
    }

    /**
    * Process one matching node, adding entries to the index if appropriate
     * @param curr the node being processed
     * @param soughtItemType the primitive item type of the argument to the key() function that triggered
     * this index to be built
     * @param foundItemTypes Optional (may be null): if supplied, a Set to be populated with the set of
     * primitive types actually found for the use expression
     * @param keydef the key definition
     * @param index the index being constructed
     * @param xc the context for evaluating expressions
     * @param isFirst indicates whether this is the first key definition with a given key name (which means
     * no sort of the resulting key entries is required)
    */

    private void processKeyNode(    NodeInfo curr,
                                    BuiltInAtomicType soughtItemType,
                                    Set<BuiltInAtomicType> foundItemTypes,
                                    KeyDefinition keydef,
                                    HashMap<Object, List<NodeInfo>> index,
                                    XPathContext xc,
                                    boolean isFirst) throws XPathException {


        // Make the node we are testing the context node,
        // with context position and context size set to 1

        AxisIterator si = SingleNodeIterator.makeIterator(curr);
        si.next();    // need to position iterator at first node

        xc.setCurrentIterator(si);

        StringCollator collation = keydef.getCollation();

        // Evaluate the "use" expression against this context node

        Expression use = keydef.getUse();
        SequenceIterator useval = use.iterate(xc);
        while (true) {
            AtomicValue item = (AtomicValue)useval.next();
            if (item == null) {
                break;
            }
            BuiltInAtomicType actualItemType = item.getPrimitiveType();
            if (foundItemTypes != null) {
                foundItemTypes.add(actualItemType);
            }
            if (!Type.isComparable(actualItemType, soughtItemType, false)) {
                // the types aren't comparable
                // simply ignore this key value
                continue;
            }
            Object val;

            if (soughtItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC) ||
                    soughtItemType.equals(BuiltInAtomicType.STRING) ||
                    soughtItemType.equals(BuiltInAtomicType.ANY_URI)) {
                // If the supplied key value is untyped atomic, we build an index using the
                // actual type returned by the use expression
                // If the supplied key value is a string, there is no match unless the use expression
                // returns a string or an untyped atomic value
                if (collation == null) {
                    val = item.getStringValue();
                } else {
                    val = collation.getCollationKey(item.getStringValue());
                }
            } else {
                // Ignore NaN values
                if (item.isNaN()) {
                    break;
                }
                try {
                    AtomicValue av = item.convert(soughtItemType, true).asAtomic();
                    val = av.getXPathComparable(false, collation, xc);
                } catch (XPathException err) {
                    // ignore values that can't be converted to the required type
                    break;
                }
            }

            List<NodeInfo> nodes = index.get(val);
            if (nodes==null) {
                // this is the first node with this key value
                nodes = new ArrayList<NodeInfo>(4);
                index.put(val, nodes);
                nodes.add(curr);
            } else {
                // this is not the first node with this key value.
                // add the node to the list of nodes for this key,
                // unless it's already there
                if (isFirst) {
                    // if this is the first index definition that we're processing,
                    // then this node must be after all existing nodes in document
                    // order, or the same node as the last existing node
                    if (nodes.get(nodes.size()-1)!=curr) {
                        nodes.add(curr);
                    }
                } else {
                    // otherwise, we need to insert the node at the correct
                    // position in document order. This code does an insertion sort:
                    // not ideal for performance, but it's very unusual to have more than
                    // one key definition for a key.
                    LocalOrderComparer comparer = LocalOrderComparer.getInstance();
                    boolean found = false;
                    for (int i=0; i<nodes.size(); i++) {
                        int d = comparer.compare(curr, nodes.get(i));
                        if (d<=0) {
                            if (d==0) {
                                // node already in list; do nothing
                            } else {
                                // add the node at this position
                                nodes.add(i, curr);
                            }
                            found = true;
                            break;
                        }
                        // else continue round the loop
                    }
                    // if we're still here, add the new node at the end
                    if (!found) {
                        nodes.add(curr);
                    }
                }
            }
        }

    }


    /**
    * Get the nodes with a given key value
    * @param keySet The set of key definitions identified by the key name used in the call to the key() function
    * @param doc The source document in question
    * @param soughtValue The required key value
    * @param context The dynamic context, needed only the first time when the key is being built
    * @return an iteration of the selected nodes, always in document order with no duplicates
    */

    public SequenceIterator selectByKey(
                                KeyDefinitionSet keySet,
                                DocumentInfo doc,
                                AtomicValue soughtValue,
                                XPathContext context) throws XPathException {

        //System.err.println("*********** USING KEY ************");
        if (soughtValue == null) {
            return EmptyIterator.getInstance();
        }
        List definitions = keySet.getKeyDefinitions();
//        if (definitions == null) {
//            throw new XPathException("Key " + context.getNamePool().getDisplayName(keyNameFingerprint) +
//            							" has not been defined", "XTDE1260", context);
//        }
        KeyDefinition definition = (KeyDefinition)definitions.get(0);
               // the itemType and collation and BC mode will be the same for all keys with the same name
        StringCollator collation = definition.getCollation();
        
        if (keySet.isBackwardsCompatible()) {
            // if backwards compatibility is in force, treat all values as strings
            soughtValue = soughtValue.convert(BuiltInAtomicType.STRING, true).asAtomic();
        } else {
            // If the key value is numeric, promote it to a double
            // TODO: this could result in two decimals comparing equal because they convert to the same double

            BuiltInAtomicType itemType = soughtValue.getPrimitiveType();
            if (itemType.equals(BuiltInAtomicType.INTEGER) ||
                    itemType.equals(BuiltInAtomicType.DECIMAL) ||
                    itemType.equals(BuiltInAtomicType.FLOAT)) {
                soughtValue = new DoubleValue(((NumericValue)soughtValue).getDoubleValue());
            }
        }

        // NOTE: This is much more elaborate than it needs to be. The option convertUntypedToOther
        // is used for an index used to support a general comparison. This reports an error if two
        // non-comparable values are compared. We could report an error immediately if foundItemTypes
        // includes a type that is not comparable to the soughtValue. In practice we only need a maximum
        // of two indexes: one for the sought item type, and one for untypedAtomic.

        HashSet<BuiltInAtomicType> foundItemTypes = null;
        AtomicValue value = soughtValue;

        // No special action needed for anyURI to string promotion (it just seems to work: tests idky44, 45)

        int keySetNumber = keySet.getKeySetNumber();
        BuiltInAtomicType itemType = value.getPrimitiveType();
        HashMap index;

        Object indexObject = getIndex(doc, keySetNumber, itemType);
        if (indexObject instanceof String) {
            // index is under construction
            XPathException de = new XPathException("Key definition is circular");
            de.setXPathContext(context);
            de.setErrorCode("XTDE0640");
            throw de;
        }
        index = (HashMap)indexObject;

        // If the index does not yet exist, then create it.
        if (index==null) {
            // Mark the index as being under construction, in case the definition is circular
            putIndex(doc, keySetNumber, itemType, "Under Construction", context);
            index = buildIndex(keySet, itemType, foundItemTypes, doc, context);
            putIndex(doc, keySetNumber, itemType, index, context);
            if (foundItemTypes != null) {
                // build indexes for each item type actually found
                for (Iterator<BuiltInAtomicType> f = foundItemTypes.iterator(); f.hasNext();) {
                    BuiltInAtomicType t = f.next();
                    if (!t.equals(BuiltInAtomicType.STRING)) {
                        putIndex(doc, keySetNumber, t, "Under Construction", context);
                        index = buildIndex(keySet, t, null, doc, context);
                        putIndex(doc, keySetNumber, t, index, context);
                    }
                }
            }
        }


        if (foundItemTypes == null) {
            ArrayList nodes = (ArrayList)index.get(getCollationKey(value, itemType, collation, context));
            if (nodes==null) {
                return EmptyIterator.getInstance();
            } else {
                return new client.net.sf.saxon.ce.tree.iter.ListIterator(nodes);
            }
        } else {
            // we need to search the indexes for all possible types, and combine the results.
            SequenceIterator result = null;
            HashMap<Long, Object> docIndex = docIndexes.get(doc);
            if (docIndex != null) {
                for (Iterator<Long> i= index.keySet().iterator(); i.hasNext();) {
                    long key = (i.next()).longValue();
                    if (((key >> 32)) == keySetNumber) {
                        int typefp = (int)key;

                        BuiltInAtomicType type = (BuiltInAtomicType)BuiltInType.getSchemaType(typefp);

                        Object indexObject2 = getIndex(doc, keySetNumber, type);
                        if (indexObject2 instanceof String) {
                            // index is under construction
                            XPathException de = new XPathException("Key definition is circular");
                            de.setXPathContext(context);
                            de.setErrorCode("XTDE0640");
                            throw de;
                        }
                        HashMap index2 = (HashMap)indexObject2;
                        // NOTE: we've been known to encounter a null index2 here, but it doesn't seem possible
                        if (!index2.isEmpty()) {
                            value = soughtValue.convert(type, true).asAtomic();
                            ArrayList nodes = (ArrayList)index2.get(getCollationKey(value, type, collation, context));
                            if (nodes != null) {
                                if (result == null) {
                                    result = new ListIterator(nodes);
                                } else {
                                    result = new UnionEnumeration(result, new ListIterator(nodes), LocalOrderComparer.getInstance());
                                }
                            }
                        }
                    }
                }
            }
            if (result == null) {
                return EmptyIterator.getInstance();
            } else {
                return result;
            }
        }
    }

    private static Object getCollationKey(AtomicValue value, BuiltInAtomicType itemType,
                                          StringCollator collation, XPathContext context) throws XPathException {
        Object val;
        if (itemType.equals(BuiltInAtomicType.STRING) ||
                itemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC) ||
                itemType.equals(BuiltInAtomicType.ANY_URI)) {
            if (collation==null) {
                val = value.getStringValue();
            } else {
                val = collation.getCollationKey(value.getStringValue());
            }
        } else {
            val = value.getXPathComparable(false, collation, context);
        }
        return val;
    }

    /**
    * Save the index associated with a particular key, a particular item type,
    * and a particular document. This
    * needs to be done in such a way that the index is discarded by the garbage collector
    * if the document is discarded. We therefore use a WeakHashMap indexed on the DocumentInfo,
    * which returns HashMap giving the index for each key fingerprint. This index is itself another
    * HashMap.
    * The methods need to be synchronized because several concurrent transformations (which share
    * the same KeyManager) may be creating indexes for the same or different documents at the same
    * time.
     * @param doc the document being indexed
     * @param keyFingerprint represents the name of the key definition
     * @param itemType the primitive type of the values being indexed
     * @param index the index being saved
     * @param context the dynamic evaluation context
    */

    private synchronized void putIndex(DocumentInfo doc, int keyFingerprint,
                                       AtomicType itemType, Object index, XPathContext context) {
        if (docIndexes==null) {
            // it's transient, so it will be null when reloading a compiled stylesheet
            docIndexes = new HashMap<DocumentInfo, HashMap<Long, Object>>(10);
        }
        HashMap<Long, Object> indexRef = docIndexes.get(doc);
        HashMap<Long, Object> indexList;
        if (indexRef==null) {
            indexList = new HashMap<Long, Object>(10);
            // Ensure there is a firm reference to the indexList for the duration of a transformation
            // But for keys associated with temporary trees, or documents that have been discarded from
            // the document pool, keep the reference within the document node itself.
            Controller controller = context.getController();
            if (controller.getDocumentPool().contains(doc)) {
                context.getController().setUserData(doc, "saxon:key-index-list", indexList);
            } else {
                doc.setUserData("saxon:key-index-list", indexList);
            }
            docIndexes.put(doc, new HashMap<Long, Object>(indexList));
        } else {
            indexList = indexRef;
        }
        indexList.put(Long.valueOf(((long)keyFingerprint)<<32 | itemType.getFingerprint()), index);
    }

    /**
     * Get the index associated with a particular key, a particular source document,
     * and a particular primitive item type
     * @param doc the document whose index is required
     * @param keyFingerprint the name of the key definition
     * @param itemType the primitive item type of the values being indexed
     * @return either an index (as a HashMap), or the String "under construction", or null
    */

    private synchronized Object getIndex(DocumentInfo doc, int keyFingerprint, AtomicType itemType) {
        if (docIndexes==null) {
            // it's transient, so it will be null when reloading a compiled stylesheet
            docIndexes = new HashMap<DocumentInfo, HashMap<Long, Object>>(10);
        }
        HashMap<Long, Object> docIndex = docIndexes.get(doc);
        if (docIndex==null) return null;
        return docIndex.get(Long.valueOf(((long)keyFingerprint)<<32 | itemType.getFingerprint()));
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
