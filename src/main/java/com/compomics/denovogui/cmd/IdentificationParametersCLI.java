package com.compomics.denovogui.cmd;

import com.compomics.denovogui.DeNovoSequencingHandler;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.experiment.identification.search_parameters_cli.AbstractIdentificationParametersCli;
import java.io.File;
import org.apache.commons.cli.Options;

/**
 * The SearchParametersCLI allows creating search parameters files using command
 * line arguments.
 *
 * @author Marc Vaudel
 */
public class IdentificationParametersCLI extends AbstractIdentificationParametersCli {

    /**
     * Construct a new SearchParametersCLI runnable from a list of arguments.
     * When initialization is successful, calling "run" will write the created
     * parameters file.
     *
     * @param args the command line arguments
     */
    public IdentificationParametersCLI(String[] args) {
        initiate(args);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new IdentificationParametersCLI(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected File getModificationsFile() {
        return new File(getJarFilePath(), DeNovoSequencingHandler.getDefaultModificationFile());
    }

    @Override
    protected File getUserModificationsFile() {
        return new File(getJarFilePath(), DeNovoSequencingHandler.getUserModificationFile());
    }

    @Override
    protected File getEnzymeFile() {
        return new File(getJarFilePath(), DeNovoSequencingHandler.getEnzymeFile());
    }

    @Override
    protected void createOptionsCLI(Options options) {
        DeNovoIdentificationParametersCLIParams.createOptionsCLI(options);
    }

    @Override
    protected String getOptionsAsString() {
        return DeNovoIdentificationParametersCLIParams.getOptionsAsString();
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("IdentificationParametersCLI.class").getPath(), "DeNovoGUI");
    }
}
