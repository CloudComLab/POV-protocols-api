package org.cclab;

/**
 *
 * @author scottie
 */
public class Operation {
    public final OperationType type;
    public final Object obj;
    
    public Operation(OperationType type, Object object) {
        this.type = type;
        this.obj = object;
    }
}
