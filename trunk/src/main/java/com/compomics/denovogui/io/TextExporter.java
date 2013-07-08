package com.compomics.denovogui.io;

import com.compomics.util.denovo.PeptideAssumptionDetails;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class allows exporting the results in a text file.
 *
 * @author Thilo Muth
 */
public class TextExporter {

    /**
     * Separator used for the export.
     */
    private static final String separator = "\t";

    /**
     * The different export types.
     */
    public enum ExportType {

        PSMS, ASSUMPTIONS
    }

    /**
     * This method exports the PSM results.
     *
     * @param filePath The file path to the exported file.
     * @param identification The identification result.
     * @throws IOException Exception thrown when the file access fails.
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportPSMs(String filePath, Identification identification) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        // Init the buffered writer.
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath)));

        int count = 1;

        // Peptide header.
        writer.write(getPSMHeader());
        writer.newLine();

        for (String spectrumFile : identification.getSpectrumFiles()) {

            identification.loadSpectrumMatches(spectrumFile, null);

            for (String spectrumTitle : identification.getSpectrumIdentification(spectrumFile)) {

                String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                PeptideAssumption peptideAssumption = spectrumMatch.getFirstHit(Advocate.PEPNOVO);

                writer.write(count++ + separator);
                writer.write(spectrumFile + separator);
                writer.write(spectrumTitle + separator);
                Peptide peptide = peptideAssumption.getPeptide();
                //@TODO add modifications
                writer.write(peptide.getSequence() + separator);
                String proteinList = "";
                for (String accession : peptide.getParentProteins()) {
                    if (!proteinList.equals("")) {
                        proteinList += ", ";
                    }
                    proteinList += accession;
                }
                writer.write(proteinList + separator);
                //@TODO: implement other stuffs
                writer.newLine();
            }
        }

        writer.close();
    }

    /**
     * This method exports the assumptions for a selected spectrum.
     *
     * @param selectedSpectrumFile Selected MGF file name.
     * @param selectedSpectrumTitle Selected spectrum title.
     * @param selectedFile Selected output file.
     * @param identification De novo identification.
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static void exportSingleAssumptions(String selectedSpectrumFile, String selectedSpectrumTitle, File selectedFile, Identification identification) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException {

        String psmKey = Spectrum.getSpectrumKey(selectedSpectrumFile, selectedSpectrumTitle);
        if (identification.matchExists(psmKey)) {
            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);

            HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);

            if (selectedFile != null) {
                FileWriter w = new FileWriter(selectedFile);
                BufferedWriter bw = new BufferedWriter(w);

                // Write MGF file name.
                bw.write("MGF=" + selectedSpectrumFile);
                bw.newLine();
                // Write spectrum title.
                bw.write("TITLE=" + selectedSpectrumTitle);
                bw.newLine();

                ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                Collections.sort(scores, Collections.reverseOrder());
                for (int i = 0; i < scores.size(); i++) {
                    for (PeptideAssumption assumption : assumptionsMap.get(scores.get(i))) {
                        bw.write(assumption.getPeptide().getSequence() + "\t" + assumption.getScore());
                        bw.newLine();
                    }
                }
                bw.close();
                w.close();
            }
        }
    }

    /**
     * This method exports the de novo assumptions.
     *
     * @param selectedFile The file to export to.
     * @param identification The identification result.
     * @throws IOException Exception thrown when the file access fails.
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportAssumptions(File selectedFile, Identification identification) throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        FileWriter w = new FileWriter(selectedFile);
        BufferedWriter bw = new BufferedWriter(w);
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

        // Iterate the spectrum files.
        for (String fileName : spectrumFactory.getMgfFileNames()) {
            // Iterate the spectrum titles.
            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(fileName)) {
                String psmKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);

                if (identification.matchExists(psmKey)) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                    HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);

                    if (selectedFile != null && assumptionsMap != null) {
                        // Write MGF file name.
                        bw.write("MGF=" + fileName);
                        bw.newLine();

                        // Write spectrum title.
                        bw.write("TITLE=" + spectrumTitle);
                        bw.newLine();

                        ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                        Collections.sort(scores, Collections.reverseOrder());
                        for (int i = 0; i < scores.size(); i++) {
                            for (PeptideAssumption assumption : assumptionsMap.get(scores.get(i))) {
                                bw.write(assumption.getPeptide().getSequence() + "\t" + assumption.getScore());
                                bw.newLine();
                            }
                        }
                    }
                    bw.newLine();
                }
            }
        }
        bw.close();
        w.close();
    }

    /**
     * Returns the PSM header string.
     *
     * @return The PSM header string.
     */
    private static String getPSMHeader() {
        return "#" + separator
                + "File" + separator
                + "Spectrum title" + separator
                + "Peptide Sequence" + separator
                + "Protein Accession" + separator;
    }

