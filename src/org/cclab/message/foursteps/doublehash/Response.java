package org.cclab.message.foursteps.doublehash;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.cclab.message.XMLDocument;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott
 */
public class Response extends XMLDocument {
    private final String clientHash;
    private final String mainHash;
    private final Request request;
    
    public Response(String clientHash, String mainHash, Request req)
        throws ParserConfigurationException,
               DatatypeConfigurationException,
               TransformerException {
        super("response");
        
        this.clientHash = clientHash;
        this.mainHash = mainHash;
        this.request = req;
        
        super.add2Body("clienthash", clientHash);
        super.add2Body("mainhash", mainHash);
        super.add2Body("request", req.toXMLString());
    }
    
    /**
     * Construct the response by the XML string.
     * @param str the XML string to be parsed.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public Response(String str)
        throws ParserConfigurationException,
               SAXException,
               IOException {
        super(XMLDocument.parse(str));
        
        NodeList body = super.rootNode.getChildNodes();
        String clientHash = body.item(0).getTextContent();
        String mainHash = body.item(1).getTextContent();
        String reqStr = body.item(2).getTextContent();
        
        this.clientHash = clientHash;
        this.mainHash = mainHash;
        this.request = new Request(reqStr);
    }
    
    public String getClientHash() {
        return clientHash;
    }
    
    public String getMainHash() {
        return mainHash;
    }
    
    public Request getRequest() {
        return request;
    }
}
