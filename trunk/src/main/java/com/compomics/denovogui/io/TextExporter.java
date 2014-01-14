package com.compomics.denovogui.io;

import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.tags.Tag;
import com.compomics.util.experiment.identification.tags.TagComponent;
import com.compomics.util.experiment.identification.tags.tagcomponents.MassGap;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.refinementparameters.PepnovoAssumptionDetails;
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
     * Separator used for the export.
     */
    private static final String separator2 = ";";

    /**
     * Exports the peptide matching results to a given file.
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
     * @throws java.lang.InterruptedException
     */
    public static void exportPeptides(File destinationFile, Identification identification, SearchParameters searchParameters, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        FileWriter f = new FileWriter(destinationFile);

        try {
            BufferedWriter b = new BufferedWriter(f);

            try {

                b.write("File Name" + separator + "Spectrum Title" + separator + "Measured m/z" + separator + "Measured Charge" + separator
                        + "Rank" + separator + "Protein(s)" + separator + "Peptide" + separator + "Variable Modifications" + separator + "Modified Sequence"
                        + separator + "Tag" + separator + "Modified tag sequence" + separator
                        + "RankScore" + separator + "Score" + separator + "N-Gap" + separator + "C-Gap" + separator
                        + "Theoretic m/z" + separator + "Identification Charge");
                b.newLine();

                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...");
                    // reset the progress bar
                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
                }

                for (String mgfFile : identification.getSpectrumFiles()) {
                    for (String spectrumKey : identification.getSpectrumIdentification(mgfFile)) {
                        if (identification.matchExists(spectrumKey)) {

                            String spectrumDetails = "";

                            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            spectrumDetails += mgfFile + separator + spectrumTitle + separator;

                            Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                            spectrumDetails += precursor.getMz() + separator + precursor.getPossibleChargesAsString() + separator;

                            ArrayList<PeptideAssumption> assumptions = new ArrayList<PeptideAssumption>();
                            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(Advocate.pepnovo.getIndex());
                            if (assumptionsMap != null) {
                                ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                                Collections.sort(scores, Collections.reverseOrder());
                                for (Double score : scores) {
                                    for (SpectrumIdentificationAssumption assumption : assumptionsMap.get(score)) {
                                        PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                        assumptions.add(peptideAssumption);
                                    }
                                }
                            }

                            for (PeptideAssumption peptideAssumption : assumptions) {
                                
                                b.write(spectrumDetails);
                                b.write(peptideAssumption.getRank() + separator);
                                
                                Peptide peptide = peptideAssumption.getPeptide();
                                String proteinText = "";
                                ArrayList<String> proteins = peptide.getParentProteinsNoRemapping();
                                Collections.sort(proteins);
                                for (String accession : proteins) {
                                    if (!proteinText.equals("")) {
                                        proteinText += separator2;
                                    }
                                    proteinText += accession;
                                }
                                b.write(proteinText + separator);
                                
                                b.write(peptide.getSequence() + separator);
                                b.write(getPeptideModificationsAsString(peptide) + separator);
                                b.write(peptide.getTaggedModifiedSequence(searchParameters.getModificationProfile(), false, false, true, false) + separator);
                                
                                TagAssumption tagAssumption = new TagAssumption();
                                tagAssumption = (TagAssumption) peptideAssumption.getUrParam(tagAssumption);
                                Tag tag = tagAssumption.getTag();
                                PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                                pepnovoAssumptionDetails = (PepnovoAssumptionDetails) tagAssumption.getUrParam(pepnovoAssumptionDetails);
                                b.write(tag.asSequence() + separator);
                                b.write(tag.getLongestAminoAcidSequence() + separator);
                                b.write(getTagModificationsAsString(tag) + separator);
                                b.write(tag.getTaggedModifiedSequence(searchParameters.getModificationProfile(), false, false, true, false) + separator);
                                b.write(pepnovoAssumptionDetails.getRankScore() + separator);
                                b.write(tagAssumption.getScore() + separator);
                                b.write(tag.getNTerminalGap() + separator);
                                b.write(tag.getCTerminalGap() + separator);
                                b.write(tag.getMass() + separator);
                                b.write(tagAssumption.getIdentificationCharge().value + separator);
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
     * @throws java.lang.InterruptedException
     */
    public static void exportTags(File destinationFile, Identification identification, SearchParameters searchParameters, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        FileWriter f = new FileWriter(destinationFile);

        try {
            BufferedWriter b = new BufferedWriter(f);

            try {

                b.write("File Name" + separator + "Spectrum Title" + separator + "Measured m/z" + separator + "Measured Charge" + separator
                        + "Rank" + separator + "Tag" + separator + "Longest AminoAcid sequence" + separator + "Variable Modifications" + separator + "Modified Sequence" + separator
                        + "RankScore" + separator + "Score" + separator + "N-Gap" + separator + "C-Gap" + separator
                        + "Theoretic m/z" + separator + "Identification Charge");
                b.newLine();

                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...");
                    // reset the progress bar
                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
                }

                for (String mgfFile : identification.getSpectrumFiles()) {
                    for (String spectrumKey : identification.getSpectrumIdentification(mgfFile)) {
                        if (identification.matchExists(spectrumKey)) {

                            String spectrumDetails = "";

                            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            spectrumDetails += mgfFile + separator + spectrumTitle + separator;

                            Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                            spectrumDetails += precursor.getMz() + separator + precursor.getPossibleChargesAsString() + separator;

                            ArrayList<TagAssumption> assumptions = new ArrayList<TagAssumption>();
                            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(Advocate.pepnovo.getIndex());
                            if (assumptionsMap != null) {
                                ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                                Collections.sort(scores, Collections.reverseOrder());
                                for (Double score : scores) {
                                    for (SpectrumIdentificationAssumption assumption : assumptionsMap.get(score)) {
                                        TagAssumption tagAssumption = (TagAssumption) assumption;
                                        assumptions.add(tagAssumption);
                                    }
                                }
                            }

                            int rank = 0;
                            for (TagAssumption tagAssumption : assumptions) {
                                Tag tag = tagAssumption.getTag();
                                PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                                pepnovoAssumptionDetails = (PepnovoAssumptionDetails) tagAssumption.getUrParam(pepnovoAssumptionDetails);
                                b.write(spectrumDetails);
                                b.write(++rank + separator);
                                b.write(tag.asSequence() + separator);
                                b.write(tag.getLongestAminoAcidSequence() + separator);
                                b.write(getTagModificationsAsString(tag) + separator);
                                b.write(tag.getTaggedModifiedSequence(searchParameters.getModificationProfile(), false, false, true, false) + separator);
                                b.write(pepnovoAssumptionDetails.getRankScore() + separator);
                                b.write(tagAssumption.getScore() + separator);
                                b.write(tag.getNTerminalGap() + separator);
                                b.write(tag.getCTerminalGap() + separator);
                                b.write(tag.getMass() + separator);
                                b.write(tagAssumption.getIdentificationCharge().value + separator);
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
     * Exports the BLAST-compatible identification results to a given file.
     *
     * @param destinationFile the destination file
     * @param identification the identification object containing identification
     * details
     * @param searchParameters the search parameters used for the search
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing to cancel the process.
     * @param scoreThreshold De novo score threshold
     *
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws MzMLUnmarshallerException
     * @throws java.lang.InterruptedException
     */
    public static void exportBlastPSMs(File destinationFile, Identification identification, SearchParameters searchParameters,
            WaitingHandler waitingHandler, String scoreThreshold) throws IOException, SQLException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        FileWriter f = new FileWriter(destinationFile);
        double threshold = 0;
        if (!scoreThreshold.equals("")) {
            threshold = Double.valueOf(scoreThreshold);
        }

        try {
            BufferedWriter b = new BufferedWriter(f);

            try {

                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...");
                    // reset the progress bar
                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
                }

                for (String mgfFile : identification.getSpectrumFiles()) {
                    for (String spectrumKey : identification.getSpectrumIdentification(mgfFile)) {
                        if (identification.matchExists(spectrumKey)) {

                            String spectrumDetails = ">";

                            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            spectrumDetails += mgfFile + separator2 + spectrumTitle + separator2;

                            Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                            spectrumDetails += precursor.getMz() + separator2 + precursor.getPossibleChargesAsString() + separator2;

                            ArrayList<TagAssumption> assumptions = new ArrayList<TagAssumption>();
                            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(Advocate.pepnovo.getIndex());
                            if (assumptionsMap != null) {
                                ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                                Collections.sort(scores, Collections.reverseOrder());
                                for (Double score : scores) {
                                    for (SpectrumIdentificationAssumption assumption : assumptionsMap.get(score)) {
                                        TagAssumption tagAssumption = (TagAssumption) assumption;
                                        assumptions.add(tagAssumption);
                                    }
                                }
                            }

                            for (TagAssumption tagAssumption : assumptions) {
                                PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                                pepnovoAssumptionDetails = (PepnovoAssumptionDetails) tagAssumption.getUrParam(pepnovoAssumptionDetails);
                                if (tagAssumption.getScore() > threshold) {
                                    b.write(spectrumDetails);
                                    b.write(pepnovoAssumptionDetails.getRankScore() + separator2);
                                    b.write(tagAssumption.getScore() + "");
                                    b.newLine();
                                    b.write(tagAssumption.getTag().getLongestAminoAcidSequence());
                                    b.newLine();
                                }
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

    /**
     * Returns the tag modifications as a string.
     *
     * @param tag the tag
     * @return the peptide modifications as a string
     */
    public static String getTagModificationsAsString(Tag tag) {

        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
        int offset = 0;
        for (TagComponent tagComponent : tag.getContent()) {
            if (tagComponent instanceof MassGap) {
                offset++;
            } else if (tagComponent instanceof AminoAcidPattern) {
                AminoAcidPattern aminoAcidPattern = (AminoAcidPattern) tagComponent;
                for (int i = 1; i <= aminoAcidPattern.length(); i++) {
                    for (ModificationMatch modificationMatch : aminoAcidPattern.getModificationsAt(i)) {
                        if (modificationMatch.isVariable()) {
                            if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                                modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                            }
                            modMap.get(modificationMatch.getTheoreticPtm()).add(i + offset);
                        }
                    }
                }
                offset += aminoAcidPattern.length();
            } else {
                throw new IllegalArgumentException("Modification summary not implemented for TagComponent " + tagComponent.getClass() + ".");
            }
        }
        
        StringBuilder result = new StringBuilder();
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
