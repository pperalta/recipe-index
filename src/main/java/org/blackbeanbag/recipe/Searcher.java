package org.blackbeanbag.recipe;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

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
            DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
            m_indexSearcher = new IndexSearcher(directoryReader);
            m_analyzer = new EnglishAnalyzer(Version.LUCENE_47);
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
            QueryParser parser = new QueryParser(Version.LUCENE_47, "ingredient", m_analyzer);
            Query query = parser.parse(criteria);
            ScoreDoc[] hits = m_indexSearcher.search(query, null, 1000).scoreDocs;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Search found " + hits.length + " hits");
            }
            List<Map<String, String>> results = new ArrayList<Map<String, String>>(hits.length);
            for (ScoreDoc hit : hits) {
                Document doc = m_indexSearcher.doc(hit.doc);
                Map<String, String> map = new HashMap<String, String>();
                map.put("file", doc.get("file"));
                map.put("title", doc.get("title"));
                results.add(map);
            }
            return results;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
