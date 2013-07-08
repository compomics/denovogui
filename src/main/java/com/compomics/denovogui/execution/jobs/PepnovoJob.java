package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.execution.Job;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.denovogui.io.ModificationFile;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.util.ArrayList;

/**
 * <b>PepnovoJob</b> <p> This job class runs the PepNovo+ software in wrapper
 * mode. </p>
 *
 * @author Thilo Muth
 */
public class PepnovoJob extends Job {

    /**
     * Title of the PepNovo+ executable.
     */
    public String exeTitle;
    /**
     * The spectrumFile file.
     */
    private File spectrumFile;
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
     * Constructor for the PepnovoJob.
     *
     * @param pepNovoFolder The path to the PepNovo executable.
     * @param exeTitle Title of the PepNovo executable.
     * @param mgfFile The spectrum MGF file.
     * @param outputFolder The output folder.
     * @param searchParameters The search parameters.
     * @param waitingHandler the waiting handler
     */
    public PepnovoJob(File pepNovoFolder, String exeTitle, File mgfFile, File outputFolder, SearchParameters searchParameters, WaitingHandler waitingHandler) {
        this.pepNovoFolder = pepNovoFolder;
        this.exeTitle = exeTitle;
        this.spectrumFile = mgfFile;
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
        procCommands.add(pepNovoFolder.getAbsolutePath() + File.separator + exeTitle);
        procCommands.trimToSize();

        // Link to the MGF file
        procCommands.add("-file");
        procCommands.add(spectrumFile.getAbsolutePath());

        // Add Model
        procCommands.add("-model");
        procCommands.add(searchParameters.getFragmentationModel());

        // Add modifications
        ArrayList<String> modifications = searchParameters.getModificationProfile().getAllModifications();
        if (!modifications.isEmpty()) {
            procCommands.add("-PTMs");
            procCommands.add(ModificationFile.getModsString(searchParameters.getModificationProfile().getAllModifications()));
        }

        // Add fragment tolerance
        procCommands.add("-fragment_tolerance");
        procCommands.add(String.valueOf(searchParameters.getFragmentIonAccuracy()));

        // Add solution number
        procCommands.add("-num_solutions");
        procCommands.add(String.valueOf(searchParameters.getHitListLengthDeNovo()));

        // Precursor tolerance
        procCommands.add("-pm_tolerance");
        procCommands.add(String.valueOf(searchParameters.getPrecursorAccuracy()));

        // Use spectrum charge: no by default
        if (!searchParameters.isEstimateCharge()) {
            procCommands.add("-use_spectrum_charge");
        }

        // Use spectrum mz: no by default
        if (!searchParameters.isCorrectPrecursorMass()) {
            procCommands.add("-use_spectrum_mz");
        }

        // Remove low quality spectra: yes by default
        if (!searchParameters.getDiscardLowQualitySpectra()) {
            procCommands.add("-no_quality_filter");
        }

        // Generate blast query
        if (searchParameters.generateQuery()) {
            procCommands.add("-msb_generate_query");
            procCommands.add("-msb_query_name");
            procCommands.add(outputFolder.getAbsolutePath() + System.getProperty("file.separator") + spectrumFile.getName() + ".query");
        }

        // Add output path
        outputFile = FileProcessor.getOutFile(outputFolder, spectrumFile);
        procCommands.trimToSize();

        // Set the description - yet not used
        setDescription("PEPNOVO");
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
            log.info(">> De novo sequencing has been canceled.");
        }
    }

    @Override
    public void run() {
        super.run();
    }
}
