package org.blackbeanbag.recipe;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.lucene.document.Document;
import org.junit.Test;


public class IndexerTest {
	public static final String DOC_DIR = "H:\\opt\\src\\recipe-index\\data\\";
	public static final String INDEX_DIR = "H:\\opt\\src\\recipe-index\\index\\";
	
	@Test
	public void foo() {
		
	}
	
	public void testScanWordDocument() {
		String file = "H:\\opt\\src\\recipe-index\\data\\Arroz con Gandules Recipe.doc";
		Indexer indexer = new Indexer(DOC_DIR, INDEX_DIR);
		Document d = indexer.scanWordDocument(file);
		assertNotNull(d);
		assertEquals(file, d.getValues("file")[0]);		
	}
	
	public void testScanDirectory() {		
		Indexer indexer = new Indexer(DOC_DIR, INDEX_DIR);
		List<String> files = indexer.scanDirectory();
		assertNotNull(files);
	}

}
