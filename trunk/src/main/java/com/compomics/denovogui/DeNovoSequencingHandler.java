package com.compomics.denovogui;

import com.compomics.denovogui.execution.jobs.PepnovoJob;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.denovogui.io.ModificationFile;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.WaitingHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;

/**
 * This handle the searches with the search parameters taken from the GUI or the
 * command line.
 *
 * @author Marc Vaudel
 * @author Thilo Muth
 * @author Harald Barsnes
 */
public class DeNovoSequencingHandler {

    /**
     * The PepNovo folder.
     */
    private File pepNovoFolder;
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
     * The name of the paramters file saved by default
     */
    public final static String paramtersFileName = "denovoGUI.parameters";
    /**
     * The chunk files.
     */
    private ArrayList<File> chunkFiles;
    /**
     * Number of threads to use for the processing.
     */
    private int nThreads = Runtime.getRuntime().availableProcessors(); // @TODO: should be moved to user preferences?
    /**
     * The thread executor
     */
    private ExecutorService threadExecutor = null;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The job queue.
     */
    private Deque<PepnovoJob> jobs;
    /**
     * The out folder.
     */
    private File outputFolder;

    /**
     * Constructor.
     *
     * @param pepNovoFolder the pep novo folder
     */
    public DeNovoSequencingHandler(File pepNovoFolder) {
        this.pepNovoFolder = pepNovoFolder;
    }