    /**
     * Returns the score header string.
     *
     * @return The score header string.
     */
    private static String getAssumptionHeader() {
        return "PSM No." + separator
                + "File" + separator
                + "Title" + separator
                + "Rank" + separator
                + "sequence" + separator
                + "length" + separator
                + "identification charge" + separator
                + "N-term gap" + separator
                + "C-term gap" + separator
                + "rank score" + separator
                + "Pepnovo score" + separator;
    }

    /**
     * Exports the identification results to a given file.
     *
     * @param destinationFile the destination file
     * @param identification the identification object containing identification
     * details
     * @param searchParameters the search parameters used for the search
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing to cancel the process.
     *
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws MzMLUnmarshallerException
     */
    public static void exportPSMs(File destinationFile, Identification identification, SearchParameters searchParameters, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, MzMLUnmarshallerException {

        FileWriter f = new FileWriter(destinationFile);
        String leftPaddding = separator + separator + separator + separator;

        try {

            BufferedWriter b = new BufferedWriter(f);

            try {

                b.write("File Name" + separator + "Spectrum Title" + separator + "Measured m/z" + separator + "Measured Charge" + separator
                        + "Rank" + separator + "Sequence" + separator + "Variable Modifications" + separator + "Modified Sequence" + separator
                        + "RankScore" + separator + "Score" + separator + "N-Gap" + separator + "C-Gap" + separator
                        + "Theoretic m/z" + separator + "Identification Charge");
                b.newLine();

                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...)");
                    // reset the progress bar
                    waitingHandler.setSecondaryProgressCounter(0);
                    waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
                }

                for (String mgfFile : identification.getSpectrumFiles()) {
                    for (String spectrumKey : identification.getSpectrumIdentification(mgfFile)) {
                        if (identification.matchExists(spectrumKey)) {

                            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            b.write(mgfFile + separator + spectrumTitle + separator);

                            Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                            b.write(precursor.getMz() + separator + precursor.getPossibleChargesAsString() + separator);

                            ArrayList<PeptideAssumption> assumptions = new ArrayList<PeptideAssumption>();
                            HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);
                            if (assumptionsMap != null) {
                                ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                                Collections.sort(scores, Collections.reverseOrder());
                                for (Double score : scores) {
                                    assumptions.addAll(assumptionsMap.get(score));
                                }
                            }
                            int rank = 0;
                            for (PeptideAssumption peptideAssumption : assumptions) {
                                Peptide peptide = peptideAssumption.getPeptide();
                                PeptideAssumptionDetails peptideAssumptionDetails = new PeptideAssumptionDetails();
                                peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);

                                String padding = "";

                                if (rank > 0) {
                                    padding = leftPaddding;
                                }

                                b.write(padding + ++rank + separator);
                                b.write(peptide.getSequence() + separator);
                                b.write(getPeptideModificationsAsString(peptide) + separator);
                                b.write(peptide.getTaggedModifiedSequence(searchParameters.getModificationProfile(), false, false, true) + separator);
                                b.write(peptideAssumptionDetails.getRankScore()+ separator);
                                b.write(peptideAssumption.getScore() + separator);
                                b.write(peptideAssumptionDetails.getnTermGap() + separator);
                                b.write(peptideAssumptionDetails.getcTermGap() + separator);
                                b.write(peptideAssumption.getTheoreticMz() + separator);
                                b.write(peptideAssumption.getIdentificationCharge().value + separator);
                                b.newLine();
                            }
                            if (assumptions.isEmpty()) {
                                b.newLine(); //This should not happen. Should.
                            }
                            if (waitingHandler != null) {
                                waitingHandler.increaseSecondaryProgressCounter();
                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
                            }
                        }

                        b.newLine();
                    }
                }

            } finally {
                b.close();
            }
        } finally {
            f.close();
        }
    }

    /**
     * Returns the peptide modifications as a string.
     *
     * @param peptide the peptide
     * @return the peptide modifications as a string
     */
    public static String getPeptideModificationsAsString(Peptide peptide) {

        StringBuilder result = new StringBuilder();

        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                    modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                }
                modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
            }
        }

        boolean first = true, first2;
        ArrayList<String> mods = new ArrayList<String>(modMap.keySet());

        Collections.sort(mods);
        for (String mod : mods) {
            if (first) {
                first = false;
            } else {
                result.append(", ");
            }
            first2 = true;
            result.append(mod);
            result.append(" (");
            for (int aa : modMap.get(mod)) {
                if (first2) {
                    first2 = false;
                } else {
                    result.append(", ");
                }
                result.append(aa);
            }
            result.append(")");
        }

        return result.toString();
    }
}
