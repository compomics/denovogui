package com.compomics.denovogui.gui;

import com.compomics.denovogui.DeNovoSequencingHandler;
import com.compomics.denovogui.DeNovoGUIWrapper;
import com.compomics.denovogui.execution.jobs.PepNovoJob;
import com.compomics.denovogui.util.Properties;
import com.compomics.software.CommandLineUtils;
import com.compomics.software.ToolFactory;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.denovogui.preferences.DeNovoGUIPathPreferences;
import com.compomics.denovogui.preferences.DeNovoGUIPathPreferences.DeNovoGUIPathKey;
import com.compomics.software.CompomicsWrapper;
import com.compomics.software.autoupdater.MavenJarFile;
import com.compomics.software.dialogs.JavaHomeOrMemoryDialogParent;
import com.compomics.software.dialogs.JavaSettingsDialog;
import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathPreferences;
import com.compomics.software.settings.gui.PathSettingsDialog;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.WaitingDialogExceptionHandler;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.AndromedaParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.CometParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.DirecTagParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.MsAmandaParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.MsgfParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.MyriMatchParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.OmssaParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.PNovoParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.PepnovoParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.TideParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.XtandemParameters;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.PrivacySettingsDialog;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.ptm.ModificationsDialog;
import com.compomics.util.gui.searchsettings.SearchSettingsDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.waiting.WaitingActionListener;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.io.ConfigurationFile;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.LastSelectedFolder;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
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
public class DeNovoGUI extends javax.swing.JFrame implements JavaHomeOrMemoryDialogParent {

    /**
     * The compomics enzyme factory.
     */
    private EnzymeFactory enzymeFactory;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory;
    /**
     * The factory used to handle the modifications.
     */
    private PTMFactory ptmFactory;
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
    private LastSelectedFolder lastSelectedFolder = new LastSelectedFolder("example_dataset");
    /**
     * The example mgf file.
     */
    public final static String exampleMgf = "resources/reference_dataset/Arabidopsis_P1_Top5CID_01.mgf";
    /**
     * The example out file.
     */
    public final static String exampleOutFile = "resources/reference_dataset/Arabidopsis_P1_Top5CID_01.mgf.out";
    /**
     * The example search parameters.
     */
    public final static String exampleSearchParams = "resources/reference_dataset/denovoGUI_example.parameters";
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
     * The selected DirecTag folder.
     */
    private File direcTagFolder;
    /**
     * The selected pNovo folder.
     */
    private File pNovoFolder;
    /**
     * Title of the PepNovo executable.
     */
    private String pepNovoExecutable = "PepNovo_Windows.exe";
    /**
     * Title of the DirecTag executable.
     */
    private String direcTagExecutable = "DirecTag_Windows.exe";
    /**
     * Title of the pNovo executable.
     */
    private String pNovoExecutable = "pNovoplus.exe";
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
    private FrameExceptionHandler exceptionHandler = new FrameExceptionHandler(this, "http://code.google.com/p/denovogui/issues/list");
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The implemented algorithms. The matches of these will be displayed only
     * and in this order.
     */
    public final static Advocate[] implementedAlgorithms = {Advocate.direcTag, Advocate.pepnovo, Advocate.pNovo};
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserPreferences utilitiesUserPreferences = null;
    /**
     * The sequence matching preferences.
     */
    private SequenceMatchingPreferences sequenceMatchingPreferences;

    /**
     * Creates a new DeNovoGUI.
     *
     * @param spectrumFiles the spectrum files (can be null)
     * @param searchParameters the search parameters (can be null)
     * @param outputFolder the output folder (can be null)
     */
    public DeNovoGUI(ArrayList<File> spectrumFiles, SearchParameters searchParameters, File outputFolder) {

        // set up the ErrorLog
        setUpLogFile();

        // set path configuration
        try {
            setPathConfiguration();
        } catch (Exception e) {
            // Will be taken care of next 
        }
        try {
            if (!DeNovoGUIPathPreferences.getErrorKeys().isEmpty()) {
                editPathSettings();
            }
        } catch (Exception e) {
            editPathSettings();
        }

        enzymeFactory = EnzymeFactory.getInstance();
        spectrumFactory = SpectrumFactory.getInstance(1000);
        ptmFactory = PTMFactory.getInstance();

        // load the utilities user preferences
        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occurred when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        // check for new version
        boolean newVersion = false;
        if (!getJarFilePath().equalsIgnoreCase(".") && utilitiesUserPreferences.isAutoUpdate()) {
            newVersion = checkForNewVersion();
        }

        if (!newVersion) {

            String osName = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();

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
                    pepNovoExecutable = "PepNovo_Mac";
                } else if (osName.contains("windows")) {
                    pepNovoExecutable = "PepNovo_Windows.exe";
                } else if (osName.contains("nix") || osName.contains("nux")) {
                    if (arch.lastIndexOf("64") != -1) {
                        pepNovoExecutable = "PepNovo_Linux";
                    } else {
                        // linux 32 bit is not supported
                        pepNovoCheckBox.setSelected(false);
                        pepNovoCheckBox.setEnabled(false);
                    }
                } else {
                    // unsupported OS version
                    pepNovoCheckBox.setSelected(false);
                    pepNovoCheckBox.setEnabled(false);
                }
            }

            // Set the default DirecTag folder
            if (new File(getJarFilePath() + "/resources/DirecTag").exists()) {

                // OS check
                if (osName.contains("windows")) {
                    if (arch.lastIndexOf("64") != -1) {
                        direcTagFolder = new File(getJarFilePath() + "/resources/DirecTag/windows_64bits");
                    } else {
                        direcTagFolder = new File(getJarFilePath() + "/resources/DirecTag/windows_32bits");
                    }
                    direcTagExecutable = "directag.exe";
                } else if (osName.contains("nix") || osName.contains("nux")) {
                    if (arch.lastIndexOf("64") != -1) {
                        direcTagFolder = new File(getJarFilePath() + "/resources/DirecTag/linux_64bit");
                    } else {
                        direcTagFolder = new File(getJarFilePath() + "/resources/DirecTag/linux_32bit");
                    }
                    direcTagExecutable = "directag";
                } else if (osName.contains("mac os")) {
                    // mac os x not supported
                    direcTagCheckBox.setSelected(false); // @TODO: does not work???
                    direcTagCheckBox.setEnabled(false);
                } else {
                    // unsupported OS version
                    direcTagCheckBox.setSelected(false);
                    direcTagCheckBox.setEnabled(false);
                }
            }

            // set the default pNovo folder
            if (new File(getJarFilePath() + "/resources/pNovo").exists()) {

                // OS check
                if (osName.contains("mac os")) {
                    // mac os x not supported
                    pNovoCheckBox.setSelected(false);
                    pNovoCheckBox.setEnabled(false);
                } else if (osName.contains("windows")) {
                    pNovoFolder = new File(getJarFilePath() + "/resources/pNovo/windows");
                    pNovoExecutable = "pNovoplus.exe";
                } else if (osName.contains("nix") || osName.contains("nux")) {
                    if (arch.lastIndexOf("64") != -1) {
                        // linux 64 bit is not supported
                        pNovoCheckBox.setSelected(false);
                        pNovoCheckBox.setEnabled(false);
                    } else {
                        // linux 32 bit is not supported
                        pNovoCheckBox.setSelected(false);
                        pNovoCheckBox.setEnabled(false);
                    }
                } else {
                    // unsupported OS version
                    pNovoCheckBox.setSelected(false);
                    pNovoCheckBox.setEnabled(false);
                }
            }

