package org.blackbeanbag.recipe;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class SwingMain extends JFrame {
    private static final Logger     LOG                 = Logger.getLogger(SwingMain.class);
    private final ResultsTableModel m_resultsTableModel = new ResultsTableModel();
    private JTextField              m_status;
    private Searcher                m_searcher;

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
        String settings = System.getProperty("user.home") + File.separator + ".recipe-index";
        if (LOG.isDebugEnabled()) {
            LOG.debug("Settings directory: " + settings);
        }

        // Ensure that $HOME/recipe-index directory exists
        File settingsDir = new File(settings);
        if (settingsDir.exists()) {
            if (!settingsDir.isDirectory()) {
                throw new IllegalStateException("File " + settings + " must be a directory");
            }
        }
        else {
            settingsDir.mkdir();
        }

        // Load properties file
        Properties p = new Properties();
        String settingsFileName = settings + File.separator + "recipe-index.properties";
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
            throw new IllegalStateException(
                    "Missing property 'doc.dir' in file " + settingsFileName);
        }
        if (!docDir.endsWith(File.separator)) {
            docDir += File.separator;
        }

        String indexDir = settings + File.separator + "index";

        // TODO: this should not be done upon every startup if the index already exists
        new Indexer(docDir, indexDir).createIndex();

        m_searcher = new Searcher(indexDir);
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
                    int column = 0; // table.columnAtPoint(p);
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
    public static class ResultsTableModel extends AbstractTableModel {
        private List<Map<String, String>> m_files = new ArrayList<Map<String, String>>();

        public List<Map<String, String>> getFiles() {
            return m_files;
        }

        public void setFiles(List<Map<String, String>> files) {
            this.m_files = files;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return "File Name";
            case 1:
                return "Description";
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return m_files.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Map<String, String> m = m_files.get(rowIndex);
            switch (columnIndex) {
            case 0:
                return m.get("file");
            case 1:
                return m.get("title");
            }
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
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
        }
    }
}
