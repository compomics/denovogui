package com.compomics.denovogui.gui;

import com.compomics.denovogui.DeNovoSequencingHandler;
import com.compomics.denovogui.DeNovoGUIWrapper;
import com.compomics.denovogui.util.ExtensionFileFilter;
import com.compomics.denovogui.util.Properties;
import com.compomics.software.CommandLineUtils;
import com.compomics.software.CompomicsWrapper;
import com.compomics.software.ToolFactory;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.general.ExceptionHandler;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.ptm.ModificationsDialog;
import com.compomics.util.gui.ptm.PtmDialogParent;
import com.compomics.util.gui.waiting.WaitingActionListener;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
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
import java.util.Date;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import net.jimmc.jshortcut.JShellLink;

/**
 * The main DeNovoGUI frame.
 *
 * @author Thilo Muth
 * @author Harald Barsnes
 */
public class DeNovoGUI extends javax.swing.JFrame implements PtmDialogParent {

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
     * The search handler.
     */
    private DeNovoSequencingHandler deNovoSequencingHandler;
    /**
     * The search parameters.
     */
    private SearchParameters searchParameters = null;
    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "resources/example_dataset";
    /**
     * The example dataset.
     */
    private final String exampleDataset = "resources/example_dataset/Arabidopsis_P1_Top5CID_01.mgf";
    /**
     * The example dataset.
     */
    private final String exampleSearchParams = "resources/example_dataset/denovoGUI_example.parameters";
    /**
     * /**
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
    private ArrayList<File> spectrumFiles = new ArrayList<File>();
    /**
     * The dialog displayed during the search.
     */
    private WaitingDialog waitingDialog;
    /**
     * The search task.
     */
    private SequencingWorker sequencingWorker;
    /**
     * The text to display when default settings are loaded.
     */
    public static final String defaultSettingsTxt = "[default]";
    /**
     * The text to display when user defined settings are loaded.
     */
    public static final String userSettingsTxt = "[user settings]";
    /**
     * The parameter file.
     */
    private File parametersFile = null;
    /**
     * The list of the default modifications.
     */
    private ArrayList<String> modificationUse = new ArrayList<String>();
    /**
     * Reference for the separation of modifications.
     */
    public static final String MODIFICATION_SEPARATOR = "//";
    /**
     * The exception handler.
     */
    private ExceptionHandler exceptionHandler = new ExceptionHandler(this);
    /**
     * Title of the PepNovo executable.
     */
    private String exeTitle = "PepNovo_Windows.exe"; // @TODO: should only be set once!!!

