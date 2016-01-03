package org.cclab.message.foursteps.doublehash;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.cclab.Operation;
import org.cclab.message.XMLDocument;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott
 */
public class Request extends XMLDocument {
    private final Operation operation;
    private final String clientID;
    
    public Request(Operation op, String clientID)
        throws ParserConfigurationException,
               DatatypeConfigurationException,
               TransformerException {
        super("request");
        
        this.operation = op;
        this.clientID = clientID;
        
        super.add2Body(operation.toMap());
        super.add2Body("clientID", clientID);
    }
    
    /**
     * Construct the request by the XML string.
     * @param str the XML string to be parsed.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public Request(String str)
        throws ParserConfigurationException,
               SAXException,
               IOException {
        super(XMLDocument.parse(str));
        
        NodeList body = super.rootNode.getChildNodes();
        NodeList operationNode = body.item(0).getChildNodes();
        String id = body.item(1).getTextContent();
        
        this.operation = new Operation(operationNode);
        this.clientID = id;
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public String getClientID() {
        return clientID;
    }
}
