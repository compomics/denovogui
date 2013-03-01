/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui.gui.qc;

import com.compomics.util.experiment.identification.Identification;
import java.io.IOException;
import java.sql.SQLException;
import org.jfree.chart.JFreeChart;

/**
 * <b>Chart</b>
 * <p>
 * This class provides general functions of a statistics chart.
 * </p>
 *
 * @author T.Muth
 */
public abstract class Chart {

    /**
     * Histogram chart object.
     */
    protected JFreeChart chart = null;

    /**
     * Creates an instance of the Chart object.
     * All inheritance classes call this constructor.
     *
     * @param identification the identification containing all results
     */
    public Chart(Identification identification) {
        if (identification != null) {
            try {
                process(identification);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Show the result data in the chart.
     *
     * @param identification the identification containing all results
     */
    protected abstract void process(Identification identification) throws IOException, SQLException, ClassNotFoundException;

    /**
     * Sets the chart object.
     */
    protected abstract void setChart();

    /**
     * Returns the chart title.
     *
     * @return the title of the chart
     */
    public abstract String getChartTitle();

    /**
     * Returns the chart object.
     *
     * @return JFreeChart the chart object
     */
    public final JFreeChart getChart() {
        return chart;
    }
}
