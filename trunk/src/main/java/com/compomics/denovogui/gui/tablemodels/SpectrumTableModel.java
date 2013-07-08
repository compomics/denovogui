package com.compomics.denovogui.gui.tablemodels;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import java.util.ArrayList;
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
    String spectrumFile = null;
    /**
     * The ordered spectrum keys
     */
    private ArrayList<String> orderedSpectrumKeys = null;

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
     */
    public SpectrumTableModel(String spectrumFile, Identification identification) {
        this.spectrumFile = spectrumFile;
        this.identification = identification;
        this.orderedSpectrumKeys = orderedSpectrumKeys;
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
        return 5;
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
                String spectrumTitle = spectrumFactory.getSpectrumTitles(spectrumFile).get(row);
                return spectrumTitle;
            case 2:
                try {
                    spectrumTitle = spectrumFactory.getSpectrumTitles(spectrumFile).get(row);
                    Precursor precursor = spectrumFactory.getPrecursor(spectrumFile, spectrumTitle);
                    return precursor.getMz();
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error";
                }
            case 3:
                try {
                    spectrumTitle = spectrumFactory.getSpectrumTitles(spectrumFile).get(row);
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
                    return "Error";
                }
            case 4:
                spectrumTitle = spectrumFactory.getSpectrumTitles(spectrumFile).get(row);
                String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                return identification.matchExists(spectrumKey);
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
