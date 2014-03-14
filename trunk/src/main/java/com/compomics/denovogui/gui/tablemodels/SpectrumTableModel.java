package com.compomics.denovogui.gui.tablemodels;

import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.math.BasicMathFunctions;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.table.DefaultTableModel;

/**
 * Model for a spectrum table.
 *
 * @author Marc Vaudel
 */
public class SpectrumTableModel extends DefaultTableModel {

    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The identification
     */
    private Identification identification;
    /**
     * The name of the spectrum file.
     */
    private String spectrumFile = null;
    /**
     * The ordered spectrum keys.
     */
    private ArrayList<String> orderedSpectrumTitles = null;
    /**
     * Boolean indicating whether the content of the table should be updated.
     */
    private boolean update = true;

    /**
     * Constructor.
     */
    public SpectrumTableModel() {
    }

    /**
     * Constructor.
     *
     * @param spectrumFile the spectrum file
     * @param identification the identifications
     * @param orderedSpectrumTitles the spectrum keys in the desired order. If
     * null the default order will be used
     */
    public SpectrumTableModel(String spectrumFile, Identification identification, ArrayList<String> orderedSpectrumTitles) {
        this.spectrumFile = spectrumFile;
        this.identification = identification;
        if (orderedSpectrumTitles != null) {
            this.orderedSpectrumTitles = orderedSpectrumTitles;
        } else {
            this.orderedSpectrumTitles = spectrumFactory.getSpectrumTitles(spectrumFile);
        }
    }

    @Override
    public int getRowCount() {
        if (spectrumFile == null) {
            return 0;
        }
        return spectrumFactory.getNSpectra(spectrumFile);
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
                return "Title";
            case 2:
                return "m/z";
            case 3:
                return "Charge";
            case 4:
                return "Int";
            case 5:
                return "RT";
            case 6:
                return "#Peaks";
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
        switch (column) {
            case 0:
                return row + 1;
            case 1:
                String spectrumTitle = orderedSpectrumTitles.get(row);
                return spectrumTitle;
            case 2:
                try {
                    spectrumTitle = orderedSpectrumTitles.get(row);
                    Precursor precursor = spectrumFactory.getPrecursor(spectrumFile, spectrumTitle);
                    return precursor.getMz();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 3:
                try {
                    spectrumTitle = orderedSpectrumTitles.get(row);
                    Precursor precursor = spectrumFactory.getPrecursor(spectrumFile, spectrumTitle);

                    if (precursor.getPossibleCharges().size() == 1) {
                        return precursor.getPossibleCharges().get(0).value;
                    } else if (precursor.getPossibleCharges().size() > 1) {
                        return precursor.getPossibleCharges().get(0).value; // @TODO: better support for multiple charges
                    } else {
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 4:
                try {
                    spectrumTitle = orderedSpectrumTitles.get(row);
                    Precursor precursor = spectrumFactory.getPrecursor(spectrumFile, spectrumTitle);
                    return precursor.getIntensity();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 5:
                try {
                    spectrumTitle = orderedSpectrumTitles.get(row);
                    Precursor precursor = spectrumFactory.getPrecursor(spectrumFile, spectrumTitle);
                    return precursor.getRt();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 6:
                try {
                    spectrumTitle = orderedSpectrumTitles.get(row);
                    MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFile, spectrumTitle);
                    return spectrum.getPeakList().size();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 7:
                try {
                    spectrumTitle = orderedSpectrumTitles.get(row);
                    String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                    if (update && identification.matchExists(spectrumKey)) {
                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> hitMap = spectrumMatch.getAllAssumptions(Advocate.pepnovo.getIndex());
                        if (hitMap != null) {
                            double bestScore = 0;
                            for (double score : hitMap.keySet()) {
                                if (score > bestScore) {
                                    bestScore = score;
                                }
                            }
                            return bestScore;
                        }
                    }
                    return "";
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 8:
                try {
                    spectrumTitle = orderedSpectrumTitles.get(row);
                    String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                    if (update && identification.matchExists(spectrumKey)) {
                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> hitMap = spectrumMatch.getAllAssumptions(Advocate.DirecTag.getIndex());
                        if (hitMap != null) {
                        double bestScore = 0;
                            for (double score : hitMap.keySet()) {
                                if (bestScore == 0 || score < bestScore) {
                                    bestScore = score;
                                }
                            }
                        return bestScore;
                        }
                    }
                    return "";
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 9:
                spectrumTitle = orderedSpectrumTitles.get(row);
                String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                return identification.matchExists(spectrumKey);
            default:
                return null;
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

    /**
     * Sets whether the table content should update.
     *
     * @param update if true the content of the table will update
     */
    public void setUpdate(boolean update) {
        this.update = update;
    }
}
