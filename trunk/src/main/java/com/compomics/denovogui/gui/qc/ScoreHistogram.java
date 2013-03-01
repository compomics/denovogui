/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui.gui.qc;

import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;


/**
 * <p>Class to plot a histogram of the number of peaks of all intensity values.</p>
 *
 * @author T.Muth
 */
public class ScoreHistogram extends Chart {

    private double[] data;
    /**
     * Constructs a denovo score histogram
     *
     * @param identification the identification
     */
    public ScoreHistogram(Identification identification) {
        super(identification);
    }

    @Override
    protected void process(Identification identification) throws IOException, SQLException, ClassNotFoundException {
        // List of all the scores
        List<Double> scores = new ArrayList<Double>();
        for (String spectrumFile : identification.getSpectrumFiles()) {
            identification.loadSpectrumMatches(spectrumFile, null);
            for (String spectrumTitle : identification.getSpectrumIdentification(spectrumFile)) {
                String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                PeptideAssumption peptideAssumption = spectrumMatch.getFirstHit(Advocate.PEPNOVO);
                scores.add(peptideAssumption.getScore());
            }
        }

        // Set data.
        data = new double[scores.size()];
        for (int i = 0; i < scores.size(); i++) {
            data[i] = scores.get(i);
        }

        setChart();
    }

    @Override
    protected void setChart() {
        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.FREQUENCY);
        dataset.addSeries("Best Assumption Score", data, 40);
        chart = ChartFactory.createHistogram(getChartTitle(),
                "Score",
                "Frequency",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);


        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundAlpha(0f);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setOutlineVisible(false);
        
        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setShadowVisible(false);
        plot.setRenderer(renderer);
    }

    @Override
    public String getChartTitle() {
        return "De Novo Score Histogram";
    }
    
}
