package client.net.sf.saxon.ce.regex;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.z.IntRangeSet;
import client.net.sf.saxon.ce.expr.z.IntSet;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;

import java.util.HashMap;
import java.util.Map;


/**
 * This class provides knowledge of the names and contents of Unicode character blocks,
 * as referenced using the \p{IsXXXXX} construct in a regular expression. The underlying
 * data is in an XML resource file UnicodeBlocks.xml, which is accessed from the server
 * the first time it is needed.
 */
public class UnicodeBlocks {

    private static Map<String, IntSet> blocks = null;

    public static IntSet getBlock(String name) throws RESyntaxException {
        if (blocks == null) {
            readBlocks(new Configuration());
        }
        IntSet cc = blocks.get(name);
        if (cc != null) {
            return cc;
        }
        cc = blocks.get(normalizeBlockName(name));
        return cc;
    }

    private static String normalizeBlockName(String name) {
        FastStringBuffer fsb = new FastStringBuffer(name.length());
        for (int i=0; i<name.length(); i++) {
            final char c = name.charAt(i);
            switch (c) {
                case ' ': case '\t': case '\r': case '\n': case '_':
                    // no action
                    break;
                default:
                    fsb.append(c);
            }
        }
        return fsb.toString();
    }

    private synchronized static void readBlocks(Configuration config) throws RESyntaxException {
        blocks = new HashMap<String, IntSet>(250);
        DocumentInfo doc;
        try {
            doc = config.buildDocument("unicodeBlocks.xml");
        } catch (XPathException err) {
            throw new RESyntaxException("Unable to read unicodeBlocks.xml file");
        }


        AxisIterator iter = doc.iterateAxis(Axis.DESCENDANT, new NameTest(Type.ELEMENT, "", "block"));
        while (true) {
            NodeInfo item = (NodeInfo)iter.next();
            if (item == null) {
                break;
            }
            String blockName = normalizeBlockName(Navigator.getAttributeValue(item, "", "name"));
            IntRangeSet range = null;
            AxisIterator ranges = item.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
            while (true) {
                NodeInfo rangeElement = (NodeInfo)ranges.next();
                if (rangeElement == null) {
                    break;
                }
                int from = Integer.parseInt(Navigator.getAttributeValue(rangeElement, "", "from").substring(2), 16);
                int to = Integer.parseInt(Navigator.getAttributeValue(rangeElement, "", "to").substring(2), 16);
                if (range == null) {
                    range = new IntRangeSet(new int[]{from}, new int[]{to});
                } else {
                    range.addRange(from, to);
                }
            }
            blocks.put(blockName, range);
        }

    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.