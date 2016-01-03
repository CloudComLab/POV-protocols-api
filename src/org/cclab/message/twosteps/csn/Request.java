package org.cclab.message.twosteps.csn;

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
    private final Integer consecutiveSequenceNumber;
    
    public Request(Operation op, Integer csn)
        throws ParserConfigurationException,
               DatatypeConfigurationException,
               TransformerException {
        super("request");
        
        this.operation = op;
        this.consecutiveSequenceNumber = csn;
        
        super.add2Body(operation.toMap());
        super.add2Body("CSN", consecutiveSequenceNumber.toString());
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
        String csn = body.item(1).getTextContent();
        
        this.operation = new Operation(operationNode);
        this.consecutiveSequenceNumber = Integer.parseInt(csn);
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public Integer getConsecutiveSequenceNumber() {
        return consecutiveSequenceNumber;
    }
}
