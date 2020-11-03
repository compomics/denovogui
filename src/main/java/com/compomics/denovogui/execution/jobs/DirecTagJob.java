package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.execution.Job;
import com.compomics.software.cli.CommandLineUtils;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.io.IoUtil;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.identification.tool_specific.DirecTagParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.util.ArrayList;

/**
 * This job class runs DirecTag for tag-based de novo sequencing.
 *
 * @author Thilo Muth
 * @author Harald Barsnes
 */
public class DirecTagJob extends Job {

    /**
     * Title of the DirecTag executable.
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
     * The post-translational modifications factory.
     */
    private ModificationFactory modFactory = ModificationFactory.getInstance();
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
     * The command executed.
     */
    private String command = "";
    /**
     * The DirecTag modification index.
     */
    private int modIndex = 0;

    /**
     * Constructor for the DirecTag algorithm job.
     *
     * @param exeFolder the path to the algorithm executable
     * @param exeTitle title of the algorithm executable
     * @param spectrumFile the spectrum file
     * @param nThreads the number of threads
     * @param outputFolder the output folder
     * @param searchParameters the search parameters
     * @param waitingHandler the waiting handler
     * @param exceptionHandler the exception handler
     */
    public DirecTagJob(File exeFolder, String exeTitle, File spectrumFile, int nThreads, File outputFolder, SearchParameters searchParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
        this.exeFolder = exeFolder;
        this.exeTitle = exeTitle;
        this.spectrumFile = spectrumFile;
        this.nThreads = nThreads;
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
            // get the DirecTag specific parameters
            DirecTagParameters direcTagParameters = (DirecTagParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.direcTag.getIndex());

            // full path to executable
            procCommands.add(exeFolder.getAbsolutePath() + File.separator + exeTitle);
            procCommands.trimToSize();

            // link to the spectrum file
            procCommands.add(spectrumFile.getAbsolutePath());

            // number of cores
            procCommands.add("-cpus");
            procCommands.add(Integer.toString(nThreads));

            // add fixed mods
            String fixedModsAsString = "";
            ArrayList<String> fixedPtms = searchParameters.getModificationParameters().getFixedModifications();
            for (String ptmName : fixedPtms) {
                Modification mod = modFactory.getModification(ptmName);
                if (mod.getModificationType()== ModificationType.modaa) {
                    fixedModsAsString += getFixedPtmFormattedForDirecTag(ptmName);
                }
            }
            fixedModsAsString = fixedModsAsString.trim();
            if (!fixedModsAsString.isEmpty()) {
                procCommands.add("-StaticMods");
                procCommands.add(CommandLineUtils.getQuoteType() + fixedModsAsString + CommandLineUtils.getQuoteType());
            }

            // add variable mods
            ArrayList<String> utilitiesPtms = new ArrayList<String>();
            String variableModsAsString = "";
            ArrayList<String> variablePtms = searchParameters.getModificationParameters().getVariableModifications();
            for (String ptmName : variablePtms) {
                Modification ptm = modFactory.getModification(ptmName);
                if (ptm.getModificationType() == ModificationType.modaa) {
                    variableModsAsString += getVariablePtmFormattedForDirecTag(ptmName, utilitiesPtms);
                }
            }
            variableModsAsString = variableModsAsString.trim();
            if (!variableModsAsString.isEmpty()) {
                procCommands.add("-DynamicMods");
                procCommands.add(CommandLineUtils.getQuoteType() + variableModsAsString + CommandLineUtils.getQuoteType());
                direcTagParameters.setModifications(utilitiesPtms);
            }

            // fragment tolerance
            double tolerance = searchParameters.getFragmentIonAccuracy();
            if (searchParameters.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.PPM) {
                tolerance = IdentificationParameters.getDaTolerance(tolerance, 1000); //@TODO: make the reference mass a user parameter?
            }
            procCommands.add("-FragmentMzTolerance");
            procCommands.add(String.valueOf(tolerance));

            // precursor tolerance
            tolerance = searchParameters.getPrecursorAccuracy();
            if (searchParameters.getPrecursorAccuracyType() == SearchParameters.MassAccuracyType.PPM) {
                tolerance = IdentificationParameters.getDaTolerance(tolerance, 1000); //@TODO: make the reference mass a user parameter?
            }
            procCommands.add("-PrecursorMzTolerance");
            procCommands.add(String.valueOf(tolerance));

            // tag length
            procCommands.add("-TagLength");
            procCommands.add(String.valueOf(direcTagParameters.getTagLength()));

            // maximum tag count
            procCommands.add("-MaxTagCount");
            procCommands.add(String.valueOf(direcTagParameters.getMaxTagCount()));

            // maximum peak count
            procCommands.add("-MaxPeakCount");
            procCommands.add("100");
            // procCommands.add(String.valueOf(direcTagParameters.getMaxPeakCount())); // @TODO: figure out why adding this parameter seems to make DirecTag very slow, even when the default value is used
            // number of intensity classes
            procCommands.add("-NumIntensityClasses");
            procCommands.add(String.valueOf(direcTagParameters.getNumIntensityClasses()));

            // adjust precursor mass
            procCommands.add("-TicCutoffPercentage");
            procCommands.add(String.valueOf(direcTagParameters.getTicCutoffPercentage() / 100));

            // adjust precursor mass
            procCommands.add("-AdjustPrecursorMass");
            procCommands.add(String.valueOf(direcTagParameters.isAdjustPrecursorMass()));

            // min precursor adjustment
            procCommands.add("-MinPrecursorAdjustment");
            procCommands.add(String.valueOf(direcTagParameters.getMinPrecursorAdjustment()));

            // max precursor adjustment
            procCommands.add("-MaxPrecursorAdjustment");
            procCommands.add(String.valueOf(direcTagParameters.getMaxPrecursorAdjustment()));

            // precursor adjustment step
            procCommands.add("-PrecursorAdjustmentStep");
            procCommands.add(String.valueOf(direcTagParameters.getPrecursorAdjustmentStep()));

            // number of charge states
            procCommands.add("-NumChargeStates");
            procCommands.add(String.valueOf(direcTagParameters.getNumChargeStates()));

            // the output suffix
            if (!direcTagParameters.getOutputSuffix().trim().isEmpty()) {
                procCommands.add("-OutputSuffix");
                procCommands.add(direcTagParameters.getOutputSuffix());
            }

            // use charge state from spectrum
            procCommands.add("-UseChargeStateFromMS");
            procCommands.add(String.valueOf(direcTagParameters.isUseChargeStateFromMS()));

            // duplicate spectra per charge
            procCommands.add("-DuplicateSpectra");
            procCommands.add(String.valueOf(direcTagParameters.isDuplicateSpectra()));

            // deisotoping mode
            procCommands.add("-DeisotopingMode");
            procCommands.add(String.valueOf(direcTagParameters.getDeisotopingMode()));

            // isotope mz tolerance
            procCommands.add("-IsotopeMzTolerance");
            procCommands.add(String.valueOf(direcTagParameters.getIsotopeMzTolerance()));

            // complement mz tolerance
            procCommands.add("-IsotopeMzTolerance");
            procCommands.add(String.valueOf(direcTagParameters.getComplementMzTolerance()));

            // max number of variable modifications
            procCommands.add("-MaxDynamicMods");
            procCommands.add(String.valueOf(direcTagParameters.getMaxDynamicMods()));

            // intensity score weight
            procCommands.add("-IntensityScoreWeight");
            procCommands.add(String.valueOf(direcTagParameters.getIntensityScoreWeight()));

            // mz fidelity score weight
            procCommands.add("-MzFidelityScoreWeight");
            procCommands.add(String.valueOf(direcTagParameters.getMzFidelityScoreWeight()));

            // complement score weight
            procCommands.add("-ComplementScoreWeight");
            procCommands.add(String.valueOf(direcTagParameters.getComplementScoreWeight()));

            // set the output directory
            procCommands.add("-workdir");
            procCommands.add(outputFolder.getAbsolutePath());

            procCommands.trimToSize();

            outputFile = new File(outputFolder, IoUtil.getFileName(spectrumFile) + "_directag.log");

            // save command line
            for (String commandComponent : procCommands) {
                if (!command.equals("")) {
                    command += " ";
                }
                command += commandComponent;
            }
            
            writeCommand();

            // set the description
            setDescription("DirecTag");

            procBuilder = new ProcessBuilder(procCommands);
            procBuilder.directory(exeFolder);

            // set error out and std out to same stream
            procBuilder.redirectErrorStream(true);

        } catch (Exception e) {
            exceptionHandler.catchException(e);
            waitingHandler.appendReport("An error occurred running DirecTag. See error log for details. " + e.getMessage(), true, true);
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

    /**
     * Get the given modification as a string in the DirecTag format.
     *
     * @param ptmName the utilities name of the PTM
     * @param utilitiesPtms the list of utilities PTMs, index is the index used
     * in the DirecTag output (note that the same PTM may occur more than once
     * in the list as multiple DirecTag PTM can map to the same utilities PTM)
     * @return the given modification as a string in the DirecTag format
     */
    private String getVariablePtmFormattedForDirecTag(String ptmName, ArrayList<String> utilitiesPtms) {

        Modification tempMod = modFactory.getModification(ptmName);
        String ptmAsString = "";

        // get the targeted amino acids
        if (tempMod.getPattern() != null && !tempMod.getPattern().getAminoAcidsAtTarget().isEmpty()) {
            for (Character aa : tempMod.getPattern().getAminoAcidsAtTarget()) {
                ptmAsString += " " + aa + " " + modIndex++ + " " + tempMod.getRoundedMass();
                utilitiesPtms.add(ptmName);
            }
        }

        // return the ptm
        return ptmAsString;
    }

    /**
     * Get the given modification as a string in the DirecTag format.
     *
     * @param ptmName the utilities name of the PTM
     * @return the given modification as a string in the DirecTag format
     */
    private String getFixedPtmFormattedForDirecTag(String ptmName) {

        Modification tempMod = modFactory.getModification(ptmName);
        String ptmAsString = "";

        // get the targeted amino acids
        if (tempMod.getPattern() != null && !tempMod.getPattern().getAminoAcidsAtTarget().isEmpty()) {
            for (Character aa : tempMod.getPattern().getAminoAcidsAtTarget()) {
                ptmAsString += " " + aa + " " + tempMod.getRoundedMass();
            }
        }

        // return the ptm
        return ptmAsString;
    }

    @Override
    public void writeCommand() {
        System.out.println(System.getProperty("line.separator") + System.getProperty("line.separator") + "DirecTag command: " + command + System.getProperty("line.separator"));
    }
}
