/**
 * XML Parsing library for the key-value store
 * 
 * @author Mosharaf Chowdhury (http://www.mosharaf.com)
 * @author Prashanth Mohan (http://www.cs.berkeley.edu/~prmohan)
 * 
 * Copyright (c) 2012, University of California at Berkeley
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of University of California, Berkeley nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *    
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


/**
 * This is the object that is used to generate messages the XML based messages 
 * for communication between clients and servers. 
 */
public class KVMessage {
	public static final String TYPE_RESP = "resp";
	public static final String REQ_PUT = "putreq";
	public static final String REQ_GET = "getreq";
	public static final String REQ_DEL = "delreq";
	
	public static final String UNKNOWN_ERROR = "Unknown Error: ";
	public static final String NETWORK_SEND_ERROR = "Network Error: Could not send data";
	public static final String NETWORK_CONNECT_ERROR = "Network Error: Could not connect";
	public static final String NETWORK_SOCKET_ERROR = "Network Error: Could not create socket";
	public static final String NETWORK_RECIEVE_ERROR = "Network Error: Could not receive data";
	
	public static final String OVERSIZED_KEY = "Oversized key";
    public static final String OVERSIZED_VALUE = "Oversized value";
    
	private String msgType = null;
	private String key = null;
	private String value = null;
	private String status = null;
	private String message = null;
	
	public final String getKey() {
		return key;
	}

	public final void setKey(String key) {
		this.key = key;
	}

	public final String getValue() {
		return value;
	}

	public final void setValue(String value) {
		this.value = value;
	}

	public final String getStatus() {
		return status;
	}

	public final void setStatus(String status) {
		this.status = status;
	}

	public final String getMessage() {
		return message;
	}

	public final void setMessage(String message) {
		this.message = message;
	}

	public String getMsgType() {
		return msgType;
	}

	/* Solution from http://weblogs.java.net/blog/kohsuke/archive/2005/07/socket_xml_pitf.html */
	private class NoCloseInputStream extends FilterInputStream {
	    public NoCloseInputStream(InputStream in) {
	        super(in);
	    }
	    
	    public void close() {} // ignore close
	}
	
	/***
	 * 
	 * @param msgType
	 * @throws KVException of type "resp" with message "Message format incorrect" if msgType is unknown
	 */
	public KVMessage(String msgType) throws KVException {
	    // TODO: implement me
		if (msgType.equals("getreq") || msgType.equals("putreq") || msgType.equals("delreq") || msgType.equals("resp")) {
            this.msgType = msgType;
        } else {
            throw new KVException(new KVMessage("resp", "Message format incorrect"));
        }
	}
	
	public KVMessage(String msgType, String message) throws KVException {
        // TODO: implement me
		this(msgType);
		this.message = message;
	}
	
	 /***
     * Parse KVMessage from incoming network connection
     * @param sock
     * @throws KVException if there is an error in parsing the message. The exception should be of type "resp and message should be :
     * a. "XML Error: Received unparseable message" - if the received message is not valid XML.
     * b. "Network Error: Could not receive data" - if there is a network error causing an incomplete parsing of the message.
     * c. "Message format incorrect" - if there message does not conform to the required specifications. Examples include incorrect message type. 
     */
	public KVMessage(InputStream input) throws KVException {
	     // TODO: implement me
		DocumentBuilder xmlBuilder;
        Document xmlDoc;
        try {
        	xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new KVException (new KVMessage("resp", "Unknown Error: DocumentBuilder error"));
        }
        try {
            xmlDoc = xmlBuilder.parse(new NoCloseInputStream(input));
            xmlDoc.setXmlStandalone(true);
         } catch (IOException e) {
            throw new KVException(new KVMessage("resp", "Could not receive data"));
         } catch (SAXException e) {
            throw new KVException(new KVMessage("resp", "Received unparseable message"));
         } try {
             Element rootElement = xmlDoc.getDocumentElement();
             String msgType = rootElement.getAttribute("type");
             if (msgType.equals("putreq")) {
                 Node putkey = rootElement.getFirstChild(); 
                 Node putval = rootElement.getLastChild();
                 if(putkey == null || putkey.getTextContent()==null){
                 	throw new KVException (new KVMessage("resp", "Message format incorrect"));
                 }
                 if(putval == null || putval.getTextContent()==null){
                 	throw new KVException (new KVMessage("resp", "Message format incorrect"));
                 }
                 this.msgType = "putreq";
                 this.key = putkey.getTextContent();
                 this.value = putval.getTextContent();
             } else if (msgType.equals("getreq")) {
                 Node getkey = rootElement.getFirstChild();
                 
                 if(getkey == null || getkey.getTextContent()==null){
                 	throw new KVException (new KVMessage("resp", "Message format incorrect"));
                 }
                 this.msgType = "getreq";
                 this.key = getkey.getTextContent();
             } else if (msgType.equals("delreq")) {
                 Node delkey = rootElement.getFirstChild();
                 if(delkey == null || delkey.getTextContent()==null){
                 	throw new KVException (new KVMessage("resp", "Message format incorrect"));
                 }
                 this.msgType = "delreq";
                 this.key = delkey.getTextContent();
             } else if (msgType.equals("resp")) {
                 if (((Element)rootElement.getFirstChild()).getTagName() == "Key") {
                     Node getkey = rootElement.getFirstChild();
                     Node getval = rootElement.getLastChild();
                     if(getkey.getTextContent()==null){
                     	throw new KVException (new KVMessage("resp", "Message format incorrect"));
                     }
                     if(getval.getTextContent()==null){
                     	throw new KVException (new KVMessage("resp", "Message format incorrect"));
                     }
                     this.msgType = "resp";
                     this.key = getkey.getTextContent();
                     this.value = getval.getTextContent();
                 } else {
                     Node respmsg = rootElement.getFirstChild();
                     this.msgType = "resp";
                     this.message = respmsg.getTextContent();
                 }
             } else {
                 throw new KVException (new KVMessage("resp", "Message format incorrect"));
             }
         } catch (DOMException e) {
        	 throw new KVException (new KVMessage("resp", "Message format incorrect"));
         }
	}
	
