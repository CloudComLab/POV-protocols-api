package org.cclab;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.w3c.dom.NodeList;

/**
 * Operation is the message to let service provider know what to do in the
 * transaction.
 * 
 * @author scottie
 * @see OperationType
 */
public class Operation implements Serializable {
    /**
     * Type of this operation.
     */
    public final OperationType type;
    
    /**
     * The parameters of this operation. It can be null.
     */
    public final Map<String, String> args;
    
    public Operation(OperationType type, Map<String, String> args) {
        this.type = type;
        this.args = args;
    }
    
    /**
     * Construct a operation with NodeList in XML.
     * @param list the NodeList in XML.
     */
    public Operation(NodeList list) {
        this.type = OperationType.valueOf(list.item(0).getTextContent());
        
        this.args = new LinkedHashMap<>();
        for (int i = 1; i < list.getLength(); i++) {
            args.put(list.item(i).getNodeName(), list.item(i).getTextContent());
        }
    }

    public LinkedHashMap<String, String> toMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        
        map.put("type", type.name());
        
        if (args != null) {
            map.putAll(args);
        }
        
        return map;
    }
}
