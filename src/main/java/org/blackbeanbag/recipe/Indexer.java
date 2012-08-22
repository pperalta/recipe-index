package org.blackbeanbag.recipe;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class Indexer {
    private static final Logger LOG = Logger.getLogger(Indexer.class);
    private String              m_docDir;
    private String              m_indexDir;
    private IndexWriter         m_writer;

    public Indexer(String docDir, String indexDir) {
        super();
        this.m_docDir = docDir;
        this.m_indexDir = indexDir;

        try {
            this.m_writer = new IndexWriter(indexDir, new StandardAnalyzer(), true);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getDocDir() {
        return m_docDir;
    }

    public String getIndexDir() {
        return m_indexDir;
    }

    public void createIndex() {
        LOG.debug("creating index");
        List<String> docs = scanDirectory();
        if (LOG.isDebugEnabled()) {
            LOG.debug("found docs: " + docs);
        }
        for (String doc : docs) {
            indexDocument(scanWordDocument(doc));
        }
        try {
            m_writer.optimize();
            m_writer.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * @return list of Word document files
     */
    List<String> scanDirectory() {
        return scanDirectory(new File(m_docDir));
    }

    /**
     * @param fileDir directory to search
     * 
     * @return list of word docs in this directory
     */
    private List<String> scanDirectory(File fileDir) {
        List<String> fileList = new LinkedList<String>();
        List<File> dirList = new LinkedList<File>();

        if (!fileDir.isDirectory()) {
            throw new IllegalArgumentException(fileDir + " must be a directory");
        }

        LOG.info("Scanning directory " + fileDir);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning directory " + fileDir);
        }

        File[] files = fileDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                dirList.add(files[i]);
            }
            else if (files[i].getName().endsWith("doc")) {
                fileList.add(files[i].getAbsolutePath());
            }
        }

        for (File dir : dirList) {
            fileList.addAll(scanDirectory(dir));
        }

        return fileList;
    }

    /**
     * @param file location of Word document
     * 
     * @return Lucene document suitable for indexing
     */
    Document scanWordDocument(String file) {
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
                LOG.debug("created document " + doc);
            }
            return doc;
        }
        catch (Exception e) {
            LOG.error("Error parsing file " + file, e);
            return null;
        }
    }

    /**
     * todo
     *
     * @param file
     * @return
     */
    Document scanPlainTextDocument(String file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String title = reader.readLine();

            Document doc = new Document();
            doc.add(Field.Keyword("file", file));
            doc.add(Field.Keyword("title", title));

            String line;

            while ((line = reader.readLine()) != null) {
                StringTokenizer t = new StringTokenizer(line);
                while (t.hasMoreTokens()) {
                    doc.add(Field.Text("ingredient", t.nextToken().trim()));
                }
            }

            reader.close();
            reader = null;

            if (LOG.isDebugEnabled()) {
                LOG.debug("Scanned file " + file);
                LOG.debug("created document " + doc);
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

    /**
     * @param doc Lucene document to index
     */
    void indexDocument(Document doc) {
        if (doc == null) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Indexing document:  " + doc);
        }
        try {
            m_writer.addDocument(doc);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
