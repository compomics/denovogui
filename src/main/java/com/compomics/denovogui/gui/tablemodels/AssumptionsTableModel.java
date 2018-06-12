package com.compomics.denovogui.gui.tablemodels;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.refinementparameters.PepnovoAssumptionDetails;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;

/**
 * A model of table presenting the tag assumptions of a spectrum match.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class AssumptionsTableModel extends DefaultTableModel {

    /**
     * The ordered assumptions of the selected spectrum match.
     */
    private ArrayList<SpectrumIdentificationAssumption> assumptions = null;
    /**
     * The modification profile of the search.
     */
    private PtmSettings modificationProfile = null;
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
     * @param assumptions the tag assumptions
     * @param modificationProfile the modification profile
     * @param excludeAllFixedPtms are fixed PTMs are to be indicated in the
     * table
     */
    public AssumptionsTableModel(ArrayList<SpectrumIdentificationAssumption> assumptions, PtmSettings modificationProfile, boolean excludeAllFixedPtms) {
        this.assumptions = assumptions;
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
        if (assumptions == null) {
            return 0;
        }
        return assumptions.size();
    }

    @Override
    public int getColumnCount() {
        return 13;
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
                return "Score (N)";
            case 12:
                return "  ";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        SpectrumIdentificationAssumption assumption = assumptions.get(row);
        switch (column) {
            case 0:
                return (row + 1);
            case 1:
                return assumption.getAdvocate();
            case 2:
                if (assumption instanceof PeptideAssumption) {
                    Peptide peptide = ((PeptideAssumption) assumption).getPeptide();
                    return peptide.getTaggedModifiedSequence(modificationProfile, true, true, true, excludeAllFixedPtms);
                } else if (assumption instanceof TagAssumption) {
                    TagAssumption tagAssumption = (TagAssumption) assumption;
                    return tagAssumption.getTag().getTaggedModifiedSequence(modificationProfile, true, true, true, excludeAllFixedPtms, false);
                } else {
                    throw new UnsupportedOperationException("Sequence display not implemented for assumption " + assumption.getClass() + ".");
                }
            case 3:
                if (assumption instanceof TagAssumption) {
                    TagAssumption tagAssumption = (TagAssumption) assumption;
                    return tagAssumption.getTheoreticMz(true, true);
                } else {
                    try {
                        return assumption.getTheoreticMz();
                    } catch (InterruptedException ex) {
                        return null;
                    }
                }
            case 4:
                return assumption.getIdentificationCharge().value;
            case 5:
                if (assumption instanceof TagAssumption) {
                    TagAssumption tagAssumption = (TagAssumption) assumption;
                    return tagAssumption.getTag().getNTerminalGap();
                } else {
                    return 0.0;
                }
            case 6:
                if (assumption instanceof TagAssumption) {
                    TagAssumption tagAssumption = (TagAssumption) assumption;
                    return tagAssumption.getTag().getCTerminalGap();
                } else {
                    return 0.0;
                }
            case 7:
                if (assumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
                    PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                    pepnovoAssumptionDetails = (PepnovoAssumptionDetails) assumption.getUrParam(pepnovoAssumptionDetails);
                    return pepnovoAssumptionDetails.getRankScore();
                }
                return null;
            case 8:
                if (assumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
                    return assumption.getScore();
                }
                return null;
            case 9:
                if (assumption.getAdvocate() == Advocate.direcTag.getIndex()) {
                    return assumption.getScore();
                }
                return null;
            case 10:
                if (assumption.getAdvocate() == Advocate.pNovo.getIndex()) {
                    return assumption.getScore();
                }
                return null;
            case 11:
                if (assumption.getAdvocate() == Advocate.novor.getIndex()) {
                    return assumption.getScore();
                }
                return null;
            case 12:
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
            case 11:
                return Double.class;
            case 12:
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
