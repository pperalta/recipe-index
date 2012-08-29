package org.blackbeanbag.recipe.scanners;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.blackbeanbag.recipe.Indexer;

import java.io.FileInputStream;
import java.util.*;


/**
 * Implementation of {@link Scanner} that supports Microsoft
 * word documents (Word 95 through Word 2003).
 */
public class WordScanner implements Scanner {
    private static final Logger LOG = Logger.getLogger(WordScanner.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsFile(String file) {
        return file.toLowerCase().endsWith("doc");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document scan(String file) {
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

                // Someday this tokenizer will be smarter and distinguish
                // between ingredients and amounts. The index (or the search)
                // should be able to perform quantity conversions and recognize
                // common quantity abbreviations. This may be done with a custom
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
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace("Created document " + doc);
            }
            return doc;
        }
        catch (Exception e) {
            LOG.error("Error parsing file " + file, e);
            return null;
        }
    }
}