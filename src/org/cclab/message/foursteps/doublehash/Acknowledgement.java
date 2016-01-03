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
public class Acknowledgement extends XMLDocument {
    private final String result;
    private final ReplyResponse replyResponse;
    
    public Acknowledgement(String result, ReplyResponse rr)
        throws ParserConfigurationException,
               DatatypeConfigurationException,
               TransformerException {
        super("acknowledgement");
        
        this.result = result;
        this.replyResponse = rr;
        
        super.add2Body("result", result);
        super.add2Body("reply-response", replyResponse.toXMLString());
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
        String rrStr = body.item(1).getTextContent();
        
        this.result = result;
        this.replyResponse = new ReplyResponse(rrStr);
    }
    
    public String getResult() {
        return result;
    }
    
    public ReplyResponse getReplyResponse() {
        return replyResponse;
    }
}
