package org.blackbeanbag.recipe;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.lucene.document.Document;
import org.junit.Test;

public class IndexerTest {
    public static final String DOC_DIR   = "data";
    public static final String INDEX_DIR = "index";

    @Test
    public void testScanWordDocument() {
        String file = "data/Arroz con Gandules Recipe.doc";
        Indexer indexer = new Indexer(DOC_DIR, INDEX_DIR);
        Document d = indexer.scanWordDocument(file);
        assertNotNull(d);
        assertEquals(file, d.getValues("file")[0]);
    }

    @Test
    public void testScanPlainText() {
        String file = "data/Arroz con Gandules Recipe.doc";
        Indexer indexer = new Indexer(DOC_DIR, INDEX_DIR);
        Document d = indexer.scanPlainTextDocument(file);
        assertNotNull(d);
        assertEquals(file, d.getValues("file")[0]);
    }

    @Test
    public void testScanDirectory() {
        Indexer indexer = new Indexer(DOC_DIR, INDEX_DIR);
        List<String> files = indexer.scanDirectory();
        assertNotNull(files);
    }
}
