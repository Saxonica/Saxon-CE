package client.net.sf.saxon.ce.lib;

/**
 * This Log Handler class does not perform logging, instead, it serves to listen
 * for the first log event. It then adds required Log Handlers based on the capabilities
 * of the host browser before removing itself. This delayed adding of handlers is mainly
 * to prevents a popup appearing if there's nothing to log, but it may also *marginally*
 * improve performance when no logging is required.
 */

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.SaxonceApi;

public class ListenerLogHandler extends Handler {
	
	public ListenerLogHandler() {
		
	}

	@Override
	public void close() {
		//No action
		
	}

	@Override
	public void flush() {
		// No action
		
	}
	
	private boolean removed = false;

	@Override
	public void publish(LogRecord record) {
		
		Logger logger = Logger.getLogger("");
		// this handler is no longer needed
		logger.removeHandler(this);
		// add new required handlers according to browser capabilities - and publish
		int handlerIndex = logger.getHandlers().length;
		LogController.addRequiredLogHanders(record);
		// call publish on newly added handlers - no need because publish is called on them next
		//LogController.getJsLogHandler().publish(record); // this handler is missed out - because its first?
		
		Logger localLogger = Logger.getLogger("ListenerLogHandler");
		localLogger.log(Level.FINE, "Log handlers added (" + (SaxonceApi.isLogHandlerExternal()? "includes external)": "internal only)"));		
	}

}
