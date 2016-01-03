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
public class ReplyResponse extends XMLDocument {
    private final Response response;
    
    public ReplyResponse(Response res)
        throws ParserConfigurationException,
               DatatypeConfigurationException,
               TransformerException {
        super("reply-response");
        
        this.response = res;
        
        super.add2Body("reply-response", res.toXMLString());
    }
    
    /**
     * Construct the reply-response by the XML string.
     * @param xmlStr the XML string to be parsed.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public ReplyResponse(String xmlStr)
        throws ParserConfigurationException,
               SAXException,
               IOException {
        super(XMLDocument.parse(xmlStr));
        
        NodeList body = super.rootNode.getChildNodes();
        String rrStr = body.item(0).getTextContent();
        
        this.response = new Response(rrStr);
    }
    
    public Response getResponse() {
        return response;
    }
}
