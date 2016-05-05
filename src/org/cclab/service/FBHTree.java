package org.cclab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import org.cclab.utility.HashUtils;

/**
 * The full binary hash tree proposed by Hong-Fu Chen in 2015.
 *  
 * @author scott
 */
public class FBHTree {
    private static final int DEFAULT_TREE_HEIGHT = 17;
    
    private final int height;
    private final Node[] nodes;
    
    /**
     * Construct a FBHTree with initial tree height.
     * @param treeHeight the initial tree height
     * @throws IllegalArgumentException if the specified initial tree height is
     *         smaller than 2
     */
    public FBHTree(int treeHeight) {
        if (treeHeight < 2) {
            throw new IllegalArgumentException("The minimum value for tree height is 2.");
        }
        
        this.height = treeHeight;
        this.nodes = new Node[1 << height];
        
        for (int i = nodes.length - 1; i > 0; i--) {
            if (i >= (1 << (height - 1))) { // leaf node
                nodes[i] = new Node(i, null, null);
            } else { // internal node
                nodes[i] = new Node(i, nodes[i * 2], nodes[(i * 2) + 1]);
            }
        }
    }
    
    /**
     * Construct a FBHTree with default tree height.
     */
    public FBHTree() {
        this(DEFAULT_TREE_HEIGHT);
    }
    
    /**
     * Calculate the slot index which key should be in.
     * @return slot index
     */
    private int calcLeafIndex(String key) {
        byte[] digest = HashUtils.sha256(key.getBytes());
        int index = 0;
        
        if (digest.length >= 4) {
            for (int i = 0; i < 4; i++) {
                index += digest[i] << (i * 8);
            }
        }
        
        return (1 << (height - 1)) + Math.abs(index) % (1 << (height - 1));
    }
    
    /**
     * Associates the specified value with the specified key in this FBHTree.
     * If the FBHTree previously contained a mapping for the key, the old
     * value is replaced.
     */
    public void put(String key, byte[] digestValue) {
        int index = calcLeafIndex(key);
        
        nodes[index].put(key, digestValue);
        
        for (int i = index; i > 0; i >>= 1) {
            nodes[i].setDirty(true);
        }
    }
    
    /**
     * Returns <tt>true</tt> if this FBHTree contains a mapping for the
     * specified key.
     */
    public boolean contains(String key) {
        return nodes[calcLeafIndex(key)].contains(key);
    }
    
    /**
     * Removes the mapping for the specified key from this FBHTree if present.
     * 
     * @return <tt>true</tt> if the specified key was in the FBHTree.
     */
    public boolean remove(String key) {
        int index = calcLeafIndex(key);
        
        for (int i = index; i > 0; i /= 2) {
            nodes[i].setDirty(true);
        }
        
        return nodes[index].remove(key);
    }
    
    /**
     * Returns the root hash of this FBHTree.
     */
    public byte[] getRootHash() {
        return nodes[1].getContentDigest();
    }
    
    /**
     * Extract a slice from this FBHTree by specified key.
     * @return a formatted slice string
     */
    public String extractSlice(String key) {
        if (!contains(key)) {
            return "";
        }
        
        String slice = "";
        
        int index = calcLeafIndex(key);
        String delim = "";
        
        // leaf node => (digest1,digest2,digest3,...,digestN)
        slice += "(";
        for (byte[] bytes: nodes[index].getContents()) {
            slice += delim + HashUtils.byte2hex(bytes);
            delim = ",";
        }
        slice += ")";
        
        // internal nodes
        for (; index > 1; index /= 2) {
            byte[] parentDigest = nodes[index / 2].getContentDigest();
            String parentHexStr = HashUtils.byte2hex(parentDigest);
            
            if (index % 2 == 0) { // left => [currentSlice,parentDigest,rightDigest]
                byte[] rightDigest = nodes[index + 1].getContentDigest();
                String rightHexStr = HashUtils.byte2hex(rightDigest);
                
                slice = "[" + slice + "," + parentHexStr + "," + rightHexStr + "]";
            } else { // right => [leftDigest,parentDigest,currentSlice]
                byte[] leftDigest = nodes[index - 1].getContentDigest();
                String leftHexStr = HashUtils.byte2hex(leftDigest);
                
                slice = "[" + leftHexStr + "," + parentHexStr + "," + slice + "]";
            }
        }
        
        return slice;
    }
    
