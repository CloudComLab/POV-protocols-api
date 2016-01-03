package org.cclab.message.twosteps.chainhash;

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
public class Acknowledgement extends XMLDocument {
    private final String result;
    private final String lastChainHash;
    private final Request request;
    
    public Acknowledgement(String result, String hash, Request req)
        throws ParserConfigurationException,
               DatatypeConfigurationException,
               TransformerException {
        super("request");
        
        this.result = result;
        this.lastChainHash = hash;
        this.request = req;
        
        super.add2Body("result", result);
        super.add2Body("chainhash", lastChainHash);
        super.add2Body("request", req.toXMLString());
    }
    
    /**
     * Construct the acknowledgement by the XML string.
     * @param str the XML string to be parsed.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public Acknowledgement(String str)
        throws ParserConfigurationException,
               SAXException,
               IOException {
        super(XMLDocument.parse(str));
        
        NodeList body = super.rootNode.getChildNodes();
        String result = body.item(0).getTextContent();
        String chainHash = body.item(1).getTextContent();
        String reqStr = body.item(2).getTextContent();
        
        this.result = result;
        this.lastChainHash = chainHash;
        this.request = new Request(reqStr);
    }
    
    public String getResult() {
        return result;
    }
    
    public String getLastChainHash() {
        return lastChainHash;
    }
    
    public Request getRequest() {
        return request;
    }
}
