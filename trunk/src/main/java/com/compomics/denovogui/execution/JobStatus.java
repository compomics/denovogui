package com.compomics.denovogui.execution;

/**
 * The status of a job. 
 * <p> 
 * Possible values for the job status are:
 * <p/>
 * WAITING - job is waiting for processing thread.
 * <p/>
 * RUNNING - job is running.
 * <p/>
 * FINISHED - job is finished.
 * <p/>
 * ERROR - job gave an error.
 * <p/>
 * CANCELED - job was canceled. </p>
 *
 * @author Thilo Muth
 */
public enum JobStatus {

    /**
     * Job is waiting for processing thread.
     */
    WAITING, 
    /**
     * Job is running.
     */
    RUNNING, 
    /**
     * Job is finished.
     */
    FINISHED, 
    /**
     * Job gave an error.
     */
    ERROR, 
    /**
     * Job was canceled.
     */
    CANCELED
}
