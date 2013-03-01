/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui.execution;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.log4j.Logger;

/**
 * <b>Job</b>
 * <p>
 * Abstract class of a job to be executed.
 * Implements the interface Executable which provides the specification of a Job.
 * </p>
 *
 * @author T.Muth
 */
public abstract class Job implements Executable {

    // The job ID
    protected int id;

    // Default setting for the error --> NULL
    protected String error = null;

    // Default setting on JobStatus.WAITING
    private JobStatus status = JobStatus.WAITING;

    // Default description is an empty string
    private String description = "";

    // Filename representing the file.
    private String filename;

    /**
     * Output file object.
     */
    protected File outputFile;

    /**
     * The ProcessBuilder object
     */
    protected ProcessBuilder procBuilder;

    /**
     * The Process object.
     */
    protected Process proc;

    /**
     * List of process commands.
     */
    protected ArrayList<String> procCommands = new ArrayList<String>();

    /**
     * Init the job logger.
     */
    protected static Logger log = Logger.getLogger(Job.class);

    protected int progress = 0;

    protected PropertyChangeSupport pSupport = new PropertyChangeSupport(this);

    private int oldProgress;

    /**
     * Executes a job.
     */
    public void execute() {
        proc = null;
        try {
            proc = procBuilder.start();
            setStatus(JobStatus.RUNNING);
        } catch (IOException ioe) {
            setStatus(JobStatus.ERROR);
            setError(ioe.getMessage());
            ioe.printStackTrace();
        }

        // Retrieve input stream from process.
        Scanner scan = new Scanner(proc.getInputStream());
        scan.useDelimiter(System.getProperty("line.separator"));

        // Temporary string variable
        String temp;

        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(outputFile));

            // Get input from scanner and send to stdout
            while (scan.hasNext()) {
                temp = scan.next();
                writer.write(temp);
                writer.newLine();

                if (temp.endsWith("done.") || temp.endsWith("loaded.") || temp.endsWith("started.") || temp.endsWith("loaded.")) {
                    temp += "\n";
                } else {
                    temp += " ";
                }

                if (temp.startsWith(">>") || temp.startsWith("#Processed spectra")) {
                    oldProgress = progress;
                    progress++;
                    pSupport.firePropertyChange("progress", progress, oldProgress);
                }
                log.info(temp);
            }
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        scan.close();

        try {
            proc.waitFor();
            setStatus(JobStatus.FINISHED);
        } catch (InterruptedException e) {
            setError(e.getMessage());
            setStatus(JobStatus.ERROR);
            e.printStackTrace();
            if (proc != null) {
                log.warn("SUBPROCESS KILLED!");
                proc.destroy();
            }
        }
    }

    /**
     * Returns the error message of the job.
     */
    public String getError() {
        return error;
    }

    /**
     * Returns the error message of the job.
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Returns the status of the job.
     *
     * @return The status of this job
     */
    public final JobStatus getStatus() {
        return this.status;
    }

    /**
     * This method sets the status.
     *
     * @param status
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * Returns the description of the job.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the job.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the filename for a job specific file.
     *
     * @return the filename for a job specific file
     */
    public String getOutputFilePath() {
        return filename;
    }

    /**
     * Sets the filename for a job specific file.
     *
     * @param filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the progress
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Adds the property change listener.
     *
     * @param l
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pSupport.addPropertyChangeListener(l);
    }

    /**
     * Cancels the job by destroying the process.
     */
    public void cancel() {
        if (proc != null) {
            proc.destroy();
            log.info("PROCESS CANCELED.");
        }
    }
    
}
