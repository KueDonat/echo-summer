package com.echosummer.game.ds;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic N-ary Tree data structure used for story decision branches and narrative hierarchies.
 *
 * @param <T> Node payload data type
 */
public class CustomTree<T> {

    public static class TreeNode<E> {
        private E data;
        private TreeNode<E> parent;
        private final List<TreeNode<E>> children;

        public TreeNode(E data) {
            this.data = data;
            this.parent = null;
            this.children = new ArrayList<>();
        }

        public E getData() {
            return data;
        }

        public void setData(E data) {
            this.data = data;
        }

        public TreeNode<E> getParent() {
            return parent;
        }

        public List<TreeNode<E>> getChildren() {
            return children;
        }

        public TreeNode<E> addChild(E childData) {
            TreeNode<E> childNode = new TreeNode<>(childData);
            childNode.parent = this;
            this.children.add(childNode);
            return childNode;
        }

        public void addChild(TreeNode<E> childNode) {
            childNode.parent = this;
            this.children.add(childNode);
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public boolean isRoot() {
            return parent == null;
        }
    }

    private TreeNode<T> root;

    public CustomTree() {
        this.root = null;
    }

    public CustomTree(T rootData) {
        this.root = new TreeNode<>(rootData);
    }

    public TreeNode<T> getRoot() {
        return root;
    }

    public void setRoot(TreeNode<T> root) {
        this.root = root;
    }

    public boolean isEmpty() {
        return root == null;
    }

    /**
     * Traverses the tree pre-order and collects all node payloads.
     */
    public List<T> preOrderTraversal() {
        List<T> result = new ArrayList<>();
        if (root != null) {
            preOrderHelper(root, result);
        }
        return result;
    }

    private void preOrderHelper(TreeNode<T> node, List<T> result) {
        result.add(node.getData());
        for (TreeNode<T> child : node.getChildren()) {
            preOrderHelper(child, result);
        }
    }

    /**
     * Finds a node in the tree matching the given predicate.
     */
    public TreeNode<T> findNode(java.util.function.Predicate<T> predicate) {
        if (root == null) return null;
        return findHelper(root, predicate);
    }

    private TreeNode<T> findHelper(TreeNode<T> curr, java.util.function.Predicate<T> predicate) {
        if (predicate.test(curr.getData())) {
            return curr;
        }
        for (TreeNode<T> child : curr.getChildren()) {
            TreeNode<T> res = findHelper(child, predicate);
            if (res != null) return res;
        }
        return null;
    }
}
