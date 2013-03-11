package com.compomics.denovogui.gui;

import com.compomics.denovogui.DeNovoSearchHandler;
import com.compomics.denovogui.DeNovoGUIWrapper;
import com.compomics.denovogui.execution.jobs.PepnovoJob;
import com.compomics.denovogui.gui.panels.ResultsPanel;
import com.compomics.denovogui.util.ExtensionFileFilter;
import com.compomics.denovogui.util.Properties;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.idfilereaders.PepNovoIdfileReader;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.preferences.ModificationProfile;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import net.jimmc.jshortcut.JShellLink;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;

/**
 * The main DeNovoGUI frame.
 *
 * @author Thilo Muth
 * @author Harald Barsnes
 */
public class DeNovoGUI extends javax.swing.JFrame {

    /**
     * Modification file.
     */
    private final static String MODIFICATION_FILE = "resources/conf/denovogui_mods.xml";
    /**
     * User modification file.
     */
    private final static String USER_MODIFICATION_FILE = "resources/conf/denovogui_usermods.xml";
    /**
     * The enzyme file
     */
    private final static String ENZYME_FILE = "resources/conf/enzymes.xml";
    /**
     * The compomics enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance(1000);
    /**
     * The factory used to handle the modifications.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The location of the folder used for caching.
     */
    public final static String CACHE_DIRECTORY = "resources/cache";
    /**
     * De novo identification
     */
    private Identification identification;
    /**
     * The search handler
     */
    private DeNovoSearchHandler searchHandler;
    /**
     * The search parameters
     */
    private SearchParameters searchParameters = null;
    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "user.home";
    /**
     * The selected output folder for the de novo search.
     */
    private File outputFolder;
    /**
     * The selected PepNovo folder.
     */
    private File pepNovoFolder;
    /**
     * Spectra files list.
     */
    private List<File> spectrumFiles = new ArrayList<File>();
    /**
     * The color used for the sparkline bar chart plots.
     */
    private Color sparklineColor = new Color(110, 196, 97);
    /**
     * The label with for the numbers in the jsparklines columns.
     */
    private int labelWidth = 50;
    /**
     * The dialog displayed during the search.
     */
    private WaitingDialog waitingDialog;
    /**
     * If set to true SearchGUI is ran from the command line only, i.e., no GUI
     * will appear.
     */
    private static boolean useCommandLine = false;
    /**
     * The search task.
     */
    private SearchTask searchWorker;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;

