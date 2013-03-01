package com.compomics.denovogui.execution;

/**
 * <b>Executable</b> <p> Executable-Interface implemented by the Job class. </p>
 *
 * @author Thilo Muth
 */
public interface Executable {

    /**
     * Return a description for the job.
     *
     * @return description The description represented as String
     */
    public String getDescription();

    /**
     * Returns the job status.
     *
     * @return status The JobStatus
     */
    public JobStatus getStatus();

    /**
     * Returns the error (if any error has occurred).
     *
     * @return error The error represented as String.
     */
    public String getError();

    /**
     * Executes the job.
     */
    public void execute();

    /**
     * Cancels the job.
     */
    public void cancel();
}
