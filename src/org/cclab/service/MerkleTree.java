package org.cclab.service;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.cclab.utility.HashUtils;

/**
 *
 * @author Scott
 */
public class MerkleTree implements Serializable {
    private final Node root;
    
    public MerkleTree(File root) {
        this.root = buildMerkleTree(root);
    }
    
    private Node buildMerkleTree(File rootFile) {
        Node node = new Node(rootFile);
        
        if (rootFile.isDirectory()) {
            for (File childFile: rootFile.listFiles()) {
                node.addChild(buildMerkleTree(childFile));
            }
        }
        
        node.getDigest();
        
        return node;
    }
    
    public void delete(Node node) {
        if (node.parent == null) {
            throw new IllegalArgumentException("cannot delete root");
        }
        
        node.parent.remove(node);
    }
    
    public void update(Node node) {
        node.isDirty = true;
    }
    
    public Node retrieve(String path) throws NoSuchElementException {
        Node target = root;
        
        for (String pathName: path.split(File.pathSeparator)) {
            target = target.findChild(pathName);
        }
        
        return target;
    }
    
    public Node getRoot() {
        return root;
    }
    
    public static class Node implements Serializable {
        protected File file;
        
        private String digest;
        private boolean isDirty;
        private Node parent;
        private final List<Node> children;
        
        public Node(File file) {
            this.file = file;
            
            this.isDirty = true;
            this.children = new ArrayList<>();
        }
        
        public Node getParent() {
            return parent;
        }
        
        public boolean setParent(Node newParent) {
            if (!newParent.children.contains(this)) {
                newParent.addChild(this);
                return true;
            } else {
                return false;
            }
        }
        
        public boolean addChild(Node child) {
            child.parent = this;
            
            if (!children.contains(child)) {
                children.add(child);
                return true;
            } else {
                return false;
            }
        }
        
        public boolean remove(Node child) {
            if (children.contains(child)) {
                children.remove(child);
                isDirty = true;
                
                return true;
            } else {
                return false;
            }
        }
        
        public Node findChild(String targetName) throws NoSuchElementException {
            for (Node child: children) {
                if (child.file.getName().equals(targetName)) {
                    return child;
                }
            }
            
            throw new NoSuchElementException("cannot find " + targetName);
        }
        
        public String getDigest() {
            if (isDirty) {
                if (file.isDirectory()) {
                    StringBuilder sb = new StringBuilder();
                    
                    for (Node child: children) {
                        sb.append(child.getDigest());
                    }
                    
                    digest = HashUtils.sha256(sb.toString());
                } else {
                    digest = HashUtils.sha256(file);
                }
                
                isDirty = false;
            }
            
            return digest;
        }
    }
}
