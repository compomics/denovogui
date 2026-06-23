package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.execution.Job;
import com.compomics.denovogui.execution.JobStatus;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.identification.tool_specific.InstaNovoParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Runs InstaNovo and InstaNovo+.
 *
 * @author CompOmics
 */
public class InstaNovoJob extends Job {

    /**
     * The InstaNovo executable name.
     */
    public static final String EXECUTABLE_FILE_NAME = "instanovo";

    /**
     * InstaNovo execution modes.
     */
    public enum Mode {

        /**
         * Transformer based InstaNovo prediction.
         */
        transformer,
        /**
         * Diffusion based InstaNovo+ prediction.
         */
        diffusion,
        /**
         * InstaNovo prediction refined with InstaNovo+.
         */
        refined
    }

    /**
     * The InstaNovo checkout or installation folder.
     */
    private final File instaNovoFolder;
    /**
     * The spectrum file.
     */
    private final File spectrumFile;
    /**
     * The output folder.
     */
    private final File outputFolder;
    /**
     * The search parameters.
     */
    private final SearchParameters searchParameters;
    /**
     * The execution mode.
     */
    private final Mode mode;
    /**
     * The command executed.
     */
    private String command = "";

    /**
     * Constructor.
     *
     * @param instaNovoFolder the InstaNovo checkout or installation folder
     * @param spectrumFile the spectrum file
     * @param outputFolder the output folder
     * @param searchParameters the search parameters
     * @param mode the execution mode
     * @param waitingHandler the waiting handler
     * @param exceptionHandler the exception handler
     */
    public InstaNovoJob(
            File instaNovoFolder,
            File spectrumFile,
            File outputFolder,
            SearchParameters searchParameters,
            Mode mode,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler
    ) {

        this.instaNovoFolder = instaNovoFolder;
        this.spectrumFile = spectrumFile;
        this.outputFolder = outputFolder;
        this.searchParameters = searchParameters;
        this.mode = mode;
        this.waitingHandler = waitingHandler;
        this.exceptionHandler = exceptionHandler;
        initJob();
    }

    /**
     * Initializes the job command.
     */
    private void initJob() {

        try {
            InstaNovoParameters instaNovoParameters = getInstaNovoParameters();

            File executableFile = getExecutable(instaNovoFolder);
            procCommands.add(executableFile.getPath());

            if (mode == Mode.transformer) {
                procCommands.add("transformer");
                procCommands.add("predict");
                outputFile = FileProcessor.getInstaNovoResultFile(outputFolder, spectrumFile);
                setDescription("InstaNovo");
            } else if (mode == Mode.diffusion) {
                procCommands.add("diffusion");
                procCommands.add("predict");
                outputFile = FileProcessor.getInstaNovoPlusResultFile(outputFolder, spectrumFile);
                setDescription("InstaNovo+");
            } else {
                procCommands.add("predict");
                outputFile = FileProcessor.getInstaNovoRefinedResultFile(outputFolder, spectrumFile);
                setDescription("InstaNovo with refinement");
            }

            procCommands.add("--data-path");
            procCommands.add(spectrumFile.getAbsolutePath());
            procCommands.add("--output-path");
            procCommands.add(outputFile.getAbsolutePath());
            procCommands.add("--denovo");

            if (mode == Mode.transformer || mode == Mode.refined) {
                procCommands.add("--instanovo-model");
                procCommands.add(instaNovoParameters.getInstaNovoModel());
            }

            if (mode == Mode.diffusion || mode == Mode.refined) {
                procCommands.add("--instanovo-plus-model");
                procCommands.add(instaNovoParameters.getInstaNovoPlusModel());
            }

            if (mode == Mode.diffusion) {
                procCommands.add("--no-refinement");
            } else if (mode == Mode.refined) {
                procCommands.add("--with-refinement");
            }

            if (instaNovoParameters.getConfigFile() != null && !instaNovoParameters.getConfigFile().trim().isEmpty()) {
                procCommands.add("--config-path");
                procCommands.add(instaNovoParameters.getConfigFile());
            }

            procCommands.add("num_beams=" + instaNovoParameters.getNumberOfBeams());
            procCommands.add("use_knapsack=" + Boolean.toString(instaNovoParameters.isUseKnapsack()));
            procCommands.add("save_all_predictions=" + Boolean.toString(instaNovoParameters.isSaveAllPredictions()));
            procCommands.add("batch_size=" + instaNovoParameters.getBatchSize());
            procCommands.add("force_cpu=" + Boolean.toString(instaNovoParameters.isForceCpu()));
            procCommands.add("log_interval=1");

            procCommands.trimToSize();

            for (String commandComponent : procCommands) {
                if (!command.equals("")) {
                    command += " ";
                }
                command += commandComponent;
            }

            writeCommand();

            procBuilder = new ProcessBuilder(procCommands);
            if (instaNovoFolder != null && instaNovoFolder.exists()) {
                procBuilder.directory(instaNovoFolder);
            }
            procBuilder.redirectErrorStream(true);
        } catch (Exception e) {
            exceptionHandler.catchException(e);
            waitingHandler.appendReport("An error occurred running " + getDescription() + ". See error log for details. " + e.getMessage(), true, true);
            waitingHandler.setRunCanceled();
        }
    }

