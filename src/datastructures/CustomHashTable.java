package datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom implementation of a Hash Table from scratch using separate chaining.
 * This class handles collisions using linked lists and supports dynamic resizing.
 * Zero external dependencies.
 */
public class CustomHashTable {
    private static final int INITIAL_CAPACITY = 1009; // A prime number for better distribution
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;

    private Node[] table;
    private int size;

    private static class Node {
        final String key;
        int value;
        Node next;

        Node(String key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    public CustomHashTable() {
        this.table = new Node[INITIAL_CAPACITY];
        this.size = 0;
    }

    /**
     * Helper to compute bucket index for a key.
     */
    private int getBucketIndex(String key) {
        if (key == null) return 0;
        // Ensure index is positive using bitwise AND to clear the sign bit
        int hashCode = key.hashCode();
        return (hashCode & 0x7FFFFFFF) % table.length;
    }

    /**
     * Inserts or updates the value of the key.
     */
    public void put(String key, int value) {
        if (key == null) return;

        // Check if load factor exceeds threshold for resizing
        if ((double) size / table.length >= LOAD_FACTOR_THRESHOLD) {
            resize();
        }

        int index = getBucketIndex(key);
        Node head = table[index];
        Node curr = head;

        // Search for existing key
        while (curr != null) {
            if (curr.key.equals(key)) {
                curr.value = value;
                return;
            }
            curr = curr.next;
        }

        // Key doesn't exist, insert at the head of the list (separate chaining)
        Node newNode = new Node(key, value);
        newNode.next = table[index];
        table[index] = newNode;
        size++;
    }

    /**
     * Retrieves the value associated with the key.
     * Returns 0 if key not found (useful for frequency lookup).
     */
    public int get(String key) {
        if (key == null) return 0;
        int index = getBucketIndex(key);
        Node curr = table[index];
        while (curr != null) {
            if (curr.key.equals(key)) {
                return curr.value;
            }
            curr = curr.next;
        }
        return 0;
    }

    /**
     * Checks if key exists in the hash table.
     */
    public boolean containsKey(String key) {
        if (key == null) return false;
        int index = getBucketIndex(key);
        Node curr = table[index];
        while (curr != null) {
            if (curr.key.equals(key)) {
                return true;
            }
            curr = curr.next;
        }
        return false;
    }

    /**
     * Returns the total number of hash collisions in the table.
     * A collision is defined as any node that is chained after the head node in a bucket.
     */
    public int getCollisionCount() {
        int collisions = 0;
        for (int i = 0; i < table.length; i++) {
            Node curr = table[i];
            if (curr != null) {
                // The first node is not a collision, but any subsequent nodes in the chain are
                curr = curr.next;
                while (curr != null) {
                    collisions++;
                    curr = curr.next;
                }
            }
        }
        return collisions;
    }

    /**
     * Returns a list of all keys currently in the hash table.
     */
    public List<String> keys() {
        List<String> keyList = new ArrayList<>(size);
        for (int i = 0; i < table.length; i++) {
            Node curr = table[i];
            while (curr != null) {
                keyList.add(curr.key);
                curr = curr.next;
            }
        }
        return keyList;
    }

    /**
     * Returns the total number of key-value mappings.
     */
    public int size() {
        return size;
    }

    /**
     * Resizes the array when load factor exceeds threshold.
     */
    private void resize() {
        Node[] oldTable = table;
        // Double capacity + 1 (often yields a prime or odd number)
        int newCapacity = oldTable.length * 2 + 1;
        table = new Node[newCapacity];
        size = 0; // Will be recalculated during re-insertion

        for (int i = 0; i < oldTable.length; i++) {
            Node curr = oldTable[i];
            while (curr != null) {
                put(curr.key, curr.value);
                curr = curr.next;
            }
        }
    }
}
