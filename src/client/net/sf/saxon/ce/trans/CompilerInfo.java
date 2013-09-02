package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.lib.ErrorListener;
import client.net.sf.saxon.ce.om.StructuredQName;


/**
 * This class exists to hold information associated with a specific XSLT compilation episode.
 * In JAXP, the URIResolver and ErrorListener used during XSLT compilation are those defined in the
 * TransformerFactory. The .NET API and the s9api API, however, allow finer granularity,
 * and this class exists to support that.
 */

public class CompilerInfo {

    private transient ErrorListener errorListener;
    private boolean versionWarning;
    private StructuredQName defaultInitialMode;
    private StructuredQName defaultInitialTemplate;


    /**
     * Create an empty CompilerInfo object with default settings
     */

    public CompilerInfo() {}

    /**
     * Set the ErrorListener to be used during this compilation episode
     * @param listener The error listener to be used. This is notified of all errors detected during the
     * compilation.
     * @since 8.7
     */

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Get the ErrorListener being used during this compilation episode
     * @return listener The error listener in use. This is notified of all errors detected during the
     * compilation.
     * @since 8.7
     */

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    /**
     * Ask whether a warning is to be output when the stylesheet version does not match the processor version.
     * In the case of stylesheet version="1.0", the XSLT specification requires such a warning unless the user disables it.
     *
     * @return true if these messages are to be output.
     * @since 9.2
     */

    public boolean isVersionWarning() {
        return versionWarning;
    }

    /**
     * Say whether a warning is to be output when the stylesheet version does not match the processor version.
     * In the case of stylesheet version="1.0", the XSLT specification requires such a warning unless the user disables it.
     *
     * @param warn true if these messages are to be output.
     * @since 9.2
     */

    public void setVersionWarning(boolean warn) {
        versionWarning = warn;
    }

    /**
     * Set the default initial template name for a stylesheet compiled using this CompilerInfo.
     * This is only a default; it can be overridden when the stylesheet is executed
     * @param initialTemplate the name of the default initial template, or null if there is
     * no default. No error occurs (until run-time) if the stylesheet does not contain a template
     * with this name.
     * @since 9.3
     */

    public void setDefaultInitialTemplate(StructuredQName initialTemplate) {
        defaultInitialTemplate = initialTemplate;
    }

    /**
     * Get the default initial template name for a stylesheet compiled using this CompilerInfo.
     * This is only a default; it can be overridden when the stylesheet is executed
     * @return the name of the default initial template, or null if there is
     * no default, as set using {@link #setDefaultInitialTemplate}
     * @since 9.3
     */

    public StructuredQName getDefaultInitialTemplate() {
        return defaultInitialTemplate;
    }

    /**
     * Set the default initial mode name for a stylesheet compiled using this CompilerInfo.
     * This is only a default; it can be overridden when the stylesheet is executed
     * @param initialMode the name of the default initial mode, or null if there is
     * no default. No error occurs (until run-time) if the stylesheet does not contain a mode
     * with this name.
     * @since 9.3
     */

    public void setDefaultInitialMode(StructuredQName initialMode) {
        defaultInitialMode = initialMode;
    }

    /**
     * Get the default initial mode name for a stylesheet compiled using this CompilerInfo.
     * This is only a default; it can be overridden when the stylesheet is executed
     * @return the name of the default initial mode, or null if there is
     * no default, as set using {@link #setDefaultInitialMode}
     * @since 9.3
     */

    public StructuredQName getDefaultInitialMode() {
        return defaultInitialMode;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.