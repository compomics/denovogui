package com.compomics.denovogui.gui.tablemodels;

import com.compomics.util.denovo.PeptideAssumptionDetails;
import com.compomics.util.experiment.biology.ions.ElementaryIon;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.preferences.ModificationProfile;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

/**
 * A model of table presenting the peptide assumptions of a spectrum match.
 *
 * @author Marc Vaudel
 */
public class SpectrumMatchTableModel extends DefaultTableModel {

    /**
     * The ordered peptide assumptions of the selected spectrum match.
     */
    private ArrayList<PeptideAssumption> peptideAssumptions = null;
    /**
     * The modification profile of the search.
     */
    private ModificationProfile modificationProfile = null;
    /**
     * If true, the fixed PTMs are indicated in the peptide sequences.
     */
    private boolean excludeAllFixedPtms = true;

    /**
     * Constructor.
     */
    public SpectrumMatchTableModel() {
    }

    /**
     * Constructor.
     *
     * @param peptideAssumptions the peptide assumptions
     * @param modificationProfile the modification profile
     * @param excludeAllFixedPtms are fixed PTMs are to be indicated in the
     * table
     */
    public SpectrumMatchTableModel(ArrayList<PeptideAssumption> peptideAssumptions, ModificationProfile modificationProfile, boolean excludeAllFixedPtms) {
        this.peptideAssumptions = peptideAssumptions;
        this.modificationProfile = modificationProfile;
        this.excludeAllFixedPtms = excludeAllFixedPtms;
    }

    /**
     * Set if the fixed PTMs are to be indicated in the table or not.
     *
     * @param excludeAllFixedPtms are fixed PTMs are to be indicated in the
     * table
     */
    public void setExcludeAllFixedPtms(boolean excludeAllFixedPtms) {
        this.excludeAllFixedPtms = excludeAllFixedPtms;
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
                return "m/z";
            case 3:
                return "Charge";
            case 4:
                return "N-Gap";
            case 5:
                return "C-Gap";
            case 6:
                return "Rank Score";
            case 7:
                return "Score";
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

                String taggedSequence = peptideAssumption.getPeptide().getTaggedModifiedSequence(modificationProfile, true, true, true, excludeAllFixedPtms);

                PeptideAssumptionDetails peptideAssumptionDetails = new PeptideAssumptionDetails();
                peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);

                if (peptideAssumptionDetails.getNTermGap() > 0) {
                    taggedSequence = taggedSequence.replaceAll("NH2-", "...");
                }

                if (peptideAssumptionDetails.getCTermGap() > 0) {
                    taggedSequence = taggedSequence.replaceAll("-COOH", "...");
                }

                return taggedSequence;
            case 2:
                peptideAssumption = peptideAssumptions.get(row);
                peptideAssumptionDetails = new PeptideAssumptionDetails();
                peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);
                int charge = peptideAssumption.getIdentificationCharge().value;
                return (peptideAssumptionDetails.getMH() + ElementaryIon.proton.getTheoreticMass() * (charge - 1)) / charge;
            case 3:
                peptideAssumption = peptideAssumptions.get(row);
                return peptideAssumption.getIdentificationCharge().value;
            case 4:
                peptideAssumption = peptideAssumptions.get(row);
                peptideAssumptionDetails = new PeptideAssumptionDetails();
                peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);
                return peptideAssumptionDetails.getNTermGap();
            case 5:
                peptideAssumption = peptideAssumptions.get(row);
                peptideAssumptionDetails = new PeptideAssumptionDetails();
                peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);
                return peptideAssumptionDetails.getCTermGap();
            case 6:
                peptideAssumption = peptideAssumptions.get(row);
                peptideAssumptionDetails = new PeptideAssumptionDetails();
                peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);
                return peptideAssumptionDetails.getRankScore();
            case 7:
                peptideAssumption = peptideAssumptions.get(row);
                return peptideAssumption.getScore();
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