    /**
     * Creates a new DeNovoGUI.
     *
     * @param spectrumFiles the spectrum files (can be null)
     * @param searchParameters the search parameters (can be null)
     * @param outputFolder the output folder (can be null)
     */
    public DeNovoGUI(ArrayList<File> spectrumFiles, SearchParameters searchParameters, File outputFolder) {

        // check for new version
        CompomicsWrapper.checkForNewVersion(getVersion(), "DeNovoGUI", "denovogui");

        // set up the ErrorLog
        setUpLogFile();
        String osName = System.getProperty("os.name").toLowerCase();

        // add desktop shortcut?
        if (!getJarFilePath().equalsIgnoreCase(".")
                && osName.lastIndexOf("windows") != -1
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
        if (new File(getJarFilePath() + "/resources/PepNovo").exists()) {
            pepNovoFolder = new File(getJarFilePath() + "/resources/PepNovo");

            // OS check
            if (osName.contains("mac os")) {
                exeTitle = "PepNovo_Mac";
            } else if (osName.contains("windows")) {
                exeTitle = "PepNovo_Windows.exe";
            } else if (osName.indexOf("nix") != -1 || osName.indexOf("nux") != -1) {
                exeTitle = "PepNovo_Linux";
            } else {
                // unsupported OS version
            }
        }

        deNovoSequencingHandler = new DeNovoSequencingHandler(pepNovoFolder);

        setUpGUI();

        // set the title
        this.setTitle("DeNovoGUI " + getVersion());

        // set the title of the frame and add the icon
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")));

        // load modifications
        try {
            ptmFactory.importModifications(new File(DeNovoSequencingHandler.MODIFICATION_FILE), false);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error while reading " + DeNovoSequencingHandler.MODIFICATION_FILE + ".", "Modification File Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            ptmFactory.importModifications(new File(DeNovoSequencingHandler.USER_MODIFICATION_FILE), true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error while reading " + DeNovoSequencingHandler.USER_MODIFICATION_FILE + ".", "Modification File Error", JOptionPane.ERROR_MESSAGE);
        }

        // load the enzymes
        try {
            enzymeFactory.importEnzymes(new File(DeNovoSequencingHandler.ENZYME_FILE));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error while reading " + DeNovoSequencingHandler.ENZYME_FILE + ".", "Enzyme File Error", JOptionPane.ERROR_MESSAGE);
        }

        if (searchParameters == null) {
            searchParameters = new SearchParameters();
            searchParameters.setHitListLength(20); // @TODO: should not be hardcoded here, but default is 25, and de novo max is 20...
            searchParameters.setPrecursorAccuracy(1.0); // @TODO: should not be hardcoded here, but default is 10, and de novo max is 5...
            setDefaultParameters(); // label the configs as default
        } else {
            loadModifications(searchParameters);
            settingsFileJTextField.setText(searchParameters.getParametersFile().getName());
        }

        loadModificationUse(deNovoSequencingHandler.loadModificationsUse());

        // set the default enzyme to trypsin
        if (searchParameters.getEnzyme() == null) {
            searchParameters.setEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
        }

        this.searchParameters = searchParameters;

        // set the results folder
        if (outputFolder != null && outputFolder.exists()) {
            setOutputFolder(outputFolder);
        }

        // set the spectrum files
        if (spectrumFiles != null) {
            setSpectrumFiles(spectrumFiles);
        }

        setLocationRelativeTo(null);
        setVisible(true);

        validateInput(false);
        this.searchParameters = searchParameters;
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {
        // @TODO: should there be anything here?
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
        searchEnginesPanel = new javax.swing.JPanel();
        enablePepNovoJCheckBox = new javax.swing.JCheckBox();
        pepNovoLinkLabel = new javax.swing.JLabel();
        pepNovoButton = new javax.swing.JButton();
        startButton = new javax.swing.JButton();
        inputFilesPanel1 = new javax.swing.JPanel();
        spectraFilesLabel = new javax.swing.JLabel();
        clearSpectraButton = new javax.swing.JButton();
        addSpectraButton = new javax.swing.JButton();
        spectrumFilesTextField = new javax.swing.JTextField();
        configurationFileLbl = new javax.swing.JLabel();
        settingsFileJTextField = new javax.swing.JTextField();
        viewConfigurationsButton = new javax.swing.JButton();
        loadConfigurationsButton = new javax.swing.JButton();
        resultFolderLbl = new javax.swing.JLabel();
        outputFolderTextField = new javax.swing.JTextField();
        resultFolderBrowseButton = new javax.swing.JButton();
        aboutButton = new javax.swing.JButton();
        deNovoGuiWebPageJLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        loadExampleMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        settingsMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
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
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        searchEnginesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Sequencing Methods"));
        searchEnginesPanel.setOpaque(false);

        enablePepNovoJCheckBox.setSelected(true);
        enablePepNovoJCheckBox.setToolTipText("Enable PepNovo+");
        enablePepNovoJCheckBox.setOpaque(false);
        enablePepNovoJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enablePepNovoJCheckBoxActionPerformed(evt);
            }
        });

        pepNovoLinkLabel.setText("<html>De Novo Peptide Sequencing via Probabilistic Network Modeling - <a href=\"http://proteomics.ucsd.edu/Software/PepNovo.html\">PepNovo web page</a></html> ");
        pepNovoLinkLabel.setToolTipText("Open the OMSSA web page");
        pepNovoLinkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                pepNovoLinkLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pepNovoLinkLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pepNovoLinkLabelMouseExited(evt);
            }
        });

        pepNovoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pepnovo.png"))); // NOI18N
        pepNovoButton.setToolTipText("Open the PepNovo+ web page");
        pepNovoButton.setBorder(null);
        pepNovoButton.setBorderPainted(false);
        pepNovoButton.setContentAreaFilled(false);
        pepNovoButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pepNovoButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pepNovoButtonMouseExited(evt);
            }
        });
        pepNovoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pepNovoButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout searchEnginesPanelLayout = new javax.swing.GroupLayout(searchEnginesPanel);
        searchEnginesPanel.setLayout(searchEnginesPanelLayout);
        searchEnginesPanelLayout.setHorizontalGroup(
            searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchEnginesPanelLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(enablePepNovoJCheckBox)
                .addGap(71, 71, 71)
                .addComponent(pepNovoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addComponent(pepNovoLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(44, Short.MAX_VALUE))
        );
        searchEnginesPanelLayout.setVerticalGroup(
            searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchEnginesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(enablePepNovoJCheckBox)
                    .addComponent(pepNovoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pepNovoLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        startButton.setBackground(new java.awt.Color(0, 153, 0));
        startButton.setFont(startButton.getFont().deriveFont(startButton.getFont().getStyle() | java.awt.Font.BOLD));
        startButton.setForeground(new java.awt.Color(255, 255, 255));
        startButton.setText("Start Sequencing!");
        startButton.setToolTipText("Click here to start sequencing");
        startButton.setEnabled(false);
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        inputFilesPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Input & Output"));
        inputFilesPanel1.setOpaque(false);

        spectraFilesLabel.setText("Spectrum File(s)");

        clearSpectraButton.setText("Clear");
        clearSpectraButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSpectraButtonActionPerformed(evt);
            }
        });

        addSpectraButton.setText("Add");
        addSpectraButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSpectraButtonActionPerformed(evt);
            }
        });

        spectrumFilesTextField.setEditable(false);
        spectrumFilesTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        configurationFileLbl.setText("Settings File");

        settingsFileJTextField.setEditable(false);
        settingsFileJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        viewConfigurationsButton.setText("Edit");
        viewConfigurationsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewConfigurationsButtonActionPerformed(evt);
            }
        });

        loadConfigurationsButton.setText("Load");
        loadConfigurationsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadConfigurationsButtonActionPerformed(evt);
            }
        });

        resultFolderLbl.setText("Output Folder");

        outputFolderTextField.setEditable(false);
        outputFolderTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        resultFolderBrowseButton.setText("Browse");
        resultFolderBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resultFolderBrowseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout inputFilesPanel1Layout = new javax.swing.GroupLayout(inputFilesPanel1);
        inputFilesPanel1.setLayout(inputFilesPanel1Layout);
        inputFilesPanel1Layout.setHorizontalGroup(
            inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputFilesPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(inputFilesPanel1Layout.createSequentialGroup()
                        .addGroup(inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(configurationFileLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(resultFolderLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(outputFolderTextField)
                            .addComponent(settingsFileJTextField))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(viewConfigurationsButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(resultFolderBrowseButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(inputFilesPanel1Layout.createSequentialGroup()
                        .addComponent(spectraFilesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spectrumFilesTextField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(addSpectraButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(clearSpectraButton, javax.swing.GroupLayout.DEFAULT_SIZE, 70, Short.MAX_VALUE)
                    .addComponent(loadConfigurationsButton, javax.swing.GroupLayout.DEFAULT_SIZE, 70, Short.MAX_VALUE))
                .addContainerGap())
        );

        inputFilesPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addSpectraButton, clearSpectraButton, loadConfigurationsButton, resultFolderBrowseButton, viewConfigurationsButton});

        inputFilesPanel1Layout.setVerticalGroup(
            inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputFilesPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spectraFilesLabel)
                    .addComponent(spectrumFilesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearSpectraButton)
                    .addComponent(addSpectraButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(configurationFileLbl)
                    .addComponent(settingsFileJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(viewConfigurationsButton)
                    .addComponent(loadConfigurationsButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resultFolderLbl)
                    .addComponent(resultFolderBrowseButton)
                    .addComponent(outputFolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        aboutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/denovogui_shadow.png"))); // NOI18N
        aboutButton.setToolTipText("Open the DeNovoGUI web page");
        aboutButton.setBorder(null);
        aboutButton.setBorderPainted(false);
        aboutButton.setContentAreaFilled(false);
        aboutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                aboutButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                aboutButtonMouseExited(evt);
            }
        });
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });

        deNovoGuiWebPageJLabel.setForeground(new java.awt.Color(0, 0, 255));
        deNovoGuiWebPageJLabel.setText("<html><u><i>For additional information see http://denovogui.googlecode.com</i></u></html>");
        deNovoGuiWebPageJLabel.setToolTipText("Open the DeNovoGUI web page");
        deNovoGuiWebPageJLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                deNovoGuiWebPageJLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                deNovoGuiWebPageJLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                deNovoGuiWebPageJLabelMouseExited(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(searchEnginesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(inputFilesPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(aboutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(45, 45, 45)
                        .addComponent(deNovoGuiWebPageJLabel)
                        .addGap(18, 18, 18)
                        .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(21, 21, 21))))
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchEnginesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inputFilesPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deNovoGuiWebPageJLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aboutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        openMenuItem.setText("Open Results...");
        openMenuItem.setToolTipText("Open existing de novo results");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);
        fileMenu.add(jSeparator4);

        loadExampleMenuItem.setText("Load Example");
        loadExampleMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadExampleMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(loadExampleMenuItem);
        fileMenu.add(jSeparator3);

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

        settingsMenuItem.setMnemonic('S');
        settingsMenuItem.setText("Settings");
        settingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(settingsMenuItem);
        editMenu.add(jSeparator2);

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
        new HelpDialog(this, getClass().getResource("/html/DeNovoGUI.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.pgn")),
                "DeNovoGUI - Help", 700, 10);
    }//GEN-LAST:event_helpMenuItemActionPerformed

    /**
     * Open the about dialog.
     *
     * @param evt
     */
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/html/AboutDeNovoGUI.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.pgn")),
                "About DeNovoGUI", 700, 10);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

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
     * Start the de novo sequencing.
     *
     * @param evt
     */
    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed

        // no search parameters set, use the defaults
        if (searchParameters == null) {
            searchParameters = new SearchParameters();
        }

        saveConfigurationFile(); // save the ptms usage

        waitingDialog = new WaitingDialog(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")), false,
                //null, // @TODO: add tips?
                "De Novo Sequencing",
                "DeNovoGUI",
                new Properties().getVersion(),
                true);
        waitingDialog.addWaitingActionListener(new WaitingActionListener() {
            @Override
            public void cancelPressed() {
                cancelSequencing();
            }
        });
        waitingDialog.setCloseDialogWhenImportCompletes(true, true);
        waitingDialog.setLocationRelativeTo(this);
        startSequencing(waitingDialog);

    }//GEN-LAST:event_startButtonActionPerformed

    /**
     * Edit the modifications.
     *
     * @param evt
     */
    private void modsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modsMenuItemActionPerformed
        new ModificationsDialog(this, this, true);
    }//GEN-LAST:event_modsMenuItemActionPerformed

    /**
     * Enable/disable PepNovo.
     *
     * @param evt
     */
    private void enablePepNovoJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enablePepNovoJCheckBoxActionPerformed

        // @TODO: add testsing of the PepNovo installation

        validateInput(false);
    }//GEN-LAST:event_enablePepNovoJCheckBoxActionPerformed

    /**
     * Open the PepNovo web page.
     *
     * @param evt
     */
    private void pepNovoLinkLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepNovoLinkLabelMouseClicked
        pepNovoButtonActionPerformed(null);
    }//GEN-LAST:event_pepNovoLinkLabelMouseClicked

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void pepNovoLinkLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepNovoLinkLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pepNovoLinkLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void pepNovoLinkLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepNovoLinkLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pepNovoLinkLabelMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void pepNovoButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepNovoButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pepNovoButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void pepNovoButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepNovoButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pepNovoButtonMouseExited

    /**
     * Open the PepNovo web page.
     *
     * @param evt
     */
    private void pepNovoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pepNovoButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://proteomics.ucsd.edu/Software/PepNovo.html");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pepNovoButtonActionPerformed

    /**
     * Clear the spectrum selection.
     *
     * @param evt
     */
    private void clearSpectraButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSpectraButtonActionPerformed
        setSpectrumFiles(new ArrayList<File>());
        spectrumFilesTextField.setText(getSpectrumFiles().size() + " file(s) selected");
        validateInput(false);
    }//GEN-LAST:event_clearSpectraButtonActionPerformed

    /**
     * Opens a file browser to select the spectrum files.
     *
     * @param evt
     */
    private void addSpectraButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSpectraButtonActionPerformed
        // First check whether a file has already been selected.
        // If so, start from that file's parent.

        File startLocation = new File(lastSelectedFolder);
        ArrayList<File> tempSpectrumFiles = getSpectrumFiles();
        if (tempSpectrumFiles.size() > 0) {
            File temp = tempSpectrumFiles.get(0);
            startLocation = temp.getParentFile();
        }
        JFileChooser fc = new JFileChooser(startLocation);
        fc.setFileFilter(new ExtensionFileFilter("mgf", false));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder = fc.getSelectedFile().getAbsolutePath();

            File[] files = fc.getSelectedFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    File[] currentFiles = files[i].listFiles();
                    for (int k = 0; k < currentFiles.length; k++) {
                        if (fc.getFileFilter().accept(currentFiles[k])) {
                            tempSpectrumFiles.add(currentFiles[k]);
                        }
                    }
                } else {
                    tempSpectrumFiles.add(files[i]);
                }
            }
            spectrumFilesTextField.setText(tempSpectrumFiles.size() + " file(s) selected");
            setSpectrumFiles(tempSpectrumFiles);
            //filename = spectraFiles.get(0).getName();
            // @TODO: re-add back the progress bar..?
        }

        validateInput(false);
    }//GEN-LAST:event_addSpectraButtonActionPerformed

    /**
     * Open the settings dialog.
     *
     * @param evt
     */
    private void viewConfigurationsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewConfigurationsButtonActionPerformed
        new SettingsDialog(this, searchParameters, true, true);
    }//GEN-LAST:event_viewConfigurationsButtonActionPerformed

    /**
     * Load search parameters from a file.
     *
     * @param evt
     */
    private void loadConfigurationsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadConfigurationsButtonActionPerformed

        // First check whether a file has already been selected.
        // If so, start from that file's parent.
        File startLocation = new File(lastSelectedFolder);
        if (searchParameters.getParametersFile() != null && !settingsFileJTextField.getText().trim().equals("")
                && !settingsFileJTextField.getText().trim().equals(defaultSettingsTxt)
                && !settingsFileJTextField.getText().trim().equals(userSettingsTxt)) {
            startLocation = searchParameters.getParametersFile().getParentFile();
        }
        JFileChooser fc = new JFileChooser(startLocation);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".properties")
                        || myFile.getName().toLowerCase().endsWith(".parameters")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "DeNovoGUI sequencing parameters";
            }
        };
        fc.setFileFilter(filter);
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            lastSelectedFolder = file.getAbsolutePath();
            try {
                searchParameters = SearchParameters.getIdentificationParameters(file);
                loadModifications(searchParameters);
                parametersFile = file;
                searchParameters.setParametersFile(parametersFile);
                settingsFileJTextField.setText(parametersFile.getName());

                SettingsDialog settingsDialog = new SettingsDialog(this, searchParameters, false, true);
                boolean valid = settingsDialog.validateParametersInput(false);

                if (!valid) {
                    settingsDialog.validateParametersInput(true);
                    settingsDialog.setVisible(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error occured while reading " + file + ". Please verify the de novo parameters.", "File Error", JOptionPane.ERROR_MESSAGE);
                SettingsDialog settingsDialog = new SettingsDialog(this, searchParameters, false, true);
                settingsDialog.setVisible(true);
            }
        }

        validateInput(false);
    }//GEN-LAST:event_loadConfigurationsButtonActionPerformed

    /**
     * Open a file chooser where the output folder can be selected.
     *
     * @param evt
     */
    private void resultFolderBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resultFolderBrowseButtonActionPerformed

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
            File tempOutputFolder = fc.getSelectedFile();
            outputFolderTextField.setText(tempOutputFolder.getAbsolutePath());
            lastSelectedFolder = tempOutputFolder.getAbsolutePath();
            setOutputFolder(tempOutputFolder);
        }

        validateInput(false);
    }//GEN-LAST:event_resultFolderBrowseButtonActionPerformed

    /**
     * Edit the search settings.
     *
     * @param evt
     */
    private void settingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsMenuItemActionPerformed
        new SettingsDialog(this, searchParameters, true, true);
    }//GEN-LAST:event_settingsMenuItemActionPerformed

    /**
     * Changes the cursor back to a hand cursor.
     *
     * @param evt
     */
    private void aboutButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void aboutButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseExited

    /**
     * Open the DeNovoGUI web page.
     *
     * @param evt
     */
    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://denovogui.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonActionPerformed

    /**
     * Open the DeNovoGUI web page.
     *
     * @param evt
     */
    private void deNovoGuiWebPageJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoGuiWebPageJLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://denovogui.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_deNovoGuiWebPageJLabelMouseClicked

    /**
     * Changes the cursor into a hand cursor.
     *
     * @param evt
     */
    private void deNovoGuiWebPageJLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoGuiWebPageJLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_deNovoGuiWebPageJLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void deNovoGuiWebPageJLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoGuiWebPageJLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_deNovoGuiWebPageJLabelMouseExited

    /**
     * Open the results.
     * 
     * @param evt 
     */
    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        new ResultsFrame(this, null, searchParameters);
    }//GEN-LAST:event_openMenuItemActionPerformed

    /**
     * Load the example dataset.
     * 
     * @param evt 
     */
    private void loadExampleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadExampleMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/html/ExampleDataset.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.pgn")),
                "About DeNovoGUI Example Dataset", 700, 10);
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
        fc.setDialogTitle("Please select an output folder");
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File tempOutputFolder = fc.getSelectedFile();
            outputFolderTextField.setText(tempOutputFolder.getAbsolutePath());
            lastSelectedFolder = tempOutputFolder.getAbsolutePath();
            setOutputFolder(tempOutputFolder);
            // Set default example dataset
            spectrumFilesTextField.setText("Example dataset selected");
            ArrayList<File> exampleFile = new ArrayList<File>();
            exampleFile.add(new File(getJarFilePath(), exampleDataset));
            setSpectrumFiles(exampleFile);
            // Set default search parameters
            try {
                File exampleParametersFile = new File(getJarFilePath(), exampleSearchParams);
                searchParameters = SearchParameters.getIdentificationParameters(exampleParametersFile);
                loadModifications(searchParameters);
                parametersFile = exampleParametersFile;
                searchParameters.setParametersFile(exampleParametersFile);
                settingsFileJTextField.setText(parametersFile.getName());

                SettingsDialog settingsDialog = new SettingsDialog(this, searchParameters, false, true);
                boolean valid = settingsDialog.validateParametersInput(false);

                if (!valid) {
                    settingsDialog.validateParametersInput(true);
                    settingsDialog.setVisible(true);
                }

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error occured while reading " + parametersFile + ". Please verify the de novo parameters.", "File Error", JOptionPane.ERROR_MESSAGE);
                SettingsDialog settingsDialog = new SettingsDialog(this, searchParameters, false, true);
                settingsDialog.setVisible(true);
            }

            validateInput(false);
        }
    }//GEN-LAST:event_loadExampleMenuItemActionPerformed

    /**
     * The main method.
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

        ArrayList<File> spectrumFiles = null;
        SearchParameters searchParameters = null;
        File outputFolder = null;
        boolean spectrum = false, parameters = false, output = false;

        for (String arg : args) {
            if (spectrum) {
                try {
                    ArrayList<String> extensions = new ArrayList<String>();
                    extensions.add(".mgf");
                    spectrumFiles = CommandLineUtils.getFiles(arg, extensions);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "Failed importing spectrum files from command line option " + arg + ".", "Spectrum files",
                            JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                }
                spectrum = false;
            }
            if (parameters) {
                File searchParametersFile = new File(arg);
                try {
                    searchParameters = SearchParameters.getIdentificationParameters(searchParametersFile);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "Failed to import search parameters from: " + searchParametersFile.getAbsolutePath() + ".", "Search Parameters",
                            JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                }
                parameters = false;
            }
            if (output) {
                outputFolder = new File(arg);
            }
            if (arg.equals(ToolFactory.searchGuiSpectrumFileOption)) { // @TODO: generalize the option names
                spectrum = true;
            }
            if (arg.equals(ToolFactory.searchGuiParametersFileOption)) { // @TODO: generalize the option names
                parameters = true;
            }
            if (arg.equals(ToolFactory.outputFolderOption)) {
                parameters = true;
            }
        }

        new DeNovoGUI(spectrumFiles, searchParameters, outputFolder);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton addSpectraButton;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton clearSpectraButton;
    private javax.swing.JLabel configurationFileLbl;
    private javax.swing.JLabel deNovoGuiWebPageJLabel;
    private javax.swing.JMenu editMenu;
    private javax.swing.JCheckBox enablePepNovoJCheckBox;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JPanel inputFilesPanel1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JButton loadConfigurationsButton;
    private javax.swing.JMenuItem loadExampleMenuItem;
    private javax.swing.JMenuItem logReportMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem modsMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JTextField outputFolderTextField;
    private javax.swing.JButton pepNovoButton;
    private javax.swing.JLabel pepNovoLinkLabel;
    private javax.swing.JMenuItem pepNovoMenuItem;
    private javax.swing.JButton resultFolderBrowseButton;
    private javax.swing.JLabel resultFolderLbl;
    private javax.swing.JPanel searchEnginesPanel;
    private javax.swing.JTextField settingsFileJTextField;
    private javax.swing.JMenuItem settingsMenuItem;
    private javax.swing.JLabel spectraFilesLabel;
    private javax.swing.JTextField spectrumFilesTextField;
    private javax.swing.JButton startButton;
    private javax.swing.JButton viewConfigurationsButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Starts the search.
     *
     * @param waitingHandler the waiting handler
     */
    public void startSequencing(WaitingHandler waitingHandler) {

        sequencingWorker = new SequencingWorker(waitingHandler);
        sequencingWorker.execute();

        // Display the waiting dialog
        if (waitingHandler != null && waitingHandler instanceof WaitingDialog) {
            ((WaitingDialog) waitingHandler).setVisible(true);
            ((WaitingDialog) waitingHandler).setModal(true);
        }
    }

    /**
     * Cancel the sequencing.
     */
    public void cancelSequencing() {

        if (waitingDialog != null) {
            waitingDialog.appendReportEndLine();
            waitingDialog.appendReport("Sequencing Cancelled.\n", true, true);
        }
        if (sequencingWorker != null) {
            sequencingWorker.cancel(true);
        }
        if (waitingDialog != null) {
            waitingDialog.setRunCanceled();
        }
        if (deNovoSequencingHandler != null) {
            try {
                deNovoSequencingHandler.cancelSequencing();
            } catch (Exception e) {
                catchException(e);
            }
        }
    }

    @Override
    public void updateModifications() {
        // do nothing
    }

    /**
     * Returns a reference to the DeNovoGUI command line interface.
     *
     * @return a reference to the DeNovoGUI command line interface
     */
    public DeNovoSequencingHandler getDeNovoSequencingHandler() {
        return deNovoSequencingHandler;
    }

    @SuppressWarnings("rawtypes")
    private class SequencingWorker extends SwingWorker {

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
        public SequencingWorker(WaitingHandler waitingHandler) {
            this.waitingHandler = waitingHandler;
        }

        @Override
        protected Object doInBackground() throws Exception {

            waitingHandler.appendReport("Starting DeNovoGUI.", true, true);
            waitingHandler.appendReportEndLine();

            try {
                waitingHandler.appendReport("Loading the spectra.", true, true);
                loadSpectra(spectrumFiles);
                waitingHandler.appendReport("Done loading the spectra.", true, true);
                waitingHandler.appendReportEndLine();
                deNovoSequencingHandler.startSequencing(spectrumFiles, searchParameters, outputFolder, exeTitle, waitingHandler);
            } catch (Exception e) {
                catchException(e);
            }
            return 0;
        }

        @Override
        protected void done() {
            finished = true;

            if (!waitingHandler.isRunCanceled()) {
                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("The de novo sequencing is complete.", true, true);
                waitingHandler.setRunFinished();

                try {
                    displayResults();
                } catch (Exception e) {
                    catchException(e);
                }
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
        ArrayList<File> outFiles = FileProcessor.getOutFiles(outputFolder, spectrumFiles);
        new ResultsFrame(this, outFiles, searchParameters);
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
     * Gets the output folder.
     *
     * @return outputFolder Output folder.
     */
    public File getOutputFolder() {
        return outputFolder;
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
     * Returns the selected spectrum files.
     *
     * @return The selected spectrum files.
     */
    public ArrayList<File> getSpectrumFiles() {
        return spectrumFiles;
    }

    /**
     * Sets the selected spectrum file.
     *
     * @param spectrumFiles Spectrum files.
     */
    public void setSpectrumFiles(ArrayList<File> spectrumFiles) {
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
        } catch (Exception e) {
            catchException(e);
        }

        return p.getProperty("denovogui.version");
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
     * Set the search parameters.
     *
     * @param searchParameters the search parameters to set
     */
    public void setSearchParameters(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;
        parametersFile = searchParameters.getParametersFile();
        if (parametersFile != null) {
            settingsFileJTextField.setText(parametersFile.getName());
        } else {
            settingsFileJTextField.setText(userSettingsTxt);
        }
        validateInput(false);
    }

    /**
     * Verifies that the modifications backed-up in the search parameters are
     * loaded and alerts the user in case conflicts are found.
     *
     * @param searchParameters the search parameters to load
     */
    public void loadModifications(SearchParameters searchParameters) {
        ArrayList<String> toCheck = ptmFactory.loadBackedUpModifications(searchParameters, false); // @TODO: have to set the searchparams???
        if (!toCheck.isEmpty()) {
            String message = "The definition of the following PTM(s) seems to have change and was not loaded:\n";
            for (int i = 0; i < toCheck.size(); i++) {
                if (i > 0) {
                    if (i < toCheck.size() - 1) {
                        message += ", ";
                    } else {
                        message += " and ";
                    }
                    message += toCheck.get(i);
                }
            }
            message += ".\nPlease verify the definition of the PTM(s) in the modifications editor.";
            javax.swing.JOptionPane.showMessageDialog(null,
                    message,
                    "PTM definition obsolete", JOptionPane.OK_OPTION);
        }
    }

    /**
     * Validates the input.
     *
     * @param showMessage if true an error messages are shown to the users
     * @return a boolean indicating if the input is valid.
     */
    private boolean validateInput(boolean showMessage) {

        boolean valid = true;

        if (!enablePepNovoJCheckBox.isSelected()) {
            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You need to select at least one search engine.", "No Search Engines Selected", JOptionPane.WARNING_MESSAGE);
            }
            valid = false;
        }

        if (spectrumFiles.isEmpty()) {
            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You need to select at least one spectrum file.", "Spectra Files Not Found", JOptionPane.WARNING_MESSAGE);
            }
            spectraFilesLabel.setForeground(Color.RED);
            spectraFilesLabel.setToolTipText("Please select at least one spectrum file");
            valid = false;
        } else {
            spectraFilesLabel.setToolTipText(null);
            spectraFilesLabel.setForeground(Color.BLACK);
        }

        if (valid) {
            valid = validateParametersInput(showMessage);
            if (!valid) {
                //feedbackLabel.setText("<html> <a href><font color=red>Something went wrong with the search parameters but no help could be found. Please contact the developers.</font></a> </html>");
                //feedbackLabel.setVisible(true);
                // @TODO: show search params dialog
            }
        }

        // validate the output folder
        if (outputFolderTextField.getText() == null || outputFolderTextField.getText().trim().equals("")) {
            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You need to specify an output folder.", "Output Folder Not Found", JOptionPane.WARNING_MESSAGE);
            }
            resultFolderLbl.setForeground(Color.RED);
            resultFolderLbl.setToolTipText("Please select an output folder");
            valid = false;
        } else if (!new File(outputFolderTextField.getText()).exists()) {
            int value = JOptionPane.showConfirmDialog(this, "The selected output folder does not exist. Do you want to create it?", "Folder Not Found", JOptionPane.YES_NO_OPTION);

            if (value == JOptionPane.YES_OPTION) {
                boolean success = new File(outputFolderTextField.getText()).mkdir();

                if (!success) {
                    JOptionPane.showMessageDialog(this, "Failed to create the output folder. Please create it manually and re-select it.", "File Error", JOptionPane.ERROR_MESSAGE);
                    valid = false;
                } else {
                    resultFolderLbl.setForeground(Color.BLACK);
                    resultFolderLbl.setToolTipText(null);
                }
            }
        } else {
            resultFolderLbl.setForeground(Color.BLACK);
            resultFolderLbl.setToolTipText(null);
        }

        if (searchParameters == null) {
            configurationFileLbl.setForeground(Color.RED);
            configurationFileLbl.setToolTipText("Please check the settings");
        } else {
            // @TODO: do we need to check more??
        }

        startButton.setEnabled(valid);
        return valid;
    }

    /**
     * Inspects the parameters validity.
     *
     * @param showMessage if true an error messages are shown to the users
     * @return a boolean indicating if the parameters are valid
     */
    public boolean validateParametersInput(boolean showMessage) {

        boolean valid = true;

        // @TODO: do we need any validation here??

        return valid;
    }

    /**
     * Shows the user that these are default settings.
     */
    private void setDefaultParameters() {
        settingsFileJTextField.setText(defaultSettingsTxt);
    }

    /**
     * Returns a line with the most used modifications.
     *
     * @return a line containing the most used modifications
     */
    public String getModificationUseAsString() {
        String result = "";
        for (String name : modificationUse) {
            result += name + MODIFICATION_SEPARATOR;
        }
        return result;
    }

    /**
     * Returns a list with the most used modifications.
     *
     * @return a list with the most used modifications
     */
    public ArrayList<String> getModificationUse() {
        return modificationUse;
    }

    /**
     * Method called whenever an exception is caught.
     *
     * @param e the exception caught
     */
    public void catchException(Exception e) {
        exceptionHandler.catchException(e);
    }

    /**
     * Loads the use of modifications from a line.
     *
     * @param aLine modification use line from the configuration file
     */
    private void loadModificationUse(String aLine) {
        ArrayList<String> modificationUses = new ArrayList<String>();

        // Split the different modifications.
        int start;

        while ((start = aLine.indexOf(MODIFICATION_SEPARATOR)) >= 0) {
            String name = aLine.substring(0, start);
            aLine = aLine.substring(start + 2);
            if (!name.trim().equals("")) {
                modificationUses.add(name);
            }
        }

        for (String name : modificationUses) {
            start = name.indexOf("_");
            String modificationName = name;

            if (start != -1) {
                modificationName = name.substring(0, start); // old format, remove usage statistics
            }

            if (ptmFactory.containsPTM(modificationName)) {
                modificationUse.add(modificationName);
            }
        }
    }

    /**
     * This method saves PTM usage in the conf folder.
     */
    private void saveConfigurationFile() {

        File folder = new File(getJarFilePath() + File.separator + "resources" + File.separator + "conf" + File.separator);

        if (!folder.exists()) {
            JOptionPane.showMessageDialog(this, new String[]{"Unable to find folder: '" + folder.getAbsolutePath() + "'!",
                "Could not save PTM usage."}, "Folder Not Found", JOptionPane.WARNING_MESSAGE);
        } else {
            File output = new File(folder, DeNovoSequencingHandler.DENOVOGUI_COMFIGURATION_FILE);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(output));
                bw.write("Modification use:" + System.getProperty("line.separator"));
                bw.write(getModificationUseAsString() + System.getProperty("line.separator"));
                bw.flush();
                bw.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(this, new String[]{"Unable to write file: '" + ioe.getMessage() + "'!",
                    "Could not save PTM usage."}, "File Location Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
