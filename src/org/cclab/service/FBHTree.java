package org.cclab.service;

import java.io.Serializable;
import java.util.ArrayList;
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
public class FBHTree implements Serializable {
    private static final int DEFAULT_TREE_HEIGHT = 17;
    
    private static final char SLICE_DELIMITER = '.';
    private static final int ESTIMATED_SLICE_LENGTH = 4000;
    
    private final int height;
    private final Node[] nodes;
    
    /**
     * Construct a FBHTree with initial tree height.
     * @param treeHeight the initial tree height
     * @throws IllegalArgumentException if the specified initial tree height is
     *         smaller than 1
     */
    public FBHTree(int treeHeight) {
        if (treeHeight <= 0) {
            throw new IllegalArgumentException("The minimum value for tree height is 1.");
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
     * 
     * @return a formatted slice string
     * @throws NoSuchElementException if the specified key does not exist in this
     *         FBHTree.
     */
    public String extractSlice(String key) throws NoSuchElementException {
        if (!contains(key)) {
            throw new NoSuchElementException("The specified key does not exist in this FBHTree");
        }
        
        int index = calcLeafIndex(key);
        String leftHexStr, rightHexStr;
        StringBuilder sliceBuilder = new StringBuilder(ESTIMATED_SLICE_LENGTH);
        
        sliceBuilder.append(index).append(SLICE_DELIMITER);
        
        // leaf node => (digest1,digest2,digest3,...,digestN)
        StringBuilder leaf = new StringBuilder("(");
        String delim = "";
        for (byte[] bytes: nodes[index].getContents()) {
            leaf.append(delim).append(HashUtils.byte2hex(bytes));
            delim = ",";
        }
        leftHexStr = rightHexStr = leaf.append(')').toString();
        
        // internal nodes
        for (; index > 1; index /= 2) {
            if (index % 2 == 0) {
                rightHexStr = nodes[index + 1].getContentDigestHexString();
            } else {
                leftHexStr = nodes[index - 1].getContentDigestHexString();
            }
            
            sliceBuilder
                    .append(leftHexStr)
                    .append(SLICE_DELIMITER)
                    .append(rightHexStr)
                    .append(SLICE_DELIMITER);
            
            leftHexStr = rightHexStr = nodes[index / 2].getContentDigestHexString();
        }
        
        // root hash must be in the right (2/2=1, 3/2=1, 1%2=1)
        sliceBuilder.append(rightHexStr);
        
        return sliceBuilder.toString();
    }
    
    /**
     * Parse and evalute the digest value of formatted leaf node in the
     * slice.
     * 
     * @param leaf is formatted in "(digest1,digest2,digest3,...,digestN)".
     * @return byte array of the digest of the formatted leaf node.
     */
    private static byte[] evalLeafOfSlice(String leaf) {
        List<byte[]> digests = new ArrayList<>();
        String hashes = leaf.substring(1, leaf.length() - 1);

        for (String s: hashes.split(",")) {
            digests.add(HashUtils.hex2byte(s));
        }

        return HashUtils.sha256(digests);
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
        String[] tokens = slice.split(String.valueOf(SLICE_DELIMITER));
        int index = Integer.parseInt(tokens[0]);
        
        if (tokens[1].startsWith("(")) {
            tokens[1] = HashUtils.byte2hex(evalLeafOfSlice(tokens[1]));
        } else {
            tokens[2] = HashUtils.byte2hex(evalLeafOfSlice(tokens[2]));
        }
        
        int parentIndex;
        byte[] parentDigest = null;
        
        for (int i = 1; index > 1; i += 2, index /= 2) {
            parentIndex = i + 2 + (index / 2) % 2;
            parentDigest = HashUtils.sha256(
                    HashUtils.hex2byte(tokens[i]),
                    HashUtils.hex2byte(tokens[i + 1]));
            
            if (!HashUtils.byte2hex(parentDigest).equals(tokens[parentIndex])) {
                throw new VerifyError("Hashes of slice do not match.");
            }
        }
        
        return parentDigest;
    }
    
    /**
     * Basic node for FBHTree.
     */
    private static class Node implements Serializable {
        private int id;
        private boolean isLeaf;
        private boolean dirty;
        private byte[] contentDigest;
        private String contentDigestHexStr;
        
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
            
            this.contentDigestHexStr = HashUtils.byte2hex(contentDigest);
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
        
        public String getContentDigestHexString() {
            if (isDirty()) {
                contentDigestHexStr = HashUtils.byte2hex(getContentDigest());
            }
            
            return contentDigestHexStr;
        }
        
        public Collection<byte[]> getContents() {
            if (isLeaf) {
                if (contents == null) {
                    contents = new LinkedHashMap<>();
                }
                
                return contents.values();
            } else {
                throw new IllegalStateException("Internal node does not have contents.");
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
