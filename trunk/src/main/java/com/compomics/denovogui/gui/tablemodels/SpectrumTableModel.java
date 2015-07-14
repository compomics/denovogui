package com.compomics.denovogui.gui.tablemodels;

import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
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
        return 12;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return " ";
            case 1:
                return "ID";
            case 2:
                return "Title";
            case 3:
                return "m/z";
            case 4:
                return "Charge";
            case 5:
                return "Int";
            case 6:
                return "RT";
            case 7:
                return "#Peaks";
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

        String spectrumTitle = orderedSpectrumTitles.get(row);

        switch (column) {
            case 0:
                return row + 1;
            case 1:
                try {
                    String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                    if (update && identification.matchExists(spectrumKey)) {
                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> allAssumptions = identification.getAssumptions(spectrumKey);
                        return allAssumptions.keySet().size();
                    }
                    return 0; // no match found
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 2:
                return spectrumTitle;
            case 3:
                try {
                    Precursor precursor = spectrumFactory.getPrecursor(spectrumFile, spectrumTitle);
                    return precursor.getMz();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 4:
                try {
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
            case 5:
                try {
                    Precursor precursor = spectrumFactory.getPrecursor(spectrumFile, spectrumTitle);
                    return precursor.getIntensity();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 6:
                try {
                    Precursor precursor = spectrumFactory.getPrecursor(spectrumFile, spectrumTitle);
                    return precursor.getRt();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 7:
                try {
                    MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFile, spectrumTitle);
                    return spectrum.getPeakList().size();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 8:
                try {
                    String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                    if (update && identification.matchExists(spectrumKey)) {
                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> allAssumptions = identification.getAssumptions(spectrumKey);
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> hitMap = allAssumptions.get(Advocate.pepnovo.getIndex());
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
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 9:
                try {
                    String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                    if (update && identification.matchExists(spectrumKey)) {
                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> allAssumptions = identification.getAssumptions(spectrumKey);
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> hitMap = allAssumptions.get(Advocate.direcTag.getIndex());
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
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 10:
                try {
                    String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                    if (update && identification.matchExists(spectrumKey)) {
                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> allAssumptions = identification.getAssumptions(spectrumKey);
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> hitMap = allAssumptions.get(Advocate.pNovo.getIndex());
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
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 11:
                String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                return identification.matchExists(spectrumKey);
            default:
                return null;
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
            case 1:
            case 4:
            case 7:
                return Integer.class;
            case 2:
                return String.class;
            case 3:
            case 5:
            case 6:
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

    /**
     * Sets whether the table content should update.
     *
     * @param update if true the content of the table will update
     */
    public void setUpdate(boolean update) {
        this.update = update;
    }
}
