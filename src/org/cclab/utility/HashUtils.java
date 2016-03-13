package org.cclab.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Scott
 */
public class HashUtils {
    public static final Logger LOG;
    public static final char[] HEX_CHARS;
    
    static {
        LOG = Logger.getLogger(HashUtils.class.getName());
        HEX_CHARS = "0123456789abcdef".toCharArray();
    }
    
    public static String byte2hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        
        for (byte b: bytes) {
            int v = b & 0xff;
            
            sb.append(HEX_CHARS[v >> 2]).append(HEX_CHARS[v & 0xf]);
        }
        
        return sb.toString();
    }
    
    public static String byte2HEX(byte[] bytes) {
        return byte2hex(bytes).toUpperCase();
    }
    
    public static byte[] sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            md.update(bytes);
            
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            LOG.log(Level.SEVERE, null, ex);
            
            return null;
        }
    }
    
    public static String sha256(String data) {
        return byte2hex(data.getBytes());
    }
    
    public static String sha256(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            InputStream is = new FileInputStream(file);
            
            try (DigestInputStream dis = new DigestInputStream(is, md)) {
                while (dis.read() != -1);
            }

            return byte2hex(md.digest());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            
            return null;
        }
    }
}
