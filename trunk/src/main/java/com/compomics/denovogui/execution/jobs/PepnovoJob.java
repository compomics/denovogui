/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.execution.Job;
import com.compomics.denovogui.io.ModificationFile;
import com.compomics.util.Util;
import com.compomics.util.experiment.identification.SearchParameters;
import java.io.File;

/**
 * <b>PepnovoJob</b>
 * <p>
 * This job class runs the PepNovo+ software in wrapper mode.
 * </p>
 *
 * @author T.Muth
 */
public class PepnovoJob extends Job {

    /**
     * Name of the PepNovo+ executable.
     */
    public final static String PEPNOVO_EXE = "PepNovo.exe";

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
     * @param mgfFile       The spectrum MGF file.
     * @param outputFolder    The output folder.
     * @param searchParameters        The search parameters.
     */
    public PepnovoJob(File pepNovoFolder, File mgfFile, File outputFolder, SearchParameters searchParameters) {
        this.pepNovoFolder = pepNovoFolder;
        this.spectrumFile = mgfFile;
        this.outputFolder = outputFolder;
        this.searchParameters = searchParameters;
        initJob();
    }

    /**
     * Initializes the job, setting up the commands for the ProcessBuilder.
     */
    private void initJob() {
        // full path to executable
        procCommands.add(pepNovoFolder.getAbsolutePath() + "/" + PEPNOVO_EXE);
        procCommands.trimToSize();

        // Link to the MGF file
        procCommands.add("-file");
        procCommands.add(spectrumFile.getAbsolutePath());

        // Add Model
        procCommands.add("-model");
        procCommands.add(searchParameters.getFragmentationModel());

        // Add modifications
        procCommands.add("-PTMs");
        //@TODO implement fixed modifications
        procCommands.add(ModificationFile.getModsString(searchParameters.getModificationProfile().getAllModifications())); //params.getMods());

        // Add fragment tolerance
        procCommands.add("-fragment_tolerance");
        procCommands.add(String.valueOf(searchParameters.getFragmentIonAccuracy()));

        // Add solution number
        procCommands.add("-num_solutions");
        procCommands.add(String.valueOf(searchParameters.getHitListLength()));

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
        outputFile = getOutputFile(outputFolder, Util.getFileName(spectrumFile));
        procCommands.trimToSize();

        // Set the description - yet not used
        setDescription("PEPNOVO");
        procBuilder = new ProcessBuilder(procCommands);

        procBuilder.directory(pepNovoFolder);

        // set error out and std out to same stream
        procBuilder.redirectErrorStream(true);
    }

    /**
     * Returns the expected result file for a given spectrum file in a given output folder
     * @param folder the output folder
     * @param spectrumFileName the spectrum file name
     * @return 
     */
    public static File getOutputFile(File folder, String spectrumFileName) {
        return new File(folder, spectrumFileName + ".out");
    }

    /**
     * Cancels the job by destroying the process.
     */
    @Override
    public void cancel() {
        if (proc != null) {
            proc.destroy();
            log.info(">> De novo search has been canceled.");
        }
    }
    
}
