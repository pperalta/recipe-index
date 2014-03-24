package org.blackbeanbag.recipe.scanners;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Implementation of {@link Scanner} that supports plain
 * text documents.
 */
public class TextScanner implements Scanner {
    private static final Logger LOG = Logger.getLogger(TextScanner.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsFile(String file) {
        return file.toLowerCase().endsWith("txt");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document scan(String file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String title = reader.readLine();

            Document doc = new Document();
            doc.add(new Field("file", file, TextField.TYPE_STORED));
            doc.add(new Field("title", title, TextField.TYPE_STORED));

            String line;

            while ((line = reader.readLine()) != null) {
                StringTokenizer t = new StringTokenizer(line);
                while (t.hasMoreTokens()) {
                    doc.add(new Field("ingredient", t.nextToken().trim(), TextField.TYPE_STORED));
                }
            }

            reader.close();
            reader = null;

            if (LOG.isDebugEnabled()) {
                LOG.debug("Scanned file " + file);
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace("Created document " + doc);
            }
            return doc;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    LOG.warn("Error closing file", e);
                }
            }
        }
    }
}