package com.compomics.denovogui.io;

import com.compomics.util.experiment.io.mass_spectrometry.MsFileHandler;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.io.IoUtil;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Compatibility wrapper around the Utilities 5 spectrum provider.
 */
public class SpectrumFactory {

    /**
     * The singleton instance.
     */
    private static final SpectrumFactory instance = new SpectrumFactory();
    /**
     * The spectrum provider.
     */
    private MsFileHandler msFileHandler = new MsFileHandler();
    /**
     * File names indexed by the Utilities file name without extension.
     */
    private final HashMap<String, File> registeredFiles = new HashMap<>();

    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance
     */
    public static SpectrumFactory getInstance() {
        return instance;
    }

    /**
     * Clears all registered spectra.
     */
    public void clearFactory() {
        closeFiles();
        msFileHandler = new MsFileHandler();
        registeredFiles.clear();
    }

    /**
     * Registers spectra from the given file.
     *
     * @param spectrumFile the spectrum file
     * @param waitingHandler the waiting handler
     *
     * @throws IOException if the file cannot be read
     */
    public void addSpectra(File spectrumFile, WaitingHandler waitingHandler) throws IOException {
        msFileHandler.register(spectrumFile, waitingHandler);
        registeredFiles.put(IoUtil.removeExtension(spectrumFile.getName()), spectrumFile);
    }

    /**
     * Closes the registered spectrum files.
     */
    public void closeFiles() {
        msFileHandler.close();
    }
    public void close() {
        closeFiles();
    }

    public SpectrumProvider getSpectrumProvider() {
        return msFileHandler;
    }

    /**
     * Returns the registered spectrum file names.
     *
     * @return the registered spectrum file names
     */
    public ArrayList<String> getMgfFileNames() {
        return new ArrayList<>(Arrays.asList(msFileHandler.getOrderedFileNamesWithoutExtensions()));
    }

    /**
     * Returns all spectrum titles for the given spectrum file.
     *
     * @param spectrumFile the spectrum file
     *
     * @return all spectrum titles
     */
    public ArrayList<String> getSpectrumTitles(String spectrumFile) {
        String fileName = IoUtil.removeExtension(new File(spectrumFile).getName());
        return new ArrayList<>(Arrays.asList(msFileHandler.getSpectrumTitles(fileName)));
    }

    /**
     * Returns the number of spectra in all files.
     *
     * @return the number of spectra
     */
    public int getNSpectra() {
        return getMgfFileNames().stream().mapToInt(this::getNSpectra).sum();
    }

    /**
     * Returns the number of spectra in a file.
     *
     * @param spectrumFile the spectrum file
     *
     * @return the number of spectra
     */
    public int getNSpectra(String spectrumFile) {
        return getSpectrumTitles(spectrumFile).size();
    }

    /**
     * Returns the precursor for the given spectrum key.
     *
     * @param spectrumKey the spectrum key
     *
     * @return the precursor
     */
    public Precursor getPrecursor(String spectrumKey) {
        return getPrecursor(Spectrum.getSpectrumFile(spectrumKey), Spectrum.getSpectrumTitle(spectrumKey));
    }

    /**
     * Returns the precursor for the given spectrum.
     *
     * @param spectrumFile the spectrum file
     * @param spectrumTitle the spectrum title
     *
     * @return the precursor
     */
    public Precursor getPrecursor(String spectrumFile, String spectrumTitle) {
        String fileName = IoUtil.removeExtension(new File(spectrumFile).getName());
        return msFileHandler.getPrecursor(fileName, spectrumTitle);
    }

    /**
     * Returns the spectrum for the given spectrum key.
     *
     * @param spectrumKey the spectrum key
     *
     * @return the spectrum
     */
    public Spectrum getSpectrum(String spectrumKey) {
        return getSpectrum(Spectrum.getSpectrumFile(spectrumKey), Spectrum.getSpectrumTitle(spectrumKey));
    }

