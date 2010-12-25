package org.blackbeanbag.recipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class Indexer {
	private static final Logger LOG = Logger.getLogger(Indexer.class);
	private String m_docDir;
	private String m_indexDir;
    private Analyzer m_analyzer;
    private IndexWriter m_writer; 
		
	public Indexer(String docDir, String indexDir) {
		super();
		this.m_docDir = docDir;
		this.m_indexDir = indexDir;
		this.m_analyzer = new StandardAnalyzer();
		try {
			this.m_writer = new IndexWriter(indexDir, m_analyzer, true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getDocDir() {
		return m_docDir;
	}

	public String getIndexDir() {
		return m_indexDir;
	}
	
	public void createIndex() {
		LOG.debug("creating index");
		List<String> docs = scanDirectory();
		if (LOG.isDebugEnabled()) {
			LOG.debug("found docs: " + docs);
		}
		for (String doc: docs) {
			indexDocument(scanWordDocument(m_docDir + doc));
		}
		try {
			m_writer.optimize();
			m_writer.close();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
	/**
	 * 
	 * @return list of Word document files
	 */
	List<String> scanDirectory() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("scanning dir " + m_docDir);
		}
		
		File f = new File(m_docDir);
		if (!f.isDirectory()) {
			throw new IllegalArgumentException(m_docDir + " must be a directory");
		}
		String[] list = f.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".doc");
			}
		});
		return Arrays.asList(list);
	}
	
	/**
	 * @param file location of Word document
	 * 
	 * @return Lucene document suitable for indexing
	 */
	Document scanWordDocument(String file) {
		try {
			POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(file));
			WordExtractor extractor = new WordExtractor(fs);
			String[] paragraphs = extractor.getParagraphText();

			// assuming the first line is the recipe title
			String title = paragraphs[0].trim();
			
			Document doc = new Document();
			doc.add(Field.Keyword("file", file));
			doc.add(Field.Keyword("title", title));
			
			for (int i = 0; i < paragraphs.length; i++) {
				String paragraph = paragraphs[i];
				
				// Someday this tokenizer will be smarter and distinguish between
				// ingredients and amounts.  The index (or the search) should be
				// able to perform quantity conversions and recognize common
				// quantity abbreviations.  This may be done with a custom
				// Lucene tokenizer.
				//
				// For now we'll naively index each string that we come across
				
				StringTokenizer t = new StringTokenizer(paragraph);
				while (t.hasMoreTokens()) {
					doc.add(Field.Text("ingredient", t.nextToken().trim()));
				}
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Scanned file " + file);
				LOG.debug("created document " + doc);
			}
			return doc;					
		}
		catch (Exception e) {
		    LOG.error("Error parsing file " + file, e);
		    return null;
		}
	}	
	
	/**
	 * 
	 * @param doc Lucene document to index
	 */
	void indexDocument(Document doc) {
	    if (doc == null) {
	        return;
	    }
		if (LOG.isDebugEnabled()) {
			LOG.debug("Indexing document:  " + doc);
		}		
        try {
			m_writer.addDocument(doc);
		} 
        catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