	/**
	 * Generate the XML representation for this message.
	 * @return the XML String
	 * @throws KVException if not enough data is available to generate a valid KV XML message
	 */
	public String toXML() throws KVException {
		DocumentBuilder xmlBuilder;
        try {
        xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new KVException (new KVMessage("Unknown Error: DocumentBuilder error"));
        }
        Document xmlDoc = xmlBuilder.newDocument();
        xmlDoc.setXmlStandalone(true);
        Element root = xmlDoc.createElement("KVMessage");
        root.setAttribute("type", this.msgType);
        xmlDoc.appendChild(root);
            if (this.msgType == "putreq") {
            	if(this.key==null||this.value==null){
            		 throw new KVException (new KVMessage("resp", "Message format incorrect"));
            	}
                Element childkey = xmlDoc.createElement("Key");
                Element childvalue = xmlDoc.createElement("Value");
                childkey.setTextContent(this.key);
                childvalue.setTextContent(this.value);
                root.appendChild(childkey);
                root.appendChild(childvalue);
            } else if (this.msgType.equals("getreq") || this.msgType.equals("delreq")) {
            	if(this.key==null){
            		throw new KVException (new KVMessage("resp", "Message format incorrect"));
            	}
                Element childkey = xmlDoc.createElement("Key");
                childkey.setTextContent(this.key);
                root.appendChild(childkey);
            } else if (this.msgType.equals("resp")) {
                if(this.key == null && this.value == null) {
                    Element childmsg = xmlDoc.createElement("Message");
                    childmsg.setTextContent(this.message);
                    root.appendChild(childmsg); 
                } else if (this.key != null && this.value != null) {
                    Element childkey = xmlDoc.createElement("Key");
                    Element childvalue = xmlDoc.createElement("Value");
                    childkey.setTextContent(this.key);
                    childvalue.setTextContent(this.value);
                    root.appendChild(childkey);
                    root.appendChild(childvalue);
                } else {
                    throw new KVException (new KVMessage("resp", "Message format incorrect"));
                }
            } else {
                throw new KVException (new KVMessage("resp", "Message format incorrect"));
            } try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                StreamResult result = new StreamResult(new StringWriter());
                DOMSource source = new DOMSource(xmlDoc);
                transformer.transform(source, result);
                return result.getWriter().toString();
            } catch (TransformerException e) {
                throw new KVException (new KVMessage("resp", "Unknown Error: transforming error"));
            }
	}
	
	public void sendMessage(Socket sock) throws KVException {
		String xml = this.toXML();
		Writer out = null;
		try{
			out = new OutputStreamWriter(sock.getOutputStream());
	        out.write(xml);
	        out.flush();
		} catch (IOException e) {
            throw new KVException(new KVMessage("resp", NETWORK_SEND_ERROR));
        } finally {
            try { 
                sock.shutdownOutput();
            } catch (IOException e) {
                throw new KVException(new KVMessage("resp", UNKNOWN_ERROR + "Could Not Close socket Output"));   //TODO: needs implementation
            }
        }
	}
}