    /**
     * Starts the sequencing for a list of files which will be processed
     * sequentially.
     *
     * @param spectrumFiles the spectrum files to process
     * @param searchParameters the search parameters
     * @param outputFolder the output folder
     * @param exeTitle the name of the executable
     * @param waitingHandler the waiting handler
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void startSequencing(List<File> spectrumFiles, SearchParameters searchParameters, File outputFolder, String exeTitle, WaitingHandler waitingHandler) throws IOException, ClassNotFoundException {

        this.outputFolder = outputFolder;

        long startTime = System.nanoTime();
        waitingHandler.setMaxProgressValue(spectrumFactory.getNSpectra());
        waitingHandler.setSecondaryProgressDialogIndeterminate(true);

        // Write the modification file
        try {
            File folder = new File(pepNovoFolder, "Models");
            ModificationFile.writeFile(folder, searchParameters.getModificationProfile());
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while writing the modification file.", true, true);
            e.printStackTrace();
        }

        // Back-up the parameters
        try {
            SearchParameters.saveIdentificationParameters(searchParameters, new File(outputFolder, paramtersFileName));
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while writing the sequencing parameters.", true, true);
            e.printStackTrace();
        }

        // Get the number of available threads
        String fileEnding = "";
        if (spectrumFactory.getMgfFileNames().size() > 1) {
            fileEnding = "s";
        }
        String spectrumEnding = "";
        if (nThreads > 1) {
            spectrumEnding = "s";
        }
        waitingHandler.appendReport("Starting de novo sequencing: " + spectrumFactory.getNSpectra() + " spectra in "
                + spectrumFactory.getMgfFileNames().size() + " file" + fileEnding + " using " + nThreads + " thread" + spectrumEnding + ".", true, true);
        waitingHandler.appendReportEndLine();

        for (File spectrumFile : spectrumFiles) {
            startSequencing(spectrumFile, searchParameters, outputFolder, exeTitle, waitingHandler, spectrumFiles.size() > 1);
            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        if (!waitingHandler.isRunCanceled()) {
            double elapsedTime = (System.nanoTime() - startTime) * 1.0e-9;
            System.out.println("Total sequencing time: " + Util.roundDouble(elapsedTime, 2) + " sec.");
            waitingHandler.appendReport("Total sequencing time: " + Util.roundDouble(elapsedTime, 2) + " sec.", true, true);
            waitingHandler.setRunFinished();
        }
    }

    /**
     * Starts the sequencing for a single file.
     *
     * @param spectrumFile the spectrum file to process
     * @param searchParameters the search parameters
     * @param outputFolder the output folder
     * @param exeTitle the name of the executable
     * @param waitingHandler the waiting handler
     * @param secondaryProgress if true the progress on the given file will be
     * displayed
     */
    private void startSequencing(File spectrumFile, SearchParameters searchParameters, File outputFolder, String exeTitle, WaitingHandler waitingHandler, boolean secondaryProgress) throws IOException {

        // Start a fixed thread pool
        threadExecutor = Executors.newFixedThreadPool(nThreads);

        // Job queue.
        jobs = new ArrayDeque<PepnovoJob>();
        try {
            int nSpectra = spectrumFactory.getNSpectra(spectrumFile.getName());
            int remaining = nSpectra % nThreads;
            int chunkSize = nSpectra / nThreads;
            String report = "Processing " + spectrumFile.getName() + " (" + nSpectra + " spectra, " + chunkSize;
            if (remaining > 0) {
                int maxSize = chunkSize + 1;
                report += "-" + maxSize;
            }
            report += " spectra per thread).";
            waitingHandler.appendReport(report, true, true);

            waitingHandler.appendReport("Preparing the spectra.", true, true);
            chunkFiles = FileProcessor.chunkFile(spectrumFile, chunkSize, remaining, nSpectra, waitingHandler);
            if (waitingHandler.isRunCanceled()) {
                return;
            }

            waitingHandler.setWaitingText("Processing " + spectrumFile.getName());
            if (secondaryProgress) {
                waitingHandler.resetSecondaryProgressBar();
                waitingHandler.setMaxSecondaryProgressValue(nSpectra);
            } else {
                waitingHandler.setSecondaryProgressDialogIndeterminate(true);
            }

            // Distribute the chunked spectra to the different jobs.
            for (File chunkFile : chunkFiles) {
                PepnovoJob job = new PepnovoJob(pepNovoFolder, exeTitle, chunkFile, outputFolder, searchParameters, waitingHandler);
                jobs.add(job);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReportEndLine();
        waitingHandler.appendReport("Starting de novo sequencing of " + spectrumFile.getName(), true, true);
        waitingHandler.appendReportEndLine();

        // Execute the jobs from the queue.
        Iterator<PepnovoJob> iterator = jobs.iterator();
        while (iterator.hasNext()) {
            PepnovoJob job = iterator.next();
            threadExecutor.execute(job);
            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }

        // Wait for executor service to shutdown.      
        threadExecutor.shutdown();
        try {
            threadExecutor.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException ex) {
            if (!waitingHandler.isRunCanceled()) {
                threadExecutor.shutdownNow();
                ex.printStackTrace();
            }
        }

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReportEndLine();
        waitingHandler.appendReport("Sequencing of " + spectrumFile.getName() + " finished.", true, true);
        waitingHandler.appendReportEndLine();
        waitingHandler.setSecondaryProgressDialogIndeterminate(true);

        FileProcessor.mergeAndDeleteOutputFiles(FileProcessor.getOutFiles(outputFolder, chunkFiles));

        // Delete the mgf file chunks.
        FileProcessor.deleteChunkMgfFiles(chunkFiles);
    }

    /**
     * Cancels the sequencing process.
     *
     * @throws IOException
     */
    public void cancelSequencing() throws IOException {
        if (jobs != null) {
            // cancel the jobs and delete temp .out files
            for (PepnovoJob job : jobs) {
                job.cancel();
            }
        }
        if (threadExecutor != null) {
            threadExecutor.shutdownNow();
            // Delete the mgf file chunks.
            FileProcessor.deleteChunkMgfFiles(chunkFiles);
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
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("DeNovoSequencingHandler.class").getPath(), "DeNovoGUI");
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

    /**
     * Get the number of threads to use for the processing.
     *
     * @return the mgfNSpectra
     */
    public int getNThreads() {
        return nThreads;
    }

    /**
     * Set the number of threads to use.
     *
     * @param nThreads the nThreads to set
     */
    public void setNThreads(int nThreads) {
        this.nThreads = nThreads;
    }
}
