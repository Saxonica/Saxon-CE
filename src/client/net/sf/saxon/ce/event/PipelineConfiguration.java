package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.lib.ErrorListener;

/**
 * A PipelineConfiguration sets options that apply to all the operations in a pipeline.
 * Unlike the global Configuration, these options are always local to a process.
 */

public class PipelineConfiguration {

    private Configuration config;
    private ErrorListener errorListener;
    private Controller controller;

    /**
     * Create a PipelineConfiguration. Note: the normal way to create
     * a PipelineConfiguration is via the factory methods in the Controller and
     * Configuration classes
     * @see Configuration#makePipelineConfiguration
     * @see Controller#makePipelineConfiguration
     */

    public PipelineConfiguration() {
    }

    /**
     * Create a PipelineConfiguration as a copy of an existing
     * PipelineConfiguration
     * @param p the existing PipelineConfiguration
     */

    public PipelineConfiguration(PipelineConfiguration p) {
        config = p.config;
        controller = p.controller;
    }

    /**
     * Get the Saxon Configuration object
     * @return the Saxon Configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the Saxon Configuration object
     * @param config the Saxon Configuration
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public void setErrorListener(ErrorListener listener) {
        this.errorListener = listener;
    }

    /**
     * Get the controller associated with this pipelineConfiguration
     * @return the controller if it is known; otherwise null.
     */

    public Controller getController() {
        return controller;
    }

    /**
     * Set the Controller associated with this pipelineConfiguration
     * @param controller the Controller
     */

    public void setController(Controller controller) {
        this.controller = controller;
    }

  }

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
