package com.compomics.denovogui.cmd;

import com.compomics.denovogui.DeNovoSequencingHandler;
import com.compomics.denovogui.preferences.DeNovoGUIPathPreferences;
import com.compomics.software.CompomicsWrapper;
import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathPreferences;
import com.compomics.util.exceptions.exception_handlers.CommandLineExceptionHandler;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import org.apache.commons.cli.*;

/**
 * This class can be used to control DeNovoGUI in command line.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class DeNovoCLI implements Callable {

    /**
     * The command line parameters.
     */
    private DeNovoCLIInputBean deNovoCLIInputBean;
    /**
     * The post translational modifications factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The exception handler for the command line process.
     */
    private CommandLineExceptionHandler exceptionHandler = new CommandLineExceptionHandler();

    /**
     * Construct a new SearchCLI runnable from a SearchCLI Bean. When
     * initialization is successful, calling "run" will start SearchGUI and
     * write the output files when finished.
     *
     * @param args the command line arguments
     */
    public DeNovoCLI(String[] args) {

        try {
            // Load modifications
            try {
                ptmFactory.importModifications(DeNovoSequencingHandler.getModificationsFile(getJarFilePath()), false);
            } catch (Exception e) {
                System.out.println("An error occurred while loading the modifications.");
                exceptionHandler.catchException(e);
            }
            try {
                ptmFactory.importModifications(DeNovoSequencingHandler.getUserModificationsFile(getJarFilePath()), true);
            } catch (Exception e) {
                System.out.println("An error occurred while loading the user modifications.");
                exceptionHandler.catchException(e);
            }
            try {
                enzymeFactory.importEnzymes(DeNovoSequencingHandler.getEnzymesFile(getJarFilePath()));
            } catch (Exception e) {
                System.out.println("An error occurred while loading the enzymes.");
                exceptionHandler.catchException(e);
            }
            Options lOptions = new Options();
            DeNovoCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

            if (!DeNovoCLIInputBean.isValidStartup(line)) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print("\n======================" + System.getProperty("line.separator"));
                lPrintWriter.print("DeNovoCLI" + System.getProperty("line.separator"));
                lPrintWriter.print("======================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(DeNovoCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                deNovoCLIInputBean = new DeNovoCLIInputBean(line);
                call();
            }
        } catch (Exception e) {
            exceptionHandler.catchException(e);
        }
    }

    /**
     * Calling this method will run the configured DeNovoCLI process.
     */
    public Object call() {

        PathSettingsCLIInputBean pathSettingsCLIInputBean = deNovoCLIInputBean.getPathSettingsCLIInputBean();
        if (pathSettingsCLIInputBean.hasInput()) {
            PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(pathSettingsCLIInputBean);
            pathSettingsCLI.setPathSettings();
        } else {
            try {
                setPathConfiguration();
            } catch (Exception e) {
                System.out.println("An error occurred when setting path configuration. Default will be used.");
                exceptionHandler.catchException(e);
            }
        }
        try {
            ArrayList<PathKey> errorKeys = DeNovoGUIPathPreferences.getErrorKeys();
            if (!errorKeys.isEmpty()) {
                System.out.println("Impossible to write in the following configuration folders, please use a temporary folder, the path configuration command line, or edit the configuration paths from the graphical interface.");
                for (PathKey pathKey : errorKeys) {
                    System.out.println(pathKey.getId() + ": " + pathKey.getDescription());
                }
            }
        } catch (Exception e) {
            System.out.println("Impossible to load path configuration, default will be used.");
        }

        try {
            WaitingHandlerCLIImpl waitingHandlerCLIImpl = new WaitingHandlerCLIImpl();

            boolean runPepNovo = deNovoCLIInputBean.enablePepNovo();
            boolean runDirecTag = deNovoCLIInputBean.enableDirecTag();
            boolean runPNovo = deNovoCLIInputBean.enablePNovo();

            File pepNovoExecutable = deNovoCLIInputBean.getPepNovoExecutable();
            File direcTagExecutable = deNovoCLIInputBean.getDirecTagExecutable();
            File pNovoExecutable = deNovoCLIInputBean.getPNovoExecutable();
            File pepNovoFolder = null;
            File direcTagFolder = null;
            File pNovoFolder = null;
            String pepNovoExecutableTitle = null;
            String direcTagExecutableTitle = null;
            String pNovoExecutableTitle = null;

            // OS check
            String osName = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();

            // pepNovo
            if (pepNovoExecutable != null) {
                pepNovoExecutableTitle = pepNovoExecutable.getName();
                pepNovoFolder = pepNovoExecutable.getParentFile();
            } else if (new File(getJarFilePath() + "/resources/PepNovo").exists()) {

                // use the default PepNovo folder if not set by user
                pepNovoFolder = new File(getJarFilePath() + "/resources/PepNovo");

                if (osName.contains("mac os")) {
                    pepNovoExecutableTitle = "PepNovo_Mac";
                } else if (osName.contains("windows")) {
                    pepNovoExecutableTitle = "PepNovo_Windows.exe";
                } else if (osName.contains("nix") || osName.contains("nux")) {
                    pepNovoExecutableTitle = "PepNovo_Linux";
                } else {
                    // unsupported OS version
                }
            }

            // direcTag
            if (direcTagExecutable != null) {
                direcTagExecutableTitle = direcTagExecutable.getName();
                direcTagFolder = direcTagExecutable.getParentFile();
            } else if (new File(getJarFilePath() + "/resources/DirecTag").exists()) {

                // use the default DirecTag folder if not set by user
                if (osName.contains("windows")) {
                    if (arch.lastIndexOf("64") != -1) {
                        direcTagFolder = new File(getJarFilePath() + "/resources/DirecTag/windows_64bits");
                    } else {
                        direcTagFolder = new File(getJarFilePath() + "/resources/DirecTag/windows_32bits");
                    }
                    direcTagExecutableTitle = "directag.exe";
                } else if (osName.contains("nix") || osName.contains("nux")) {
                    if (arch.lastIndexOf("64") != -1) {
                        direcTagFolder = new File(getJarFilePath() + "/resources/DirecTag/linux_64bit");
                    } else {
                        direcTagFolder = new File(getJarFilePath() + "/resources/DirecTag/linux_32bit");
                    }
                    direcTagExecutableTitle = "directag";
                } else if (osName.contains("mac os")) {

                    // try the linux version..?
                    direcTagFolder = new File(getJarFilePath() + "/resources/DirecTag/linux_32bit");
                    direcTagExecutableTitle = "directag";

                } else {
                    // unsupported OS version
                }
            }
            
            // pNovo
            if (pNovoExecutable != null) {
                pNovoExecutableTitle = pNovoExecutable.getName();
                pNovoFolder = pNovoExecutable.getParentFile();
            } else if (new File(getJarFilePath() + "/resources/pNovo").exists()) {

                // use the default pNovo folder if not set by user
                pNovoFolder = new File(getJarFilePath() + "/resources/pNovo");

                if (osName.contains("mac os")) {
                    // unsupported OS version
                } else if (osName.contains("windows")) {
                    pNovoExecutableTitle = "pNovoplus.exe";
                } else if (osName.contains("nix") || osName.contains("nux")) {
                    // unsupported OS version
                } else {
                    // unsupported OS version
                }
            }

            // check if the PepNovo folder is set
            if (pepNovoFolder == null && runPepNovo) {
                waitingHandlerCLIImpl.appendReport("\nPepNovo+ location not set! Sequencing canceled.", false, true);
                System.exit(0);
            }

            // check of the PepNovo executable is set
            if (pepNovoExecutableTitle == null && runPepNovo) {
                waitingHandlerCLIImpl.appendReport("\nPepNovo+ executable not set! Sequencing canceled.", false, true);
                System.exit(0);
            }

            // check if the DirecTag folder is set
            if (direcTagFolder == null && runDirecTag) {
                waitingHandlerCLIImpl.appendReport("\nDirecTag location not set! Sequencing canceled.", false, true);
                System.exit(0);
            }

            // check of the DirecTag executable is set
            if (direcTagExecutableTitle == null && runDirecTag) {
                waitingHandlerCLIImpl.appendReport("\nDirecTag executable not set! Sequencing canceled.", false, true);
                System.exit(0);
            }
            
            // check if the pNovo folder is set
            if (pNovoFolder == null && runPNovo) {
                waitingHandlerCLIImpl.appendReport("\npNovo+ location not set! Sequencing canceled.", false, true);
                System.exit(0);
            }

            // check of the pNovo executable is set
            if (pNovoExecutableTitle == null && runPNovo) {
                waitingHandlerCLIImpl.appendReport("\npNovo+ executable not set! Sequencing canceled.", false, true);
                System.exit(0);
            }

            if (!runPepNovo && !runDirecTag && !runPNovo) {
                waitingHandlerCLIImpl.appendReport("\nNo sequencing algorithms selected! Sequencing canceled.", false, true);
                System.exit(0);
            }

            // check precursor tolerance, max is 5, but default for search params is 10...
            if (deNovoCLIInputBean.getSearchParameters().getPrecursorAccuracyDalton() > 5) {
                waitingHandlerCLIImpl.appendReport("\nPrecursor tolerance has to be between 0 and 5.0!", false, true);
                System.exit(0);
            }

            // starting the DeNovoCLI
            waitingHandlerCLIImpl.appendReportEndLine();
            waitingHandlerCLIImpl.appendReport("Starting DeNovoCLI.", true, true);
            waitingHandlerCLIImpl.appendReportEndLine();

            // Load the spectra into the factory
            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            waitingHandlerCLIImpl.appendReport("Loading the spectra.", true, true);
            for (File spectrumFile : deNovoCLIInputBean.getSpectrumFiles()) {
                spectrumFactory.addSpectra(spectrumFile, waitingHandlerCLIImpl);
            }
            waitingHandlerCLIImpl.appendReport("Done loading the spectra.", true, true);

            // start the sequencing
            DeNovoSequencingHandler searchHandler = new DeNovoSequencingHandler(pepNovoFolder, direcTagFolder, pNovoFolder);
            searchHandler.setNThreads(deNovoCLIInputBean.getNThreads());
            searchHandler.startSequencing(deNovoCLIInputBean.getSpectrumFiles(),
                    deNovoCLIInputBean.getSearchParameters(),
                    deNovoCLIInputBean.getOutputFile(), pepNovoExecutableTitle, direcTagExecutableTitle, pNovoExecutableTitle,
                    runPepNovo, runDirecTag, runPNovo, waitingHandlerCLIImpl, exceptionHandler);
        } catch (Exception e) {
            exceptionHandler.catchException(e);
        }

        return null;
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
     * DeNovoCLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "DeNovoCLI performs de novo sequencing using the PepNovo+, DirecTag and pNovo+ algoritms." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Spectra must be provided in the Mascot Generic File (mgf) format." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "The identification parameters can be provided as a file as saved from the GUI or generated using the IdentificationParametersCLI." + System.getProperty("line.separator")
                + "See http://code.google.com/p/compomics-utilities/wiki/IdentificationParametersCLI for more details." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://denovogui.googlecode.com and http://code.google.com/p/denovogui/wiki/DeNovoCLI." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at http://groups.google.com/group/denovogui." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + "\n";
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new DeNovoCLI(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("DeNovoCLI.class").getPath(), "DeNovoGUI");
    }
}
