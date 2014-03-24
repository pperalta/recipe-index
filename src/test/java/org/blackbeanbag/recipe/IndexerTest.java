package org.blackbeanbag.recipe;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.apache.lucene.document.Document;
import org.blackbeanbag.recipe.scanners.Scanner;
import org.junit.Test;

public class IndexerTest {
    public static final String DOC_DIR   = "data";
    public static final String INDEX_DIR = "index";

    @Test
    public void testScanWordDocument() {
        testScan("data/Arroz con Gandules Recipe.doc");
    }

    @Test
    public void testScanPlainText() {
        testScan("data/Arroz con Gandules Recipe.txt");
    }

    private void testScan(String file) {
        Indexer indexer = new Indexer(DOC_DIR, INDEX_DIR);

        try {
            for (Scanner scanner : indexer.getScanners()) {
                if (scanner.supportsFile(file)) {
                    Document doc = scanner.scan(file);
                    assertNotNull(doc);
                    assertEquals(file, doc.getValues("file")[0]);
                }
            }
        }
        finally {
            indexer.close();
        }

    }
}
