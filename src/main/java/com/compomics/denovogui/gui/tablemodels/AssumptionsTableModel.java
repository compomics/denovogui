package com.compomics.denovogui.gui.tablemodels;

import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.refinementparameters.PepnovoAssumptionDetails;
import com.compomics.util.preferences.ModificationProfile;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

/**
 * A model of table presenting the tag assumptions of a spectrum match.
 *
 * @author Marc Vaudel
 */
public class AssumptionsTableModel extends DefaultTableModel {

    /**
     * The ordered peptide assumptions of the selected spectrum match.
     */
    private ArrayList<TagAssumption> tagAssumptions = null;
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
    public AssumptionsTableModel() {
    }

    /**
     * Constructor.
     *
     * @param tagAssumptions the tag assumptions
     * @param modificationProfile the modification profile
     * @param excludeAllFixedPtms are fixed PTMs are to be indicated in the
     * table
     */
    public AssumptionsTableModel(ArrayList<TagAssumption> tagAssumptions, ModificationProfile modificationProfile, boolean excludeAllFixedPtms) {
        this.tagAssumptions = tagAssumptions;
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
        if (tagAssumptions == null) {
            return 0;
        }
        return tagAssumptions.size();
    }

    @Override
    public int getColumnCount() {
        return 10;
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
                return "Rank Score (P)";
            case 7:
                return "Score (P)";
            case 8:
                return "Score (D)";
            case 9:
                return "  ";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        TagAssumption tagAssumption = tagAssumptions.get(row);
        switch (column) {
            case 0:
                String rank;
                if (tagAssumption.getAdvocate() == Advocate.DirecTag.getIndex()) {
                    rank = "D";
                } else {
                    rank = "P";
                }
                return rank + tagAssumption.getRank();
            case 1:
                String taggedSequence = tagAssumption.getTag().getTaggedModifiedSequence(modificationProfile, true, true, true, excludeAllFixedPtms, false);
                return taggedSequence;
            case 2:
                return tagAssumption.getTheoreticMz(true, true);
            case 3:
                return tagAssumption.getIdentificationCharge().value;
            case 4:
                return tagAssumption.getTag().getNTerminalGap();
            case 5:
                return tagAssumption.getTag().getCTerminalGap();
            case 6:
                if (tagAssumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
                    PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                    pepnovoAssumptionDetails = (PepnovoAssumptionDetails) tagAssumption.getUrParam(pepnovoAssumptionDetails);
                    return pepnovoAssumptionDetails.getRankScore();
                }
                return "";
            case 7:
                if (tagAssumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
                    return tagAssumption.getScore();
                }
                return "";
            case 8:
                if (tagAssumption.getAdvocate() == Advocate.DirecTag.getIndex()) {
                    return tagAssumption.getScore();
                }
                return "";
            case 9:
                return true;
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
