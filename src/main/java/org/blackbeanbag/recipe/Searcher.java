package org.blackbeanbag.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * Searcher is used for performing text queries against a
 * Lucene index.
 */
public class Searcher {
    /**
     * Logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(Searcher.class);

    /**
     * Lucene index searcher.
     */
    private IndexSearcher m_indexSearcher;

    /**
     * Lucene Analyzer.
     */
    private Analyzer m_analyzer;

    /**
     * Construct a Searcher based on an index directory.
     * <b>Note that the index must have been created prior to
     * creating a Searcher.</b>
     *
     * @see Indexer
     *
     * @param indexDir directory containing existing index
     */
    public Searcher(String indexDir) {
        try {
            m_indexSearcher = new IndexSearcher(indexDir);
            m_analyzer = new StandardAnalyzer();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute a search. The search results are a {@link List}. Each list
     * element consists of a {@link Map} which contains two entries: an
     * entry containing the file name of the result, and an entry containing
     * the document title. Keys and values for this map are Strings.
     *
     * @param criteria the search criteria
     *
     * @return a list of results
     */
    public List<Map<String, String>> doSearch(String criteria) {
        try {
            QueryParser parser = new QueryParser("ingredient", m_analyzer);
            Query query = parser.parse(criteria);
            Hits hits = m_indexSearcher.search(query);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Search found " + hits.length() + " hits");
            }
            List<Map<String, String>> results = new ArrayList<Map<String, String>>(hits.length());
            for (int i = 0; i < hits.length(); i++) {
                Map<String, String> map = new HashMap<String, String>();
                map.put("file", hits.doc(i).get("file"));
                map.put("title", hits.doc(i).get("title"));
                results.add(map);
            }
            return results;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
