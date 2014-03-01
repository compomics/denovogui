package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.execution.Job;
import com.compomics.software.CommandLineUtils;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.identification_parameters.DirecTagParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.util.ArrayList;

/**
 * <b>DirecTagJob</b>
 * <p>
 * This job class runs DirecTag for tag-based de novo sequencing</p>
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
     * The post translational modifications factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
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
     * Constructor for the DirecTag algorithm job.
     *
     * @param exeFolder The path to the algorithm executable.
     * @param exeTitle Title of the algorithm executable.
     * @param spectrumFile The spectrum file.
     * @param nThreads The number of threads.
     * @param outputFolder The output folder.
     * @param searchParameters The search parameters.
     * @param waitingHandler the waiting handler
     */
    public DirecTagJob(File exeFolder, String exeTitle, File spectrumFile, int nThreads, File outputFolder, SearchParameters searchParameters, WaitingHandler waitingHandler) {
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

        // get the DirecTag specific parameters
        DirecTagParameters direcTagParameters = (DirecTagParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.DirecTag.getIndex());

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
        ArrayList<String> fixedPtms = searchParameters.getModificationProfile().getFixedModifications();
        for (String ptmName : fixedPtms) {
            fixedModsAsString += getFixedPtmFormattedForDirecTag(ptmName);
        }
        fixedModsAsString = fixedModsAsString.trim();
        if (!fixedModsAsString.isEmpty()) {
            procCommands.add("-StaticMods");
            procCommands.add(CommandLineUtils.getQuoteType() + fixedModsAsString + CommandLineUtils.getQuoteType());
        }

        // add variable mods
        int modCounter = 0;
        String variableModsAsString = "";
        ArrayList<String> variablePtms = searchParameters.getModificationProfile().getVariableModifications();
        for (String ptmName : variablePtms) {
            variableModsAsString += getVariablePtmFormattedForDirecTag(ptmName, modCounter++);
        }
        variableModsAsString = variableModsAsString.trim();
        if (!variableModsAsString.isEmpty()) {
            procCommands.add("-DynamicMods");
            procCommands.add(CommandLineUtils.getQuoteType() + variableModsAsString + CommandLineUtils.getQuoteType());
        }

        // fragment tolerance
        procCommands.add("-FragmentMzTolerance");
        procCommands.add(String.valueOf(searchParameters.getFragmentIonAccuracy()));

        // precursor tolerance
        procCommands.add("-PrecursorMzTolerance");
        procCommands.add(String.valueOf(searchParameters.getPrecursorAccuracyDalton()));

        // tag length
        procCommands.add("-TagLength");
        procCommands.add(String.valueOf(direcTagParameters.getTagLength()));

        // maximum tag count
        procCommands.add("-MaxTagCount");
        procCommands.add(String.valueOf(direcTagParameters.getMaxTagCount()));

        // maximum peak count
//        procCommands.add("-MaxPeakCount");
//        procCommands.add(String.valueOf(direcTagParameters.getMaxPeakCount())); // @TODO: figure out why adding this parameter seems to make DirecTag very slow, even when the default value is used

        // number of intensity classes
        procCommands.add("-NumIntensityClasses");
        procCommands.add(String.valueOf(direcTagParameters.getNumIntensityClasses()));

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

        outputFile = new File(outputFolder, Util.getFileName(spectrumFile) + "_directag.log");

        // print the command to the log file
        System.out.println(System.getProperty("line.separator") + System.getProperty("line.separator") + "DirecTag command: ");
        for (int i = 0; i < procCommands.size(); i++) {
            System.out.print(procCommands.get(i) + " ");
        }
        System.out.println(System.getProperty("line.separator"));

        // set the description
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
    
    /**
     * Get the given modification as a string in the DirecTag format.
     * 
     * @param ptmName the utilities name of the PTM
     * @return the given modification as a string in the DirecTag format
     */
    private String getVariablePtmFormattedForDirecTag(String ptmName, int modIndex) {

        PTM tempPtm = ptmFactory.getPTM(ptmName);
        double ptmMass = tempPtm.getMass();
        String ptmAsString = "";
        
        // get the targeted amino acids
        for (AminoAcid aa : tempPtm.getPattern().getAminoAcidsAtTarget()) {
            ptmAsString += " " + aa.singleLetterCode + " " + modIndex + " " + ptmMass;
        }
        if (tempPtm.getPattern().getAminoAcidsAtTarget().isEmpty()) {
            for(String aminoAcid : AminoAcid.getAminoAcidsList()) {
                ptmAsString += " " + aminoAcid + " " + modIndex + " " + ptmMass;
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

        PTM tempPtm = ptmFactory.getPTM(ptmName);
        double ptmMass = tempPtm.getMass();
        String ptmAsString = "";
        
        // get the targeted amino acids
        for (AminoAcid aa : tempPtm.getPattern().getAminoAcidsAtTarget()) {
            ptmAsString += " " + aa.singleLetterCode + " " + ptmMass;
        }
        if (tempPtm.getPattern().getAminoAcidsAtTarget().isEmpty()) {
            for(String aminoAcid : AminoAcid.getAminoAcidsList()) {
                ptmAsString += " " + aminoAcid + " " + ptmMass;
            }
        }
        
        // return the ptm
        return ptmAsString;
    }
}
