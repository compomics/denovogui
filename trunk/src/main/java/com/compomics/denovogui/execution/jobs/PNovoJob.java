package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.execution.Job;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.identification_parameters.PNovoParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import javax.swing.JOptionPane;

/**
 * This job class runs the pNovo+ software in wrapper mode.
 *
 * @author Harald Barsnes
 */
public class PNovoJob extends Job {

    /**
     * Title of the pNovo+ executable.
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
     * The path to the pNovo executable.
     */
    private File pNovoFolder;
    /**
     * The output path.
     */
    private File outputFolder;
    /**
     * The command executed.
     */
    private String command = "";
    /**
     * The name of the pNovo parameters file.
     */
    private String parameterFileName = "pnovo_params.txt";
    /**
     * The post translational modifications factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The number of threads.
     */
    private int nThreads;

    /**
     * Constructor for the PNovoJob.
     *
     * @param pNovoFolder the path to the pNovo executable
     * @param exeTitle title of the pNovo executable
     * @param mgfFile the spectrum MGF file
     * @param nThreads the number of threads
     * @param outputFolder the output folder
     * @param searchParameters the search parameters
     * @param waitingHandler the waiting handler
     * @param exceptionHandler the exception handler
     */
    public PNovoJob(File pNovoFolder, String exeTitle, File mgfFile, int nThreads, File outputFolder, SearchParameters searchParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
        this.pNovoFolder = pNovoFolder;
        this.exeTitle = exeTitle;
        this.spectrumFile = mgfFile;
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
            // full path to executable
            procCommands.add(pNovoFolder.getAbsolutePath() + File.separator + exeTitle);
            procCommands.trimToSize();

            // create the parameters file
            createParameterFile();

            // add the parameters
            procCommands.add(pNovoFolder.getAbsolutePath() + File.separator + parameterFileName);

            // add output folder
            procCommands.add("\"\""); // @TODO: add folder name?

            procCommands.trimToSize();

            // save command line
            for (String commandComponent : procCommands) {
                if (!command.equals("")) {
                    command += " ";
                }
                command += commandComponent;
            }

            String txtFileName = spectrumFile.getName().substring(0, spectrumFile.getName().lastIndexOf("."));
            outputFile = new File(outputFolder, txtFileName + ".txt");

            // Set the description - yet not used
            setDescription("pNovo+");
            procBuilder = new ProcessBuilder(procCommands);
            procBuilder.directory(pNovoFolder);

            // set error out and std out to same stream
            procBuilder.redirectErrorStream(true);

        } catch (Exception e) {
            exceptionHandler.catchException(e);
            waitingHandler.appendReport("An error occurred running pNovo+. See error log for details. " + e.getMessage(), true, true);
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
        System.out.println(System.getProperty("line.separator") + System.getProperty("line.separator") + "pNovo+ command: " + command + System.getProperty("line.separator"));
    }

