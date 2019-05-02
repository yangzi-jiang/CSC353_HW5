package edu.davidson.csc353.microdb.indexes.bptree;

import java.util.ArrayList;
import java.util.function.Function;

import edu.davidson.csc353.microdb.files.Tuple;
import edu.davidson.csc353.microdb.files.Record;
import edu.davidson.csc353.microdb.files.Queriable;

import edu.davidson.csc353.microdb.indexes.RecordLocation;

import edu.davidson.csc353.microdb.indexes.SecondaryIndex;

public class BPTreeIndex<T extends Tuple, K extends Comparable<K>> implements SecondaryIndex<T, K> {
	public BPTree<K, RecordLocation> tree;
	public BPTreeIndex(Queriable<T> queriable, Function<T, K> keyExtractor, Function<String, K> loadKey) {
		//TODO: - create a BPTree<K, RecordLocation> called "tree"
		//      - make sure to pass the appropriate function to convert from a string "(x, y)" to a RecordLocation
		//        block number x and record number y
		//      - iterate over the queriable, and add the entries to the B+Tree
		
		Function<String, RecordLocation> loadValue = (str) -> {
			str = str.substring(1, str.length()-1);
			String[] values = str.split(", ");
			//System.out.println(Integer.parseInt(values[0]) + "," + Integer.parseInt(values[1]));
			return new RecordLocation(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
		};
		
		tree = new BPTree<K, RecordLocation>(loadKey, loadValue);
		
		for (Record<T> t : queriable){
			K key = keyExtractor.apply(t.getTuple());
			RecordLocation value = new RecordLocation(t.getBlockNumber(), t.getRecordNumber());
			tree.insert(key, value);
		}
		
	}

	public Iterable<RecordLocation> get(K key) {
		ArrayList<RecordLocation> results = new ArrayList<>();
		
		RecordLocation location = tree.get(key);

		if(location != null) {
			results.add(location);
		}

		return results;
	}
}