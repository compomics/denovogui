/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui.gui.tablemodels;

import com.compomics.util.denovo.PeptideAssumptionDetails;
import com.compomics.util.experiment.identification.PeptideAssumption;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

/**
 * A model of table presenting the peptide assumptions of a spectrum match
 *
 * @author Marc
 */
public class SpectrumMatchTableModel extends DefaultTableModel {

    /**
     * The ordered peptide assumptions of the selected spectrum match
     */
    private ArrayList<PeptideAssumption> peptideAssumptions = null;

    /**
     * Constructor
     */
    public SpectrumMatchTableModel() {
    }
    /**
     * Constructor
     */
    public SpectrumMatchTableModel(ArrayList<PeptideAssumption> peptideAssumptions) {
        this.peptideAssumptions = peptideAssumptions;
    }

    @Override
    public int getRowCount() {
        if (peptideAssumptions == null) {
            return 0;
        }
        return peptideAssumptions.size();
    }

    @Override
    public int getColumnCount() {
        return 8;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return " ";
            case 1:
                return "Sequence";
            case 2:
                return "RankScore";
            case 3:
                return "Score";
            case 4:
                return "N-Gap";
            case 5:
                return "C-Gap";
            case 6:
                return "m/z";
            case 7:
                return "Charge";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        switch (column) {
            case 0:
                return row + 1;
            case 1:
                PeptideAssumption peptideAssumption = peptideAssumptions.get(row);
                return peptideAssumption.getPeptide().getSequence();
            case 2:
                peptideAssumption = peptideAssumptions.get(row);
                return peptideAssumption.getScore();
            case 3:
                peptideAssumption = peptideAssumptions.get(row);
                PeptideAssumptionDetails peptideAssumptionDetails = new PeptideAssumptionDetails();
                peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);
                return peptideAssumptionDetails.getPepNovoScore();
            case 4:
                peptideAssumption = peptideAssumptions.get(row);
                peptideAssumptionDetails = new PeptideAssumptionDetails();
                peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);
                return peptideAssumptionDetails.getnTermGap();
            case 5:
                peptideAssumption = peptideAssumptions.get(row);
                peptideAssumptionDetails = new PeptideAssumptionDetails();
                peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);
                return peptideAssumptionDetails.getcTermGap();
            case 6:
                peptideAssumption = peptideAssumptions.get(row);
                return peptideAssumption.getTheoreticMz();
            case 7:
                peptideAssumption = peptideAssumptions.get(row);
                return peptideAssumption.getIdentificationCharge().toString();
            default:
                return "";
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, columnIndex) != null) {
                return getValueAt(i, columnIndex).getClass();
            }
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}