    /**
     * Parse and evaluate the root hash of the given slice recursively.
     * 
     * @return byte array of the root hash of the given slice
     * @throws IllegalArgumentException if the slice has wrong format
     * @throws VerifyError if any parent digest does not match to the digest of
     *         the left child and the right child.
     */
    public static byte[] evalRootHashFromSlice(String slice) {
        if (slice.startsWith("[")) { // internal node
            slice = slice.substring(1, slice.length() - 1);
            
            String left, parent, right;
            
            // [left],parent,right or (left),parent,right
            if (slice.startsWith("[") || slice.startsWith("(")) {
                int comma = slice.lastIndexOf(',');
                left = slice.substring(0, comma);
                right = slice.substring(comma + 1);
                
                comma = left.lastIndexOf(',');
                parent = left.substring(comma + 1);
                left = left.substring(0, comma);
            // left,parent,[right] or // left,parent,(right)
            } else if (slice.endsWith("]") || slice.endsWith(")")) {
                int comma = slice.indexOf(',');
                left = slice.substring(0, comma);
                right = slice.substring(comma + 1);
                
                comma = right.indexOf(',');
                parent = right.substring(0, comma);
                right = right.substring(comma + 1);
            } else {
                throw new IllegalArgumentException("Cannot parse slice: " + slice);
            }
            
            byte[] evalRootHash = HashUtils.sha256(
                    evalRootHashFromSlice(left),
                    evalRootHashFromSlice(right));
            
            if (!Arrays.equals(evalRootHash, HashUtils.hex2byte(parent))) {
                throw new VerifyError("Hashes of slice do not match.");
            }
            
            return evalRootHash;
        } else if (slice.startsWith("(")) { // leaf node
            slice = slice.substring(1, slice.length() - 1);
            
            List<byte[]> digests = new ArrayList<>();
            
            for (String s: slice.split(",")) {
                digests.add(HashUtils.hex2byte(s));
            }
            
            return HashUtils.sha256(digests);
        } else { // hex string, convert to byte[]
            return HashUtils.hex2byte(slice);
        }
    }
    
    /**
     * Basic node for FBHTree.
     */
    private static class Node {
        private int id;
        private boolean isLeaf;
        private boolean dirty;
        private byte[] contentDigest;
        
        private Node leftChild;
        private Node rightChild;
        private LinkedHashMap<String, byte[]> contents;
        
        public Node(int id, Node leftChild, Node rightChild) {
            this.id = id;
            this.dirty = false;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            
            if (leftChild == null || rightChild == null) { // leaf node
                this.isLeaf = true;
                this.contentDigest = new byte[32];
                
                new Random().nextBytes(this.contentDigest);
            } else { // internal node
                this.isLeaf = false;
                this.contentDigest = HashUtils.sha256(
                        leftChild.getContentDigest(),
                        rightChild.getContentDigest());
            }
            
            this.contents = null;
        }
        
        public void put(String key, byte[] bytes) {
            if (contents == null) {
                contents = new LinkedHashMap<>();
            }
            
            contents.put(key, bytes);
            dirty = true;
        }
        
        public boolean contains(String key) {
            if (contents != null) {
                return contents.containsKey(key);
            } else {
                return false;
            }
        }
        
        public boolean remove(String key) {
            if (contains(key)) {
                contents.remove(key);
                dirty = true;
                
                return true;
            } else {
                return false;
            }
        }
        
        public byte[] getContentDigest() {
            if (isDirty()) {
                if (isLeaf) {
                    contentDigest = HashUtils.sha256(contents.values());
                } else {
                    contentDigest = HashUtils.sha256(
                            leftChild.getContentDigest(),
                            rightChild.getContentDigest());
                }
                
                setDirty(false);
            }
            
            return contentDigest;
        }
        
        public Collection<byte[]> getContents() {
            if (isLeaf) {
                if (contents == null) {
                    contents = new LinkedHashMap<>();
                }
                
                return contents.values();
            } else {
                return null;
            }
        }
        
        public void setDirty(boolean dirty) {
            this.dirty = dirty;
        }
        
        public boolean isDirty() {
            return dirty;
        }
    }
}
