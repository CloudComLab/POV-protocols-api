package org.cclab.message.twosteps.chainhash;

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
    
    public Request(Operation op)
        throws ParserConfigurationException,
               DatatypeConfigurationException,
               TransformerException {
        super("request");
        
        this.operation = op;
        
        super.add2Body(operation.toMap());
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
        
        this.operation = new Operation(operationNode);
    }
    
    public Operation getOperation() {
        return operation;
    }
}