            deNovoSequencingHandler = new DeNovoSequencingHandler(pepNovoFolder, direcTagFolder, pNovoFolder);

            setUpGUI();

            // set the title
            this.setTitle("DeNovoGUI " + getVersion());

            // set the title of the frame and add the icon
            this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")));

            // load the enzymes
            try {
                enzymeFactory.importEnzymes(new File(DeNovoSequencingHandler.getEnzymeFile()));
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error while reading " + DeNovoSequencingHandler.getEnzymeFile() + ".", "Enzyme File Error", JOptionPane.ERROR_MESSAGE);
            }

            if (searchParameters == null) {
                searchParameters = new SearchParameters();
                setDefaultParameters(); // label the configs as default
            } else {
                loadModifications(searchParameters);
                settingsFileJTextField.setText(searchParameters.getParametersFile().getName());
            }

            File folder = new File(getJarFilePath() + File.separator + "resources" + File.separator + "conf" + File.separator);
            File modUseFile = new File(folder, DeNovoSequencingHandler.DENOVOGUI_COMFIGURATION_FILE);
            ConfigurationFile configFile = new ConfigurationFile(modUseFile); // @TODO: standardize the use of the ConfigurationFile
            try {
                modificationUse = SearchSettingsDialog.loadModificationsUse(configFile);
            } catch (IOException e) {

            }

            // set the default enzyme to trypsin
            if (searchParameters.getEnzyme() == null) {
                searchParameters.setEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
            }

