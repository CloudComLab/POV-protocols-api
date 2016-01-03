package org.cclab.message.foursteps.chainhash_lsn;

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
    private final Integer localSequenceNumber;
    
    public Request(Operation op, String clientID, Integer lsn)
        throws ParserConfigurationException,
               DatatypeConfigurationException,
               TransformerException {
        super("request");
        
        this.operation = op;
        this.clientID = clientID;
        this.localSequenceNumber = lsn;
        
        super.add2Body(operation.toMap());
        super.add2Body("clientID", clientID);
        super.add2Body("lsn", localSequenceNumber.toString());
    }
    
    /**
     * Construct the request by the XML string.
     * @param xmlStr the XML string to be parsed.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public Request(String xmlStr)
        throws ParserConfigurationException,
               SAXException,
               IOException {
        super(XMLDocument.parse(xmlStr));
        
        NodeList body = super.rootNode.getChildNodes();
        NodeList operationNode = body.item(0).getChildNodes();
        String id = body.item(1).getTextContent();
        Integer lsn = Integer.decode(body.item(2).getTextContent());
        
        this.operation = new Operation(operationNode);
        this.clientID = id;
        this.localSequenceNumber = lsn;
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public String getClientID() {
        return clientID;
    }
    
    public Integer getLocalSequenceNumber() {
        return localSequenceNumber;
    }
}
