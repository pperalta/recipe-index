package org.blackbeanbag.recipe;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class Searcher {
	private static final Logger LOG = Logger.getLogger(Searcher.class);
	
    private IndexSearcher m_indexSearcher;
    private Analyzer m_analyzer;
		
	public Searcher(String indexDir) {
		try {
		    m_indexSearcher = new IndexSearcher(indexDir);
		    m_analyzer = new StandardAnalyzer();			
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}	
	}

	public List<String> doSearch(String criteria) {
		try {
	        QueryParser parser = new QueryParser("ingredient", m_analyzer);
	        Query query = parser.parse(criteria);
	        Hits hits = m_indexSearcher.search(query);
	        if (LOG.isDebugEnabled()) {
	        	LOG.debug("Search found " + hits.length() + " hits");
	        }
	        List<String> results = new ArrayList<String>(hits.length());
	        for (int i = 0; i < hits.length(); i++) {
	        	String file = hits.doc(i).get("file");
	        	results.add(file);
	        }
	        return results;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
