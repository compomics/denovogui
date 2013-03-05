package com.compomics.denovogui.gui;

import com.compomics.denovogui.DeNovoSearchHandler;
import com.compomics.denovogui.DeNovoGUIWrapper;
import com.compomics.denovogui.execution.jobs.PepnovoJob;
import com.compomics.denovogui.gui.panels.InputPanel;
import com.compomics.denovogui.gui.panels.ResultsPanel;
import com.compomics.denovogui.gui.panels.StatisticsPanel;
import com.compomics.denovogui.util.Properties;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.EnzymeFactory;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import net.jimmc.jshortcut.JShellLink;

/**
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
     * The input panel.
     */
    private InputPanel inputPanel;
    /**
     * The results panel.
     */
    private ResultsPanel resultsPanel;
    /**
     * The statistics panel.
     */
    private StatisticsPanel statisticsPanel;
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
        inputPanel = new InputPanel(this);
        resultsPanel = new ResultsPanel(this);
        statisticsPanel = new StatisticsPanel(this);
        inputJPanel.add(inputPanel);
        resultsJPanel.add(resultsPanel);
        statisticsJPanel.add(statisticsPanel);
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

        tabbedPane = new javax.swing.JTabbedPane();
        inputJPanel = new javax.swing.JPanel();
        resultsJPanel = new javax.swing.JPanel();
        statisticsJPanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        importResultsMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        exportMenu = new javax.swing.JMenu();
        helpMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        logReportMenu = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("DeNovoGUI");

        tabbedPane.setBackground(new java.awt.Color(230, 230, 230));

        inputJPanel.setLayout(new javax.swing.BoxLayout(inputJPanel, javax.swing.BoxLayout.LINE_AXIS));
        tabbedPane.addTab("Input", inputJPanel);

        resultsJPanel.setLayout(new javax.swing.BoxLayout(resultsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        tabbedPane.addTab("Results", resultsJPanel);

        statisticsJPanel.setLayout(new javax.swing.BoxLayout(statisticsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        tabbedPane.addTab("Statistics", statisticsJPanel);

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

        exportMenu.setMnemonic('E');
        exportMenu.setText("Export");
        menuBar.add(exportMenu);

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
            .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1103, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 673, Short.MAX_VALUE)
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
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu exportMenu;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JMenuItem importResultsMenuItem;
    private javax.swing.JPanel inputJPanel;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JMenuItem logReportMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JPanel resultsJPanel;
    private javax.swing.JPanel statisticsJPanel;
    private javax.swing.JTabbedPane tabbedPane;
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
     */
    public void startSearch() {
        try {
            loadSpectra(spectrumFiles);
        } catch (Exception e) {
            e.printStackTrace();
        }
        searchParameters = inputPanel.getSearchParametersFromGUI();
        searchHandler = new DeNovoSearchHandler(pepNovoFolder);
        searchHandler.startSearch(spectrumFiles, searchParameters, outputFolder, inputPanel);
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
        ArrayList<File> outputFiles = new ArrayList<File>();
        for (File file : spectrumFiles) {
            File resultFile = PepnovoJob.getOutputFile(outputFolder, Util.getFileName(file));
            if (resultFile.exists()) {
                outputFiles.add(resultFile);
            } else {
                inputPanel.appendReport("File " + Util.getFileName(file) + " not found.", true, true);
            }
        }
        importPepnovoResults(outputFiles);
        resultsPanel.diplayResults();
        //@TODO: select the result pane
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
     * Imports the pepnovo results from the given files and puts all matches in
     * the identification
     *
     * @param outFiles the pepnovo result files as a list
     */
    private void importPepnovoResults(ArrayList<File> outFiles) throws SQLException, FileNotFoundException, IOException, IllegalArgumentException, ClassNotFoundException, Exception {
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

        for (File file : outFiles) {
            // initiate the parser
            PepNovoIdfileReader idfileReader = new PepNovoIdfileReader(file);
            // put the identification results in the identification object
            HashSet<SpectrumMatch> allSpectrumMatches = idfileReader.getAllSpectrumMatches(null);
            Iterator iter = allSpectrumMatches.iterator();
            while (iter.hasNext()) {
                SpectrumMatch sm = (SpectrumMatch) iter.next();
                //System.out.println("in: " + sm.getKey());
            }
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
}
