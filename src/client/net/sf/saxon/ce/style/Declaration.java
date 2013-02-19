package client.net.sf.saxon.ce.style;

/**
 * The object represents a declaration (that is, a top-level element) in a stylesheet.
 * A declaration exists within a stylesheet module and takes its import precedence
 * from that of the module. The declaration corresponds to a source element in a stylesheet
 * document. However, if a stylesheet module is imported twice with different precedences,
 * then two declarations may share the same source element.
 */

public class Declaration {

    private StyleElement sourceElement;
    private StylesheetModule module;

    public Declaration(StylesheetModule module, StyleElement source) {
        this.module = module;
        this.sourceElement = source;
    }

    public StylesheetModule getModule() {
        return module;
    }

    public StyleElement getSourceElement() {
        return sourceElement;
    }

    public int getPrecedence() {
        return module.getPrecedence();
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


