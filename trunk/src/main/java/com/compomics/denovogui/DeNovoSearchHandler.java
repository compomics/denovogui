package com.compomics.denovogui;

import com.compomics.denovogui.execution.jobs.PepnovoJob;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.denovogui.io.ModificationFile;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.WaitingHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * This handle the searches with the search parameters taken from the GUI or the
 * command line.
 *
 * @author Marc Vaudel
 * @author Thilo Muth
 * @author Harald Barsnes
 */
public class DeNovoSearchHandler {

    /**
     * The PepNovo folder.
     */
    private File pepNovoFolder;
    /**
     * If true, debug output will be given.
     */
    private boolean debug = false;
    /**
     * Default PTM selection.
     */
    public static final String DENOVOGUI_COMFIGURATION_FILE = "DeNovoGUI_configuration.txt";
    /**
     * Modification file.
     */
    public final static String MODIFICATION_FILE = "resources/conf/denovogui_mods.xml";
    /**
     * User modification file.
     */
    public final static String USER_MODIFICATION_FILE = "resources/conf/denovogui_usermods.xml";
    /**
     * The enzyme file.
     */
    public final static String ENZYME_FILE = "resources/conf/enzymes.xml";

    /**
     * Constructor.
     *
     * @param pepNovoFolder the pep novo folder
     */
    public DeNovoSearchHandler(File pepNovoFolder) {
        this.pepNovoFolder = pepNovoFolder;
    }

    /**
     * Starts the search.
     *
     * @param spectrumFiles the spectrum files to process
     * @param searchParameters the search parameters
     * @param outputFolder the output folder
     * @param waitingHandler the waiting handler
     */
    public void startSearch(List<File> spectrumFiles, SearchParameters searchParameters, File outputFolder, WaitingHandler waitingHandler) {

        long startTime = System.nanoTime();
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        waitingHandler.setMaxProgressValue(spectrumFactory.getNSpectra());

        // Write the modification file
        try {
            File folder = new File(pepNovoFolder, "Models");
            ModificationFile.writeFile(folder, searchParameters.getModificationProfile());
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while writing the modification file.", true, true);
            e.printStackTrace();
        }

        // Get the number of available cores
        int nCores = Runtime.getRuntime().availableProcessors();

        if (debug) {
            System.out.println("number of cores:" + nCores);
        }

        // Start a fixed thread pool
        ExecutorService threadExecutor = Executors.newFixedThreadPool(nCores);

        // Job queue.
        Deque<PepnovoJob> jobs = new ArrayDeque<PepnovoJob>();
        try {
            int nSpectra = FileProcessor.getNumberOfSpectra(spectrumFiles);

            if (debug) {
                System.out.println("no. spectra: " + nSpectra);
            }

            int chunkSize = nSpectra / nCores;

            if (debug) {
                System.out.println("chunk size: " + chunkSize);
            }

            List<File> chunkFiles = FileProcessor.chunkFiles(spectrumFiles, chunkSize);

            // Distribute the chunked spectra to the different jobs.
            for (File spectrumFile : chunkFiles) {
                PepnovoJob job = new PepnovoJob(pepNovoFolder, spectrumFile, outputFolder, searchParameters, waitingHandler);
                jobs.add(job);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DeNovoSearchHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DeNovoSearchHandler.class.getName()).log(Level.SEVERE, null, ex);
        }


        // Execute the jobs from the queue.
        for (PepnovoJob job : jobs) {
            threadExecutor.execute(job);
        }

        // Wait for executor service to shutdown (necessary for reliable exectime calculation.
        try {
            threadExecutor.shutdown();
            threadExecutor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.getLogger(DeNovoSearchHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

        double elapsedTime = (System.nanoTime() - startTime) * 1.0e-9;

        if (debug) {
            System.out.println("used time (sec): " + elapsedTime);
        }
    }

    /**
     * Returns a string with the modifications used.
     *
     * @return String with the X!Tandem location, as specified in the file, or
     * 'null' if the file could not be found, or is empty.
     */
    public String loadModificationsUse() {
        String result = "";

        File folder = new File(getJarFilePath() + File.separator + "resources" + File.separator + "conf" + File.separator);
        if (folder.exists()) {
            File input = new File(folder, DENOVOGUI_COMFIGURATION_FILE);
            try {
                BufferedReader br = new BufferedReader(new FileReader(input));
                String line;
                while ((line = br.readLine()) != null) {
                    // Skip empty lines and comment ('#') lines.
                    line = line.trim();
                    if (line.equals("") || line.startsWith("#")) {
                        // skip lines
                    } else if (line.equals("Modification use:")) {
                        result = br.readLine().trim();
                    }
                }
                br.close();
            } catch (IOException ioe) {
                ioe.printStackTrace(); // @TODO: this exception should be thrown to the GUI!
                JOptionPane.showMessageDialog(null, "An error occured when trying to load the modifications preferences.",
                        "Configuration import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return result;
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("DeNovoSearchHandler.class").getPath(), "DeNovoGUI");
    }

    /**
     * Returns the modifications file.
     *
     * @param jarFolder the folder containing the jar file
     * @return the modifications file
     */
    public static File getModificationsFile(String jarFolder) {
        File result = new File(jarFolder, MODIFICATION_FILE);
        if (!result.exists()) {
            JOptionPane.showMessageDialog(null, MODIFICATION_FILE + " not found.", "Modification File Error", JOptionPane.ERROR_MESSAGE);
        }
        return result;
    }

    /**
     * Returns the user defined enzymes file.
     *
     * @param jarFolder the folder containing the jar file
     * @return the user defined enzymes file
     */
    public static File getEnzymesFile(String jarFolder) {
        File result = new File(jarFolder, ENZYME_FILE);
        if (!result.exists()) {
            JOptionPane.showMessageDialog(null, ENZYME_FILE + " not found.", "Enzymes File Error", JOptionPane.ERROR_MESSAGE);
        }
        return result;
    }

    /**
     * Returns the user defined modifications file.
     *
     * @param jarFolder the folder containing the jar file
     * @return the user defined modifications file
     */
    public static File getUserModificationsFile(String jarFolder) {
        File result = new File(jarFolder, USER_MODIFICATION_FILE);
        if (!result.exists()) {
            JOptionPane.showMessageDialog(null, USER_MODIFICATION_FILE + " not found.", "User Modification File Error", JOptionPane.ERROR_MESSAGE);
        }
        return result;
    }
}