    /**
     * Returns the spectrum.
     *
     * @param spectrumFile the spectrum file
     * @param spectrumTitle the spectrum title
     *
     * @return the spectrum
     */
    public Spectrum getSpectrum(String spectrumFile, String spectrumTitle) {
        String fileName = IoUtil.removeExtension(new File(spectrumFile).getName());
        return msFileHandler.getSpectrum(fileName, spectrumTitle);
    }
    public ArrayList<String> getPostcursorSpectrumTitles(String fileNameWithoutExtension, String spectrumTitle) {
        return msFileHandler.getPostcursorSpectrumTitles(fileNameWithoutExtension, spectrumTitle);
    }
    public double getPrecursorMz(String fileNameWithoutExtension, String spectrumTitle) {
        return msFileHandler.getPrecursorMz(fileNameWithoutExtension, spectrumTitle);
    }
    public double getPrecursorRt(String fileNameWithoutExtension, String spectrumTitle) {
        return msFileHandler.getPrecursorRt(fileNameWithoutExtension, spectrumTitle);
    }
    public int getSpectrumLevel(String fileNameWithoutExtension, String spectrumTitle) {
        return msFileHandler.getSpectrumLevel(fileNameWithoutExtension, spectrumTitle);
    }
    public double[][] getPeaks(String fileNameWithoutExtension, String spectrumTitle) {
        return msFileHandler.getPeaks(fileNameWithoutExtension, spectrumTitle);
    }
    public double getMinPrecMz(String fileNameWithoutExtension) {
        return msFileHandler.getMinPrecMz(fileNameWithoutExtension);
    }
    public double getMaxPrecMz(String fileNameWithoutExtension) {
        return msFileHandler.getMaxPrecMz(fileNameWithoutExtension);
    }
    public double getMaxPrecInt(String fileNameWithoutExtension) {
        return msFileHandler.getMaxPrecInt(fileNameWithoutExtension);
    }
    public double getMaxPrecRT(String fileNameWithoutExtension) {
        return msFileHandler.getMaxPrecRT(fileNameWithoutExtension);
    }
    public double getMinPrecMz() {
        return msFileHandler.getMinPrecMz();
    }
    public String[] getOrderedFileNamesWithoutExtensions() {
        return msFileHandler.getOrderedFileNamesWithoutExtensions();
    }
    public HashMap<String, String> getFilePaths() {
        return msFileHandler.getFilePaths();
    }
    public HashMap<String, String> getCmsFilePaths() {
        return msFileHandler.getCmsFilePaths();
    }

    /**
     * Returns true if the spectrum is loaded.
     *
     * @param spectrumKey the spectrum key
     *
     * @return true if the spectrum is loaded
     */
    public boolean spectrumLoaded(String spectrumKey) {
        return getSpectrum(spectrumKey) != null;
    }

    /**
     * Returns the maximum precursor m/z.
     *
     * @return the maximum precursor m/z
     */
    public Double getMaxMz() {
        return msFileHandler.getMaxPrecMz();
    }

    /**
     * Returns the maximum charge.
     *
     * @return the maximum charge
     */
    public Integer getMaxCharge() {
        int maxCharge = 0;
        for (String fileName : getMgfFileNames()) {
            for (String spectrumTitle : getSpectrumTitles(fileName)) {
                Precursor precursor = getPrecursor(fileName, spectrumTitle);
                for (int charge : precursor.possibleCharges) {
                    maxCharge = Math.max(maxCharge, Math.abs(charge));
                }
            }
        }
        return maxCharge;
    }

    /**
     * Returns the maximum intensity.
     *
     * @return the maximum intensity
     */
    public Double getMaxIntensity() {
        return msFileHandler.getMaxPrecInt();
    }

    /**
     * Returns the maximum retention time.
     *
     * @return the maximum retention time
     */
    public Double getMaxRT() {
        return msFileHandler.getMaxPrecRT();
    }

    /**
     * Returns the minimum retention time.
     *
     * @return the minimum retention time
     */
    public Double getMinRT() {
        double minRt = Double.MAX_VALUE;
        for (String fileName : getMgfFileNames()) {
            for (String spectrumTitle : getSpectrumTitles(fileName)) {
                minRt = Math.min(minRt, getPrecursor(fileName, spectrumTitle).rt);
            }
        }
        return minRt == Double.MAX_VALUE ? 0.0 : minRt;
    }

    /**
     * Returns the maximum number of peaks.
     *
     * @return the maximum number of peaks
     */
    public Integer getMaxPeakCount() {
        int maxPeakCount = 0;
        for (String fileName : getMgfFileNames()) {
            for (String spectrumTitle : getSpectrumTitles(fileName)) {
                maxPeakCount = Math.max(maxPeakCount, getSpectrum(fileName, spectrumTitle).getNPeaks());
            }
        }
        return maxPeakCount;
    }
}
