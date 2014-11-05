package com.compomics.denovogui.execution;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.log4j.Logger;

/**
 * Abstract class of a job to be executed. Implements the interface Executable
 * which provides the specification of a Job.
 *
 * @author Thilo Muth
 * @author Harald Barsnes
 */
public abstract class Job implements Executable, Runnable {

    /**
     * The job ID.
     */
    protected int id;
    /**
     * Default setting for the error.
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
     * Waiting handler displaying feedback to the user.
     */
    protected WaitingHandler waitingHandler;
    /**
     * The exception handler.
     */
    protected ExceptionHandler exceptionHandler;

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
            waitingHandler.appendReport("Could not start " + getDescription() + "!", true, true);
            waitingHandler.appendReportEndLine();
            waitingHandler.setRunCanceled();
            exceptionHandler.catchException(ioe);
        }

        // Retrieve input stream from process.
        Scanner scan = new Scanner(proc.getInputStream());
        scan.useDelimiter(System.getProperty("line.separator"));

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            // set the progress dialog update count
            int totalSpectrumCount = waitingHandler.getMaxPrimaryProgressCounter();
            int spectrumCount = 1000;
            if (totalSpectrumCount <= 1000) {
                spectrumCount = 100;
            }
            if (totalSpectrumCount <= 100) {
                spectrumCount = 10;
            }

            // Get input from scanner and send to stdout
            while (scan.hasNextLine() && !waitingHandler.isRunCanceled()) {
                String temp = scan.nextLine();
                writer.write(temp);
                writer.newLine();

                if (description.equalsIgnoreCase("DirecTag") || description.equalsIgnoreCase("pNovo+")) {
                    waitingHandler.appendReport(temp, false, true);
                    // @TODO: better processing of pNovo progress output
                } else {
                    if (temp.startsWith(">>")) {
                        int progressCounter = waitingHandler.getPrimaryProgressCounter();
                        if (progressCounter % spectrumCount == 0 || progressCounter == 1) {
                            if (progressCounter == 1) {
                                progressCounter = 0;
                            }
                            waitingHandler.appendReport("Processing spectrum " + (progressCounter + 1)
                                    + "-" + Math.min(progressCounter + spectrumCount, totalSpectrumCount)
                                    + " of " + totalSpectrumCount + ".", true, true);
                        }
                        waitingHandler.increasePrimaryProgressCounter();
                        waitingHandler.increaseSecondaryProgressCounter();
                    }
                }
            }

            writer.flush();
            writer.close();
        } catch (IOException ex) {
            exceptionHandler.catchException(ex);
        }

        scan.close();

        try {
            proc.waitFor();
            setStatus(JobStatus.FINISHED);
        } catch (InterruptedException e) {
            if (!waitingHandler.isRunCanceled()) {
                setError(e.getMessage());
                setStatus(JobStatus.ERROR);
                exceptionHandler.catchException(e);
                if (proc != null) {
                    log.warn("SUBPROCESS KILLED!");
                    proc.destroy();
                }
                waitingHandler.setRunCanceled();
            }
        }
    }

    @Override
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

    @Override
    public final JobStatus getStatus() {
        return this.status;
    }

    /**
     * This method sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    @Override
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
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Writes the command executed to the out stream.
     */
    public abstract void writeCommand();

    @Override
    public void cancel() {
        if (proc != null) {
            proc.destroy();
            log.info("PROCESS CANCELED.");
        }
    }
}
