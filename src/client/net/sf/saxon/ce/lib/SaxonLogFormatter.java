package client.net.sf.saxon.ce.lib;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.logging.impl.FormatterImpl;

import java.util.Date;
import java.util.logging.LogRecord;

/**
 * Formats LogRecords into 2 lines of text.
 */


public class SaxonLogFormatter extends FormatterImpl {
  private boolean showStackTraces;
  private static DateTimeFormat dtf = DateTimeFormat.getFormat("HH:mm:ss.SSS");
  
  public SaxonLogFormatter(boolean showStackTraces) {
    this.showStackTraces = showStackTraces;
  }

  @Override
  public String format(LogRecord event) {
    StringBuilder message = new StringBuilder();
    message.append(formatEvent(event));
    message.append(event.getMessage());
    if (showStackTraces) {
      message.append(getStackTraceAsString(event.getThrown(), "\n", "\t"));
    }
    return message.toString();
  }
  
  private static String formatEvent(LogRecord event) {
	  // this required number of milliseconds since midnight
	  Date date = new Date(event.getMillis());
      String timeString = dtf.format(date);

	    StringBuilder s = new StringBuilder();
	    s.append("SaxonCE.");
	    s.append(event.getLoggerName());
	    s.append(" ");
	    s.append(timeString);
	    s.append("\n");
	    s.append(event.getLevel().getName());
	    s.append(": ");
	    return s.toString();
  }
}
