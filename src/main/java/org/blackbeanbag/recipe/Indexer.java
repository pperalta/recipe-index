package org.blackbeanbag.recipe;


import org.apache.log4j.Logger;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;

import org.blackbeanbag.recipe.scanners.Scanner;
import org.blackbeanbag.recipe.scanners.TextScanner;
import org.blackbeanbag.recipe.scanners.WordScanner;

import java.io.File;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The Indexer class creates a Lucene index for supported documents.
 */
public class Indexer {
    private static final Logger LOG = Logger.getLogger(Indexer.class);
    private String              m_docDir;
    private String              m_indexDir;
    private IndexWriter         m_writer;
    private Collection<Scanner> m_scanners;

    /**
     * Construct an Indexer.
     *
     * @param docDir    directory containing documents to index
     * @param indexDir  directory containing the index
     */
    public Indexer(String docDir, String indexDir) {
        this(docDir, indexDir, Arrays.asList(new WordScanner(), new TextScanner()));
    }

    /**
     * Construct an Indexer.
     *
     * @param docDir    directory containing documents to index
     * @param indexDir  directory containing the index
     * @param scanners  a collection of {@link Scanner} objects
     */
    public Indexer(String docDir, String indexDir, Collection<Scanner> scanners) {
        this.m_docDir = docDir;
        this.m_indexDir = indexDir;
        this.m_scanners = scanners;

        try {
            this.m_writer = new IndexWriter(indexDir, new StandardAnalyzer(), true);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the directory containing documents to index.
     *
     * @return directory containing documents to index
     */
    public String getDocDir() {
        return m_docDir;
    }

    /**
     * Return the directory containing the index file.
     *
     * @return the directory containing the index
     */
    public String getIndexDir() {
        return m_indexDir;
    }

    /**
     * Return the Lucene index writer.
     *
     * @return the index writer
     */
    public IndexWriter getWriter() {
        return m_writer;
    }

    /**
     * Return the collection of scanners used by this indexer.
     *
     * @return scanners used by this indexer
     */
    public Collection<Scanner> getScanners() {
        return m_scanners;
    }

    /**
     * Create the index.
     */
    public void createIndex() {
        LOG.debug("Creating index");

        scanDirectory();

        try {
            getWriter().optimize();
            getWriter().close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Scan the configured document directory for supported
     * documents.
     */
    protected void scanDirectory() {
        scanDirectory(new File(getDocDir()));
    }

    /**
     * Scan the given directory file for supported documents.
     *
     * @param fileDir directory to search
     */
    private void scanDirectory(File fileDir) {
        List<File> dirList = new LinkedList<File>();

        if (!fileDir.isDirectory()) {
            throw new IllegalArgumentException(fileDir + " must be a directory");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning directory " + fileDir);
        }

        File[] files = fileDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                dirList.add(files[i]);
            }
            else {
                String file = files[i].getAbsolutePath();

                for (Scanner scanner : getScanners()) {
                    if (scanner.supportsFile(file)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Scanning file " + file);
                        }
                        try {
                            getWriter().addDocument(scanner.scan(file));
                        }
                        catch (Exception e) {
                            LOG.warn("Could not process file " + file, e);
                        }
                    }
                }
            }
        }

        for (File dir : dirList) {
            scanDirectory(dir);
        }
    }
}
