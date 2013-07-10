package com.compomics.denovogui.io;

import com.compomics.util.denovo.PeptideAssumptionDetails;
import com.compomics.util.experiment.biology.Peptide;
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
 * @author Marc Vaudel
 */
public class TextExporter {

    /**
     * Separator used for the export.
     */
    private static final String separator = "\t";

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
