package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.execution.Job;
import com.compomics.util.Util;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;

/**
 * <b>DirectagJob</b>
 * <p>
 * This job class runs DirecTag for tag-based denovo sequencing</p>
 *
 * @author Thilo Muth
 */
public class DirectagJob extends Job {

    /**
     * Title of the Directag executable.
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
     * The path to the executable.
     */
    private File exeFolder;
    /**
     * The output path.
     */
    private File outputFolder;

    /**
     * The number of threads.
     */
    private int nThreads;

    /**
     * Constructor for the Directag algorithm job.
     *
     * @param exeFolder The path to the algorithm executable.
     * @param exeTitle Title of the algorithm executable.
     * @param spectrumFile The spectrum file.
     * @param nThreads The number of threads.
     * @param outputFolder The output folder.
     * @param searchParameters The search parameters.
     * @param waitingHandler the waiting handler
     */
    public DirectagJob(File exeFolder, String exeTitle, File spectrumFile, int nThreads, File outputFolder, SearchParameters searchParameters, WaitingHandler waitingHandler) {
        this.exeFolder = exeFolder;
        this.exeTitle = exeTitle;
        this.spectrumFile = spectrumFile;
        this.nThreads = nThreads;
        this.outputFolder = outputFolder;
        this.searchParameters = searchParameters;

        this.waitingHandler = waitingHandler;
        initJob();
    }

    /**
     * Initializes the job, setting up the commands for the ProcessBuilder.
     */
    private void initJob() {

        // Full path to executable.
        procCommands.add(exeFolder.getAbsolutePath() + File.separator + exeTitle);
        procCommands.trimToSize();

        // Link to the spectrum file.
        procCommands.add(spectrumFile.getAbsolutePath());

        // Number of cores.
        procCommands.add("-cpus");
        procCommands.add(Integer.toString(nThreads));

        // TODO: Fixed modifications should be parameterized.
        /**
         * If a residue (or multiple residues) should always be treated as
         * having a modification on their natural mass, set this parameter to
         * inform the tagging engine which residues are modified. Residues are
         * entered into this string as a space-delimited list of pairs. Each
         * pair is of the form: <AA residue character> <mod mass>
         * Thus, to treat cysteine as always being carboxymethylated, this
         * parameter would be set to something like the string C 57.0215
         */
        procCommands.add("-StaticMods");
        procCommands.add("C 57.0215");

        // TODO: Variable modifications should be parameterized.
        /**
         * In order to generate tags with potential post-translational
         * modifications to amino acid residues, the user must configure this
         * parameter to inform the tagging engine which residues may be
         * modified. Residues that are modifiable are entered into this string
         * in a space-delimited list of triplets. Each triplet is of the form:
         * <AA residue character> <character to represent mod> <mod mass>
         * Thus, to generate tags for potentially oxidized methionine, this
         * parameter would be set to something like the string M * 15.995.
         */
        procCommands.add("-DynamicMods");
        procCommands.add("M * 15.995");

        // Add fragment tolerance
        procCommands.add("-FragmentMzTolerance");
        procCommands.add(String.valueOf(searchParameters.getFragmentIonAccuracy()));

        // Add precursor tolerance
        procCommands.add("-PrecursorMzTolerance");
        procCommands.add(String.valueOf(searchParameters.getPrecursorAccuracyDalton()));

        //TODO: Parameterize hard-coded tag length...
        procCommands.add("-TagLength");
        procCommands.add("3");

        // Add maximum tag count
        procCommands.add("-MaxTagCount");
        procCommands.add("20");
        
        // Set the output directory
        procCommands.add("-workdir");
        procCommands.add(outputFolder.getAbsolutePath());

        procCommands.trimToSize();

        outputFile = new File(outputFolder, Util.getFileName(spectrumFile) + "_directag.out");

        // Set the description - yet not used
        setDescription("DirecTag");

        procBuilder = new ProcessBuilder(procCommands);
        procBuilder.directory(exeFolder);

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