    /**
     * Returns the InstaNovo parameters.
     *
     * @return the InstaNovo parameters
     */
    private InstaNovoParameters getInstaNovoParameters() {

        Object parameters = searchParameters.getIdentificationAlgorithmParameter(Advocate.instanovo.getIndex());

        if (!(parameters instanceof InstaNovoParameters)) {
            parameters = searchParameters.getIdentificationAlgorithmParameter(Advocate.instanovoPlus.getIndex());
        }

        if (!(parameters instanceof InstaNovoParameters)) {
            parameters = searchParameters.getIdentificationAlgorithmParameter(Advocate.instanovoRefined.getIndex());
        }

        return parameters instanceof InstaNovoParameters ? (InstaNovoParameters) parameters : new InstaNovoParameters();
    }

    /**
     * Returns the executable to run.
     *
     * @param instaNovoFolder the InstaNovo checkout or installation folder
     * @return the executable
     */
    public static File getExecutable(File instaNovoFolder) {

        if (instaNovoFolder != null) {

            String[] relativeExecutablePaths = {
                ".venv" + File.separator + "bin" + File.separator + EXECUTABLE_FILE_NAME,
                ".venv" + File.separator + "Scripts" + File.separator + EXECUTABLE_FILE_NAME,
                ".venv" + File.separator + "Scripts" + File.separator + EXECUTABLE_FILE_NAME + ".exe",
                EXECUTABLE_FILE_NAME,
                EXECUTABLE_FILE_NAME + ".exe"
            };

            for (String relativeExecutablePath : relativeExecutablePaths) {

                File executable = new File(instaNovoFolder, relativeExecutablePath);

                if (executable.exists()) {
                    executable.setExecutable(true);
                    return executable;
                }
            }
        }

        return new File(EXECUTABLE_FILE_NAME);
    }

    /**
     * Returns the command line used by this job.
     *
     * @return the command line
     */
    public List<String> getCommand() {
        return new ArrayList<String>(procCommands);
    }

    @Override
    public void run() {

        proc = null;

        try {
            proc = procBuilder.start();
            setStatus(JobStatus.RUNNING);
        } catch (IOException e) {
            setStatus(JobStatus.ERROR);
            setError(e.getMessage());
            waitingHandler.appendReport("Could not start " + getDescription() + "!", true, true);
            waitingHandler.appendReportEndLine();
            waitingHandler.setRunCanceled();
            exceptionHandler.catchException(e);
            return;
        }

        Scanner scanner = new Scanner(proc.getInputStream());

        try {
            while (scanner.hasNextLine() && !waitingHandler.isRunCanceled()) {
                waitingHandler.appendReport(scanner.nextLine(), false, true);
            }

            int exitCode = proc.waitFor();

            if (exitCode == 0 && !waitingHandler.isRunCanceled()) {
                setStatus(JobStatus.FINISHED);
            } else if (!waitingHandler.isRunCanceled()) {
                String errorMessage = getDescription() + " exited with status " + exitCode + ".";
                setStatus(JobStatus.ERROR);
                setError(errorMessage);
                waitingHandler.appendReport(errorMessage, true, true);
                waitingHandler.setRunCanceled();
            }
        } catch (InterruptedException e) {
            if (!waitingHandler.isRunCanceled()) {
                setStatus(JobStatus.ERROR);
                setError(e.getMessage());
                exceptionHandler.catchException(e);
                waitingHandler.setRunCanceled();
            }
        } finally {
            scanner.close();
            if (waitingHandler.isRunCanceled() && proc != null) {
                proc.destroy();
            }
        }
    }

    @Override
    public void cancel() {
        if (proc != null) {
            proc.destroy();
            log.info(">> De novo sequencing has been canceled.");
        }
    }

    @Override
    public void writeCommand() {
        System.out.println(System.getProperty("line.separator") + System.getProperty("line.separator") + getDescription() + " command: " + command + System.getProperty("line.separator"));
    }
}
