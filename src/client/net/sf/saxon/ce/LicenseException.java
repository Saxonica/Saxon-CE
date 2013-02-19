package client.net.sf.saxon.ce;

import com.google.gwt.logging.client.LogConfiguration;

public class LicenseException extends RuntimeException {

    private int reason;
    private String message = "";

    public static final int EXPIRED = 1;
    public static final int INVALID = 2;
    public static final int NOT_FOUND = 3;
    public static final int WRONG_FEATURES = 4;
    public static final int CANNOT_READ = 5;

    public LicenseException(String message, int reason) {
        super();
        if (LogConfiguration.loggingIsEnabled()){
        	this.message = message;
        }
        this.reason = reason;
    }
    
    public String getMessage() {
    	return this.message;
    }
    
    public String toString() {
    	return this.message;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
