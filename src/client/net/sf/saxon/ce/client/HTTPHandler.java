package client.net.sf.saxon.ce.client;

import com.google.gwt.dom.client.Node;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;

public class HTTPHandler {
      int waitCount = 1;
	  private State responseState;
	  private String errorMessage = "";
	  private Node resultNode = null;
	  
	  public HTTPHandler() {
		  responseState = State.NONE;
	  }
	  
	  public enum State {
		  NONE, COMPLETED, ERROR
	  }
	  
	  public State getResponseState() {
		  return responseState;
	  }
	  
	  public String getErrorMessage() {
		  return errorMessage;
	  }
	  
	  public void setErrorMessage(String value) {
		  responseState = State.ERROR;
		  errorMessage = value;
	  }
	  
	  public void setResultNode(Node node) {
		  resultNode = node;
	  }
	  
	  public Node getResultNode() {
		  return resultNode;
	  }
	  
	  public void doGet(String url, RequestCallback callback) {
	    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
	    responseState = State.COMPLETED;

	    try {
	       Request response = builder.sendRequest(null, callback);
	    } catch (RequestException e) {
	       responseState = State.ERROR;
	       errorMessage = e.getMessage();
	    }
	  }

	}
