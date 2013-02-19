package client.net.sf.saxon.ce.lib;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.RuntimeException;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.SaxonceApi;

import com.google.gwt.logging.client.TextLogFormatter;

/**
 * A Handler that prints logs to the window.console - this is modelled on
 * the gwt firebug handler but designed for other consoles - for firebug,
 * the debug() method is called instead of the log() method
 */
public class GenericLogHandler extends Handler {
	
	private static boolean useHandler;
	private static boolean isFirebug;
	private static boolean isDirxml;

  public GenericLogHandler() {
	//previously:
    //setFormatter(new TextLogFormatter(true));
    setFormatter(new SaxonLogFormatter(false)); // false = don't show stack traces in browser console
    setLevel(Level.ALL);
    isFirebug = isFirebug();
    isDirxml = isDirxml();
    useHandler = (isSupported());
    
  }
  
  @Override
  public void close() {
    // No action needed
  }

  @Override
  public void flush() {
    // No action needed
  }
  
  private long previousMillis = 0;

  @Override
  public void publish(LogRecord record) {
	if (record == null) {
		return;
	}
	
    if (!isLoggable(record)) {
        return;
    }
        
	int val = record.getLevel().intValue();
	String msgText = record.getMessage();
	// prevent duplicate console messages to Chrome or re-thrown JavaScript API logging:
	if (val ==  Level.SEVERE.intValue()) {
		if (previousMillis == record.getMillis() || (previousMillis != 0 && msgText.startsWith("[js] "))) {
			return;
		}
		previousMillis = record.getMillis();
	}
	// add INFO and FINE log items to the timeline - supported by
	// latest chrome and firebug releases 
	
  	if (val == Level.INFO.intValue() || val == Level.FINE.intValue()){
		timeMark(msgText);
	}
  	    
    if (val == Level.FINEST.intValue()) {
    	aggFinest(msgText); // aggregate xml for later
    } else {
    	// add all info messages to the timeline


    	String msg = getFormatter().format(record);
	    if (val <= Level.FINE.intValue()) {
	    	logGeneral(msg);
	    } else if (val < Level.WARNING.intValue()) {
	        info(msg);
	    } else if (val < Level.SEVERE.intValue()) {
	        warn(msg);
	    } else {
	      if (LogController.traceIsEnabled() && isDirxml && finestSb != null) {
	    	  // xml will not be well-formed so dump as a string
	    	  if (finestSb.length() > 0) {
		    	  String m = finestSb.toString();
		    	  finestSb = new StringBuilder();
		    	  logGeneral(m);
	    	  }
	      }
	      error(msg);
	    }
    }
  }
  
  private void logGeneral(String msg) {
	  	if (isFirebug) {
		    debug(msg);	
		} else {
	        log(msg);
		}
	  }
  

  
  private static StringBuilder finestSb = null;
  
  public static void aggFinest(String str) {
	  if (finestSb == null) {
		  finestSb = new StringBuilder();
	  }
	  finestSb.append(str + "\n");
  }
  
 
  public static void dumpTrace() {
	  if (LogController.traceIsEnabled()) {
		  if (finestSb != null) {
			  if(isDirxml()){
			      logDirxml(finestSb.toString());
			  } else {
				  log(finestSb.toString());
			  }
		      finestSb = new StringBuilder();
		  }
	  }
  }
  
  public static native void logDirxml(String text) /*-{
    if (window.DOMParser) {
        parser = new DOMParser();
        xmlDoc = parser.parseFromString(text, "text/xml");
    }
    // For Internet Explorer - but IE doesn't support console.dirxml
    // so shouldn't ever be called
    else 
         
    {
        xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
        xmlDoc.async = false;
        xmlDoc.loadXML(text);
    }
    console.dirxml(xmlDoc); 
  }-*/;	

  public native boolean isSupported() /*-{
    return !!(window.console);
  }-*/;
  
  public native boolean isFirebug() /*-{
    return !!((window.console && window.console.firebug));
  }-*/;
  
  public static native boolean isDirxml() /*-{
     return !!((window.console && window.console.dirxml));
  }-*/;
  
  private native void debug(String message) /*-{
    window.console.debug(message);
  }-*/;
  
  private native boolean timeMark(String message) /*-{
  	    if ($wnd.console){
		  	if ($wnd.console.timeStamp) {
		  		$wnd.console.timeStamp(message);
		  		return true;
		  	} else if ($wnd.console.markTimeline) {
		  		$wnd.console.markTimeline(message);
		  		return true;
		  	}
  	    }
  	    return false;
  }-*/;
  
  private static native void log(String message) /*-{
    window.console.log(message);
  }-*/;
  

  private native void error(String message) /*-{
	if (window.console.error) {
      window.console.error(message);
	} else {
	window.console.log(message);	
	}
  }-*/;

  private native void info(String message) /*-{
	if (window.console.info) {
      window.console.info(message);
	} else {
	window.console.log(message);	
	}
  }-*/;


  private native void warn(String message) /*-{
	if (window.console.warn) {
      window.console.warn(message);
	} else {
	window.console.log(message);	
	}
  }-*/;
  
  

}
