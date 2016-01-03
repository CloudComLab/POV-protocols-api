package org.cclab.message.foursteps.chainhash_lsn;

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
    private final String result;
    private final String lastChainHash;
    private final Request request;
    
    public Response(String result, String lastCH, Request req)
        throws ParserConfigurationException,
               DatatypeConfigurationException,
               TransformerException {
        super("response");
        
        this.result = result;
        this.lastChainHash = lastCH;
        this.request = req;
        
        super.add2Body("result", result);
        super.add2Body("chainhash", lastChainHash);
        super.add2Body("request", req.toXMLString());
    }
    
    /**
     * Construct the response by the XML string.
     * @param xmlStr the XML string to be parsed.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public Response(String xmlStr)
        throws ParserConfigurationException,
               SAXException,
               IOException {
        super(XMLDocument.parse(xmlStr));
        
        NodeList body = super.rootNode.getChildNodes();
        String result = body.item(0).getTextContent();
        String lastCH = body.item(1).getTextContent();
        String reqStr = body.item(2).getTextContent();
        
        this.result = result;
        this.lastChainHash = lastCH;
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
