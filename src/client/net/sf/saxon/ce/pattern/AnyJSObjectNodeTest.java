package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.js.JSObjectValue;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.type.*;

public class AnyJSObjectNodeTest extends NodeTest {

    private static AnyJSObjectNodeTest THE_INSTANCE = new AnyJSObjectNodeTest();

    public static AnyJSObjectNodeTest getInstance() {
        return THE_INSTANCE;
    }

    public boolean isAtomicType() {
        return false;
    }

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config) {
        return item instanceof JSObjectValue;
    }

    public ItemType getSuperType(TypeHierarchy th) {
        return AnyItemType.getInstance();
    }

    public ItemType getPrimitiveItemType() {
        return AnyItemType.getInstance();
    }

    public int getRequiredNodeKind() {
        return Type.ITEM;
    }

    public String toString(NamePool pool) {
        return "JavaScriptObject";
    }

    public BuiltInAtomicType getAtomizedItemType() {
        return null;
    }

	@Override
	public double getDefaultPriority() {
		return 0;
	}

	@Override
	public boolean matches(int nodeKind, StructuredQName fingerprint, int annotation) {
        // not yet used for matching
		return false;
	}
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
