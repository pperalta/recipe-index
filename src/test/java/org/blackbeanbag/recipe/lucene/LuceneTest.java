package org.blackbeanbag.recipe.lucene;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.BeforeClass;
import org.junit.Test;

public class LuceneTest {

    public static Document createDocument() {
        Document doc = new Document();
        doc.add(new Field("title", "Wild Turkey Surprise", TextField.TYPE_STORED));
        doc.add(new Field("ingredient", "1/2 cup sugar", TextField.TYPE_STORED));
        doc.add(new Field("ingredient", "1 tsp salt", TextField.TYPE_STORED));
        doc.add(new Field("ingredient", "15 cups of vinegar", TextField.TYPE_STORED));
        return doc;
    }

    public static void indexDocument(Document document) throws IOException {
        Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_47);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
        Directory directory = FSDirectory.open(new File("index"));
        IndexWriter writer = new IndexWriter(directory, config);
        writer.deleteAll();

        writer.addDocument(document);
        writer.close();
    }

    public static IndexSearcher createIndexSearcher() throws IOException {
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(new File("index")));
        return new IndexSearcher(directoryReader);
    }

    public static TopDocs doSearch(IndexSearcher searcher, String term) throws IOException, ParseException {
        Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_47);
        QueryParser parser = new QueryParser(Version.LUCENE_47, "ingredient", analyzer);

        Query query = parser.parse(term);
        return searcher.search(query, null, 1000);
    }

    private static void assertDocumentHit(TopDocs docs, IndexSearcher searcher) throws IOException {
        assertEquals("One result expected", 1, docs.totalHits);
        assertEquals("Expected document match", searcher.doc(0).getField("title")
                .stringValue(), "Wild Turkey Surprise");
    }

    @BeforeClass
    public static void createIndex() throws IOException, ParseException {
        indexDocument(createDocument());
    }

    @Test
    public void simpleSearch() throws IOException, ParseException {
        IndexSearcher searcher = createIndexSearcher();
        assertDocumentHit(doSearch(searcher, "sugar"), searcher);
    }

    @Test
    public void multipleTokenSearch() throws IOException, ParseException {
        IndexSearcher searcher = createIndexSearcher();
        assertDocumentHit(doSearch(searcher, "1 tsp"), searcher);
    }

}
