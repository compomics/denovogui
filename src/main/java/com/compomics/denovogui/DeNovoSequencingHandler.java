package com.compomics.denovogui;

import com.compomics.denovogui.io.SpectrumFactory;

import com.compomics.denovogui.execution.Job;
import com.compomics.denovogui.execution.jobs.DirecTagJob;
import com.compomics.denovogui.execution.jobs.InstaNovoJob;
import com.compomics.denovogui.execution.jobs.NovorJob;
import com.compomics.denovogui.execution.jobs.PNovoJob;
import com.compomics.denovogui.execution.jobs.PepNovoJob;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.denovogui.io.PepNovoModificationFile;
import com.compomics.denovogui.util.Properties;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.identification.tool_specific.PepnovoParameters;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.waiting.Duration;
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
     * The pNovo folder.
     */
    private File pNovoFolder;
    /**
     * The Novor folder.
     */
    private File novorFolder;
    /**
     * The InstaNovo folder.
     */
    private File instaNovoFolder;
    /**
     * If true, PepNovo will be run.
     */
    private boolean enablePepNovo = true;
    /**
     * If true, DirecTag will be used.
     */
    private boolean enableDirecTag = true;
    /**
     * If true, pNovo+ will be run.
     */
    private boolean enablePNovo = false;
    /**
     * If true, Novor will be run.
     */
    private boolean enableNovor = false;
    /**
     * If true, InstaNovo will be run.
     */
    private boolean enableInstaNovo = false;
    /**
     * If true, InstaNovo+ will be run.
     */
    private boolean enableInstaNovoPlus = false;
    /**
     * If true, InstaNovo with refinement will be run.
     */
    private boolean enableInstaNovoRefined = false;
    /**
     * Default Modification selection.
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
     * The chunk files.
     */
    private ArrayList<File> chunkFiles;
    /**
     * Number of threads to use for the processing.
     */
    private int nThreads = Runtime.getRuntime().availableProcessors(); // @TODO: should be moved to user preferences?
    /**
     * The thread executor.
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
     * The factory used to handle the modifications.
     */
    private ModificationFactory ptmFactory = ModificationFactory.getInstance();

    /**
     * Constructor.
     *
     * @param pepNovoFolder the PepNovo+ folder
     * @param direcTagFolder the DirecTag folder
     * @param pNovoFolder the pNovo folder
     * @param novorFolder the Novor folder
     */
    public DeNovoSequencingHandler(File pepNovoFolder, File direcTagFolder, File pNovoFolder, File novorFolder) {
        this(pepNovoFolder, direcTagFolder, pNovoFolder, novorFolder, null);
    }

    /**
     * Constructor.
     *
     * @param pepNovoFolder the PepNovo+ folder
     * @param direcTagFolder the DirecTag folder
     * @param pNovoFolder the pNovo folder
     * @param novorFolder the Novor folder
     * @param instaNovoFolder the InstaNovo folder
     */
    public DeNovoSequencingHandler(File pepNovoFolder, File direcTagFolder, File pNovoFolder, File novorFolder, File instaNovoFolder) {
        this.pepNovoFolder = pepNovoFolder;
        this.direcTagFolder = direcTagFolder;
        this.pNovoFolder = pNovoFolder;
        this.novorFolder = novorFolder;
        this.instaNovoFolder = instaNovoFolder;
    }

    /**
     * Starts the sequencing for a list of files which will be processed
     * sequentially.
     *
     * @param spectrumFiles the spectrum files to process
     * @param searchParameters the search parameters
     * @param outputFolder the output folder
     * @param searchParametersFile the file where to save the search parameters
     * @param pepNovoExeTitle the name of the PepNovo+ executable
     * @param direcTagExeTitle the name of the DirecTag executable
     * @param pNovoExeTitle the name of the pNovo+ executable
     * @param novorExeTitle the name of the Novor executable
     * @param enablePepNovo run PepNovo?
     * @param enableDirecTag run DirecTag?
     * @param enablePNovo run pNovo?
     * @param enableNovor run Novor?
     * @param waitingHandler the waiting handler
     * @param exceptionHandler the exception handler to use when an exception is
     * caught
     *
     * @throws IOException thrown if there is an IO issue
     * @throws ClassNotFoundException thrown if the search parameters cannot be
     * processed from file
     */
    public void startSequencing(List<File> spectrumFiles, SearchParameters searchParameters, File outputFolder, File searchParametersFile, String pepNovoExeTitle, String direcTagExeTitle, String pNovoExeTitle, String novorExeTitle,
            boolean enablePepNovo, boolean enableDirecTag, boolean enablePNovo, boolean enableNovor, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) throws IOException, ClassNotFoundException {
        startSequencing(spectrumFiles, searchParameters, outputFolder, searchParametersFile, pepNovoExeTitle, direcTagExeTitle, pNovoExeTitle, novorExeTitle,
                enablePepNovo, enableDirecTag, enablePNovo, enableNovor, false, false, false, waitingHandler, exceptionHandler);
    }

    /**
     * Starts the sequencing for a list of files which will be processed
     * sequentially.
     *
     * @param spectrumFiles the spectrum files to process
     * @param searchParameters the search parameters
     * @param outputFolder the output folder
     * @param searchParametersFile the file where to save the search parameters
     * @param pepNovoExeTitle the name of the PepNovo+ executable
     * @param direcTagExeTitle the name of the DirecTag executable
     * @param pNovoExeTitle the name of the pNovo+ executable
     * @param novorExeTitle the name of the Novor executable
     * @param enablePepNovo run PepNovo?
     * @param enableDirecTag run DirecTag?
     * @param enablePNovo run pNovo?
     * @param enableNovor run Novor?
     * @param enableInstaNovo run InstaNovo?
     * @param enableInstaNovoPlus run InstaNovo+?
     * @param enableInstaNovoRefined run InstaNovo with refinement?
     * @param waitingHandler the waiting handler
     * @param exceptionHandler the exception handler to use when an exception is
     * caught
     *
     * @throws IOException thrown if there is an IO issue
     * @throws ClassNotFoundException thrown if the search parameters cannot be
     * processed from file
     */
    public void startSequencing(List<File> spectrumFiles, SearchParameters searchParameters, File outputFolder, File searchParametersFile, String pepNovoExeTitle, String direcTagExeTitle, String pNovoExeTitle, String novorExeTitle,
            boolean enablePepNovo, boolean enableDirecTag, boolean enablePNovo, boolean enableNovor,
            boolean enableInstaNovo, boolean enableInstaNovoPlus, boolean enableInstaNovoRefined,
            WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) throws IOException, ClassNotFoundException {

        this.enablePepNovo = enablePepNovo;
        this.enableDirecTag = enableDirecTag;
        this.enablePNovo = enablePNovo;
        this.enableNovor = enableNovor;
        this.enableInstaNovo = enableInstaNovo;
        this.enableInstaNovoPlus = enableInstaNovoPlus;
        this.enableInstaNovoRefined = enableInstaNovoRefined;
        this.exceptionHandler = exceptionHandler;

        int numberOfSpectrumFiles = spectrumFiles.size();
        int primaryProgressCounterMax = 1;
        if (enablePepNovo) {
            primaryProgressCounterMax += numberOfSpectrumFiles + 1;
            // write the modification file
            try {
                File folder = new File(pepNovoFolder, "Models");
                PepNovoModificationFile.writeFile(folder, searchParameters.getModificationParameters());
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while writing the modification file: " + e.getMessage(), true, true);
                exceptionHandler.catchException(e);
                waitingHandler.setRunCanceled();
                return;
            }
        }
        if (enableDirecTag) {
            primaryProgressCounterMax += numberOfSpectrumFiles;
        }
        if (enablePNovo) {
            primaryProgressCounterMax += numberOfSpectrumFiles;
        }
        if (enableNovor) {
            primaryProgressCounterMax += numberOfSpectrumFiles;
        }
        if (enableInstaNovo) {
            primaryProgressCounterMax += numberOfSpectrumFiles;
        }
        if (enableInstaNovoPlus) {
            primaryProgressCounterMax += numberOfSpectrumFiles;
        }
        if (enableInstaNovoRefined) {
            primaryProgressCounterMax += numberOfSpectrumFiles;
        }

        waitingHandler.setMaxPrimaryProgressCounter(primaryProgressCounterMax);
        waitingHandler.increasePrimaryProgressCounter();
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        // get the number of available threads
        String fileEnding = "";
        if (spectrumFactory.getMgfFileNames().size() > 1) {
            fileEnding = "s";
        }
        String threadEnding = "";
        if (nThreads > 1) {
            threadEnding = "s";
        }
        
        // set this version as the default DeNovoGUI version
        if (!getJarFilePath().equalsIgnoreCase(".")) {
            UtilitiesUserParameters utilitiesUserParameters = UtilitiesUserParameters.loadUserParameters();
            String versionNumber = Properties.getVersion();
            utilitiesUserParameters.setDeNovoGuiPath(new File(getJarFilePath(), "DeNovoGUI-" + versionNumber + ".jar").getAbsolutePath());
            UtilitiesUserParameters.saveUserParameters(utilitiesUserParameters);
        }

        Duration duration = new Duration();
        duration.start();

        waitingHandler.appendReport("Starting de novo sequencing: " + spectrumFactory.getNSpectra() + " spectra in "
                + spectrumFactory.getMgfFileNames().size() + " file" + fileEnding + " using " + nThreads + " thread" + threadEnding + ".", true, true);
        waitingHandler.appendReportEndLine();

        for (File spectrumFile : spectrumFiles) {
            startSequencing(spectrumFile, searchParameters, outputFolder, pepNovoExeTitle, direcTagExeTitle, pNovoExeTitle, novorExeTitle, waitingHandler);
            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        if (!waitingHandler.isRunCanceled()) {
            duration.end();
            waitingHandler.appendReport("De novo sequencing completed (" + duration.toString() + ").", true, true);

            // store the pepnovo to utilities ptm mapping
            PepnovoParameters pepnovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex());
            pepnovoParameters.setPepNovoPtmMap(PepNovoModificationFile.getInvertedModIdMap());

            // make sure that the ptm mappings are saved, also for novor and pnovo
            if (searchParametersFile != null) {
                SearchParameters.saveIdentificationParameters(searchParameters, searchParametersFile);
            }

            // check if we have any output files
            ArrayList<File> resultFiles = FileProcessor.getAllResultFiles(outputFolder, spectrumFiles,
                    enablePepNovo, enableDirecTag, enablePNovo, enableNovor,
                    enableInstaNovo, enableInstaNovoPlus, enableInstaNovoRefined);

            if (resultFiles.isEmpty()) {
                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("The de novo sequencing did not generate any output files!", true, true);
                waitingHandler.setRunCanceled();
            } else {
                waitingHandler.setRunFinished();
            }
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
     * @param pNovoExeTitle the name of the pNovo+ executable
     * @param novorExeTitle the name of the Novor executable
     * @param waitingHandler the waiting handler
     * @param secondaryProgress if true the progress on the given file will be
     * displayed
     */
    private void startSequencing(File spectrumFile, SearchParameters searchParameters, File outputFolder, String pepNovoExeTitle,
            String direcTagExeTitle, String pNovoExeTitle, String novorExeTitle, WaitingHandler waitingHandler) throws IOException {

        try {
            jobs = new ArrayDeque<Job>();

            // InstaNovo
            if (enableInstaNovo && !waitingHandler.isRunCanceled()) {
                runInstaNovo(spectrumFile, searchParameters, outputFolder, waitingHandler, InstaNovoJob.Mode.transformer, "InstaNovo");
            }

            // InstaNovo+
            if (enableInstaNovoPlus && !waitingHandler.isRunCanceled()) {
                runInstaNovo(spectrumFile, searchParameters, outputFolder, waitingHandler, InstaNovoJob.Mode.diffusion, "InstaNovo+");
            }

            // InstaNovo with refinement
            if (enableInstaNovoRefined && !waitingHandler.isRunCanceled()) {
                runInstaNovo(spectrumFile, searchParameters, outputFolder, waitingHandler, InstaNovoJob.Mode.refined, "InstaNovo with refinement");
            }

            // Novor
            if (enableNovor && !waitingHandler.isRunCanceled()) {

                Duration algorithmDuration = new Duration();
                algorithmDuration.start();

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using Novor.", true, true);
                waitingHandler.appendReportEndLine();

                threadExecutor = Executors.newFixedThreadPool(1);
                NovorJob novorJob = new NovorJob(novorFolder, spectrumFile, outputFolder, searchParameters, waitingHandler instanceof WaitingHandlerCLIImpl, waitingHandler, exceptionHandler);
                threadExecutor.submit(novorJob);
                jobs.add(novorJob);

                // wait for executor service to shutdown
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

                algorithmDuration.end();

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using Novor completed (" + algorithmDuration.toString() + ").", true, true);
                waitingHandler.appendReportEndLine();
                waitingHandler.increasePrimaryProgressCounter();
            }

            // DirecTag
            if (enableDirecTag && !waitingHandler.isRunCanceled()) {

                Duration algorithmDuration = new Duration();
                algorithmDuration.start();

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using DirecTag.", true, true);
                waitingHandler.appendReportEndLine();

                threadExecutor = Executors.newFixedThreadPool(1);
                DirecTagJob direcTagJob = new DirecTagJob(direcTagFolder, direcTagExeTitle, spectrumFile, nThreads, outputFolder, searchParameters, waitingHandler, exceptionHandler);
                threadExecutor.submit(direcTagJob);
                jobs.add(direcTagJob);

                // wait for executor service to shutdown
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

                algorithmDuration.end();

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using DirecTag completed (" + algorithmDuration.toString() + ").", true, true);
                waitingHandler.appendReportEndLine();
                waitingHandler.increasePrimaryProgressCounter();
            }

            // PepNovo+
            if (enablePepNovo && !waitingHandler.isRunCanceled()) {

                Duration algorithmDuration = new Duration();
                algorithmDuration.start();

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using PepNovo+.", true, true);
                waitingHandler.appendReportEndLine();

                // start a fixed thread pool
                threadExecutor = Executors.newFixedThreadPool(nThreads);

                // job queue
                int nSpectra = spectrumFactory.getNSpectra(spectrumFile.getName());
                if (nThreads > 1) {
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
                }

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                waitingHandler.increasePrimaryProgressCounter();

                waitingHandler.setWaitingText("Processing " + spectrumFile.getName() + ".");
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setMaxSecondaryProgressCounter(nSpectra);

                // verify that the file is chunked and use the entire if not
                boolean chunksuccess = true;
                for (File chunkFile : chunkFiles) {
                    if (!chunkFile.exists()) {
                        chunksuccess = false;
                        waitingHandler.appendReport("Processing of the spectra failed. Only one thread will be used for PepNovo+.", true, true);
                    }
                }

                // distribute the chunked spectra to the different PepNovo+ jobs
                if (chunksuccess) {
                    for (File chunkFile : chunkFiles) {
                        PepNovoJob pepNovoJob = new PepNovoJob(pepNovoFolder, pepNovoExeTitle, chunkFile, outputFolder, searchParameters, waitingHandler, exceptionHandler);
                        threadExecutor.submit(pepNovoJob);
                        jobs.add(pepNovoJob);
                    }
                } else {
                    PepNovoJob pepNovoJob = new PepNovoJob(pepNovoFolder, pepNovoExeTitle, spectrumFile, outputFolder, searchParameters, waitingHandler, exceptionHandler);
                    threadExecutor.submit(pepNovoJob);
                    jobs.add(pepNovoJob);
                }

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                // wait for executor service to shutdown
                threadExecutor.shutdown();

                try {
                    threadExecutor.awaitTermination(12, TimeUnit.HOURS);
                } catch (InterruptedException ex) {
                    if (!waitingHandler.isRunCanceled()) {
                        threadExecutor.shutdownNow();
                        exceptionHandler.catchException(ex);
                    }
                }

                waitingHandler.setSecondaryProgressCounterIndeterminate(true);

                FileProcessor.mergeAndDeleteOutputFiles(FileProcessor.getOutFiles(outputFolder, chunkFiles));

                // delete the mgf file chunks
                FileProcessor.deleteChunkFiles(chunkFiles, waitingHandler);

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                algorithmDuration.end();

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using PepNovo+ completed (" + algorithmDuration.toString() + ").", true, true);
                waitingHandler.appendReportEndLine();
                waitingHandler.increasePrimaryProgressCounter();
            }

            // pNovo+
            if (enablePNovo && !waitingHandler.isRunCanceled()) {
                
                Duration algorithmDuration = new Duration();
                algorithmDuration.start();

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using pNovo+.", true, true);
                waitingHandler.appendReportEndLine();

                threadExecutor = Executors.newFixedThreadPool(1); // @TODO: implement chunking so that pNovo can be run with more than one thread...
                PNovoJob pNovoJob = new PNovoJob(pNovoFolder, pNovoExeTitle, spectrumFile, 1, outputFolder, searchParameters, waitingHandler, exceptionHandler);
                threadExecutor.submit(pNovoJob);
                jobs.add(pNovoJob);

                // wait for executor service to shutdown
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

                algorithmDuration.end();

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using pNovo+ completed (" + algorithmDuration.toString() + ").", true, true);
                waitingHandler.appendReportEndLine();
                waitingHandler.increasePrimaryProgressCounter();
            }

        } catch (FileNotFoundException ex) {
            exceptionHandler.catchException(ex);
        } catch (IOException ex) {
            exceptionHandler.catchException(ex);
        }
    }

    /**
     * Runs an InstaNovo job.
     *
     * @param spectrumFile the spectrum file
     * @param searchParameters the search parameters
     * @param outputFolder the output folder
     * @param waitingHandler the waiting handler
     * @param mode the execution mode
     * @param name the display name
     */
    private void runInstaNovo(File spectrumFile, SearchParameters searchParameters, File outputFolder, WaitingHandler waitingHandler,
            InstaNovoJob.Mode mode, String name) {

        Duration algorithmDuration = new Duration();
        algorithmDuration.start();

        waitingHandler.appendReportEndLine();
        waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using " + name + ".", true, true);
        waitingHandler.appendReportEndLine();

        threadExecutor = Executors.newFixedThreadPool(1);
        InstaNovoJob instaNovoJob = new InstaNovoJob(instaNovoFolder, spectrumFile, outputFolder, searchParameters, mode, waitingHandler, exceptionHandler);
        threadExecutor.submit(instaNovoJob);
        jobs.add(instaNovoJob);

        threadExecutor.shutdown();

        try {
            threadExecutor.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException ex) {
            if (!waitingHandler.isRunCanceled()) {
                threadExecutor.shutdownNow();
                exceptionHandler.catchException(ex);
            }
        }

        if (!waitingHandler.isRunCanceled()) {
            algorithmDuration.end();

            waitingHandler.appendReportEndLine();
            waitingHandler.appendReport("Sequencing " + spectrumFile.getName() + " using " + name + " completed (" + algorithmDuration.toString() + ").", true, true);
            waitingHandler.appendReportEndLine();
            waitingHandler.increasePrimaryProgressCounter();
        }
    }

    /**
     * Cancels the sequencing process.
     *
     * @param outputFolder the output folder
     * @param waitingHandler the waiting handler
     * @throws IOException thrown if the deletion of the chunk files fail
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
                // delete the output files
                FileProcessor.deleteChunkFiles(FileProcessor.getOutFiles(outputFolder, chunkFiles), waitingHandler);

                // delete the mgf file chunks
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

    /**
     * Verifies that the modifications backed-up in the search parameters are
     * loaded and returns an error message if one was already loaded, null
     * otherwise.
     *
     * @param searchParameters the search parameters to load
     * @return an error message if one was already loaded, null otherwise
     */
    public static String loadModifications(SearchParameters searchParameters) {
        String error = null;
        ArrayList<String> toCheck = ModificationFactory.getInstance().loadBackedUpModifications(searchParameters, true);
        if (!toCheck.isEmpty()) {
            error = "The definition of the following Modification(s) seems to have changed and were overwritten:\n";
            for (int i = 0; i < toCheck.size(); i++) {
                if (i > 0) {
                    if (i < toCheck.size() - 1) {
                        error += ", ";
                    } else {
                        error += " and ";
                    }
                }
                error += toCheck.get(i);
            }
            error += ".\nPlease verify the definition of the Modification(s) in the modifications editor.";
        }
        return error;
    }
}
