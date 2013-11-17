package com.compomics.denovogui.gui.tablemodels;

import com.compomics.util.experiment.biology.ions.ElementaryIon;
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
public class TagSpectrumMatchTableModel extends DefaultTableModel {

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
    public TagSpectrumMatchTableModel() {
    }

    /**
     * Constructor.
     *
     * @param tagAssumptions the tag assumptions
     * @param modificationProfile the modification profile
     * @param excludeAllFixedPtms are fixed PTMs are to be indicated in the
     * table
     */
    public TagSpectrumMatchTableModel(ArrayList<TagAssumption> tagAssumptions, ModificationProfile modificationProfile, boolean excludeAllFixedPtms) {
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
        return 9;
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
            case 8:
                return "  ";
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
                TagAssumption tagAssumption = tagAssumptions.get(row);
                String taggedSequence = tagAssumption.getTag().getTaggedModifiedSequence(modificationProfile, true, true, true, excludeAllFixedPtms, false);
                return taggedSequence;
            case 2:
                tagAssumption = tagAssumptions.get(row);
                return tagAssumption.getTheoreticMz(false, false);
            case 3:
                tagAssumption = tagAssumptions.get(row);
                return tagAssumption.getIdentificationCharge().value;
            case 4:
                tagAssumption = tagAssumptions.get(row);
                return tagAssumption.getTag().getNTerminalGap();
            case 5:
                tagAssumption = tagAssumptions.get(row);
                return tagAssumption.getTag().getCTerminalGap();
            case 6:
                tagAssumption = tagAssumptions.get(row);
                PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                pepnovoAssumptionDetails = (PepnovoAssumptionDetails) tagAssumption.getUrParam(pepnovoAssumptionDetails);
                return pepnovoAssumptionDetails.getRankScore();
            case 7:
                tagAssumption = tagAssumptions.get(row);
                return tagAssumption.getScore();
            case 8:
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