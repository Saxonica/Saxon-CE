package client.net.sf.saxon.ce;

import client.net.sf.saxon.ce.lib.GenericLogHandler;
import client.net.sf.saxon.ce.lib.JsLogHandler;
import client.net.sf.saxon.ce.lib.ListenerLogHandler;
import client.net.sf.saxon.ce.lib.TraceListener;
import client.net.sf.saxon.ce.trace.XSLTTraceListener;
import com.google.gwt.logging.client.HasWidgetsLogHandler;
import com.google.gwt.logging.client.LoggingPopup;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasWidgets;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogController {
	
	/**
	 * Class for managing the GWT Logging
	 * Proposed use of levels:
	 * - OFF
		- SEVERE GWT Exceptions and internal errors all fatal xslt errors
		- WARNING GWT Warnings and bad but not fatal internal conditions
		- INFO xsl:message and fn:trace output
		- CONFIG Configuration data
		- FINE High-level XSLT calls to the public API = Main transformation, inward calls such as events
		- FINER XSLT/XPath/IXSL function and template calls
		- FINEST Output from TraceExpression - XSLT instructions within templates and functions
		
		SystemLogHandler - not included
		HasWidgetsLogHandler - the popup view
		DevelopmentModeLogHandler
		ConsoleLogHandler - IE or FireBug Lite consoles
		SimpleRemoteLogHandler
		FirebugLogHandler
		-----------------
		GenericLogHandler - in-house mod improves on ConsoleLogHandler + FirebugHandler
		JSLogHandler - Raises JavaScript event for each log item
		
		Set properties in Saxonce.gwt.xml to control logging: e.g.
		
		<set-property name="gwt.logging.logLevel" value="SEVERE"/>          # To change the default logLevel
        <set-property name="gwt.logging.enabled" value="FALSE"/>            # To disable logging
        <set-property name="gwt.logging.consoleHandler" value="DISABLED"/>  # To disable a default Handler
        <set-property name="gwt.logging.popupHandler" value="DISABLED"/>    # To disable the popupHandler
	 */
	
	private static boolean isTraceEnabled;
	private static Logger mainLogger;
	private static Level initLogLevel;
	
	private LogController(){
		// Class can not be instantiated	
	}
	
	public static void initLogger() {
		mainLogger = Logger.getLogger("");
		String logLevel = Window.Location.getParameter("logLevel");
		// if set, this prevents the JS API overriding the URI parameter
		initLogLevel = (logLevel == null)? null : Level.parse(logLevel);
	}
	
	public static boolean LoggingIsDisabledByURI() {
		return mainLogger.getLevel() == Level.OFF; // home.toString().indexOf("logLevel=OFF") > -1;
	}
	
	private static TraceListener traceListener = null;
	
	public static void InitializeTraceListener() {
		checkTraceIsEnabled();
		if (isTraceEnabled) {
			traceListener = new XSLTTraceListener();
		}
	}
	
	public static void openTraceListener() {		
		if (isTraceEnabled) {
			traceListener.open();
		}
	}
	
	public static void closeTraceListener(boolean success) {
		if (traceListener != null) {
			if (success) {
				if (isTraceEnabled) {
					traceListener.close();
				}
			} else {
				((XSLTTraceListener)traceListener).terminate();
			}
		}
	}
	

	public static TraceListener getTraceListener() {
		return traceListener;
	}
	
	public static boolean traceIsEnabled() {
		return isTraceEnabled;
	}
	
	private static boolean checkTraceIsEnabled() {
		isTraceEnabled = mainLogger.getLevel() == Level.FINEST;
		return isTraceEnabled;
	}
	
	public static void addJavaScriptLogHandler() {
		if (!LoggingIsDisabledByURI()) {			
			Logger.getLogger("").addHandler(new ListenerLogHandler());
		}
	}
	
	private static JsLogHandler jsLogHandler = null;
	
	public static JsLogHandler getJsLogHandler() {
		return jsLogHandler;
	}
	
	public static void setLogLevel(String newLevel){
		if (initLogLevel == null) {
			try {
				mainLogger.setLevel(Level.parse(newLevel));
			} catch (Exception e){
				Logger.getLogger("LogController").severe("invalid level for setLogLevel: " + newLevel);
			}
			
		}
	}
	
	public static String getLogLevel(){
		return mainLogger.getLevel().getName();
	}
	

	public static void addRequiredLogHanders(LogRecord record) {
		jsLogHandler = new JsLogHandler();
		mainLogger.addHandler(jsLogHandler);
		jsLogHandler.publish(record);
		
		GenericLogHandler gHandler = new GenericLogHandler();
					
		// popup & firebug must be disabled using Saxonce.gwt.xml and enabled
		// below - if required
		if (gHandler.isSupported()) {
			mainLogger.addHandler(gHandler);
			gHandler.publish(record);
		} else if (!SaxonceApi.isLogHandlerExternal()) {
	        HasWidgets loggingWidget = new LoggingPopup();
	        HasWidgetsLogHandler hw = new HasWidgetsLogHandler(loggingWidget);
	        mainLogger.addHandler(hw);
	        hw.publish(record);
		}
	}

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is �Incompatible With Secondary Licenses�, as defined by the Mozilla Public License, v. 2.0.
