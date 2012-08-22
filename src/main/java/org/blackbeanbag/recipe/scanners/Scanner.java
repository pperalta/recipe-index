package org.blackbeanbag.recipe.scanners;

import org.apache.lucene.document.Document;

/**
 * The Scanner interface defines methods required
 * to support indexing of document types.
 */
public interface Scanner {
    /**
     * Determine if a file is supported by this scanner.
     *
     * @param file file to determine support for
     *
     * @return true if this scanner supports this file
     */
    boolean supportsFile(String file);

    /**
     * Scan the given file and return a Lucene document
     * for indexing.
     *
     * @param file file to index
     *
     * @return a Lucene document for indexing
     */
    Document scan(String file);
}