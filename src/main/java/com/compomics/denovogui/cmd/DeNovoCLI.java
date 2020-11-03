package com.compomics.denovogui.cmd;

import com.compomics.denovogui.DeNovoSequencingHandler;
import com.compomics.denovogui.preferences.DeNovoGUIPathParameters;
import com.compomics.denovogui.util.Properties;
import com.compomics.software.CompomicsWrapper;
import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathParameters;
import com.compomics.util.Util;
import com.compomics.util.exceptions.exception_handlers.CommandLineExceptionHandler;
import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.io.mass_spectrometry.MsFileHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
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
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory;
    /**
     * The mass spectrometry file handler.
     */
    private final MsFileHandler msFileHandler = new MsFileHandler();
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

            Options lOptions = new Options();
            DeNovoCLIParams.createOptionsCLI(lOptions);
            DefaultParser parser = new DefaultParser();
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

                System.exit(1);
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

        if (pathSettingsCLIInputBean.getLogFolder() != null) {
            DeNovoCLI.redirectErrorStream(pathSettingsCLIInputBean.getLogFolder());
        }

        if (pathSettingsCLIInputBean.hasInput()) {
            PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(pathSettingsCLIInputBean);
            pathSettingsCLI.setPathSettings();
        } else {
            try {
                setPathConfiguration();
            } catch (Exception e) {
                System.out.println("An error occurred when setting the path configurations. Default paths will be used.");
                exceptionHandler.catchException(e);
            }
        }
        try {
            ArrayList<PathKey> errorKeys = DeNovoGUIPathParameters.getErrorKeys();
            if (!errorKeys.isEmpty()) {
                System.out.println("Unable to write in the following configuration folders. Please use a temporary folder, "
                        + "the path configuration command line, or edit the configuration paths from the graphical interface.");
                for (PathKey pathKey : errorKeys) {
                    System.out.println(pathKey.getId() + ": " + pathKey.getDescription());
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to load the path configurations. Default paths will be used.");
        }

        // load enzymes
        enzymeFactory = EnzymeFactory.getInstance();

        try {
            WaitingHandlerCLIImpl waitingHandlerCLIImpl = new WaitingHandlerCLIImpl();

            boolean runPepNovo = deNovoCLIInputBean.enablePepNovo();
            boolean runDirecTag = deNovoCLIInputBean.enableDirecTag();
            boolean runPNovo = deNovoCLIInputBean.enablePNovo();
            boolean runNovor = deNovoCLIInputBean.enableNovor();

            File pepNovoExecutable = deNovoCLIInputBean.getPepNovoExecutable();
            File direcTagExecutable = deNovoCLIInputBean.getDirecTagExecutable();
            File pNovoExecutable = deNovoCLIInputBean.getPNovoExecutable();
            File novorExecutable = deNovoCLIInputBean.getNovorExecutable();
            File pepNovoFolder = null, direcTagFolder = null, pNovoFolder = null, novorFolder = null;
            String pepNovoExecutableTitle = null, direcTagExecutableTitle = null, pNovoExecutableTitle = null, novorExecutableTitle = null;

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
                    pNovoExecutableTitle = "pNovo3.exe";
                } else if (osName.contains("nix") || osName.contains("nux")) {
                    // unsupported OS version
                } else {
                    // unsupported OS version
                }
            }

            // novor
            if (novorExecutable != null) {
                novorExecutableTitle = novorExecutable.getName();
                novorFolder = novorExecutable.getParentFile();
            } else if (new File(getJarFilePath() + "/resources/Novor").exists()) {

                // use the default Novor folder if not set by user
                novorFolder = new File(getJarFilePath() + "/resources/Novor");
                novorExecutableTitle = "novor.jar";
            }

            // check if the PepNovo folder is set
            if (pepNovoFolder == null && runPepNovo) {
                waitingHandlerCLIImpl.appendReport("\nPepNovo+ location not set! Sequencing canceled.", false, true);
                System.exit(1);
            }

            // check of the PepNovo executable is set
            if (pepNovoExecutableTitle == null && runPepNovo) {
                waitingHandlerCLIImpl.appendReport("\nPepNovo+ executable not set! Sequencing canceled.", false, true);
                System.exit(1);
            }

            // check if the DirecTag folder is set
            if (direcTagFolder == null && runDirecTag) {
                waitingHandlerCLIImpl.appendReport("\nDirecTag location not set! Sequencing canceled.", false, true);
                System.exit(1);
            }

            // check of the DirecTag executable is set
            if (direcTagExecutableTitle == null && runDirecTag) {
                waitingHandlerCLIImpl.appendReport("\nDirecTag executable not set! Sequencing canceled.", false, true);
                System.exit(1);
            }

            // check if the pNovo folder is set
            if (pNovoFolder == null && runPNovo) {
                waitingHandlerCLIImpl.appendReport("\npNovo+ location not set! Sequencing canceled.", false, true);
                System.exit(1);
            }

            // check of the pNovo executable is set
            if (pNovoExecutableTitle == null && runPNovo) {
                waitingHandlerCLIImpl.appendReport("\npNovo+ executable not set! Sequencing canceled.", false, true);
                System.exit(1);
            }

            // check of the Novor executable is set
            if (novorExecutableTitle == null && runNovor) {
                waitingHandlerCLIImpl.appendReport("\nNovor executable not set! Sequencing canceled.", false, true);
                System.exit(1);
            }

            // check if the Novor folder is set
            if (novorFolder == null && runNovor) {
                waitingHandlerCLIImpl.appendReport("\nNovor location not set! Sequencing canceled.", false, true);
                System.exit(1);
            }

            if (!runPepNovo && !runDirecTag && !runPNovo && !runNovor) {
                waitingHandlerCLIImpl.appendReport("\nNo sequencing algorithms selected! Sequencing canceled.", false, true);
                System.exit(1);
            }

            File searchParametersFile = deNovoCLIInputBean.getSearchParametersFile();
            SearchParameters searchParameters = SearchParameters.getIdentificationParameters(searchParametersFile);

            // load the project specific ptms
            String error = DeNovoSequencingHandler.loadModifications(searchParameters);
            if (error != null) {
                System.out.println(error);
            }

            // check precursor tolerance, max is 5, but default for search params is 10...
            if (searchParameters.getPrecursorAccuracyDalton() > 5) {
                waitingHandlerCLIImpl.appendReport("\nPrecursor tolerance has to be between 0 and 5.0!", false, true);
                System.exit(1);
            }

            // starting the DeNovoCLI
            waitingHandlerCLIImpl.appendReportEndLine();
            waitingHandlerCLIImpl.appendReport("Starting DeNovoCLI.", true, true);
            waitingHandlerCLIImpl.appendReportEndLine();

            // load the spectra into the factory
            waitingHandlerCLIImpl.appendReport("Loading the spectra.", true, true);
            for (File spectrumFile : deNovoCLIInputBean.getSpectrumFiles()) {
                msFileHandler.register(spectrumFile, waitingHandlerCLIImpl);
            }
            waitingHandlerCLIImpl.appendReport("Done loading the spectra.", true, true);
            
            // incrementing the counter for a new DenovoGUI run
            UtilitiesUserParameters utilitiesUserParameters = UtilitiesUserParameters.loadUserParameters();
            if (utilitiesUserParameters.isAutoUpdate()) {
                Util.sendGAUpdate("UA-36198780-4", "startrun-cl", "denovogui-" + Properties.getVersion());
            }

            // start the sequencing
            DeNovoSequencingHandler searchHandler = new DeNovoSequencingHandler(pepNovoFolder, direcTagFolder, pNovoFolder, novorFolder, msFileHandler);
            searchHandler.setNThreads(deNovoCLIInputBean.getNThreads());
            searchHandler.startSequencing(deNovoCLIInputBean.getSpectrumFiles(),
                    searchParameters,
                    deNovoCLIInputBean.getOutputFile(), searchParametersFile, pepNovoExecutableTitle, direcTagExecutableTitle, pNovoExecutableTitle, novorExecutableTitle,
                    runPepNovo, runDirecTag, runPNovo, runNovor, waitingHandlerCLIImpl, exceptionHandler);
        } catch (Exception e) {
            exceptionHandler.catchException(e);
            return 1;
        }

        return 0;
    }

    /**
     * Sets the path configuration.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(getJarFilePath(), UtilitiesPathParameters.configurationFileName);
        if (pathConfigurationFile.exists()) {
            DeNovoGUIPathParameters.loadPathParametersFromFile(pathConfigurationFile);
        }
    }

    /**
     * DeNovoCLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "DeNovoCLI performs de novo sequencing using the PepNovo+, DirecTag, pNovo+ and Novor algoritms." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Spectra must be provided in the Mascot Generic File (mgf) format." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "The identification parameters can be provided as a file as saved from the GUI or generated using the IdentificationParametersCLI." + System.getProperty("line.separator")
                + "See http://compomics.github.io/projects/compomics-utilities/wiki/identificationparameterscli.html for more details." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://compomics.github.io/projects/denovogui.html and http://compomics.github.io/projects/denovogui/wiki/denovocli.html." + System.getProperty("line.separator")
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
     * redirects the error stream to the PeptideShaker.log of a given folder.
     *
     * @param logFolder the folder where to save the log
     */
    public static void redirectErrorStream(File logFolder) {

        try {
            logFolder.mkdirs();
            File file = new File(logFolder, "PeptideShaker.log");
            System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

            System.err.println(System.getProperty("line.separator") + System.getProperty("line.separator") + new Date()
                    + ": PeptideShaker version " + Properties.getVersion() + ".");
            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
            System.err.println("Total amount of memory in the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
            System.err.println("Free memory: " + Runtime.getRuntime().freeMemory() + ".");
            System.err.println("Java version: " + System.getProperty("java.version") + ".");
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
