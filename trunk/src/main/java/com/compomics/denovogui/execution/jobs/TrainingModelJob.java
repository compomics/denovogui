package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.execution.Job;
import com.compomics.denovogui.io.ModificationFile;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.gui.waiting.WaitingHandler;
import java.io.File;
import java.util.ArrayList;

/**
 * <b>TrainingModelJob</b> <p> This job class runs the PepNovo+ model
 * training.</p>
 *
 * @author Thilo Muth
 */
public class TrainingModelJob extends Job {

    /**
     * Name of the PepNovo+ executable.
     */
    public final static String PEPNOVO_EXE = "PepNovo.exe";
    /**
     * The file contains the paths to good training files.
     */
    private File goodTrainingFile;
    /**
     * The file contains the paths to bad / crap files.
     */
    private File badTrainingFile;
    /**
     * The search parameters.
     */
    private SearchParameters searchParameters;
    /**
     * The path to the PepNovo executable.
     */
    private File pepNovoFolder;
    /**
     * The output path.
     */
    private File outputFolder;
    /**
     * The name of the (newly trained) model.
     */
    private String modelName;

    /**
     * Constructor for the TrainingModelJob.
     *
     * @param pepNovoFolder The path to the PepNovo executable.
     * @param goodTrainingFile The good training file.
     * @param badTrainingFile The bad training file.
     * @param outputFolder The output folder.
     * @param modelName The name of the (trained) model.
     * @param searchParameters The search parameters.
     * @param waitingHandler the waiting handler
     */
    public TrainingModelJob(File pepNovoFolder, File goodTrainingFile, File badTrainingFile, String modelName, File outputFolder, SearchParameters searchParameters, WaitingHandler waitingHandler) {
        this.pepNovoFolder = pepNovoFolder;
        this.goodTrainingFile = goodTrainingFile;
        this.badTrainingFile = badTrainingFile;
        this.modelName = modelName;
        this.outputFolder = outputFolder;
        this.searchParameters = searchParameters;
        this.waitingHandler = waitingHandler;
        initJob();
    }

    /**
     * Initializes the job, setting up the commands for the ProcessBuilder.
     */
    private void initJob() {

        // full path to executable
        procCommands.add(pepNovoFolder.getAbsolutePath() + "/" + PEPNOVO_EXE);
        procCommands.trimToSize();

        // Add training tolerance
        procCommands.add("-train_model");

        // Add estimated training fragment ion tolerance.
        procCommands.add("-train_tolerance");
        procCommands.add(String.valueOf(searchParameters.getFragmentIonAccuracy()));

        // Link to the good training file
        procCommands.add("-list");
        procCommands.add(goodTrainingFile.getAbsolutePath());

        procCommands.add("-neg_spec_list");
        procCommands.add(badTrainingFile.getAbsolutePath());

        /**
         * You can specify specific start and end training stages with the flags
         * -strat_train_idx and -end_train_idx (you might not want or need to
         * train all model types). The relevant training steps described in this
         * document are: 0 Partitioning according to size and charge (depending
         * on amount of training data) 1 Choosing set of fragment ion types 2
         * Precursor ion and fragment ion mass tolerances 3 Sequence Quality
         * Score models (SQS) 4 Precursor Mass Correction models (PMCR) 5
         * Breakage score models (PRM node scores) 6 PRM score normalizers 7
         * Edge score models
         */
        // Model start step == 0
        procCommands.add("-start_train_idx");
        // TODO: Hard-coded value
        procCommands.add(String.valueOf(0));

        // Model end step == 7         
        procCommands.add("-end_train_idx");
        // TODO: Hard-coded value
        procCommands.add(String.valueOf(7));

        // Add Model
        procCommands.add("-model");
        procCommands.add(modelName);

        // Add modifications
        ArrayList<String> modifications = searchParameters.getModificationProfile().getAllModifications();
        if (!modifications.isEmpty()) {
            procCommands.add("-PTMs");
            procCommands.add(ModificationFile.getModsString(searchParameters.getModificationProfile().getAllModifications()));
        }

        //TODO: Use output path here or is the model automatically written to the model folder ?
        //outputFile = getOutputFile(outputFolder, Util.getFileName(spectrumFile));

        procCommands.trimToSize();

        // Set the description - yet not used
        setDescription("MODEL TRAINING");
        procBuilder = new ProcessBuilder(procCommands);
        procBuilder.directory(pepNovoFolder);

        // set error out and std out to same stream
        procBuilder.redirectErrorStream(true);
    }

    /**
     * Cancels the job by destroying the process.
     */
    @Override
    public void cancel() {
        if (proc != null) {
            proc.destroy();
            log.info(">> Model training has been canceled.");
        }
    }

    @Override
    public void run() {
        super.run();
    }
}
