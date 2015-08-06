package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.execution.Job;
import com.compomics.denovogui.io.FileProcessor;
import com.compomics.denovogui.io.PepNovoModificationFile;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.PepnovoParameters;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.util.ArrayList;

/**
 * This job class runs the PepNovo+ software in wrapper mode.
 *
 * @author Thilo Muth
 */
public class PepNovoJob extends Job {

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
     * The command executed.
     */
    private String command = "";
    /**
     * The maximum allowed precursor tolerance.
     */
    public static final double MAX_PRECURSOR_TOLERANCE = 5.0;
    /**
     * The maximum allowed fragment ion tolerance.
     */
    public static final double MAX_FRAGMENT_ION_TOLERANCE = 0.75;

    /**
     * Constructor for the PepNovoJob.
     *
     * @param pepNovoFolder The path to the PepNovo executable
     * @param exeTitle Title of the PepNovo executable
     * @param mgfFile The spectrum MGF file
     * @param outputFolder The output folder
     * @param searchParameters The search parameters
     * @param waitingHandler the waiting handler
     * @param exceptionHandler the exception handler
     */
    public PepNovoJob(File pepNovoFolder, String exeTitle, File mgfFile, File outputFolder, SearchParameters searchParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
        this.pepNovoFolder = pepNovoFolder;
        this.exeTitle = exeTitle;
        this.spectrumFile = mgfFile;
        this.outputFolder = outputFolder;
        this.searchParameters = searchParameters;
        this.waitingHandler = waitingHandler;
        this.exceptionHandler = exceptionHandler;
        initJob();
    }

    /**
     * Initializes the job, setting up the commands for the ProcessBuilder.
     */
    private void initJob() {

        try {
            // get the PepNovo specific parameters
            PepnovoParameters pepNovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex());

            // full path to executable
            procCommands.add(pepNovoFolder.getAbsolutePath() + File.separator + exeTitle);
            procCommands.trimToSize();

            // Link to the MGF file
            procCommands.add("-file");
            procCommands.add(spectrumFile.getAbsolutePath());

            // Add Model
            procCommands.add("-model");
            procCommands.add(pepNovoParameters.getFragmentationModel());

            // Add modifications
            ArrayList<String> modifications = searchParameters.getPtmSettings().getAllModifications();
            if (!modifications.isEmpty()) {
                procCommands.add("-PTMs");
                procCommands.add(PepNovoModificationFile.getModsString(searchParameters.getPtmSettings().getAllModifications()));
            }

            // Add fragment tolerance
            double tolerance = searchParameters.getFragmentIonAccuracy();
            if (searchParameters.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.PPM) {
                tolerance = IdentificationParameters.getDaTolerance(tolerance, 1000); //@TODO: make the reference mass a user parameter?
            }
            if (tolerance > MAX_FRAGMENT_ION_TOLERANCE) { // limit to max supported value
                throw new IllegalArgumentException("The maximum fragment ion mass tolerance for PepNovo+ is " 
                        + PepNovoJob.MAX_FRAGMENT_ION_TOLERANCE + " Da! Current value: " + tolerance + " Da.");
            }
            procCommands.add("-fragment_tolerance");
            procCommands.add(String.valueOf(tolerance));

            // Add solution number
            procCommands.add("-num_solutions");
            procCommands.add(String.valueOf(pepNovoParameters.getHitListLength()));

            // Precursor tolerance
            tolerance = searchParameters.getPrecursorAccuracy();
            if (searchParameters.getPrecursorAccuracyType() == SearchParameters.MassAccuracyType.PPM) {
                tolerance = IdentificationParameters.getDaTolerance(tolerance, 1000); //@TODO: make the reference mass a user parameter?
            }
            if (tolerance > MAX_PRECURSOR_TOLERANCE) { // limit to max supported value
                throw new IllegalArgumentException("The maximum precursor ion mass tolerance for PepNovo+ is " 
                        + PepNovoJob.MAX_PRECURSOR_TOLERANCE + " Da! Current value: " + tolerance + " Da.");
            }
            procCommands.add("-pm_tolerance");
            procCommands.add(String.valueOf(tolerance));

            // Use spectrum charge: no by default
            if (!pepNovoParameters.isEstimateCharge()) {
                procCommands.add("-use_spectrum_charge");
            }

            // Use spectrum mz: no by default
            if (!pepNovoParameters.isCorrectPrecursorMass()) {
                procCommands.add("-use_spectrum_mz");
            }

            // Remove low quality spectra: yes by default
            if (!pepNovoParameters.getDiscardLowQualitySpectra()) {
                procCommands.add("-no_quality_filter");
            }

            // Generate blast query
            if (pepNovoParameters.generateQuery()) {
                procCommands.add("-msb_generate_query");
                procCommands.add("-msb_query_name");
                procCommands.add(outputFolder.getAbsolutePath() + System.getProperty("file.separator") + spectrumFile.getName() + ".query");
            }

            // Add output path
            outputFile = FileProcessor.getOutFile(outputFolder, spectrumFile);
            procCommands.trimToSize();

            // save command line
            for (String commandComponent : procCommands) {
                if (!command.equals("")) {
                    command += " ";
                }
                command += commandComponent;
            }

            // Set the description - yet not used
            setDescription("PepNovo+");
            procBuilder = new ProcessBuilder(procCommands);
            procBuilder.directory(pepNovoFolder);

            // set error out and std out to same stream
            procBuilder.redirectErrorStream(true);

        } catch (Exception e) {
            exceptionHandler.catchException(e);
            waitingHandler.appendReport("An error occurred running PepNovo+. See error log for details. " + e.getMessage(), true, true);
            waitingHandler.setRunCanceled();
        }
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

    @Override
    public void writeCommand() {
        System.out.println(System.getProperty("line.separator") + System.getProperty("line.separator") + "PepNovo+ command: " + command + System.getProperty("line.separator"));
    }
}
