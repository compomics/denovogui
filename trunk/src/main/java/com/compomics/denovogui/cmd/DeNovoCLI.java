package com.compomics.denovogui.cmd;

import com.compomics.denovogui.DeNovoSequencingHandler;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.identification.SearchParametersInputBean;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import java.io.File;
import java.io.PrintWriter;
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
    private SearchParametersInputBean searchParametersInputBean;
    /**
     * The post translational modifications factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();

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
                e.printStackTrace();
            }
            try {
                ptmFactory.importModifications(DeNovoSequencingHandler.getUserModificationsFile(getJarFilePath()), true);
            } catch (Exception e) {
                System.out.println("An error occurred while loading the user modifications.");
                e.printStackTrace();
            }
            try {
                enzymeFactory.importEnzymes(DeNovoSequencingHandler.getEnzymesFile(getJarFilePath()));
            } catch (Exception e) {
                System.out.println("An error occurred while loading the enzymes.");
                e.printStackTrace();
            }
            Options lOptions = new Options();
            DeNovoCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

            if (!SearchParametersInputBean.isValidStartup(line, SearchParametersInputBean.ToolType.DeNovoGUI)) {
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
                searchParametersInputBean = new SearchParametersInputBean(line);
                call();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calling this method will run the configured DeNovoCLI process.
     */
    public Object call() {

        try {
            WaitingHandlerCLIImpl waitingHandlerCLIImpl = new WaitingHandlerCLIImpl();

            File pepNovoFolder = searchParametersInputBean.getPepNovoLocation();

            // use the default PepNovo folder if not set by user
            if (pepNovoFolder == null && new File(getJarFilePath() + "/resources/PepNovo").exists()) {
                pepNovoFolder = new File(getJarFilePath() + "/resources/PepNovo");
            }
            if (pepNovoFolder == null) {
                waitingHandlerCLIImpl.appendReport("\nPepNovo+ location not set! Sequencing canceled.", false, true);
                System.exit(0);
            }
            
            // OS check
            String osName = System.getProperty("os.name").toLowerCase();
            String exeTitle = "PepNovo.exe";
            if (osName.contains("mac os")) {
                exeTitle = "PepNovo_Mac";
            } else if (osName.contains("windows")) {
                exeTitle = "PepNovo_Windows.exe";
            } else if (osName.indexOf("nix") != -1 || osName.indexOf("nux") != -1) {
                exeTitle = "PepNovo_Linux";
            } else {
                // unsupported OS version
            }

            // check precursor tolerance, max is 5, but default for search params is 10...
            if (searchParametersInputBean.getSearchParameters().getPrecursorAccuracy() > 5) { // @TODO: split precursor tolerance into two parameters to make this simpler?
                waitingHandlerCLIImpl.appendReport("\nPrecursor tolerance has to be between 0 and 5.0!", false, true);
                System.exit(0);
            }
            // Load the spectra into the factory
            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            waitingHandlerCLIImpl.appendReport("Loading the spectra.", true, true);
            for (File spectrumFile : searchParametersInputBean.getSpectrumFiles()) {
                spectrumFactory.addSpectra(spectrumFile);
            }
            waitingHandlerCLIImpl.appendReport("Done loading the spectra.", true, true);

            DeNovoSequencingHandler searchHandler = new DeNovoSequencingHandler(pepNovoFolder);
            searchHandler.setNThreads(searchParametersInputBean.getNThreads());
            searchHandler.startSequencing(searchParametersInputBean.getSpectrumFiles(), 
                    searchParametersInputBean.getSearchParameters(), 
                    searchParametersInputBean.getOutputFile(), exeTitle,
                    waitingHandlerCLIImpl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * DeNovoCLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "DeNovoCLI performs de novo sequencing using the PepNovo+ algoritm." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Spectra must be provided in the Mascot Generic File (mgf) format." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "The de novo parameters can be provided as a serialized instance of the compomics utilities" + System.getProperty("line.separator")
                + "com.compomics.util.experiment.identification.SearchParameters object or set individually" + System.getProperty("line.separator")
                + "using the command line options." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://denovogui.googlecode.com and http://code.google.com/p/denovogui/wiki/DeNovoCLI." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/peptide-shaker." + System.getProperty("line.separator")
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