    /**
     * Creates new form DeNovoGUI
     */
    public DeNovoGUI() {

        // check for new version
        CompomicsWrapper.checkForNewVersion(getVersion(), "DeNovoGUI", "denovogui");

        // set up the ErrorLog
        setUpLogFile();

        // add desktop shortcut?
        if (!getJarFilePath().equalsIgnoreCase(".")
                && System.getProperty("os.name").lastIndexOf("Windows") != -1
                && new File(getJarFilePath() + "/resources/conf/firstRun").exists()) {

            // @TODO: add support for desktop icons in mac and linux??

            // delete the firstRun file such that the user is not asked the next time around
            boolean fileDeleted = new File(getJarFilePath() + "/resources/conf/firstRun").delete();

            if (!fileDeleted) {
                JOptionPane.showMessageDialog(this, "Failed to delete the file /resources/conf/firstRun.\n"
                        + "Please contact the developers.", "File Error", JOptionPane.OK_OPTION);
            }

            int value = JOptionPane.showConfirmDialog(this,
                    "Create a shortcut to DeNovoGUI on the desktop?",
                    "Create Desktop Shortcut?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                addShortcutAtDeskTop();
            }
        }

        // set the font color for the titlted borders, looks better than the default black
        UIManager.put("TitledBorder.titleColor", new Color(59, 59, 59));

        initComponents();

        // set the default PepNovo folder
        if (new File(getJarFilePath() + "/resources/conf/PepNovo").exists()) {
            pepNovoFolder = new File(getJarFilePath() + "/resources/conf/PepNovo");
        }

        setUpGUI();

        // set the title
        this.setTitle("DeNovoGUI " + getVersion());

        // set the title of the frame and add the icon
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")));

        // Load modifications
        try {
            ptmFactory.importModifications(getModificationsFile(), false);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error while reading " + MODIFICATION_FILE + ".", "Modification File Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            ptmFactory.importModifications(getUserModificationsFile(), true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error while reading " + USER_MODIFICATION_FILE + ".", "Modification File Error", JOptionPane.ERROR_MESSAGE);
        }

        // Load the enzymes
        try {
            enzymeFactory.importEnzymes(new File(ENZYME_FILE));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error while reading " + ENZYME_FILE + ".", "Enzyme File Error", JOptionPane.ERROR_MESSAGE);
        }

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {

        modificationsTable.getColumn("Fixed").setMinWidth(70);
        modificationsTable.getColumn("Fixed").setMaxWidth(70);
        modificationsTable.getColumn("Variable").setMinWidth(70);
        modificationsTable.getColumn("Variable").setMaxWidth(70);

        modificationsTable.getColumn("Fixed").setCellRenderer(new NimbusCheckBoxRenderer());
        modificationsTable.getColumn("Variable").setCellRenderer(new NimbusCheckBoxRenderer());

        // make sure that the scroll panes are see-through
        modificationsTableScrollPane.getViewport().setOpaque(false);

        modificationsTable.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Set up the log file.
     */
    private void setUpLogFile() {
        if (useLogFile && !getJarFilePath().equalsIgnoreCase(".")) {
            try {
                String path = getJarFilePath() + "/resources/DeNovoGUI.log";

                File file = new File(path);
                System.setOut(new java.io.PrintStream(new FileOutputStream(file, true)));
                System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

                // creates a new log file if it does not exist
                if (!file.exists()) {
                    boolean fileCreated = file.createNewFile();

                    if (fileCreated) {
                        FileWriter w = new FileWriter(file);
                        BufferedWriter bw = new BufferedWriter(w);
                        bw.close();
                        w.close();
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to create the file log file.\n"
                                + "Please contact the developers.", "File Error", JOptionPane.OK_OPTION);
                    }
                }
                System.out.println("\n\n" + new Date() + ": DeNovoGUI version " + getVersion() + ".\n");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null, "An error occured when trying to create the DeNovoGUI log file.",
                        "Error Creating Log File", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        inputFilesPanel = new javax.swing.JPanel();
        spectrumFilesLabel = new javax.swing.JLabel();
        spectrumFilesTextField = new javax.swing.JTextField();
        browseSpectrumFilesButton = new javax.swing.JButton();
        clearSpectrumFilesButton = new javax.swing.JButton();
        outputFolderPanel = new javax.swing.JPanel();
        outputFolderLabel = new javax.swing.JLabel();
        outputFolderTextField = new javax.swing.JTextField();
        outputFolderBrowseButton = new javax.swing.JButton();
        searchSettingsPanel = new javax.swing.JPanel();
        enzymeLabel = new javax.swing.JLabel();
        modelComboBox = new javax.swing.JComboBox();
        modelLabel = new javax.swing.JLabel();
        enzymeComboBox = new javax.swing.JComboBox();
        fragmentMassToleranceLabel = new javax.swing.JLabel();
        fragmentMassToleranceSpinner = new javax.swing.JSpinner();
        precursorMassToleranceLabel = new javax.swing.JLabel();
        precursorMassToleranceSpinner = new javax.swing.JSpinner();
        numberOfSolutionsLabel = new javax.swing.JLabel();
        numberOfSolutionsSpinner = new javax.swing.JSpinner();
        spectrumChargeCheckBox = new javax.swing.JCheckBox();
        spectrumPrecursorCheckBox = new javax.swing.JCheckBox();
        filterLowQualityCheckBox = new javax.swing.JCheckBox();
        modificationsPanel = new javax.swing.JPanel();
        modificationsTableScrollPane = new javax.swing.JScrollPane();
        modificationsTable = new javax.swing.JTable();
        searchButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        importResultsMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        modsMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        pepNovoMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        logReportMenu = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("DeNovoGUI");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        inputFilesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Input Files"));
        inputFilesPanel.setOpaque(false);

        spectrumFilesLabel.setText("Spectrum File(s)");

        browseSpectrumFilesButton.setText("Browse");
        browseSpectrumFilesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseSpectrumFilesButtonActionPerformed(evt);
            }
        });

        clearSpectrumFilesButton.setText("Clear");
        clearSpectrumFilesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSpectrumFilesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout inputFilesPanelLayout = new javax.swing.GroupLayout(inputFilesPanel);
        inputFilesPanel.setLayout(inputFilesPanelLayout);
        inputFilesPanelLayout.setHorizontalGroup(
            inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumFilesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumFilesTextField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(browseSpectrumFilesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(clearSpectrumFilesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        inputFilesPanelLayout.setVerticalGroup(
            inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spectrumFilesLabel)
                    .addComponent(spectrumFilesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseSpectrumFilesButton)
                    .addComponent(clearSpectrumFilesButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        outputFolderPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Output Folder"));
        outputFolderPanel.setOpaque(false);

        outputFolderLabel.setText("Output Location");

        outputFolderBrowseButton.setText("Browse");
        outputFolderBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputFolderBrowseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout outputFolderPanelLayout = new javax.swing.GroupLayout(outputFolderPanel);
        outputFolderPanel.setLayout(outputFolderPanelLayout);
        outputFolderPanelLayout.setHorizontalGroup(
            outputFolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputFolderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(outputFolderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputFolderTextField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(outputFolderBrowseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        outputFolderPanelLayout.setVerticalGroup(
            outputFolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputFolderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(outputFolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outputFolderLabel)
                    .addComponent(outputFolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(outputFolderBrowseButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        searchSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Settings"));
        searchSettingsPanel.setOpaque(false);

        enzymeLabel.setText("Enzyme");

        modelComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "CID_IT_TRYP" }));

        modelLabel.setText("Model");

        enzymeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TRYPSIN", "NON_SPECIFIC" }));

        fragmentMassToleranceLabel.setText("Fragment Mass Tolerance");

        fragmentMassToleranceSpinner.setModel(new javax.swing.SpinnerNumberModel(0.5d, 0.0d, 2.0d, 0.1d));

        precursorMassToleranceLabel.setText("Precursor Mass Tolerance");

        precursorMassToleranceSpinner.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.0d, 2.0d, 0.01d));

        numberOfSolutionsLabel.setText("No. Solutions (max. 20)");

        numberOfSolutionsSpinner.setModel(new javax.swing.SpinnerNumberModel(10, 1, 20, 1));

        spectrumChargeCheckBox.setText("Use Spectrum Charge (No Correction)");
        spectrumChargeCheckBox.setIconTextGap(15);
        spectrumChargeCheckBox.setOpaque(false);

        spectrumPrecursorCheckBox.setText("Use Spectrum Precursor m/z (No Correction)");
        spectrumPrecursorCheckBox.setIconTextGap(15);
        spectrumPrecursorCheckBox.setOpaque(false);

        filterLowQualityCheckBox.setText("Filter Low Quality Spectra");
        filterLowQualityCheckBox.setIconTextGap(15);
        filterLowQualityCheckBox.setOpaque(false);

        javax.swing.GroupLayout searchSettingsPanelLayout = new javax.swing.GroupLayout(searchSettingsPanel);
        searchSettingsPanel.setLayout(searchSettingsPanelLayout);
        searchSettingsPanelLayout.setHorizontalGroup(
            searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                        .addComponent(enzymeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(enzymeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                        .addComponent(modelLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(modelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                        .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(filterLowQualityCheckBox)
                            .addComponent(spectrumPrecursorCheckBox)
                            .addComponent(spectrumChargeCheckBox))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, searchSettingsPanelLayout.createSequentialGroup()
                        .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                                .addComponent(numberOfSolutionsLabel)
                                .addGap(52, 52, 52))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, searchSettingsPanelLayout.createSequentialGroup()
                                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(precursorMassToleranceLabel, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(fragmentMassToleranceLabel, javax.swing.GroupLayout.Alignment.LEADING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(precursorMassToleranceSpinner, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fragmentMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(numberOfSolutionsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        searchSettingsPanelLayout.setVerticalGroup(
            searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(enzymeLabel)
                    .addComponent(enzymeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(modelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(modelLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fragmentMassToleranceLabel)
                    .addComponent(fragmentMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(precursorMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(precursorMassToleranceLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numberOfSolutionsLabel)
                    .addComponent(numberOfSolutionsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(spectrumChargeCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumPrecursorCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filterLowQualityCheckBox)
                .addContainerGap(12, Short.MAX_VALUE))
        );

        modificationsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Modifications"));
        modificationsPanel.setOpaque(false);

        modificationsTable.setModel(new ModificationsTableModel());
        modificationsTableScrollPane.setViewportView(modificationsTable);

        javax.swing.GroupLayout modificationsPanelLayout = new javax.swing.GroupLayout(modificationsPanel);
        modificationsPanel.setLayout(modificationsPanelLayout);
        modificationsPanelLayout.setHorizontalGroup(
            modificationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modificationsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modificationsTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)
                .addContainerGap())
        );
        modificationsPanelLayout.setVerticalGroup(
            modificationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modificationsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modificationsTableScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        searchButton.setBackground(new java.awt.Color(0, 153, 0));
        searchButton.setFont(searchButton.getFont().deriveFont(searchButton.getFont().getStyle() | java.awt.Font.BOLD));
        searchButton.setForeground(new java.awt.Color(255, 255, 255));
        searchButton.setText("Start the Search!");
        searchButton.setToolTipText("Click here to start the search");
        searchButton.setEnabled(false);
        searchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(inputFilesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(outputFolderPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addComponent(searchSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(modificationsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(searchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(inputFilesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputFolderPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(modificationsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        importResultsMenuItem.setText("Import Results");
        importResultsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importResultsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(importResultsMenuItem);

        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        modsMenuItem.setText("Modifications");
        modsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modsMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(modsMenuItem);
        editMenu.add(jSeparator1);

        pepNovoMenuItem.setText("PepNovo");
        pepNovoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pepNovoMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(pepNovoMenuItem);

        menuBar.add(editMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        helpMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        helpMenuItem.setMnemonic('H');
        helpMenuItem.setText("Help");
        helpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpMenuItem);
        helpMenu.add(jSeparator17);

        logReportMenu.setMnemonic('B');
        logReportMenu.setText("Bug Report");
        logReportMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logReportMenuActionPerformed(evt);
            }
        });
        helpMenu.add(logReportMenu);
        helpMenu.add(jSeparator16);

        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Close the tool.
     *
     * @param evt
     */
    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        dispose();
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void helpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_helpMenuItemActionPerformed

    /**
     * Open the about dialog.
     *
     * @param evt
     */
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    /**
     * Import existing de novo results.
     *
     * @param evt
     */
    private void importResultsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importResultsMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_importResultsMenuItemActionPerformed

    /**
     * Opens a new bug report dialog.
     *
     * @param evt
     */
    private void logReportMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logReportMenuActionPerformed
        new BugReport(this, lastSelectedFolder, "DeNovoGUI", "denovogui", getVersion(), new File(getJarFilePath() + "/resources/DeNovoGUI.log"));
    }//GEN-LAST:event_logReportMenuActionPerformed

    /**
     * Edit the PepNovo location.
     *
     * @param evt
     */
    private void pepNovoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pepNovoMenuItemActionPerformed
        new PepNovoLocationDialog(this, true);
    }//GEN-LAST:event_pepNovoMenuItemActionPerformed

    /**
     * Opens a file browser to select the spectrum files.
     *
     * @param evt
     */
    private void browseSpectrumFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseSpectrumFilesButtonActionPerformed
        // First check whether a file has already been selected.
        // If so, start from that file's parent.

        File startLocation = new File(lastSelectedFolder);
        List<File> spectrumFiles = getSpectrumFiles();
        if (spectrumFiles.size() > 0) {
            File temp = spectrumFiles.get(0);
            startLocation = temp.getParentFile();
        }
        JFileChooser fc = new JFileChooser(startLocation);
        fc.setFileFilter(new ExtensionFileFilter("mgf", false));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fc.getSelectedFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    File[] currentFiles = files[i].listFiles();
                    for (int k = 0; k < currentFiles.length; k++) {
                        if (fc.getFileFilter().accept(currentFiles[k])) {
                            spectrumFiles.add(currentFiles[k]);
                        }
                    }
                } else {
                    spectrumFiles.add(files[i]);
                }
            }
            spectrumFilesTextField.setText(spectrumFiles.size() + " file(s) selected");
            setSpectrumFiles(spectrumFiles);
            //filename = spectraFiles.get(0).getName();
            // TODO: Set back the progress bar..

        }

        setButtonConfiguration();
    }//GEN-LAST:event_browseSpectrumFilesButtonActionPerformed

    /**
     * Clear the spectrum selection.
     *
     * @param evt
     */
    private void clearSpectrumFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSpectrumFilesButtonActionPerformed
        setSpectrumFiles(new ArrayList<File>());
        spectrumFilesTextField.setText(getSpectrumFiles().size() + " file(s) selected");
        searchButton.setEnabled(false);
    }//GEN-LAST:event_clearSpectrumFilesButtonActionPerformed

    /**
     * Open a file chooser where the output folder can be selected.
     *
     * @param evt
     */
    private void outputFolderBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputFolderBrowseButtonActionPerformed
        // TODO: Setup default start location here!
        File startLocation = new File(lastSelectedFolder);
        if (outputFolderTextField.getText() != null && !outputFolderTextField.getText().trim().equals("")) {
            File temp = new File(outputFolderTextField.getText());
            if (temp.isDirectory()) {
                startLocation = temp;
            } else {
                startLocation = temp.getParentFile();
            }
        }
        JFileChooser fc = new JFileChooser(startLocation);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setMultiSelectionEnabled(false);
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File outputFolder;
            outputFolder = fc.getSelectedFile();
            outputFolderTextField.setText(outputFolder.getAbsolutePath());
            lastSelectedFolder = outputFolder.getAbsolutePath();
            setOutputFolder(outputFolder);
        }

        setButtonConfiguration();
    }//GEN-LAST:event_outputFolderBrowseButtonActionPerformed

    /**
     * Start the de novo sequencing.
     *
     * @param evt
     */
    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed

        waitingDialog = new WaitingDialog(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")), false,
                //null, // @TODO: add tips?
                "De Novo Search",
                "DeNovoGUI",
                new Properties().getVersion(),
                true);
        waitingDialog.setLocationRelativeTo(this);

        startSearch(waitingDialog);

        if (!waitingDialog.isRunCanceled()) {
            try {
                displayResults();
            } catch (Exception e) {
                e.printStackTrace(); // @TODO: better error handling!!
            }
        }
    }//GEN-LAST:event_searchButtonActionPerformed

    /**
     * Edit the modifications.
     *
     * @param evt
     */
    private void modsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modsMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_modsMenuItemActionPerformed

    /**
     * The main methos.
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        // set the look and feel
        boolean numbusLookAndFeelSet = false;
        try {
            numbusLookAndFeelSet = UtilitiesGUIDefaults.setLookAndFeel();
        } catch (Exception e) {
        }

        if (!numbusLookAndFeelSet) {
            JOptionPane.showMessageDialog(null,
                    "Failed to set the default look and feel. Using backup look and feel.\n"
                    + "DeNovoGUI will work but not look as good as it should...", "Look and Feel",
                    JOptionPane.WARNING_MESSAGE);
        }

        new DeNovoGUI();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton browseSpectrumFilesButton;
    private javax.swing.JButton clearSpectrumFilesButton;
    private javax.swing.JMenu editMenu;
    private javax.swing.JComboBox enzymeComboBox;
    private javax.swing.JLabel enzymeLabel;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JCheckBox filterLowQualityCheckBox;
    private javax.swing.JLabel fragmentMassToleranceLabel;
    private javax.swing.JSpinner fragmentMassToleranceSpinner;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JMenuItem importResultsMenuItem;
    private javax.swing.JPanel inputFilesPanel;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JMenuItem logReportMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JComboBox modelComboBox;
    private javax.swing.JLabel modelLabel;
    private javax.swing.JPanel modificationsPanel;
    private javax.swing.JTable modificationsTable;
    private javax.swing.JScrollPane modificationsTableScrollPane;
    private javax.swing.JMenuItem modsMenuItem;
    private javax.swing.JLabel numberOfSolutionsLabel;
    private javax.swing.JSpinner numberOfSolutionsSpinner;
    private javax.swing.JButton outputFolderBrowseButton;
    private javax.swing.JLabel outputFolderLabel;
    private javax.swing.JPanel outputFolderPanel;
    private javax.swing.JTextField outputFolderTextField;
    private javax.swing.JMenuItem pepNovoMenuItem;
    private javax.swing.JLabel precursorMassToleranceLabel;
    private javax.swing.JSpinner precursorMassToleranceSpinner;
    private javax.swing.JButton searchButton;
    private javax.swing.JPanel searchSettingsPanel;
    private javax.swing.JCheckBox spectrumChargeCheckBox;
    private javax.swing.JLabel spectrumFilesLabel;
    private javax.swing.JTextField spectrumFilesTextField;
    private javax.swing.JCheckBox spectrumPrecursorCheckBox;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns the modifications file.
     *
     * @return the modifications file
     */
    public File getModificationsFile() {
        File result = new File(getJarFilePath() + File.separator + MODIFICATION_FILE);

        if (!result.exists()) {
            JOptionPane.showMessageDialog(null, MODIFICATION_FILE + " not found.", "Modification File Error", JOptionPane.ERROR_MESSAGE);
        }
        return result;
    }

    /**
     * Starts the search.
     *
     * @param waitingHandler the waiting handler
     */
    public void startSearch(WaitingHandler waitingHandler) {
        searchWorker = new SearchTask(waitingHandler);
        searchWorker.execute();

        // Display the waiting dialog
        if (waitingHandler != null && waitingHandler instanceof WaitingDialog) {
            ((WaitingDialog) waitingHandler).setVisible(true);
            ((WaitingDialog) waitingHandler).setModal(true);
        } else {
            useCommandLine = true;
        }

        while (useCommandLine && !searchWorker.isFinished()) { // @TODO: is there a better way of doing this?
            // wait
        }

        // check if the background processes are complete (OMSSA, XTandem and PeptideShaker) 
        while (!useCommandLine && waitingHandler != null) {
            if (waitingHandler.isRunCanceled()) {
                cancelSearch(); // cancel the background processes
                break;
            } else if (waitingHandler.isRunFinished()) {
                break; // background processes done, break the loop
            }
        }
    }

    /**
     * Cancel the search.
     */
    public void cancelSearch() {

        if (waitingDialog != null) {
            waitingDialog.appendReportEndLine();
            waitingDialog.appendReport("Search Cancelled.\n", true, true);
        }
        searchWorker.cancel(true);
        if (waitingDialog != null) {
            waitingDialog.setRunCanceled();
        }
    }

    @SuppressWarnings("rawtypes")
    private class SearchTask extends SwingWorker {

        /**
         * The waiting handler.
         */
        private WaitingHandler waitingHandler;
        /**
         * True if the process has finished.
         */
        private boolean finished = false;

        /**
         * Constructor.
         *
         * @param waitingHandler
         */
        public SearchTask(WaitingHandler waitingHandler) {
            this.waitingHandler = waitingHandler;
        }

        protected Object doInBackground() throws Exception {

            try {
                loadSpectra(spectrumFiles);

                searchParameters = getSearchParametersFromGUI();
                searchHandler = new DeNovoSearchHandler(pepNovoFolder);
                searchHandler.startSearch(spectrumFiles, searchParameters, outputFolder, waitingHandler);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 0;
        }

        /**
         * Returns the result when the PepNovo+ has finished.
         */
        public void finished() {
            finished = true;
            if (!waitingHandler.isRunCanceled()) {
                waitingHandler.appendReport("The de novo search has finished.", true, true);
                waitingHandler.setRunFinished();
            }
        }

        /**
         * Returns a boolean indicating whether the searches have finished.
         *
         * @return a boolean indicating whether the searches have finished
         */
        public boolean isFinished() {
            return finished;
        }
    }

    /**
     * Loads the results of the given spectrum files and loads everything in the
     * identification.
     *
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    public void displayResults() throws SQLException, FileNotFoundException, IOException, IllegalArgumentException, ClassNotFoundException, Exception {

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")),
                true);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();


        final DeNovoGUI finalRef = this;

        new Thread("DisplayThread") {
            @Override
            public void run() {

                try {
                    
                    // @TODO: use progress bar to show actual progress

                    ArrayList<File> outputFiles = new ArrayList<File>();
                    for (File file : spectrumFiles) {
                        File resultFile = PepnovoJob.getOutputFile(outputFolder, Util.getFileName(file));
                        if (resultFile.exists()) {
                            outputFiles.add(resultFile);
                        } else {
                            //inputPanel.appendReport("File " + Util.getFileName(file) + " not found.", true, true); // @TODO: re-add me??
                        }
                    }

                    importPepNovoResults(outputFiles);

                    JDialog resultsDialog = new JDialog(finalRef, "De Novo Results", true);
                    resultsDialog.setSize(1200, 800); // @TODO: size should not be hardcoded!!
                    resultsDialog.setLayout(new BorderLayout());
                    ResultsPanel resultsPanel = new ResultsPanel(finalRef);
                    resultsPanel.diplayResults();
                    resultsDialog.add(resultsPanel);

                    progressDialog.setRunFinished();

                    resultsDialog.setLocationRelativeTo(finalRef);
                    resultsDialog.setVisible(true);
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    e.printStackTrace(); // @TODO: add better error handling!!
                }
            }
        }.start();
    }

    /**
     * Returns the user defined modifications file.
     *
     * @return the user defined modifications file
     */
    public File getUserModificationsFile() {
        File result = new File(getJarFilePath() + File.separator + USER_MODIFICATION_FILE);
        if (!result.exists()) {
            JOptionPane.showMessageDialog(null, USER_MODIFICATION_FILE + " not found.", "Modification File Error", JOptionPane.ERROR_MESSAGE);
        }
        return result;
    }

    /**
     * Loads the mgf files in the spectrum factory.
     *
     * @param mgfFiles loads the mgf files in the spectrum factory
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void loadSpectra(List<File> mgfFiles) throws FileNotFoundException, IOException, ClassNotFoundException {
        // Add spectrum files to the spectrum factory
        for (File spectrumFile : mgfFiles) {
            //@TODO: add progress bar
            spectrumFactory.addSpectra(spectrumFile);
        }
    }

    /**
     * Imports the PepNovo results from the given files and puts all matches in
     * the identification
     *
     * @param outFiles the PepNovo result files as a list
     */
    private void importPepNovoResults(ArrayList<File> outFiles) throws SQLException, FileNotFoundException, IOException, IllegalArgumentException, ClassNotFoundException, Exception {

        //@TODO: let the user reference his project

        String projectReference = "project reference";
        String sampleReference = "sample reference";
        int replicateNumber = 0;
        String identificationReference = Identification.getDefaultReference(projectReference, sampleReference, replicateNumber);
        MsExperiment experiment = new MsExperiment(projectReference);
        Sample sample = new Sample(sampleReference);
        SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(replicateNumber));
        experiment.addAnalysisSet(sample, analysisSet);
        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        analysis.addIdentificationResults(IdentificationMethod.MS2_IDENTIFICATION, new Ms2Identification(identificationReference));
        
        // The identification object
        identification = analysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        identification.setIsDB(true);
        
        // The cache used whenever the identification becomes too big
        String dbFolder = new File(getJarFilePath(), CACHE_DIRECTORY).getAbsolutePath();
        ObjectsCache objectsCache = new ObjectsCache();
        objectsCache.setAutomatedMemoryManagement(true);
        identification.establishConnection(dbFolder, true, objectsCache);

        
        // @TODO: use waiting dialog here?
        
        for (File file : outFiles) {
            // initiate the parser
            PepNovoIdfileReader idfileReader = new PepNovoIdfileReader(file);
             
            // put the identification results in the identification object
            identification.addSpectrumMatch(idfileReader.getAllSpectrumMatches(null));
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    protected String getJarFilePath() {
        return DeNovoGUIWrapper.getJarFilePath(this.getClass().getResource("DeNovoGUI.class").getPath(), DeNovoGUIWrapper.toolName);
    }

    /**
     * Ask the user if he/she wants to add a shortcut at the desktop.
     */
    private void addShortcutAtDeskTop() {

        String jarFilePath = getJarFilePath();

        if (!jarFilePath.equalsIgnoreCase(".")) {

            // remove the initial '/' at the start of the line
            if (jarFilePath.startsWith("\\") && !jarFilePath.startsWith("\\\\")) {
                jarFilePath = jarFilePath.substring(1);
            }

            //utilitiesUserPreferences.setDeNovoPath(jarFilePath); // @TODO: add this method to utilities

            String iconFileLocation = jarFilePath + "\\resources\\denovogui.ico";
            String jarFileLocation = jarFilePath + "\\DeNovoGUI-" + new Properties().getVersion() + ".jar";

            try {
                JShellLink link = new JShellLink();
                link.setFolder(JShellLink.getDirectory("desktop"));
                link.setName("DeNovoGUI " + new Properties().getVersion());
                link.setIconLocation(iconFileLocation);
                link.setPath(jarFileLocation);
                link.save();
            } catch (Exception e) {
                System.out.println("An error occurred when trying to create a desktop shortcut...");
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the last selected folder.
     *
     * @return the last selected folder
     */
    public String getLastSelectedFolder() {
        return lastSelectedFolder;
    }

    /**
     * Set the last selected folder.
     *
     * @param lastSelectedFolder the folder to set
     */
    public void setLastSelectedFolder(String lastSelectedFolder) {
        this.lastSelectedFolder = lastSelectedFolder;
    }

    /**
     * Sets the output folder.
     *
     * @param outputFolder Output folder.
     */
    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    /**
     * Sets the PepNovo folder.
     *
     * @param pepNovoFolder PepNovo folder.
     */
    public void setPepNovoFolder(File pepNovoFolder) {
        this.pepNovoFolder = pepNovoFolder;
    }

    /**
     * Returns the PepNovo folder.
     *
     * @return the pepNovoFolder
     */
    public File getPepNovoFolder() {
        return pepNovoFolder;
    }

    /**
     * Returns the identification containing all results.
     *
     * @return the identification
     */
    public Identification getIdentification() {
        return identification;
    }

    /**
     * Returns the selected spectrum files.
     *
     * @return The selected spectrum files.
     */
    public List<File> getSpectrumFiles() {
        return spectrumFiles;
    }

    /**
     * Sets the selected spectrum file.
     *
     * @param spectrumFiles Spectrum files.
     */
    public void setSpectrumFiles(List<File> spectrumFiles) {
        this.spectrumFiles = spectrumFiles;
    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of PeptideShaker
     */
    public String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("denovogui.properties");
            p.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return p.getProperty("denovogui.version");
    }

    /**
     * Return the color used for the sparklines.
     *
     * @return the sparklineColor
     */
    public Color getSparklineColor() {
        return sparklineColor;
    }

    /**
     * Returns the label width for the sparklines.
     *
     * @return the labelWidth
     */
    public int getLabelWidth() {
        return labelWidth;
    }

    /**
     * Returns the current search parameters.
     *
     * @return the searchParameters
     */
    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    /**
     * This method checks for the folder being the PepNovo folder.
     *
     * @param deNovoFolder the folder to check
     * @return boolean to show whether the PepNovo folder is correct
     */
    public boolean checkPepNovoFolder(File deNovoFolder) {

        boolean result = false;

        if (deNovoFolder != null && deNovoFolder.exists() && deNovoFolder.isDirectory()) {
            String[] fileNames = deNovoFolder.list();
            int count = 0;
            for (int i = 0; i < fileNames.length; i++) {
                String lFileName = fileNames[i];
                if (lFileName.startsWith("PepNovo") && lFileName.endsWith(".exe")) {
                    count++;
                }
            }
            if (count > 0) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Table model for the modifications table.
     */
    private class ModificationsTableModel extends DefaultTableModel {

        /**
         * List of all modifications
         */
        private ArrayList<String> modifications = null;
        /**
         * Map of the fixed modifications
         */
        private HashMap<String, Boolean> fixedModifications;
        /**
         * Map of the variable modifications
         */
        private HashMap<String, Boolean> variableModifications;

        /**
         * Constructor
         */
        public ModificationsTableModel() {
            modifications = ptmFactory.getPTMs();
            Collections.sort(modifications);
            fixedModifications = new HashMap<String, Boolean>();
            variableModifications = new HashMap<String, Boolean>();
            for (String modificationName : modifications) {
                fixedModifications.put(modificationName, false);
                variableModifications.put(modificationName, false);
            }
        }

        @Override
        public int getRowCount() {
            if (modifications == null) {
                return 0;
            }
            return modifications.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Fixed";
                case 2:
                    return "Variable";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            String modificationName = modifications.get(row);
            switch (column) {
                case 0:
                    return modificationName;
                case 1:
                    return fixedModifications.get(modificationName);
                case 2:
                    return variableModifications.get(modificationName);
                default:
                    return "";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            for (int i = 0; i < getRowCount(); i++) {
                if (getValueAt(i, columnIndex) != null) {
                    return getValueAt(i, columnIndex).getClass();
                }
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1 || columnIndex == 2;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                String modificationName = modifications.get(rowIndex);
                Boolean value = (Boolean) aValue;
                fixedModifications.put(modificationName, value);
                if (value) {
                    variableModifications.put(modificationName, false);
                }
            } else if (columnIndex == 2) {
                String modificationName = modifications.get(rowIndex);
                Boolean value = (Boolean) aValue;
                variableModifications.put(modificationName, value);
                if (value) {
                    fixedModifications.put(modificationName, false);
                }
            }
        }
    }

    /**
     * Returns the search parameters as set in the GUI.
     *
     * @return the search parameters as set in the GUI
     */
    public SearchParameters getSearchParametersFromGUI() {
        
        SearchParameters tempSearchParameters = new SearchParameters();

        // @TODO: implement other enzymes
        Enzyme enzyme;
        if (enzymeComboBox.getSelectedIndex() == 0) {
            enzyme = enzymeFactory.getEnzyme("Trypsin");
        } else {
            enzyme = enzymeFactory.getEnzyme("No enzyme");
        }
        tempSearchParameters.setEnzyme(enzyme);
        
        // @TODO: implement other models
        String fragmentationModel = (String) modelComboBox.getSelectedItem();
        tempSearchParameters.setFragmentationModel(fragmentationModel);
        double fragmentIonTolerance = (Double) fragmentMassToleranceSpinner.getValue();
        tempSearchParameters.setFragmentIonAccuracy(fragmentIonTolerance);
        double precursorIonTolerance = (Double) precursorMassToleranceSpinner.getValue();
        tempSearchParameters.setPrecursorAccuracy(precursorIonTolerance);
        int maxHitListLength = (Integer) numberOfSolutionsSpinner.getValue();
        tempSearchParameters.setHitListLength(maxHitListLength);
        boolean estimateCharge = !spectrumChargeCheckBox.isSelected();
        tempSearchParameters.setEstimateCharge(estimateCharge);
        boolean estimatePrecursorMass = !spectrumPrecursorCheckBox.isSelected();
        tempSearchParameters.correctPrecursorMass(estimatePrecursorMass);
        boolean filterLowQualitySpectra = filterLowQualityCheckBox.isSelected();
        tempSearchParameters.setDiscardLowQualitySpectra(filterLowQualitySpectra);
        
        ModificationProfile modificationProfile = tempSearchParameters.getModificationProfile();
        for (int row = 0; row < modificationsTable.getRowCount(); row++) {
            if ((Boolean) modificationsTable.getValueAt(row, 1)) {
                String modName = (String) modificationsTable.getValueAt(row, 0);
                PTM ptm = ptmFactory.getPTM(modName);
                modificationProfile.addFixedModification(ptm);
            } else if ((Boolean) modificationsTable.getValueAt(row, 2)) {
                String modName = (String) modificationsTable.getValueAt(row, 0);
                PTM ptm = ptmFactory.getPTM(modName);
                modificationProfile.addVariableModification(ptm);
            }
        }
        return tempSearchParameters;
    }

    /**
     * This method sets the button configuration.
     */
    public void setButtonConfiguration() {
        if (checkForValidConfiguration()) {
            searchButton.setEnabled(true);
        } else {
            searchButton.setEnabled(false);
        }
    }

    /**
     * This method checks whether the start configuration is valid or not.
     *
     * @return Boolean if the start configuration is valid.
     */
    public boolean checkForValidConfiguration() {
        boolean appFolderValid = checkPepNovoFolder(pepNovoFolder);
        return (appFolderValid && getSpectrumFiles().size() > 0 && outputFolderTextField.getText().length() > 0);
    }
}
