package org.blackbeanbag.recipe;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
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
    private Indexer                 m_indexer;
    private Searcher                m_searcher;
    private String                  m_docDir;
    private String                  m_indexDir;

    /**
     * Ensure that the index has been created and ready for searching
     */
    protected void initializeSearch() {
        String settings = System.getProperty("user.home") + File.separator
                + ".recipe-index";
        if (LOG.isDebugEnabled()) {
            LOG.debug("Settings directory: " + settings);
        }

        // Ensure that $HOME/recipe-index directory exists
        File settingsDir = new File(settings);
        if (settingsDir.exists()) {
            if (!settingsDir.isDirectory()) {
                throw new IllegalStateException("File " + settings
                        + " must be a directory");
            }
        }
        else {
            settingsDir.mkdir();
        }

        // Load properties file
        Properties p = new Properties();
        String settingsFileName = settings + File.separator
                + "recipe-index.properties";
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

        m_docDir = p.getProperty("doc.dir");
        if (m_docDir == null || m_docDir.isEmpty()) {
            throw new IllegalStateException(
                    "Missing property 'doc.dir' in file " + settingsFileName);
        }
        if (!m_docDir.endsWith(File.separator)) {
            m_docDir += File.separator;
        }

        m_indexDir = settings + File.separator + "index";

        m_indexer = new Indexer(m_docDir, m_indexDir);

        // TODO: this should not be done upon every startup if the index already
        // exists
        m_indexer.createIndex();

        m_searcher = new Searcher(m_indexDir);
    }

    /**
     * Create UI components and lay them out in the frame
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
                    int column = table.columnAtPoint(p);
                    String s = (String) table.getModel().getValueAt(
                            table.convertRowIndexToModel(row),
                            table.convertColumnIndexToModel(column));
                    LOG.debug("Opening document " + s);
                    try {
                        // TODO: open in new thread
                        Desktop.getDesktop().open(new File(s));
                    }
                    catch (Exception ex) {
                        String msg = "Error opening " + s + ": "
                                + ex.getMessage() == null ? ex.getClass()
                                .getName() : ex.getMessage();
                        LOG.error(msg, ex);
                        JOptionPane.showMessageDialog(table, msg, "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        JPanel tablePanel = new JPanel();
        tablePanel.setBorder(new TitledBorder("Results"));
        tablePanel.setLayout(new GridBagLayout());
        tablePanel.add(table, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 1, 1));

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
     * @param s
     *            search criteria
     */
    private void onSearch(final String s) {
        if (s.isEmpty()) {
            return;
        }
        SwingWorker<List<String>, Object> worker = new SwingWorker<List<String>, Object>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return m_searcher.doSearch(s);
            }

            @Override
            protected void done() {
                try {
                    List<String> results = get();
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
        private List<String> m_files = new ArrayList<String>();

        public List<String> getFiles() {
            return m_files;
        }

        public void setFiles(List<String> files) {
            this.m_files = files;
        }

        @Override
        public int getRowCount() {
            return m_files.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return m_files.get(rowIndex);
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            SwingMain window = new SwingMain();
            window.initializeSearch();
            window.buildUI();
        }
        catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            JOptionPane.showMessageDialog(null, msg, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
