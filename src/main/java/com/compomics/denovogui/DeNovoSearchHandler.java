package com.compomics.denovogui;

import com.compomics.denovogui.execution.Job;
import com.compomics.denovogui.execution.jobs.PepnovoJob;
import com.compomics.denovogui.gui.DeNovoGUI;
import com.compomics.denovogui.io.ModificationFile;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.WaitingHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This handle the searches with the search parameters taken from the GUI or the
 * command line.
 *
 * @author Marc Vaudel
 * @author Thilo Muth
 */
public class DeNovoSearchHandler {

    /**
     * The pepnovo folder.
     */
    private File pepNovoFolder;
    
    
    /**
     * Constructor.
     *
     * @param pepNovoFolder the pep novo folder
     */
    public DeNovoSearchHandler(File pepNovoFolder) {
        this.pepNovoFolder = pepNovoFolder;
    }

    /**
     * Starts the search.
     *
     * @param spectrumFiles the spectrum files to process
     * @param searchParameters the search parameters
     * @param outputFolder the output folder
     * @param waitingHandler the waiting handler
     */
    public void startSearch(List<File> spectrumFiles, SearchParameters searchParameters, File outputFolder, WaitingHandler waitingHandler) {
        
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        waitingHandler.setMaxProgressValue(spectrumFactory.getNSpectra());

        // Write the modification file
        try {
            File folder = new File(pepNovoFolder, "Models");
            ModificationFile.writeFile(folder, searchParameters.getModificationProfile());
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while writing the modification file.", true, true);
            e.printStackTrace();
        }
        
        // Get the number of available cores
        int nCores = Runtime.getRuntime().availableProcessors();
        System.out.println("number of cores:" + nCores);
        
        // Start a fixed thread pool
        ExecutorService threadExecutor = Executors.newFixedThreadPool(nCores);
        
        // Job queue.
        Deque<PepnovoJob> jobs = new ArrayDeque<PepnovoJob>();
        
        // Add spectrum files to the spectrum factory
        for (File spectrumFile : spectrumFiles) {
            PepnovoJob job = new PepnovoJob(pepNovoFolder, spectrumFile, outputFolder, searchParameters, waitingHandler);
            jobs.add(job);            
        }
        
        // Execute the jobs from the queue.
        int i = 1;
        for (PepnovoJob job : jobs){
            threadExecutor.execute(job);
            System.out.println("started job no.: " + i);
            i++;
        }
        
    }
}
