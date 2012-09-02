package org.blackbeanbag.recipe;

import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

/**
 * SwingMain is the main entry point for the application. Upon startup
 * the Lucene index will be created before displaying the frame.
 */
@SuppressWarnings("serial")
public class SwingMain extends JFrame {
    /**
     * Logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(SwingMain.class);

    /**
     * Table model for search results.
     */
    private final ResultsTableModel m_resultsTableModel = new ResultsTableModel();

    /**
     * Text field for status bar at the bottom of the frame.
     */
    private JTextField m_status;

    /**
     * Searcher used to perform searches.
     */
    private Searcher m_searcher;

    /**
     * Initialize the search index. The initialization process ensures that:
     * <ul>
     *     <li>the {@code ~/.recipe-index} directory has been created</li>
     *     <li>the {@code ~/.recipe-index/recipe-index.properties} file exists</li>
     *     <li>the property {@code doc.dir} exists and points to a valid directory</li>
     *     <li>the index is created with the contents of the doc directory</li>
     * </ul>
     */
    protected void initializeSearch() {
        String settingsDir = ensureSettingsDir();
        String docDir = getDocumentDir(settingsDir);
        String indexDir = settingsDir + File.separator + "index";

        // prior to indexing the documents, display a dialog box indicating
        // the creation of the index to avoid the appearance of an unresponsive application
        JDialog dialog = new JDialog(this, false);
        dialog.setLocationRelativeTo(null);
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.getContentPane().add(new JLabel("Indexing recipes, please wait..."));
        dialog.pack();
        dialog.setVisible(true);

        try {
            new Indexer(docDir, indexDir).createIndex();
        }
        finally {
            dialog.setVisible(false);
            dialog.dispose();
        }

        m_searcher = new Searcher(indexDir);
    }

    /**
     * Ensure that the settings directory {@code ($HOME/.recipe-index)} exists.
     *
     * @return the settings directory
     */
    protected String ensureSettingsDir() {
        String settings = System.getProperty("user.home") + File.separator + ".recipe-index";
        if (LOG.isDebugEnabled()) {
            LOG.debug("Settings directory: " + settings);
        }

        File settingsDir = new File(settings);
        if (settingsDir.exists()) {
            if (!settingsDir.isDirectory()) {
                throw new IllegalStateException("File " + settings + " must be a directory");
            }
        }
        else {
            boolean created = settingsDir.mkdir();
            assert created : String.format("Settings directory %s could not be created", settings);
        }
        return settings;
    }

    /**
     * Read the settings file (settingsDir + recipe-index.properties) and return
     * the document directory containing documents to be indexed. If the settings
     * file does not exist or does not contain the property, a user dialog will
     * prompt the user to choose a directory and the chosen directory will be saved
     * to the preferences file.
     * <br />
     * The property containing the document directory is {@code doc.dir}.
     *
     * @param settingsDir the settings directory (typically {@code $HOME/.recipe-index}).
     *
     * @return the directory containing documents to index
     */
    protected String getDocumentDir(String settingsDir) {
        Properties p = new Properties();
        String settingsFileName = settingsDir + File.separator + "recipe-index.properties";
        File settingsProperties = new File(settingsFileName);

        try {
            if (!settingsProperties.exists()) {
                settingsProperties.createNewFile();
            }
            p.load(new FileInputStream(settingsFileName));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        String docDir = p.getProperty("doc.dir");
        if (docDir == null || docDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Recipe directory has not been selected. " +
                    "You will now be prompted to select the recipe directory.",
                    "Information", JOptionPane.INFORMATION_MESSAGE);

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                docDir = chooser.getSelectedFile().getAbsolutePath();
                p.setProperty("doc.dir", docDir);
                try {
                    p.store(new FileWriter(settingsFileName), "Recipe Finder Settings");
                }
                catch (IOException e) {
                    String msg = String.format("Could not save preference file '%s'", settingsFileName);
                    LOG.warn(msg, e);
                    JOptionPane.showMessageDialog(this, msg + "; see error log for details",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        if (docDir == null || docDir.isEmpty()) {
            throw new IllegalStateException(
                    "Could not determine document directory; check for missing property 'doc.dir' in file " +
                            settingsFileName);
        }

        if (!docDir.endsWith(File.separator)) {
            docDir += File.separator;
        }
        return docDir;
    }

    /**
     * Create UI components and lay them out in the frame.
     */
    protected void buildUI() {
        JComponent contentPane = (JComponent) getContentPane();
        contentPane.setLayout(new GridBagLayout());

        // ----- search field and button ---------------------------------
        final JTextField search = new JTextField();

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSearch(search.getText());
            }
        };
        search.addActionListener(listener);

        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                onSearch(search.getText() + e.getKeyChar());
            }
        };
        search.addKeyListener(keyListener);

