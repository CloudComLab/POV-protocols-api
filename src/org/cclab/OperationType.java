package org.cclab;

/**
 * User can add custom operation type to tell service provider what they
 * want to do. In general, this class will be a member of Operation.
 * 
 * @author scottie
 * @see Operation
 */
public enum OperationType {
    DOWNLOAD, UPLOAD, AUDIT
}
