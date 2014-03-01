package com.compomics.denovogui;

import com.compomics.denovogui.execution.Job;
import com.compomics.denovogui.execution.jobs.DirecTagJob;
import com.compomics.denovogui.execution.jobs.PepNovoJob;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.denovogui.io.ModificationFile;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.io.FileNotFoundException;
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
     * The DirecTag folder.
     */
    private File direcTagFolder;
    /**
     * If true, PepNovo will be run.
     */
    private boolean enablePepNovo = true;
    /**
     * If true, DirecTag will be used.
     */
    private boolean enableDirecTag = true;
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
     * The name of the parameters file saved by default.
     */
    public final static String parametersFileName = "denovoGUI.parameters";
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
    private Deque<Job> jobs;

    /**
     * Constructor.
     *
     * @param pepNovoFolder the PepNovo+ folder.
     * @param direcTagFolder the DirecTag folder.
     */
    public DeNovoSequencingHandler(File pepNovoFolder, File direcTagFolder) {
        this.pepNovoFolder = pepNovoFolder;
        this.direcTagFolder = direcTagFolder;
    }

    /**
     * Starts the sequencing for a list of files which will be processed
     * sequentially.
     *
     * @param spectrumFiles the spectrum files to process
     * @param searchParameters the search parameters
     * @param outputFolder the output folder
     * @param pepNovoExeTitle the name of the PepNovo+ executable
     * @param direcTagExeTitle the name of the DirecTag executable
     * @param enablePepNovo run PepNovo?
     * @param enableDirecTag run DirecTag?
     * @param waitingHandler the waiting handler
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void startSequencing(List<File> spectrumFiles, SearchParameters searchParameters, File outputFolder, String pepNovoExeTitle, String direcTagExeTitle,
            boolean enablePepNovo, boolean enableDirecTag, WaitingHandler waitingHandler) throws IOException, ClassNotFoundException {

        this.enablePepNovo = enablePepNovo;
        this.enableDirecTag = enableDirecTag;

        long startTime = System.nanoTime();
        waitingHandler.setMaxPrimaryProgressCounter(spectrumFactory.getNSpectra());
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        // store the pepnovo to utilities ptm mapping
        searchParameters.setPepNovoPtmMap(ModificationFile.getInvertedModIdMap());
        if (searchParameters.getParametersFile() != null) {
            SearchParameters.saveIdentificationParameters(searchParameters, searchParameters.getParametersFile());
        }

        // Write the modification file
        try {
            File folder = new File(pepNovoFolder, "Models");
            ModificationFile.writeFile(folder, searchParameters.getModificationProfile());
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while writing the modification file: " + e.getMessage(), true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            return;
        }

        // Back-up the parameters
        try {
            SearchParameters.saveIdentificationParameters(searchParameters, new File(outputFolder, parametersFileName));
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while writing the sequencing parameters: " + e.getMessage(), true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            return;
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
            startSequencing(spectrumFile, searchParameters, outputFolder, pepNovoExeTitle, direcTagExeTitle, waitingHandler, spectrumFiles.size() > 1);
            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        if (!waitingHandler.isRunCanceled()) {
            double elapsedTime = (System.nanoTime() - startTime) * 1.0e-9;
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
     * @param pepNovoExeTitle the name of the PepNovo+ executable
     * @param direcTagExeTitle the name of the DirecTag executable
     * @param waitingHandler the waiting handler
     * @param secondaryProgress if true the progress on the given file will be
     * displayed
     */
    private void startSequencing(File spectrumFile, SearchParameters searchParameters, File outputFolder, String pepNovoExeTitle, String direcTagExeTitle, WaitingHandler waitingHandler, boolean secondaryProgress) throws IOException {

        // Start a fixed thread pool
        threadExecutor = Executors.newFixedThreadPool(nThreads);

        // Job queue.
        jobs = new ArrayDeque<Job>();
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

            if (enablePepNovo) {
                waitingHandler.appendReport("Preparing the spectra.", true, true);
                chunkFiles = FileProcessor.chunkFile(spectrumFile, chunkSize, remaining, nSpectra, waitingHandler);
            }

            if (waitingHandler.isRunCanceled()) {
                return;
            }

            waitingHandler.setWaitingText("Processing " + spectrumFile.getName() + ".");
            if (secondaryProgress) {
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setMaxSecondaryProgressCounter(nSpectra);
            } else {
                waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            }

            // Distribute the chunked spectra to the different PepNovo+ jobs.
            if (enablePepNovo) {
                for (File chunkFile : chunkFiles) {
                    PepNovoJob pepNovoJob = new PepNovoJob(pepNovoFolder, pepNovoExeTitle, chunkFile, outputFolder, searchParameters, waitingHandler);
                    jobs.add(pepNovoJob);
                }
            }

            // Add the DirecTag job only once - multithreading is done in the application itself!
            if (enableDirecTag) {
                DirecTagJob direcTagJob = new DirecTagJob(direcTagFolder, direcTagExeTitle, spectrumFile, nThreads, outputFolder, searchParameters, waitingHandler);
                jobs.add(direcTagJob);
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
        waitingHandler.appendReport("Starting de novo sequencing of " + spectrumFile.getName() + ".", true, true);
        waitingHandler.appendReportEndLine();

        // Execute the jobs from the queue.
        Iterator<Job> iterator = jobs.iterator();
        while (iterator.hasNext()) {
            Job job = iterator.next();
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
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        if (enablePepNovo) {
            FileProcessor.mergeAndDeleteOutputFiles(FileProcessor.getOutFiles(outputFolder, chunkFiles));

            // Delete the mgf file chunks.
            FileProcessor.deleteChunkFiles(chunkFiles, waitingHandler);
        }
    }

    /**
     * Cancels the sequencing process.
     *
     * @param outputFolder the output folder
     * @param waitingHandler the waiting handler
     * @throws IOException
     */
    public void cancelSequencing(File outputFolder, WaitingHandler waitingHandler) throws IOException {
        if (jobs != null) {
            // cancel the jobs and delete temp .out files
            for (Job job : jobs) {
                job.cancel();
            }
        }
        if (threadExecutor != null) {
            threadExecutor.shutdown();
            try {
                threadExecutor.awaitTermination(12, TimeUnit.HOURS);
            } catch (InterruptedException ex) {
                if (waitingHandler.isRunCanceled()) {
                    threadExecutor.shutdownNow();
                    ex.printStackTrace();
                }
            }

            if (chunkFiles != null) {
                // Delete the output files.
                FileProcessor.deleteChunkFiles(FileProcessor.getOutFiles(outputFolder, chunkFiles), waitingHandler);

                // Delete the mgf file chunks.
                FileProcessor.deleteChunkFiles(chunkFiles, waitingHandler);
            }
        }
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
