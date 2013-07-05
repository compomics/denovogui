package com.compomics.denovogui.execution;

import com.compomics.util.gui.waiting.WaitingHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.log4j.Logger;

/**
 * <b>Job</b> <p> Abstract class of a job to be executed. Implements the
 * interface Executable which provides the specification of a Job. </p>
 *
 * @author Thilo Muth
 */
public abstract class Job implements Executable, Runnable {

    /**
     * The job ID.
     */
    protected int id;
    /**
     * Default setting for the error --> NULL.
     */
    protected String error = null;
    /**
     * Default setting on JobStatus.WAITING.
     */
    private JobStatus status = JobStatus.WAITING;
    /**
     * Default description is an empty string.
     */
    private String description = "";
    /**
     * Filename representing the file.
     */
    private String filename;
    /**
     * Output file object.
     */
    protected File outputFile;
    /**
     * The ProcessBuilder object.
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
    /**
     * Waiting handler displaying feedback to the user
     */
    protected WaitingHandler waitingHandler;

    /**
     * Executes a job.
     */
    @Override
    public void run() {
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

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            // Get input from scanner and send to stdout
            while (scan.hasNext() && !waitingHandler.isRunCanceled()) {
                String temp = scan.next();
                writer.write(temp);
                writer.newLine();

                if (temp.startsWith(">>")) {                    
                    waitingHandler.increaseProgressValue();
                    if (waitingHandler.getPrimaryProgressBar() != null) {
                        waitingHandler.appendReport("Processed spectrum " + waitingHandler.getPrimaryProgressBar().getValue() + "/" + waitingHandler.getPrimaryProgressBar().getMaximum() + ".", true, true);
                    } else {
                        waitingHandler.appendReport("Processed spectrum.", true, true);
                    }
                }
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
            if (!waitingHandler.isRunCanceled()) {
                setError(e.getMessage());
                setStatus(JobStatus.ERROR);
                e.printStackTrace();
                if (proc != null) {
                    log.warn("SUBPROCESS KILLED!");
                    proc.destroy();
                }
                waitingHandler.setRunCanceled();
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
     *
     * @param error the error
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
     * Cancels the job by destroying the process.
     */
    public void cancel() {
        if (proc != null) {
            proc.destroy();
            log.info("PROCESS CANCELED.");
        }
    }
}
