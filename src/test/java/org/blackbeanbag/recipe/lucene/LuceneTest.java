package org.blackbeanbag.recipe.lucene;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.junit.BeforeClass;
import org.junit.Test;

public class LuceneTest {

    public static Document createDocument() {
        Document doc = new Document();
        doc.add(Field.Keyword("title", "Wild Turkey Surprise"));
        doc.add(Field.Text("ingredient", "1/2 cup sugar"));
        doc.add(Field.Text("ingredient", "1 tsp salt"));
        doc.add(Field.Text("ingredient", "15 cups of vinegar"));
        return doc;
    }

    public static void indexDocument(Document document) throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter writer = new IndexWriter("index", analyzer, true);
        writer.addDocument(document);
        writer.optimize();
        writer.close();
    }

    public static Hits doSearch(String term) throws IOException, ParseException {
        IndexSearcher is = new IndexSearcher("index");
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("ingredient", analyzer);
        Query query = parser.parse(term);
        return is.search(query);
    }

    private static void assertDocumentHit(Hits hits) throws IOException {
        assertEquals("One result expected", 1, hits.length());
        assertEquals("Expected document match", hits.doc(0).getField("title")
                .stringValue(), "Wild Turkey Surprise");
    }

    @BeforeClass
    public static void createIndex() throws IOException, ParseException {
        indexDocument(createDocument());
    }

    @Test
    public void simpleSearch() throws IOException, ParseException {
        assertDocumentHit(doSearch("sugar"));
    }

    @Test
    public void multipleTokenSearch() throws IOException, ParseException {
        assertDocumentHit(doSearch("1 tsp"));
    }

}
