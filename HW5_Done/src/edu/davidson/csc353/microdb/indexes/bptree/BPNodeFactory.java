package edu.davidson.csc353.microdb.indexes.bptree;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.RandomAccessFile;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.function.Function;

import edu.davidson.csc353.microdb.files.Block;
import edu.davidson.csc353.microdb.files.Relation;
//import edu.davidson.csc353.microdb.files.BlockManager.BlockTimestamp;
import edu.davidson.csc353.microdb.utils.DecentPQ;


public class BPNodeFactory<K extends Comparable<K>, V> {
	public static final int DISK_SIZE = 512;

	public static final int CAPACITY = 15;

	private String indexName;

	private Function<String, K> loadKey;
	private Function<String, V> loadValue;

	private int numberNodes;

	private RandomAccessFile relationFile;
	private FileChannel relationChannel;

	// You should change the type of this nodeMap
	private HashMap<Integer, NodeTimestamp> nodeMap;
	private DecentPQ<NodeTimestamp> PQ;

	private class NodeTimestamp implements Comparable<NodeTimestamp> {
		public BPNode<K,V> node;
		public long lastUsed;

		public NodeTimestamp(BPNode<K,V> node, long lastUsed) {
			this.node = node;
			this.lastUsed = lastUsed;
		}

		public int compareTo(NodeTimestamp other) {
			return (int) (lastUsed - other.lastUsed);
		}
	}
	
	
	/**
	 * Creates a new NodeFactory object, which will operate a buffer manager for
	 * the nodes of a B+Tree.
	 * 
	 * @param indexName The name of the index stored on disk.
	 */
	public BPNodeFactory(String indexName, Function<String, K> loadKey, Function<String, V> loadValue) {
		try {
			this.indexName = indexName;
			this.loadKey = loadKey;
			this.loadValue = loadValue;

			Files.delete(Paths.get(indexName + ".db"));

			relationFile = new RandomAccessFile(indexName + ".db", "rws");
			relationChannel = relationFile.getChannel();

			numberNodes = 0;

			nodeMap = new HashMap<>();
			PQ = new DecentPQ<>();
		}
		catch (FileNotFoundException exception) {
			// Ignore: a new file has been created
		}
		catch(IOException exception) {
			throw new RuntimeException("Error accessing " + indexName);
		}
	}

	/**
	 * Creates a B+Tree node.
	 * 
	 * @param leaf Flag indicating whether the node is a leaf (true) or internal node (false)
	 * 
	 * @return A new B+Tree node.
	 */
	public BPNode<K,V> create(boolean leaf) {
		if(nodeMap.size() >= CAPACITY){
			evict();
		}
		BPNode<K,V> created = new BPNode<K,V>(leaf);
		created.number = numberNodes;
		
		//nodeMap.put(created.number, created);
		numberNodes++;
		
		// TODO
		NodeTimestamp timeStamp = new NodeTimestamp(created, System.nanoTime());
		
		nodeMap.put(created.number, timeStamp);
		
		PQ.add(timeStamp);
		
		return created;
	}

	/**
	 * Saves a node into disk.
	 * 
	 * @param node Node to be saved into disk.
	 */
	public void save(BPNode<K,V> node) {
		writeNode(node);
	}

	/**
	 * Reads a node from the disk.
	 * 
	 * @param nodeNumber Number of the node read.
	 * 
	 * @return Node read from the disk that has the provided number.
	 */
	private BPNode<K,V> readNode(int nodeNumber) {
		// TODO
		ByteBuffer buffer = ByteBuffer.allocate(DISK_SIZE);
		
		try {
			relationChannel.read(buffer, nodeNumber*DISK_SIZE);
			BPNode<K,V> newNode = new BPNode<K,V>(true);
			newNode.load(buffer, loadKey, loadValue);
			return newNode;
		}
		catch (IOException e) {
			throw new RuntimeException("Error accessing " + nodeNumber + " on file " + relationFile);
		}
		
	}

	/**
	 * Writes a node into disk.
	 * 
	 * @param node Node to be saved into disk.
	 */
	private void writeNode(BPNode<K,V> node) {
		// TODO
		ByteBuffer buffer = ByteBuffer.allocate(DISK_SIZE);
		
		node.save(buffer);
		
		try {
			buffer.rewind();
			relationChannel.write(buffer, node.number * DISK_SIZE);
		}
		catch (IOException e) {
			throw new RuntimeException("Error accessing " + node.number + " on file " + relationFile);
		}
		
		
	}

	/**
	 * Evicts the last recently used node back into disk.
	 */
	private void evict() {
		NodeTimestamp oldest = PQ.removeMin();
		nodeMap.remove(oldest.node.number);
		writeNode(oldest.node);
		// TODO
	}

	/**
	 * Returns the node associated with a particular number.
	 * 
	 * @param number The number to be converted to node (loading it from disk, if necessary).
	 * 
	 * @return The node associated with the provided number.
	 */
	public BPNode<K,V> getNode(int number) {
		// TODO
		if(nodeMap.containsKey(number)){
			NodeTimestamp timeStamp = nodeMap.get(number);
			timeStamp.lastUsed = System.nanoTime();
			PQ.increaseKey(timeStamp);
			return nodeMap.get(number).node;
		}
		else{
			BPNode<K,V> newNode = readNode(number);
			NodeTimestamp timeStamp = new NodeTimestamp(newNode, number);
			nodeMap.put(number, timeStamp);
			PQ.add(timeStamp);
			return nodeMap.get(number).node;
			
		}
	}
}
