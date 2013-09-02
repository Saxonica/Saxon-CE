package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.SlotManager;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.trans.KeyDefinition;
import client.net.sf.saxon.ce.trans.KeyManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Whitespace;


/**
* Handler for xsl:key elements in stylesheet. <br>
*/

public class XSLKey extends StyleElement implements StylesheetProcedure {

    private Pattern match;
    private Expression use;
    private String collationName;
    private StructuredQName keyName;
    SlotManager stackFrameMap;
                // needed if variables are used

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
      * Determine whether this type of element is allowed to contain a sequence constructor
      * @return true: yes, it may contain a sequence constructor
      */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Get the Procedure object that looks after any local variables declared in the content constructor
     */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }

    public void prepareAttributes() throws XPathException {

        String nameAtt = null;
        String matchAtt = null;
        String useAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
			if (f.equals("name")) {
        		nameAtt = Whitespace.trim(atts.getValue(a)) ;
        	} else if (f.equals("use")) {
        		useAtt = atts.getValue(a);
        	} else if (f.equals("match")) {
        		matchAtt = atts.getValue(a);
        	} else if (f.equals("collation")) {
        		collationName = Whitespace.trim(atts.getValue(a)) ;
        	} else {
        		checkUnknownAttribute(qn);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
            return;
        }
        try {
            keyName = makeQName(nameAtt);
            setObjectName(keyName);
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
        } catch (XPathException err) {
            compileError(err);
        }

        if (matchAtt==null) {
            reportAbsence("match");
            matchAtt = "*";
        }
        match = makePattern(matchAtt);

        if (useAtt!=null) {
            use = makeExpression(useAtt);
        }
    }

    public StructuredQName getKeyName() {
    	//We use null to mean "not yet evaluated"
        try {
        	if (getObjectName()==null) {
        		// allow for forwards references
        		String nameAtt = getAttributeValue("", "name");
        		if (nameAtt != null) {
        			setObjectName(makeQName(nameAtt));
                }
            }
            return getObjectName();
        } catch (NamespaceException err) {
            return null;          // the errors will be picked up later
        } catch (XPathException err) {
            return null;
        }
    }

    public void validate(Declaration decl) throws XPathException {

        stackFrameMap = new SlotManager();
        checkTopLevel(null);
        if (use!=null) {
            // the value can be supplied as a content constructor in place of a use expression
            if (hasChildNodes()) {
                compileError("An xsl:key element with a @use attribute must be empty", "XTSE1205");
            }
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:key/use", 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                use = TypeChecker.staticTypeCheck(
                                use,
                                SequenceType.makeSequenceType(BuiltInAtomicType.ANY_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_MORE),
                                false, role, makeExpressionVisitor());
            } catch (XPathException err) {
                compileError(err);
            }
        } else {
            if (!hasChildNodes()) {
                compileError("An xsl:key element must either have a @use attribute or have content", "XTSE1205");
            }
        }
        use = typeCheck(use);
        match = typeCheck("match", match);

        // Do a further check that the use expression makes sense in the context of the match pattern
        if (use != null) {
            use = makeExpressionVisitor().typeCheck(use, match.getNodeTest());
        }

        if (collationName != null) {
            URI collationURI;
            try {
                collationURI = new URI(collationName, true);
                if (!collationURI.isAbsolute()) {
                    URI base = new URI(getBaseURI());
                    collationURI = base.resolve(collationURI.toString());
                    collationName = collationURI.toString();
                }
            } catch (URI.URISyntaxException err) {
                compileError("Collation name '" + collationName + "' is not a valid URI");
                //collationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
            }
        } else {
            collationName = getDefaultCollationName();
        }
    }

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
        StructuredQName keyName = getKeyName();
        if (keyName != null) {
            top.getPreparedStylesheet().getExecutable().getKeyManager().preRegisterKeyDefinition(keyName);
        }
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        StaticContext env = getStaticContext();
        Configuration config = env.getConfiguration();
        StringCollator collator = null;
        if (collationName != null) {
            collator = getConfiguration().getNamedCollation(collationName);
            if (collator==null) {
                compileError("The collation name " + Err.wrap(collationName, Err.URI) + " is not recognized", "XTSE1210");
                collator = CodepointCollator.getInstance();
            }
            if (collator instanceof CodepointCollator) {
                // if the user explicitly asks for the codepoint collation, treat it as if they hadn't asked
                collator = null;
                collationName = null;

            } else {
                compileError("The collation used for xsl:key must be capable of generating collation keys", "XTSE1210");
            }
        }

        if (use==null) {
            Expression body = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));

            try {
                ExpressionVisitor visitor = makeExpressionVisitor();
                use = new Atomizer(body);
                use = visitor.simplify(use);
            } catch (XPathException e) {
                compileError(e);
            }

            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:key/use", 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                use = TypeChecker.staticTypeCheck(
                                use,
                                SequenceType.makeSequenceType(BuiltInAtomicType.ANY_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_MORE),
                                false, role, makeExpressionVisitor());
                // Do a further check that the use expression makes sense in the context of the match pattern
                use = makeExpressionVisitor().typeCheck(use, match.getNodeTest());


            } catch (XPathException err) {
                compileError(err);
            }
        }
        final TypeHierarchy th = config.getTypeHierarchy();
        BuiltInAtomicType useType = (BuiltInAtomicType)use.getItemType(th).getPrimitiveItemType();
        if (xPath10ModeIsEnabled()) {
            if (!useType.equals(BuiltInAtomicType.STRING) && !useType.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                use = new AtomicSequenceConverter(use, BuiltInAtomicType.STRING);
                useType = BuiltInAtomicType.STRING;
            }
        }
        allocateSlots(use);
        int slots = match.allocateSlots(getStaticContext(), stackFrameMap, 0);
        allocatePatternSlots(slots);
        //allocateSlots(new PatternSponsor(match));


        KeyManager km = getExecutable().getKeyManager();
        KeyDefinition keydef = new KeyDefinition(match, use, collationName, collator);
        keydef.setIndexedItemType(useType);
        keydef.setStackFrameMap(stackFrameMap);
        keydef.setSourceLocator(this);
        keydef.setExecutable(getExecutable());
        keydef.setBackwardsCompatible(xPath10ModeIsEnabled());
        try {
            km.addKeyDefinition(keyName, keydef, exec.getConfiguration());
        } catch (XPathException err) {
            compileError(err);
        }
        return null;
    }

    /**
     * Optimize the stylesheet construct
     * @param declaration
     */

    public void optimize(Declaration declaration) throws XPathException {
        // already done earlier
    }
}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