        JButton button = new JButton("Search");
        button.addActionListener(listener);

        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(new TitledBorder("Search"));
        searchPanel.setLayout(new GridBagLayout());

        searchPanel.add(search, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 2, 2));
        searchPanel.add(button, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 2, 2));

        // ----- table --------------------------------------------------
        JTable table = new JTable(m_resultsTableModel);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getComponent().isEnabled()
                        && e.getButton() == MouseEvent.BUTTON1
                        && e.getClickCount() == 2) {
                    JTable table = (JTable) e.getSource();
                    Point p = e.getPoint();
                    int row = table.rowAtPoint(p);
                    int column = ResultsTableModel.FILE_NAME;
                    final String s = (String) table.getModel().getValueAt(
                            table.convertRowIndexToModel(row),
                            table.convertColumnIndexToModel(column));

                    LOG.debug("Opening document " + s);

                    try {
                        new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                Desktop.getDesktop().open(new File(s));
                                return null;
                            }
                        }.execute();
                    }
                    catch (Exception ex) {
                        String msg = "Error opening " + s + ": "
                                + (ex.getMessage() == null ? ex.getClass()
                                .getName() : ex.getMessage());
                        LOG.error(msg, ex);
                        JOptionPane.showMessageDialog(table, msg, "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // ----- table scroll pane --------------------------------------

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel tablePanel = new JPanel();
        tablePanel.setBorder(new TitledBorder("Results"));

        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.PAGE_AXIS));
        tablePanel.add(scrollPane);

        // ----- status bar ---------------------------------------------
        m_status = new JTextField("Total Results: 0");
        m_status.setEditable(false);

        // ----- set up main panel --------------------------------------
        contentPane.add(searchPanel, new GridBagConstraints(0, 0, 1, 1, 1.0,
                0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 2, 2));
        contentPane.add(tablePanel, new GridBagConstraints(0, 1, 1, 1, 0.0,
                0.8, GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                new Insets(2, 2, 2, 2), 2, 2));
        contentPane.add(m_status, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
                        2, 2, 2, 2), 2, 2));

        // ----- finalize window ----------------------------------------
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(640, 480));
        pack();
        setTitle("Recipe Search");
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Execute search and update UI with results. It is assumed that this will
     * be invoked via the AWT thread, thus the actual search will be performed
     * in another thread (using {@link SwingWorker}).
     * 
     * @param s search criteria
     */
    private void onSearch(final String s) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Search term: " + s);
        }

        SwingWorker<List<Map<String, String>>, Object> worker = new SwingWorker<List<Map<String, String>>, Object>() {
            @Override
            protected List<Map<String, String>> doInBackground()
                    throws Exception {
                if (s == null || s.length() < 2) {
                    return Collections.emptyList();
                }
                else {
                    return m_searcher.doSearch(s);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, String>> results = get();
                    m_resultsTableModel.setFiles(results);
                    m_resultsTableModel.fireTableDataChanged();
                    m_status.setText("Total Results: " + results.size());
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        worker.execute();
    }

    /**
     * Table model that represents search results
     */
    public class ResultsTableModel extends AbstractTableModel {
        /**
         * Table column for file description.
         */
        public static final int DESCRIPTION = 0;

        /**
         * Table column for file name.
         */
        public static final int FILE_NAME = 1;

        /**
         * Search results.
         */
        private List<Map<String, String>> m_files = new ArrayList<Map<String, String>>();

        /**
         * Return the search results.
         *
         * @return search results
         *
         * @see Searcher#doSearch(String)
         */
        public List<Map<String, String>> getFiles() {
            return m_files;
        }

        /**
         * Set the search results to display.
         *
         * @param files search results
         *
         * @see Searcher#doSearch(String)
         */
        public void setFiles(List<Map<String, String>> files) {
            this.m_files = files;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
            case FILE_NAME:
                return "File Name";
            case DESCRIPTION:
                return "Description";
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return m_files.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return 2;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Map<String, String> m = m_files.get(rowIndex);
            switch (columnIndex) {
            case FILE_NAME:
                return m.get("file");
            case DESCRIPTION:
                return m.get("title");
            }
            return null;
        }
    }

    /**
     * Main entry point for the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            LOG.info("Logging to " + System.getProperty("java.io.tmpdir"));
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            SwingMain window = new SwingMain();
            window.initializeSearch();
            window.buildUI();
        }
        catch (Exception e) {
            LOG.error("Unhandled exception", e);
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            JOptionPane.showMessageDialog(null, msg, "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
}