            this.searchParameters = searchParameters;

            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.omssa.getIndex(), new OmssaParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.xtandem.getIndex(), new XtandemParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.msgf.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.msgf.getIndex(), new MsgfParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.comet.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.comet.getIndex(), new CometParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.tide.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.tide.getIndex(), new TideParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.msAmanda.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.msAmanda.getIndex(), new MsAmandaParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.myriMatch.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.myriMatch.getIndex(), new MyriMatchParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.andromeda.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.andromeda.getIndex(), new AndromedaParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex(), new PepnovoParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.direcTag.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.direcTag.getIndex(), new DirecTagParameters());
            }
            if (searchParameters.getIdentificationAlgorithmParameter(Advocate.pNovo.getIndex()) == null) {
                searchParameters.setIdentificationAlgorithmParameter(Advocate.pNovo.getIndex(), new PNovoParameters());
            }

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
        }
    }

    /**
     * Sets the path configuration.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(getJarFilePath(), UtilitiesPathPreferences.configurationFileName);
        if (pathConfigurationFile.exists()) {
            DeNovoGUIPathPreferences.loadPathPreferencesFromFile(pathConfigurationFile);
        }
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
                        null, "An error occurred when trying to create the DeNovoGUI log file.",
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
        pepNovoLinkLabel = new javax.swing.JLabel();
        pepNovoButton = new javax.swing.JButton();
        direcTagButton = new javax.swing.JButton();
        direcTagLinkLabel = new javax.swing.JLabel();
        direcTagCheckBox = new javax.swing.JCheckBox();
        pepNovoCheckBox = new javax.swing.JCheckBox();
        direcTagPlatformsButton = new javax.swing.JButton();
        pepNovoPlatformsButton = new javax.swing.JButton();
        pNovoCheckBox = new javax.swing.JCheckBox();
        pNovoButton = new javax.swing.JButton();
        pNovoPlatformsButton = new javax.swing.JButton();
        pNovoLinkLabel = new javax.swing.JLabel();
        betaLabel = new javax.swing.JLabel();
        directTagSettingsJButton = new javax.swing.JButton();
        pepNovoSettingsJButton = new javax.swing.JButton();
        pNovoSettingsJButton = new javax.swing.JButton();
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
        algorithmLocationsMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        javaSettingsJMenuItem = new javax.swing.JMenuItem();
        privacyMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        logReportMenu = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("DeNovoGUI");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        searchEnginesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Sequencing Methods"));
        searchEnginesPanel.setOpaque(false);

        pepNovoLinkLabel.setText("<html>PepNovo+ De Novo Peptide Sequencing - <a href=\"http://proteomics.ucsd.edu/Software/PepNovo.html\">PepNovo+ web page</a></html> ");
        pepNovoLinkLabel.setToolTipText("Open the PepNovo+ web page");
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
        pepNovoButton.setToolTipText("Enable PepNovo+");
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

        direcTagButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/directag.png"))); // NOI18N
        direcTagButton.setToolTipText("Enable DirecTag");
        direcTagButton.setBorder(null);
        direcTagButton.setBorderPainted(false);
        direcTagButton.setContentAreaFilled(false);
        direcTagButton.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        direcTagButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                direcTagButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                direcTagButtonMouseExited(evt);
            }
        });
        direcTagButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                direcTagButtonActionPerformed(evt);
            }
        });

        direcTagLinkLabel.setText("<html>DirecTag MS/MS Sequence Tagging - <a href=\"http://fenchurch.mc.vanderbilt.edu/bumbershoot/directag/\">DirecTag web page</a></html> ");
        direcTagLinkLabel.setToolTipText("Open the DirecTag web page");
        direcTagLinkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                direcTagLinkLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                direcTagLinkLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                direcTagLinkLabelMouseExited(evt);
            }
        });

        direcTagCheckBox.setSelected(true);
        direcTagCheckBox.setToolTipText("Enable DirecTag");
        direcTagCheckBox.setOpaque(false);
        direcTagCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                direcTagCheckBoxActionPerformed(evt);
            }
        });

        pepNovoCheckBox.setSelected(true);
        pepNovoCheckBox.setToolTipText("Enable PepNovo+");
        pepNovoCheckBox.setOpaque(false);
        pepNovoCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pepNovoCheckBoxActionPerformed(evt);
            }
        });

        direcTagPlatformsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/windows_and_linux_gray.png"))); // NOI18N
        direcTagPlatformsButton.setToolTipText("Supported on Windows and Linux");
        direcTagPlatformsButton.setBorderPainted(false);
        direcTagPlatformsButton.setContentAreaFilled(false);

        pepNovoPlatformsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/all_platforms_gray.png"))); // NOI18N
        pepNovoPlatformsButton.setToolTipText("<html>\nSupported on Windows, Mac and Linux<br>\n(Note: requires Linux 64 bit)\n</html>");
        pepNovoPlatformsButton.setBorderPainted(false);
        pepNovoPlatformsButton.setContentAreaFilled(false);

        pNovoCheckBox.setToolTipText("Enable pNovo+");
        pNovoCheckBox.setOpaque(false);
        pNovoCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pNovoCheckBoxActionPerformed(evt);
            }
        });

        pNovoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pnovo.png"))); // NOI18N
        pNovoButton.setToolTipText("Enable pNovo+");
        pNovoButton.setBorder(null);
        pNovoButton.setBorderPainted(false);
        pNovoButton.setContentAreaFilled(false);
        pNovoButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pNovoButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pNovoButtonMouseExited(evt);
            }
        });
        pNovoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pNovoButtonActionPerformed(evt);
            }
        });

        pNovoPlatformsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/windows_only_gray.png"))); // NOI18N
        pNovoPlatformsButton.setToolTipText("<html>\nSupported on Windows<br>\n</html>");
        pNovoPlatformsButton.setBorderPainted(false);
        pNovoPlatformsButton.setContentAreaFilled(false);

        pNovoLinkLabel.setText("<html>pNovo+ De Novo Peptide Sequencing - <a href=\"http://pfind.ict.ac.cn/software/pNovo/\">pNovo+ web page</a></html> ");
        pNovoLinkLabel.setToolTipText("Open the pNovo+ web page");
        pNovoLinkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                pNovoLinkLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pNovoLinkLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pNovoLinkLabelMouseExited(evt);
            }
        });

        betaLabel.setFont(betaLabel.getFont().deriveFont(betaLabel.getFont().getStyle() & ~java.awt.Font.BOLD));
        betaLabel.setText("(beta)");

        directTagSettingsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit_gray.png"))); // NOI18N
        directTagSettingsJButton.setToolTipText("Edit Advanced DirecTag Settings");
        directTagSettingsJButton.setBorder(null);
        directTagSettingsJButton.setBorderPainted(false);
        directTagSettingsJButton.setContentAreaFilled(false);
        directTagSettingsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit.png"))); // NOI18N
        directTagSettingsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                directTagSettingsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                directTagSettingsJButtonMouseExited(evt);
            }
        });
        directTagSettingsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directTagSettingsJButtonActionPerformed(evt);
            }
        });

        pepNovoSettingsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit_gray.png"))); // NOI18N
        pepNovoSettingsJButton.setToolTipText("Edit Advanced PepNovo+ Settings");
        pepNovoSettingsJButton.setBorder(null);
        pepNovoSettingsJButton.setBorderPainted(false);
        pepNovoSettingsJButton.setContentAreaFilled(false);
        pepNovoSettingsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit.png"))); // NOI18N
        pepNovoSettingsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pepNovoSettingsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pepNovoSettingsJButtonMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                pepNovoSettingsJButtonMouseReleased(evt);
            }
        });

        pNovoSettingsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit_gray.png"))); // NOI18N
        pNovoSettingsJButton.setToolTipText("Edit Advanced pNovo+ Settings");
        pNovoSettingsJButton.setBorder(null);
        pNovoSettingsJButton.setBorderPainted(false);
        pNovoSettingsJButton.setContentAreaFilled(false);
        pNovoSettingsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit.png"))); // NOI18N
        pNovoSettingsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pNovoSettingsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pNovoSettingsJButtonMouseExited(evt);
            }
        });
        pNovoSettingsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pNovoSettingsJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout searchEnginesPanelLayout = new javax.swing.GroupLayout(searchEnginesPanel);
        searchEnginesPanel.setLayout(searchEnginesPanelLayout);
        searchEnginesPanelLayout.setHorizontalGroup(
            searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchEnginesPanelLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(searchEnginesPanelLayout.createSequentialGroup()
                        .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(direcTagCheckBox)
                            .addComponent(pepNovoCheckBox))
                        .addGap(71, 71, 71)
                        .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pepNovoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(direcTagButton, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(searchEnginesPanelLayout.createSequentialGroup()
                        .addComponent(pNovoCheckBox)
                        .addGap(71, 71, 71)
                        .addComponent(pNovoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(betaLabel)))
                .addGap(18, 18, 18)
                .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(pNovoPlatformsButton)
                    .addComponent(pepNovoPlatformsButton)
                    .addComponent(direcTagPlatformsButton))
                .addGap(31, 31, 31)
                .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(direcTagLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pepNovoLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pNovoLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(71, 71, 71)
                .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(directTagSettingsJButton)
                    .addComponent(pepNovoSettingsJButton)
                    .addComponent(pNovoSettingsJButton))
                .addGap(81, 81, 81))
        );

        searchEnginesPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {direcTagButton, pepNovoButton});

        searchEnginesPanelLayout.setVerticalGroup(
            searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchEnginesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(direcTagCheckBox)
                    .addComponent(direcTagButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(direcTagLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(direcTagPlatformsButton)
                    .addComponent(directTagSettingsJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(pepNovoCheckBox)
                    .addComponent(pepNovoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pepNovoLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pepNovoPlatformsButton)
                    .addComponent(pepNovoSettingsJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(pNovoCheckBox)
                    .addComponent(pNovoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pNovoLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pNovoPlatformsButton)
                    .addComponent(betaLabel)
                    .addComponent(pNovoSettingsJButton))
                .addContainerGap())
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
                .addGroup(inputFilesPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
        deNovoGuiWebPageJLabel.setText("<html>Please cite DeNovoGUI as <a href=\"http://www.ncbi.nlm.nih.gov/pubmed/24295440\">Muth <i>et al.</i>: J Proteome Res. 2014 Feb 7;13(2):1143-6</a>.</html>");
        deNovoGuiWebPageJLabel.setToolTipText("Open the DeNovoGUI publication");
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
                    .addComponent(searchEnginesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(inputFilesPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(aboutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(45, 45, 45)
                        .addComponent(deNovoGuiWebPageJLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(11, 11, 11)))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(inputFilesPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchEnginesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deNovoGuiWebPageJLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aboutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        openMenuItem.setMnemonic('O');
        openMenuItem.setText("Open...");
        openMenuItem.setToolTipText("Open existing de novo results");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);
        fileMenu.add(jSeparator4);

        loadExampleMenuItem.setMnemonic('E');
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

        modsMenuItem.setMnemonic('M');
        modsMenuItem.setText("Modifications");
        modsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modsMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(modsMenuItem);
        editMenu.add(jSeparator1);

        algorithmLocationsMenuItem.setMnemonic('L');
        algorithmLocationsMenuItem.setText("Algorithm Locations");
        algorithmLocationsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                algorithmLocationsMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(algorithmLocationsMenuItem);
        editMenu.add(jSeparator5);

        javaSettingsJMenuItem.setMnemonic('O');
        javaSettingsJMenuItem.setText("Java Settings");
        javaSettingsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                javaSettingsJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(javaSettingsJMenuItem);

        privacyMenuItem.setMnemonic('P');
        privacyMenuItem.setText("Privacy Settings");
        privacyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                privacyMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(privacyMenuItem);

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
        saveModificationUsage();
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
        new BugReport(this, lastSelectedFolder, "DeNovoGUI", "denovogui", getVersion(),
                "denovogui", "DeNovoGUI", new File(getJarFilePath() + "/resources/DeNovoGUI.log"));
    }//GEN-LAST:event_logReportMenuActionPerformed

    /**
     * Edit the algorithm locations.
     *
     * @param evt
     */
    private void algorithmLocationsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_algorithmLocationsMenuItemActionPerformed
        new LocationDialog(this, true);
    }//GEN-LAST:event_algorithmLocationsMenuItemActionPerformed

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

        boolean validInput = true;

        // check if all ptms are valid for pNovo+
        if (pNovoCheckBox.isSelected()) {
            for (String tempPtm : searchParameters.getModificationProfile().getAllModifications()) {
                PTM currentPtm = ptmFactory.getPTM(tempPtm);
                if (currentPtm.isCTerm() || currentPtm.isNTerm()) {
                    JOptionPane.showMessageDialog(this, "Terminal modifications are currently not supported for pNovo+.\n"
                            + "Please remove \'" + tempPtm + "\' or disable pNovo+.", "Settings Error", JOptionPane.WARNING_MESSAGE);
                    validInput = false;
                }
            }
        }

        // check if all ptms are valid for DirecTag
        if (direcTagCheckBox.isSelected()) {
            boolean terminalPtmsSelected = false;
            for (String tempPtm : searchParameters.getModificationProfile().getAllModifications()) {
                PTM currentPtm = ptmFactory.getPTM(tempPtm);
                if (currentPtm.isCTerm() || currentPtm.isNTerm()) {
                    terminalPtmsSelected = true;
                }
            }

            if (terminalPtmsSelected) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Terminal modifications are not supported for DirecTag and will be ignored.\n"
                        + "Do you still want to continue?", "Settings Error", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (option == JOptionPane.NO_OPTION) {
                    validInput = false;
                }
            }
        }

        // check for valid mass accuracy values for PepNovo
        if (validInput && pepNovoCheckBox.isSelected()) {
            double precursorToleranceInDalton = searchParameters.getPrecursorAccuracy();
            if (searchParameters.getPrecursorAccuracyType() == SearchParameters.MassAccuracyType.PPM) {
                precursorToleranceInDalton = IdentificationParameters.getDaTolerance(precursorToleranceInDalton, 1000); //@TODO: make the reference mass a user parameter?
            }
            if (precursorToleranceInDalton > PepNovoJob.MAX_PRECURSOR_TOLERANCE) {
                JOptionPane.showMessageDialog(this, "The maximum precursor ion mass tolerance for PepNovo+ is " + PepNovoJob.MAX_PRECURSOR_TOLERANCE + " Da.\n"
                        + "Please edit the settings or disable PepNovo+.", "Settings Error", JOptionPane.WARNING_MESSAGE);
                validInput = false;
            }
            double fragmentToleranceInDalton = searchParameters.getFragmentIonAccuracy();
            if (searchParameters.getPrecursorAccuracyType() == SearchParameters.MassAccuracyType.PPM) {
                fragmentToleranceInDalton = IdentificationParameters.getDaTolerance(fragmentToleranceInDalton, 1000); //@TODO: make the reference mass a user parameter?
            }
            if (fragmentToleranceInDalton > PepNovoJob.MAX_FRAGMENT_ION_TOLERANCE) {
                JOptionPane.showMessageDialog(this, "The maximum fragment ion mass tolerance for PepNovo+ is " + PepNovoJob.MAX_FRAGMENT_ION_TOLERANCE + " Da.\n"
                        + "Please edit the settings or disable PepNovo+.", "Settings Error", JOptionPane.WARNING_MESSAGE);
                validInput = false;
            }
        }

        if (validInput) {
            sequenceMatchingPreferences = SequenceMatchingPreferences.getDefaultSequenceMatching();
            saveModificationUsage(); // save the ptms usage

            waitingDialog = new WaitingDialog(this,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")), false,
                    getTips(),
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
        }
    }//GEN-LAST:event_startButtonActionPerformed

    /**
     * Edit the modifications.
     *
     * @param evt
     */
    private void modsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modsMenuItemActionPerformed
        new ModificationsDialog(this, true);
    }//GEN-LAST:event_modsMenuItemActionPerformed

    /**
     * Open the PepNovo web page.
     *
     * @param evt
     */
    private void pepNovoLinkLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepNovoLinkLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://proteomics.ucsd.edu/Software/PepNovo/");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
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
     * Select PepNovo+.
     *
     * @param evt
     */
    private void pepNovoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pepNovoButtonActionPerformed
        pepNovoCheckBox.setSelected(!pepNovoCheckBox.isSelected());
        validateInput(false);
    }//GEN-LAST:event_pepNovoButtonActionPerformed

    /**
     * Clear the spectrum selection.
     *
     * @param evt
     */
    private void clearSpectraButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSpectraButtonActionPerformed
        setSpectrumFiles(new ArrayList<File>());
        spectrumFactory.clearFactory();
        try {
            spectrumFactory.closeFiles();
        } catch (IOException e) {
            catchException(e);
        }
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

        File startLocation = new File(lastSelectedFolder.getLastSelectedFolder());
        ArrayList<File> tempSpectrumFiles = getSpectrumFiles();
        if (tempSpectrumFiles.size() > 0) {
            File temp = tempSpectrumFiles.get(0);
            startLocation = temp.getParentFile();
        }
        JFileChooser fc = new JFileChooser(startLocation);
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".mgf")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Peak list (.mgf)";
            }
        };
        fc.setFileFilter(filter);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder.setLastSelectedFolder(fc.getSelectedFile().getAbsolutePath());

            File[] files = fc.getSelectedFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    File[] currentFiles = file.listFiles();
                    for (File currentFile : currentFiles) {
                        if (fc.getFileFilter().accept(currentFile)) {
                            tempSpectrumFiles.add(currentFile);
                        }
                    }
                } else {
                    tempSpectrumFiles.add(file);
                }
            }
            spectrumFilesTextField.setText(tempSpectrumFiles.size() + " file(s) selected");
            setSpectrumFiles(tempSpectrumFiles);
            //filename = spectraFiles.get(0).getName();
            // @TODO: re-add the progress bar..?
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
        File startLocation = new File(lastSelectedFolder.getLastSelectedFolder());
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
            lastSelectedFolder.setLastSelectedFolder(file.getAbsolutePath());
            try {
                searchParameters = SearchParameters.getIdentificationParameters(file);
                loadModifications(searchParameters);
                parametersFile = file;
                searchParameters.setParametersFile(parametersFile);
                settingsFileJTextField.setText(parametersFile.getName());
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error occurred while reading " + file + ". Please verify the de novo parameters.", "File Error", JOptionPane.ERROR_MESSAGE);
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
        File startLocation = new File(lastSelectedFolder.getLastSelectedFolder());
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
            lastSelectedFolder.setLastSelectedFolder(tempOutputFolder.getAbsolutePath());
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
     * Open the DeNovoGUI publication.
     *
     * @param evt
     */
    private void deNovoGuiWebPageJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoGuiWebPageJLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://www.ncbi.nlm.nih.gov/pubmed/24295440");
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
        if (sequenceMatchingPreferences == null) {
            sequenceMatchingPreferences = SequenceMatchingPreferences.getDefaultSequenceMatching();
        }
        new ResultsFrame(this, null, searchParameters);
    }//GEN-LAST:event_openMenuItemActionPerformed

    /**
     * Load the example dataset.
     *
     * @param evt
     */
    private void loadExampleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadExampleMenuItemActionPerformed

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Example. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("LoadExampleThread") {
            @Override
            public void run() {

                try {
                    spectrumFiles = new ArrayList<File>();
                    File tempSpectrumFile = new File(getJarFilePath(), exampleMgf);
                    spectrumFiles.add(tempSpectrumFile);
                    spectrumFactory.addSpectra(tempSpectrumFile, progressDialog);

                    ArrayList<File> outFiles = new ArrayList<File>();
                    outFiles.add(new File(getJarFilePath(), exampleOutFile));

                    searchParameters = SearchParameters.getIdentificationParameters(new File(getJarFilePath(), exampleSearchParams));

                    progressDialog.setRunFinished();
                    setVisible(false);
                    if (sequenceMatchingPreferences == null) {
                        sequenceMatchingPreferences = SequenceMatchingPreferences.getDefaultSequenceMatching();
                    }
                    new ResultsFrame(DeNovoGUI.this, outFiles, searchParameters);
                } catch (ClassNotFoundException e) {
                    progressDialog.setRunFinished();
                    catchException(e);
                } catch (IOException e) {
                    progressDialog.setRunFinished();
                    catchException(e);
                }
            }
        }.start();
    }//GEN-LAST:event_loadExampleMenuItemActionPerformed

    /**
     * Close the tool.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        exitMenuItemActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    /**
     * Changes the cursor into a hand cursor.
     *
     * @param evt
     */
    private void direcTagButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_direcTagButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_direcTagButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void direcTagButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_direcTagButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_direcTagButtonMouseExited

    /**
     * Select DirecTag.
     *
     * @param evt
     */
    private void direcTagButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_direcTagButtonActionPerformed
        direcTagCheckBox.setSelected(!direcTagCheckBox.isSelected());
        validateInput(false);
    }//GEN-LAST:event_direcTagButtonActionPerformed

    /**
     * Open the DirecTag web page.
     *
     * @param evt
     */
    private void direcTagLinkLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_direcTagLinkLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://fenchurch.mc.vanderbilt.edu/bumbershoot/directag");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_direcTagLinkLabelMouseClicked

    /**
     * Changes the cursor into a hand cursor.
     *
     * @param evt
     */
    private void direcTagLinkLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_direcTagLinkLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_direcTagLinkLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void direcTagLinkLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_direcTagLinkLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_direcTagLinkLabelMouseExited

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void pepNovoCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pepNovoCheckBoxActionPerformed
        validateInput(false);
    }//GEN-LAST:event_pepNovoCheckBoxActionPerformed

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void direcTagCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_direcTagCheckBoxActionPerformed
        validateInput(false);
    }//GEN-LAST:event_direcTagCheckBoxActionPerformed

    /**
     * Open the Java settings dialog.
     *
     * @param evt
     */
    private void javaSettingsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaSettingsJMenuItemActionPerformed
        new JavaSettingsDialog(this, this, null, "DeNovoGUI", true);
    }//GEN-LAST:event_javaSettingsJMenuItemActionPerformed

    /**
     * Open the PrivacySettingsDialog.
     *
     * @param evt
     */
    private void privacyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_privacyMenuItemActionPerformed
        new PrivacySettingsDialog(this, Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")));
    }//GEN-LAST:event_privacyMenuItemActionPerformed

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void pNovoCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pNovoCheckBoxActionPerformed
        validateInput(false);
    }//GEN-LAST:event_pNovoCheckBoxActionPerformed

    /**
     * Changes the cursor into a hand cursor.
     *
     * @param evt
     */
    private void pNovoButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pNovoButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pNovoButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void pNovoButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pNovoButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pNovoButtonMouseExited

    /**
     * Select pNovo+.
     *
     * @param evt
     */
    private void pNovoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pNovoButtonActionPerformed
        pNovoCheckBox.setSelected(!pNovoCheckBox.isSelected());
        validateInput(false);
    }//GEN-LAST:event_pNovoButtonActionPerformed

    /**
     * Open the pNovo web page.
     *
     * @param evt
     */
    private void pNovoLinkLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pNovoLinkLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://pfind.ict.ac.cn/software/pNovo/");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pNovoLinkLabelMouseClicked

    /**
     * Changes the cursor into a hand cursor.
     *
     * @param evt
     */
    private void pNovoLinkLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pNovoLinkLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pNovoLinkLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void pNovoLinkLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pNovoLinkLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pNovoLinkLabelMouseExited

    private void directTagSettingsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_directTagSettingsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_directTagSettingsJButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void directTagSettingsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_directTagSettingsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_directTagSettingsJButtonMouseExited

    /**
     * Open the DirecTag advanced settings.
     *
     * @param evt
     */
    private void directTagSettingsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_directTagSettingsJButtonActionPerformed
        new DirecTagSettingsDialog(this, searchParameters, true);
    }//GEN-LAST:event_directTagSettingsJButtonActionPerformed

    /**
     * Changes the cursor into a hand cursor.
     *
     * @param evt
     */
    private void pNovoSettingsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pNovoSettingsJButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pNovoSettingsJButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void pNovoSettingsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pNovoSettingsJButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pNovoSettingsJButtonMouseExited

    /**
     * Open the pNovo advanced settings.
     *
     * @param evt
     */
    private void pNovoSettingsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pNovoSettingsJButtonActionPerformed
        new PNovoSettingsDialog(this, searchParameters, true);
    }//GEN-LAST:event_pNovoSettingsJButtonActionPerformed

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void pepNovoSettingsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepNovoSettingsJButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pepNovoSettingsJButtonMouseExited

    /**
     * Changes the cursor into a hand cursor.
     *
     * @param evt
     */
    private void pepNovoSettingsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepNovoSettingsJButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pepNovoSettingsJButtonMouseEntered

    /**
     * Show a message that there are no advanced PepNovo settings.
     *
     * @param evt
     */
    private void pepNovoSettingsJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepNovoSettingsJButtonMouseReleased
        JOptionPane.showMessageDialog(this, "There are no advanced settings for PepNovo+. Please use the general settings.", "PepNovo+ Advanced Settings", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_pepNovoSettingsJButtonMouseReleased

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
            // ignore, use default look and feel
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
    private javax.swing.JMenuItem algorithmLocationsMenuItem;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JLabel betaLabel;
    private javax.swing.JButton clearSpectraButton;
    private javax.swing.JLabel configurationFileLbl;
    private javax.swing.JLabel deNovoGuiWebPageJLabel;
    private javax.swing.JButton direcTagButton;
    private javax.swing.JCheckBox direcTagCheckBox;
    private javax.swing.JLabel direcTagLinkLabel;
    private javax.swing.JButton direcTagPlatformsButton;
    private javax.swing.JButton directTagSettingsJButton;
    private javax.swing.JMenu editMenu;
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
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JMenuItem javaSettingsJMenuItem;
    private javax.swing.JButton loadConfigurationsButton;
    private javax.swing.JMenuItem loadExampleMenuItem;
    private javax.swing.JMenuItem logReportMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem modsMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JTextField outputFolderTextField;
    private javax.swing.JButton pNovoButton;
    private javax.swing.JCheckBox pNovoCheckBox;
    private javax.swing.JLabel pNovoLinkLabel;
    private javax.swing.JButton pNovoPlatformsButton;
    private javax.swing.JButton pNovoSettingsJButton;
    private javax.swing.JButton pepNovoButton;
    private javax.swing.JCheckBox pepNovoCheckBox;
    private javax.swing.JLabel pepNovoLinkLabel;
    private javax.swing.JButton pepNovoPlatformsButton;
    private javax.swing.JButton pepNovoSettingsJButton;
    private javax.swing.JMenuItem privacyMenuItem;
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

        sequencingWorker = new SequencingWorker(waitingHandler, true);
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
                deNovoSequencingHandler.cancelSequencing(outputFolder, waitingDialog);
            } catch (Exception e) {
                catchException(e);
            }
        }
    }

    /**
     * Returns a reference to the DeNovoGUI command line interface.
     *
     * @return a reference to the DeNovoGUI command line interface
     */
    public DeNovoSequencingHandler getDeNovoSequencingHandler() {
        return deNovoSequencingHandler;
    }

    @Override
    public void restart() {
        dispose();
        new DeNovoGUIWrapper();
        System.exit(0); // have to close the current java process (as a new one is started on the line above)
    }

    @Override
    public UtilitiesUserPreferences getUtilitiesUserPreferences() {
        return utilitiesUserPreferences;
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
         * If true, the results will be displayed in the results frame when
         * done.
         */
        private boolean displayResults = true;
        /**
         * Exception handler making use of the waiting dialog.
         */
        private WaitingDialogExceptionHandler workerExceptionHandler;

        /**
         * Constructor.
         *
         * @param waitingHandler
         */
        public SequencingWorker(WaitingHandler waitingHandler, boolean displayResults) {
            this.waitingHandler = waitingHandler;
            this.workerExceptionHandler = new WaitingDialogExceptionHandler(waitingDialog, "http://code.google.com/p/denovogui/issues/list");
            this.displayResults = displayResults;
        }

        @Override
        protected Object doInBackground() throws Exception {

            waitingHandler.appendReport("Starting DeNovoGUI.", true, true);
            waitingHandler.appendReportEndLine();

            try {
                waitingHandler.appendReport("Loading the spectra.", true, true);
                loadSpectra(spectrumFiles, waitingHandler);
                waitingHandler.appendReport("Done loading the spectra.", true, true);
                waitingHandler.appendReportEndLine();
                deNovoSequencingHandler.startSequencing(spectrumFiles, searchParameters, outputFolder, pepNovoExecutable, direcTagExecutable, pNovoExecutable,
                        pepNovoCheckBox.isSelected(), direcTagCheckBox.isSelected(), pNovoCheckBox.isSelected(), waitingHandler, exceptionHandler);
            } catch (Exception e) {
                workerExceptionHandler.catchException(e);
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

                DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
                String fileName = "DeNovoGUI Report " + df.format(new Date()) + ".html";
                String report = "";

                if (waitingHandler instanceof WaitingDialog) {
                    report = "<pre>" + ((WaitingDialog) waitingHandler).getReport(new File(outputFolder, fileName)) + "</pre>";
                }

                // append the search parameters
                report += searchParameters.toString(true);
                report = "<html>" + report + "</html>";

                try {
                    FileWriter fw = new FileWriter(new File(outputFolder, fileName));
                    fw.write(report);
                    fw.close();
                } catch (IOException e) {
                    if (waitingHandler != null) {
                        waitingHandler.appendReport("Failed to write to the report file!", true, true);
                    }
                    catchException(e);
                }

                if (displayResults) {
                    try {
                        displayResults();
                    } catch (Exception e) {
                        catchException(e);
                    }
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
     * @throws Exception thrown if an exception occurs
     */
    public void displayResults() throws Exception {

        ArrayList<File> resultFiles = new ArrayList<File>();

        if (pepNovoCheckBox.isSelected()) {
            resultFiles = FileProcessor.getOutFiles(outputFolder, spectrumFiles);
        }
        if (direcTagCheckBox.isSelected()) {
            resultFiles.addAll(FileProcessor.getTagsFiles(outputFolder, spectrumFiles));
        }
        if (pNovoCheckBox.isSelected()) {
            resultFiles.addAll(FileProcessor.getPNovoResultFiles(outputFolder, spectrumFiles));
        }

        setVisible(false);
        if (sequenceMatchingPreferences == null) {
            sequenceMatchingPreferences = SequenceMatchingPreferences.getDefaultSequenceMatching();
        }
        new ResultsFrame(this, resultFiles, searchParameters);
    }

    /**
     * Loads the mgf files in the spectrum factory.
     *
     * @param mgfFiles loads the mgf files in the spectrum factory
     * @param waitingHandler the waiting handler
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void loadSpectra(List<File> mgfFiles, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException {
        // Add spectrum files to the spectrum factory
        for (File spectrumFile : mgfFiles) {
            spectrumFactory.addSpectra(spectrumFile, waitingHandler);
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
    public LastSelectedFolder getLastSelectedFolder() {
        if (lastSelectedFolder == null) {
            lastSelectedFolder = new LastSelectedFolder();
            utilitiesUserPreferences.setLastSelectedFolder(lastSelectedFolder);
        }
        return lastSelectedFolder;
    }

    /**
     * Set the last selected folder.
     *
     * @param lastSelectedFolder the folder to set
     */
    public void setLastSelectedFolder(LastSelectedFolder lastSelectedFolder) {
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
     * Returns the PepNovo executable.
     *
     * @return the pepNovoFolder
     */
    public File getPepNovoFolder() {
        return pepNovoFolder;
    }

    /**
     * Sets the PepNovo executable.
     *
     * @param pepNovoExecutable PepNovo executable.
     */
    public void setPepNovoExecutable(String pepNovoExecutable) {
        this.pepNovoExecutable = pepNovoExecutable;
    }

    /**
     * Returns the PepNovo folder.
     *
     * @return the pepNovoExecutable
     */
    public String getPepNovoExecutable() {
        return pepNovoExecutable;
    }

    /**
     * Sets the DirecTag folder.
     *
     * @param direcTagFolder DirecTag folder.
     */
    public void setDirecTagFolder(File direcTagFolder) {
        this.direcTagFolder = direcTagFolder;
    }

    /**
     * Returns the PepNovo folder.
     *
     * @return the direcTagFolder
     */
    public File getDirecTagFolder() {
        return direcTagFolder;
    }

    /**
     * Sets the DirecTag executable.
     *
     * @param direcTagExecutable DirecTag executable.
     */
    public void setDirecTagExecutable(String direcTagExecutable) {
        this.direcTagExecutable = direcTagExecutable;
    }

    /**
     * Returns the DirecTag executable.
     *
     * @return the direcTagExecutable
     */
    public String getDirecTagExecutable() {
        return direcTagExecutable;
    }

    /**
     * Sets the pNovo folder.
     *
     * @param pNovoFolder pNovo folder.
     */
    public void setPNovoFolder(File pNovoFolder) {
        this.pNovoFolder = pNovoFolder;
    }

    /**
     * Returns the pNovo executable.
     *
     * @return the pNovoFolder
     */
    public File getPNovoFolder() {
        return pNovoFolder;
    }

    /**
     * Sets the pNovo executable.
     *
     * @param pNovoExecutable pNovo executable.
     */
    public void setPNovoExecutable(String pNovoExecutable) {
        this.pNovoExecutable = pNovoExecutable;
    }

    /**
     * Returns the pNovo folder.
     *
     * @return the pNovoExecutable
     */
    public String getPNovoExecutable() {
        return pNovoExecutable;
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
            int executableCounter = 0;
            int modelFolderCounter = 0;
            for (String lFileName : fileNames) {
                if (lFileName.startsWith("PepNovo")) {
                    executableCounter++;
                }
                if (lFileName.equalsIgnoreCase("Models")) {
                    modelFolderCounter++;
                }
            }
            if (executableCounter > 0 && modelFolderCounter > 0) {
                result = true;
            }
        }

        pepNovoCheckBox.setEnabled(result);

        return result;
    }

    /**
     * This method checks for the folder being the DirecTag folder.
     *
     * @param deNovoFolder the folder to check
     * @return boolean to show whether the DirecTag folder is correct
     */
    public boolean checkDirecTagFolder(File deNovoFolder) {

        boolean result = false;

        if (deNovoFolder != null && deNovoFolder.exists() && deNovoFolder.isDirectory()) {
            String[] fileNames = deNovoFolder.list();
            int executableCounter = 0;
            for (String lFileName : fileNames) {
                if (lFileName.startsWith("directag")) {
                    executableCounter++;
                }
            }
            if (executableCounter > 0) {
                result = true;
            }
        }

        direcTagCheckBox.setEnabled(result);

        return result;
    }

    /**
     * This method checks for the folder being the pNovo folder.
     *
     * @param deNovoFolder the folder to check
     * @return boolean to show whether the pNovo folder is correct
     */
    public boolean checkPNovoFolder(File deNovoFolder) {

        boolean result = false;

        if (deNovoFolder != null && deNovoFolder.exists() && deNovoFolder.isDirectory()) {
            String[] fileNames = deNovoFolder.list();
            int executableCounter = 0;
            for (String lFileName : fileNames) {
                if (lFileName.startsWith("pNovoplus")) {
                    executableCounter++;
                }
            }
            if (executableCounter > 0) {
                result = true;
            }
        }

        pNovoCheckBox.setEnabled(result);

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
        sequenceMatchingPreferences = SequenceMatchingPreferences.getDefaultSequenceMatching();
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
                }
                message += toCheck.get(i);
            }
            message += ".\nPlease verify the definition of the PTM(s) in the modifications editor.";
            javax.swing.JOptionPane.showMessageDialog(null, message, "PTM Definition Obsolete", JOptionPane.OK_OPTION);
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

        if (!pepNovoCheckBox.isSelected() && !direcTagCheckBox.isSelected() && !pNovoCheckBox.isSelected()) {
            if (showMessage && valid) {
                JOptionPane.showMessageDialog(this, "You need to select at least one sequencing method.", "No Sequencing Methods Selected", JOptionPane.WARNING_MESSAGE);
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

        // validate the search parameters
        if (!validateParametersInput(showMessage)) {
            valid = false;
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
     * @param showMessage if true an error message is shown to the users
     * @return a boolean indicating if the parameters are valid
     */
    public boolean validateParametersInput(boolean showMessage) {

        // @TODO: do something here?
//        SettingsDialog settingsDialog = new SettingsDialog(this, searchParameters, false, true);
//        boolean valid = settingsDialog.validateParametersInput(false);
//
//        if (!valid) {
//            if (showMessage) {
//                settingsDialog.validateParametersInput(true);
//                settingsDialog.setVisible(true);
//            } else {
//                configurationFileLbl.setForeground(Color.RED);
//                configurationFileLbl.setToolTipText("Please check the search settings");
//            }
//        } else {
//            configurationFileLbl.setToolTipText(null);
//            configurationFileLbl.setForeground(Color.BLACK);
//        }
        return true;
    }

    /**
     * Shows the user that these are default settings.
     */
    private void setDefaultParameters() {
        settingsFileJTextField.setText(defaultSettingsTxt);
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
    private void catchException(Exception e) {
        exceptionHandler.catchException(e);
    }

    /**
     * This method saves PTM usage in the conf folder.
     */
    private void saveModificationUsage() {

        File folder = new File(getJarFilePath() + File.separator + "resources" + File.separator + "conf" + File.separator);

        if (!folder.exists()) {
            JOptionPane.showMessageDialog(this, new String[]{"Unable to find folder: '" + folder.getAbsolutePath() + "'!",
                "Could not save PTM usage."}, "Folder Not Found", JOptionPane.WARNING_MESSAGE);
        } else {
            File output = new File(folder, DeNovoSequencingHandler.DENOVOGUI_COMFIGURATION_FILE);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(output));
                bw.write("Modification use:" + System.getProperty("line.separator"));
                bw.write(SearchSettingsDialog.getModificationUseAsString(modificationUse) + System.getProperty("line.separator"));
                bw.flush();
                bw.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(this, new String[]{"Unable to write file: '" + ioe.getMessage() + "'!",
                    "Could not save PTM usage."}, "File Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Returns the tips of the day.
     *
     * @return the tips of the day in an ArrayList
     */
    public ArrayList<String> getTips() {

        ArrayList<String> tips;

        try {
            InputStream stream = getClass().getResource("/tips.txt").openStream();
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader b = new BufferedReader(streamReader);
            tips = new ArrayList<String>();
            String line;

            while ((line = b.readLine()) != null) {
                tips.add(line);
            }

            b.close();
        } catch (Exception e) {
            catchException(e);
            tips = new ArrayList<String>();
        }

        return tips;
    }

    /**
     * Returns the best score out of a set of scores for the given algorithm. 0
     * if none found.
     *
     * @param advocate the advocate algorithm
     * @param scores the list of scores to inspect
     *
     * @return the best score for this algorithm
     */
    public static double getBestScore(Advocate advocate, Set<Double> scores) {
        double bestScore = 0.0;
        for (double score : scores) {
            if (advocate == Advocate.direcTag) {
                if (bestScore == 0.0 || score < bestScore) {
                    bestScore = score;
                }
            } else if (advocate == Advocate.pepnovo || advocate == Advocate.pNovo) {
                if (score > bestScore) {
                    bestScore = score;
                }
            } else {
                throw new IllegalArgumentException("Best score selection not implemented for algorithm " + advocate + ".");
            }
        }
        return bestScore;
    }

    /**
     * Sorts the score for the given algorithm.
     *
     * @param advocate the advocate algorithm
     * @param scores the scores to sort
     */
    public static void sortScores(Advocate advocate, ArrayList<Double> scores) {
        if (advocate == Advocate.direcTag) {
            Collections.sort(scores);
        } else if (advocate == Advocate.pepnovo || advocate == Advocate.pNovo) {
            Collections.sort(scores, Collections.reverseOrder());
        } else {
            throw new IllegalArgumentException("Sorting order not implemented for algorithm " + advocate + ".");
        }
    }

    /**
     * Check for new version.
     *
     * @return true if a new version is to be downloaded
     */
    public boolean checkForNewVersion() {
        try {
            File jarFile = new File(DeNovoGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            MavenJarFile oldMavenJarFile = new MavenJarFile(jarFile.toURI());
            URL jarRepository = new URL("http", "genesis.ugent.be", new StringBuilder().append("/maven2/").toString());
            return CompomicsWrapper.checkForNewDeployedVersion("DeNovoGUI", oldMavenJarFile, jarRepository, "denovogui.ico",
                    false, true, true, Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")), true);
        } catch (UnknownHostException ex) {
            // no internet connection
            System.out.println("Checking for new version failed. No internet connection.");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns the sequence matching preferences.
     *
     * @return the sequence matching preferences
     */
    public SequenceMatchingPreferences getSequenceMatchingPreferences() {
        return sequenceMatchingPreferences;
    }

    /**
     * Sets the sequence matching preferences.
     *
     * @param sequenceMatchingPreferences the sequence matching preferences
     */
    public void setSequenceMatchingPreferences(SequenceMatchingPreferences sequenceMatchingPreferences) {
        this.sequenceMatchingPreferences = sequenceMatchingPreferences;
    }

    /**
     * Opens a dialog allowing the setting of paths.
     *
     */
    public void editPathSettings() {
        try {
            HashMap<PathKey, String> pathSettings = new HashMap<PathKey, String>();
            for (DeNovoGUIPathKey deNovoGUIPathKey : DeNovoGUIPathKey.values()) {
                pathSettings.put(deNovoGUIPathKey, DeNovoGUIPathPreferences.getPathPreference(deNovoGUIPathKey));
            }
            for (UtilitiesPathPreferences.UtilitiesPathKey utilitiesPathKey : UtilitiesPathPreferences.UtilitiesPathKey.values()) {
                pathSettings.put(utilitiesPathKey, UtilitiesPathPreferences.getPathPreference(utilitiesPathKey));
            }
            PathSettingsDialog pathSettingsDialog = new PathSettingsDialog(this, "DeNovoGUI", pathSettings);
            if (!pathSettingsDialog.isCanceled()) {
                HashMap<PathKey, String> newSettings = pathSettingsDialog.getKeyToPathMap();
                for (PathKey pathKey : pathSettings.keySet()) {
                    String newPath = newSettings.get(pathKey);
                    if (!pathSettings.get(pathKey).equals(newPath)) {
                        DeNovoGUIPathPreferences.setPathPreference(pathKey, newPath);
                    }
                }
                // write path file preference
                File destinationFile = new File(getJarFilePath(), UtilitiesPathPreferences.configurationFileName);
                try {
                    DeNovoGUIPathPreferences.writeConfigurationToFile(destinationFile);
                    restart();
                } catch (Exception e) {
                    catchException(e);
                }
            }
        } catch (Exception e) {
            catchException(e);
        }
    }

    /**
     * Check if the search parameters have changed and ask the user if the
     * parameters should be saved if they have.
     *
     * @param tempSearchParameters the current search parameters
     * @return true of the save to file was successful
     */
    public boolean checkSearchParameters(SearchParameters tempSearchParameters) {

        if (!searchParameters.equals(tempSearchParameters)) {

            int value = JOptionPane.showConfirmDialog(this, "The settings have changed."
                    + "\nDo you want to save the changes?", "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);

            if (value == JOptionPane.YES_OPTION) {

                boolean userSelectFile = false;

                // see if the user wants to overwrite the current settings file
                if (tempSearchParameters.getParametersFile() != null) {
                    value = JOptionPane.showConfirmDialog(this, "Overwrite current settings file?", "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION);

                    if (value == JOptionPane.NO_OPTION) {
                        userSelectFile = true;
                    } else if (value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION) {
                        return false;
                    }

                } else {
                    // no params file > have the user select a file
                    userSelectFile = true;
                }

                boolean fileSelected = true;
                if (userSelectFile) {
                    fileSelected = getNewSearchParameterFileLocation(tempSearchParameters);
                }

                if (fileSelected && tempSearchParameters.getParametersFile() != null) {
                    try {
                        SearchParameters.saveIdentificationParameters(tempSearchParameters, tempSearchParameters.getParametersFile());
                        searchParameters = tempSearchParameters;
                        loadModifications(searchParameters);
                        settingsFileJTextField.setText(parametersFile.getName());
                        return true;
                    } catch (ClassNotFoundException e) {
                        JOptionPane.showMessageDialog(this, "An error occurred when saving the settings:\n"
                                + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                        return false;
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this, "An error occurred when saving the settings:\n"
                                + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    return false;
                }
            } else if (value == JOptionPane.NO_OPTION) {
                // reject the changes
                return true;
            } else { // dialog canceled by user
                return false;
            }
        } else {
            // no changes
            return true;
        }
    }

    /**
     * Get the new search parameter file location from the user.
     *
     * @param tempSearchParameters the new search settings
     * @return true if the file was selected
     */
    public boolean getNewSearchParameterFileLocation(SearchParameters tempSearchParameters) {

        // First check whether a file has already been selected.
        // If so, start from that file's parent.
        File startLocation = new File(getLastSelectedFolder().getLastSelectedFolder());

        if (tempSearchParameters.getParametersFile() != null) {
            startLocation = tempSearchParameters.getParametersFile();
        }

        boolean complete = false;

        while (!complete) {
            JFileChooser fc = new JFileChooser(startLocation);
            FileFilter filter = new FileFilter() {
                @Override
                public boolean accept(File myFile) {
                    return myFile.getName().toLowerCase().endsWith(".parameters") || myFile.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "DeNovoGUI settings file (.parameters)";
                }
            };
            fc.setFileFilter(filter);
            int result = fc.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = fc.getSelectedFile();
                getLastSelectedFolder().setLastSelectedFolder(selected.getAbsolutePath());

                // make sure the file is appended with '.parameters'
                if (!selected.getName().toLowerCase().endsWith(".parameters")) {
                    selected = new File(selected.getParentFile(), selected.getName() + ".parameters");
                    parametersFile = selected;
                } else {
                    selected = new File(selected.getParentFile(), selected.getName());
                    parametersFile = selected;
                }
                complete = true;
                if (selected.exists()) {
                    int choice = JOptionPane.showConfirmDialog(this,
                            new String[]{"The file " + selected.getName() + " already exists.", "Overwrite?"},
                            "File Already Exists", JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.NO_OPTION) {
                        complete = false;
                    }
                }
            } else {
                return complete;
            }
        }

        tempSearchParameters.setParametersFile(parametersFile);
        return true;
    }
}
