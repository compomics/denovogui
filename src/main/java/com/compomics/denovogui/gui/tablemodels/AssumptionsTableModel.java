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
        return 12;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "";
            case 1:
                return "SA";
            case 2:
                return "Sequence";
            case 3:
                return "m/z";
            case 4:
                return "Charge";
            case 5:
                return "N-Gap";
            case 6:
                return "C-Gap";
            case 7:
                return "Rank Score (P)";
            case 8:
                return "Score (P)";
            case 9:
                return "Score (D)";
            case 10:
                return "Score (p)";
            case 11:
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
                return (row + 1);
            case 1:
                return tagAssumption.getAdvocate();
            case 2:
                return tagAssumption.getTag().getTaggedModifiedSequence(modificationProfile, true, true, true, excludeAllFixedPtms, false);
            case 3:
                return tagAssumption.getTheoreticMz(true, true);
            case 4:
                return tagAssumption.getIdentificationCharge().value;
            case 5:
                return tagAssumption.getTag().getNTerminalGap();
            case 6:
                return tagAssumption.getTag().getCTerminalGap();
            case 7:
                if (tagAssumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
                    PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                    pepnovoAssumptionDetails = (PepnovoAssumptionDetails) tagAssumption.getUrParam(pepnovoAssumptionDetails);
                    return pepnovoAssumptionDetails.getRankScore();
                }
                return null;
            case 8:
                if (tagAssumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
                    return tagAssumption.getScore();
                }
                return null;
            case 9:
                if (tagAssumption.getAdvocate() == Advocate.direcTag.getIndex()) {
                    return tagAssumption.getScore();
                }
                return null;
            case 10:
                if (tagAssumption.getAdvocate() == Advocate.pNovo.getIndex()) {
                    return tagAssumption.getScore();
                }
                return null;
            case 11:
                return true;
            default:
                return "";
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
            case 1:
            case 4:
                return Integer.class;
            case 2:
                return String.class;
            case 3:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
                return Double.class;
            case 11:
                return Boolean.class;
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
