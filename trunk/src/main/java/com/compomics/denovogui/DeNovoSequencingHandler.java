package com.compomics.denovogui;

import com.compomics.denovogui.execution.Job;
import com.compomics.denovogui.execution.jobs.DirecTagJob;
import com.compomics.denovogui.execution.jobs.PepNovoJob;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.denovogui.io.ModificationFile;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.identification_parameters.PepnovoParameters;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
    private static String MODIFICATIONS_FILE = "resources/conf/denovogui_mods.xml";
    /**
     * User modification file.
     */
    private static String USER_MODIFICATIONS_FILE = "resources/conf/denovogui_usermods.xml";
    /**
     * The enzyme file.
     */
    private static String ENZYME_FILE = "resources/conf/enzymes.xml";
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
     * The exception handler.
     */
    private ExceptionHandler exceptionHandler;

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
     * @param exceptionHandler the exception handler to use when an exception is
     * caught
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void startSequencing(List<File> spectrumFiles, SearchParameters searchParameters, File outputFolder, String pepNovoExeTitle, String direcTagExeTitle,
            boolean enablePepNovo, boolean enableDirecTag, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) throws IOException, ClassNotFoundException {

        this.enablePepNovo = enablePepNovo;
        this.enableDirecTag = enableDirecTag;
        this.exceptionHandler = exceptionHandler;

        long startTime = System.nanoTime();
        waitingHandler.setMaxPrimaryProgressCounter(spectrumFactory.getNSpectra() + 2);
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        // store the pepnovo to utilities ptm mapping
        PepnovoParameters pepnovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex());
        pepnovoParameters.setPepNovoPtmMap(ModificationFile.getInvertedModIdMap());

        if (searchParameters.getParametersFile() != null) {
            SearchParameters.saveIdentificationParameters(searchParameters, searchParameters.getParametersFile());
        }

        // Write the modification file
        try {
            File folder = new File(pepNovoFolder, "Models");
            ModificationFile.writeFile(folder, searchParameters.getModificationProfile());
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while writing the modification file: " + e.getMessage(), true, true);
            exceptionHandler.catchException(e);
            waitingHandler.setRunCanceled();
            return;
        }

        // Back-up the parameters
        try {
            SearchParameters.saveIdentificationParameters(searchParameters, new File(outputFolder, parametersFileName));
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while writing the sequencing parameters: " + e.getMessage(), true, true);
            exceptionHandler.catchException(e);
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
        waitingHandler.increasePrimaryProgressCounter();

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

            // Verify that the file is chunked and use the entire if not
            boolean chunksuccess = true;
            if (enablePepNovo) {
                for (File chunkFile : chunkFiles) {
                    if (!chunkFile.exists()) {
                        chunksuccess = false;
                        waitingHandler.appendReport("Processing of the spectra failed. Only one thread will be used for PepNovo+.", true, true);
                    }
                }
            }

            if (!chunksuccess || enableDirecTag) {
                if (!spectrumFile.exists()) {
                    waitingHandler.appendReport("Spectrum file not found.", true, true);
                    return;
                }
            }

            // Distribute the chunked spectra to the different PepNovo+ jobs.
            if (enablePepNovo) {
                if (chunksuccess) {
                    for (File chunkFile : chunkFiles) {
                        PepNovoJob pepNovoJob = new PepNovoJob(pepNovoFolder, pepNovoExeTitle, chunkFile, outputFolder, searchParameters, waitingHandler, exceptionHandler);
                        jobs.add(pepNovoJob);
                    }
                } else {
                    PepNovoJob pepNovoJob = new PepNovoJob(pepNovoFolder, pepNovoExeTitle, spectrumFile, outputFolder, searchParameters, waitingHandler, exceptionHandler);
                    jobs.add(pepNovoJob);
                }
            }

            // Add the DirecTag job only once - multithreading is done in the application itself!
            if (enableDirecTag) {
                DirecTagJob direcTagJob = new DirecTagJob(direcTagFolder, direcTagExeTitle, spectrumFile, nThreads, outputFolder, searchParameters, waitingHandler, exceptionHandler);
                jobs.add(direcTagJob);
            }

        } catch (FileNotFoundException ex) {
            exceptionHandler.catchException(ex);
            return;
        } catch (IOException ex) {
            exceptionHandler.catchException(ex);
            return;
        }

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReportEndLine();
        waitingHandler.appendReport("Starting de novo sequencing of " + spectrumFile.getName() + ".", true, true);
        waitingHandler.appendReportEndLine();

        // Execute the jobs from the queue.
        for (Job job : jobs) {
            job.writeCommand();
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
                exceptionHandler.catchException(ex);
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
        File result = new File(jarFolder, MODIFICATIONS_FILE);
        if (!result.exists()) {
            JOptionPane.showMessageDialog(null, MODIFICATIONS_FILE + " not found.", "Modification File Error", JOptionPane.ERROR_MESSAGE);
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
        File result = new File(jarFolder, USER_MODIFICATIONS_FILE);
        if (!result.exists()) {
            JOptionPane.showMessageDialog(null, USER_MODIFICATIONS_FILE + " not found.", "User Modification File Error", JOptionPane.ERROR_MESSAGE);
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

    /**
     * Returns the file containing the enzymes.
     *
     * @return the file containing the enzymes
     */
    public static String getEnzymeFile() {
        return ENZYME_FILE;
    }

    /**
     * Sets the file containing the enzymes.
     *
     * @param enzymeFile the file containing the enzymes
     */
    public static void setEnzymeFile(String enzymeFile) {
        DeNovoSequencingHandler.ENZYME_FILE = enzymeFile;
    }

    /**
     * Returns the file used for default modifications pre-loading.
     *
     * @return the file used for default modifications pre-loading
     */
    public static String getDefaultModificationFile() {
        return MODIFICATIONS_FILE;
    }

    /**
     * Sets the file used for default modifications pre-loading.
     *
     * @param modificationFile the file used for default modifications
     * pre-loading
     */
    public static void setDefaultModificationFile(String modificationFile) {
        DeNovoSequencingHandler.MODIFICATIONS_FILE = modificationFile;
    }

    /**
     * Returns the file used for user modifications pre-loading.
     *
     * @return the file used for user modifications pre-loading
     */
    public static String getUserModificationFile() {
        return USER_MODIFICATIONS_FILE;
    }

    /**
     * Sets the file used for user modifications pre-loading.
     *
     * @param modificationFile the file used for user modifications pre-loading
     */
    public static void setUserModificationFile(String modificationFile) {
        DeNovoSequencingHandler.USER_MODIFICATIONS_FILE = modificationFile;
    }
}