    /**
     * Create the pNovo parameters file.
     */
    private void createParameterFile() {

        // get the pNovo specific parameters
        PNovoParameters pNovoParameters = (PNovoParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.pNovo.getIndex());

        try {
            /////////////////////////////////////////////////////
            //
            // From the pNovo help:
            //
            // #If you want to add a variable modification, 
            // #please use a letter from (B,J,O,U,X,Z) instead.
            // #For example, if M+Oxidation is to be added,
            // #you can add the line below(without '#'), 
            // #in which 147.0354 = mass(M) + mass(Oxidation)
            // J=147.0354
            //
            // #N- or C- terminal variable modifications can be added as follows
            // n-term=42.010565
            // n-term=43.005814
            // c-term=0.984016
            //
            // #fixed modifications can be added like:
            // C=160.030654
            // #in which 160.030654 = mass(C) + mass(Carbamidomethyl)
            //
            // #file
            // #DTA/MS2/MGF are valid options.if DTA is specified, 
            // #please set the following path(s) as the folder containing the DTA file(s)
            // spec_type=MGF
            //
            // #1:means only one activation type, CID/HCD/ETD, is used
            // #		spec_path1 should be set as the path of the data
            // #2:(HCD + ETD) is used. In this case, activation_type is ignored.
            // #		spec_path1 should be set as the path of the HCD data,
            // #		and spec_path2 should be set as the path of the ETD data.
            // spec_path=1
            // spec_path1=E:\data
            //
            // #If only one activation type of spectra is used (spec_path=1),
            // #you can specify a folder containing several MS2 or MGF files.
            // #Set spec_path1 as the foler,
            // #and pNovo+ will sequence the MS/MS data files one by one. 
            // #if folder=no, then the value of 'spec_path1' above must be a MS/MS file path. 
            // folder=yes
            //
            /////////////////////////////////////////////////////
            //

            FileWriter r = new FileWriter(pNovoFolder.getAbsolutePath() + File.separator + parameterFileName);
            BufferedWriter br = new BufferedWriter(r);

            br.write("[meta]" + System.getProperty("line.separator"));

            Character[] variableModificationsCharacters = {'B', 'J', 'O', 'U', 'X', 'Z'}; // @TODO: is it possible to add more/other characters..?
            int variableModCount = 0;

            // add the variable modifications
            if (!searchParameters.getModificationProfile().getVariableModifications().isEmpty()) {
                br.write("#variable modifications" + System.getProperty("line.separator"));
            }

            // create maps for mapping back to the ptms used
            HashMap<Character, String> modificationLetterToPtmMap = new HashMap<Character, String>();
            HashMap<Character, Character> modificationLetterToResidueMap = new HashMap<Character, Character>();

            // variable modifications targetting specific amino acids
            if (!searchParameters.getModificationProfile().getVariableModifications().isEmpty()) {
                for (String variableModification : searchParameters.getModificationProfile().getVariableModifications()) {
                    PTM ptm = ptmFactory.getPTM(variableModification);
                    if (!ptm.isCTerm() && !ptm.isNTerm()) {
                        for (Character target : ptm.getPattern().getAminoAcidsAtTarget()) {

                            if (variableModCount > variableModificationsCharacters.length) {
                                System.out.println("The number of variable PTM targets have to be smaller than " + variableModificationsCharacters.length + "!"); // @TODO: handle this better!
                                break;
                            }

                            char currentModLetter = variableModificationsCharacters[variableModCount++];
                            modificationLetterToPtmMap.put(currentModLetter, variableModification);
                            modificationLetterToResidueMap.put(currentModLetter, target);
                            br.write(currentModLetter + "=" + (AminoAcid.getAminoAcid(target).getMonoisotopicMass() + ptm.getRoundedMass()) + System.getProperty("line.separator"));
                        }
                    }
                }
            }

            pNovoParameters.setPNovoPtmMap(modificationLetterToPtmMap);
            pNovoParameters.setPNovoPtmResiduesMap(modificationLetterToResidueMap);

            // variable modifications targetting the n or c term
            if (!searchParameters.getModificationProfile().getVariableModifications().isEmpty()) {
                for (String variableModification : searchParameters.getModificationProfile().getVariableModifications()) {
                    PTM ptm = ptmFactory.getPTM(variableModification);
                    if (ptm.isNTerm()) {
                        br.write("n-term=" + ptm.getRoundedMass() + System.getProperty("line.separator"));
                    } else if (ptm.isCTerm()) {
                        br.write("c-term=" + ptm.getRoundedMass() + System.getProperty("line.separator"));
                    }
                }
                // @TODO: how to parse terminal ptms from the output? seems to be annotated using 'n' or 'c' at the start/end of the sequence. but if more than one terminal ptm which do they refer to..?
                // @TODO: what about terminal ptms at specific amino acids? not supported?
            }

            // add the fixed modifications
            if (!searchParameters.getModificationProfile().getFixedModifications().isEmpty()) {
                br.write(System.getProperty("line.separator") + "#fixed modifications" + System.getProperty("line.separator"));

                for (String fixedModification : searchParameters.getModificationProfile().getFixedModifications()) {
                    PTM ptm = ptmFactory.getPTM(fixedModification);
                    if (ptm.getPattern() != null) {
                        for (Character target : ptm.getPattern().getAminoAcidsAtTarget()) {
                            br.write(target + "=" + (AminoAcid.getAminoAcid(target).getMonoisotopicMass() + ptm.getRoundedMass()) + System.getProperty("line.separator"));
                        }
                    }
                }

                // @TODO: what about fixed terminal ptms at specific amino acids? not supported?
                // @TODO: what about fixed terminal ptms without a specific amino acid target?
            }

            // add the ion types
            br.write(System.getProperty("line.separator") + "#The lines below show the basic ion types of HCD and ETD data." + System.getProperty("line.separator"));
            br.write("HCDIONTYPE=4" + System.getProperty("line.separator"));
            br.write("HCDIONTYPE1=b 1 1 1 0.0" + System.getProperty("line.separator"));
            br.write("HCDIONTYPE2=y 1 0 1 18.0105647" + System.getProperty("line.separator"));
            br.write("HCDIONTYPE3=b 2 1 1 0.0" + System.getProperty("line.separator"));
            br.write("HCDIONTYPE4=y 2 0 1 18.0105647" + System.getProperty("line.separator"));
            br.write("ETDIONTYPE=6" + System.getProperty("line.separator"));
            br.write("ETDIONTYPE1=c 1 1 1 17.026549105" + System.getProperty("line.separator"));
            br.write("ETDIONTYPE2=z 1 0 1 1.99129206512" + System.getProperty("line.separator"));
            br.write("ETDIONTYPE3=c-1 1 1 0 16.01872407" + System.getProperty("line.separator"));
            br.write("ETDIONTYPE4=z+1 1 0 1 2.999665665" + System.getProperty("line.separator"));
            br.write("ETDIONTYPE5=c 2 1 0 17.026549105" + System.getProperty("line.separator"));
            br.write("ETDIONTYPE6=z 2 0 0 1.99129206512" + System.getProperty("line.separator")); // @TODO: should these be editable..?

            // set the enzyme
            if (searchParameters.getEnzyme() != null) {
                br.write("enzyme=" + getPNovoEnzyme() + System.getProperty("line.separator")); // @TODO: will this one ever be used? as an enzyme is not usually set?
            }

            // set multithreading
            br.write(System.getProperty("line.separator") + "#the number of threads (1 - 8)" + System.getProperty("line.separator"));
            br.write("thread=" + nThreads + System.getProperty("line.separator"));

            // set the precursor mass ranges
            br.write(System.getProperty("line.separator") + "#precursor mass range" + System.getProperty("line.separator"));
            br.write("mass_lower_bound=" + pNovoParameters.getLowerPrecursorMass() + System.getProperty("line.separator"));
            br.write("mass_upper_bound=" + pNovoParameters.getUpperPrecursorMass() + System.getProperty("line.separator"));

            // set the activation type (HCD, CID or ETD)
            br.write(System.getProperty("line.separator") + "#actication type" + System.getProperty("line.separator"));
            br.write("activation_type=" + pNovoParameters.getActicationType() + System.getProperty("line.separator"));

            // precursor tolerance
            br.write(System.getProperty("line.separator") + "#precursor tolerance" + System.getProperty("line.separator"));
            br.write("pep_tol=" + searchParameters.getPrecursorAccuracy() + System.getProperty("line.separator"));
            if (searchParameters.getPrecursorAccuracyType() == SearchParameters.MassAccuracyType.DA) {
                br.write("pep_tol_type_ppm=0" + System.getProperty("line.separator"));
            } else {
                br.write("pep_tol_type_ppm=1" + System.getProperty("line.separator"));
            }

            // fragment ion tolerance
            br.write(System.getProperty("line.separator") + "#fragment ion tolerance" + System.getProperty("line.separator"));
            br.write("frag_tol=" + searchParameters.getFragmentIonAccuracy() + System.getProperty("line.separator"));
            if (searchParameters.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.DA) {
                br.write("frag_tol_type_ppm=0" + System.getProperty("line.separator"));
            } else {
                br.write("frag_tol_type_ppm=1" + System.getProperty("line.separator"));
            }

            br.write(System.getProperty("line.separator") + System.getProperty("line.separator") + "[file]" + System.getProperty("line.separator"));

            // set the spectrum type (DTA, MS2 or MGF)
            br.write(System.getProperty("line.separator") + "#spectrum file type" + System.getProperty("line.separator"));
            br.write("spec_type=MGF" + System.getProperty("line.separator"));
            br.write("spec_path=1" + System.getProperty("line.separator"));
            br.write("spec_path1=" + spectrumFile.getAbsolutePath() + System.getProperty("line.separator"));
            br.write("folder=no" + System.getProperty("line.separator"));

            // set the outout folder
            br.write(System.getProperty("line.separator") + "#output folder" + System.getProperty("line.separator"));
            br.write("out_path=" + outputFolder.getAbsolutePath() + System.getProperty("line.separator"));

            // set the number of peptides reported per spectrum
            br.write(System.getProperty("line.separator") + "#number of peptides reported" + System.getProperty("line.separator"));
            br.write("report_pep=" + pNovoParameters.getNumberOfPeptides() + System.getProperty("line.separator"));

            br.close();
            r.close();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, new String[]{"Unable to write file: '" + e.getMessage() + "'!",
                "Could not save pNovo+ parameter file."}, "pNovo+ Parameter File Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Returns the enzyme in the pNovo formatting.
     *
     * @return the enzyme in the pNovo formatting
     */
    private String getPNovoEnzyme() {

        // #An enzyme can be set as: 
        // #[EnzymeName] [CleavageSites] [N/C] (Cleave at N- or C- terminal)
        // enzyme=Trypsin KR C
        String enzymeAsString = "";
        Enzyme enzyme = searchParameters.getEnzyme();
        enzymeAsString += enzyme.getName() + " ";
        if (enzyme.getAminoAcidBefore().size() > 0) {
            for (Character aa : enzyme.getAminoAcidBefore()) {
                enzymeAsString += aa;
            }
            enzymeAsString += " C";
//            for (Character aa : enzyme.getRestrictionAfter()) {
//                enzymeAsString += aa;
//            }
        } else if (enzyme.getAminoAcidAfter().size() > 0) {
            for (Character aa : enzyme.getAminoAcidAfter()) {
                enzymeAsString += aa;
            }
            enzymeAsString += " N";
//            for (Character aa : enzyme.getRestrictionBefore()) {
//                enzymeAsString += aa;
//            }
        }

        return enzymeAsString;
    }
}
