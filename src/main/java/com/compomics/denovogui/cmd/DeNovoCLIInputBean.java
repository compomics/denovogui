package com.compomics.denovogui.cmd;

import com.compomics.software.CommandLineUtils;
import com.compomics.util.experiment.identification.SearchParameters;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;

/**
 * The DeNovoCLIInputBean reads and stores command line options from a command
 * line.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class DeNovoCLIInputBean {

    /**
     * The spectrum files.
     */
    private ArrayList<File> spectrumFiles;
    /**
     * The output folder.
     */
    private File outputFolder;
    /**
     * The search parameters.
     */
    private SearchParameters searchParameters;
    /**
     * If true, PepNovo+ is enabled.
     */
    private boolean pepNovoEnabled = true;
    /**
     * If true, DirecTag is enabled.
     */
    private boolean direcTagEnabled = true;
    /**
     * If true, pNovo+ is enabled.
     */
    private boolean pNovoEnabled = true;
    /**
     * The PepNovo executable. Full path.
     */
    private File pepNovoExecutable = null;
    /**
     * The DirecTag executable. Full path.
     */
    private File direcTagExecutable = null;
    /**
     * The pNovo executable. Full path.
     */
    private File pNovoExecutable = null;
    /**
     * Number of threads to use. Defaults to the number of cores available.
     */
    private int nThreads = Runtime.getRuntime().availableProcessors();
    /**
     * The path settings.
     */
    private PathSettingsCLIInputBean pathSettingsCLIInputBean;

    /**
     * Takes all the arguments from a command line.
     *
     * @param aLine the command line
     * @throws FileNotFoundException thrown if a spectrum or search parameter
     * file cannot be found
     * @throws IOException thrown if there are problems accessing a spectrum or
     * search parameter file
     * @throws ClassNotFoundException thrown if the search parameters cannot be
     * processed
     */
    public DeNovoCLIInputBean(CommandLine aLine) throws FileNotFoundException, IOException, ClassNotFoundException {

        // get the files needed for the search
        String filesTxt = aLine.getOptionValue(DeNovoCLIParams.SPECTRUM_FILES.id);
        spectrumFiles = getSpectrumFiles(filesTxt);
        String arg = aLine.getOptionValue(DeNovoCLIParams.OUTPUT_FOLDER.id);
        outputFolder = new File(arg);
        String fileTxt = aLine.getOptionValue(DeNovoCLIParams.IDENTIFICATION_PARAMETERS.id);
        searchParameters = SearchParameters.getIdentificationParameters(new File(fileTxt));

        // see which algorithms to use
        if (aLine.hasOption(DeNovoCLIParams.PEPNOVO.id)) {
            String pepNovoOption = aLine.getOptionValue(DeNovoCLIParams.PEPNOVO.id);
            if (pepNovoOption.trim().equals("0")) {
                pepNovoEnabled = false;
            }
        }
        if (aLine.hasOption(DeNovoCLIParams.DIRECT_TAG.id)) {
            String direcTagOption = aLine.getOptionValue(DeNovoCLIParams.DIRECT_TAG.id);
            if (direcTagOption.trim().equals("0")) {
                direcTagEnabled = false;
            }
        }
        if (aLine.hasOption(DeNovoCLIParams.PNOVO.id)) {
            String pNovoOption = aLine.getOptionValue(DeNovoCLIParams.PNOVO.id);
            if (pNovoOption.trim().equals("0")) {
                pNovoEnabled = false;
            }
        }

        // search engine folders
        if (aLine.hasOption(DeNovoCLIParams.PEPNOVO_LOCATION.id)) {
            String tempPepNovoExecutable = aLine.getOptionValue(DeNovoCLIParams.PEPNOVO_LOCATION.id);
            this.pepNovoExecutable = new File(tempPepNovoExecutable);
        }
        if (aLine.hasOption(DeNovoCLIParams.DIRECTAG_LOCATION.id)) {
            String tempDirecTagExecutable = aLine.getOptionValue(DeNovoCLIParams.DIRECTAG_LOCATION.id);
            this.direcTagExecutable = new File(tempDirecTagExecutable);
        }
        if (aLine.hasOption(DeNovoCLIParams.PNOVO_LOCATION.id)) {
            String tempPNovoExecutable = aLine.getOptionValue(DeNovoCLIParams.PNOVO_LOCATION.id);
            this.pNovoExecutable = new File(tempPNovoExecutable);
        }

        // get the number of threads
        if (aLine.hasOption(DeNovoCLIParams.THREADS.id)) {
            arg = aLine.getOptionValue(DeNovoCLIParams.THREADS.id);
            nThreads = new Integer(arg);
        }

        pathSettingsCLIInputBean = new PathSettingsCLIInputBean(aLine);
    }

    /**
     * Return the spectrum files.
     *
     * @return the spectrum files
     */
    public ArrayList<File> getSpectrumFiles() {
        return spectrumFiles;
    }

    /**
     * Returns the output folder.
     *
     * @return the output folder
     */
    public File getOutputFile() {
        return outputFolder;
    }

    /**
     * Returns the search parameters.
     *
     * @return the search parameters
     */
    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    /**
     * Returns a list of spectrum files as imported from the command line
     * option.
     *
     * @param optionInput the command line option
     * @return a list of file candidates
     * @throws FileNotFoundException exception thrown whenever a file is not
     * found
     */
    public static ArrayList<File> getSpectrumFiles(String optionInput) throws FileNotFoundException {
        ArrayList<String> extentions = new ArrayList<String>();
        extentions.add(".mgf");
        return CommandLineUtils.getFiles(optionInput, extentions);
    }

    /**
     * Returns the PepNovo+ executable. Null if not set.
     *
     * @return the PepNovo+ executable
     */
    public File getPepNovoExecutable() {
        return pepNovoExecutable;
    }

    /**
     * Returns the DirecTag executable. Null if not set.
     *
     * @return the DirecTag executable
     */
    public File getDirecTagExecutable() {
        return direcTagExecutable;
    }

    /**
     * Returns the pNovo+ executable. Null if not set.
     *
     * @return the pNovo+ executable
     */
    public File getPNovoExecutable() {
        return pNovoExecutable;
    }

    /**
     * Returns if PepNovo+ is to be run or not.
     *
     * @return if PepNovo+ is to be run or not
     */
    public boolean enablePepNovo() {
        return pepNovoEnabled;
    }

    /**
     * Returns if DirecTag is to be run or not.
     *
     * @return if DirecTag is to be run or not
     */
    public boolean enableDirecTag() {
        return direcTagEnabled;
    }

    /**
     * Returns if pNovo+ is to be run or not.
     *
     * @return if pNovo+ is to be run or not
     */
    public boolean enablePNovo() {
        return pNovoEnabled;
    }

    /**
     * Returns the number of threads to use.
     *
     * @return the number of threads to use
     */
    public int getNThreads() {
        return nThreads;
    }

    /**
     * Verifies the command line start parameters.
     *
     * @param aLine the command line to validate
     *
     * @return true if the startup was valid
     *
     * @throws FileNotFoundException thrown if a spectrum file cannot be found
     */
    public static boolean isValidStartup(CommandLine aLine) throws FileNotFoundException {

        if (aLine.getOptions().length == 0) {
            return false;
        }

        if (!aLine.hasOption(DeNovoCLIParams.SPECTRUM_FILES.id) || ((String) aLine.getOptionValue(DeNovoCLIParams.SPECTRUM_FILES.id)).equals("")) {
            System.out.println(System.getProperty("line.separator") + "Spectrum files not specified." + System.getProperty("line.separator"));
            return false;
        } else {
            ArrayList<File> tempSpectrumFiles = DeNovoCLIInputBean.getSpectrumFiles(aLine.getOptionValue(DeNovoCLIParams.SPECTRUM_FILES.id));
            for (File file : tempSpectrumFiles) {
                if (!file.exists()) {
                    System.out.println(System.getProperty("line.separator") + "File \'" + file.getName() + "\' not found." + System.getProperty("line.separator"));
                    return false;
                }
            }
        }

        if (!aLine.hasOption(DeNovoCLIParams.OUTPUT_FOLDER.id) || ((String) aLine.getOptionValue(DeNovoCLIParams.OUTPUT_FOLDER.id)).equals("")) {
            System.out.println(System.getProperty("line.separator") + "Output folder not specified." + System.getProperty("line.separator"));
            return false;
        } else {
            File file = new File(((String) aLine.getOptionValue(DeNovoCLIParams.OUTPUT_FOLDER.id)));
            if (!file.exists()) {
                System.out.println(System.getProperty("line.separator") + "Output folder \'" + file.getName() + "\' not found." + System.getProperty("line.separator"));
                return false;
            }
        }

        if (aLine.hasOption(DeNovoCLIParams.IDENTIFICATION_PARAMETERS.id)) {
            try {
                String fileTxt = aLine.getOptionValue(DeNovoCLIParams.IDENTIFICATION_PARAMETERS.id);
                SearchParameters tempSearchParameters = SearchParameters.getIdentificationParameters(new File(fileTxt));

            } catch (Exception e) {
                System.out.println(System.getProperty("line.separator") + "An error occurred while reading the search parameters:"
                        + System.getProperty("line.separator") + e.getLocalizedMessage() + System.getProperty("line.separator"));
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the path settings provided by the user.
     *
     * @return the path settings provided by the user
     */
    public PathSettingsCLIInputBean getPathSettingsCLIInputBean() {
        return pathSettingsCLIInputBean;
    }
}
