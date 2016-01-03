package org.cclab.message;

import javax.xml.transform.TransformerException;

/**
 *
 * @author Scott
 */
public interface XMLable {
    public String toXMLString() throws TransformerException;
}
