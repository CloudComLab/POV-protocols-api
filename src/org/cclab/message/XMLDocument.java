package org.cclab.message;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The base class of all messages.
 * @author Scott
 */
public abstract class XMLDocument implements XMLable, Serializable {
    private static final Logger LOGGER;
    private static final DocumentBuilderFactory DocumentFactory;
    
    protected final Document document;
    protected final Element rootNode;
    
    static {
        LOGGER = Logger.getLogger(XMLDocument.class.getName());
        DocumentFactory = DocumentBuilderFactory.newInstance();
    }
    
    /**
     * Construct a XML document with specific root name.
     * @param name Name of root node in the document.
     * @throws javax.xml.parsers.ParserConfigurationException
     */
    public XMLDocument(String name) throws ParserConfigurationException {
        document = DocumentFactory.newDocumentBuilder().newDocument();

        rootNode = document.createElement(name);
        document.appendChild(rootNode);
    }
    
    /**
     * Construct a XML document with existed document.
     * @param doc the existed document.
     */
    public XMLDocument(Document doc) {
        document = doc;
        rootNode = doc.getDocumentElement();
    }
    
    /**
     * Construct a new child node to mountNode.
     * @param mountNode the parent node of new constructed node.
     * @param key the name of new node.
     * @param value the value of new node.
     */
    private void add(Element mountNode, String key, String value) {
        Element node = document.createElement(key);
        node.setTextContent(value);
        
        mountNode.appendChild(node);   
    }
    
    /**
     * Insert child nodes to specific node recursively.
     * @param mountNode the parent node of child nodes.
     * @param args informations of new constructed child nodes.
     */
    private void add(Element mountNode, LinkedHashMap<String, ?> args)
        throws DatatypeConfigurationException, TransformerException {
        for (String key : args.keySet()) {
            Object value = args.get(key);
            Element node = document.createElement(key);
            
            if (value instanceof LinkedHashMap) {
                mountNode.appendChild(node);
                
                add(node, (LinkedHashMap) value);
            } else if (value instanceof XMLable) {
                add(mountNode, key, ((XMLable) value).toXMLString());
            } else if (value instanceof String) {
                add(mountNode, key, String.valueOf(value));
            } else {
                throw new DatatypeConfigurationException(
                    "Unknown node value type: " + value.getClass().getName());
            }
        }
    }

    /**
     * Insert nodes into document body recursively.
     * @param args informations of new constructed child nodes. The value of
     * Map can be another Map.
     * @throws javax.xml.datatype.DatatypeConfigurationException raised if type
     * of value for Map is not String or Map.
     * @throws javax.xml.transform.TransformerException raised if XML transfer
     * failed.
     */
    protected void add2Body(LinkedHashMap<String, ?> args)
        throws DatatypeConfigurationException, TransformerException {
        add(rootNode, args);
    }
    
    /**
     * Insert one XMLable node into document body.
     * @param key node name.
     * @param xmlable node value.
     * @throws TransformerException raised if XML transfer failed.
     */
    protected void add2Body(String key, XMLable xmlable)
        throws TransformerException {
        add(rootNode, key, xmlable.toXMLString());
    }
    
    /**
     * Insert one node into document body.
     * @param key node name.
     * @param value node value.
     */
    protected void add2Body(String key, String value) {
        add(rootNode, key, value);
    }
    
    /**
     * Embed a digital signature into the document to demonstrate its authenticity.
     * @param keyPair the key pair used to sign the document.
     * @return true if sign successfully.
     */
    public boolean sign(KeyPair keyPair) {
        try {
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
            
            CanonicalizationMethod cMethod = fac.newCanonicalizationMethod(
                    CanonicalizationMethod.INCLUSIVE,
                    (C14NMethodParameterSpec) null);
            SignatureMethod signMethod = fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
            DigestMethod digestMethod = fac.newDigestMethod(DigestMethod.SHA1, null);
            Reference ref = fac.newReference("", digestMethod);
            
            SignedInfo signedInfo = fac.newSignedInfo(cMethod, signMethod, Collections.singletonList(ref));
            
            KeyInfoFactory kif = fac.getKeyInfoFactory();
            KeyValue kv = kif.newKeyValue(keyPair.getPublic());
            KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(kv));
            
            DOMSignContext dsc = new DOMSignContext(keyPair.getPrivate(),
                                                    document.getDocumentElement());
            
            XMLSignature sig = fac.newXMLSignature(signedInfo, keyInfo);
            
            sig.sign(dsc);
            
            return true;
        } catch (NoSuchAlgorithmException |
                 InvalidAlgorithmParameterException |
                 XMLSignatureException |
                 MarshalException |
                 KeyException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            
            return false;
        }
    }
    
    /**
     * Verify the digital signature in the document.
     * @param publicKey the public key used to verify the digital signature.
     * @return true if the digital signature is valid.
     */
    public boolean verifyDigitalSignature(PublicKey publicKey) {
        try {
            NodeList nl = document.getElementsByTagName("Signature");
            
            // Create a DOM XMLSignatureFactory that will be used to unmarshal
            // the document containing the XMLSignature
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
            
            // Create a DOMValidateContext and specify a KeyValue KeySelector
            // and document context
            DOMValidateContext valContext = new DOMValidateContext(publicKey, nl.item(0));
            
            // unmarshal the XMLSignature
            XMLSignature signature = fac.unmarshalXMLSignature(valContext);
            
            // Validate the XMLSignature (generated above)
            return signature.validate(valContext);
        } catch (MarshalException | XMLSignatureException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    /**
     * Convert document into XML format string.
     * @return XML format string
     * @throws TransformerException 
     */
    @Override
    public String toXMLString() throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(rootNode);
        transformer.transform(source, result);

        return result.getWriter().toString().replaceAll("[\n\r]", "");
    }
    
    /**
     * Parse a string to XMLDocument.
     * @param str the string to be parsed.
     * @return the parsed document.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public static Document parse(String str)
        throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);

        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();

        InputSource inputSource = new InputSource(new StringReader(str));

        return documentBuilder.parse(inputSource);
    }
}
