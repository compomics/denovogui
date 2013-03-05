package com.compomics.denovogui.gui.panels;

import com.compomics.denovogui.gui.DeNovoGUI;
import com.compomics.denovogui.gui.tablemodels.SpectrumMatchTableModel;
import com.compomics.denovogui.gui.tablemodels.SpectrumTableModel;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import java.awt.Color;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 *
 * @author Thilo Muth
 * @author Harald Barsnes
 */
public class ResultsPanel extends javax.swing.JPanel {

    /**
     * A references to the main frame.
     */
    private DeNovoGUI deNovoGUI;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The current peptide assumptions.
     */
    private ArrayList<PeptideAssumption> assumptions = new ArrayList<PeptideAssumption>();

    /**
     * Creates a new ResultsPanel.
     *
     * @param deNovoGUI a references to the main frame
     */
    public ResultsPanel(DeNovoGUI deNovoGUI) {
        initComponents();
        this.deNovoGUI = deNovoGUI;
        setUpGUI();
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {

        // make sure that the scroll panes are see-through
        querySpectraTableScrollPane.getViewport().setOpaque(false);
        deNovoPeptidesTableScrollPane.getViewport().setOpaque(false);

        querySpectraTable.getTableHeader().setReorderingAllowed(false);
        deNovoPeptidesTable.getTableHeader().setReorderingAllowed(false);

        querySpectraTable.setAutoCreateRowSorter(true);
        deNovoPeptidesTable.setAutoCreateRowSorter(true);

        // correct the color for the upper right corner
        JPanel queryCorner = new JPanel();
        queryCorner.setBackground(querySpectraTable.getTableHeader().getBackground());
        deNovoPeptidesTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, queryCorner);
        JPanel peptideCorner = new JPanel();
        peptideCorner.setBackground(deNovoPeptidesTable.getTableHeader().getBackground());
        deNovoPeptidesTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, peptideCorner);
    }

    /**
     * Set the table properties.
     */
    private void setTableProperties() {

        querySpectraTable.getColumn(" ").setMaxWidth(50);
        querySpectraTable.getColumn(" ").setMinWidth(50);
        querySpectraTable.getColumn("m/z").setMaxWidth(80);
        querySpectraTable.getColumn("m/z").setMinWidth(80);
        querySpectraTable.getColumn("Charge").setMaxWidth(80);
        querySpectraTable.getColumn("Charge").setMinWidth(80);
        querySpectraTable.getColumn("  ").setMaxWidth(30);
        querySpectraTable.getColumn("  ").setMinWidth(30);

        querySpectraTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());
        querySpectraTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, deNovoGUI.getSparklineColor())); // @TODO: set max charge
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth() - 30);
        querySpectraTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1000.0, deNovoGUI.getSparklineColor())); // @TODO: set max m/z
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());

        deNovoPeptidesTable.getColumn(" ").setMaxWidth(50);
        deNovoPeptidesTable.getColumn(" ").setMinWidth(50);

        deNovoPeptidesTable.getColumn("RankScore").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, -10.0, 10.0, Color.BLUE, Color.RED)); // @TODO: set min and max RankScore
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("RankScore").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, deNovoGUI.getSparklineColor())); // @TODO: set max Score
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("N-Gap").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1000.0, deNovoGUI.getSparklineColor())); // @TODO: set max N-Gap
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("N-Gap").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("C-Gap").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1000.0, deNovoGUI.getSparklineColor())); // @TODO: set max N-Gap
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("C-Gap").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1000.0, deNovoGUI.getSparklineColor())); // @TODO: set max m/z
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, deNovoGUI.getSparklineColor())); // @TODO: set max charge
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth() - 30);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        debovoResultsPanel = new javax.swing.JPanel();
        spectrumViewerPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        querySpectraPanel = new javax.swing.JPanel();
        querySpectraTableScrollPane = new javax.swing.JScrollPane();
        querySpectraTable = new javax.swing.JTable();
        deNovoPeptidesPanel = new javax.swing.JPanel();
        deNovoPeptidesTableScrollPane = new javax.swing.JScrollPane();
        deNovoPeptidesTable = new javax.swing.JTable();

        debovoResultsPanel.setBackground(new java.awt.Color(230, 230, 230));

        spectrumViewerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Viewer"));
        spectrumViewerPanel.setOpaque(false);

        spectrumJPanel.setLayout(new javax.swing.BoxLayout(spectrumJPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout spectrumViewerPanelLayout = new javax.swing.GroupLayout(spectrumViewerPanel);
        spectrumViewerPanel.setLayout(spectrumViewerPanelLayout);
        spectrumViewerPanelLayout.setHorizontalGroup(
            spectrumViewerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumViewerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumViewerPanelLayout.setVerticalGroup(
            spectrumViewerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumViewerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addContainerGap())
        );

        querySpectraPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Query Spectra"));
        querySpectraPanel.setOpaque(false);

        querySpectraTable.setModel(new SpectrumTableModel());
        querySpectraTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                querySpectraTableMouseReleased(evt);
            }
        });
        querySpectraTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                querySpectraTableKeyReleased(evt);
            }
        });
        querySpectraTableScrollPane.setViewportView(querySpectraTable);

        javax.swing.GroupLayout querySpectraPanelLayout = new javax.swing.GroupLayout(querySpectraPanel);
        querySpectraPanel.setLayout(querySpectraPanelLayout);
        querySpectraPanelLayout.setHorizontalGroup(
            querySpectraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(querySpectraPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(querySpectraTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
                .addContainerGap())
        );
        querySpectraPanelLayout.setVerticalGroup(
            querySpectraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, querySpectraPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(querySpectraTableScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        deNovoPeptidesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("De Novo Peptides"));
        deNovoPeptidesPanel.setOpaque(false);

        deNovoPeptidesTable.setModel(new SpectrumMatchTableModel());
        deNovoPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                deNovoPeptidesTableMouseReleased(evt);
            }
        });
        deNovoPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                deNovoPeptidesTableKeyReleased(evt);
            }
        });
        deNovoPeptidesTableScrollPane.setViewportView(deNovoPeptidesTable);

        javax.swing.GroupLayout deNovoPeptidesPanelLayout = new javax.swing.GroupLayout(deNovoPeptidesPanel);
        deNovoPeptidesPanel.setLayout(deNovoPeptidesPanelLayout);
        deNovoPeptidesPanelLayout.setHorizontalGroup(
            deNovoPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deNovoPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deNovoPeptidesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                .addContainerGap())
        );
        deNovoPeptidesPanelLayout.setVerticalGroup(
            deNovoPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deNovoPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deNovoPeptidesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout debovoResultsPanelLayout = new javax.swing.GroupLayout(debovoResultsPanel);
        debovoResultsPanel.setLayout(debovoResultsPanelLayout);
        debovoResultsPanelLayout.setHorizontalGroup(
            debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debovoResultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(debovoResultsPanelLayout.createSequentialGroup()
                        .addComponent(querySpectraPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deNovoPeptidesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(spectrumViewerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        debovoResultsPanelLayout.setVerticalGroup(
            debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debovoResultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumViewerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(deNovoPeptidesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(querySpectraPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(debovoResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(debovoResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the assumptions table.
     *
     * @param evt
     */
    private void querySpectraTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_querySpectraTableMouseReleased
        updateAssumptionsTable();
    }//GEN-LAST:event_querySpectraTableMouseReleased

    /**
     * Update the assumptions table.
     *
     * @param evt
     */
    private void querySpectraTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_querySpectraTableKeyReleased
        updateAssumptionsTable();
    }//GEN-LAST:event_querySpectraTableKeyReleased

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void deNovoPeptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoPeptidesTableMouseReleased
        updateSpectrum();
    }//GEN-LAST:event_deNovoPeptidesTableMouseReleased

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void deNovoPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_deNovoPeptidesTableKeyReleased
        updateSpectrum();
    }//GEN-LAST:event_deNovoPeptidesTableKeyReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel deNovoPeptidesPanel;
    private javax.swing.JTable deNovoPeptidesTable;
    private javax.swing.JScrollPane deNovoPeptidesTableScrollPane;
    private javax.swing.JPanel debovoResultsPanel;
    private javax.swing.JPanel querySpectraPanel;
    private javax.swing.JTable querySpectraTable;
    private javax.swing.JScrollPane querySpectraTableScrollPane;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JPanel spectrumViewerPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays new results.
     */
    public void diplayResults() {
        TableModel tableModel = new SpectrumTableModel(getSelectedSpectrumFile(), deNovoGUI.getIdentification());
        querySpectraTable.setModel(tableModel);
        querySpectraTable.setRowSelectionInterval(0, 0);
        updateAssumptionsTable();
    }

    /**
     * Returns the name of the output file displayed.
     *
     * @return the name of the output file displayed
     */
    public String getSelectedOutputFile() {
        //@TODO: allow the user to chose the file
        return getSelectedSpectrumFile().replaceFirst("mgf", "mgf.out");
    }

    /**
     * Returns the name of the output file displayed.
     *
     * @return the name of the output file displayed
     */
    public String getSelectedSpectrumFile() {
        //@TODO: allow the user to chose the file
        return spectrumFactory.getMgfFileNames().get(0);
    }

    /**
     * Returns the title of the selected spectrum.
     *
     * @return the title of the selected spectrum
     */
    public String getSelectedSpectrumTitle() {
        int selectedRow = querySpectraTable.getSelectedRow();
        int modelRow = querySpectraTable.convertRowIndexToModel(selectedRow);
        return spectrumFactory.getSpectrumTitles(getSelectedSpectrumFile()).get(modelRow);
    }

    /**
     * Updates the assumption table based on the selected line.
     */
    public void updateAssumptionsTable() {

        try {
            assumptions = new ArrayList<PeptideAssumption>();
            Identification identification = deNovoGUI.getIdentification();
            String psmKey = Spectrum.getSpectrumKey(getSelectedOutputFile(), getSelectedSpectrumTitle());

            //System.out.println(psmKey);
            if (identification.matchExists(psmKey)) {
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);
                ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                Collections.sort(scores, Collections.reverseOrder());
                for (Double score : scores) {
                    assumptions.addAll(assumptionsMap.get(score));
                }
            }
            TableModel tableModel = new SpectrumMatchTableModel(assumptions);
            deNovoPeptidesTable.setModel(tableModel);

            ((DefaultTableModel) deNovoPeptidesTable.getModel()).fireTableDataChanged();
            setTableProperties();

            if (deNovoPeptidesTable.getRowCount() > 0) {
                deNovoPeptidesTable.setRowSelectionInterval(0, 0);
            }

            updateSpectrum();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the spectrum and annotations.
     */
    private void updateSpectrum() {

        spectrumJPanel.removeAll();
        String spectrumKey = Spectrum.getSpectrumKey(getSelectedSpectrumFile(), getSelectedSpectrumTitle());

        if (spectrumFactory.spectrumLoaded(spectrumKey) && deNovoPeptidesTable.getSelectedRow() != -1) {

            try {
                MSnSpectrum currentSpectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);

                // add the data to the spectrum panel
                Precursor precursor = currentSpectrum.getPrecursor();
                PeptideAssumption peptideAssumption = assumptions.get(deNovoPeptidesTable.getSelectedRow());

                SpectrumPanel spectrumPanel = new SpectrumPanel(
                        currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                        precursor.getMz(), peptideAssumption.getIdentificationCharge().toString(),
                        "", 40, false, false, false, 2, false);
                spectrumPanel.setBorder(null);

                spectrumJPanel.add(spectrumPanel);

                // @TODO: add better error handling

            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (MzMLUnmarshallerException e) {
                e.printStackTrace();
            }
        }

        spectrumJPanel.revalidate();
        spectrumJPanel.repaint();
    }
}
