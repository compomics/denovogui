/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui;

import com.compomics.denovogui.execution.jobs.PepnovoJob;
import com.compomics.util.Util;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;

/**
 * This handle the searches with the search parameters taken from the GUI or the
 * command line
 *
 * @author Marc
 */
public class DenovoSearchHandler {

    /**
     * The pepnovo folder
     */
    private File pepNovoFolder;

    /**
     * Constructor
     *
     * @param pepNovoFolder the pep novo folder
     */
    public DenovoSearchHandler(File pepNovoFolder) {
        this.pepNovoFolder = pepNovoFolder;
    }

    /**
     * Starts the search
     *
     * @param spectrumFiles the spectrum files to process
     * @param searchParameters the search parameters
     * @param outputFolder the output folder
     */
    public void startSearch(ArrayList<File> spectrumFiles, SearchParameters searchParameters, File outputFolder) {

        // Add spectrum files to the spectrum factory
        for (File spectrumFile : spectrumFiles) {
            PepnovoJob job = new PepnovoJob(pepNovoFolder, spectrumFile, outputFolder, searchParameters);
            job.execute();
        }
    }
}
