package org.blackbeanbag.recipe.poi;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.Test;

/**
 * Test case for asserting the Apache POI functionality.
 * 
 * @author pperalta
 * 
 */
public class WordDocPoiTest {
    @Test
    public void readFirstLine() throws FileNotFoundException, IOException {
        POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(
                "data/Arroz con Gandules Recipe.doc"));
        assertNotNull(fs);

        WordExtractor extractor = new WordExtractor(fs);
        assertNotNull(extractor);

        String[] paragraphs = extractor.getParagraphText();
        assertNotNull(paragraphs);

        String title = paragraphs[0].trim();
        assertEquals("Document title match", "Arroz con Gandules Recipe", title);
    }
}
